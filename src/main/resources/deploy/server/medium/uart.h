/**
 * Handles communication over UART/USB.
 * This includes medium-specific initialisation as well as the listening loop.
 * @file
 * @author Thomas Fischer
 * @since 05.02.2013
 */

#ifndef UART_H_
#define UART_H_

/** initialise this communication medium */
void init_medium();

/** start listening for in-going packages */
int start_application();

#endif /* UART_H_ */
