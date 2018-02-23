#include "ets_sys.h"
#include "osapi.h"
#include "gpio.h"
#include "os_type.h"
#include "user_config.h"
#include "user_interface.h"
#include "espconn.h"
#include "at_custom.h"
#include "mem.h"
#include "driver/UDP_Source.h"
#include "driver/services.h"
#include "driver/configs.h"
#include "driver/LSM303.h"
#include "driver/Calculations.h"
#include "driver/uart.h"
//=========================================================================================
//extern u_CONFIG configs;
struct espconn *UDP_PC;
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
	uint8 needAnswer = 0;
	dataPresent = DATA_PRS_AMNT;
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
		pesp_conn->proto.udp->remote_ip[3] = 255;//premot->remote_ip[3];


		switch(pusrdata[1])
		{

		 case CMD_CALIB:
			 {
				 switch(pusrdata[3])
				 {
					 case GET_COMPASS_RAW://if(pusrdata[3] == 0) // get
					 {
						 ets_uart_printf("GET COMPASS\r\n");
						 dataLng = 7;
						 needAnswer = 1;
						 ansBuffer[3] = pusrdata[3];
						 memcpy(ansBuffer + 4, compass.byte, 7);
					 }break;
					 case SET_COMPASS_CALIBS:
					 {
						 memcpy(configs.calibs.byte, pusrdata + 4, 12);

						 ets_uart_printf("CMD_CALIB_SET: %d, %d, %d, %d, %d, %d,  \r\n",
								 configs.calibs.Max.x,
								 configs.calibs.Max.y,
								 configs.calibs.Max.z,
								 configs.calibs.Min.x,
								 configs.calibs.Min.y,
								 configs.calibs.Min.z);

						 flashWriteBit = 1;
						 needAnswer = 1;
						 ansBuffer[3] = OK;
						 dataLng = 1;
					 }break;

					 case GET_ACCEL_RAW:
					 {
						 ets_uart_printf("GET ACCEL\r\n");
						 dataLng = 3;
						 needAnswer = 1;
						 ansBuffer[3] = pusrdata[3];
						 ansBuffer[4] = uncalPitch;
						 ansBuffer[5] = uncalPitch >> 8;
					 }break;

					 case SET_ACCEL_CALIBS:
					 {
						 memcpy(configs.calibs.byte + 12, pusrdata + 4, 4);
						 ets_uart_printf("SET ACCEL a0 = %d, a90 = %d\r\n", configs.calibs.acc0deg, configs.calibs.acc90deg);
						 flashWriteBit = 1;
						 needAnswer = 1;
						 ansBuffer[3] = OK;
						 dataLng = 1;
					 }break;
				 }
			 }
			 break;

		 case CMD_GOHOME:
				 break;

		    case CMD_SET_POSITION:
		    	orientation.income.azimuth   = (sint16)(pusrdata[5] | (pusrdata[6] << 8));
		    	orientation.income.elevation = (sint16)(pusrdata[3] | (pusrdata[4] << 8));

				ets_uart_printf("Elevation: %d, Azimuth: %d\r\n",
						orientation.income.elevation,
						orientation.income.azimuth);
				sysState.newPosition = 1;
				break;

			case CMD_ANGLE:
				dataLng = 1;
				needAnswer = 1;
				ansBuffer[3] = OK;
//				inProcess = 5;
//				blink = BLINK_WAIT;
				break;

			case CMD_AZIMUTH:
				dataLng = 1;
				needAnswer = 1;
				ansBuffer[3] = OK;
//				inProcess = 5;
//				blink = BLINK_WAIT;
				break;

			case CMD_LEFT:
				move(LEFT);
				goto ans;
			case CMD_RIGHT:
				move(RIGHT);
				goto ans;
			case CMD_UP:
				move(UP);
				goto ans;
			case CMD_DOWN:
				move(DOWN);
ans:			dataLng = 1;
				needAnswer = 1;
				ansBuffer[3] = OK;
				sysState.byte = 0;
				sysState.manualMove = 1; //inProcess = PROC_DURATION;

				break;

			case CMD_STATE:
				dataLng   = 9;
				needAnswer = 1;
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
				needAnswer = 1;
				ansBuffer[3] = OK;
				break;

			case CMD_WIFI:
				{
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
					needAnswer = 1;
					ansBuffer[3] = OK;
					dataLng = 1;
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



