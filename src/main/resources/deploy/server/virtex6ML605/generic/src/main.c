/*
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#include "fsl.h"

// forward declarations
void init_platform();
void init_medium();
void init_components();
void start_application();
void cleanup_platform();

void reset_components();
void reset_queues();
void schedule();

//void XUartLite_SendByte(u32 BaseAddress, u8 Data);
//
//void UartSendInt(int number) {
//	int i;
//	char byte;
//	for(i = 3; i>= 0; i--) {
//		byte = (number >> i*8) &0xff;
//		XUartLite_SendByte(UART_BASEADDR, byte);
//	}
//}

void test_axi_communication () {
	int rst_cfg = 0x00000001;
	int seed1   = 0x88cb47c9;
	int seed2   = 0x8a9cdf65;
	int seed3   = 0xcaf40ed9;
	int i, RngNumber, numberValues = 8;

	xil_printf("\nreset config");
	// TODO This is very confusing... Why are we using FSL here, when we have AXI streams??
	putfslx(rst_cfg, 0, FSL_DEFAULT);
	xil_printf("\nwriting 6 values ...");
	putfslx(seed1,   0, FSL_DEFAULT);
	putfslx(seed2,   0, FSL_DEFAULT);
	putfslx(seed3,   0, FSL_DEFAULT);
	putfslx(seed1,   0, FSL_DEFAULT);
	putfslx(seed1,   0, FSL_DEFAULT);
	putfslx(seed1,   0, FSL_DEFAULT);

	xil_printf(" done\nread %d values ...", numberValues);
	for(i = 0; i < numberValues; i++) {
		getfslx(RngNumber, 0, FSL_DEFAULT);
		//		xil_printf("\n  number %d: %d", i, RngNumber);
//		UartSendInt(RngNumber)
	}
}

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

	/* start the application (web server, rxtest, txtest, etc..) */
	// Note, that this contains the listening loop (which is basically an infinite loop)
	start_application();

//	test_axi_communication();

	schedule();

	/* never reached */
	cleanup_platform();

	return 0;
}
