/*
 * message.h
 *
 *  Created on: 27.02.2013
 *      Author: thomas
 */

#ifndef MESSAGE_H_
#define MESSAGE_H_

struct Message {
	int *header, headerSize;
	int *payload, payloadSize;
};

struct Message* message_new();

void message_append(struct Message *m, int values[], int size);

void message_header(struct Message *m, int header[], int size);

void message_free(struct Message *m);

#endif /* MESSAGE_H_ */
