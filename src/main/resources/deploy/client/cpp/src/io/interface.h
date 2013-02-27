/**
 * Describes communication mediums, which encapsulate the communication between board and host.
 * @file
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef INTERFACE_H_
#define INTERFACE_H_

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <vector>

/**
 * An abstract representation of a communication medium.
 * The communication medium models how the board is attached to the host.
 * This represents an abstract medium and should not be instantiated.
 */
class interface {

private:
	/**
	 * The counter of references to this medium. Manipulated through reg() and
	 * unreg(). If it reaches 0, the medium is cleaned up.
	 */
	int refCount;
// -------------------- connection management -----------------------------
	/**
	 * Set up the connection between client and server.
	 * This method has to be overridden in all subclasses and performs
	 * interface-specific setup operations.
	 */
	virtual void setup() = 0;
	/**
	 * Shuts down the connection between client and server.
	 * This method has to be overridden in all subclasses and performs
	 * interface-specific shut down operations.
	 */
	virtual void teardown() = 0;
public:
// ---------------- constructor & destructor -------------------------
	interface();
	virtual ~interface() { }
// -------------------- registering -----------------------------
	/**
 	 * Registers a new core that uses this communication medium.
 	 * Effectively increments the reference counter of the medium.
 	 */
	void incRef();
	/**
 	 * Unregisters a core that uses this communication medium.
 	 * Effectively dencrements the reference counter of the medium.
 	 * If the counter reaches 0, no more references to this medium
 	 * exist and it is cleaned up.
 	 */
	void decRef();
// -------------------- communication -----------------------------
	/**
	 * Sends a single integer value to the board.
	 * @param val The integer value to be sent.
	 * @return true if successful, false otherwise.
	 */
	virtual bool send(int val) = 0;
	/**
	 * Send a vector of integer values to the board.
	 * @param val A vector of integer values to be sent.
	 * @return true if successful, false otherwise.
	 */
	virtual bool send(std::vector<int> val) = 0;
	/**
	 * Send an array of integer values to the board.
	 * @param val An array of integer values to be sent.
	 * @param size The size of the array.
	 * @return true if successful, false otherwise.
	 */
	virtual bool send(int val[], int size) = 0;
	/**
	 * Reads a single integer value from the medium.
	 * @param val Pointer to where the read value should be stored.
	 * @return true if successful, false otherwise
	 */
	virtual bool readInt(int *val) = 0;
	/**
	 * Waits until data arrives or a timeout occurs.
	 * @param timeout Number of seconds until a timeout occurs.
	 * @return true, if data arrived before timeout, false otherwise
	 */
	virtual bool waitForData(int timeout) = 0;
};

/**
 * An abstract representation of an ethernet medium.
 * Encapsulates communication with a board attached with ethernet.
 */
class ethernet : public interface {
private:
	int Data_SocketFD;
	const char *ip;
	unsigned short int port;

// -------------------- connection management -----------------------------
	/** Sets up an TCP/IP connection over Ethernet. */
	void setup();
	/** Tears down an active TCP/IP connection over Ethernet. */
	void teardown();

// -------------------- communication -----------------------------
//	bool writeValues(int buf[], int size);
	bool readInt    (int buf[], int size);
public:
// ---------------- constructor & destructor -------------------------
	/**
	 * Constructor for an Ethernet-type communication medium.
	 * @param ip IP address of the interface.
	 * @param port TCP Port for the communication.
	 */
	ethernet(const char *ip, unsigned short int port);
	~ethernet();

// -------------------- communication -----------------------------
	/**
	 * Send an integer value to the board.
	 * @param val the value to be sent.
	 * @return true if sending was successful, false otherwise
	 */
	bool send(int val);
	/**
	 * Send a vector of integer values to the board.
	 * @param val A vector of integer values to be sent.
	 * @return true if successful, false otherwise.
	 */
	bool send(std::vector<int> val);
	/**
	 * Send an array of integer values to the board.
	 * @param val an array of integer values to be sent.
	 * @param size the size of the array.
	 * @return true if sending was susccessful, false otherwise
	 */
	bool send(int val[], int size);
	/**
	 * Reads a single integer value from the medium.
	 * @param val Pointer to where the read value should be stored.
	 * @return true if successful, false otherwise
	 */
	bool readInt(int *val);
	/**
	 * Waits until data arrives or a timeout occurs.
	 * @param timeout Number of seconds until a timeout occurs.
	 * @return true, if data arrived before timeout, false otherwise
	 */
	bool waitForData(int timeout);
};
//
///**
// * An abstract representation of a uart medium.
// * Encapsulates communication with a board attached with uart/usb.
// */
//class uart : public interface {
//public:
//	// constructor & destructor
//	/**
//	 * Constructor for an UART/USB-type communication medium.
//	 */
//	uart();
//	~uart();
//
//	// communication
//	bool send(int val);
//	bool send(int val[], int size);
//	bool send(std::vector<int> val);
//private:
//	// connection management
//	void setup();
//	void teardown();
//};

#endif /* INTERFACE_H_ */
