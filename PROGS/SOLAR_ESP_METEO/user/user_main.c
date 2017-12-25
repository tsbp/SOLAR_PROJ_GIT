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
#include "driver/configs.h"
#include "driver/Calculations.h"
#include "driver/UDP_Source.h"
//============================================================================================================================
extern int ets_uart_printf(const char *fmt, ...);
int (*console_printf)(const char *fmt, ...) = ets_uart_printf;

#define LOOP_PERIOD		(100) // in msec
#define user_procTaskPrio        0
#define user_procTaskQueueLen    1

static volatile os_timer_t loop_timer;
static void  loop(os_event_t *events);
uint8 addr = 0x3f;

uint8 out = 0;
unsigned char tmp[6];

sint16 Pitch, Roll, Yaw;

#define VERTICAL_OFFSET	(50)


int manualDuration = PROC_DURATION;
int vertMoveCntr = 10;
struct ip_info ipconfig;
//======================= Main code function ============================================================
void ICACHE_FLASH_ATTR loop(os_event_t *events)
{
	static cntr = 10;

	if (cntr == 0 && flashWriteBit == 1) saveConfigs();

	//if(!sysState.byte) // standby
	{
		if(wifi_station_get_connect_status() == STATION_GOT_IP)
		{

			wifi_get_ip_info(STATION_IF, &ipconfig);
			currentIP = ipconfig.ip.addr;
			//ets_uart_printf(IPSTR, IP2STR(&currentIP));
			blink = BLINK_WAIT;
			UDP_cmdState();
		}
		else														   blink = BLINK_WAIT_UNCONNECTED;
	}
	if(cntr == 0)
	{
		freq = pulseCntr;
		pulseCntr = 0;

		Calculate(
				mState.dateTime.year + 2000,
				mState.dateTime.month,
				mState.dateTime.day,
				mState.dateTime.hour - 2, //- time zone
				mState.dateTime.min,
				mState.dateTime.sec);


//		ets_uart_printf("%d.%d.%d, %d:%d:%d\n", mState.dateTime.year + 2000,
//				mState.dateTime.month,
//				mState.dateTime.day,
//				mState.dateTime.hour - 2, //- time zone
//				mState.dateTime.min,
//				mState.dateTime.sec);

		ets_uart_printf("azim:%d, elev:%d\n", (int)(1000 * azimuth * 57.2958), (int)(1000 * elev * 57.2958));
		mState.azim = (int)(100 * azimuth * 57.2958);
		mState.elev = (int)(100 * elev * 57.2958);
		mState.wind = freq;
	}
	if(cntr)cntr--;
	else cntr = 10 ;

}

//==============================================================================
void ICACHE_FLASH_ATTR setup(void)
{

	indicationInit();

	set_gpio_mode(2, GPIO_PULLUP, GPIO_OUTPUT);
	set_gpio_mode(1, GPIO_PULLUP, GPIO_OUTPUT);


	button_init();
	freq_cntr_init();

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
	os_delay_us(100000);
	ets_uart_printf("System init...\r\n");

//	//saveConfigs();
	readConfigs();
//	checkConfigs();


	wifi_station_disconnect();
	wifi_station_set_auto_connect(0);


	ets_uart_printf("configs.wifi.SSID %s\r\n", configs.wifi.SSID);
	ets_uart_printf("configs.wifi.SSID_PASS %s\r\n", configs.wifi.SSID_PASS);

	if(configs.wifi.mode == STATION_MODE)
		setup_wifi_st_mode();
	else if(configs.wifi.mode == SOFTAP_MODE)
		setup_wifi_ap_mode();

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