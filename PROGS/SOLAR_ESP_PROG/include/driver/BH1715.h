//======================================================================================
#ifndef __BH1715_H__
#define __BH1715_H__
//======================================================================================
#include "i2c.h"
//======================================================================================
void ICACHE_FLASH_ATTR BH1715(unsigned char aOp, unsigned char aDev, unsigned char aOpCode, unsigned char *aBuf, unsigned int aLng);
//======================================================================================
#endif