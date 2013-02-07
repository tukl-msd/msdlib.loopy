/*
 * uart.c
 * @author Thomas Fischer
 * @since 05.02.2013
 */
#include "uart.h"

#include "../constants.h"

#include "xparameters.h"
#include "xuartlite.h"

#define UART_BASEADDR XPAR_UARTLITE_1_BASEADDR

int decode_header(unsigned char version);
u8 XUartLite_RecvByte();
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


unsigned char recv_char() {
	return XUartLite_RecvByte(UART_BASEADDR);
}

int start_application() {
	if(DEBUG) xil_printf("\nStart listening for inbound message headers ...");

	// Start listening loop
	while(1) {
		decode_header(recv_char());
	}
	return 0;
}

void init_medium() {
	if(DEBUG) xil_printf("\nSetting up UART/USB interface");
	// Nothing to do... works out of the box, it seems...
}

