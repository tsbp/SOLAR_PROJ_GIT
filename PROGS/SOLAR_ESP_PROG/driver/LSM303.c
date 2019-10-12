//==============================================================================
#include "driver/LSM303.h"
#include "driver/i2c.h"
#include "driver/services.h"
//==============================================================================
u3AXIS_DATA accel, compass;
//==============================================================================
uint16 lsm303(unsigned char aOp, unsigned char aDev, unsigned char aReg, unsigned char *aBuf, unsigned int aLng)
{
	uint16 i;

	i2c_start();

	uint8 addr =  aDev << 1;
	unsigned int reg = aReg;

	if(aOp == I2C_WRITE)
	{
		i2c_writeByte(addr);
		if (!i2c_check_ack()) { i2c_stop(); return 0;}

		i2c_writeByte(reg);
		if (!i2c_check_ack()) { i2c_stop(); return 0;}

		i2c_writeByte(*aBuf);
		if (!i2c_check_ack()) { i2c_stop(); return 0;}

	}
	else if (aOp == I2C_READ)
	{
		i2c_writeByte(addr);
		if (!i2c_check_ack()) { i2c_stop(); return 0;}

		if(aLng > 1) reg = (reg | BIT7);
		i2c_writeByte(reg);
		if (!i2c_check_ack()) { i2c_stop(); return 0;}

		i2c_start();

		addr |= BIT0;
		i2c_writeByte(addr);
		if (!i2c_check_ack()) { i2c_stop(); return 0;}

		for(i = 0; i < aLng; i++)
		{
			aBuf[i] = i2c_readByte();
			if(i == aLng -1) i2c_send_ack(0); // NOACK
			else i2c_send_ack(1);
		}
	}

	i2c_stop();
	return 1;
}

//==============================================================================
unsigned char LSM303_CFG[4][3] = {
//		       ADDR				REGISTER      VALUE
		{LSM303A_I2C_ADDR, LSM303A_CTRL_REG1, 0x57},
		{LSM303A_I2C_ADDR, LSM303A_CTRL_REG4, 0x08},
		{LSM303M_I2C_ADDR, LSM303M_CRA_REG,   0x9c},
		{LSM303M_I2C_ADDR, LSM303M_MR_REG,    0x00}
};

//==============================================================================
void LSM303Init(void)
{
	uint16 a = 0, i;
    unsigned char tmp;

	for(i = 0; i < 4; i++)
	{
			a += lsm303(I2C_WRITE, LSM303_CFG[i][0], LSM303_CFG[i][1], &LSM303_CFG[i][2], 1);
	        lsm303(I2C_READ,  LSM303_CFG[i][0], LSM303_CFG[i][1], &tmp, 1);
	        ets_uart_printf("%02x.%02x: %02x => %02x\r\n",
	        		LSM303_CFG[i][0],
					LSM303_CFG[i][1],
					LSM303_CFG[i][2],
					tmp);
	}
  if(a) sysState.sensorError = 0;
  ets_uart_printf("a = %d\r\n",  a);
}

//==============================================================================
uint16 sensorCfgOK(void)
{
	int i;
	unsigned char tmp;
	for(i = 0; i < 4; i++)
	{
		lsm303(I2C_READ,  LSM303_CFG[i][0], LSM303_CFG[i][1], &tmp, 1);
		if(tmp != LSM303_CFG[i][2]) return 0;
	}
	return 1;
}
