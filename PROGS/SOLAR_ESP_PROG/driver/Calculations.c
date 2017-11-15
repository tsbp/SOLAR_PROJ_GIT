//==============================================================================
#include "driver\Calculations.h"
#include "driver\v_math.h"
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
const sint16 MAG_MIN_MAX[3][2] = {
		{-634, 428}, // x
		{-761, 305}, // y
		{-267, 654}  // z
};
//==============================================================================
float scale(int aAxis, sint16 aVal)
{

//	Magx = (Magx-Mag_minx)/(Mag_maxx-Mag_minx)*2-1;
//	Magy = (Magy-Mag_miny)/(Mag_maxy-Mag_miny)*2-1;
//	Magz = (Magz-Mag_minz)/(Mag_maxz-Mag_minz)*2-1;

	float a = (aVal                   - MAG_MIN_MAX[aAxis][0]);
	float b = ((MAG_MIN_MAX[aAxis][1] - MAG_MIN_MAX[aAxis][0]));
	float c = 2 * (a / b);
	return (c - 1);
}
//==============================================================================
float tatan2(float y, float x){
  float z = y / x;
  return z - (z*z*z / 3) + (z*z*z*z*z / 5) - (z*z*z*z*z*z*z / 7) + (z*z*z*z*z*z*z*z*z / 9);
}
//================================================================
void getAngles(u3AXIS_DATA* aRawAcc, u3AXIS_DATA* aRawMag, sint16 * aPitch, sint16 * aRoll, sint16 * aHead)
{
  //===== roll, pitch ===========
	long a = (long)aRawAcc->x;
	long b = (long)aRawAcc->y;
	long c = (long)aRawAcc->z;
	long d;

//	ets_uart_printf("aRawAcc->x = %d, aRawAcc->y = %d, aRawAcc->z = %d\r\n",
//						a,
//						b,
//						c);

	long cdf = iSqrt(a*a + b * b + c * c); //q_add(a, q_add(b, c));
//	ets_uart_printf("Q = %d\r\n", cdf);

	float accxnorm = (float) aRawAcc->x / cdf;
	float accynorm = (float) aRawAcc->y / cdf;
	float accznorm = (float) aRawAcc->z / cdf;

	a = (int)(accxnorm * 10000);
	b = (int)(accynorm * 10000);
	c = (int)(accznorm * 10000);

//	ets_uart_printf("accxnorm = %d, accynorm = %d, accznorm = %d,\r\n", a, b, c);

	float sinP = 0 - accxnorm; //q_sub(0, accxnorm);
	float sinR = 0 - accynorm; //q_sub(0, accynorm);
	float cosP = (float)iSqrt(100000000*(1 - sinP * sinP))/10000;
	float cosR = (float)iSqrt(100000000*(1 - sinR * sinR))/10000;



//    a = (int)(sinP * 10000);
//    b = (int)(sinR * 10000);
//    c = (int)(cosP * 10000);
//    d = (int)(cosR * 10000);
//
//	ets_uart_printf("sinP = %d, sinR = %d, cosP = %d, cosR = %d, \r\n", a, b, c, d);

	*aPitch = iAsin((sint16)(sinP * 10000)); //*aPitch = asin_fx(-accxnorm);
	*aRoll =  iAsin((sint16)(sinR * 10000));         // *aRoll =  asin(accynorm/cos(*aPitch));

	ets_uart_printf("aPitch = %d, aRoll = %d \r\n", *aPitch, *aRoll);


	//====== heading ======================

	//  Magx *= ((double)1100 / 1000);
	//  Magy *= ((double)1100 / 1000);
	//  Magz *= ((double)980  / 1000);

	// use calibration values to shift and scale magnetometer measurements
	float Magx = scale(X_SCALE, aRawMag->x);//(Magx-Mag_minx)/(Mag_maxx-Mag_minx)*2-1;
	float Magy = scale(Y_SCALE, aRawMag->y);//(Magy-Mag_miny)/(Mag_maxy-Mag_miny)*2-1;
	float Magz = scale(Z_SCALE, aRawMag->z);//(Magz-Mag_minz)/(Mag_maxz-Mag_minz)*2-1;

//	ets_uart_printf("aRawMag->x = %d, aRawMag->y = %d, aRawMag->z = %d\r\n",
//					(aRawMag->x),
//					(aRawMag->y),
//					(aRawMag->z));
//
//	ets_uart_printf("Magx = %d, Magy = %d, Magz = %d\r\n",
//				(int)(Magx*10000),
//				(int)(Magy*10000),
//				(int)(Magz*10000));


	// tilt compensated magnetic sensor measurements
	float magxcomp = Magx*cosP + Magz*sinP;
	float magycomp = Magx*sinR*sinP + Magy*cosR - Magz*sinR*cosP;

	ets_uart_printf("magxcomp = %d, magycomp = %d\r\n",
					(int)(magxcomp*10000),
					(int)(magycomp*10000));

	// arctangent of y/x
	float cc = magycomp / magxcomp;
	ets_uart_printf("cc = %d \r\n", (int)(cc * 1000));

	int uPI = 31416;

			int u = iAtan((sint16)(cc * 1000));
			    if( magxcomp < 0.0 )// 2nd, 3rd quadrant
			    {
			        if( u > 0 )// will go to 3rd quadrant
			            u -= uPI;
			        else
			            u += uPI;
			    }



	*aHead =  u;//(atan2(magycomp, magxcomp));
	ets_uart_printf("atan_fx = %d \r\n", *aHead);


}
//==============================================================================
long heading(u3AXIS_DATA* aRawMag, long aPitch, long aRoll)
{
//  long Magx = toFixed(aRawMag->x);
//  long Magy = toFixed(aRawMag->y);
//  long Magz = toFixed(aRawMag->z);
//
////  Magx *= ((double)1100 / 1000);
////  Magy *= ((double)1100 / 1000);
////  Magz *= ((double)980  / 1000);
//
//  // use calibration values to shift and scale magnetometer measurements
//  Magx = (Magx-Mag_minx)/(Mag_maxx-Mag_minx)*2-1;
//  Magy = (Magy-Mag_miny)/(Mag_maxy-Mag_miny)*2-1;
//  Magz = (Magz-Mag_minz)/(Mag_maxz-Mag_minz)*2-1;
//
//  long sinR = sin(aRoll);
//  long cosR = cos(aRoll);
//  long sinP = sin(aPitch);
//  long cosP = cos(aPitch);
//
//
//  // tilt compensated magnetic sensor measurements
//  double magxcomp = Magx*cosP+Magz*sinP;
//  double magycomp = Magx*sinR*sinP+Magy*cosR-Magz*sinR*cosP;
//
//  // arctangent of y/x
//  //double Heading = (Math.atan2(magycomp,magxcomp));
//  return (atan2(magycomp, magxcomp));
	return 1;

}
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
        unsigned int tmp =  avgBuf[k];
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
