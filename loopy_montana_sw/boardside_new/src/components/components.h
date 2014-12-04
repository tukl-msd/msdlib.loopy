/**
 * Contains component-specific initialisation and processing procedures.
 * @file
 */
#ifndef COMPONENTS_H_
#define COMPONENTS_H_

// procedures of components
/**
 * Initialises all components on this board.
 * This includes gpio components and user-defined IPCores,
 * but not the communication medium this board is attached with.
 */
void init_components ( );
/**
 * Resets all components in this board.
 * This includes gpio components and user-defined IPCores,
 * but not the communication medium, this board is attached with.
 */
void reset_components ( );

#endif /* COMPONENTS_H_ */