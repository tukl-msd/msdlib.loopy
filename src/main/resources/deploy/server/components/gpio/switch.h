/*
 * switch.h
 *
 *  Created on: 04.02.2013
 *      Author: thomas
 */

#ifndef SWITCH_H_
#define SWITCH_H_

#include "xbasic_types.h"

int init_switch();
u32 read_switch();

// fwd definition from led
int set_LED(u32 state);

#endif /* SWITCH_H_ */
