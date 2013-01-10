/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef INTERFACE_H_
#define INTERFACE_H_

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

//int DEBUG = 1;

class inter {
public:

	// constructor & destructor
//	inter() {};
	virtual ~inter() {};

	// connection management
	virtual void setup() {};
	virtual void teardown() {};

	// communication
	virtual void send(int val) {};

	// test application
	virtual void test() {};
};

class ethernet : public inter {
public:
	// constructor & destructor
	ethernet(const char *ip, unsigned short int port);
	~ethernet();

private:
	int Data_SocketFD;
	const char *ip;
	unsigned short int port;

	// connection management
	void setup();
	void teardown();

	// communication
	void send(int val);

	int writeValues(int buf[], int size);
	int readInt    (int buf[], int size);

	// test application
	void test();

	int setLEDStateByArr(int state[8]);
	int setLEDState(int state);
};

class uart : public inter {
public:
	// constructor & destructor
	uart();
	~uart();

private:

	// connection management
	void setup();
	void teardown();

	// communication
	void send(int val);

	// test application
	void test();
};

#endif /* INTERFACE_H_ */
