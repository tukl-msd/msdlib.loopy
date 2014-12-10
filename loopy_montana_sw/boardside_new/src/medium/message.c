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

void print_message(struct Message *m) {
    log_fine("sending message with total size: %d", m->headerSize + m->payloadSize);
    log_finer("header int:  %d", m->header[0]);
    log_finer("payload size: %d", m->payloadSize);

    // print the values of the payload
    log_finest("payload values: ");
    int i;
    for(i = 0; i < m->payloadSize; i++)
        log_finest("%d", m->payload[i]);
}
