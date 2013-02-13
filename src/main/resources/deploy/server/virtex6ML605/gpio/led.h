/**
 * Contains procedures handling the LED gpio component
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#ifndef LED_C_
#define LED_C_

#include "xbasic_types.h"

/**
 * Initialises the LED component.
 * @returns XST_SUCCESS, if successful.
            Otherwise an error code corresponding to the occurred error.
 */
int init_LED();
/**
 * Writes the state of the LED component.
 * @param value The new state for the component.
 */
int set_LED ( u32 value );

#endif /* LED_C_ */
