/**
 * Describes AXI-Stream ports that can be instantiated by components.
 * @file
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef PORT_H_
#define PORT_H_

// pointer
#include <memory>

// standard library
#include <stdio.h>

// locking
#include <mutex>
#include <condition_variable>

// data types
#include <vector>
// FIXME move queues somewhere else?
#include "../linkedQueue.h"
#include "../io/state.h"
/**
 * An abstract representation of an AXI-Stream port.
 * This marks a general port without a direction and should not be instantiated.
 */
class port {
public:
	virtual ~port() {};
};

/**
 * An abstract representation of an in-going AXI-Stream port.
 * Instances of this class represent in-going ports of IPCores.
 * Communication with a core should be handled through these ports.
 */
class in : protected port {
friend void scheduleWriter();
friend void acknowledge(unsigned char pid, unsigned int count);
friend void acknowledge_unsafe(unsigned char pid, unsigned int count);
private:
	std::shared_ptr<LinkedQueue<WriteState>> writeTaskQueue;
	std::shared_ptr<unsigned int> transit;

	std::shared_ptr<State> write(std::shared_ptr<WriteState> s);

	/** Wait until a blocking write has been finished.
	 *  Acquires the port lock and waits for a notification from the
	 *  underlying writer, that the write queue has been processed.
	 *  Since no more writes can occur after the first blocking write,
	 *  it is ensured, that the blocking write had to be the last one in
	 *  the queue.
	 *  @Warning This method is not implemented so far!
	 */
	void block();

//protected:
//	std::mutex in_port_mutex;
//	std::condition_variable in_empty;
//	std::condition_variable in_not_empty;
public:
	/**
	 * Constructor for an in-going data port.
	 * Instantiates all queues used for this port as well as pointers to them.
	 * @param pid The id of this port. The id is identical to the id used by the microblaze.
	 */
	in(int pid);
	~in() { }

	/**
	 * Writes an integer value to this port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written value.
	 * @param val The integer value to be written.
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	void write(int val);

	/**
	 * Writes a vector of integer values to this port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written values.
	 * @param val The integer vector to be written.
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	void write(std::vector<int> val);

	/**
	 * Writes an array of integer values to this port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written values.
	 * @param val The integer array to be written.
	 * @param size The size of the integer array.
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	void write(int val[], int size);


	/**
	 * Writes an integer value to this port without waiting for it to return.
	 * This still implies, that the value has not been received by the board (or not even
     * been sent to the board) yet.
	 * @param val The integer value to be written.
	 * @return A #State representing this write.
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	std::shared_ptr<State> nbwrite(int val);
	/**
	 * Writes a vector of integer values to this port without waiting for it to return.
	 * This still implies, that the value has not been received by the board (or not even
     * been sent to the board) yet.
     * @param val The integer vector to be written.
	 * @return A #State representing this write.
	 */
	std::shared_ptr<State> nbwrite(std::vector<int> val);
	/**
	 * Writes an array of integer values to this port without waiting for it to return.
	 * This still implies, that the value has not been received by the board (or not even
     * been sent to the board) yet.
	 * @param val The integer array to be written.
	 * @param size The size of the integer array.
	 * @return A #State representing this write.
	 */
	std::shared_ptr<State> nbwrite(int val[], int size);

	// declaring this method as friend has two effects:
	// a) this method has access to the classes private and protected fields
	// b) this isn't really a method.
	//    it's global (i.e. needs no qualification for being called, but
	//                 also contains no implicit reference to callee.
	//                 Instead, the callee is explicitly referenced in the first parameter

	/**
	 * Writes an integer value to a port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written value.
	 * @param i The port, the value should be written to.
	 * @param val The integer value to be written.
	 * @return The port, the value has been written to.
     *         Returning the port allows concatenating stream operations.
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	friend in& operator <<(in &i, const int val);
	/**
	 * Writes a vector of integer values to a port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written value.
	 * @param i The port, the value should be written to.
	 * @param val The vector of integers to be written.
	 * @return The port, the vector has been written to.
     *         Returning the port allows concatenating stream operations.
	 * @warning Though described as a blocking write, this method currently only blocks
	 *          until the microblaze has received the value, not until the component
	 *          has received it. This will be fixed in a later version
	 */
	friend in& operator <<(in &i, const std::vector<int> val);
};

/**
 * An abstract representation of an out-going AXI-Stream port.
 * Instances of this class represent out-going ports of IPCores.
 * Communication with a core should be handled through these ports.
 */
class out : protected port {
friend void scheduleReader();
friend void read(int pid, int val);
friend void read_unsafe(int pid, int val);
private:
	std::shared_ptr<LinkedQueue<int>> readValueQueue;
	std::shared_ptr<LinkedQueue<ReadState>> readTaskQueue;

	/**
	 * Underlying read operation, called by more usable read methods.
	 * This operation does actually block until the read is finished or
	 * no more values are available. While this does not exactly mirror
	 * the semantics of a non-blocking read, the difference in runtime
	 * should be negligible, even for larger read operations.
	 * @param s #State of the read operation to be executed.
	 * @return updated #State
	 */
	std::shared_ptr<State> read(std::shared_ptr<ReadState> &s);
	void block();
//protected:
	std::mutex out_port_mutex;
	std::condition_variable task_empty;
	std::condition_variable val_not_empty;
public:
	/**
	 * Constructor for an out-going data port.
	 * Instantiates all queues used for this port as well as pointers to them.
	 * @param pid The id of this port. The id is identical to the id used by the microblaze.
	 */
	out(int pid);
	~out() { }

	/**
	 * Reads a single integer value from this port.
	 * This is a blocking read, meaning that the reading program will wait until a value is returned.
	 * @return The read value.
	 * @throws readException if the read failed.
	 */
	int  read();

	/**
	 * Reads a single integer value from this port.
	 * This is a blocking read, meaning that the reading program will wait until a value is returned.
	 * @param val Variable, where the read value should be stored
	 * @throws readException if the read failed.
	 */
	void read(int &val);

	/**
	 * Reads several integer values from this port into a vector.
	 * This is a blocking read, meaning that the reading program will wait until a value is returned.
	 * @param val The vector, into which the values are stored.
	 * @throws readException if the read failed.
	 */
	void read(std::vector<int> &val);

	/**
	 * Reads several integer values from this port into an array.
	 * This is a blocking read, meaning that the reading program will wait until a value is returned.
	 * @param val The array, into which the values are stored.
	 * @param size The number of values that should be read and the size of the array.
	 * @throws readException if the read failed.
	 */
	void read(int val[], unsigned int size);

	/**
	 * Reads a single integer value from this port.
	 * This is a non-blocking read, meaning that the reading program does not wait for a value to be read.
	 * @param val Variable, where the read value should be stored
	 * @return A #State representing this read.
	 */
	std::shared_ptr<State> nbread(int &val);

	/**
	 * Reads several integer values from this port into a vector.
	 * This is a non-blocking read, meaning that the reading program does not wait for a value to be read.
	 * @param val The vector, into which the values are stored.
	 * @return A #State representing this read.
	 */
	std::shared_ptr<State> nbread(std::vector<int> &val);

	/**
	 * Reads several integer values from this port into an array.
	 * This is a non-blocking read, meaning that the reading program does not wait for a value to be read.
	 * @param val The array, into which the values are stored.
	 * @param size The number of values that should be read and the size of the array.
	 * @return A #State representing this read.
	 */
	std::shared_ptr<State> nbread(int val[], int size);

	/**
	 * Reads a single integer value from a port.
	 * This is a blocking read, meaning that the reading program will wait until a value is returned.
	 * @param o Port, where the value should be read from.
	 * @param val Variable, where the read value should be stored.
	 * @return The port, the value has been read from.
     *         Returning the port allows concatenating stream operations.
	 * @throws readException if the read failed.
	 */
	friend out& operator >>(out &o, int &val);
	/**
	 * Reads a vector of integer values from a port.
	 * This is a blocking read, meaning that the reading program will wait until a value is returned.
	 * @param o Port, where the vector should be read from.
	 * @param val The vector, where the read values should be stored.
	 * @return The port, the vector has been read from.
	 *         Returning the port allows concatenating stream operations.
	 * @throws readException if the read failed.
	 */
	friend out& operator >>(out &o, std::vector<int> &val);
};

/**
 * An abstract representation of a bi-directional AXI-Stream port.
 * Instances of this class represent bi-directional ports of IPCores.
 * Communication with a cores should be handled through these ports.
 */
class dual : public in, public out {
public:
	/**
	 * Constructor for a bi-directional data port.
	 * Instantiates all queues used for this port as well as pointers to them.
	 * @param wid The id of the writing part of this port. The id is identical to the id used by the microblaze.
	 * @param rid The id of the reading part of this port. The id is identical to the id used by the microblaze.
	 */
	dual(unsigned char wid, unsigned char rid) : in(wid), out(rid) { }
	~dual() { }
};

#endif /* PORT_H_ */
