/*
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#include "../interrupts.h"

#include "gpio.h"

static u16 GlobalIntrMask; /* GPIO channel mask that is needed by the Interrupt Handler */

static volatile u32 IntrFlag; /* Interrupt Handler Flag */

//void GpioHandler(void *CallbackRef) {
//	XGpio *GpioPtr = (XGpio *)CallbackRef;
//
//	IntrFlag = 1;
//	/*
//	 * Clear the Interrupt
//	 */
//	XGpio_InterruptClear(GpioPtr, GlobalIntrMask);
//
//}

int GpioIntrSetup(XGpio *InstancePtr, u16 DeviceId, u16 IntrId, u16 IntrMask, Xil_ExceptionHandler GpioHandler) {
	int Result;

	GlobalIntrMask = IntrMask;

#ifdef XPAR_INTC_0_DEVICE_ID

#ifndef TESTAPP_GEN
	INTC *intc = getIntc();

	/*
	 * Initialize the interrupt controller driver so that it's ready to use.
	 * specify the device ID that was generated in xparameters.h
	 */
	xil_printf("a");
//	Result = XIntc_Initialize(IntcInstancePtr, INTC_DEVICE_ID);
//	if (Result != XST_SUCCESS) {
//		return Result;
//	}
#endif /* TESTAPP_GEN */

	/* Hook up interrupt service routine */
	xil_printf("b");
	XIntc_Connect(intc, IntrId, GpioHandler, InstancePtr);

	/* Enable the interrupt vector at the interrupt controller */
	xil_printf("c");
	XIntc_Enable(intc, IntrId);

#ifndef TESTAPP_GEN
	/*
	 * Start the interrupt controller such that interrupts are recognized
	 * and handled by the processor
	 */
	xil_printf("d");
	Result = XIntc_Start(intc, XIN_REAL_MODE);
	if (Result != XST_SUCCESS) {
		return Result;
	}
#endif /* TESTAPP_GEN */

// This branch is not executed (which probably is platform dependent again)
#else /* !XPAR_INTC_0_DEVICE_ID */

#ifndef TESTAPP_GEN
	XScuGic_Config *IntcConfig;

	/*
	 * Initialize the interrupt controller driver so that it is ready to
	 * use.
	 */
	xil_printf("e");
	IntcConfig = XScuGic_LookupConfig(INTC_DEVICE_ID);
	if (NULL == IntcConfig) {
		return XST_FAILURE;
	}
	xil_printf("f");
	Result = XScuGic_CfgInitialize(IntcInstancePtr, IntcConfig,
					IntcConfig->CpuBaseAddress);
	if (Result != XST_SUCCESS) {
		return XST_FAILURE;
	}
#endif /* TESTAPP_GEN */
	xil_printf("g");
	XScuGic_SetPriorityTriggerType(intc, IntrId,
					0xA0, 0x3);

	/*
	 * Connect the interrupt handler that will be called when an
	 * interrupt occurs for the device.
	 */
	xil_printf("h");
	Result = XScuGic_Connect(intc, IntrId,
				 (Xil_ExceptionHandler)GpioHandler, InstancePtr);
	if (Result != XST_SUCCESS) {
		return Result;
	}

	/*
	 * Enable the interrupt for the GPIO device.
	 */
	xil_printf("i");
	XScuGic_Enable(intc, IntrId);
#endif /* XPAR_INTC_0_DEVICE_ID */

	/*
	 * Enable the GPIO channel interrupts so that push button can be
	 * detected and enable interrupts for the GPIO device
	 */
	xil_printf("j");
	XGpio_InterruptEnable(InstancePtr, IntrMask);
	xil_printf("k");
	XGpio_InterruptGlobalEnable(InstancePtr);

	/*
	 * Initialize the exception table and register the interrupt
	 * controller handler with the exception table
	 */
	xil_printf("l");
	Xil_ExceptionInit();

	xil_printf("m");
	Xil_ExceptionRegisterHandler(XIL_EXCEPTION_ID_INT,
			 (Xil_ExceptionHandler)INTC_HANDLER, intc);

	xil_printf("n");
	/* Enable non-critical exceptions */
	Xil_ExceptionEnable();

	return XST_SUCCESS;
}
