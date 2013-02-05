/**
 * Alignment procedures required for (Ethernet?) communication.
 * @file
 * @author Philipp Schlaefer
 * @since 01.02.2013
 */

#ifndef ALIGNMENT_H_
#define ALIGNMENT_H_

void set_unaligned ( int *target, int *data );
int  get_unaligned ( int *data );

#endif /* ALIGNMENT_H_ */
