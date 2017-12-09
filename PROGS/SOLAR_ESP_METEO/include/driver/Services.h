/*
 * Services.h
 *
 *  Created on: 16 трав. 2016
 *      Author: Voodoo
 */

#ifndef INCLUDE_DRIVER_SERVICES_H_
#define INCLUDE_DRIVER_SERVICES_H_
//==============================================================================

void ICACHE_FLASH_ATTR service_timer_start (void);
void ICACHE_FLASH_ATTR service_timer_stop (void);
void ICACHE_FLASH_ATTR button_intr_callback(unsigned pin, unsigned level);
void ICACHE_FLASH_ATTR button_init(void);

//==============================================================================
#define MODE_NORMAL		(0)
#define MODE_SW_RESET	(1)
#define MODE_BTN_RESET	(2)
#define MODE_REMOTE_CONTROL	(3)
#define MODE_FLASH_WRITE	(4)
//==============================================================================
extern uint8	serviceMode;
extern int cntr;
//==============================================================================
#define BLINK_FORWARD   (0x8040)
#define BLINK_BACKWARD  (0x4080)
#define BLINK_UP        (0xc0ff)
#define BLINK_DOWN      (0xffc0)
#define BLINK_WAIT      (0x9000)
#define BLINK_WAIT_UNCONNECTED      (0x0090)

#define BLNK_MAX 		(15)

void ICACHE_FLASH_ATTR indicationInit(void);
void ICACHE_FLASH_ATTR blinking(unsigned int blnk);
void ICACHE_FLASH_ATTR leds(unsigned char aStt);
void ICACHE_FLASH_ATTR move(uint8 a);

extern unsigned int blink;
extern uint16 freq, pulseCntr;
//==============================================================================
#define PROC_DURATION	(50)
extern uint8 terminators, inProcess;
uint16 light;
//==============================================================================
typedef union
{
	uint16 byte;
	struct
	{
		uint16 manualMove	    :1;
		uint16 automaticMoveH	:1;
		uint16 automaticMoveV	:1;
		uint16 goHome		    :1;
	};
}sSYSTEM_STATE;
extern sSYSTEM_STATE sysState;
//==============================================================================
#endif /* INCLUDE_DRIVER_SERVICES_H_ */
