/*
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef PORTOUT_H_
#define PORTOUT_H_

#include <memory>
#include <bitset>

#include "../linkedQueue.h"
#include "../io/state.h"

class abstractOutPort;
extern abstractOutPort *outPorts[];

class abstractOutPort {
friend void scheduleReader();
friend void read_unsafe(unsigned char pid, int val);
friend void read(unsigned char pid, int val[], int size);
protected:
	int pid;
	std::shared_ptr<LinkedQueue<int>> readValueQueue;
	std::shared_ptr<LinkedQueue<abstractReadState>> readTaskQueue;

	std::mutex port_mutex;
	std::condition_variable task_empty;
public:
	abstractOutPort(int pid) : pid(pid) {
		outPorts[pid] = this;

		readValueQueue = std::shared_ptr<LinkedQueue<int>>(new LinkedQueue<int>());
		readTaskQueue  = std::shared_ptr<LinkedQueue<abstractReadState>>(new LinkedQueue<abstractReadState>());
	}
	virtual ~abstractOutPort() { }
};

/**
 * An abstract representation of an out-going AXI-Stream port.
 * Instances of this class represent out-going ports of IPCores.
 * Communication with a core should be handled through these ports.
 */
template <int width>
class outPort : public abstractOutPort {
protected:

	/**
	 * Underlying read operation, called by more usable read methods.
	 * This operation does actually block until the read is finished or
	 * no more values are available. While this does not exactly mirror
	 * the semantics of a non-blocking read, the difference in runtime
	 * should be negligible, even for larger read operations.
	 * @param s #State of the read operation to be executed.
s	 */
	void read(readState<width> *state) {
		std::shared_ptr<readState<width>> s(state);

		// acquire port lock
		 std::unique_lock<std::mutex> lock(port_mutex);

		// if there are unfinished tasks in the read queue, append this one
		if(! readTaskQueue->empty()) {
			readTaskQueue->put(s);
		} else while(!s->finished()) {

			// if the result queue is empty, append this read to the task list and return
			if(readValueQueue->empty()) {
				readTaskQueue->put(s);
				break;
			}
			// otherwise, take a value, update the state and do another iteration
			int a = *readValueQueue->take();

			s->store(&a, 1);
		}

		task_empty.wait(lock);
	}

	/**
	 * Underlying read operation, called by more usable read methods.
	 * This operation does actually block until the read is finished or
	 * no more values are available. While this does not exactly mirror
	 * the semantics of a non-blocking read, the difference in runtime
	 * should be negligible, even for larger read operations.
	 * @param s #State of the read operation to be executed.
	 * @return Shared pointer to the state
	 */
	std::shared_ptr<readState<width>> nbread(readState<width> &state) {
		std::shared_ptr<readState<width>> s(state);

		// acquire port lock
		std::unique_lock<std::mutex> lock(port_mutex);

		// if there are unfinished tasks in the read queue, just append this one
		if(! readTaskQueue->empty()) {
			readTaskQueue->put(s);
			return s;
		}

		while(!s->finished()) {
			// if the result queue is empty, append this read to the task list and return
			if(readValueQueue->empty()) {
				readTaskQueue->put(s);
				return s;
			}
			// otherwise, take a value, update the state and do another iteration
			int a = *readValueQueue->take();
			s->values[s->size - s->done] = a;
			s->done++;
		}

		return s;
	}

public:
	outPort(int pid) : abstractOutPort(pid) { }
	~outPort() { }

	/**
	 * Reads a single value from this port.
	 * This is a blocking read, meaning that the reading program will wait until a value is returned.
	 * @return The read value.
	 * @throws readException if the read failed.
	 */
	std::bitset<width> read() {
		std::bitset<width> val;
		read(new readState<width>(&val, 1));
		return val;
	}

	/**
	 * Reads a single value from this port.
	 * This is a blocking read, meaning that the reading program will wait until a value is returned.
	 * @param val Variable, where the read value should be stored
	 * @throws readException if the read failed.
	 */
	void read(std::bitset<width> &val) {
		read(new readState<width>(&val, 1));
	}

	/**
	 * Reads several values from this port into a vector.
	 * This is a blocking read, meaning that the reading program will wait until a value is returned.
	 * @param val The vector, into which the values are stored.
	 * @throws readException if the read failed.
	 */
	void read(std::vector<std::bitset<width>> &vals) {
		read(new readState<width>(vals.data(), vals.size()));
	}

	/**
	 * Reads several values from this port into an array.
	 * This is a blocking read, meaning that the reading program will wait until a value is returned.
	 * @param val The array, into which the values are stored.
	 * @param size The number of values that should be read and the size of the array.
	 * @throws readException if the read failed.
	 */
	void read(std::bitset<width> vals[], unsigned int size) {
		read(new readState<width>(vals, size));
	}

	/**
	 * Reads a single value from this port.
	 * This is a non-blocking read, meaning that the reading program does not wait for a value to be read.
	 * @param val Variable, where the read value should be stored
	 * @return A #State representing this read.
	 */
	std::shared_ptr<readState<width>> nbread(std::bitset<width> &val) {
		return nbread(new readState<width>(&val, 1));
	}

	/**
	 * Reads several values from this port into a vector.
	 * This is a non-blocking read, meaning that the reading program does not wait for a value to be read.
	 * @param val The vector, into which the values are stored.
	 * @return A #State representing this read.
	 */
	std::shared_ptr<readState<width>> nbread(std::vector<std::bitset<width>> vals) {
		return nbread(new readState<width>(vals.data(), vals.size()));
	}

	/**
	 * Reads several values from this port into an array.
	 * This is a non-blocking read, meaning that the reading program does not wait for a value to be read.
	 * @param val The array, into which the values are stored.
	 * @param size The number of values that should be read and the size of the array.
	 * @return A #State representing this read.
	 */
	std::shared_ptr<readState<width>> nbread(std::bitset<width> vals[], unsigned int size) {
		return nbread(new readState<width>(vals, size));
	}

	/**
	 * Reads a single value from a port.
	 * This is a blocking read, meaning that the reading program will wait until a value is returned.
	 * @param o Port, where the value should be read from.
	 * @param val Variable, where the read value should be stored.
	 * @return The port, the value has been read from.
     *         Returning the port allows concatenating stream operations.
	 * @throws readException if the read failed.
	 */
	friend outPort& operator >>(outPort &o, std::bitset<width> &val) {
		o.read(val);
		return o;
	}
	/**
	 * Reads a vector of values from a port.
	 * This is a blocking read, meaning that the reading program will wait until a value is returned.
	 * @param o Port, where the values should be read from.
	 * @param val The vector, where the read values should be stored.
	 * @return The port, the values have been read from.
	 *         Returning the port allows concatenating stream operations.
	 * @throws readException if the read failed.
	 */
	friend outPort& operator >>(outPort &o, std::vector<std::bitset<width>> &val) {
		o.read(val);
		return o;
	}

};

#endif /* PORTOUT_H_ */
