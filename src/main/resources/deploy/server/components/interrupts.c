/*
 * interrupts.c
 *
 *  Created on: 04.02.2013
 *      Author: thomas
 */
#include "interrupts.h"

static INTC intc; /* The Instance of the Interrupt Controller Driver */

INTC* getIntc() {
	return &intc;
}

