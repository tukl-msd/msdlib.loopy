/**
 * Describes out-going messages to be send over the medium.
 * @file
 * @author Thomas Fischer
 * @since 27.02.2013
 */

#ifndef MESSAGE_H_
#define MESSAGE_H_

/**
 * Representation of an out-going message.
 * Differentiates between header and payload of the message.
 */
struct Message {
	/**
	 * Pointer to an array containing the header.
	 * Memory should be managed by manipulating procedures.
	 */
	int *header;
	/**
	 * Pointer to an array containing the payload.
	 * Memory should NOT be managed by manipulating procedures
	 * in order to reuse the storage area of the sw queue.
	 */
	int *payload;
	/** Size of the referenced header array. */
	int headerSize;
	/** Size of the referenced payload array. */
	int payloadSize;
};

/**
 * Allocates memory for a new message.
 * Instantiates an empty header and payload.
 * @return Pointer to the allocated memory.
 */
struct Message* message_new();

/**
 * Sets the payload of a message.
 * @param m The message, for which the payload should be set.
 * @param payload Pointer to the array representing the payload of the message
 * @param size Size of the payload.
 */
void message_payload(struct Message *m, int payload[], int size);

/**
 * Sets the header of a message.
 * @param m The message to be modified.
 * @param header The header to be added.
 * @param size Size of the header (in 4-byte integer values).
 */
void message_header(struct Message *m, int header[], int size);

/**
 * Frees the memory used by a message.
 * Does NOT free the payload pointer (since this is usually a fixed
 * block of memory reused by the driver).
 * @param m Pointer to the message to be removed.
 */
void message_free(struct Message *m);

/**
 * Prints a message using the specified debug print method.
 * @param m Pointer to the message to be printed.
 */
void print_message(struct Message *m);

#endif /* MESSAGE_H_ */
