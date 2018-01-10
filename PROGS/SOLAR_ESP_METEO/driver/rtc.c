//==============================================================================
#include "driver/rtc.h"
//==============================================================================
//s_LOCAL_TIME localTime;
//=================== Internal Functions ===================
uint8_t RTC_Read(uint8_t address, uint8_t n_bytes, uint8_t *data);
uint8_t RTC_Write(uint8_t address, uint8_t n_bytes, uint8_t *data);
//==============================================================================
void i2c_bus_scan()
{
    int index = 0;
    uint8_t result;

    ets_uart_printf("Scanning I2C bus for devices\r\n");
    //for (index=1; index <=127; index ++)
    {
        i2c_master_start();                     // Start I2C request

        i2c_master_writeByte(index<<1| 0b00000001);     //DS1307 address + W
        result = i2c_master_checkAck();

        if (result)
        {
        	ets_uart_printf("\tI2C device present at 0x%x\r\n", index);
        }

        i2c_master_stop();

    }
    ets_uart_printf("Scanning I2C bus for devices done\r\n");

}
//======================================================================================
char *convert_epoc_to_str(uint32_t current_stamp)
{
    char *str = (char *)sntp_get_real_time(current_stamp);
    *(str+strlen(str)-1) = 0;
    return (str);
}
//=================== getDayOfWeek =====================================================
int getDayOfWeek(int aYear, int aMonth, int aDay)
{
	int adjustment, mm, yy;

		adjustment = (14 - aMonth) / 12;
		mm = aMonth + 12 * adjustment - 2;
		yy = aYear - adjustment;
		return (aDay + (13 * mm - 1) / 5 +
			yy + yy / 4 - yy / 100 + yy / 400) % 7;
}
//=================== Returns the current time from EPOC (using RTC) ===================
void rtc_get_current_time()
{
    uint8_t     rtc_bytes[8];
    uint8_t     bytes_read;
    //uint32_t    timestamp = 0;
    //struct tm   ts;

    //os_printf("Corrected time: %d:%d:%d %d/%d/%d\r\n", ts->tm_hour, ts->tm_min, ts->tm_sec, ts->tm_mday, ts->tm_mon, ts->tm_year);
    os_memset(rtc_bytes, 0, 7);
    bytes_read = RTC_Read(0x00, 7, rtc_bytes);
    //if (bytes_read < 7) return timestamp;

    /* unload contents in to the TS structure */
    mState.dateTime.sec  = (((rtc_bytes[0] & 0b01110000)>>4)*10)+(rtc_bytes[0] & 0b00001111);
    mState.dateTime.min  = (((rtc_bytes[1] & 0b01110000)>>4)*10)+(rtc_bytes[1] & 0b00001111);


    /* AM/PM Logic not present */
    mState.dateTime.hour = (((rtc_bytes[2] & 0b00110000)>>4)*10)+(rtc_bytes[2] & 0b00001111);

    mState.dateTime.day = (((rtc_bytes[4] & 0b00110000)>>4) *10)+ (rtc_bytes[4] & 0b00001111);
    mState.dateTime.month  = (((rtc_bytes[5] & 0b00110000)>>4) *10)+ (rtc_bytes[5] & 0b00001111);


    mState.dateTime.year = (((rtc_bytes[6] & 0b11110000)>>4) *10)+ (rtc_bytes[6] & 0b00001111);


}
//=============================================================================================
void rtc_set_time(s_DATE_TIME * aTime)
{
    //struct tm   *ts;
    uint8_t     rtc_bytes[8];
    uint16_t     year;
    uint8_t     bytes_written;

    rtc_bytes[0] = ((aTime->sec/10)<<4)|(aTime->sec%10);
    rtc_bytes[1] = ((aTime->min/10)<<4)|(aTime->min%10);

    /* AM/PM Logic not present */
    rtc_bytes[2] = ((aTime->hour/10)<<4)|(aTime->hour%10);
    rtc_bytes[3] = 0;//getDayOfWeek(aTime->year, aTime->month, aTime->day);
    rtc_bytes[4] = ((aTime->day/10)<<4)|(aTime->day%10);

    //ts->tm_mon ++;
    rtc_bytes[5] = ((aTime->month/10)<<4)|(aTime->month%10);
    year = aTime->year;// - 2000;
    rtc_bytes[6] = ((year/10)<<4)|(year%10);

    bytes_written = RTC_Write(0x00, 7, rtc_bytes);
    //return 1;
}




/*----------------------------------------------------------------------------*/
/*                        Internal Functions                                  */
/*----------------------------------------------------------------------------*/


/* ---------------------------------------------------------------------------
    Internal Function To Read Internal Registers of DS1307

    address     [IN] : Address of the register (refer datasheet)
    data        [OUT]: Value of register is copied to this.

    Returns:
        0 = Failure
        n = Success (bytes read)
 --------------------------------------------------------------------------- */
uint8_t RTC_Read(uint8_t address, uint8_t n_bytes, uint8_t *data)
{
    uint8_t result;
    uint8_t i;
    uint8_t  *tmp = data;
    i2c_master_start();                     // Start I2C request

    i2c_master_writeByte(RTC_WRITE_BYTE);     //DS1307 address + W
    result = i2c_master_checkAck();
    if (!result) return 0;

    //Now send the address of required register
    i2c_master_writeByte(address);
    result = i2c_master_checkAck();
    if (!result) return 0;

    i2c_master_start();

    i2c_master_writeByte(RTC_READ_BYTE);    //DS1307 Address + R
    result = i2c_master_checkAck();
    if (!result) return 0;

    //Now read the value with NACK
    for(i=0; i <= n_bytes; i++, tmp++)
    {
        *(tmp) = i2c_master_readByte();


        if (i < n_bytes)
        {
            i2c_master_send_ack();
        }
        else
        {
            i2c_master_send_nack();
        }
    }

    i2c_master_stop();

    return i;
}

/* ---------------------------------------------------------------------------
    Internal Function To write Internal Registers of DS1307

    address     [IN] : Address of the register (refer datasheet)
    data        [IN]: Value of register is copied to this.

    Returns:
        0 = Failure
        n = Success (bytes written)
 --------------------------------------------------------------------------- */
uint8_t RTC_Write(uint8_t address, uint8_t n_bytes, uint8_t *data)
{
    uint8_t result;
    uint8_t i;
    i2c_master_start();                     // Start I2C request

    i2c_master_writeByte(RTC_WRITE_BYTE);     //DS1307 address + W
    result = i2c_master_checkAck();
    if (!result) return 0;

    //Now send the address of required register
    i2c_master_writeByte(address);
    result = i2c_master_checkAck();
    if (!result) return 0;

    //Now read the value with NACK
    for(i=0; i<n_bytes; i++)
    {
        i2c_master_writeByte(*(data+i));
        result = i2c_master_checkAck();
        if (!result) break;
    }

    i2c_master_stop();

    return i;
}
