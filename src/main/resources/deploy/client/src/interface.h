/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef INTERFACE_H_
#define INTERFACE_H_

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

class interface {
protected:
	bool isSetup;
public:
	// -------------------- destructor -----------------------------
	virtual ~interface() {}

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

	// -------------------- communication -----------------------------
	/**
	 * Sends an integer value to the board.
	 * @param val the integer value to be sent.
	 * @returns true if successful, false otherwise.
	 */
	virtual bool send(int val) = 0;
	virtual bool send(int val[], int size) = 0;
	bool setLEDState(int val);

};

class ethernet : public interface {
public:
	// constructor & destructor
	/**
	 * Constructor for an ethernet-type interface.
	 * @param ip ip address of the interface
	 * @param port port for the communications
	 */
	ethernet(const char *ip, unsigned short int port);
	~ethernet();

	// communication
	bool send(int val);
	bool send(int val[], int size);

	// test application
	void test();

private:
	int Data_SocketFD;
	const char *ip;
	unsigned short int port;

	// connection management
	void setup();
	void teardown();

	// communication
//	bool writeValues(int buf[], int size);
	bool readInt    (int buf[], int size);
};

class uart : public interface {
public:
	// constructor & destructor
	uart();
	~uart();

private:
	// connection management
	void setup();
	void teardown();

	// communication
	bool send(int val);
};

#endif /* INTERFACE_H_ */
