//======================================================================================
#include "driver\BH1715.h"
//======================================================================================
void ICACHE_FLASH_ATTR BH1715(unsigned char aOp, unsigned char aDev, unsigned char aOpCode, unsigned char *aBuf, unsigned int aLng)
{
	uint16 i;

	i2c_start();

	aDev = aDev << 1;
	if(aOp == I2C_READ) aDev |= 1;

	i2c_writeByte(aDev);
	if (!i2c_check_ack())
	{
		i2c_stop();
		return;
	}

	if (aOp == I2C_WRITE)
	{
		i2c_writeByte(aOpCode);
		if (!i2c_check_ack())
		{
			i2c_stop();
			return ;
		}
		i2c_stop();
	}

	else if (aOp == I2C_READ)
	{
		for(i = 0; i < aLng; i++)
		{
			aBuf[i] = i2c_readByte();
			i2c_send_ack(1);
		}
	}
	i2c_send_ack(0); // NOACK
	i2c_stop();


//  if(aOp == I2C_WRITE)
//  {
//    MSData[0] = aOpCode;//*aBuf;
//    NUM_BYTES_TX = 1;
//    Setup_TX(aDev);
//    i2cTransmit();
//     while (UCB0CTL1 & UCTXSTP);
//  }
//
//  if(aOp == I2C_READ)
//  {
//    Setup_RX(aDev);
//    i2cReceive(aLng/*6*/, aBuf);
//    while (UCB0CTL1 & UCTXSTP);
//    __no_operation();
//  }
}
//======================================================================================


