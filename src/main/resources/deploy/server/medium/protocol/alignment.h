/**
 * Alignment procedures required for (Ethernet?) communication.
 * @file
 * @author Philipp Schlaefer
 * @since 01.02.2013
 */

#ifndef ALIGNMENT_H_
#define ALIGNMENT_H_

//void set_unaligned ( int *target, int *data );
//int  get_unaligned ( int *data );

unsigned int compose(unsigned char c1, unsigned char c2, unsigned char c3, unsigned char c4);

#endif /* ALIGNMENT_H_ */
