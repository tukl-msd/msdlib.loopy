/**
 * @author Thomas Fischer
 * @since 27.02.2013
 */
#include "message.h"

#include <stdlib.h>

#include "../constants.h"

struct Message* message_new() {
	struct Message *m = malloc(sizeof(struct Message));
	m->header = NULL;
	m->payload = NULL;
	m->headerSize = 0;
	m->payloadSize = 0;
	return m;
}

void message_payload(struct Message *m, int values[], int size) {
	m->payloadSize = size;
	m->payload = values;
//	m->payload = malloc(sizeof(int) * size);
//	int i;
//	for(i = 0; i < size; i++) m->payload[i] = values[i];

}

void message_header(struct Message *m, int header[], int size) {
	// allocate memory for the header
	m->header = malloc(sizeof(int) * size);

	// assign single integer values
	int i;
	for(i = 0; i < size; i++) m->header[i] = header[i];

	// set the header size
	m->headerSize = size;
}

void message_free(struct Message *m) {
	free(m->header);
//	free(m->payload);
	free(m);
}

void inline print_message(struct Message *m) {
    loopy_print("\nsending message of size %d", m->headerSize + m->payloadSize);
    loopy_print("\nheader int:  %d", m->header[0]);

    loopy_print(" (Payload: ", m->payloadSize);
    int i;
    for(i = 0; i < m->payloadSize-1; i++) loopy_print("%d, ", m->payload[i]);
    loopy_print("%d", m->payload[m->payloadSize-1]);
    loopy_print(" )");
}
