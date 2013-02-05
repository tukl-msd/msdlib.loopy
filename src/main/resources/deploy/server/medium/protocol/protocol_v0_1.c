/*
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#include <math.h>
#include "alignment.h"
#include "protocol_v0_1.h"

void set_LED(u32 state);

int decode_v0_1( struct pbuf *p ) {

	int header = get_unaligned((void*)(((int)p->payload)));

	int type, size;

	// read the version information from the header
	if(floor(header / pow(2, 24)) != 1) {
		// if this happens, the delegation failed!
		xil_printf("ERROR: fatal error during protocol delegation");
		return 1;
	}

	// remove the version information to retain the rest
	header = fmod(header, pow(2, 24));

	// set type and size as specified in header type 1
	type = floor(header / pow(2, 16));
	size = fmod(header, pow(2, 16));

	// TODO: handle this with flags? lowest 16 bit for source/target,
	//       17th & 18th bit for type (master, slave, read, write, gpio? not sure what is possible)
	switch(type) {
	case 0:
		// This message type marks setting of the LED state in protocol version 1.
		// We COULD directly use the size for this... or we require size one and then read only the lowest bits...
		// QUESTION: size = byte or word? since word is dependent on the architecture, it is probably better to use bytes...
		// QUESTION: is it possible to send "single" bytes? yes, unsigned chars. BUT: alignment methods are currently not made for this...
		if(size != 1) {
			xil_printf("ERROR: wrong payload for LED state (has to be exactly one)");
			return 1;
		}
		// read the first byte of the "data" buffer
		unsigned int a = get_unaligned(p->payload + 4);
		if(a > 255) {
			xil_printf("ERROR: to large number for LED state (255 is max)");
			return 1;
		}

		set_LED(a);
		break;
	case 1:
		// This message type marks poll of the Switch state in protocol version 1.
		break;
	case 2:
		// This message type marks poll of the Button state in protocol version 1.
		break;
	default:
		xil_printf("ERROR: unknown type %d for protocol version 0.1", type);
		return 1;
	}

	size = fmod(header, pow(2, 16));

	xil_printf("received package (version 0.1, type: %d, size: %d)\n", type, size);

	return 0;

}

