//============================================================================================================================
#include "ets_sys.h"
#include "osapi.h"
#include "gpio.h"
#include "os_type.h"
#include "user_config.h"
#include "user_interface.h"
#include "driver/uart.h"
#include "driver/gpio16.h"
#include "driver/services.h"
#include "driver/PCF8574.h"
#include "driver/BH1715.h"
#include "driver/LSM303.h"
#include "driver/Configs.h"
#include "driver/Calculations.h"
//============================================================================================================================
extern int ets_uart_printf(const char *fmt, ...);
int (*console_printf)(const char *fmt, ...) = ets_uart_printf;

#define LOOP_PERIOD		(20) // in msec
#define user_procTaskPrio        0
#define user_procTaskQueueLen    1

static volatile os_timer_t loop_timer;
static void  loop(os_event_t *events);
uint8 addr = 0x3f;

uint8 out = 0;
unsigned char tmp[6];

sint16 Pitch, Roll, Yaw;

#define VERTICAL_OFFSET	    (600)
#define HORIZONTAL_OFFSET	(100)
#define ELEVATION_MAX		(5400)
#define ELEVATION_MIN		(1000)


int manualDuration = PROC_DURATION;
int vertMove = 10;



uint16 mTout = 1000;

sint16 headF, headFarr[FILTER_LENGHT];

//===================================================================================
void ICACHE_FLASH_ATTR stopMoving(void)
{
	direction = 0;
	move(direction);
	sysState.newPosition = 0;
}
//===================================================================================
void ICACHE_FLASH_ATTR motorFault(void)
{
	ets_uart_printf("mot err\r\n");
	sysState.motorFault = 1;
	blink = BLINK_MOTOR_FLT;
	stopMoving();
}
//======================= Main code function ============================================================
void ICACHE_FLASH_ATTR loop(os_event_t *events)
{

	if (flashWriteBit == 1) saveConfigs();

	if(dataPresent) dataPresent--;

	if(!sysState.byte) // standby
	{
		if(wifi_station_get_connect_status() == STATION_GOT_IP)
		{
			if(dataPresent) blink = BLINK_WAIT;
			else 			blink = BLINK_WAIT_NODATA;
		}
		else
			blink = BLINK_WAIT_UNCONNECTED;
	}

	//======== PCF8574 =====================
	terminators = PCF8574_readByte(addr)>>4;
	//======== BH1715  =====================
	static uint16 lightOld, lightCntr = 10;
	if(lightCntr) lightCntr--;
	else
	{
		lightCntr = 100;
		BH1715(I2C_READ, 0x23, 0x01, (unsigned char*)&light, 2);
		light = ((unsigned char*)&light)[1] |
				((unsigned char*)&light)[0] << 8;
	}
	//=======================================
	if(!sysState.manualMoveRemote) keyProcessing((~PCF8574_readByte(0x3B)) & (~0x70));

	//======== LSM303  =====================
//	if (!lsm303(I2C_READ,  LSM303A_I2C_ADDR, LSM303A_OUT_X_L, accel.byte, 6) && !sysState.manualMove && !sysState.motorFault)
//		motorFault();
	static uint16 compassOk = 100;

	if(sysState.sensorError)
	{
		if(compassOk) compassOk--;
		if(!compassOk )
		{
			//======== light sensor init =======================
			BH1715(I2C_WRITE, 0x23, 0x01, 0, 1);
			BH1715(I2C_WRITE, 0x23, 0x10, 0, 1);
			LSM303Init();
			compassOk = 100;
		}

	}
	else if(!lsm303(I2C_READ,  LSM303A_I2C_ADDR, LSM303A_OUT_X_L, accel.byte, 6))
	{
		if(compassOk) compassOk--;
		if(!compassOk )//&& !sysState.manualMove && !sysState.motorFault)
	    {
			if(!sysState.manualMove)
			{
				sysState.sensorError = 1;
				motorFault();
			}
			compassOk = 100;
			ets_uart_printf("Sensor error \r\n");
			//LSM303Init();
	    }
	}
	else compassOk = 100;


	lsm303(I2C_READ,  LSM303M_I2C_ADDR, LSM303M_OUT_X_H, tmp, 6);
	compass.x = ((tmp[0] << 8) | tmp[1]) ;
	compass.z = ((tmp[2] << 8) | tmp[3]) ;
	compass.y = ((tmp[4] << 8) | tmp[5]) ;

	//ets_uart_printf("%d\t%d\t%d\n", cc.x, cc.y, cc.z);


	addValueToArray(compass.x,  cx); addValueToArray(compass.y,  cy); addValueToArray(compass.z,  cz);
	addValueToArray(accel.x,  ax); 	 addValueToArray(accel.y,  ay);	  addValueToArray(accel.z,  az);

	cc.x = mFilter(cx,  FILTER_LENGHT);	cc.y = mFilter(cy,  FILTER_LENGHT);	cc.z = mFilter(cz,  FILTER_LENGHT);
	aa.x = mFilter(ax,  FILTER_LENGHT);	aa.y = mFilter(ay,  FILTER_LENGHT);	aa.z = mFilter(az,  FILTER_LENGHT);

	getAngles(&aa, &cc, &Pitch, &Roll, &Yaw);

	_roll    = Roll;
	_pitch   = Pitch;
	_heading = Yaw;

	orientation.real.elevation = ((long)_pitch   * 18000 / 31416);
	orientation.real.azimuth   = ((long)_heading * 18000 / 31416);
	if(orientation.real.azimuth < 0) orientation.real.azimuth += 36000;

	addValueToArray(orientation.real.azimuth,  headFarr);
	headF =  mFilter(headFarr,  FILTER_LENGHT);
	//ets_uart_printf("sysState = %d\r\n", sysState);


 //=== sun tracking ====================================================

 if(!sysState.manualMove){

		static sint16 azOld = 0, elOld = 0;

		if(sysState.newPosition && !sysState.motorFault)
		{
			sysState.newPosition = 0;
			if(!sysState.moving)
			{
				if      (orientation.income.elevation > ( orientation.real.elevation + 6 * HORIZONTAL_OFFSET)) {direction = DOWN; elOld = orientation.real.elevation;}
				else if (orientation.income.elevation < ( orientation.real.elevation - 6 * HORIZONTAL_OFFSET)) {direction = UP;   elOld = orientation.real.elevation;}
				else if (orientation.income.azimuth   > ( headF + 6 * HORIZONTAL_OFFSET)) {direction = RIGHT; azOld = orientation.real.azimuth;}
				else if (orientation.income.azimuth   < ( headF - 6 * HORIZONTAL_OFFSET)) {direction = LEFT;  azOld = orientation.real.azimuth;}
				else  direction = 0;

				move(direction);
				mTout = 1000;//6000;
			}
		}

		if (sysState.moving)
		{
			if (mTout) 	mTout--;

			switch (direction)
			{
				case RIGHT:
				case LEFT:
					{
						if (orientation.income.azimuth <= (orientation.real.azimuth + HORIZONTAL_OFFSET) &&
							orientation.income.azimuth >= (orientation.real.azimuth - HORIZONTAL_OFFSET))
						{
							stopMoving();
							ets_uart_printf("mTout = %d\r\n", mTout);
							ets_uart_printf("    real      old \r\nazim %d,  %d\r\nelev %d,  %d\r\n",
									orientation.real.azimuth, azOld, orientation.real.elevation, elOld);
						}
						else if (!mTout & // not moving
								(orientation.real.azimuth < (azOld + 200) && orientation.real.azimuth > (azOld - 200)))
							motorFault();

					}break;

				case UP:
				case DOWN:
					{
						if (orientation.income.elevation <= (orientation.real.elevation + HORIZONTAL_OFFSET) &&
							orientation.income.elevation >= (orientation.real.elevation - HORIZONTAL_OFFSET))
						{
							stopMoving();
							ets_uart_printf("mTout = %d\r\n", mTout);
							ets_uart_printf("    real      old \r\nazim %d,  %d\r\nelev %d,  %d\r\n",
									orientation.real.azimuth, azOld, orientation.real.elevation, elOld);
						}
						else if (!mTout &  // not moving
								(orientation.real.elevation < (elOld + 200) && orientation.real.elevation > (elOld - 200)))
							motorFault();
					}break;
			}
		}

	}
}
//==============================================================================
void ICACHE_FLASH_ATTR setup(void)
{
	indicationInit();

	set_gpio_mode(2, GPIO_PULLUP, GPIO_OUTPUT);
	set_gpio_mode(1, GPIO_PULLUP, GPIO_OUTPUT);

	i2c_init();
	PCF8574_writeByte(0x3f, 0 | 0x0f);
	PCF8574_writeByte(0x3f, ((0x0f) << 4) | 0x0f);
	//move(0);
	PCF8574_writeByte(0x3B, (0x00 << 4) | 0x8f);

	//======== light sensor init =======================
	BH1715(I2C_WRITE, 0x23, 0x01, 0, 1);
	BH1715(I2C_WRITE, 0x23, 0x10, 0, 1);
	//==================================================
	LSM303Init();


	readConfigs();
	wifi_station_disconnect();
	wifi_station_set_auto_connect(0);

	ets_uart_printf("configs.wifi.SSID %s\r\n", 		configs.wifi.SSID);
	ets_uart_printf("configs.wifi.SSID_PASS %s\r\n", 	configs.wifi.SSID_PASS);
	ets_uart_printf("configs.wifi.OTAIP %s\r\n", 		configs.wifi.OTAIP);

	if(configs.wifi.mode == STATION_MODE)
		setup_wifi_st_mode();
	else if(configs.wifi.mode == SOFTAP_MODE)
		setup_wifi_ap_mode();

	button_init();
	UDP_Init_client();

	// Start loop timer
	os_timer_disarm(&loop_timer);
	os_timer_setfn(&loop_timer, (os_timer_func_t *) loop, NULL);
	os_timer_arm(&loop_timer, LOOP_PERIOD, true);
}
//========================== Init function  =============================================================
//
void ICACHE_FLASH_ATTR user_init(void)
{



	uart_init(BIT_RATE_115200, BIT_RATE_115200);
	os_delay_us(1000);
	ets_uart_printf("\r\n\r\n*** Solar slave\r\n*** Firmware compiled: %s\r\n\r\n", VERSION);

	char macaddr[6];
//	wifi_get_macaddr(STATION_MODE, macaddr);
//	ets_uart_printf(MACSTR, MAC2STR(macaddr));
//	ets_uart_printf("\r\n");
//	wifi_get_macaddr(SOFTAP_MODE, macaddr);
//	ets_uart_printf(MACSTR, MAC2STR(macaddr));
//	ets_uart_printf("\r\n");
	wifi_get_macaddr(NULL_MODE, macaddr);
		ets_uart_printf(MACSTR, MAC2STR(macaddr));
		ets_uart_printf("\r\n");

	thingspeakField =  macMatch(macaddr);

	ets_uart_printf("mac match %d\r\n", thingspeakField);

//	//saveConfigs();

//	checkConfigs();




	// Start setup timer
	os_timer_disarm(&loop_timer);
	os_timer_setfn(&loop_timer, (os_timer_func_t *) setup, NULL);
	os_timer_arm(&loop_timer, LOOP_PERIOD * 2, false);
}
/******************************************************************************
 * FunctionName : user_rf_cal_sector_set
 * Description  : SDK just reversed 4 sectors, used for rf init data and paramters.
 *                We add this function to force users to set rf cal sector, since
 *                we don't know which sector is free in user's application.
 *                sector map for last several sectors : ABBBCDDD
 *                A : rf cal
 *                B : at parameters
 *                C : rf init data
 *                D : sdk parameters
 * Parameters   : none
 * Returns      : rf cal sector
*******************************************************************************/
uint32 ICACHE_FLASH_ATTR user_rf_cal_sector_set(void)
{
    enum flash_size_map size_map = system_get_flash_size_map();
    uint32 rf_cal_sec = 0;

    switch (size_map) {
        case FLASH_SIZE_4M_MAP_256_256:
            rf_cal_sec = 128 - 8;
            break;

        case FLASH_SIZE_8M_MAP_512_512:
            rf_cal_sec = 256 - 5;
            break;

        case FLASH_SIZE_16M_MAP_512_512:
        case FLASH_SIZE_16M_MAP_1024_1024:
            rf_cal_sec = 512 - 5;
            break;

        case FLASH_SIZE_32M_MAP_512_512:
        case FLASH_SIZE_32M_MAP_1024_1024:
            rf_cal_sec = 1024 - 5;
            break;

        default:
            rf_cal_sec = 0;
            break;
    }

    return rf_cal_sec;
}

void ICACHE_FLASH_ATTR user_rf_pre_init(void)
{
}
