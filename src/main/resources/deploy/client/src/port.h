/**
 * Describes AXI-Stream ports that can be instantiated by components.
 * @file
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef PORT_H_
#define PORT_H_

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
class in : public port {
public:
	in() {}
	~in() {}

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
class out : public port {
public:
	out() {}
	~out() {}

	/**
	 * Reads an integer value from this port.
	 * @param val An array, into which the value is stored.
	 * @return true, if the read was successful, false otherwise.
	 */
	// TODO why array? use pointer...a
	bool read(int val[]);
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
