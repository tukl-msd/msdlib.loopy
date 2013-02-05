/**
 * Describes AXI-Stream ports that can be instantiated by components.
 * @file
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef PORT_H_
#define PORT_H_

/** An Exception that marks a failed write operation on a port. */
class writeException {};
/** An Exception that marks a failed read operation on a port.  */
class readException {};

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
public:
	in() {}
	~in() {}

	/**
	 * Writes an array of integer values to this port.
	 * @param val The integer array to be written.
	 * @param size The size of the integer array.
	 * @return true if the write was successful, false otherwise.
	 */
	bool write(int val[], int size);
	/**
	 * Writes an integer value to this port.
	 * @param val The integer value to be written.
	 * @return true if the write was successful, false otherwise.
	 */
	bool write(int val);
};

/**
 * An abstract representation of an out-going AXI-Stream port.
 * Instances of this class represent out-going ports of IPCores.
 * Communication with a core should be handled through these ports.
 */
class out : protected port {
public:
	out() {}
	~out() {}

	/**
	 * Reads several integer values from this port.
	 * @param val An array, into which the values are stored.
	 * @param size The number of values that should be read and the size of the array.
	 * @return true, if the read was successful, false otherwise.
	 */
	bool read(int val[], int size);
	/**
	 * Reads a single integer value from this port.
	 * @param val Pointer where the read value should be stored
	 * @return true, if the read was successful, false otherwise.
	 */
	bool read(int *val);
	/**
	 * Reads a single integer value from this port.
	 * @return The read value.
	 * @throws readException if the read failed.
	 */
	int  read();
};

/**
 * An abstract representation of a bi-directional AXI-Stream port.
 * Instances of this class represent bi-directional ports of IPCores.
 * Communication with a cores should be handled through these ports.
 */
class dual : public in, public out {
public:
	dual() {}
	~dual() {}
};

#endif /* PORT_H_ */
