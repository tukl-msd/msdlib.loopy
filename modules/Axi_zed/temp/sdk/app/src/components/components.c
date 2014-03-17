#include "components.h"

#include "../constants.h"
#include "../io.h"
// procedures of components
/**
 * Initialises all components on this board.
 * This includes gpio components and user-defined IPCores,
 * but not the communication medium this board is attached with.
 */
void init_components ( ) {
    int status;
    inQueue[0] = createQueue(8);
    outQueueCap[0] = 1024;
    isPolling[0] = 1;
    pollCount[0] = 64;
}

/**
 * Resets all components in this board.
 * This includes gpio components and user-defined IPCores,
 * but not the communication medium, this board is attached with.
 */
void reset_components ( ) { }

/**
 * Write a value to an AXI stream.
 * @param val Value to be written to the stream.
 * @param target Target stream identifier.
 */
int axi_write ( int val, int target ) {
    log_finer("\nwriting to in-going port %d (value: %d) ...", target, val);
    if(target > 0) {
        log_error("unknown axi stream port %d", target);
        return 1;
    }
    // putdfslx(val, target, FSL_NONBLOCKING);

    int rslt = 1;
    // fsl_isinvalid(rslt);
    log_finer(" (invalid: %d)", rslt);
    return rslt;
}

/**
 * Read a value from an AXI stream.
 * @param val Pointer to the memory area, where the read value will be stored.
 * @param target Target stream identifier.
 */
int axi_read ( int *val, int target ) {
    log_finer("\nreading from out-going port %d ...", target);
    if(target > 0) {
        log_error("unknown axi stream port %d", target);
        return 1;
    }
    // getdfslx(*val, target, FSL_NONBLOCKING);

    log_finer("\n %d", *val);
    int rslt = 1;
    // fsl_isinvalid(rslt);
    log_finer(" (invalid: %d)", rslt);
    return rslt;
}

