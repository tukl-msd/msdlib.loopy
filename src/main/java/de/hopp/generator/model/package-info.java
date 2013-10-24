/**
 *
 */
/**
 * Contains abstract syntax trees used by different parts of the generator.
 * <p>
 * First of all, this includes the .bdl model itself, which is used as input
 * for the generator.
 * Furthermore, models used by the backends are located here.
 * Finally, unparsers for these backend models are provided, which translate a
 * model representation to an actual file.
 * <p>
 * Currently, this includes the MHS model used by the Xilinx ISE workflow.
 * For this model, a single unparser is provided.
 * Also, a C/C++ model used by both, Xilinx ISE and the C++ host backend is contained.
 * Different unparsers for header files, plain C and C++ sources exist.
 *
 * @author Thomas Fischer
 */
package de.hopp.generator.model;