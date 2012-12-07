#include <stdio.h>
#include "platform.h"
#include "xparameters.h"
#include "xgpio.h"
#include "xstatus.h"
#include "xintc.h"
#include "xil_exception.h"

// debug variable for additional console output
#define DEBUG 1

// definition for I/O devices
#define GPIO_CHANNEL1 1

// led test definition
#define LED_DELAY 1000000
#define LED_MAX_BLINK 0x1

// Interrupt service
static XIntc intc;

// I/O peripherals
XGpio leds;
XGpio buttons;
XGpio switches;

// required forward definitions
extern double pow(double base, double exp);
extern void xil_printf(const char *ctrl1, ...);

// conversion from "boolean" array to single value
int convertToInt(int values[8]) {

	if(DEBUG == 1) xil_printf("\n  converting 8-bit array to single value ...");

	int i, rslt = 0;
	for(i = 0; i < 8; i++)
		if(values[i] != 0) rslt = rslt + (int)pow(2, i);

	if(DEBUG == 1) xil_printf(" done\n  hex value after conversion: 0x%X", rslt);

	return rslt;
}

// conversion from single int value to a "boolean" array (1 = true, 0 = false)
int convertToArr(int size, int values[], int dataRead) {
	if(DEBUG == 1) xil_printf("\n    converting single value to %d-bit array ...", size);

	// check, if given value is to large for specified array
	if(dataRead >= (int)pow(2,size)) {
		if(DEBUG == 1) xil_printf("\n  ERROR: value was to large for given array");
		return XST_FAILURE;
	}

	// set values array accordingly
	int i;
	for(i = size-1; i >= 0; i--) {
		int cur = (int)pow(2,i);
		if(dataRead / cur > 0) {
			values[i] = 1;
			dataRead -= cur;
		} else values[i] = 0;
	}

	if(DEBUG == 1) {
		xil_printf(" done\n  value after conversion: [");
		for(i = 0; i < size; i++) xil_printf(" %d", values[i]);
		xil_printf(" ]");
	}

	return XST_SUCCESS;
}

// Sets LED state to given value
int setLEDRAW(u32 value) {
	XGpio_DiscreteWrite(&leds, GPIO_CHANNEL1, value);
	return XST_SUCCESS;
}

// Sets LED state to value represented by int array
int setLED(int values[8]) {
	if(DEBUG == 1) xil_printf("\n  setting LED state to [ %d %d %d %d %d %d %d %d ] ...",
			values[0] != 0, values[1] != 0, values[2] != 0, values[3] != 0,
			values[4] != 0, values[5] != 0, values[6] != 0, values[7] != 0);
	setLEDRAW(convertToInt(values));
	if(DEBUG == 1) xil_printf("\n  LED state set successfully");
	return XST_SUCCESS;
}

u32 readSwitchesRAW() {
	return XGpio_DiscreteRead(&switches, GPIO_CHANNEL1);
}

int readSwitches(int values[8]) {
	u32 dataRead = readSwitchesRAW();
	if(DEBUG == 1) xil_printf("\n  reading switch state at hex value: 0x%X ...", dataRead);
	if(convertToArr(8, values, dataRead) != XST_SUCCESS) return XST_FAILURE;
	if(DEBUG == 1) xil_printf("\n  finished reading button state\n");
	return XST_SUCCESS;
}

u32 readButtonsRAW() {
	return XGpio_DiscreteRead(&buttons, GPIO_CHANNEL1);
}

int readButtons(int values[5]) {
	u32 dataRead = readButtonsRAW();
	if(DEBUG == 1) xil_printf("\n  reading push button state at hex value: 0x%X ...", dataRead);
	if(convertToArr(5, values, dataRead) != XST_SUCCESS) return XST_FAILURE;
	if(DEBUG == 1) xil_printf("\n  finished reading button switch state\n");
	return XST_SUCCESS;
}

// TODO find out, which parts of this setup are really required
// Setup interrupt controller
int IntcInterruptSetup() {
	if (DEBUG == 1) xil_printf("\n  initialising interrupt controller driver ...");

	// initialize interrupt controller driver so that it is ready to use
	int Status = XIntc_Initialize(&intc, XPAR_MICROBLAZE_0_INTC_DEVICE_ID);
	if (Status != XST_SUCCESS) return Status;

	// Perform a self-test to ensure that the hardware was built  correctly.
	Status = XIntc_SelfTest(&intc);
	if (Status != XST_SUCCESS) return Status;

	// Initialize the exception table.
	Xil_ExceptionInit();

	// Register the interrupt controller handler with the exception table.
//	Xil_ExceptionRegisterHandler(XIL_EXCEPTION_ID_INT, (Xil_ExceptionHandler)XIntc_DeviceInterruptHandler, &intc);
	Xil_ExceptionRegisterHandler(XIL_EXCEPTION_ID_INT, (Xil_ExceptionHandler)XIntc_InterruptHandler, &intc);

	// Enable exceptions.
	Xil_ExceptionEnable();

	// Start the interrupt controller such that interrupts are enabled for all devices that cause interrupts.
	Status = XIntc_Start(&intc, XIN_REAL_MODE);
	if (Status != XST_SUCCESS) return Status;

	if(DEBUG == 1) xil_printf(" done");

	return XST_SUCCESS;
}

// TODO different handlers for different devices...
// TODO this is a test input handler. The "final" input handler should delegate
//      these events to the host with a specific callback method there.
void GpioHandlerSwitches(void *CallbackRef) {
	XGpio *GpioPtr = (XGpio *)CallbackRef;

	// Test application: set LED state to Switch state
	setLEDRAW(readSwitchesRAW());

	//Clear the Interrupt
	XGpio_InterruptClear(GpioPtr, GPIO_CHANNEL1);

}

void GpioHandlerButtons(void *CallbackRef) {
	XGpio *GpioPtr = (XGpio *)CallbackRef;

	// Test application: print out some text
	xil_printf("\nhey - stop pushing!! %d", readButtonsRAW());

	//Clear the Interrupt
	XGpio_InterruptClear(GpioPtr, GPIO_CHANNEL1);
}

int GpioSetupIntrSystem(XGpio *instancePtr, u16 intrId, Xil_ExceptionHandler handler) {
	if(DEBUG == 1) xil_printf("\n  hooking up interrupt service routines ...");

		// TODO I think THIS is the only interesting line and the rest can be removed (except perhaps exception management)
		// Hook up interrupt service routines
		// TODO provide better data than &buttons/&switches??
		XIntc_Connect(&intc, intrId,  handler, instancePtr);

		if(DEBUG == 1) xil_printf(" done\n  enabling interrupt vectors at interrupt controller ...");

		// Enable the interrupt vector at the interrupt controller
		XIntc_Enable(&intc, intrId);

		if(DEBUG == 1) xil_printf(" done\n  enabling GPIO channel interrupts ...");

		// Start the interrupt controller such that interrupts are recognized and handled by the processor
		//	result = XIntc_Start(&intc, XIN_REAL_MODE);
		//	if (result != XST_SUCCESS) return result;

		/*
		 * Enable the GPIO channel interrupts so that push button can be
		 * detected and enable interrupts for the GPIO device
		 */
		XGpio_InterruptEnable(instancePtr, GPIO_CHANNEL1);
		XGpio_InterruptGlobalEnable(instancePtr);

		//	if(DEBUG == 1) xil_printf(" done\n  registering exception handler ...");
		// maybe we need Xil_ExceptionInit() and Xil_ExceptionEnable() as well...

		if(DEBUG == 1) xil_printf(" done");

		return XST_SUCCESS;
}

int GpioSetupIntrSystemFull() {


	return XST_SUCCESS;
}

int init() {
	if (DEBUG == 1) xil_printf("\ninitialisation ... ");

	// initialize platform
	init_platform();

	// initialize I/O devices
	if (DEBUG == 1) xil_printf("\nI/O peripheral initialisation ...");
	if (XGpio_Initialize(&leds,             XPAR_LEDS_8BITS_DEVICE_ID) != XST_SUCCESS) return XST_FAILURE;
	if (XGpio_Initialize(&buttons,  XPAR_PUSH_BUTTONS_5BITS_DEVICE_ID) != XST_SUCCESS) return XST_FAILURE;
	if (XGpio_Initialize(&switches, XPAR_DIP_SWITCHES_8BITS_DEVICE_ID) != XST_SUCCESS) return XST_FAILURE;

	// set data direction for I/O devices
	if(DEBUG == 1) xil_printf(" done\nsetting I/O data directions ...");
	XGpio_SetDataDirection(&leds,     GPIO_CHANNEL1, 0x0);
	XGpio_SetDataDirection(&buttons,  GPIO_CHANNEL1, 0xFFFFFFFF);
	XGpio_SetDataDirection(&switches, GPIO_CHANNEL1, 0xFFFFFFFF);

	// set LED state to 0 (just to be safe)
	if(DEBUG == 1) xil_printf(" done\nsetting initial LED state ...");
	XGpio_DiscreteWrite(&leds, GPIO_CHANNEL1, 0x0);

	// setup interrupts
	if (DEBUG == 1) xil_printf(" done\nsetting up interrupt serive ...");
	if (IntcInterruptSetup() != XST_SUCCESS) return XST_FAILURE;
	if (GpioSetupIntrSystem(&buttons,  XPAR_MICROBLAZE_0_INTC_PUSH_BUTTONS_5BITS_IP2INTC_IRPT_INTR,
            (Xil_ExceptionHandler)GpioHandlerButtons) != XST_SUCCESS) return XST_FAILURE;
	if (GpioSetupIntrSystem(&switches, XPAR_MICROBLAZE_0_INTC_DIP_SWITCHES_8BITS_IP2INTC_IRPT_INTR,
            (Xil_ExceptionHandler)GpioHandlerSwitches) != XST_SUCCESS) return XST_FAILURE;
	// return
	if(DEBUG == 1) xil_printf("\nfinished initialisation\n");
	return XST_SUCCESS;
}

int cleanup() {

	if(DEBUG == 1) xil_printf("\ncleaning up");

	// cleanup platform
	cleanup_platform();

	return XST_SUCCESS;
}

// busy waiting loop for LED sim
//#ifndef __SIM__
//	 	/*
//	 	 * Wait a small amount of time so the LED is visible
//	 	 */
//	 	for (Delay = 0; Delay < LED_DELAY; Delay++);
//	 #endif

int main() {
	if(DEBUG == 1) xil_printf("\nstarting up\n");

	// initialize everything
    init();

    // set LED state to Switch state
    setLEDRAW(readSwitchesRAW());

    // cleanup
    cleanup_platform();

    return 0;
}
