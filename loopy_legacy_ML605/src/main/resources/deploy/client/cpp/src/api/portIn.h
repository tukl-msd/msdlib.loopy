/**
 * Describes in-going (writing) ports.
 * @file
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef PORTIN_H_
#define PORTIN_H_

#include <memory>
#include <bitset>
#include <mutex>

#include "../utils.h"
#include "../linkedQueue.h"
#include "../io/state.h"

class abstractInPort;
/** List of all in-going ports of this driver. */
extern abstractInPort *inPorts[];
/** The writer mutex, which is required for notification of the writer. */
extern std::mutex writer_mutex;
/** Condition variable of the writer, which is notified, if a new value is written to an idling port. */
extern std::condition_variable can_write;

/**
 * Abstract, unparameterised representation of an in-going port without specified width.
 * It is to be only used by the I/O threads which are independent of the actual bitwidth of a port.
 */
class abstractInPort {
friend void scheduleWriter();
friend void recv_ack_unsafe(unsigned char pid, unsigned int count);
friend void recv_ack(unsigned char pid, unsigned int count);
friend void recv_poll(unsigned char pid);
protected:
	/** ID of the port. */
	int pid;
	/** The queue of write tasks to be performed by the port. */
	std::shared_ptr<LinkedQueue<abstractWriteState>> writeTaskQueue;
	/** Counter of values currently in transit. -1 marks a blocked port. */
	std::shared_ptr<int> transit;

	/** Port mutex, which has to be acquired before modifying the task queue. */
	std::mutex port_mutex;
	/** Condition variable, waiting for the task queue to get empty. */
	std::condition_variable_any task_empty;
public:
	/**
	 * Constructor for unparameterised in-going ports, initialising all queues and parameters.
	 * @param pid ID of the port.
	 */
	abstractInPort(int pid) : pid(pid), transit(new int(0)){
		inPorts[pid] = this;

		writeTaskQueue = std::shared_ptr<LinkedQueue<abstractWriteState>>(new LinkedQueue<abstractWriteState>());
	}
	virtual ~abstractInPort() { }
};

/**
 * An abstract representation of an in-going AXI-Stream port.
 * Instances of this class represent in-going ports of IPCores.
 * Communication with a core should be handled through these ports.
 */
template <int width>
class inPort : public abstractInPort {
protected:

	/**
	 * Underlying read operation, called by more usable read methods.
	 * This operation does actually block until the read is finished or
	 * no more values are available.
	 * @param state #state of the read operation to be executed.
	 * @return updated #state
	 */
	void write(writeState<width> *state) {
		std::shared_ptr<writeState<width>> s(state);

		// acquire port lock
		std::unique_lock<std::mutex> port_lock(port_mutex);

		// put the value in the queue
		writeTaskQueue->put(s);

		// acquire writer lock
		std::unique_lock<std::mutex> write_lock(writer_mutex);

		// notify, if the port is ready (may notify for tasks further ahead in queue, but doesn't matter)
		if(*transit == 0) can_write.notify_one();

		// release the port lock!
		port_lock.unlock();

		// wait for the queue to get empty
		task_empty.wait(write_lock);
	}

	/**
	 * Underlying read operation, called by more usable read methods.
	 * This operation does not block until the value is written.
	 * Still, it waits for locks and may therefore take some time to finish.
	 * @param state #state of the read operation to be executed.
	 * @return updated #state
	 */
	std::shared_ptr<writeState<width>> nbwrite(writeState<width> *state) {
		std::shared_ptr<writeState<width>> s(state);

		// acquire port lock
		std::unique_lock<std::mutex> port_lock(port_mutex);

		// put the value in the queue
		writeTaskQueue->put(s);

		// acquire writer lock
		std::unique_lock<std::mutex> write_lock(writer_mutex);

		// notify, if the port is ready
		if(*transit == 0) can_write.notify_one();

		// release locks and return state pointer
		return s;
	}

public:
	/**
	 * Constructor for in-going ports, initialising all queues and parameters.
	 * @param pid ID of the port.
	 */
	inPort(int pid) : abstractInPort(pid) { }
	~inPort() { }

	/**
	 * Writes a bit vector to this port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written value.
	 * @param val The bit vector to be written.
	 * @throws protocolException Indicates a problem with message encoding.
	 *                           This should not happen, when using this port interface.
	 * @throws mediumException   Indicates a problem with the communication medium.
	 *                           This usually means, that the connection to the board
	 *                           has been lost for some reason (and therefore, the driver
	 *                           has failed completely).
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	void write(const std::bitset<width> val) {
		write(new writeState<width>(&val, 1));
	}

	/**
	 * Writes a vector of bit vectors to this port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written values.
	 * @param vals The bit vectors to be written.
	 * @throws protocolException Indicates a problem with message encoding.
	 *                           This should not happen, when using this port interface.
	 * @throws mediumException   Indicates a problem with the communication medium.
	 *                           This usually means, that the connection to the board
	 *                           has been lost for some reason (and therefore, the driver
	 *                           has failed completely).
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	void write(const std::vector<std::bitset<width>> vals) {
		write(new writeState<width>(vals.data(), vals.size()));
	}

	/**
	 * Writes an array of bit vectors to this port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written values.
	 * @param vals The bit vectors to be written.
	 * @param size The size of the bit vector array.
	 * @throws protocolException Indicates a problem with message encoding.
	 *                           This should not happen, when using this port interface.
	 * @throws mediumException   Indicates a problem with the communication medium.
	 *                           This usually means, that the connection to the board
	 *                           has been lost for some reason (and therefore, the driver
	 *                           has failed completely).
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	void write(const std::bitset<width> vals[], unsigned int size) {
		write(new writeState<width>(vals, size));
	}

	/**
	 * Writes bit vectors from a file to this port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written values.
	 *
	 * @see utils.h for the underlying read_file operation.
	 * @param file File containing the values to be written.
	 * @param delim Separation character between two values.
	 * @param f Function formatting values read from the file (cf ios_base.h).
	 * @throws protocolException Indicates a problem with message encoding.
	 *                           This should not happen, when using this port interface.
	 * @throws mediumException   Indicates a problem with the communication medium.
	 *                           This usually means, that the connection to the board
	 *                           has been lost for some reason (and therefore, the driver
	 *                           has failed completely).
	 * @throws invalidArgument If the provided file does not exist.
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	void write(const char *file, const char delim, std::ios_base& (*f)(std::ios_base&)) {
	    std::vector<std::bitset<width>> vals = read_file<width>(file, delim, f);
	    write(vals);
	}

	/**
	 * Writes a bit vector value to this port without waiting for it to return.
	 * This still implies, that the value has not been received by the board (or not even
     * been sent to the board) yet.
	 * @param val The bit vector to be written.
	 * @return A #state representing this write.
	 */
	std::shared_ptr<writeState<width>> nbwrite(const std::bitset<width> val) {
		return nbwrite(new writeState<width>(&val, 1));
	}

	/**
	 * Writes a vector of bit vectors to this port without waiting for it to return.
	 * This still implies, that the value has not been received by the board (or not even
     * been sent to the board) yet.
     * @param vals The bit vectors to be written.
	 * @return A #state representing this write.
	 */
	std::shared_ptr<writeState<width>> nbwrite(const std::vector<std::bitset<width>> vals) {
		return nbwrite(new writeState<width>(vals.data(), vals.size()));
	}

	/**
	 * Writes an array of bit vectors to this port without waiting for it to return.
	 * This still implies, that the value has not been received by the board (or not even
     * been sent to the board) yet.
	 * @param vals The bit vector array to be written.
	 * @param size The size of the bit vector array.
	 * @return A #state representing this write.
	 */
	std::shared_ptr<writeState<width>> nbwrite(const std::bitset<width> vals[], unsigned int size) {
		return nbwrite(new writeState<width>(vals, size));
	}

    /**
     * Writes bit vectors from a file to this port without waiting for it to return.
     * This still implies, that the value has not been received by the board (or not even
     * been sent to the board) yet.
     * @see utils.h for the underlying read_file operation.
     * @param file File containing the values to be written.
     * @param delim Separation character between two values.
     * @param f Function formatting values read from the file (cf ios_base.h).
     * @return A #state representing this write.
     * @throws invalidArgument If the provided file does not exist.
     */
	std::shared_ptr<writeState<width>> nbwrite(const char *file, const char delim, std::ios_base& (*f)(std::ios_base&)) {
        std::vector<std::bitset<width>> vals = read_file<width>(file, delim, f);
        return nbwrite(vals);
    }

	/**
	 * Writes a bit vector to a port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written value.
	 * @param i The port, the value should be written to.
	 * @param val The bit vector to be written.
	 * @return The port, the vector has been written to.
     *         Returning the port allows concatenating stream operations.
	 * @throws protocolException Indicates a problem with message encoding.
	 *                           This should not happen, when using this port interface.
	 * @throws mediumException   Indicates a problem with the communication medium.
	 *                           This usually means, that the connection to the board
	 *                           has been lost for some reason (and therefore, the driver
	 *                           has failed completely).
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	friend inPort& operator <<(inPort &i, const std::bitset<width> val) {
		i.write(val);
		return i;
	}

	/**
	 * Writes a vector of bit vectors to a port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written value.
	 * @param i The port, the value should be written to.
	 * @param vals The bit vectors to be written.
	 * @return The port, the vectors have been written to.
     *         Returning the port allows concatenating stream operations.
	 * @throws protocolException Indicates a problem with message encoding.
	 *                           This should not happen, when using this port interface.
	 * @throws mediumException   Indicates a problem with the communication medium.
	 *                           This usually means, that the connection to the board
	 *                           has been lost for some reason (and therefore, the driver
	 *                           has failed completely).
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	friend inPort& operator <<(inPort &i, const std::vector<std::bitset<width>> vals) {
		i.write(vals);
		return i;
	}
};

#endif /* PORTIN_H_ */
