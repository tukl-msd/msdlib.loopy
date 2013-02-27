/*
 * gpio.cpp
 *
 *  Created on: 28.01.2013
 *      Author: thomas
 */

// header file
#include "gpio.h"

// standard library
#include <unistd.h>
#include <stdio.h>
#include <math.h>

// other datatypes
#include "../constants.h"
#include "../io/io.h"

/**
 * Minimal value for the LED test application.
 * This value marks the two least significant LEDs to be set.
 */
#define MIN_VALUE 3
/**
 * Maximal value for the LED test application.
 * This value marks the two most significant LEDs to be set.
 */
#define MAX_VALUE 192

/**
 * Sets the next LED out of the current LED state and a direction. The next
 * LED state is considered to be the current state shifted one LED into direction
 * if possible, otherwise shifted in the opposite direction.
 *
 * @param direction The current direction, into which the LEDs should are shifted.
 *        0 will shift to the right, 1 will shift to the left (I guess)
 * @param state Pointer to the current LED state. The state will be changed by
 *        this procedure
 * @return the direction for the next step of LED shifting.
 */
bool next(bool direction, int &state) {
	if(direction) {
		if(state >= MAX_VALUE) return next(!direction, state);
		state = state * 2;
	} else {
		if(state <= MIN_VALUE) return next(!direction, state);
		state = state / 2;
	}
	return direction;
}

// conversion from "boolean" array to single value
int convertToInt(bool values[8]) {

	if(DEBUG) printf("\n  converting 8-bit array to single value ...");

	int i, rslt = 0;
	for(i = 0; i < 8; i++)
		if(values[i]) rslt = rslt + (int)pow(2, i);

	if(DEBUG) printf(" done\n  hex value after conversion: 0x%X", rslt);

	return rslt;
}

/**
 * Converts a single integer value to a binary number represented by a boolean array.
 * true marks a 1 value, false a 0 value.
 * @param size size of the binary number and therefore boolean array
 * @param values pointer to the array where the result will be stored
 * @param val the integer number which will be converted
 * @returns returns true if the conversion was successful, false otherwise
 */
bool convertToArr(int size, bool values[], int val) {
	if(DEBUG) printf("\n    converting single value to %d-bit array ...", size);

	// check, if given value is to large for specified array
	if(val >= (int)pow(2,size)) {
		if(DEBUG) printf("\n  ERROR: value was to large for given array");
		return 1;
	}

	// set values array accordingly
	int i;
	for(i = size-1; i >= 0; i--) {
		int cur = (int)pow(2,i);
		if(val / cur > 0) {
			values[i] = true;
			val -= cur;
		} else values[i] = false;
	}

	if(DEBUG) {
		printf(" done\n  value after conversion: [");
		for(i = 0; i < size; i++) printf(" %d", values[i]);
		printf(" ]");
	}

	return 0;
}

void leds::writeState(bool state[8]) {
	this->writeState(convertToInt(state));
}

void leds::writeState(int state) {
	// acquire writer lock
	std::unique_lock<std::mutex> lock(writer_mutex);

	// write the new state atomically (yay)
	*(this->state) = state;
	// notify (doesn't matter, if it was written before... then we just notified twice. woohoo
	can_write.notify_one();

}

bool switches::readState(bool state[8]) {
	return true;
}

bool buttons::readState(bool state[5]) {
	return true;
}

void leds::test() {
//	if(DEBUG) printf("\nstarting hopp lwip led test ...\n");
	bool direction = false;
	int state = MIN_VALUE;

	int i = 0;
	while(i < 13) {
		this->writeState(state);
		direction = next(direction, state);
		i++;
		usleep(175000);
	}
}



