/*
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef PORTIN_H_
#define PORTIN_H_

#include <memory>
#include <bitset>
#include <mutex>

#include "../linkedQueue.h"
#include "../io/state.h"


class abstractInPort;
extern abstractInPort *inPorts[];
extern std::mutex writer_mutex;
extern std::condition_variable can_write;

class abstractInPort {
friend void scheduleWriter();
friend void acknowledge_unsafe(unsigned char pid, unsigned int count);
friend void acknowledge(unsigned char pid, unsigned int count);
friend void poll(unsigned char pid);
protected:
	int pid;
	std::shared_ptr<LinkedQueue<abstractWriteState>> writeTaskQueue;
	std::shared_ptr<int> transit;

	std::mutex port_mutex;
	std::condition_variable_any task_empty;
public:
	abstractInPort(int pid) : pid(pid), transit(new int(0)){
		inPorts[pid] = this;

		writeTaskQueue = std::shared_ptr<LinkedQueue<abstractWriteState>>(new LinkedQueue<abstractWriteState>());
	}
	virtual ~abstractInPort() { }
};

template <int width>
class inPort : public abstractInPort {
protected:

	/**
	 * Underlying read operation, called by more usable read methods.
	 * This operation does actually block until the read is finished or
	 * no more values are available.
	 * @param s #State of the read operation to be executed.
	 * @return updated #State
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
	 * @param s #State of the read operation to be executed.
	 * @return updated #State
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
	inPort(int pid) : abstractInPort(pid) { }
	~inPort() { }

	/**
	 * Writes a bit vector to this port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written value.
	 * @param val The bit vector to be written.
	 * @throws protocolException  Indicates a problem with message encoding.
	 *                            This should not happen, when using this port interface.
	 * @throws interfaceException Indicates a problem with the communication medium.
	 *                            This usually means, that the connection to the board
	 *                            has been lost for some reason (and therefore, the driver
	 *                            has failed completely).
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	void write(std::bitset<width> val) {
		write(new writeState<width>(&val, 1));
	}

	/**
	 * Writes a vector of bit vectors to this port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written values.
	 * @param vals The bit vectors to be written.
	 * @throws protocolException  Indicates a problem with message encoding.
	 *                            This should not happen, when using this port interface.
	 * @throws interfaceException Indicates a problem with the communication medium.
	 *                            This usually means, that the connection to the board
	 *                            has been lost for some reason (and therefore, the driver
	 *                            has failed completely).
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	void write(std::vector<std::bitset<width>> vals) {
		write(new writeState<width>(vals.data(), vals.size()));
	}

	/**
	 * Writes an array of bit vectors to this port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written values.
	 * @param vals The bit vectors to be written.
	 * @param size The size of the bit vector array.
	 * @throws protocolException  Indicates a problem with message encoding.
	 *                            This should not happen, when using this port interface.
	 * @throws interfaceException Indicates a problem with the communication medium.
	 *                            This usually means, that the connection to the board
	 *                            has been lost for some reason (and therefore, the driver
	 *                            has failed completely).
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	void write(std::bitset<width> vals[], unsigned int size) {
		write(new writeState<width>(vals, size));
	}

	/**
	 * Writes a bit vector value to this port without waiting for it to return.
	 * This still implies, that the value has not been received by the board (or not even
     * been sent to the board) yet.
	 * @param val The bit vector to be written.
	 * @return A #State representing this write.
	 */
	std::shared_ptr<writeState<width>> nbwrite(std::bitset<width> val) {
		return nbwrite(new writeState<width>(&val, 1));
	}

	/**
	 * Writes a vector of bit vectors to this port without waiting for it to return.
	 * This still implies, that the value has not been received by the board (or not even
     * been sent to the board) yet.
     * @param vals The bit vectors to be written.
	 * @return A #State representing this write.
	 */
	std::shared_ptr<writeState<width>> nbwrite(std::vector<std::bitset<width>> vals) {
		return nbwrite(new writeState<width>(vals.data(), vals.size()));
	}

	/**
	 * Writes an array of bit vectors to this port without waiting for it to return.
	 * This still implies, that the value has not been received by the board (or not even
     * been sent to the board) yet.
	 * @param vals The bit vector array to be written.
	 * @param size The size of the bit vector array.
	 * @return A #State representing this write.
	 */
	std::shared_ptr<writeState<width>> nbwrite(std::bitset<width> vals[], unsigned int size) {
		return nbwrite(new writeState<width>(vals, size));
	}

	/**
	 * Writes a bit vector to a port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written value.
	 * @param i The port, the value should be written to.
	 * @param val The bit vector to be written.
	 * @return The port, the vector has been written to.
     *         Returning the port allows concatenating stream operations.
	 * @throws protocolException  Indicates a problem with message encoding.
	 *                            This should not happen, when using this port interface.
	 * @throws interfaceException Indicates a problem with the communication medium.
	 *                            This usually means, that the connection to the board
	 *                            has been lost for some reason (and therefore, the driver
	 *                            has failed completely).
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
	 * @throws protocolException  Indicates a problem with message encoding.
	 *                            This should not happen, when using this port interface.
	 * @throws interfaceException Indicates a problem with the communication medium.
	 *                            This usually means, that the connection to the board
	 *                            has been lost for some reason (and therefore, the driver
	 *                            has failed completely).
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
