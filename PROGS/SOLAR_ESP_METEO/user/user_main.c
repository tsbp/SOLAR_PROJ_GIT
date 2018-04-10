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
#include "mem.h"
//============================================================================================================================
extern int ets_uart_printf(const char *fmt, ...);
int (*console_printf)(const char *fmt, ...) = ets_uart_printf;

#define LOOP_PERIOD		(100) // in msec
#define user_procTaskPrio        0
#define user_procTaskQueueLen    1

static volatile os_timer_t loop_timer;
static void  loop(os_event_t *events);


struct ip_info ipconfig;


//uint8 windHigh = 0;
//================== SNTP ==================================================
void ICACHE_FLASH_ATTR sntp_initialize(void)
{
	ip_addr_t *addr = (ip_addr_t *)os_zalloc(sizeof(ip_addr_t));
	sntp_setservername(0, "us.pool.ntp.org"); // set server 0 by domain name
	//sntp_setservername(0, "domain.kbp.radiy.com");
	sntp_setservername(1, "ntp.sjtu.edu.cn"); // set server 1 by domain name
	//ipaddr_aton("210.72.145.44", addr);
	ipaddr_aton("10.10.10.2", addr);
	sntp_setserver(2, addr); // set server 2 by IP address
	sntp_init();
	os_free(addr);

	sntp_set_timezone((sint8)configs.meteo.timeZone);
}
//================== SNTP ==================================================
void ICACHE_FLASH_ATTR sntp_get_stamp(void)
{
	sint8 tz  = sntp_get_timezone();
	uint32 ts = sntp_get_current_timestamp();
	//ets_uart_printf("sntp: %d, %s zone = %d\n",ts, sntp_get_real_time(ts), tz);

	uint8 t[24], i;
	os_memset(t, 0, sizeof(t));
	os_sprintf(t, "%s", sntp_get_real_time(ts));

	if(ts != 0)
	{
		mState.dateTime.sec  = (t[17]-'0')*10 + (t[18]-'0');
		mState.dateTime.min  = (t[14]-'0')*10 + (t[15]-'0');
		mState.dateTime.hour = (t[11]-'0')*10 + (t[12]-'0');

		mState.dateTime.day  = (t[8]-'0')*10 + (t[9]-'0');
		mState.dateTime.year  = /*(t[20]-'0')*1000 + (t[21]-'0')*100 + */(t[22]-'0')*10 + (t[23]-'0');

		char mon [12][3]= {
				{'J','a','n'}, {'F','e','b'}, {'M','a','r'},{'A','p','r'}, {'M','a','y'}, {'J','u','n'},
				{'J','u','l'}, {'A','u','g'}, {'S','e','p'},{'O','c','t'}, {'N','o','v'}, {'D','e','c'}};
		for(i = 0; i < 12; i++)
			if(t[4] == mon[i][0] && t[5] == mon[i][1] && t[6] == mon[i][2]) mState.dateTime.month = i+1;
	}

}

//======================= Main code function ============================================================
void ICACHE_FLASH_ATTR loop(os_event_t *events)
{
	static int cntr_b = 10;

	if (cntr_b == 0 && flashWriteBit == 1) saveConfigs();

	//ets_uart_printf("wStat  = %d, cn %d\r\n", wifi_station_get_connect_status(), cntr_b);
	if(cntr_b) // standby
	{
		cntr_b--;

		if(wifi_station_get_connect_status() == STATION_GOT_IP || configs.wifi.mode == SOFTAP_MODE)
		{
			if(configs.wifi.mode == SOFTAP_MODE) wifi_get_ip_info(SOFTAP_IF,  &ipconfig);
			else                                 wifi_get_ip_info(STATION_IF, &ipconfig);
			currentIP = ipconfig.ip.addr;
			//ets_uart_printf(IPSTR, IP2STR(&currentIP));
			if(mState.stt)	blink = BLINK_WAIT;
			else   blink = BLINK_WAIT_NODATA;
			if(cntr_b == 1 && !rtc_get_current_time()) sntp_get_stamp();
			UDP_cmdState();
		}
		else
		{
			blink = BLINK_WAIT_UNCONNECTED;
		}
	}
	else
	{
		cntr_b = 10;
		if(mState.stt == TRACKING)
			Calculate(
				0.01 * configs.meteo.latit,
				0.01 * configs.meteo.longit,
				mState.dateTime.year + 2000,
				mState.dateTime.month,
				mState.dateTime.day,
				mState.dateTime.hour - configs.meteo.timeZone, //- time zone
				mState.dateTime.min,
				mState.dateTime.sec);



//		ets_uart_printf("azim:%d, elev:%d\n", (int)(1000 * azimuth * 57.2958), (int)(1000 * elev * 57.2958));

		//====== check if sun is under horizont ========================
		mState.azim = (int)(100 * azimuth * 57.2958);
		mState.elev = (int)(9000 - 100 * elev * 57.2958);
		if(mState.elev > MAX_ELEVATION)
		{
			mState.azim = HOME_AZIMUTH;
			mState.elev = HOME_ELEVATION;
		}
		//else if(mState.elev > MAX_ELEVATION_SET) mState.elev = MAX_ELEVATION_SET;
		//==============================================================
		meteoProcessing();

		//==============================================================
		if(mState.stt) UDP_Angles();
	}

//	static c = 10;
//	if(c) c--;
//	else
//	{
//		c = 10;
//
//	}


}

//==============================================================================
void ICACHE_FLASH_ATTR setup(void)
{
	indicationInit();

	set_gpio_mode(2, GPIO_PULLUP, GPIO_OUTPUT);
	set_gpio_mode(1, GPIO_PULLUP, GPIO_OUTPUT);

	//======= RTC ==========
	ets_uart_printf("i2c scan\r\n");
	i2c_master_gpio_init();
	i2c_bus_scan();
	//======================

	button_init();
	freq_cntr_init();

	UDP_Init_client();
	sntp_initialize();

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
	ets_uart_printf("flash size %d\r\n", system_get_flash_size_map());
	ets_uart_printf("System init...\r\n");

//	//saveConfigs();
	readConfigs();
//	checkConfigs();


	wifi_station_disconnect();
	wifi_station_set_auto_connect(0);


	ets_uart_printf("configs.wifi.SSID %s\r\n", configs.wifi.SSID);
	ets_uart_printf("configs.wifi.SSID_PASS %s\r\n", configs.wifi.SSID_PASS);

	ets_uart_printf("configs.meteo.light %d\r\n", configs.meteo.light);
	ets_uart_printf("configs.meteo.wind  %d\r\n", configs.meteo.wind);

	configs.wifi.mode = STATION_MODE;
	ets_uart_printf("configs.wifi.mode  %d\r\n", configs.wifi.mode);

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
