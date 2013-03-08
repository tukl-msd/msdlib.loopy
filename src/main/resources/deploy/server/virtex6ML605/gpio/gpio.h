/**
 * Generic procedures and definitions used by gpio components.
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#ifndef GPIO_H_
#define GPIO_H_

#include "xbasic_types.h"
#include "xil_exception.h"
#include "xgpio.h"

#define GPIO_CHANNEL1 1

/**
 * Set up interrupt handler for a gpio component.
 * @param InstancePtr Pointer for the gpio component to be set up.
 * @param DeviceId Id of the gpio component.
 * @param IntrId Id of the interrupt controller.
 * @param IntrMask The mask to enable. Bit positions of 1 are enabled.
 * @param GpioHandler Interrupt handler, called whenever the components state changes.
 * @return XST_SUCCESS if successful, an error code otherwise, describing the occurred error.
 */
int GpioIntrSetup(XGpio *InstancePtr, u16 DeviceId, u16 IntrId, u16 IntrMask, Xil_ExceptionHandler GpioHandler);

#endif /* GPIO_H_ */
