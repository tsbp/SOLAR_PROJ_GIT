//==============================================================================
#include "io430.h"
//==============================================================================
float abs(float a)
{
  if (a < 0) return a*(-1);
  return a;
}
//==============================================================================
float square_root(float num) {
  float x1 = (num * 1.0) / 2;
  float x2= (x1 + (num / x1)) / 2;
  while(abs(x1 - x2) >= 0.0000001) {
    x1 = x2;
    x2 = (x1 + (num / x1)) / 2;
  }
  return x2;
}
//==============================================================================
float sq, dd = 1.44f;
//==============================================================================
void main( void )
{
  // Stop watchdog timer to prevent time out reset
  WDTCTL = WDTPW + WDTHOLD;
  sq = square_root(dd);
  
  while(1)
  {
    
  }
}