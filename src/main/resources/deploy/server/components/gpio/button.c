/*
 * button.c
 *
 *  Created on: 04.02.2013
 *      Author: thomas
 */

#include "button.h"
#include "gpio.h"
#include "xparameters.h"

static XGpio buttons;

void callbackButtons();

void GpioHandlerButtons ( void *CallbackRef ) {
    XGpio *GpioPtr = (XGpio *)CallbackRef;

    // execute user-defined callback procedure
    callbackButtons();

    // Clear the Interrupt
    XGpio_InterruptClear(GpioPtr, GPIO_CHANNEL1);
}

u32 read_button ( ) {
    return XGpio_DiscreteRead(&buttons, GPIO_CHANNEL1);
}

int init_button() {
	xil_printf("buttons\n");
	int status = XGpio_Initialize(&buttons, XPAR_PUSH_BUTTONS_5BITS_DEVICE_ID);
	XGpio_SetDataDirection(&buttons,  GPIO_CHANNEL1, 0xFFFFFFFF);
	status = GpioIntrSetup(&buttons, XPAR_PUSH_BUTTONS_5BITS_DEVICE_ID,
			XPAR_MICROBLAZE_0_INTC_PUSH_BUTTONS_5BITS_IP2INTC_IRPT_INTR, GPIO_CHANNEL1, GpioHandlerButtons);
	return status;
}

