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
void ICACHE_FLASH_ATTR sendToTingspeak(void);
void ICACHE_FLASH_ATTR keyProcessing(uint8 dd);
//==============================================================================
#define MODE_NORMAL		(0)
#define MODE_SW_RESET	(1)
#define MODE_BTN_RESET	(2)
#define MODE_REMOTE_SEND	(3)
#define MODE_FLASH_WRITE	(4)
//==============================================================================
#define LEFT	(2)
#define RIGHT	(3)
#define UP		(8)
#define DOWN	(12)
//==============================================================================
extern uint8	serviceMode;
extern int cntr;
//==============================================================================
#define BLINK_FORWARD   (0x8040)
#define BLINK_BACKWARD  (0x4080)
#define BLINK_UP        (0xc0ff)
#define BLINK_DOWN      (0xffc0)
#define BLINK_WAIT      (0x9000)
#define BLINK_WAIT_NODATA      (0x9090)
#define BLINK_WAIT_UNCONNECTED      (0x0090)
#define BLINK_MANUAL      (0xf00f)
#define BLINK_MOTOR_FLT      (0x00aa)

#define BLNK_MAX 		(15)

void ICACHE_FLASH_ATTR indicationInit(void);
void ICACHE_FLASH_ATTR blinking(unsigned int blnk);
void ICACHE_FLASH_ATTR leds(unsigned char aStt);
void ICACHE_FLASH_ATTR move(uint8 a);
void ICACHE_FLASH_ATTR modeSwitch(void);

extern uint16 dataPresent;
extern unsigned int blink;
//extern sint16 azimuth, elevation;
extern uint8 direction;
//==============================================================================
#define PROC_DURATION	(120)
extern uint8 terminators, inProcess;
uint16 light;
//==============================================================================
typedef union
{
	uint8 byte;
	struct
	{
		uint8 manualMoveRemote :1;
		uint8 manualMove	    :1;
		uint8 moving       	:1;
		uint8 automaticMoveV	:1;
		uint8 newPosition  	:1;
		uint8 motorFault	    :1;
	};
}sSYSTEM_STATE;
extern sSYSTEM_STATE sysState;
//==============================================================================
typedef union
{
	sint16 byte[4];
	struct
	{
		sint16 azimuth;
		sint16 elevation;
	};
}uORIENT;
//==============================================================================
typedef union
{
	sint16 byte[8];
	struct
	{
		uORIENT income;
		uORIENT real;
	};
}uORIENTATION;
extern uORIENTATION orientation;
//==============================================================================
#endif /* INCLUDE_DRIVER_SERVICES_H_ */
