/**
 * @author Thomas Fischer
 * @author Mathias Weber
 * @since 19.02.2013
 */

// header file
#include "protocol.h"

// standard library
#include <cmath>
#include <stdio.h>
#include <iostream>

// constants
#include "../constants.h"

// others
#include "io.h"
#include "../constants.h"
#include "../exceptions.h"
#include "../logger.h"

// protocol instance to be used
protocol *proto = new protocol_v1();

protocol::protocol() {}

// message types
#define reset    0
#define reqcheck 1
#define checksum 2
#define debug    7
#define data     9
#define poll    10
#define gpio    14
#define ack     15

#define info    3
#define warning 8
#define error  13

protocol_v1::protocol_v1() {}

unsigned int protocol_v1::max_size() {
	return MAX_SIZE;
}

void protocol_v1::decode(int first) {
	int version = floor(first / pow(2, 24));

	// check if the version fits this decoder
	if(version != 1) throw protocolException("unknown protocol version " + version);

	first = fmod(first, pow(2, 24));
	int type = floor(first / pow(2, 20));

	// set id as specified in protocol version 1
	first = fmod(first, pow(2, 20));
	unsigned int id = floor(first / pow(2, 16));

	// the last two bytes mark the size of this frame
	unsigned int size = fmod(first, pow(2, 16));

	logger_host << FINE << "decoded the following message header: " << first << std::endl;
	logger_host << FINE << "  version : " << version << std::endl;
	logger_host << FINE << "  type    : " << type << std::endl;
	logger_host << FINE << "  target  : " << id << std::endl;
	logger_host << FINE << "  size    : " << size << std::endl;

	// 8 bit protocol version
	// 4 bit message type
	//   0xxx Config related
	//     0000 "Soft Reset"
	//     0001 request checksum
	//     0010 checksum
	//     0111 Error
	//   1xxx Data related
	//     1000 Data Non-Blocking
	//     1001 Data Blocking
	//     1010 Poll Non-Blocking
	//     1011 Poll Blocking
	//     1110 GPIO
	//     1111 ACK
	// 4 bit component identifier
	// 16 bit size or value, depending on type
	// <size> bytes data, depending on type

	// encoding v2
	// 00xx config related
	//  0000 soft reset    0
	//  0001 request checksum 1
	//  0010 checksum      2
	//  0011 info          3
	// 01xx data related
	//  0100 data          4
	//  0101 gpio          5
	//  0110 poll          6
	//  0111 ack           7
	switch(type) {
	case  reset: // This is a soft reset.
			 // receiving a soft reset from the board indicates, that the board performed a successful reset.
		     // signal the application (since reset() is a blocking call)
		break;
    case reqcheck:
        // should only be sent to the board, not the host
        break;
    case checksum:
        int received_cs[4];

        unsigned int i;
        for(i = 0; i<4; i++) {
            try {
                if(intrfc->waitForData(0,250000)) intrfc->readInt(received_cs + i);
            } catch(mediumException &e) {
                logger_host << ERROR << e.what() << std::endl;
                intrfc->waitForData(0,250000);
            }
        }

        if(received_cs[0] != CHECKSUM1 || received_cs[1] != CHECKSUM2 || received_cs[2] != CHECKSUM3 || received_cs[3] != CHECKSUM4) {
            //TODO better way to inform about checksum failure?
            logger_host << "checksum of board-side driver and host-side api do not match!" << std::endl;
            std::exit(1);
        }

        break;
    case  debug: // This is an error message. It should be stored in some error queue (throw it, if the call was synchronous?)
        // severity
        // 0011  3 info
        // 1000  8 warning
        // 1101 13 error

        if(size == 0) break;
        else { // need a new scope here...

            // the size is given in sizeof(int)
            unsigned int i;
            int payload[size];

            for(i = 0; i < size; i++) {
                try {
                    if(intrfc->waitForData(0, 250000)) intrfc->readInt(payload + i);
                } catch(mediumException &e) {
                    logger_host << ERROR << e.what() << std::endl;
                    intrfc->waitForData(0, 250000);
                }
            }

            // stream severity to logger
            logger_board << (severity)id << (char*)payload << std::endl;
        }

        break;
	case  data: // This is a blocking data package.
		if(size == 0) break;

		else { // need a new scope here
			// check if the pid is in range
			if(id > OUT_PORT_COUNT-1) throw protocolException(std::string("pid value (") +
					std::to_string(id) + ") of received data message exceeded count of out-going ports (" +
					std::to_string(OUT_PORT_COUNT) + ")");

			// the size is given in sizeof(int)
			int payload[size];
			unsigned int i = 0;

			// try to read a value from the medium
			while(i < size) {
				try {
				    if(intrfc->waitForData(0, 250000)) {
                        intrfc->readInt(payload + i);
                        i++;
                    }

//				    if(intrfc->waitForData(0, 250000))
//				        i += intrfc->readInts(payload + i, size - i);
                } catch(mediumException &e) {
                    // TODO is it a good idea, to ignore read "exceptions" here??
                    // what does it mean, to have an exception here?
                    // there should be another value according to size, but it can't be read from the medium currently
                    // this might happen, if...
                    // a) the medium is really slow and we loop faster than values arrive
                    // b) considering e.g. Ethernet, the message is split into multiple layer 3 messages
                    //    and the next one hasn't arrived yet
                    // c) broken board-side driver didn't send all the values specified in the size field...
                    // d) broken medium (lost connection for some reason)
                    // while a) and b) fix themselves by ignoring this message, c) and d) do not...
                    logger_host << ERROR << e.what() << std::endl;
                    // SOLUTION for d): perform a short wait and die over there, if something is wrong with the medium
                    // this however does NOT solve c) - buggy drivers will not send enough values...
                    //  this probably is a stupid use-case though. We do not look at attackers...
                    intrfc->waitForData(0, 250000);
                }
			}
//			intrfc->readInts(payload, size);

			// shift read values to the respective queue
			recv_data(id, payload, size);
		}
		break;
	case poll: // This is a non-blocking poll.
		if(id > IN_PORT_COUNT-1) throw protocolException(std::string("pid value (") +
				std::to_string(id) + ") of received poll message exceeded count of in-going ports (" +
				std::to_string(IN_PORT_COUNT) + ")");
		recv_poll(id); break;
	case gpio: // This marks a GPIO message.
		if(id > GPI_COUNT-1) throw protocolException(std::string("GPIO id value (") +
				std::to_string(id) + ") of received GPIO message exceeded count of GPIO input devices (" +
				std::to_string(GPI_COUNT) + ")");
		recv_gpio(id, size); break;
	case ack: // This is an acknowledgment.
		if(id > IN_PORT_COUNT-1) throw protocolException(std::string("pid value (") +
				std::to_string(id) + ") of received acknowledgment exceeded count of in-going ports (" +
				std::to_string(IN_PORT_COUNT) + ")");
		recv_ack(id, size);
		break;
	default:
		throw protocolException(
		        std::string("unknown message type (") +
		        std::to_string(type) + ") for protocol version 1");
	}

	logger_host << FINE << "finished message interpretation" << std::endl;
}

std::vector<int> protocol_v1::encode_data(unsigned char pid, std::vector<int> val) {
	// check value size
	if(val.size() > MAX_SIZE) throw protocolException(std::string("actual message size (") +
			std::to_string(val.size()) + ") exceeded message capacity (" + std::to_string(MAX_SIZE) + ")");
	// check port id
	if(pid > IN_PORT_COUNT-1) throw protocolException(std::string("port id (") +
			std::to_string(pid) + ") exceeded port range for in-going ports (" + std::to_string(IN_PORT_COUNT) + ")");

	// append header and return
	val.insert(val.begin(),construct_header(data, pid, val.size()));
	return val;
}


std::vector<int> protocol_v1::encode_poll(unsigned char pid, unsigned int count) {
	// check value size
//	if(count > MAX_SIZE) throw protocolException(std::string("request count (") +
//			std::to_string(count) + ") exceeded message capacity (" + std::to_string(MAX_SIZE) + ")");
	// check port id
	if(pid > OUT_PORT_COUNT-1) throw protocolException(std::string("port id (") +
			std::to_string(pid) + ") exceeded port range for out-going ports (" + std::to_string(OUT_PORT_COUNT) + ")");

	// construct message and return
	std::vector<int> v;

	while(count > MAX_SIZE) {
		v.push_back(construct_header(poll, pid, MAX_SIZE));
		count -= MAX_SIZE;
	}

	v.push_back(construct_header(poll, pid, count));
	return v;
}

std::vector<int> protocol_v1::encode_gpio(unsigned char gid, unsigned char val) {
	// check port id
	if(gid > GPI_COUNT-1) throw protocolException(std::string("GPIO id (") +
			std::to_string(gid) + ") exceeded GPIO output  device range (" + std::to_string(GPI_COUNT) + ")");

	// construct message and return
	std::vector<int> v;
	v.push_back(construct_header(gpio, gid, val));
	return v;
}

std::vector<int> protocol_v1::encode_reset() {
	std::vector<int> v;
	v.push_back(construct_header(reset, 0, 0));
	return v;
}

std::vector<int> protocol_v1::encode_request_checksum() {
	std::vector<int> v;
	v.push_back(construct_header(reqcheck, 0, 0));
	return v;
}

int protocol_v1::construct_header(unsigned char type, unsigned char id, unsigned int size) {
	return (1 << 24) + (type << 20) + (id << 16) + size;
}
