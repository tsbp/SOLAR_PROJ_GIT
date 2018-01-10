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
#define HORIZONTAL_OFFSET	(600)


int manualDuration = PROC_DURATION;
int vertMove = 10;
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
	//PCF8574_writeByte(0x3f, ((out++) << 4) | 0x0f);

	//======== BH1715  =====================
	BH1715(I2C_READ, 0x23, 0x01, (unsigned char*)&light, 2);
	light = ((unsigned char*)&light)[1] |
			((unsigned char*)&light)[0] << 8;

	//======== LSM303  =====================
	lsm303(I2C_READ,  LSM303A_I2C_ADDR, LSM303A_OUT_X_L, accel.byte, 6);
	lsm303(I2C_READ,  LSM303M_I2C_ADDR, LSM303M_OUT_X_H, tmp, 6);
	compass.x = ((tmp[0] << 8) | tmp[1]) ;
	compass.z = ((tmp[2] << 8) | tmp[3]) ;
	compass.y = ((tmp[4] << 8) | tmp[5]) ;

	//ets_uart_printf("%d\t%d\t%d\n", compass.x, compass.y, compass.z);

	//ets_uart_printf("x = %d, y = %d, z = %d\r\n", compass.x, compass.y, compass.z);

	getAngles(&accel, &compass, &Pitch, &Roll, &Yaw);
	addValueToArray((sint16)(Roll),  rollArray);
	addValueToArray((sint16)(Pitch), pitchArray);
	addValueToArray((sint16)(Yaw),   yawArray);

	_roll    = mFilter(rollArray,  FILTER_LENGHT);
	_pitch   = mFilter(pitchArray, FILTER_LENGHT);
	_heading = mFilter(yawArray,   FILTER_LENGHT);

	long angle = (_pitch  * 18000 / 31416);

	long head = ( _heading  * 18000 / 31416);
	if(head < 0) head += 36000;
	//ets_uart_printf("angle = %d, elev = %d\r\n", angle, elevation);


	if(sysState.manualMove)
	{
		if(manualDuration) manualDuration--;
		else
		{
			manualDuration = PROC_DURATION;
			azimuth   = (uint16) head;
			elevation = (uint16) angle;
			move(0);
		}
	}

	//=== sun tracking ====================================================
	//ets_uart_printf("sysState = %d, head = %d, angle = %d\r\n", sysState, head, angle);
	{
		//=== horizontal ==============

		if(sysState.newPosition)
		{
			sysState.automaticMoveH = 1;
			if(azimuth > ((uint16) head + HORIZONTAL_OFFSET))		move(RIGHT);
			else if (azimuth < ((uint16) head - HORIZONTAL_OFFSET))	move(LEFT);
		}
		if(sysState.automaticMoveH &&
				(azimuth <= ((uint16) head + HORIZONTAL_OFFSET)) &&
				(azimuth >= ((uint16) head - HORIZONTAL_OFFSET)))
		{
			sysState.automaticMoveH = 0;
			move(0);
		}

		//=== vertical ==============
		if(sysState.newPosition && !sysState.automaticMoveH)
		{
			sysState.automaticMoveV = 1;
			if(elevation > ( angle + VERTICAL_OFFSET))		move(UP);
			else if (elevation < (angle - VERTICAL_OFFSET))	move(DOWN);
		}
		if (sysState.automaticMoveV &&
				(elevation <= (angle + VERTICAL_OFFSET)) &&
				(elevation >= (angle - VERTICAL_OFFSET)))
		{
			sysState.newPosition = 0;
			sysState.automaticMoveV = 0;
			move(0);
		}
	}


}
//==============================================================================
void ICACHE_FLASH_ATTR setup(void)
{



	indicationInit();

	set_gpio_mode(2, GPIO_PULLUP, GPIO_OUTPUT);
	set_gpio_mode(1, GPIO_PULLUP, GPIO_OUTPUT);


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


	i2c_init();
		PCF8574_writeByte(0x3f, 0 | 0x0f);
		move(0);

	uart_init(BIT_RATE_115200, BIT_RATE_115200);
	os_delay_us(1000);
	ets_uart_printf("System init...\r\n");

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
