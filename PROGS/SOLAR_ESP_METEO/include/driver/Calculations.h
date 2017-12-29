//==============================================================================
#ifndef CALC_H
#define CALC_H
//==============================================================================
#include "c_types.h"
//==============================================================================
extern double azimuth, elev;
void ICACHE_FLASH_ATTR Calculate(double Lat, double Lon, int aYear, int aMonth, int aDay, int aHour, int aMinute, int aSecond);
void ICACHE_FLASH_ATTR meteoProcessing(void);
//==============================================================================
#endif
