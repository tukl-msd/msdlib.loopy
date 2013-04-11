/*
 * @author Thomas Fischer
 * @since 04.02.2013
 */

#include "button.h"
#include "gpio.h"
#include "xparameters.h"
#include "../../constants.h"

static XGpio buttons;

// forward declaration of the callback procedure
void callback_buttons();

/**
 * The Xil_ExceptionHandler to be used as callback for the button component.
 * This handler calls the user-defined callback method and clears the interrupt.
 */
void GpioHandlerButtons ( void *CallbackRef ) {
    XGpio *GpioPtr = (XGpio *)CallbackRef;

    // execute user-defined callback procedure
    callback_buttons();

    // Clear the Interrupt
    XGpio_InterruptClear(GpioPtr, GPIO_CHANNEL1);
}

u32 read_buttons() {
    return XGpio_DiscreteRead(&buttons, GPIO_CHANNEL1);
}

int init_buttons() {
	if(DEBUG) xil_printf("initialise buttons ...\n");
	int status = XGpio_Initialize(&buttons, XPAR_PUSH_BUTTONS_5BITS_DEVICE_ID);
	XGpio_SetDataDirection(&buttons,  GPIO_CHANNEL1, 0xFFFFFFFF);
	status = GpioIntrSetup(&buttons, XPAR_PUSH_BUTTONS_5BITS_DEVICE_ID,
			XPAR_MICROBLAZE_0_INTC_PUSH_BUTTONS_5BITS_IP2INTC_IRPT_INTR, GPIO_CHANNEL1, GpioHandlerButtons);
	return status;
}

