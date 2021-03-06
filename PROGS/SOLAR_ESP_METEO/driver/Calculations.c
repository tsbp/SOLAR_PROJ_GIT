//==============================================================================
#include "driver\services.h"
#include "driver\Calculations.h"
#include "driver\Configs.h"
#include "math.h"
//==============================================================================
#define  DEG_TO_RAD (0.01745329)
#define LAT	(48.5 * DEG_TO_RAD)
#define LON ( 32.23 * DEG_TO_RAD)
double PI =  3.141592654;
double TWOPI = 6.28318531;
int Hour, Minute = 0, Second = 0, Month = 11, Day = 30, Year, Zone = +2;
double azimuth, elev;
//==============================================================================================
double _fmod(double x, double y)
{
	int f = (int)(x / y + 0.5);
	///ets_uart_printf("f: %d\n", (int)(1000 * f));
	return x - y * f;
}
//==============================================================================================
long JulianDate(int year, int month, int day)
{
	long JD_whole;
	int A, B;
	if (month <= 2)
	{
		year--;
		month += 12;
	}
	A = year / 100;
	B = 2 - A + A / 4;
	JD_whole = (long) (365.25 * (year + 4716)) + (int) (30.6001 * (month + 1)) + day + B - 1524;
	return JD_whole;
}
//==============================================================================
void ICACHE_FLASH_ATTR Calculate(double Lat, double Lon, int aYear, int aMonth, int aDay, int aHour, int aMinute, int aSecond)
{
	Lat *= DEG_TO_RAD;
	Lon *= DEG_TO_RAD;

        double T, JD_frac, L0, M, e, C, L_true, f, R, GrHrAngle, Obl, RA, Decl, HrAngle;
        long JD_whole, JDx;

        JD_whole = JulianDate(aYear, aMonth, aDay);
        JD_frac = ((double)aHour + (double)aMinute/60. + (double)aSecond/3600.)/24. - 0.5;

        T  = JD_whole - 2451545; T = (T + JD_frac)/36525.;

        double a = 280.46645 + 36000.76983 * T;
        L0 = DEG_TO_RAD * _fmod(a, 360);
        M  = DEG_TO_RAD * _fmod(357.5291  + 35999.0503  * T, 360);

        e  = 0.016708617 - 0.000042037 * T;
        C  = DEG_TO_RAD * ((1.9146-0.004847*T)*sin(M) + (0.019993-0.000101*T)*sin(2*M) + 0.00029*sin(3*M));
        f  = M + C;

        Obl = DEG_TO_RAD * (23 + 26/60. + 21.448/3600. - 46.815/3600*T);
        JDx = JD_whole - 2451545;
        GrHrAngle = 280.46061837 + (360*JDx)%360 + .98564736629*JDx + 360.98564736629*JD_frac;
        GrHrAngle = _fmod(GrHrAngle,360.);

        L_true    = _fmod(C+L0,TWOPI);
        R    = 1.000001018*(1-e*e)/(1+e*cos(f));
        RA   = atan2(sin(L_true)*cos(Obl),cos(L_true));

        Decl = asin(sin(Obl)*sin(L_true));
        HrAngle = DEG_TO_RAD*GrHrAngle+Lon-RA;

        elev = asin(sin(Lat)*sin(Decl) + cos(Lat)*(cos(Decl)*cos(HrAngle)));
        // Azimuth measured eastward from north.
        azimuth = PI + atan2(sin(HrAngle),cos(HrAngle)*sin(Lat) - tan(Decl)*cos(Lat));

        //ets_uart_printf("Lat: %d, Decl: %d, HrAngle: %d\n", (int)(1000 * Lat), (int)(1000 * Decl), (int)(1000 * HrAngle));
    }
//==============================================================================
void addValueToArray(sint16 aVal, sint16 * aArr)
{
  unsigned int k;
  for(k = FILTER_LENGHT-1; k > 0; k--)
    aArr[k] = aArr[k-1];

  aArr[0] = aVal;
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
    for(i = k + 1; i < aLng; i++)
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


