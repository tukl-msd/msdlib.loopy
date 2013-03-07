/**
 * message.c
 * @author Thomas Fischer
 * @since 27.02.2013
 */
#include "message.h"

#include <stdlib.h>

struct Message* message_new() {
	struct Message *m = malloc(sizeof(struct Message));
	m->header = NULL;
	m->payload = NULL;
	m->headerSize = 0;
	m->payloadSize = 0;
	return m;
}

//void message_append(struct Message *m, int values[], int size) {
//	// TODO Not very efficient )= Better no append...
//	m->payload = realloc(m->payload, m->payloadSize + sizeof(int) * size);
//
//	int i;
//	for(i = 0; i < size; i++) m->payload[m->payloadSize + i] = values[i];
//
//	m->payloadSize += i;
//}
void message_payload(struct Message *m, int values[], int size) {
	m->payload = values;
	m->payloadSize = size;
}

void message_header(struct Message *m, int header[], int size) {
	// (re)allocate memory for the header
	m->header = realloc(m->header, sizeof(int) * size);

	// assign single integer values
	int i;
	for(i = 0; i < size; i++) m->header[i] = header[i];

	// set the header size
	m->headerSize = size;
}

void message_free(struct Message *m) {
	free(m->header);
//	free(m->payload);
//  m->header = NULL;
//  m->payload = NULL;
//	m->headerSize = 0;
//	m->payloadSize = 0;
	free(m);
}
