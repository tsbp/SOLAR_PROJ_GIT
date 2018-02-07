//==============================================================================
#include "driver\Calculations.h"
#include "driver\Configs.h"
#include "driver\v_math.h"
#include "math.h"
//==============================================================================
sint16 _roll, _pitch, _heading;
sint16 rollArray[FILTER_LENGHT], pitchArray[FILTER_LENGHT], yawArray[FILTER_LENGHT];
//==============================================================================
void ICACHE_FLASH_ATTR addValueToArray(sint16 aVal, sint16 * aArr)
{
  unsigned int k;
  for(k = FILTER_LENGHT-1; k > 0; k--)
    aArr[k] = aArr[k-1];
  
  aArr[0] = aVal;
}
//==============================================================================
#define X_SCALE	(0)
#define Y_SCALE	(1)
#define Z_SCALE	(2)
//==============================================================================
#define MEAS_90	( 1.43117f)
#define MEAS_0	(-0.0558505f)
//#define MEAS_90	( 1.79769f)
//#define MEAS_0	(-0.010472f)
//==============================================================================
double ICACHE_FLASH_ATTR getScaled(double aVal)
{
	return ((aVal - MEAS_0)/(MEAS_90 - MEAS_0)) * M_PI/2;
}
//==============================================================================
float ICACHE_FLASH_ATTR scale(int aAxis, sint16 aVal)
{

//	Magx = (Magx-Mag_minx)/(Mag_maxx-Mag_minx)*2-1;
//	Magy = (Magy-Mag_miny)/(Mag_maxy-Mag_miny)*2-1;
//	Magz = (Magz-Mag_minz)/(Mag_maxz-Mag_minz)*2-1;

	float a = (aVal                            - configs.calibs.Min.data[aAxis]);
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

//	ets_uart_printf("x = %d, y = %d, z = %d\r\n",
//			aRawAcc->x,
//			aRawAcc->y,
//			aRawAcc->z);

	double cdf = sqrt((double) (a * a + b * b + c * c));

	double accxnorm =  a / cdf;
	double accynorm =  b / cdf;
	double accznorm =  c / cdf;

	double tP = asin(-accxnorm);
	double tR = asin(accynorm/cos(tP));

	if(c < 0) tP = (M_PI - tP);
	tP = getScaled(tP);


	*aPitch =(sint16)(10000 * tP);
	*aRoll = (sint16)(10000 * tR);

//	*aPitch = 0;
//	*aRoll = 0;

	//====== heading ======================
	// use calibration values to shift and scale magnetometer measurements
	double Magx = scale(X_SCALE, aRawMag->x);
	double Magy = scale(Y_SCALE, aRawMag->y);
	double Magz = scale(Z_SCALE, aRawMag->z);

	// tilt compensated magnetic sensor measurements
//	double magxcomp = Magx*cosP + Magz*sinP;
//	double magycomp = Magx*sinR*sinP + Magy*cosR - Magz*sinR*cosP;

	double magxcomp = Magx*cos(tP)+Magz*sin(tP);
	double magycomp = Magx*sin(tR)*sin(tP)+Magy*cos(tR)- Magz*sin(tR)*cos(tP);
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
//==============================================================================
// Kalman's filter
//==============================================================================
#define Q  (1000.0f)
#define R  (10000.0)
#define F  (1.0)
#define H  (1.0)

double ICACHE_FLASH_ATTR kfs(double data)
{
	static double X0 = 0.0;
	static double 	P0 = 0.0;

	static double State = 0.0;
	static double Covariance = 0.0;
//
//	def __init__(self, q, r, f, h):
//		self.Q = q
//		self.R = r
//		self.F = f
//		self.H = h

//	def correct(self, data):
//		# +180 -> -180  or -180 -> +180
//		if (data > 90 and self.State < -90) or (data < -90 and self.State > 90):
//			if data > 0:
//				self.State = self.State + 360
//			if data < -0:
//				self.State = self.State - 360

		X0 = F*State;
		P0 = F*Covariance*F + Q;
		double K = H * P0/(H * P0 * H + R);
		State = X0 + K * (data - H * X0);
		Covariance = (1 - K * H) * P0;

//		if self.State > 180:
//			self.State = self.State - 360
//		if self.State < -180:
//			self.State = self.State + 360
		return State;
}


