/**
 * Handles protocol-specific encoding and decoding.
 * @file
 * @author Thomas Fischer
 * @since 19.02.2013
 */

#ifndef PROTOCOL_H_
#define PROTOCOL_H_

#include <vector>

/**
 * Abstract superclass for the host-side protocol encoder and decoder.
 * This class describes the functionality required by a protocol.
 * All future protocols have to be subclass of this protocol and provide
 * at least the described functionality.
 * The encoder has to be defined statically. The decoder is chosen
 * dynamically, depending on the version number of received messages.
 * @see protocol_v1 Version 1 of the protocol.
 */
class protocol {
public:
	protocol();
	virtual ~protocol() { };

	/**
	 * The maximal payload size for a message.
	 * @return maximal payload size.
	 */
	virtual unsigned int max_size() = 0;

	/**
	 * Decodes an incoming message.
	 * @param first The first integer-sized value of the message.
	 * @throws protocolException For errors or unexpected header values encountered during decoding.
	 */
	virtual void decode(int first) = 0;
	/**
	 * Encodes a data package.
	 * This includes generating and appending a fitting header.
	 * @param pid Target port id.
	 * @param val Data to be sent to the port.
	 * @return The encoded package.
	 */
	virtual std::vector<int> encode_data(unsigned char pid, std::vector<int> val) = 0;
	/**
	 * Encodes a data request.
	 * This includes generating and appending a fitting header.
	 * @param pid Target port id.
	 * @param count Number of requested values.
	 * @return The encoded package.
	 */
	virtual std::vector<int> encode_poll(unsigned char pid, unsigned int count) = 0;
	/**
	 * Encodes a gpio package.
	 * This includes generating and appending a fitting header.
	 * @param gid Target gpio device id.
	 * @param val Data to be sent to the gpio device.
	 * @return The encoded package.
	 */
	virtual std::vector<int> encode_gpio(unsigned char gid, unsigned char val) = 0;
	/**
	 * Generates a reset message.
	 * @return The encoded message.
	 */
	virtual std::vector<int> encode_reset() = 0;
};

/**
 * Host-side protocol encoder and decoder version 1.
 */
class protocol_v1 : public protocol {
private:
	static const unsigned int MAX_SIZE = 65536;
	int construct_header(unsigned char type, unsigned char id, unsigned int size);
public:
	protocol_v1();
	~protocol_v1() { };
	unsigned int max_size();
	void decode(int first);
	std::vector<int> encode_data(unsigned char pid, std::vector<int> val);
	std::vector<int> encode_poll(unsigned char pid, unsigned int count);
	std::vector<int> encode_gpio(unsigned char gid, unsigned char val);
	std::vector<int> encode_reset();
};

/** Instance of the protocol used for sending */
extern protocol *proto;

#endif /* PROTOCOL_H_ */
