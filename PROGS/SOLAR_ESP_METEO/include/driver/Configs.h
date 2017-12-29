//==============================================================================
#define PRIV_PARAM_START_SEC		0x3C
#define PRIV_PARAM_SAVE     0
#define SPI_FLASH_SEC_SIZE      4096
//==============================================================================
typedef union __packed
{
	uint8 byte[98];
	struct __packed
	{
		uint8 mode;
		uint8 auth;
		uint8 SSID[32];
		uint8 SSID_PASS[64];
	};
}s_WIFI_CFG;
//==============================================================================
typedef union __packed
{
	uint8 byte[10];
	struct
	{
		uint16 latit;
		uint16 longit;
		uint16 timeZone;
		uint16 wind;
		uint16 light;
	};
}u_METEO;
//==============================================================================
typedef union __packed
{
	  uint8 byte[sizeof(u_METEO) + sizeof(s_WIFI_CFG)];
	  struct __packed
	  {
		  u_METEO meteo;
		  s_WIFI_CFG wifi;
	  };
}u_CONFIG;
extern u_CONFIG configs;
//==============================================================================
extern u_CONFIG configs;
extern uint8 flashWriteBit;
extern uint8 timeTrue;
//==============================================================================
void checkConfigs(void);
void configsProcced(void);
void sntp_initialize(void);
uint8 timeSync(void);


