/*
 * led.c
 *
 *  Created on: 01.02.2013
 *      Author: thomas
 */

#include "led.h"
#include "xparameters.h"
#include "gpio.h"

static XGpio leds;

int init_LED() {
	xil_printf("leds\n");
	int status = XGpio_Initialize(&leds, XPAR_LEDS_8BITS_DEVICE_ID);
	if(status != XST_SUCCESS) { } // TODO do something
	XGpio_SetDataDirection(&leds, GPIO_CHANNEL1, 0x0);
	return XST_SUCCESS;
}

int set_LED ( u32 value ) {
    XGpio_DiscreteWrite(&leds, GPIO_CHANNEL1, value);
    return XST_SUCCESS;
}

