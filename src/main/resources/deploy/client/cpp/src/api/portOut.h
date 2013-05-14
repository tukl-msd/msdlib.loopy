/**
 * Describes out-going (reading) ports.
 * @file
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
/** List of all out-going ports of this driver. */
extern abstractOutPort *outPorts[];

/** Sends a data request to the specified port.
 * @param pid ID of the port, for which data is requested.
 * @param count Number of values requested.
 */
void send_poll(unsigned char pid, unsigned int count);

/**
 * Abstract, unparameterised representation of an out-going port without specified width.
 * It is to be only used by the I/O threads which are independent of the actual bitwidth of a port.
 */
class abstractOutPort {
friend void scheduleReader();
friend void read_unsafe(unsigned char pid, int val);
friend void read(unsigned char pid, int val[], int size);
protected:
	/** ID of the port. */
	int pid;
	/** Flag for polling ports. If true, port is set to polling mode (cf. documentation for more details). */
	bool polling;
	/** The queue of read tasks to be performed by the port. */
	std::shared_ptr<LinkedQueue<abstractReadState>> readTaskQueue;
	/** The queue of values that have been forwarded, but not yet read. */
	std::shared_ptr<LinkedQueue<int>> readValueQueue;

	/** Port mutex, which has to be acquired before modifying the task or value queue. */
	std::mutex port_mutex;
	/** Condition variable, waiting for the task queue to get empty. */
	std::condition_variable task_empty;
public:
	/**
	 * Constructor for unparameterised out-going ports, initialising all queues and parameters.
	 * @param pid ID of the port.
	 * @param polling Flag for polling ports. If true, port is set to polling mode (cf. documentation for more details).
	 */
	abstractOutPort(int pid, bool polling) : pid(pid), polling(polling) {
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
	 * @param state #state of the read operation to be executed.
	 */
	void read(readState<width> *state) {
		std::shared_ptr<readState<width>> s(state);

		// acquire port lock
		std::unique_lock<std::mutex> lock(port_mutex);

		// send a poll request for the read values to the board,
		// either to fill the task or re-fill the queue
		if(polling) send_poll(pid, s->total());

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
			std::shared_ptr<int> val = readValueQueue->take();

			s->store(*val);
		}

		task_empty.wait(lock);
	}

	/**
	 * Underlying read operation, called by more usable read methods.
	 * This operation does actually block until the read is finished or
	 * no more values are available. While this does not exactly mirror
	 * the semantics of a non-blocking read, the difference in runtime
	 * should be negligible, even for larger read operations.
	 * @param state #state of the read operation to be executed.
	 * @return Shared pointer to the #state
	 */
	std::shared_ptr<readState<width>> nbread(readState<width> &state) {
		std::shared_ptr<readState<width>> s(state);

		// acquire port lock
		std::unique_lock<std::mutex> lock(port_mutex);

		// send a poll request for the read values to the board,
		// either to fill the task or re-fill the queue
		if(polling) send_poll(pid, s->total());

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
	/**
	 * Constructor for out-going ports, initialising all queues and parameters.
	 * @param pid ID of the port.
	 * @param polling Flag for polling ports. If true, port is set to polling mode (cf. documentation for more details).
	 */
	outPort(int pid, bool polling) : abstractOutPort(pid, polling) { }
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
	 * @param vals The vector, into which the values are stored.
	 * @throws readException if the read failed.
	 */
	void read(std::vector<std::bitset<width>> &vals) {
		read(new readState<width>(vals.data(), vals.size()));
	}

	/**
	 * Reads several values from this port into an array.
	 * This is a blocking read, meaning that the reading program will wait until a value is returned.
	 * @param vals The array, into which the values are stored.
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
	 * @return A #state representing this read.
	 */
	std::shared_ptr<readState<width>> nbread(std::bitset<width> &val) {
		return nbread(new readState<width>(&val, 1));
	}

	/**
	 * Reads several values from this port into a vector.
	 * This is a non-blocking read, meaning that the reading program does not wait for a value to be read.
	 * @param vals The vector, into which the values are stored.
	 * @return A #state representing this read.
	 */
	std::shared_ptr<readState<width>> nbread(std::vector<std::bitset<width>> vals) {
		return nbread(new readState<width>(vals.data(), vals.size()));
	}

	/**
	 * Reads several values from this port into an array.
	 * This is a non-blocking read, meaning that the reading program does not wait for a value to be read.
	 * @param vals The array, into which the values are stored.
	 * @param size The number of values that should be read and the size of the array.
	 * @return A #state representing this read.
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
	 * @param vals The vector, where the read values should be stored.
	 * @return The port, the values have been read from.
	 *         Returning the port allows concatenating stream operations.
	 * @throws readException if the read failed.
	 */
	friend outPort& operator >>(outPort &o, std::vector<std::bitset<width>> &vals) {
		o.read(vals);
		return o;
	}

};

#endif /* PORTOUT_H_ */
