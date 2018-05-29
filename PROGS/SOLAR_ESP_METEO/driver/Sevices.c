//==============================================================================
#include "ets_sys.h"
#include "osapi.h"
#include "os_type.h"
#include "user_interface.h"
#include "gpio.h"
#include "driver/uart.h"
#include "driver/Configs.h"
#include "driver/UDP_Source.h"
#include "driver/wifi.h"
#include "driver/gpio16.h"
#include "driver/services.h"

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
						configs.meteo.wind = 6;


						configs.wifi.mode = STATION_MODE;
						configs.wifi.auth = AUTH_WPA_WPA2_PSK;
						saveConfigs();
						system_restart();					
					}
				} else {
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
		if     (mState.wind >= configs.meteo.wind)
		{
			mState.stt = ALARM;
			windTimeoutCntr = WIND_TIMEOUT;
		}
		else if((mState.stt != STOPPED) &&
				(mState.wind <= configs.meteo.wind - 2) &&
				(windTimeoutCntr == 0))
		{
			mState.stt = TRACKING;
		}
		windTimeoutCntr--;
	}

	if(mState.stt == ALARM || mState.stt == MANUAL_ALARM) mState.elev = HOME_ELEVATION; // wind to fast
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


