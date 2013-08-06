/**
 * Describes the state representation of an operation.
 * This includes the general state as well as the more
 * specialised write and read states.
 * States are used by ports to store unfinished tasks
 * and are also handed to the user to allow him to check
 * for progress.
 * @file
 * @author Thomas Fischer
 * @since 26.02.2013
 */
#ifndef STATE_H_
#define STATE_H_

#include <deque>
#include <bitset>
#include <math.h>
#include <vector>

#include "../logger.h"

template <int width>
class outPort;

/**
 * Abstract representation of the state of an operation.
 * These are used as return values for non-blocking calls and
 * allow users to check the progress of the call.
 */
class state {
protected:
	/** Number of 32-bit values to be written or read. */
	unsigned int size;
	/** Number of already written or read 32-bit values. */
	unsigned int done;
	/** Actual bitwidth of of values within this state */
	unsigned int w;
	/** Number of 32-bit values representing a value in the actual bitwidth of the state */
	unsigned int intPerValue;
	/** Failed flag */
	bool fail;
	/** Message of an occurred exception. */
	std::string m;
	/**
	 * Internal constructor, initialising size and done values.
	 * @param size Total number of values to be processed.
	 */
	state(int size) : done(0), w(32), fail(false), m("") {
		intPerValue = ceil((double) w / (sizeof(int) * 8));
		this->size = size * intPerValue;
	}
	/**
	 * Internal constructor, initialising size and done values.
	 * @param size Total number of values to be processed.
	 * @param width Actual bitwidth of the state
	 */
	state(int size, int width) : done(0), w(width), fail(false), m("") {
		intPerValue = ceil((double) w / (sizeof(int) * 8));
		this->size = size * intPerValue;
	}
public:
	virtual ~state() { };

	/**
	 * Checks, if a call has finished.
	 * This means, all values have been processed and is
	 * equivalent with #size == #done.
	 * @return true if the operation has been finished, false otherwise.
	 */
	bool finished() { return size == done; }

	/**
	 * Checks, how many values have already been processed.
	 * @return The number of processed values.
	 */
	unsigned int processed() {
		return done / intPerValue;
	}
	/**
	 * Checks, how many values have yet to be processed.
	 * @return The number of values NOT processed so far.
	 */
	unsigned int remaining() {
		return floor((size - done) / intPerValue);
	}
	/**
	 * Checks, how many values have to be processed in total.
	 * @return The total number of values to be processed.
	 */
	unsigned int total() {
		return size / intPerValue;
	}
};

/**
 * Abstract, unparameterised supertype of a read state read state without specified width.
 * It is to be only used by the I/O threads which are independent of the actual bitwidth of a port.
 */
class abstractWriteState : public state {
friend void scheduleWriter();
friend std::vector<int> take(std::shared_ptr<LinkedQueue<abstractWriteState>> q, unsigned int count);
friend void recv_ack_unsafe(unsigned char pid, unsigned int count);
private:
	/**
	 * Peeks at the first #count values of the state and stores them into the provided array.
	 * If less values are available, less values are read.
	 * @param val Array where to store peeked values
	 * @param count Number of values to be peeked at
	 * @return Actual number of values stored (<= #count)
	 */
	virtual unsigned int peek(int val[], unsigned int count) = 0;
public:
	/**
	 * Constructor of the abstract write state. While a width parameter is provided,
	 * it is only stored internally but does not influence write-specific behaviour here.
	 * @param size Total number of values to be processed.
	 * @param width Actual bitwidth of the state
	 */
	abstractWriteState(int size, int width) : state(size, width) { }
	virtual ~abstractWriteState() { }
};

/**
 * Abstract, unparameterised supertype of a read state read state without specified width.
 * It is to be only used by the I/O threads which are independent of the actual bitwidth of a port.
 */
class abstractReadState : public state {
friend void scheduleReader();
friend void recv_data_unsafe(unsigned char pid, int val);
private:
	/**
	 * Tries to store #count values in the read state.
	 * If the state cannot receive all values, less values are stored.
	 * @param val Array of values to be stored
	 * @param count Size of the array (i.e. number of values to be stored)
	 * @return Number of values that could fit into this state (<= #count)
	 */
	virtual unsigned int store(int val[], unsigned int count) = 0;
public:
	/**
	 * Constructor of the abstract read state. While a width parameter is provided,
	 * it is only stored internally but does not influence read-specific behaviour here.
	 * @param size Total number of values to be processed.
	 * @param width Actual bitwidth of the state
	 */
	abstractReadState(int size, int width) : state(size, width) { }
	virtual ~abstractReadState() { }
};



/**
 * Abstract representation of the state of a write operation.
 * These are used as return values for non-blocking calls and
 * allow users to check the progress of the call.
 */
template <int width>
class writeState : public abstractWriteState {
private:
	/** The values (in the states bitwidth) to be written. */
	std::bitset<width> *vals;

	// this is basically the exposed method for reading from the value queue!
	unsigned int peek(int val[], unsigned int count) {
		unsigned int read = 0;

		while(read < count) {
			if(read + done == size) break;
			std::deque<int> currentValue = convert(vals[int(floor((read + done) / intPerValue))]);
			val[read] = currentValue[(read + done) % intPerValue];
			read++;
		}

		return read;
	}

	/**
	 * Transforms a value of the states width to a deque of integer values for transmission.
	 * @param val The value represented by a bitset
	 * @return A deque of integer values representing the value
	 */
	std::deque<int> convert (const std::bitset<width> val) {
		std::deque<int> vals;
		for(unsigned int i = 0; i < ceil((double)val.size() / (sizeof(int) * 8)); i++) {
			int rslt = 0;
			for(unsigned int j = 0; j < (sizeof(int) * 8); j++) {
				if(i*(sizeof(int) * 8) + j == val.size()) break;
				else rslt += val[i*(sizeof(int) * 8) + j] << j;
			}
			vals.push_front(rslt);
		}
		return vals;
	}

public:
	/**
	 * Constructor of the write state.
	 * @param vals Array of values to be written.
	 * @param size Number of values to be written (i.e. size of the array).
	 */
	writeState(const std::bitset<width> vals[], unsigned int size) : abstractWriteState(size, width) {
		// allocate memory for the values
		this->vals = (std::bitset<width>*)malloc(this->size * sizeof(std::bitset<width>));
		// make a local copy of each value
		for(unsigned int i = 0; i < size; i++) this->vals[i] = vals[i];
	}

	~writeState() { }
};

/**
 * Abstract representation of the state of a write operation.
 * These are used as return values for non-blocking calls and
 * allow users to check the progress of the call.
 */
template <int width>
class readState : public abstractReadState {
friend class outPort<width>;
private:
	/** Memory, where read values (in the states bitwidth) should be stored. */
	std::bitset<width> *vals;

	/**
	 * The value in the states bitwidth, that is currently being read.
	 * Additional 32-bit values can be added by shifting this value beforehand.
	 */
	std::bitset<width> currentValue;
	/** Index, how many 32-bit values have been read into the current value. */
	unsigned int currentValueIndex;

	/**
	 * Stores a single integer value to the read state.
	 * This includes shifting the current value by 32-bit, adding the new integer,
	 * and - if the current value is finished - adding the current value to the
	 * value array and resetting it.
	 * @param val 32-bit value to be stored.
	 */
	void store(int val) {
		// left shift current value
		currentValue = currentValue << (sizeof(int) * 8);

		// append next integer values
		// looks dangerous, but the shift above implies, there are no collisions here...
		currentValue |= val;

		// increase value index and check for completeness of value
		if(++currentValueIndex == intPerValue) {
			// if complete, assign value
			vals[int(floor(done / intPerValue))] = currentValue;
			// reset value and index
			currentValue = 0;
			currentValueIndex = 0;
		}
		done++;
	}

	// this is basically the exposed method
	unsigned int store(int val[], unsigned int count) {
		unsigned int put = 0;
		// idea: put as many values into state as possible
		//       return number of successfully put values
		while(put < count) {
			if(put + done == size) break;

			logger_host << FINE << "storing value @ state: " << val[put] << std::endl;

			// leftshift current value
			currentValue = currentValue << (sizeof(int) * 8);

			// append next integer values
			// looks dangerous, but the shift above implies, there are no collisions here...
			currentValue |= val[put];

			// increase value index and check for completeness of value
			if(++currentValueIndex == intPerValue) {
				// if complete, assign value
				vals[int(floor((put + done) / intPerValue))] = currentValue;
				// reset value and index
				currentValue = 0;
				currentValueIndex = 0;
			}

			// increase put counter
			put++;
		}

		// update done value
		done = done + put;

		// return number of put values
		return put;
	}

public:
	/**
	 * Constructor of the read state.
	 * @param vals Memory reserved for values to be read.
	 * @param size Number of values to be read (i.e. size of the array).
	 */
	readState(std::bitset<width> vals[], unsigned int size) : abstractReadState(size, width), vals(vals) {
		currentValueIndex = 0;
	}
	~readState() { }
};

#endif /* STATE_H_ */
