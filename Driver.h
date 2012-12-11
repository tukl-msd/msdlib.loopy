
// attributes of Driver
extern XIntc intc;
extern XGpio buttons;
extern XGpio leds;
extern XGpio switches;

// procedures of Driver
int IntcInterruptSetup ( );
int GpioSetupIntrSystem ( XGpio *instancePtr, u16 intrId, Xil_ExceptionHandler handler );
int getButtons ( );
void GpioHandlerButtons ( void *CallbackRef );
int setLED ( u32 value );
int getSwitches ( );
void GpioHandlerSwitches ( void *CallbackRef );
int init ( );
int cleanup ( );
int main ( );
