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

//=========================================================================================
uint8 crcCalc(uint8 *aBuf, uint8 aLng)
    {
        uint16 sum = 0, i;
        for(i = 0; i < aLng; i++)
            sum += aBuf[i];
        return  (((sum >> 8) & 0xff) + (sum & 0xff));
    }
//=========================================================================================

uint8 ansBuffer[20] = {ID_METEO, 0, 0};

uint16 angV = 27000, angH = 15846;
//=========================================================================================
void UDP_Recieved(void *arg, char *pusrdata, unsigned short length)
{
	int a, i;
	uint8 needAnswer = 0;
//	ets_uart_printf("recv udp data: ");
//	for (a = 0; a < length; a++)
//		ets_uart_printf("%02x ", pusrdata[a]);
//	ets_uart_printf("\r\n");

	uint8 dataLng = 0;

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

		 case CMD_GOHOME:
				 break;

		    case CMD_SET_POSITION:
				break;

			case CMD_ANGLE:
				break;

			case CMD_AZIMUTH:
				break;

			case CMD_LEFT:
				break;

			case CMD_RIGHT:
				break;

			case CMD_UP:
				break;

			case CMD_DOWN:
				break;

			case CMD_STATE:
				needAnswer = 1;
				dataLng   = 9;
//				ansBuffer[3]  = _pitch;
//				ansBuffer[4]  = _pitch >> 8;
//				ansBuffer[5]  = _roll;
//				ansBuffer[6]  = _roll >> 8;
//				ansBuffer[7]  = _heading;
//				ansBuffer[8]  = _heading >> 8;
				ansBuffer[9]  = freq;
				ansBuffer[10] = freq >> 8;
//				ansBuffer[11] = terminators;
				break;

			case CMD_CFG:
				ansBuffer[3] = OK;
				break;

			case CMD_WIFI:
				{

						ets_uart_printf("recv udp data: ");
						for (a = 0; a < length; a++)
							ets_uart_printf("%02x ", pusrdata[a]);
						ets_uart_printf("\r\n");

					int i, j;

					os_memset(configs.wifi.SSID,      0, sizeof(configs.wifi.SSID));
					os_memset(configs.wifi.SSID_PASS, 0, sizeof(configs.wifi.SSID_PASS));

					for (i = 0; i < length; i++)
					{
						if (pusrdata[i + 3] == '$')	break;
						else	configs.wifi.byte[i] = pusrdata[i + 3];
					}

					j = i + 4;
					for (i = 0; i < length - j - 2; i++) configs.wifi.SSID_PASS[i] = pusrdata[i + j];

					ets_uart_printf("configs.wifi.SSID %s\r\n", configs.wifi.SSID);
					ets_uart_printf("configs.wifi.SSID_PASS %s\r\n", configs.wifi.SSID_PASS);

					serviceMode = MODE_SW_RESET;
					service_timer_start();
					flashWriteBit = 1;
					ansBuffer[3] = OK;
				}
				break;
		}

		if(needAnswer)
		{
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
}



