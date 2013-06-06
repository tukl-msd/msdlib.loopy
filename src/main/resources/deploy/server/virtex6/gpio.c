/*
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#include "gpio.h"
#include "interrupts.h"

static u16 GlobalIntrMask; /* GPIO channel mask that is needed by the Interrupt Handler */

static volatile u32 IntrFlag; /* Interrupt Handler Flag */

int gpio_write(int target, int val) {
    XGpio_DiscreteWrite(&gpo_components[target], GPIO_CHANNEL1, val);
    return XST_SUCCESS;
}

int gpio_read(int target) {
    return XGpio_DiscreteRead(&gpi_components[target], GPIO_CHANNEL1);
}

int GpioIntrSetup(XGpio *InstancePtr, u16 DeviceId, u16 IntrId, u16 IntrMask, Xil_ExceptionHandler GpioHandler) {
	int Result;

	GlobalIntrMask = IntrMask;

#ifdef XPAR_INTC_0_DEVICE_ID

#ifndef TESTAPP_GEN
	INTC *intc = getIntc();

	/*
	 * Initialise the interrupt controller driver so that it's ready to use.
	 * specify the device ID that was generated in xparameters.h
	 */
	// This should already be done by platform.c
//	Result = XIntc_Initialize(IntcInstancePtr, INTC_DEVICE_ID);
//	if (Result != XST_SUCCESS) return Result;

#endif /* TESTAPP_GEN */

	/* Hook up interrupt service routine */
	XIntc_Connect(intc, IntrId, GpioHandler, InstancePtr);

	/* Enable the interrupt vector at the interrupt controller */
	XIntc_Enable(intc, IntrId);

#ifndef TESTAPP_GEN
	/* Start the interrupt controller such that interrupts are recognised and handled by the processor  */
	Result = XIntc_Start(intc, XIN_REAL_MODE);
	if (Result != XST_SUCCESS) return Result;

#endif /* TESTAPP_GEN */

// This branch is not executed (which probably is platform dependent again)
#else /* !XPAR_INTC_0_DEVICE_ID */

#ifndef TESTAPP_GEN
	XScuGic_Config *IntcConfig;

	/* Initialise the interrupt controller driver so that it is ready to use. */
	IntcConfig = XScuGic_LookupConfig(INTC_DEVICE_ID);
	if (NULL == IntcConfig) return XST_FAILURE;

	Result = XScuGic_CfgInitialize(IntcInstancePtr, IntcConfig, IntcConfig->CpuBaseAddress);
	if (Result != XST_SUCCESS) return XST_FAILURE;
#endif /* TESTAPP_GEN */
	XScuGic_SetPriorityTriggerType(intc, IntrId,
					0xA0, 0x3);

	/* Connect the interrupt handler that will be called when an interrupt occurs for the device. */
	Result = XScuGic_Connect(intc, IntrId, (Xil_ExceptionHandler)GpioHandler, InstancePtr);
	if (Result != XST_SUCCESS) return Result;


	/* Enable the interrupt for the GPIO device. */
	XScuGic_Enable(intc, IntrId);
#endif /* XPAR_INTC_0_DEVICE_ID */

	/*
	 * Enable the GPIO channel interrupts so that push button can be
	 * detected and enable interrupts for the GPIO device
	 */
	XGpio_InterruptEnable(InstancePtr, IntrMask);
	XGpio_InterruptGlobalEnable(InstancePtr);

	/*
	 * Initialise the exception table and register the interrupt
	 * controller handler with the exception table
	 */
	Xil_ExceptionInit();

	Xil_ExceptionRegisterHandler(XIL_EXCEPTION_ID_INT, (Xil_ExceptionHandler)INTC_HANDLER, intc);

	/* Enable non-critical exceptions */
	Xil_ExceptionEnable();

	return XST_SUCCESS;
}
