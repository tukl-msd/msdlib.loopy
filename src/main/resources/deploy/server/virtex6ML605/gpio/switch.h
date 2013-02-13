/**
 * Contains procedures handling the pushbutton gpio component
 * @file
 * @author Thomas Fischer
 * @since 04.02.2013
 */

#ifndef SWITCH_H_
#define SWITCH_H_

#include "xbasic_types.h"

/**
 * Initialises the switch component.
 * @returns XST_SUCCESS, if successful.
            Otherwise an error code corresponding to the occurred error.
 */
int init_switch();
/**
 * Reads the state of the pushbutton component.
 * @returns The current state of the component.
 */
u32 read_switch();

#endif /* SWITCH_H_ */
