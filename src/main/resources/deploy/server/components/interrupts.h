/**
 * Contains the interrupt controller instance of the board.
 * This instance is shared between all calls requiring an interrupt controller.
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#ifndef INTERRUPTS_H_
#define INTERRUPTS_H_

#include "xparameters.h"

#ifdef XPAR_INTC_0_DEVICE_ID
 #include "xintc.h"
 #include <stdio.h>
#else
 #include "xscugic.h"
 #include "xil_printf.h"
#endif

#ifdef XPAR_INTC_0_DEVICE_ID
 #define INTC_DEVICE_ID	XPAR_INTC_0_DEVICE_ID
 #define INTC		XIntc
 #define INTC_HANDLER	XIntc_InterruptHandler
#else
 #define INTC_DEVICE_ID	XPAR_SCUGIC_SINGLE_DEVICE_ID
 #define INTC		XScuGic
 #define INTC_HANDLER	XScuGic_InterruptHandler
#endif /* XPAR_INTC_0_DEVICE_ID */

/** Get the instance of the interrupt controller */
INTC* getIntc();

#endif /* INTERRUPTS_H_ */
