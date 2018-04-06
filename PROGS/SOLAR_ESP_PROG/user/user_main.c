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
#define ELEVATION_MAX		(7500)


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
		terminators = PCF8574_readByte(addr);
		//======== BH1715  =====================
		BH1715(I2C_READ, 0x23, 0x01, (unsigned char*)&light, 2);
		light = ((unsigned char*)&light)[1] |
				((unsigned char*)&light)[0] << 8;

		if(!sysState.manualMoveRemote) keyProcessing((~PCF8574_readByte(0x3B)) & (~0x70));

	//======== LSM303  =====================
	lsm303(I2C_READ,  LSM303A_I2C_ADDR, LSM303A_OUT_X_L, accel.byte, 6);
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
	//_heading = ;

//	long angle = (_pitch  * 18000 / 31416);
//
//	long head = ( _heading  * 18000 / 31416);
//	if(head < 0) head += 36000;

	orientation.real.elevation = ((long)_pitch   * 18000 / 31416);
	orientation.real.azimuth   = ((long)_heading * 18000 / 31416);
	if(orientation.real.azimuth < 0) orientation.real.azimuth += 36000;

	addValueToArray(orientation.real.azimuth,  headFarr);
	headF =  mFilter(headFarr,  FILTER_LENGHT);
	//ets_uart_printf("sysState = %d\r\n", sysState);

	//=====================================================================
//	if(sysState.manualMoveRemote)
//	{
//		if(manualDuration) manualDuration--;
//		else
//		{
//			sysState.byte = 0;
//			manualDuration = PROC_DURATION;
//			orientation.income.azimuth   = (uint16) orientation.real.azimuth;
//			orientation.income.elevation = (uint16) orientation.real.elevation;
//			move(0);
//		}
//	}
//
//	//=== sun tracking ====================================================
//	else
		if(!sysState.manualMove){

		if(orientation.income.elevation > ELEVATION_MAX) orientation.income.elevation = ELEVATION_MAX;

		if(sysState.newPosition)
		{
			//sysState.newPosition = 0;
			if(!direction)
			{
				if      (orientation.income.elevation > ( orientation.real.elevation + 6 * HORIZONTAL_OFFSET)) direction = DOWN;
				else if (orientation.income.elevation < ( orientation.real.elevation - 6 * HORIZONTAL_OFFSET)) direction = UP;
				else if (orientation.income.azimuth   > ( headF + 6 * HORIZONTAL_OFFSET)) direction = RIGHT;
				else if (orientation.income.azimuth   < ( headF - 6 * HORIZONTAL_OFFSET)) direction = LEFT;

				if(direction && !sysState.motorFault)move(direction);
				mTout = 1000;
			}
			if(mTout) mTout--;
			else  stopMoving();
		}
		switch(direction)
				{
					case RIGHT:
					case LEFT:
					{
						if (orientation.income.azimuth <= (orientation.real.azimuth + HORIZONTAL_OFFSET) &&
							orientation.income.azimuth >= (orientation.real.azimuth - HORIZONTAL_OFFSET))
							stopMoving();
					}break;

					case UP:
					case DOWN:
					{
						if (orientation.income.elevation <= (orientation.real.elevation + HORIZONTAL_OFFSET) &&
							orientation.income.elevation >= (orientation.real.elevation - HORIZONTAL_OFFSET))
							stopMoving();
					}break;
				}


	}


	//=====================================================================
//		static c = 50;
//		if(c) c--;
//		else
//		{
//			c = 50;
//			ets_uart_printf("elevation = %d, head = %d, angle = %d\r\n", elevation, head, angle);
//
//		}
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

	ets_uart_printf("configs.wifi.SSID %s\r\n", configs.wifi.SSID);
	ets_uart_printf("configs.wifi.SSID_PASS %s\r\n", configs.wifi.SSID_PASS);

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
