#include "c_types.h"
#include "user_interface.h"
#include "espconn.h"
#include "mem.h"
#include "osapi.h"
#include "upgrade.h"
#include "user_config.h"

#define pheadbuffer "Connection: keep-alive\r\n\
Cache-Control: no-cache\r\n\
User-Agent: Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101 Safari/537.36 \r\n\
Accept: */*\r\n\
Accept-Encoding: gzip,deflate\r\n\
Accept-Language: en-US;q=0.8\r\n\r\n"


//#define pheadbuffer "Connection: keep-alive\r\n\
//Cache-Control: max-age=0\r\n\
//Upgrade-Insecure-Requests: 1\r\n\
//User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36\r\n\
//Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8\r\n\
//Accept-Encoding: gzip, deflate\r\n\
//Accept-Language: ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7,de;q=0.6,es;q=0.5,lt;q=0.4,pt;q=0.3,uk;q=0.2,vi;q=0.1\r\n\r\n"

struct espconn *pespconn = NULL;
struct upgrade_server_info *upServer = NULL;

static os_timer_t ota_delay_check;
static struct espconn *pTcpServer = NULL;
static ip_addr_t host_ip;

/******************************************************************************
 * FunctionName : user_esp_platform_upgrade_cb
 * Description  : Processing the downloaded data from the server
 * Parameters   : pespconn -- the espconn used to connetion with the host
 * Returns      : none
*******************************************************************************/
LOCAL void ICACHE_FLASH_ATTR ota_rsp(void *arg)
{
	struct upgrade_server_info *server = arg;
	if(server->upgrade_flag == true)
	{
		#ifdef PLATFORM_DEBUG
		UARTSendStr("ota_rsp: device_upgrade_success.\r\n");
		#endif
		system_upgrade_reboot();
	}
	else
	{
		#ifdef PLATFORM_DEBUG
		UARTSendStr("ota_rsp: device_upgrade_failed.\r\n");
		#endif
	}
	os_free(server->url);
	server->url = NULL;
	os_free(server);
	server = NULL;
}

/**
  * @brief  Tcp client disconnect success callback function.
  * @param  arg: contain the ip link information
  * @retval None
  */
static void ICACHE_FLASH_ATTR ota_discon_cb(void *arg)
{
	struct espconn *pespconn = (struct espconn *)arg;
	uint8_t idTemp = 0;
	if(pespconn->proto.tcp != NULL)
	{
		os_free(pespconn->proto.tcp);
	}
	if(pespconn != NULL)
	{
		os_free(pespconn);
	}

	#ifdef PLATFORM_DEBUG
	UARTSendStr("ota_discon_cb: disconnect\r\n\r\n");
	#endif

	if(system_upgrade_start(upServer) == false)
	{
		#ifdef PLATFORM_DEBUG
		UARTSendStr("ota_discon_cb: FW upgrade failed to start.\r\n");
		#endif
	}
	else
	{
		#ifdef PLATFORM_DEBUG
		UARTSendStr("ota_discon_cb: FW upgrade started.\r\n");
		#endif
	}
}

/**
  * @brief  Udp server receive data callback function.
  * @param  arg: contain the ip link information
  * @retval None
  */
LOCAL void ICACHE_FLASH_ATTR ota_recv(void *arg, char *pusrdata, unsigned short len)
{
	struct espconn *pespconn = (struct espconn *)arg;
	char temp[32] = {0};
	uint8_t user_bin[22] = {0};
	uint8_t i = 0;

	os_timer_disarm(&ota_delay_check);
	#ifdef PLATFORM_DEBUG
	UARTSendStr("ota_recv: FW upgrade ready.\r\n");
	#endif

	upServer = (struct upgrade_server_info *)os_zalloc(sizeof(struct upgrade_server_info));
	upServer->upgrade_version[5] = '\0';
	upServer->pespconn = pespconn;
	os_memcpy(upServer->ip, pespconn->proto.tcp->remote_ip, 4);
	upServer->port = pespconn->proto.tcp->remote_port;
	upServer->check_cb = ota_rsp;
	upServer->check_times = 60000;
	if(upServer->url == NULL)
	{
		upServer->url = (uint8 *) os_zalloc(1024);
	}

	uint8 a = system_upgrade_userbin_check();

	ets_uart_printf("userbin_check(%d)\r\n", a);


	if(a == UPGRADE_FW_BIN1)
	{
		os_memcpy(user_bin, "user2.1024.new.2.bin", 20);
	}
	else if(a == UPGRADE_FW_BIN2)
	{
		os_memcpy(user_bin, "user1.1024.new.2.bin", 20);
	}

	os_sprintf(upServer->url, "POST /%s HTTP/1.1\r\nHost: "IPSTR":8000\r\n"pheadbuffer"", user_bin, IP2STR(upServer->ip));
#ifdef PLATFORM_DEBUG
	ets_uart_printf("%s\r\n", upServer->url);
#endif
}

LOCAL void ICACHE_FLASH_ATTR ota_wait(void *arg)
{
	struct espconn *pespconn = arg;
	os_timer_disarm(&ota_delay_check);
	if(pespconn != NULL)
	{
		espconn_disconnect(pespconn);
	}
	else
	{
		#ifdef PLATFORM_DEBUG
		UARTSendStr("ota_recv: upgrade error\r\n");
		#endif
	}
}

/******************************************************************************
 * FunctionName : user_esp_platform_sent_cb
 * Description  : Data has been sent successfully and acknowledged by the remote host.
 * Parameters   : arg -- Additional argument to pass to the callback function
 * Returns      : none
*******************************************************************************/
LOCAL void ICACHE_FLASH_ATTR ota_sent_cb(void *arg)
{
	struct espconn *pespconn = arg;
	os_timer_disarm(&ota_delay_check);
	os_timer_setfn(&ota_delay_check, (os_timer_func_t *)ota_wait, pespconn);
	os_timer_arm(&ota_delay_check, 5000, 0);
	#ifdef PLATFORM_DEBUG
	UARTSendStr("ota_sent_cb\r\n");
	#endif
}

/**
  * @brief  Tcp client connect success callback function.
  * @param  arg: contain the ip link information
  * @retval None
  */
static void ICACHE_FLASH_ATTR ota_connect_cb(void *arg)
{
	struct espconn *pespconn = (struct espconn *)arg;
	uint8_t user_bin[22] = {0};
	char *temp = NULL;

	#ifdef PLATFORM_DEBUG
	UARTSendStr("OTA: 2\r\n");
	#endif

	espconn_regist_disconcb(pespconn, ota_discon_cb);
	espconn_regist_recvcb(pespconn, ota_recv);
	espconn_regist_sentcb(pespconn, ota_sent_cb);

	temp = (uint8 *) os_zalloc(512);

	os_sprintf(temp,"GET /index.html HTTP/1.0\r\nHost: "IPSTR"\r\n"pheadbuffer"",
             IP2STR(pespconn->proto.tcp->remote_ip));

	espconn_sent(pespconn, temp, os_strlen(temp));
	os_free(temp);
}

/**
  * @brief  Tcp client connect repeat callback function.
  * @param  arg: contain the ip link information
  * @retval None
  */
static void ICACHE_FLASH_ATTR ota_recon_cb(void *arg, sint8 errType)
{
	struct espconn *pespconn = (struct espconn *)arg;
	if(pespconn->proto.tcp != NULL)
	{
		os_free(pespconn->proto.tcp);
	}
	os_free(pespconn);
	#ifdef PLATFORM_DEBUG
	UARTSendStr("ota_recon_cb\r\n");
	#endif
	if(upServer != NULL)
	{
		os_free(upServer);
		upServer = NULL;
	}
}

void ICACHE_FLASH_ATTR ota_start()
{
	char otaserverip[15];
	pespconn = (struct espconn *)os_zalloc(sizeof(struct espconn));
	pespconn->type = ESPCONN_TCP;
	pespconn->state = ESPCONN_NONE;
	pespconn->proto.tcp = (esp_tcp *)os_zalloc(sizeof(esp_tcp));
	pespconn->proto.tcp->local_port = espconn_port();
	pespconn->proto.tcp->remote_port = 8000;
	os_sprintf(otaserverip, "%s", OTASERVERIP);
	host_ip.addr = ipaddr_addr(otaserverip);
	#ifdef PLATFORM_DEBUG
	UARTSendStr("OTA: 1\r\n");
	#endif
	os_memcpy(pespconn->proto.tcp->remote_ip, &host_ip.addr, 4);
	espconn_regist_connectcb(pespconn, ota_connect_cb);
	espconn_regist_reconcb(pespconn, ota_recon_cb);
	espconn_connect(pespconn);
}
