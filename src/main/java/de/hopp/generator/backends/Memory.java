package de.hopp.generator.backends;

/**
 * The memory model of the board.
 *
 * This is used to allocate memory for IP cores on the board.
 * Depending on the board type, a different amount of memory might be
 * allocated for IP cores and the area to be used by these cores might differ.
 *
 * @author Thomas Fischer
 * @since 22.8.2013
 */
public class Memory {

    public class Range {
        private final int baseAddress;
        private final int highAddress;

        public Range(int baseAddress, int highAddress) {
            this.baseAddress = baseAddress;
            this.highAddress = highAddress;
        }
        public int getBaseAddress() { return baseAddress; }
        public int getHighAddress() { return highAddress; }

    }

    /** The lowest memory address that can be allocated */
    private final int baseAddress;
    /** The highest memory address that can be allocated */
    private final int highAddress;

    /** The lowest free memory address */
    private int curAddress;

    /**
     *
     * @param baseAddress The lowest memory address that can be allocated
     * @param highAddress The highest memory address that can be allocated
     */
    public Memory(int baseAddress, int highAddress) {
        this.baseAddress = baseAddress;
        this.highAddress = highAddress;
        this.curAddress  = baseAddress;
    }

    /**
     * Allocates memory on the board to be used by an IP core.
     *
     * The memory has to fit in the remaining free memory.
     * @param size Size of the memory block to be allocated.
     * @return Allocated memory range.
     * @throws IllegalArgumentException If the block does not fit
     *      in the remaining free memory space
     */
    public Range allocateMemory(int size) throws IllegalArgumentException {
        int baseAddress = curAddress;
        curAddress += size;
        if(curAddress > highAddress) throw new IllegalArgumentException("Could not allocate enough memory");
        int highAddress = curAddress;
        curAddress++;

        return new Range(baseAddress, highAddress);
    }
}
