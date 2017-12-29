//==============================================================================
#include "driver\Calculations.h"
#include "driver\Configs.h"
#include "driver\v_math.h"
#include "math.h"
//==============================================================================
sint16 _roll, _pitch, _heading;
sint16 rollArray[FILTER_LENGHT], pitchArray[FILTER_LENGHT], yawArray[FILTER_LENGHT];

//==============================================================================
void addValueToArray(sint16 aVal, sint16 * aArr)
{
  unsigned int k;
  for(k = FILTER_LENGHT-1; k > 0; k--)
    aArr[k] = aArr[k-1];
  
  aArr[0] = aVal;
}
//==============================================================================
const sint16 Mag_minx = -6340;
const sint16 Mag_miny = -7610;
const sint16 Mag_minz = -2670;
const sint16 Mag_maxx =  4280;
const sint16 Mag_maxy =  3050;
const sint16 Mag_maxz =  6540;

//const int Mag_minx = -518;
//const int Mag_miny = -646;
//const int Mag_minz = -814;
//const int Mag_maxx =  674;
//const int Mag_maxy =  561;
//const int Mag_maxz =  334;

#define X_SCALE	(0)
#define Y_SCALE	(1)
#define Z_SCALE	(2)
//const sint16 MAG_MIN_MAX[3][2] = {
//		{-634, 428}, // x
//		{-761, 305}, // y
//		{-267, 654}  // z

const sint16 MAG_MIN_MAX[3][2] = {
		{-631, 517}, // x
		{-565, 544}, // y
		{-462, 530}  // z
};
//==============================================================================
float scale(int aAxis, sint16 aVal)
{

//	Magx = (Magx-Mag_minx)/(Mag_maxx-Mag_minx)*2-1;
//	Magy = (Magy-Mag_miny)/(Mag_maxy-Mag_miny)*2-1;
//	Magz = (Magz-Mag_minz)/(Mag_maxz-Mag_minz)*2-1;

//	float a = (aVal                   - MAG_MIN_MAX[aAxis][0]);
//	float b = ((MAG_MIN_MAX[aAxis][1] - MAG_MIN_MAX[aAxis][0]));
//	float c = 2 * (a / b);
//	return (c - 1);

	float a = (aVal                   - configs.calibs.Min.data[aAxis]);
	float b = ((configs.calibs.Max.data[aAxis] - configs.calibs.Min.data[aAxis]));
	float c = 2 * (a / b);
	return (c - 1);
}
//==============================================================================
void ICACHE_FLASH_ATTR getAngles(u3AXIS_DATA* aRawAcc, u3AXIS_DATA* aRawMag, sint16 * aPitch, sint16 * aRoll, sint16 * aHead)
{
  //===== roll, pitch ===========
	double a = (double)aRawAcc->x;
	double b = (double)aRawAcc->y;
	double c = (double)aRawAcc->z;

	double cdf = sqrt((double) (a * a + b * b + c * c));

	double accxnorm = (double) aRawAcc->x / cdf;
	double accynorm = (double) aRawAcc->y / cdf;
	double accznorm = (double) aRawAcc->z / cdf;

	double sinP = 0 - accxnorm;
	double sinR = 0 - accynorm;
	double cosP = sqrt(1 - sinP * sinP);
	double cosR = sqrt(1 - sinR * sinR);

	double tP = asin(-accxnorm);
	double tR = asin(accynorm/cos(tP));

	*aPitch =(sint16)(10000 * tP);
	*aRoll = (sint16)(10000 * tR);

	//====== heading ======================
	// use calibration values to shift and scale magnetometer measurements
	double Magx = scale(X_SCALE, aRawMag->x);
	double Magy = scale(Y_SCALE, aRawMag->y);
	double Magz = scale(Z_SCALE, aRawMag->z);

	// tilt compensated magnetic sensor measurements
	double magxcomp = Magx*cosP + Magz*sinP;
	double magycomp = Magx*sinR*sinP + Magy*cosR - Magz*sinR*cosP;
//
//	ets_uart_printf("magxcomp = %d, magycomp = %d\r\n",
//					(int)(magxcomp*10000),
//					(int)(magycomp*10000));

	// arctangent of y/x
	*aHead = (sint16)(atan2f(magycomp, magxcomp) * 10000);
	//ets_uart_printf("atan_fx = %d \r\n", *aHead);
}
//==============================================================================
sint16 avgBuf[FILTER_LENGHT];
//==============================================================================
sint16 mFilter(sint16 * aBuf, uint16 aLng)
{  
#define MEDIAN
	unsigned int i, k;
#ifdef MEDIAN
  
  for(i = 0; i < aLng; i++) avgBuf[i] = aBuf[i];
  
  for(k = 0; k < aLng - 1; k++)
    for(i = k + 1; i < aLng - 1; i++)
    {
      if(avgBuf[k] < avgBuf[i])
      {
        sint16 tmp =  avgBuf[k];
        avgBuf[k] = avgBuf[i];
        avgBuf[i] = tmp;
      }
    }
  return avgBuf[aLng >> 1];
#else 
  long sum = 0;
  for(i = 0; i < aLng; i++)sum += aBuf[i];
  return sum / aLng; 
  
#endif
}
