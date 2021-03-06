//==============================================================================
#include "ets_sys.h"
#include "osapi.h"
#include "os_type.h"
#include "user_interface.h"
#include "user_config.h"
#include "gpio.h"
#include "driver/uart.h"
#include "driver/Configs.h"
#include "driver/UDP_Source.h"
#include "driver/wifi.h"
#include "driver/gpio16.h"
#include "driver/services.h"
#include "driver/httpclient.h"
//==============================================================================

static volatile os_timer_t service_timer;
static void  service_timer_cb(os_event_t *events);
uint8_t factory_reset_pin = 3, freqPin = 7;
uint8	serviceMode = MODE_NORMAL;

uint16 light;
uint8 terminators;
uMETEO_STATE mState = {.stt = TRACKING};

//int cntr = 5;
uint16 freq, pulseCntr = 0;
sint16 windArr[FILTER_LENGHT];

sLanItem items[256];
uint8 otaStarted = 0;
//==============================================================================
void ICACHE_FLASH_ATTR service_timer_start (void)
{
	 //SELECT command timer
		os_timer_disarm(&service_timer);
		os_timer_setfn(&service_timer, (os_timer_func_t *) service_timer_cb, NULL);
		os_timer_arm(&service_timer, 1000, true);
}
//==============================================================================
void ICACHE_FLASH_ATTR service_timer_stop (void)
{
	//SELECT command timer
	os_timer_disarm(&service_timer);
}
//======================= GPIO interrupt callback =======================================================
//extern uint8_t pin_num[GPIO_PIN_NUM];
int resetCntr = 0;
int8 scrOrientation = 0;
//=======================
void ICACHE_FLASH_ATTR button_intr_callback(unsigned pin, unsigned level)
{
	if(pin == factory_reset_pin)
	{
			ets_uart_printf("RESET BUTTON PRESSED!!!\r\n");
			serviceMode = MODE_BTN_RESET;
			resetCntr = 0;
			service_timer_start();
	}
	else if (pin == freqPin && level == 0) pulseCntr++;
}
//======================= GPIO init function ============================================================
void ICACHE_FLASH_ATTR button_init(void)
{
	GPIO_INT_TYPE gpio_type;
	gpio_type = GPIO_PIN_INTR_NEGEDGE;
	if (set_gpio_mode(factory_reset_pin, GPIO_FLOAT, GPIO_INT))
		if (gpio_intr_init(factory_reset_pin, gpio_type))  gpio_intr_attach(button_intr_callback);
}
//======================= GPIO init function ============================================================
void ICACHE_FLASH_ATTR freq_cntr_init(void)
{
	GPIO_INT_TYPE gpio_type;
	gpio_type = GPIO_PIN_INTR_NEGEDGE;
	if (set_gpio_mode(freqPin, GPIO_PULLDOWN, GPIO_INT))
		if (gpio_intr_init(freqPin, gpio_type))  gpio_intr_attach(button_intr_callback);
}
//==============================================================================
static void ICACHE_FLASH_ATTR service_timer_cb(os_event_t *events) {


	switch (serviceMode)
	{
	    case MODE_REMOTE_CONTROL:
	    	resetCntr++;
	    	if (resetCntr > 5)
	    	{
	    		service_timer_stop();
	    		resetCntr = 0;
	    		serviceMode = MODE_NORMAL;
	    		channelFree = 1;
	    	}
	    	break;

		case MODE_BTN_RESET:
				if (gpio_read(factory_reset_pin) == 0)
				{
					resetCntr++;
					if (resetCntr >= 10)
					{
						os_printf("do reset \r\n");

						os_memset(configs.wifi.SSID, 0, sizeof(configs.wifi.SSID));
						os_sprintf(configs.wifi.SSID, "%s", "Solar");

						os_memset(configs.wifi.SSID_PASS, 0, sizeof(configs.wifi.SSID_PASS));
						os_sprintf(configs.wifi.SSID_PASS, "%s", "123454321");

						configs.meteo.latit=4850;
						configs.meteo.longit=3223;
						configs.meteo.timeZone = 3;
						configs.meteo.windLow = 6;
						configs.meteo.windHigh = 8;


						configs.wifi.mode = STATION_MODE;
						configs.wifi.auth = AUTH_WPA_WPA2_PSK;
						saveConfigs();
						system_restart();					
					}
				} else {
					if(resetCntr >= 4)
										{
											ets_uart_printf("OTA start\r\n");
											blink = BLINK_FW_UPDATE;
											otaStarted = 1;
											ota_start();
										}
					service_timer_stop();
					resetCntr = 0;
					serviceMode = MODE_NORMAL;
				}

				break;

		case MODE_SW_RESET:
			resetCntr++;
			if (resetCntr >= 3)
				system_restart();
			break;

		case MODE_FLASH_WRITE:
			resetCntr++;
			if (resetCntr > 0)
				system_restart();
			break;
	}

}
//========================= indication =========================================

#define IND_LOOP_PERIOD		(10) // in msec
static volatile os_timer_t indicator_timer;
static void  indicator_loop(os_event_t *events);


unsigned char ledStt = 0;
unsigned int blink = BLINK_WAIT;
//==============================================================================
void ICACHE_FLASH_ATTR indicationInit()
{
	os_timer_disarm(&indicator_timer);
			os_timer_setfn(&indicator_timer, (os_timer_func_t *) indicator_loop, NULL);
			os_timer_arm(&indicator_timer, IND_LOOP_PERIOD, true);
}
//==============================================================================
void ICACHE_FLASH_ATTR leds(unsigned char aStt)
{
  if(!aStt)
	  {
	  	  gpio_write(1, 0);//P1DIR &= ~BIT4;
	  	  gpio_write(2, 0);
	  }
  else
  {
    if (aStt == 0x03)       {gpio_write(1, 1); gpio_write(2, 1);}
    else if(aStt & BIT0)    {gpio_write(1, 1); gpio_write(2, 0);}
    else                    {gpio_write(1, 0); gpio_write(2, 1);}
  }
}
//==============================================================================
void ICACHE_FLASH_ATTR blinking(unsigned int blnk)
{
  static unsigned int bStage, bCntr = BLNK_MAX;

  if(bCntr) bCntr--;
  else
  {
    bCntr = BLNK_MAX;
    if(blnk & (1 << bStage)) ledStt |= BIT0;
    else                     ledStt &= ~BIT0;

    if((blnk >> 8) & (1 << bStage)) ledStt |= BIT1;
    else                            ledStt &= ~BIT1;

    bStage++;
    bStage %= 8;
  }
}
//======================= Main code function ============================================================
void ICACHE_FLASH_ATTR indicator_loop(os_event_t *events)
{
	blinking(blink);
	leds(ledStt);
}
//==============================================================================
uint8 daysInMonth[13] = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 30};
uint8 leapYear;
//==============================================================================
void ICACHE_FLASH_ATTR timeincrement(void)
{
	if((mState.dateTime.year % 100 != 0 && mState.dateTime.year % 4 == 0) || mState.dateTime.year % 400 == 0) daysInMonth[2] = 29;
	else daysInMonth[2] = 28;

	mState.dateTime.sec++;
	if(mState.dateTime.sec >= 60)
	{
		mState.dateTime.sec = 0;
		mState.dateTime.min++;
		if(mState.dateTime.min >= 60)
		{
			mState.dateTime.min = 0;
			mState.dateTime.hour++;
			if(mState.dateTime.hour >= 24)
			{
				mState.dateTime.hour = 0;
				mState.dateTime.day++;
				if(mState.dateTime.day > daysInMonth[mState.dateTime.month])
				{
					mState.dateTime.day = 1;
					mState.dateTime.month++;
					if(mState.dateTime.month > 12)
					{
						mState.dateTime.month = 0;
						mState.dateTime.year++;
					}
				}
			}
		}
	}
}
//==============================================================================
#define WIND_TIMEOUT	(300)//seconds
uint16 windTimeoutCntr = WIND_TIMEOUT;
//==============================================================================
void ICACHE_FLASH_ATTR meteoProcessing(void)
{
	//========= wind ==========
	//cntr = 10;
	freq = pulseCntr;
	pulseCntr = 0;
	addValueToArray((sint16)freq, windArr); //[];
	mState.wind = mFilter(windArr, FILTER_LENGHT);



	if(mState.stt != MANUAL_ALARM )
	{


		if(mState.stt != STOPPED) {
			if(windTimeoutCntr) windTimeoutCntr--;
			else
			{
				if(mState.stt == ALARM && (mState.wind <= configs.meteo.windHigh - 2))
				{
					mState.stt = ATTENTION;
					windTimeoutCntr = WIND_TIMEOUT;
				}
				else if ((mState.stt == ATTENTION && (mState.wind <= configs.meteo.windLow - 2)))
				{
					mState.stt = TRACKING;
				}
			}

			//------------------------------------------
			if ( mState.stt != ALARM && mState.wind >= configs.meteo.windLow) {
				mState.stt = ATTENTION;
				windTimeoutCntr = WIND_TIMEOUT;
			}

			if(mState.wind >= configs.meteo.windHigh) {
				mState.stt = ALARM;
				windTimeoutCntr = WIND_TIMEOUT;
			}
		}


	}

	if(mState.stt == ALARM || mState.stt == MANUAL_ALARM) // wind to fast then parking to south
	{
		//mState.elev = 100 * configs.angles.vert_min;//HOME_ELEVATION;

		mState.azim = 100 * 180;//configs.angles.horiz_min;//HOME_AZIMUTH;
		mState.elev = 100 * 10;//configs.angles.vert_min; //HOME_ELEVATION;
	}
	else if (mState.stt == ATTENTION) mState.elev = 100 * 10;//configs.angles.vert_min; //HOME_ELEVATION;
	//========= Light ==========
	int i, cntr_a = 0;
	long light = 0;

	for(i = 0; i < 256; i++)
		if(items[i].present)
		{
			ets_uart_printf("");
			//ets_uart_printf("pres at addr %d  light %d\r\n", i, items[i].light);
			light += items[i].light;
			cntr_a++;
		}

	if(cntr_a) mState.light = light / cntr_a;
	else mState.light = 0;
	//ets_uart_printf("light %d cntr %d\r\n", mState.light, cntr);
}
//==============================================================================
//                              OPEN WEATHER
//==============================================================================
//float stof(const char* s){
//	int point_seen;
//  float rez = 0, fact = 1;
//  if (*s == '-'){
//    s++;
//    fact = -1;
//  };
//  for (point_seen = 0; *s; s++){
//    if (*s == '.'){
//      point_seen = 1;
//      continue;
//    };
//    int d = *s - '0';
//    if (d >= 0 && d <= 9){
//      if (point_seen) fact /= 10.0f;
//      rez = rez * 10.0f + (float)d;
//    };
//  };
//  return rez * fact;
//};
float str2float (const char * str) {
  unsigned char abc;
  float ret = 0, fac = 1;
  for (abc = 9; abc & 1; str++) {
    abc  =  *str == '-' ?
              (abc & 6 ? abc & 14 : (abc & 47) | 36)
            : *str == '+' ?
              (abc & 6 ? abc & 14 : (abc & 15) | 4)
            : *str > 47 && *str < 58 ?
              abc | 18
            : (abc & 8) && *str == '.' ?
              (abc & 39) | 2
            : !(abc & 2) && (*str == ' ' || *str == '\t') ?
              (abc & 47) | 1
            :
              abc & 46;
    if (abc & 16) {
      ret = abc & 8 ? *str - 48 + ret * 10 : (*str - 48) / (fac *= 10) + ret;
    }
  }
  return abc & 32 ? -ret : ret;
}
//==============================================================================
void ICACHE_FLASH_ATTR openWeather_http_callback(char * response, int http_status, char * full_response)
{
	char data[20];
	int i = 0;
	ets_uart_printf("Answers: \r\n");
	if (http_status == 200)
	{

//		ets_uart_printf("response=%s<EOF>\n", response);
//		ets_uart_printf("---------------------------\r\n");

		char* a = strstr(response, "name");
		for (i = 0; i < 1024; i++)
			if(response[a - response + 7 + i] == ',' ||	response[a - response + 7 + i] == '"') break;
		response[a - response + 7 + i] = 0x00;
		//ets_uart_printf("%s_", a + 7);
		os_sprintf(mState.forecast.region, a + 7);

		a = strstr(response, "speed");
		for (i = 0; i < 1024; i++)
			if(response[a - response + 7 + i] == ',' ||	response[a - response + 7 + i] == '"') break;
		response[a - response + 7 + i] = 0x00;
		//ets_uart_printf("%s_", a + 7);
		os_sprintf(data, "%s", a + 7);
		mState.forecast.wind = (int)(str2float(data) * 100);

		a = strstr(response, "humidity");
		for (i = 0; i < 1024; i++)
			if(response[a - response + 10 + i] == ',' ||	response[a - response + 10 + i] == '"') break;
		response[a - response + 10 + i] = 0x00;
		//ets_uart_printf("%s_", a + 10);
		os_sprintf(data, "%s", a + 10);
		mState.forecast.humid = (int)(str2float(data) * 100);

		a = strstr(response, "temp");
		for (i = 0; i < 1024; i++)
			if(response[a - response + 6 + i] == ',' ||	response[a - response + 6 + i] == '"') break;
		response[a - response + 6 + i] = 0x00;
		//ets_uart_printf("%s_", a + 6);
		os_sprintf(data, "%s", a + 6);
		mState.forecast.temp = (int)(str2float(data) * 100);


		a = strstr(response, "icon");
		for (i = 0; i < 1024; i++)
			if(response[a - response + 7 + i] == ',' ||	response[a - response + 7 + i] == '"') break;
		response[a - response + 7 + i] = 0x00;
		//ets_uart_printf("%s_", a + 7);
		os_sprintf(mState.forecast.icon, a + 7);


		ets_uart_printf("%s, temp = %d, wind = %d, hum = %d, icon(%s)\n",
				mState.forecast.region, mState.forecast.temp, mState.forecast.wind, mState.forecast.humid, mState.forecast.icon);

		ets_uart_printf("---------------------------\r\n");
	}

	ets_uart_printf("Free heap size = %d\r\n", system_get_free_heap_size());
}
//==============================================================================
void ICACHE_FLASH_ATTR openWeather_request(void)
{
	static char data[256];
	static char lat[10];
	static char lon[10];

//	0.01 * configs.meteo.latit,
//	0.01 * configs.meteo.longit,

	os_sprintf(lat, "%d.%d%d", configs.meteo.latit  / 100, (configs.meteo.latit  % 100) / 10, (configs.meteo.latit  % 100) % 10);
	os_sprintf(lon, "%d.%d%d", configs.meteo.longit / 100, (configs.meteo.longit % 100) / 10, (configs.meteo.longit % 100) % 10);

	os_sprintf(data, "http://%s/data/2.5/weather?lat=%s&lon=%s&units=metric&appid=%s", OPENWEATHER_SERVER, lat, lon, OPENWEATHER_API_KEY);
	ets_uart_printf("Request: %s\r\n", data);
	http_get(data, "", openWeather_http_callback);
}
//===================================================================================
// thingspeak
//===================================================================================
void ICACHE_FLASH_ATTR thingspeak_http_callback(char * response, int http_status, char * full_response)
{
	ets_uart_printf("Answers: \r\n");
	if (http_status == 200)
	{
		ets_uart_printf("response=%s<EOF>\n", response);
		ets_uart_printf("---------------------------\r\n");
	}
	ets_uart_printf("Free heap size = %d\r\n", system_get_free_heap_size());
}
//==============================================================================
uint16 thingspeakField = 0;
//==============================================================================
void ICACHE_FLASH_ATTR sendToTingspeak(void)
{
	static char data[256];
	os_sprintf(data, "http://%s/update?api_key=%s&field4=%d", THINGSPEAK_SERVER, THINGSPEAK_API_KEY3, mState.wind);
	ets_uart_printf("Request: %s\r\n", data);
	http_get(data, "", thingspeak_http_callback);
}



