package de.hopp.generator.backends.board.zed;

import de.hopp.generator.backends.workflow.ise.xps.IPCoreVersions;
import de.hopp.generator.model.mhs.MHSFile;

/**
 * Clock model implementation for the Zed Board.
 *
 * Note, that frequencies are not allowed to be reordered here, since the port
 * identifier depends only on the order of frequencies.
 * @author Thomas Fischer
 *
 */
public class Clock implements de.hopp.generator.backends.workflow.ise.xps.Clock {

    @Override
    public String getClockPort(int frequency) {
        return getClockPort(frequency, true, false);
    }

    @Override
    public String getClockPort(int frequency, boolean buffered, boolean variablePhase) {
        if(! buffered)
            throw new IllegalArgumentException("Zed board clock model does not support unbuffered clocks");
        if(variablePhase)
            throw new IllegalArgumentException("Zed board clock model does not support clocks with variable phase");

             if(frequency == 100) return "processing_system7_0_FCLK_CLK0";
        else if(frequency == 150) return "processing_system7_0_FCLK_CLK1";
        else if(frequency ==  50) return "processing_system7_0_FCLK_CLK2";
        else throw new IllegalArgumentException(
            "Zed board clock model does only support the fixed frequencies 50, 100 and 150 MHz");
    }

    @Override
    public MHSFile getMHS(IPCoreVersions versions) {
        throw new UnsupportedOperationException("Distrinct clock mhs not required by the Zed mhs generator");
    }
}
