//==============================================================================
#ifndef CALC_H
#define CALC_H
//==============================================================================
#include "LSM303.h"
#include "c_types.h"
//==============================================================================
#define FILTER_LENGHT   (50)

#define X_SCALE	(0)
#define Y_SCALE	(1)
#define Z_SCALE	(2)

//==============================================================================
void addValueToArray(sint16 aVal, sint16 * aArr);
sint16 mFilter(sint16 * aBuf, uint16 aLng);
//void getAngles(u3AXIS_DATA* aRaw,  long * aPitch, long * aRoll);
void ICACHE_FLASH_ATTR getAngles(u3AXIS_DATA* aRawAcc, u3AXIS_DATA* aRawMag, sint16 * aPitch, sint16 * aRoll, sint16 * aHead);
long heading(u3AXIS_DATA* aRawMag, long aPitch, long aRoll);
//==============================================================================
extern sint16 _roll, _pitch, _heading;
extern sint16 rollArray[FILTER_LENGHT], pitchArray[FILTER_LENGHT], yawArray[FILTER_LENGHT];
//==============================================================================
#endif
