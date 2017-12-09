//=============================================================================
#include <ets_sys.h>
#include "osapi.h"
#include "c_types.h"
#include "gpio.h"
#include "os_type.h"
#include <mem.h>
#include "driver/wifi.h"
#include "user_config.h"
#include "user_interface.h"
#include "driver/Configs.h"
#include "driver/services.h"
//==============================================================================
uint8 flashWriteBit;
uint8 periphWord = 0x00;
uint8 timeTrue = 0;

//=============================================================================
u_CONFIG configs = {
	    .wifi.mode = SOFTAP_MODE,
		.wifi.SSID = "Solar",
        .wifi.SSID_PASS = "123454321"
};
//=============================================================================
uint16 getMinutes(uint8 aHour, uint8 aMinute)
{
	return aHour * 60 + aMinute;
}
//=============================================================================
void checkConfigs(void)
{
	int i;
	for(i = 0; i < 3; i++)
	{
		if(configs.periph[i].hStart > 23) configs.periph[i].hStart = 0;
		if(configs.periph[i].hStop  > 23) configs.periph[i].hStop = 0;

		if(configs.periph[i].mStart > 59) configs.periph[i].mStart = 0;
		if(configs.periph[i].mStop  > 59) configs.periph[i].mStop = 0;

	}

	for(i = 0; i < 2; i++)
	{
		if (configs.light[i].hour   > 23) configs.light[i].hour = 0;
		if (configs.light[i].minute > 59) configs.light[i].minute = 0;
	}
}
//=============================================================================
#define DELTA	(2)
uint8 currentLight = 0;
uint8 day_night = 0;
uint16 eatSecCounter;
//=============================================================================
void configsProcced(void)
{

}
//=============================================================================
void ICACHE_FLASH_ATTR saveConfigs(void) {
    flashWriteBit = 0;
	int result = -1;
	os_delay_us(100000);
	result = spi_flash_erase_sector(PRIV_PARAM_START_SEC + PRIV_PARAM_SAVE);
	result = -1;
	os_delay_us(100000);
	result = spi_flash_write(
			(PRIV_PARAM_START_SEC + PRIV_PARAM_SAVE) * SPI_FLASH_SEC_SIZE,
			(uint32 *) &configs, sizeof(u_CONFIG));

	ets_uart_printf("Write W = %d\r\n", result);
}

//=============================================================================
void ICACHE_FLASH_ATTR readConfigs(void) {
	int result = -1;
	result = spi_flash_read(
			(PRIV_PARAM_START_SEC + PRIV_PARAM_SAVE) * SPI_FLASH_SEC_SIZE,
			(uint32 *) &configs, sizeof(u_CONFIG));
}
//==============================================================================


