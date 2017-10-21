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
long scale(int aAxis, int aVal)
{

//	Magx = (Magx-Mag_minx)/(Mag_maxx-Mag_minx)*2-1;
//	Magy = (Magy-Mag_miny)/(Mag_maxy-Mag_miny)*2-1;
//	Magz = (Magz-Mag_minz)/(Mag_maxz-Mag_minz)*2-1;

	int a = 1000*(aVal                  - MAG_MIN_MAX[aAxis][0]);
	int b = 1000*((MAG_MIN_MAX[aAxis][1] - MAG_MIN_MAX[aAxis][0]));
	long c = q_mul(toFixed(2000), q_div(toFixed(a),toFixed(b)));
	return q_sub(c, toFixed(1000));
}
//==============================================================================
void getAngles(u3AXIS_DATA* aRawAcc, u3AXIS_DATA* aRawMag, long * aPitch, long * aRoll, long * aHead)
{
  //===== roll, pitch ===========
	long a = (long)aRawAcc->x; //q_mul(toFixed(aRawAcc->x), toFixed(aRawAcc->x));
	long b = (long)aRawAcc->y; //q_mul(toFixed(aRawAcc->y), toFixed(aRawAcc->y));
	long c = (long)aRawAcc->z; //q_mul(toFixed(aRawAcc->z), toFixed(aRawAcc->z));

	ets_uart_printf("aRawAcc->x = %d, aRawAcc->y = %d, aRawAcc->z = %d\r\n",
						a,
						b,
						c);
//	ets_uart_printf("a^2 = %d, b^2 = %d, c^2 = %d\r\n",
//							a * a,
//							b * b,
//							c * c);

	long cdf = a*a + b * b + c * c; //q_add(a, q_add(b, c));
	ets_uart_printf("cdf = %d\r\n", cdf);


	float d = f_sqrt(cdf);

	a = (int)d;
	ets_uart_printf("Q = %d\r\n", d);


	float accxnorm = aRawAcc->x / d; //q_div(toFixed(aRawAcc->x), d);
	float accynorm = aRawAcc->y / d; //q_div(toFixed(aRawAcc->y), d);
	float accznorm = aRawAcc->z / d; //q_div(toFixed(aRawAcc->z), d);

	float sinP = 0 - accxnorm; //q_sub(0, accxnorm);
	float sinR = 0 - accynorm; //q_sub(0, accynorm);
	float cosP = f_sqrt(1 - sinP * sinP); //sqrt_fx(q_sub(toFixed(1000), q_mul(sinP, sinP)));
	float cosR = f_sqrt(1 - sinR * sinR); //sqrt_fx(q_sub(toFixed(1000), q_mul(sinR, sinR)));

    a = sinP * 10000;
    b = sinP * 10000;
    c = sinP * 10000;
    d = sinP * 10000;

	ets_uart_printf("sinP = %d, sinR = %d, cosP = %d, cosR = %d, \r\n", a, b, c, d);

	*aPitch = toFloatX10000(asin_fx(sinP)); //*aPitch = asin_fx(-accxnorm);
	*aRoll =  toFloatX10000(asin_fx(sinR));         // *aRoll =  asin(accynorm/cos(*aPitch));

	float ee = -1.456, bb = 1.234;
		int ss = ee / bb* 1000000;
		ets_uart_printf("mul = %d (0x%04x)\r\n", ss);

/*
	//====== heading ======================
//	long Magx = toFixed(aRawMag->x);
//	long Magy = toFixed(aRawMag->y);
//	long Magz = toFixed(aRawMag->z);

	//  Magx *= ((double)1100 / 1000);
	//  Magy *= ((double)1100 / 1000);
	//  Magz *= ((double)980  / 1000);

	// use calibration values to shift and scale magnetometer measurements
	long Magx = scale(X_SCALE, aRawMag->x);//(Magx-Mag_minx)/(Mag_maxx-Mag_minx)*2-1;
	long Magy = scale(Y_SCALE, aRawMag->y);//(Magy-Mag_miny)/(Mag_maxy-Mag_miny)*2-1;
	long Magz = scale(Z_SCALE, aRawMag->z);//(Magz-Mag_minz)/(Mag_maxz-Mag_minz)*2-1;




	ets_uart_printf("aRawMag->x = %d, aRawMag->y = %d, aRawMag->z = %d\r\n",
					(aRawMag->x),
					(aRawMag->y),
					(aRawMag->z));

	ets_uart_printf("Magx = %d, Magy = %d, Magz = %d\r\n",
				toFloatX10000(Magx),
				toFloatX10000(Magy),
				toFloatX10000(Magz));

	//long sinR = sin(aRoll);
	//long cosR = cos(aRoll);
	//long sinP = sin(aPitch);
	//long cosP = cos(aPitch);

	// tilt compensated magnetic sensor measurements
	long magxcomp = q_add(q_mul(Magx, cosP), q_mul(Magz, sinP)); //Magx*cosP+Magz*sinP;

	a = q_mul(q_mul(Magx, sinR), sinP);
	b = q_mul(Magy, cosR);
	c = q_mul(q_mul(Magz, sinR), cosP);
	long magycomp = q_sub(q_add(a,b), c); //Magx*sinR*sinP+Magy*cosR-Magz*sinR*cosP;

	ets_uart_printf("magxcomp = %d, magycomp = %d\r\n",
					toFloatX10000(magxcomp),
					toFloatX10000(magycomp));

	// arctangent of y/x
	long cc = q_div(magycomp, magxcomp);
		ets_uart_printf("cc = %d (0x%04x)\r\n", toFloatX10000(cc), cc);

	*aHead = toFloatX10000(atan_fx(cc)); //(atan2(magycomp, magxcomp));
	ets_uart_printf("atan_fx = %d (0x%04x)\r\n", toFloatX10000(*aHead), atan_fx(cc));

	float ee = -1.456, bb = 1.234;
	int ss = ee * bb* 1000000;
	ets_uart_printf("mul = %d (0x%04x)\r\n", ss);


	*/

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
