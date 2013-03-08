/*
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#include "fsl.h"
//#include "medium/medium.h"
#include <stdlib.h>
//#include "medium/message.h"
#include "xparameters.h"

#include "components/components.h"
#include "components/gpio/led.h"

#include "io.h"

// forward declarations
void init_platform();
void init_medium();
void init_components();
void init_queue();
void start_application();
void cleanup_platform();

void reset_components();
void reset_queues();
void schedule();

void XUartLite_SendByte(u32 BaseAddress, u8 Data);
void UartSendInt(int number);

void reset() {
	reset_components();
	reset_queues();
}

// main method
int main() {
	// initialise the platform. This also sets up the interrupt controller
	init_platform();

	// initialise Ethernet
	init_medium();

	// initialise all components
	init_components();

	// initialise queues
	init_queue();

	// perform medium-specific startup operations (TODO merge with init?)
	start_application();

	// start the scheduler
	schedule();

	// never reached
	cleanup_platform();

	return 0;
}
