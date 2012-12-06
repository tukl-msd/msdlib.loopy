/*
 * Copyright (c) 2009 Xilinx, Inc.  All rights reserved.
 *
 * Xilinx, Inc.
 * XILINX IS PROVIDING THIS DESIGN, CODE, OR INFORMATION "AS IS" AS A
 * COURTESY TO YOU.  BY PROVIDING THIS DESIGN, CODE, OR INFORMATION AS
 * ONE POSSIBLE   IMPLEMENTATION OF THIS FEATURE, APPLICATION OR
 * STANDARD, XILINX IS MAKING NO REPRESENTATION THAT THIS IMPLEMENTATION
 * IS FREE FROM ANY CLAIMS OF INFRINGEMENT, AND YOU ARE RESPONSIBLE
 * FOR OBTAINING ANY RIGHTS YOU MAY REQUIRE FOR YOUR IMPLEMENTATION.
 * XILINX EXPRESSLY DISCLAIMS ANY WARRANTY WHATSOEVER WITH RESPECT TO
 * THE ADEQUACY OF THE IMPLEMENTATION, INCLUDING BUT NOT LIMITED TO
 * ANY WARRANTIES OR REPRESENTATIONS THAT THIS IMPLEMENTATION IS FREE
 * FROM CLAIMS OF INFRINGEMENT, IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

/*
 * helloworld.c: simple test application
 */

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
#define INTC XIntc

// Interrupt service
static XIntc intc;

// I/O peripherals
XGpio leds;
XGpio buttons;
XGpio switches;

// forward definitions
void print(char *str);
double pow(double base, double exp);


int IntcInterruptSetup(XIntc *IntcInstancePtr, u16 DeviceId) {

	int Status;

	/*
	 * Initialize the interrupt controller driver so that it is
	 * ready to use.
	 */
	Status = XIntc_Initialize(IntcInstancePtr, DeviceId);
	if (Status != XST_SUCCESS) return XST_FAILURE;

	/*
	 * Perform a self-test to ensure that the hardware was built  correctly.
	 */
	Status = XIntc_SelfTest(IntcInstancePtr);
	if (Status != XST_SUCCESS) return XST_FAILURE;

	/*
	 * Initialize the exception table.
	 */
	Xil_ExceptionInit();

	/*
	 * Register the interrupt controller handler with the exception table.
	 */
	Xil_ExceptionRegisterHandler(XIL_EXCEPTION_ID_INT,
			(Xil_ExceptionHandler)XIntc_DeviceInterruptHandler,
			(void*) 0);

	/*
	 * Enable exceptions.
	 */
	Xil_ExceptionEnable();

	/*
	 * Start the interrupt controller such that interrupts are enabled for
	 * all devices that cause interrupts.
	 */
	Status = XIntc_Start(IntcInstancePtr, XIN_REAL_MODE);
	if (Status != XST_SUCCESS) {
		return XST_FAILURE;
	}

	return XST_SUCCESS;
}

int setupInterrupt();
int GpioSetupIntrSystem(INTC *IntcInstancePtr, XGpio *InstancePtr, u16 DeviceId, u16 IntrId);

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
	if (IntcInterruptSetup(&intc, XPAR_MICROBLAZE_0_INTC_DEVICE_ID != XST_SUCCESS)) return XST_FAILURE;
	if (GpioSetupIntrSystem(&intc, &buttons, XPAR_PUSH_BUTTONS_5BITS_DEVICE_ID,
			XPAR_MICROBLAZE_0_INTC_PUSH_BUTTONS_5BITS_IP2INTC_IRPT_INTR) != XST_SUCCESS) return XST_FAILURE;
	if (GpioSetupIntrSystem(&intc, &switches, XPAR_DIP_SWITCHES_8BITS_DEVICE_ID,
			XPAR_MICROBLAZE_0_INTC_DIP_SWITCHES_8BITS_IP2INTC_IRPT_INTR) != XST_SUCCESS) return XST_FAILURE;

	// return
	if(DEBUG == 1) xil_printf(" done\nfinished initialisation\n");
	return XST_SUCCESS;
}

int cleanup() {

	if(DEBUG == 1) xil_printf("\ncleaning up");

	// cleanup platform
	cleanup_platform();

	return XST_SUCCESS;
}

// conversion from "boolean" array to single value
int convertToInt(int values[8]) {

	if(DEBUG == 1) xil_printf("\n  converting 8-bit array to single value ...");

	int i, rslt = 0;
	for(i = 0; i < 8; i++)
		if(values[i] != 0) rslt = rslt + (int)pow(2, i);

	if(DEBUG == 1) xil_printf(" done\n  hex value after conversion: 0x%X", rslt);

	return rslt;
}

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

int setLED(int values[8]) {
	if(DEBUG == 1) xil_printf("\n  setting LED state to [ %d %d %d %d %d %d %d %d ] ...",
			values[0] != 0, values[1] != 0, values[2] != 0, values[3] != 0,
			values[4] != 0, values[5] != 0, values[6] != 0, values[7] != 0);
	XGpio_DiscreteWrite(&leds, GPIO_CHANNEL1, convertToInt(values));
	if(DEBUG == 1) xil_printf("\n  LED state set successfully");
	return XST_SUCCESS;
}

int readSwitches(int values[8]) {
	u32 dataRead = XGpio_DiscreteRead(&switches, GPIO_CHANNEL1);
	if(DEBUG == 1) xil_printf("\n  reading switch state at hex value: 0x%X ...", dataRead);
	if(convertToArr(8, values, dataRead) != XST_SUCCESS) return XST_FAILURE;
	if(DEBUG == 1) xil_printf("\n  finished reading button state\n");
	return XST_SUCCESS;
}

int readButtons(int values[5]) {
	u32 dataRead = XGpio_DiscreteRead(&buttons, GPIO_CHANNEL1);
	if(DEBUG == 1) xil_printf("\n  reading push button state at hex value: 0x%X ...", dataRead);
	if(convertToArr(5, values, dataRead) != XST_SUCCESS) return XST_FAILURE;
	if(DEBUG == 1) xil_printf("\n  finished reading button switch state\n");
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
	if(DEBUG == 1) print("\nstarting up\n");

    init();


    print("\ntest a");

    int i;
    int val [8]= {0,0,1,1,1,1,1,1};

    setLED(val);


//    ledTest(XPAR_LEDS_8BITS_DEVICE_ID);
//
//    u32 DataRead;
//
//    switchTest2(XPAR_LEDS_8BITS_DEVICE_ID, &DataRead);
//
//    xil_printf("Read data:0x%X\n", DataRead);
//
//    switchTest(XPAR_DIP_SWITCHES_8BITS_DEVICE_ID, &DataRead);
//
//    xil_printf("Read data:0x%X\n", DataRead);

    cleanup_platform();

    return 0;
}

//static u16 GlobalIntrMask;    /* GPIO channel mask that is needed by the Interrupt Handler */
static volatile u32 IntrFlag; /* Interrupt Handler Flag */

void GpioHandler(void *CallBackRef);

//int GpioSetupIntrSystem(INTC *IntcInstancePtr, XGpio *InstancePtr, u16 DeviceId, u16 IntrId) {
//	int Result;
//
////	GlobalIntrMask = GPIO_CHANNEL1;
//
//	/*
//	 * Initialize the interrupt controller driver so that it's ready to use.
//	 * specify the device ID that was generated in xparameters.h
//	 */
//	Result = XIntc_Initialize(IntcInstancePtr, XPAR_INTC_0_DEVICE_ID);
//	if (Result != XST_SUCCESS) return Result;
//
//	/* Hook up interrupt service routine */
//	XIntc_Connect(IntcInstancePtr, IntrId, (Xil_ExceptionHandler)GpioHandler, InstancePtr);
//
//	/* Enable the interrupt vector at the interrupt controller */
//
//	XIntc_Enable(IntcInstancePtr, IntrId);
//	/*
//	 * Start the interrupt controller such that interrupts are recognized
//	 * and handled by the processor
//	 */
//	Result = XIntc_Start(IntcInstancePtr, XIN_REAL_MODE);
//	if (Result != XST_SUCCESS) return Result;
//	return Result;
//
//}
#define INTC_HANDLER	XIntc_InterruptHandler
#define INTC_DEVICE_ID	XPAR_INTC_0_DEVICE_ID

// TODO find out, which parts of this setup are really required
// TODO different handlers for different devices...
int GpioSetupIntrSystem(INTC *IntcInstancePtr, XGpio *InstancePtr, u16 DeviceId, u16 IntrId) {
	int result;

	// Hook up interrupt service routine
	// TODO I think THIS is the only interesting line and the rest can be removed (except perhaps exception management)
	XIntc_Connect(IntcInstancePtr, IntrId, (Xil_ExceptionHandler)GpioHandler, InstancePtr);

	// Enable the interrupt vector at the interrupt controller
	XIntc_Enable(IntcInstancePtr, IntrId);

	// Start the interrupt controller such that interrupts are recognized and handled by the processor
	result = XIntc_Start(IntcInstancePtr, XIN_REAL_MODE);
	if (result != XST_SUCCESS) return result;

	/*
	 * Enable the GPIO channel interrupts so that push button can be
	 * detected and enable interrupts for the GPIO device
	 */
	XGpio_InterruptEnable(InstancePtr, GPIO_CHANNEL1);
	XGpio_InterruptGlobalEnable(InstancePtr);

	/*
	 * Initialize the exception table and register the interrupt
	 * controller handler with the exception table
	 */
	Xil_ExceptionInit();

	Xil_ExceptionRegisterHandler(XIL_EXCEPTION_ID_INT, (Xil_ExceptionHandler)INTC_HANDLER, IntcInstancePtr);

	/* Enable non-critical exceptions */
	Xil_ExceptionEnable();

	return XST_SUCCESS;
}

// TODO this is a test input handler. The "final" input handler should delegate
//      these events to the host with a specific callback method there.
void GpioHandler(void *CallbackRef) {

	XGpio *GpioPtr = (XGpio *)CallbackRef;

	int values [5];

	xil_printf("\nhey - stop pushing!!");
	readButtons(values);

	//Clear the Interrupt
	XGpio_InterruptClear(GpioPtr, GPIO_CHANNEL1);

}

