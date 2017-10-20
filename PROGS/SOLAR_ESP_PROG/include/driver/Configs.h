//==============================================================================
#define PRIV_PARAM_START_SEC		0x3C
#define PRIV_PARAM_SAVE     0
#define SPI_FLASH_SEC_SIZE      4096
//==============================================================================
typedef union __packed
{
  uint32 byte[11];
  struct __packed
  {
	uint8 day[7];
    uint16 interval;
    uint8 delta;
  };
}u_NASTROYKI;

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
	uint8 byte[3];
	struct __packed
	{
		uint8 hour;
		uint8 minute;
		uint8 light;
	};
}u_LIGHT;
//==============================================================================
typedef union __packed
{
	uint8 byte[4];
	struct __packed
	{
		uint8 hStart;
		uint8 mStart;
		uint8 hStop;
		uint8 mStop;
	};
}u_PERIPHERIAL;
//==============================================================================
#define LIGHT_CNT  (2)
#define PERIPH_CNT (3)
//==============================================================================
typedef union __packed
{
	  uint8 byte[sizeof(uint16) + LIGHT_CNT * sizeof(u_LIGHT) + PERIPH_CNT * sizeof(u_PERIPHERIAL) + sizeof(s_WIFI_CFG)];
	  struct __packed
	  {
		sint16 temperature;
		u_LIGHT light[2];
		u_PERIPHERIAL periph[3];
		uint8 stepperTurnsFW;
		uint8 stepperTurnsBW;
		uint8 eatMinutes;
		s_WIFI_CFG wifi;
	  };
}u_CONFIG;
extern u_CONFIG configs;
//==============================================================================
extern u_CONFIG configs;
extern u_CONFIG *cPtrH, *cPtrW;
extern uint8 flashWriteBit;
extern uint8 periphWord;
extern uint8 day_night;
extern uint8 timeTrue;
//==============================================================================
void checkConfigs(void);
void configsProcced(void);
void sntp_initialize(void);
uint8 timeSync(void);
////==============================================================================
//typedef union __packed
//{
//  uint32 byte[2 + 2 * sizeof(u_LIGHT) + 3 * sizeof(u_PERIPHERIAL)];
//  struct __packed
//  {
//	uint16 temperature;
//	u_LIGHT light[2];
//	u_PERIPHERIAL periph[3];
//	s_WIFI_CFG wifiCfg;
//  };
//}u_CONFIGS;

