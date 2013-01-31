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

/**
 * An abstract representation of a communication medium.
 * The communication medium models how the board is attached to the host.
 * This represents an abstract medium and should not be instantiated.
 */
class interface {
public:
// -------------------- destructor -----------------------------
	interface();
	virtual ~interface() {}
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
	 * Sends an integer value to the board.
	 * @param val the integer value to be sent.
	 * @return true if successful, false otherwise.
	 */
	virtual bool send(int val) = 0;
	/**
	 * Send an array of integer values to the board.
	 * @param val an array of integer values to be sent.
	 * @param size the size of the array.
	 * @return true if successful, false otherwise.
	 */
	virtual bool send(int val[], int size) = 0;

	/**
	 * Sets the LED state to a specified value.
	 * This method is only to be used by the LED test application!
	 * @param val The new LED state.
	 * @return true if the state was set successful, false otherwise.
	 */
	bool setLEDState(int val);

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
};

/**
 * An abstract representation of an ethernet medium.
 * Encapsulates communication with a board attached with ethernet.
 */
class ethernet : public interface {
public:
	// constructor & destructor
	/**
	 * Constructor for an Ethernet-type communication medium.
	 * @param ip IP address of the interface.
	 * @param port Port for the communications.
	 */
	ethernet(const char *ip, unsigned short int port);
	~ethernet();

	// communication
	/**
	 * Send an integer value to the board.
	 * @param val the value to be sent.
	 * @return true if sending was successful, false otherwise
	 */
	bool send(int val);
	/**
	 * Send an array of integer values to the board.
	 * @param val an array of integer values to be sent.
	 * @param size the size of the array.
	 * @return true if sending was susccessful, false otherwise
	 */
	bool send(int val[], int size);
private:
	int Data_SocketFD;
	const char *ip;
	unsigned short int port;

	// connection management
	/**
	 * Sets up an TCP/IP connection over Ethernet.
	 */
	void setup();
	/**
	 * Tears down an active TCP/IP connection over Ethernet.
	 */
	void teardown();

	// communication
//	bool writeValues(int buf[], int size);
	bool readInt    (int buf[], int size);
};

/**
 * An abstract representation of a uart medium.
 * Encapsulates communication with a board attached with uart/usb.
 */
class uart : public interface {
public:
	// constructor & destructor
	/**
	 * Constructor for an UART/USB-type communication medium.
	 */
	uart();
	~uart();

	// communication
	bool send(int val);
	bool send(int val[], int size);
private:
	// connection management
	void setup();
	void teardown();
};

#endif /* INTERFACE_H_ */
