#ifndef DRIVER_H_
#define DRIVER_H_

#include "xintc.h"
#include "xgpio.h"
#include "xil_exception.h"
#include "platform.h"

// attributes of Driver
extern XGpio leds;
extern XIntc intc;
extern XGpio switches;

// procedures of Driver
int setLED ( u32 value );
int GpioSetupIntrSystem ( XGpio *instancePtr, u16 intrId, Xil_ExceptionHandler handler );
u32 getSwitches ( );
void GpioHandlerSwitches ( void *CallbackRef );
int intrSetup ( );
int init ( );
int cleanup ( );
int main ( );

#endif /* DRIVER_H_ */