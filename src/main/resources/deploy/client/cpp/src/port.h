/**
 * Describes AXI-Stream ports that can be instantiated by components.
 * @file
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef PORT_H_
#define PORT_H_

#include <ostream>
#include <vector>
#include "interface.h"

/** An Exception that marks a failed write operation on a port. */
class writeException {};
/** An Exception that marks a failed read operation on a port.  */
class readException {};

class state {
friend class in;
friend class out;
friend class dual;
private:
	int *values;
	int size, done;
public:
	state(int val);
	state(std::vector<int> val);
	state(int val[], int size);
	~state();
	bool isFinished();
	int  processedValues();
};

/**
 * An abstract representation of an AXI-Stream port.
 * This marks a general port without a direction and should not be instantiated.
 */
class port {
protected:
	interface *intrfc;
public:
	virtual ~port() {};
};

/**
 * An abstract representation of an in-going AXI-Stream port.
 * Instances of this class represent in-going ports of IPCores.
 * Communication with a core should be handled through these ports.
 */
class in : protected port {
private:
	state *writeQueue;
public:
	in(interface *intrfc);
	~in();

	/**
	 * Writes an integer value to this port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written value.
	 * @param val The integer value to be written.
	 */
	void write(int val);

	/**
	 * Writes a vector of integer values to this port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written values.
	 * @param val The integer vector to be written.
	 */
	void write(std::vector<int> val);

	/**
	 * Writes an array of integer values to this port and waits for the write to return.
	 * This implies waiting for the board to receive and acknowledge the written values.
	 * @param val The integer array to be written.
	 * @param size The size of the integer array.
	 */
	void write(int val[], int size);


	/**
	 * Writes an integer value to this port without waiting for it to return.
	 * This still implies, that the value has not been received by the board (or not even
     * been sent to the board) yet.
	 * @param val The integer value to be written.
	 * @return A #state representing this write.
	 */
	state* nbwrite(int val);
	/**
	 * Writes a vector of integer values to this port without waiting for it to return.
	 * This still implies, that the value has not been received by the board (or not even
     * been sent to the board) yet.
     * @param val The integer vector to be written.
	 * @return A #state representing this write.
	 */
	state* nbwrite(std::vector<int> val);
	/**
	 * Writes an array of integer values to this port without waiting for it to return.
	 * This still implies, that the value has not been received by the board (or not even
     * been sent to the board) yet.
	 * @param val The integer array to be written.
	 * @param size The size of the integer array.
	 * @return A #state representing this write.
	 */
	state* nbwrite(int val[], int size);

	// declaring this method as friend has two effects:
	// a) this method has access two the classes private and protected fields
	// b) this isn't really a method.
	//    it's global (i.e. needs no qualification for being called, but
	//                 also contains no implicit reference to callee.
	//                 Instead, the callee is explicitly referenced in the first parameter
	friend in& operator <<(in &i, const int val);
	friend in& operator <<(in &i, const std::vector<int> val);
};

/**
 * An abstract representation of an out-going AXI-Stream port.
 * Instances of this class represent out-going ports of IPCores.
 * Communication with a core should be handled through these ports.
 */
class out : protected port {
private:
	state *readQueue;
public:
	out(interface *intrfc);
	~out();

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
	void read(int val[], int size);

	/**
	 * Reads a single integer value from this port.
	 * This is a non-blocking read, meaning that the reading program does not wait for a value to be read.
	 * @param val Variable, where the read value should be stored
	 * @return A #state representing this read.
	 */
	state* nbread(int &val);

	/**
	 * Reads several integer values from this port into a vector.
	 * This is a non-blocking read, meaning that the reading program does not wait for a value to be read.
	 * @param val The vector, into which the values are stored.
	 * @return A #state representing this read.
	 */
	state* nbread(std::vector<int> &val);

	/**
	 * Reads several integer values from this port into an array.
	 * This is a non-blocking read, meaning that the reading program does not wait for a value to be read.
	 * @param val The array, into which the values are stored.
	 * @param size The number of values that should be read and the size of the array.
	 * @return A #state representing this read.
	 */
	state* nbread(int val[], int size);

	friend out& operator >>(out &o, int &valj);
	friend out& operator >>(out &o, std::vector<int> &val);
};

/**
 * An abstract representation of a bi-directional AXI-Stream port.
 * Instances of this class represent bi-directional ports of IPCores.
 * Communication with a cores should be handled through these ports.
 */
class dual : public in, public out {
public:
	dual(interface *intrfc) : in(intrfc), out(intrfc){}
	~dual() {}
};

#endif /* PORT_H_ */
