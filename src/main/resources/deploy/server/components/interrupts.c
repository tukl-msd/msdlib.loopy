/*
 * @author Thomas Fischer
 * @since 04.02.2013
 */
#include "interrupts.h"

/** The interrupt controller of this driver */
static INTC intc;

INTC* getIntc() {
	return &intc;
}

