#ifndef DRIVER_H_
#define DRIVER_H_

#include "xintc.h"
#include "xgpio.h"
#include "xil_exception.h"
#include "netif/xadapter.h"
#include "lwip/tcp.h"
#include "platform.h"
#include "platform_config.h"
#include "lwip/init.h"

// attributes of Driver
extern XIntc intc;
extern XGpio buttons;
extern XGpio leds;
extern unsigned char (mac_ethernet_address) [6];
extern struct ip_addr ipaddr, netmask, gw;
extern struct netif netif;
extern XGpio switches;

// procedures of Driver
int GpioSetupIntrSystem ( XGpio *instancePtr, u16 intrId, Xil_ExceptionHandler handler );
u32 getButtons ( );
void GpioHandlerButtons ( void *CallbackRef );
int setLED ( u32 value );
void set_unaligned ( int *target, int *data );
int get_unaligned ( int *data );
err_t recv_callback ( void *arg, struct tcp_pcb *tpcb, struct pbuf *p, err_t err );
err_t accept_callback ( void *arg, struct tcp_pcb *newpcb, err_t err );
int start_application ( );
u32 getSwitches ( );
void GpioHandlerSwitches ( void *CallbackRef );
int intrSetup ( );
int init ( );
int cleanup ( );
int main ( );

#endif /* DRIVER_H_ */