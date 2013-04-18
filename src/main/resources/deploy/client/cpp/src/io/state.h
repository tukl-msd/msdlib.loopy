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
	unsigned int w;

	unsigned int done;

	unsigned int intPerValue;
	/** Failed flag */
	bool fail;
	/** Message of an occurred exception. */
	std::string m;
	/**
	 * Internal constructor, initialising size and done values.
	 * @param size Total number of values to be processed.
	 */
	state(int size) : w(32), done(0), fail(false), m("") {
		intPerValue = ceil((double) w / (sizeof(int) * 8));
		this->size = size * intPerValue;
	}
	state(int size, int width) : w(width), done(0), fail(false), m("") {
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

class abstractWriteState : public state {
friend void scheduleWriter();
friend std::vector<int> take(std::shared_ptr<LinkedQueue<abstractWriteState>> q, unsigned int count);
friend void acknowledge_unsafe(unsigned char pid, unsigned int count);
private:
	virtual unsigned int peek(int val[], unsigned int count) = 0;
public:
	abstractWriteState(int size, int width) : state(size, width) { }
	virtual ~abstractWriteState() { }
};

class abstractReadState : public state {
friend void scheduleReader();
friend void read_unsafe(unsigned char pid, int val);
private:
	virtual unsigned int store(int val[], unsigned int count) = 0;
public:
	abstractReadState(int size, int width) : state(size, width) { }
	virtual ~abstractReadState() { }
};

template <int width>
class writeState : public abstractWriteState {
private:
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
	writeState(std::bitset<width> vals[], unsigned int size) : abstractWriteState(size, width) {
		this->vals = (std::bitset<width>*)malloc(this->size * sizeof(int));
		for(unsigned int i = 0; i < size; i++) this->vals[i] = vals[i];
	}

	~writeState() { }
};

template <int width>
class readState : public abstractReadState {
friend class outPort<width>;
private:
	std::bitset<width> *vals;

	unsigned int currentValueIndex;
	std::bitset<width> currentValue;

	// this is basically the exposed method
	unsigned int store(int val[], unsigned int count) {
		unsigned int put = 0;
		// idea: put as many values into state as possible
		//       return number of successfully put values
		while(put < count) {
			if(put + done == size) break;

			printf("\nstoring value @ state: %d", val[put]);

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
	readState(std::bitset<width> vals[], unsigned int size) : abstractReadState(size, width), vals(vals) {
		currentValueIndex = 0;
	}
	~readState() { }
};

#endif /* STATE_H_ */
