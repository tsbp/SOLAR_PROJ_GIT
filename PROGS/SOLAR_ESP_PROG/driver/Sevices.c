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
uint8_t factory_reset_pin = 3;
uint8	serviceMode = MODE_NORMAL;
int cntr = 5;

uint16 dataPresent = 0;
uint16 light;
uint8 terminators; //inProcess = 0;
sSYSTEM_STATE sysState;

//sint16 azimuth, elevation;
uint8 direction = 0;
uORIENTATION orientation;
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
	ets_uart_printf("RESET BUTTON PRESSED!!!\r\n");
	serviceMode = MODE_BTN_RESET;
		resetCntr = 0;
		service_timer_start();
}
//======================= GPIO init function ============================================================
void ICACHE_FLASH_ATTR button_init(void)
{
	GPIO_INT_TYPE gpio_type;
	gpio_type = GPIO_PIN_INTR_NEGEDGE;
	if (set_gpio_mode(factory_reset_pin, GPIO_FLOAT, GPIO_INT))
		if (gpio_intr_init(factory_reset_pin, gpio_type))  gpio_intr_attach(button_intr_callback);
}

//==============================================================================
static void ICACHE_FLASH_ATTR service_timer_cb(os_event_t *events) {


	switch (serviceMode)
	{
	    case MODE_REMOTE_SEND:
	    	resetCntr++;
	    	if (resetCntr > 5)
	    	{
	    		sendToTingspeak();
	    		service_timer_stop();
	    		resetCntr = 0;
	    		serviceMode = MODE_NORMAL;
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

						configs.wifi.mode = STATION_MODE;
						configs.wifi.auth = AUTH_WPA_WPA2_PSK;
						saveConfigs();
						system_restart();					
					}
				} else {
					if(resetCntr >= 4)
					{
						ets_uart_printf("OTA start\r\n");
						blink = BLINK_MOTOR_FLT;
						sysState.motorFault = 1;
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
//===================================================================================
void ICACHE_FLASH_ATTR thingspeak_http_callback(char * response, int http_status, char * full_response)
{
	ets_uart_printf("Answers: \r\n");
	if (http_status == 200)
	{
		//DHT22_DEBUG("strlen(response)=%d\r\n", strlen(response));
		//DHT22_DEBUG("strlen(full_response)=%d\r\n", strlen(full_response));
		ets_uart_printf("response=%s<EOF>\n", response);
		//DHT22_DEBUG("full_response=%s\r\n", full_response);
		ets_uart_printf("---------------------------\r\n");
	}
//	else
//	{
//		ets_uart_printf("http_status=%d\r\n", http_status);
//		DHT22_DEBUG("strlen(response)=%d\r\n", strlen(response));
//		DHT22_DEBUG("strlen(full_response)=%d\r\n", strlen(full_response));
//		DHT22_DEBUG("response=%s<EOF>\n", response);
//		DHT22_DEBUG("---------------------------\r\n");
//	}
	ets_uart_printf("Free heap size = %d\r\n", system_get_free_heap_size());
}
//==============================================================================
void ICACHE_FLASH_ATTR sendToTingspeak(void)
{
	static char data[256];
	static char azi[10], eli[10], azr[10], elr[10], li[10];

	os_sprintf(azi, "%d.%d", orientation.income.azimuth/100,   orientation.income.azimuth%100);
	os_sprintf(eli, "%d.%d", orientation.income.elevation/100, orientation.income.elevation%100);
	os_sprintf(azr, "%d.%d", orientation.real.azimuth/100,     orientation.real.azimuth%100);
	os_sprintf(elr, "%d.%d", orientation.real.elevation/100,   orientation.real.elevation%100);
	os_sprintf(li,  "%d   ", light);
				//ets_uart_printf("az %s, el %s\r\n", az, el);

	os_sprintf(data, "http://%s/update?api_key=%s&field1=%s&field2=%s&field3=%s&field4=%s&field5=%s",
			THINGSPEAK_SERVER, THINGSPEAK_API_KEY, azi, eli, azr, elr, li);
	ets_uart_printf("Request: %s\r\n", data);
	http_get(data, "", thingspeak_http_callback);
}
//==============================================================================
void ICACHE_FLASH_ATTR move(uint8 a)
{
	static sint16 azOld = 0, elOld = 0;

	PCF8574_writeByte(0x3f, ((~a) << 4) | 0x0f);
//	if(a)
//	{
//		azOld = orientation.real.azimuth;
//		elOld = orientation.real.elevation;
//	}
	switch(a)
	{
		case LEFT:
			ets_uart_printf("LEFT\r\n");
			azOld = orientation.real.azimuth;
			blink = BLINK_BACKWARD;
			break;
		case RIGHT:
			ets_uart_printf("RIGHT\r\n");
			azOld = orientation.real.azimuth;
			blink = BLINK_FORWARD;
			break;
		case UP:
			blink = BLINK_UP;
			elOld = orientation.real.elevation;
			ets_uart_printf("UP\r\n");
			 break;
		case DOWN:
			ets_uart_printf("DOWN\r\n");
			elOld = orientation.real.elevation;
			blink = BLINK_DOWN;
			break;
		default:
		{
			ets_uart_printf("stop\r\n");

			ets_uart_printf("%d,  %d\r\n%d,  %d\r\n",
					orientation.real.azimuth, azOld, orientation.real.elevation, elOld);
			//==================================
			serviceMode = MODE_REMOTE_SEND;
			resetCntr = 0;
			service_timer_start();

			// check if moving
			if((orientation.real.azimuth    < (azOld + 200) && orientation.real.azimuth    > (azOld - 200)) &&
			   (orientation.real.elevation  < (elOld + 200) && orientation.real.elevation  > (elOld - 200)) )
			{
				// not moving
				sysState.motorFault = 1;
				blink = BLINK_MOTOR_FLT;
			}
		}
	}
}
//==============================================================================
void ICACHE_FLASH_ATTR modeSwitch(void)
{
						if(sysState.manualMove)
						{
							sysState.byte = 0;
							PCF8574_writeByte(0x3B, (0x00 << 4) | 0x8f);
							direction = 0;
							stopMoving();
						}
						else
						{
							PCF8574_writeByte(0x3B, (0x01 << 4) | 0x8f);
							sysState.manualMove = 1;
							blink = BLINK_MANUAL;
							//stopMoving();
						}
}
//==============================================================================
void ICACHE_FLASH_ATTR keyProcessing(uint8 dd)
{
	static uint8 ddOld;
			//uint8  dd = (~PCF8574_readByte(0x3B)) & (~0x70);

			if (ddOld != dd)
			{
				//ets_uart_printf("%d, %d\r\n", dd, ddOld);
				switch(dd)
				{
					case 0x80:
						if(sysState.manualMove)
						{
							move(UP);
							PCF8574_writeByte(0x3B, (0x03 << 4) | 0x8f);
						}
						break;
					case 0x08:
						if(sysState.manualMove)
						{
							PCF8574_writeByte(0x3B, (0x03 << 4) | 0x8f);
							move(DOWN);
						}
						break;

					case 0x04:
						if(sysState.manualMove)
						{
							PCF8574_writeByte(0x3B, (0x03 << 4) | 0x8f);
							move(LEFT);
						}
						break;
					case 0x02:
						if(sysState.manualMove)
						{
							PCF8574_writeByte(0x3B, (0x03 << 4) | 0x8f);
							move(RIGHT);
						}
						break;
					case 0x01:
						ets_uart_printf("MODE\r\n");
						modeSwitch();
						break;
					default:
						if(sysState.manualMove)
						{
							PCF8574_writeByte(0x3B, (0x01 << 4) | 0x8f);
							stopMoving();
							blink = BLINK_MANUAL;
						}
				}
			}
			ddOld = dd;
}
//==============================================================================

