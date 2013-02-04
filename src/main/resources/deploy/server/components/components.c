/*
 * components.c
 *
 *  Created on: 01.02.2013
 *      Author: thomas
 */

#include "components.h"
#include "gpio/gpio.h"
#include "xparameters.h"
#include "xstatus.h"
#include "xintc.h"
#include "xil_exception.h"

int init_LED();
int init_button();
int init_switch();

u32 read_switch();
u32 read_button();
void set_LED(u32 state);

void init_components() {

	// initialise gpio components
	init_LED();
	init_button();
	init_switch();

}

void callbackButtons() {
    // Test application: print out some text
    xil_printf("\nhey - stop pushing!! %d", read_button());
}

void callbackSwitches() {
    // Test application: set LED state to Switch state
    set_LED(read_switch());

}

//void set_LED(u32 state) {
//	xil_printf("\n setting LED state to %d", state);
//}
