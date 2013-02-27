/*
 * message.c
 *
 *  Created on: 27.02.2013
 *      Author: thomas
 */
#include "message.h"

#include <stdlib.h>

struct Message* message_new() {
	struct Message* m = malloc(sizeof(struct Message));
	m->header = NULL;
	m->payload = NULL;
	m->headerSize = 0;
	m->payloadSize = 0;
	return m;
}

void message_append(struct Message *m, int values[], int size) {
	m->payload = realloc(m->payload, m->payloadSize + size);

	int i;
	for(i = 0; i < size; i++) m->payload[m->payloadSize + i] = values[i];

	m->payloadSize += i;
}

void message_header(struct Message *m, int header[], int size) {
	m->header = malloc(size);

	int i;
	for(i = 0; i < size; i++) m->header[i] = header[i];

	m->headerSize = size;
}

void message_free(struct Message *m) {
	free(m->header);
	free(m->payload);
	m->headerSize = 0;
	m->payloadSize = 0;
	free(m);
}
