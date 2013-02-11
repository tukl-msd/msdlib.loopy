/*
 * @author Thomas Fischer
 * @since 05.02.2013
 */
#include "medium.h"

#include "xparameters.h"
#include "xuartlite.h"

#include "protocol/protocol.h"
#include "../constants.h"

#define UART_BASEADDR XPAR_UARTLITE_1_BASEADDR

u8 XUartLite_RecvByte();

#define nextByte XUartLite_RecvByte(UART_BASEADDR);
//void XUartLite_SendByte();
//
//void UartSendInt(int number) {
//	int i;
//	char byteToSend;
//	for (i = 3; i >= 0; i--) {
//		byteToSend = (number >> i*8) & 0xff;
//		XUartLite_SendByte(UART_BASEADDR, byteToSend);
//	}
//}


unsigned int compose(unsigned char c1, unsigned char c2, unsigned char c3, unsigned char c4) {
	unsigned int rslt;
	rslt = (c1 << 24) + (c2 << 16) + (c3 << 8) + c4;
	return rslt;
}

//int recv_int() {
//	int i;
//	char bytes[4];
//
//	// caaaareful - byte order!
//	for(i = 0; i < 4; i++) bytes[i] = XUartLite_RecvByte(UART_BASEADDR);
//
//	i = compose(bytes[0], bytes[1], bytes[2], bytes[3]);
//
//	xil_printf("\ncomposed value %d", i);
//	return i;
//}

int recv_int() {
	return compose(
			XUartLite_RecvByte(UART_BASEADDR),
			XUartLite_RecvByte(UART_BASEADDR),
			XUartLite_RecvByte(UART_BASEADDR),
			XUartLite_RecvByte(UART_BASEADDR));
}

int start_application() {
	if(DEBUG) xil_printf("\nStart listening for inbound message headers ...");

	// Start listening loop
	while(1) {
		decode_header(recv_int());
	}
	return 0;
}

void init_medium() {
	if(DEBUG) xil_printf("\nSetting up UART/USB interface");
	// Nothing to do... works out of the box, it seems...
}

