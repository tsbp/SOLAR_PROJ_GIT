#include "ets_sys.h"
#include "osapi.h"
#include "gpio.h"
#include "os_type.h"
#include "user_config.h"
#include "user_interface.h"
#include "driver/uart.h"
#include "espconn.h"
#include "at_custom.h"
#include "mem.h"
#include "driver/UDP_Source.h"
#include "driver/services.h"
#include "driver/configs.h"
//#include "driver/LSM303.h"
#include "driver\Calculations.h"
//=========================================================================================
//extern u_CONFIG configs;
struct espconn *UDP_PC;


uint8 channelFree = 1;

sint16 plotData[2][24];
//============================================================================================================================
#define UDP_PORT	(7171)
//============================================================================================================================
void ICACHE_FLASH_ATTR UDP_Init_client()
{

	UDP_PC = (struct espconn *) os_zalloc(
			sizeof(struct espconn));
	UDP_PC->proto.udp = (esp_udp *) os_zalloc(sizeof(esp_udp));
	UDP_PC->state = ESPCONN_NONE;
	UDP_PC->type = ESPCONN_UDP;

	UDP_PC->proto.udp->local_port = UDP_PORT; //The port on which we want the esp to serve
	UDP_PC->proto.udp->remote_port = UDP_PORT; //The port on which we want the esp to serve

	//Set The call back functions
	espconn_regist_recvcb(UDP_PC, UDP_Recieved);
	espconn_create(UDP_PC);
}
//============================================================================================================================
//void ICACHE_FLASH_ATTR addValueToArray(char * tPtr, sint16 *arPtr, char aRot)
//{
//	int i,j;
//	if(aRot == ROTATE)
//	{
//		for(i = 0; i < POINTS_CNT-1; i++)
//			arPtr[i]= arPtr[i+1];
//	}
//
//    sint16 e = (tPtr[1]- '0') * 100 + (tPtr[2]- '0') * 10 + (tPtr[3] - '0');
//
//    if(tPtr[0] == '-')  e *= (-1);
//
//		arPtr[POINTS_CNT-1] = e;
//}
//=========================================================================================
uint8 crcCalc(uint8 *aBuf, uint8 aLng)
    {
        uint16 sum = 0, i;
        for(i = 0; i < aLng; i++)
            sum += aBuf[i];
        return  (((sum >> 8) & 0xff) + (sum & 0xff));
    }
//=========================================================================================
uint8 dataLng = 0;
uint8 ansBuffer[20] = {ID_SLAVE, 0, 0};

uint16 angV = 27000, angH = 15846;
//=========================================================================================
void UDP_Recieved(void *arg, char *pusrdata, unsigned short length)
{
	int a, i;
//	ets_uart_printf("recv udp data: ");
//	for (a = 0; a < length; a++)
//		ets_uart_printf("%02x ", pusrdata[a]);
//	ets_uart_printf("\r\n");

	struct espconn *pesp_conn = arg;
	//flashWriteBit = 0;
	remot_info *premot = NULL;

	uint8 crc = crcCalc(pusrdata, pusrdata[0] + 1);
	//ets_uart_printf("crc = %02x, in_crc = %02x\r\n", crc, pusrdata[length - 1]);
	if (espconn_get_connection_info(pesp_conn, &premot, 0) == ESPCONN_OK && pusrdata[0] == ID_MASTER)
			//&& 			crc == pusrdata[length - 1])
	{
		pesp_conn->proto.udp->remote_port = UDP_PORT;
		pesp_conn->proto.udp->remote_ip[0] = premot->remote_ip[0];
		pesp_conn->proto.udp->remote_ip[1] = premot->remote_ip[1];
		pesp_conn->proto.udp->remote_ip[2] = premot->remote_ip[2];
		pesp_conn->proto.udp->remote_ip[3] = premot->remote_ip[3];


		switch(pusrdata[1])
		{
			case CMD_ANGLE:
				dataLng = 1;
				ansBuffer[3] = OK;
//				inProcess = 5;
//				blink = BLINK_WAIT;
				break;

			case CMD_AZIMUTH:
				dataLng = 1;
				ansBuffer[3] = OK;
//				inProcess = 5;
//				blink = BLINK_WAIT;
				break;

			case CMD_LEFT:
				dataLng = 1;
				ansBuffer[3] = OK;
				inProcess = PROC_DURATION;
				blink = BLINK_BACKWARD;
				break;

			case CMD_RIGHT:
				dataLng = 1;
				ansBuffer[3] = OK;
				inProcess = PROC_DURATION;
				blink = BLINK_FORWARD;
				break;

			case CMD_UP:
				dataLng = 1;
				blink = BLINK_WAIT;
				ansBuffer[3] = OK;
				inProcess = PROC_DURATION;
				blink = BLINK_UP;
				break;

			case CMD_DOWN:
				dataLng = 1;
				ansBuffer[3] = OK;
				inProcess = PROC_DURATION;
				blink = BLINK_DOWN;
				break;

			case CMD_STATE:
				dataLng   = 9;
				ansBuffer[3]  = _pitch;
				ansBuffer[4]  = _pitch >> 8;
				ansBuffer[5]  = _roll;
				ansBuffer[6]  = _roll >> 8;
				ansBuffer[7]  = _heading;
				ansBuffer[8]  = _heading >> 8;
				ansBuffer[9]  = light;
				ansBuffer[10] = light >> 8;
				ansBuffer[11] = terminators;
				break;

			case CMD_CFG:
				ansBuffer[3] = OK;
				break;
		}

		ansBuffer[1] = pusrdata[1]; // add cmd;
		ansBuffer[2] = dataLng;     // add length;
		// add crc16

		ansBuffer[dataLng + 3] = 0xcc;
		ansBuffer[dataLng + 4] = 0xcc;

//		ets_uart_printf("ans udp data: ");
//			for (a = 0; a < dataLng + 5; a++)
//				ets_uart_printf("%02x ", ansBuffer[a]);
//			ets_uart_printf("\r\n");

		espconn_sent(pesp_conn, ansBuffer, 5 + dataLng);
	}
}



