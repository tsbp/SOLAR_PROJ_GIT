#ifndef _RTC_H_
#define _RTC_H_

#include "ets_sys.h"
#include "osapi.h"
#include "gpio.h"
#include "driver/services.h"

#include "driver/i2c_master.h"
//=================================================================================
#define I2C_RTC_ADDRESS 0x68

/* The following bytes are used in very transaction, hence they are
   preformed for code optimization. The I2C specifies the 1st byte is
   device address [7 bits] followed by a READ/~WRITE [1 bit]
 */
#define RTC_READ_BYTE   (I2C_RTC_ADDRESS<<1 | 0x01)
#define RTC_WRITE_BYTE  (I2C_RTC_ADDRESS<<1)

void i2c_bus_scan();
void rtc_set_time(s_DATE_TIME *aTime);
void rtc_get_current_time();
int getDayOfWeek(int aYear, int aMonth, int aDay);

#endif
