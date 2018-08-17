//==============================================================================
#ifndef INCLUDE_DRIVER_SERVICES_H_
#define INCLUDE_DRIVER_SERVICES_H_
//==============================================================================
#include "c_types.h"
#include "driver/Calculations.h"
//==============================================================================
void ICACHE_FLASH_ATTR service_timer_start (void);
void ICACHE_FLASH_ATTR service_timer_stop (void);
void ICACHE_FLASH_ATTR button_intr_callback(unsigned pin, unsigned level);
void ICACHE_FLASH_ATTR button_init(void);
void ICACHE_FLASH_ATTR timeincrement(void);
void ICACHE_FLASH_ATTR meteoProcessing(void);
//==============================================================================
#define MODE_NORMAL		(0)
#define MODE_SW_RESET	(1)
#define MODE_BTN_RESET	(2)
#define MODE_REMOTE_CONTROL	(3)
#define MODE_FLASH_WRITE	(4)
//==============================================================================
extern uint8	serviceMode;
//extern int cntr;
//==============================================================================
#define BLINK_FORWARD   (0x8040)
#define BLINK_BACKWARD  (0x4080)
#define BLINK_UP        (0xc0ff)
#define BLINK_DOWN      (0xffc0)
#define BLINK_WAIT      (0x9000)
#define BLINK_WAIT_NODATA      (0x90ff)
#define BLINK_WAIT_UNCONNECTED      (0x0090)
#define BLINK_TRACK_STOPPPED        (0x8080)
#define BLINK_FW_UPDATE      (0xaa55)

#define BLNK_MAX 		(15)

void ICACHE_FLASH_ATTR indicationInit(void);
void ICACHE_FLASH_ATTR blinking(unsigned int blnk);
void ICACHE_FLASH_ATTR leds(unsigned char aStt);
void ICACHE_FLASH_ATTR sntp_initialize(void);
void ICACHE_FLASH_ATTR sntp_get_stamp(void);


extern unsigned int blink;
extern uint16 freq, pulseCntr;
extern sint16 windArr[FILTER_LENGHT];
//==============================================================================
#define PROC_DURATION	(50)
extern uint8 terminators, inProcess, otaStarted;
uint16 light;
//==============================================================================
typedef union __attribute__ ((__packed__))
{
	uint8 byte[6];
	struct
	{
		uint8 year;
		uint8 month;
		uint8 day;
		uint8 hour;
		uint8 min;
		uint8 sec;
    };
}s_DATE_TIME;
//extern s_DATE_TIME dateTime;
//=========================================================================================
#define STOPPED         (0)
#define TRACKING        (1)
#define ALARM           (2)
#define MANUAL_ALARM    (3)
#define OTA_STARTED     (4)
//=========================================================================================
typedef union __packed
{
	uint8 byte[15];
	struct
	{
		s_DATE_TIME dateTime;
		sint16 azim;
		sint16 elev;
		uint16 wind;
		uint16 light;
		uint8 stt;
	};
}uMETEO_STATE;
extern uMETEO_STATE mState;
//=========================================================================================
typedef struct
{
	uint16 present;
	uint16 light;
}sLanItem;
extern sLanItem items[256];
//==============================================================================
#define HOME_AZIMUTH	    (3000)
#define HOME_ELEVATION	    (0)
#define MAX_ELEVATION	    (9100)
#define MAX_ELEVATION_SET	(7500)
//==============================================================================
#endif /* INCLUDE_DRIVER_SERVICES_H_ */
