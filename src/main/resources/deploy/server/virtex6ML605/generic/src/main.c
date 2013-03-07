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

//void reset_components();
//void reset_queues();
void schedule();
//void test_tcp();

void XUartLite_SendByte(u32 BaseAddress, u8 Data);
void UartSendInt(int number);

//void UartSendInt(int number) {
//	int i;
//	char byte;
//	for(i = 3; i>= 0; i--) {
//		byte = (number >> i*8) &0xff;
//		XUartLite_SendByte(XPAR_RS232_UART_1_BASEADDR, byte);
//	}
//}

void test_axi_communication () {
	int rst_cfg = 0x00000001;
	int seed1   = 0x88cb47c9;
	int seed2   = 0x8a9cdf65;
	int seed3   = 0xcaf40ed9;

	put(inQueue[0], rst_cfg);
	put(inQueue[0], seed1);
	put(inQueue[0], seed2);
	put(inQueue[0], seed3);
	put(inQueue[0], seed1);
	put(inQueue[0], seed1);
	put(inQueue[0], seed1);

//	int i, RngNumber = 0, numberValues = 8;
//
//	xil_printf("\nreset config");
//	// TODO This is very confusing... Why are we using FSL here, when we have AXI streams??
//
////	putfslx(rst_cfg, 0, FSL_NONBLOCKING);
////	xil_printf("\nwriting 6 values ...");
////	putfslx(seed1,   0, FSL_NONBLOCKING);
////	putfslx(seed2,   0, FSL_NONBLOCKING);
////	putfslx(seed3,   0, FSL_NONBLOCKING);
////	putfslx(seed1,   0, FSL_NONBLOCKING);
////	putfslx(seed1,   0, FSL_NONBLOCKING);
////	putfslx(seed1,   0, FSL_NONBLOCKING);
//
//	while(axi_write(rst_cfg, 0)) {}
//	xil_printf("\nwriting 6 values ...");
//	while(axi_write(seed1,   0)) {}
//	while(axi_write(seed2,   0)) {}
//	while(axi_write(seed3,   0)) {}
//	while(axi_write(seed1,   0)) {}
//	while(axi_write(seed1,   0)) {}
//	while(axi_write(seed1,   0)) {}
//
//	xil_printf(" done\nread %d values ...", numberValues);
//	for(i = 0; i < numberValues; i++) {
//		while(axi_read(&RngNumber, 0)) set_LED(1);
////		int invalid = 1;
////		while(invalid) {
////			getfslx(RngNumber,  0, FSL_NONBLOCKING);
////			fsl_isinvalid(invalid);
////		}
//		set_LED(0);
//		UartSendInt(RngNumber);
//	}
}

//void reset() {
//	reset_components();
//	reset_queues();
//}

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

//	test_axi_communication();

	schedule();

	/* never reached */
	cleanup_platform();

	return 0;
}
