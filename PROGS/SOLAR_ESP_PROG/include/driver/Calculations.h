//==============================================================================
#ifndef CALC_H
#define CALC_H
//==============================================================================
#include "LSM303.h"
#include "c_types.h"
//==============================================================================
#define FILTER_LENGHT   (35)

#define X_SCALE	(0)
#define Y_SCALE	(1)
#define Z_SCALE	(2)

//==============================================================================
void addValueToArray(sint16 aVal, sint16 * aArr);
sint16 mFilter(sint16 * aBuf, uint16 aLng);
//void getAngles(u3AXIS_DATA* aRaw,  long * aPitch, long * aRoll);
void ICACHE_FLASH_ATTR getAngles(u3AXIS_DATA* aRawAcc, u3AXIS_DATA* aRawMag, sint16 * aPitch, sint16 * aRoll, sint16 * aHead);
long heading(u3AXIS_DATA* aRawMag, long aPitch, long aRoll);
double ICACHE_FLASH_ATTR kfs(double data);
float ICACHE_FLASH_ATTR scale(int aAxis, sint16 aVal);
//==============================================================================
extern sint16 _roll, _pitch, _heading;
extern sint16 rollArray[FILTER_LENGHT], pitchArray[FILTER_LENGHT], yawArray[FILTER_LENGHT];

extern sint16 cx[FILTER_LENGHT], cy[FILTER_LENGHT], cz[FILTER_LENGHT];
extern u3AXIS_DATA cc;

extern sint16 ax[FILTER_LENGHT], ay[FILTER_LENGHT], az[FILTER_LENGHT];
extern u3AXIS_DATA aa;

extern sint16 uncalPitch;
//==============================================================================
#endif
