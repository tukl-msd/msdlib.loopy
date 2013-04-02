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

#include <vector>
#include <memory>
#include "../linkedQueue.h"

/**
 * Abstract representation of the state of an operation.
 * These are used as return values for non-blocking calls and
 * allow users to check the progress of the call.
 */
class State {
protected:
	/** Number of values to be written or read. */
	unsigned int size;
	/** Number of already written or read values. */
	unsigned int done;
	/** Failed flag */
	bool fail;
	/** Message of an occurred exception. */
	std::string m;
	/**
	 * Internal constructor, initialising size and done values.
	 * @param size Total number of values to be processed.
	 */
	State(int size) : size(size), done(0), fail(false), m("") { }
public:
	virtual ~State() { };

	/**
	 * Checks, if a call has finished.
	 * This means, all values have been processed and is
	 * equivalent with #size == #done.
	 * @return true if the operation has been finished, false otherwise.
	 */
	bool finished();

	/*
	 * Checks, if a call has failed.
	 * This indicates some sort error in the driver.
	 * A description of what went wrong is available using message().
	 * @return true if the operation failed, false otherwise.
	 */
//	bool failed();

	/*
	 * If failed() is true, a description of what went wrong is
	 * available using this method.
	 * @return A description of what went wrong,
	 *         an empty string if operation has not failed.
	 */
//	std::string message();

	/**
	 * Checks, how many values have already been processed.
	 * @return The number of processed values.
	 */
	unsigned int processed();
	/**
	 * Checks, how many values have yet to be processed.
	 * @return The number of values NOT processed so far.
	 */
	unsigned int remaining();
	/**
	 * Checks, how many values have to be processed in total.
	 * @return The total number of values to be processed.
	 */
	unsigned int total();
};

/**
 * A more specialised state for write operations.
 * It should only be used internally and not be leaked to the user.
 * The write state contains a copy of the data to be written and
 * is used to buffer unwritten data.
 */
class WriteState : public State {
friend class in;
friend class dual;
friend class QueueElem;
friend void scheduleWriter();
friend void acknowledge_unsafe(unsigned char cid, unsigned int count);
friend std::vector<int> take(std::shared_ptr<LinkedQueue<WriteState>> q, unsigned int count);
private:
	/** Pointer to values to be written. */
	int *values;
public:
	/**
	 * Instantiates a write state with a single value.
	 * @param val The value to be written.
	 */
	WriteState(int val);
	/**
	 * Instantiates a write state with a vector of values.
	 * @param val The values to be written.
	 */
	WriteState(std::vector<int> val);
	/**
	 * Instantiates a write state with an array of values.
	 * @param val The values to be written.
	 * @param size The number of values to be written.
	 */
	WriteState(int val[], int size);
	~WriteState();
};

/**
 * A more specialised state for read operations.
 * It should only be used internally and not be leaked to the user.
 * The read state contains a pointer to a memory area, to where a value
 * should be read and is used to buffer unfinished read tasks.
 */
class ReadState : public State {
friend class out;
friend void scheduleReader();
friend void read_unsafe(unsigned char pid, int val);
private:
	/** Pointer to storage for read values. */
	int *values;
public:
	/**
	 * Instantiates a read state for a single value.
	 * @param val Pointer to where the value should be read to.
	 */
	ReadState(int *val);
	/**
	 * Instantiates a read state for a vector of values.
	 * @param val Pointer to where the values should be read to.
	 */
	ReadState(std::vector<int> *val);
	/**
	 * Instantiates a read state for an array of values.
	 * @param val Pointer to where the values should be read to.
	 * @param size Number of elements to be read.
	 */
	ReadState(int val[], int size);
	~ReadState();
};

#endif /* STATE_H_ */
