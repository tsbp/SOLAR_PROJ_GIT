
//================== Commands ====================================================
#define OK			(0xff)
#define BAD			(0x00)

#define ID_SLAVE	(0x3C)
#define ID_MASTER	(0x7E)

#define CMD_ANGLE   (0x10)
#define CMD_AZIMUTH	(0x11)
#define CMD_LEFT	(0x20)
#define CMD_RIGHT	(0x21)
#define CMD_UP	    (0x22)
#define CMD_DOWN	(0x23)
#define CMD_STATE	(0xA0)
#define CMD_CFG		(0xC0)


void ICACHE_FLASH_ATTR UDP_Init();
void ICACHE_FLASH_ATTR UDP_Init_client();
void UDP_Recieved(void *arg, char *pusrdata, unsigned short length);
void   mergeAnswerWith(char tPtr[2][24][4]);
void ICACHE_FLASH_ATTR sendUDPbroadcast(uint8* abuf, uint16 aLen);
//void ICACHE_FLASH_ATTR addValueToArray(char * tPtr, sint16 *arPtr, char aRot);


extern uint8 channelFree;

