/**
 * Contains procedures handling the pushbutton gpio component
 * @file
 * @author Thomas Fischer
 * @since 04.02.2013
 */

#ifndef BUTTON_H_
#define BUTTON_H_

#include "xbasic_types.h"

/**
 * Initialises the pushbutton component.
 * @returns XST_SUCCESS, if successful.
            Otherwise an error code corresponding to the occurred error.
 */
int init_button();
/**
 * Reads the state of the pushbutton component.
 * @returns The current state of the component.
 */
u32 read_button ();

#endif /* BUTTON_H_ */
