//==============================================================================
#include "driver/LSM303.h"
#include "driver/i2c.h"
//==============================================================================
u3AXIS_DATA accel, compass;


//==============================================================================
uint16 lsm303(unsigned char aOp, unsigned char aDev, unsigned char aReg, unsigned char *aBuf, unsigned int aLng)
{
	uint16 i, err = 0;

	i2c_start();

	uint8 addr =  aDev << 1;
	unsigned int reg = aReg;

	if(aOp == I2C_WRITE)
	{
		//addr = addr << 1;
		i2c_writeByte(addr);
		if (!i2c_check_ack()) { i2c_stop(); err++;} // return 0;}

		i2c_writeByte(reg);
		if (!i2c_check_ack()) { i2c_stop(); err++;} // return 0;}

		i2c_writeByte(*aBuf);
		if (!i2c_check_ack()) { i2c_stop(); err++;} // return 0;}


	}
	else if (aOp == I2C_READ)
	{
		//addr = addr << 1;
		i2c_writeByte(addr);
		if (!i2c_check_ack()) { i2c_stop(); err++;} // return 0;}

		if(aLng > 1) reg = (reg | BIT7);
		i2c_writeByte(reg);
		if (!i2c_check_ack()) { i2c_stop(); err++;} // return 0;}

		i2c_start();

		//addr = addr << 1;
		addr |= BIT0;
		i2c_writeByte(addr);
		if (!i2c_check_ack()) { i2c_stop(); err++;} // return 0;}

		for(i = 0; i < aLng; i++)
		{
			//ets_uart_printf("aBuf[%d] %d\r\n", i, aBuf[i]);
			aBuf[i] = i2c_readByte();
			if(i == aLng -1) i2c_send_ack(0); // NOACK
			else i2c_send_ack(1);
		}
	}

	i2c_stop();
	if(err) return 0;
	return 1;
}
//==============================================================================
unsigned char regValue = 0;
//==============================================================================
void LSM303Init(void)
{
  //============= accelerometer ==========================
  lsm303(I2C_READ,  LSM303A_I2C_ADDR, LSM303A_CTRL_REG1, &regValue, 1);
  ets_uart_printf("LSM303A_CTRL_REG1 %02x\r\n",  regValue);
  regValue = 0x57;
  lsm303(I2C_WRITE, LSM303A_I2C_ADDR, LSM303A_CTRL_REG1, &regValue,   1);
  ets_uart_printf("LSM303A_CTRL_REG1 %02x\r\n",  regValue);
  //__delay_cycles(80000L);
  lsm303(I2C_READ,  LSM303A_I2C_ADDR, LSM303A_CTRL_REG1, &regValue, 1);
  //ets_uart_printf("LSM303A_CTRL_REG1 %02x\r\n",  regValue);
  
  lsm303(I2C_READ,  LSM303A_I2C_ADDR, LSM303A_CTRL_REG2, &regValue, 1);
  ets_uart_printf("LSM303A_CTRL_REG2 %02x\r\n",  regValue);
  regValue = 0x00;
  lsm303(I2C_WRITE, LSM303A_I2C_ADDR, LSM303A_CTRL_REG2, &regValue,   1);
  ets_uart_printf("LSM303A_CTRL_REG2 %02x\r\n",  regValue);
  lsm303(I2C_READ,  LSM303A_I2C_ADDR, LSM303A_CTRL_REG2, &regValue, 1);
  ets_uart_printf("LSM303A_CTRL_REG2 %02x\r\n",  regValue);
  
  lsm303(I2C_READ,  LSM303A_I2C_ADDR, LSM303A_CTRL_REG4, &regValue, 1);
  ets_uart_printf("LSM303A_CTRL_REG4 %02x\r\n",  regValue);
  regValue |= (BIT3 );//| BIT7);
  lsm303(I2C_WRITE, LSM303A_I2C_ADDR, LSM303A_CTRL_REG4, &regValue, 1);
  ets_uart_printf("LSM303A_CTRL_REG4 %02x\r\n",  regValue);
  //__delay_cycles(80000L);
  lsm303(I2C_READ,  LSM303A_I2C_ADDR, LSM303A_CTRL_REG4, &regValue, 1);
  ets_uart_printf("LSM303A_CTRL_REG4 %02x\r\n",  regValue);
  
  //============= magnitometer ===========================
  lsm303(I2C_READ,  LSM303M_I2C_ADDR, LSM303M_CRA_REG, &regValue, 1);
  regValue = 0x9c;
  lsm303(I2C_WRITE, LSM303M_I2C_ADDR, LSM303M_CRA_REG, &regValue, 1);
  //__delay_cycles(80000L);
  lsm303(I2C_READ,  LSM303M_I2C_ADDR, LSM303M_CRB_REG, &regValue, 1);
  
  lsm303(I2C_READ,  LSM303M_I2C_ADDR, LSM303M_MR_REG,  &regValue, 1);
  regValue = 0;
  lsm303(I2C_WRITE, LSM303M_I2C_ADDR, LSM303M_MR_REG,  &regValue, 1);
  //__delay_cycles(80000L);
  lsm303(I2C_READ,  LSM303M_I2C_ADDR, LSM303M_MR_REG,  &regValue, 1);
}
