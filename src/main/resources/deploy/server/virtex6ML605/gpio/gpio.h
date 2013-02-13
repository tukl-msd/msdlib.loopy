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

int GpioIntrSetup(XGpio *InstancePtr, u16 DeviceId, u16 IntrId, u16 IntrMask, Xil_ExceptionHandler GpioHandler);

#endif /* GPIO_H_ */
