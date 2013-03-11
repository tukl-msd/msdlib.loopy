/**
 * Describes the default gpio components (LEDs, switches and buttons).
 * @file
 * @author: Thomas Fischer
 * @since: 28.01.2013
 */

#ifndef GPIO_H_
#define GPIO_H_

// pointers
#include <memory>

// locking
#include <atomic>
#include <mutex>
#include <condition_variable>

// other datatyps
#include "component.h"

class gpio : public component {
protected:
	/** State of the gpio component */
	std::atomic<int> state;
};

class gpi : public gpio {
friend void scheduleWriter();
protected:
	/** Identifier of the gpi component */
	unsigned char gpi_id;

	/**
	 * Sets the gpi state using a single integer value.
	 * @param state The new gpi state represented by a single integer value.
	 *              The value has to be in the interval [0;255].
	 */
	void writeStateInternal(int state);
public:
	gpi(unsigned char gpi_id);
	virtual ~gpi() {};

};

class gpo : public gpio {
friend void recv_gpio(unsigned char gid, unsigned char val);
protected:
	/** ID of the gpo component */
	unsigned char gpo_id;

	/** mutex of the gpo component */
	std::mutex gpo_mutex;
	std::condition_variable has_changed;

	unsigned char readStateInternal();
public:
	gpo(unsigned char gpo_id);
	virtual ~gpo() {};

	void waitForChange();
};

/**
 * An abstract representation of the LED component of the board.
 * Encapsulates communication with this component. This primarily
 * includes setting the LEDs of the board to a specific state,
 * but also a sample application demonstrating communication
 * with the board.
 * @see components.h for a list of specific gpio instances within this board driver.
 */
class leds : public gpi {
private:
	/** Pointer to the state of the led component */
//	std::shared_ptr<std::atomic<int>> state;

	bool next(bool direction, int &state);
public:
	/**
	 * Constructor for the LED component.
	 */
	leds(int gpi_id) : gpi(gpi_id) { }
	~leds() {}
	/**
	 * Sets the LED state using an array of integer values.
	 * @param state The new LED state represented by a boolean array of size 8.
	 *              Each true value lights up an LED.
	 * @returns true if the write was successful, false otherwise
	 */
	void writeState(bool state[8]);
	/**
	 * Starts the test application.
	 */
	void test();
};

/**
 * An abstract representation of the button component of the board.
 * Encapsulates communication with this component. This primarily
 * includes reading the current button state into an array.
 * @see components.h for a list of specific gpio instances within this board driver.
 */
class buttons : public gpo {
private:
	/** Pointer to the state of the button component */
//	std::shared_ptr<std::atomic<int>> state;
public:
	/**
	 * Constructor for the button component.
	 * @param gpo_id Id used for protocol and state store for this component.
	 */
	buttons(int gpo_id) : gpo(gpo_id) { }
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
class switches : public gpo {
private:
	/** Pointer to the state of the switch component */
//	std::shared_ptr<std::atomic<int>> state;
public:
	/**
	 * Constructor for the switch component.
	 * @param gpo_id Id used for protocol and state store for this component.
	 */
	switches(int gpo_id) : gpo(gpo_id){ }
	/**
	 * Reads the current switch state into a boolean array.
	 * Note that the boolean array has to be at least of size 8.
	 * @param state The boolean array, into which the state will be written.
	 * @return true if the read was successful, false otherwise.
	 */
	bool readState(bool state[8]);
};

#endif /* GPIO_H_ */
