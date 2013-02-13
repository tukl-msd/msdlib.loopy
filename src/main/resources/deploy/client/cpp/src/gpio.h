/**
 * Describes the default gpio components (LEDs, switches and buttons).
 * @file
 * @author: Thomas Fischer
 * @since: 28.01.2013
 */

#ifndef GPIO_H_
#define GPIO_H_

#include "component.h"

/**
 * An abstract representation of the LED component of the board.
 * Encapsulates communication with this component. This primarily
 * includes setting the LEDs of the board to a specific state,
 * but also a sample application demonstrating communication
 * with the board.
 * @see components.h for a list of specific gpio instances within this board driver.
 */
class leds : private component {
public:
	/**
	 * Constructor for the LED component.
	 * @param intrfc The communication medium, this components board
	 *               is attached to.
	 */
	leds(interface *intrfc) : component(intrfc) {}
	/**
	 * Sets the LED state using an array of integer values.
	 * @param state The new LED state represented by a boolean array of size 8.
	 *              Each true value lights up an LED.
	 * @returns true if the write was successful, false otherwise
	 */
	bool writeState(bool state[8]);
	/**
	 * Starts the test application.
	 */
	void test();
private:
	/**
	 * Sets the LED state using a single integer value.
	 * @param state The new LED state represented by a single integer value.
	 *              The value has to be in the interval [0;255].
	 * @returns true if the write was successful, false otherwise
	 */
	bool writeState(int state);
};

/**
 * An abstract representation of the button component of the board.
 * Encapsulates communication with this component. This primarily
 * includes reading the current button state into an array.
 * @see components.h for a list of specific gpio instances within this board driver.
 */
class buttons : private component {
public:
	/**
	 * Constructor for the button component.
	 * @param intrfc The communication medium, this components board
	 *               is attached to.
	 */
	buttons(interface *intrfc) : component(intrfc) {}
	/**
	 * Reads the current button state into a boolean array.
	 * Note that the boolean array has to be at least of size 5.
	 * Note that true does NOT mark a currently pressed button, but
	 * a change between pressed and not pressed (or vice versa).
	 * @param state The boolean array, into which the state will be written.
	 * @return true if the read was successful, false otherwise.
	 */
	bool readState(bool state[5]);
};

/**
 * An abstract representation of the switch component of the board.
 * Encapsulates communication with this component. This primarily
 * includes reading the current switch state into an array.
 * @see components.h for a list of specific gpio instances within this board driver.
 */
class switches : private component {
public:
	/**
	 * Constructor for the switch component.
	 * @param intrfc The communication medium, this components board
	 *               is attached to.
	 */
	switches(interface *intrfc) : component(intrfc) {}
	/**
	 * Reads the current switch state into a boolean array.
	 * Note that the boolean array has to be at least of size 8.
	 * @param state The boolean array, into which the state will be written.
	 * @return true if the read was successful, false otherwise.
	 */
	bool readState(bool state[8]);
};

#endif /* GPIO_H_ */
