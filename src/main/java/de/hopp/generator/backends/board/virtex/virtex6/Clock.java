package de.hopp.generator.backends.board.virtex.virtex6;

import static de.hopp.generator.backends.workflow.ise.xps.MHSUtils.add;
import static de.hopp.generator.model.mhs.MHS.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.hopp.generator.backends.workflow.ise.xps.IPCoreVersions;
import de.hopp.generator.model.mhs.Attributes;
import de.hopp.generator.model.mhs.Block;
import de.hopp.generator.model.mhs.MHSFile;

public class Clock implements de.hopp.generator.backends.workflow.ise.xps.Clock {

    class Frequency implements Comparable<Frequency>{
        final int frequency;
        final boolean buffered;
        final boolean variablePhase;

        public Frequency(int frequency, boolean buffered, boolean variablePhase) {
            this.frequency = frequency;
            this.buffered = buffered;
            this.variablePhase = variablePhase;
        }

        String print() {
            return "clk_" + frequency + "_0000MHzMMCM0" +
                (buffered ? "" : "_nobuf") +
                (variablePhase ? "_varphase" : "");
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (o instanceof Frequency) {
                Frequency f = (Frequency)o;

                return(frequency == f.frequency &&
                    buffered == f.buffered &&
                    variablePhase == f.variablePhase);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int hashCode = 17 + frequency;
            hashCode = 17 * hashCode + Boolean.valueOf(buffered).hashCode();
            hashCode = 17 * hashCode + Boolean.valueOf(variablePhase).hashCode();
            return hashCode;
        }

        public int compareTo(Frequency f) {
            if(frequency != f.frequency) return (frequency < f.frequency) ? -1 : 1;
            else if(buffered != f.buffered) return buffered ? -1 : 1;
            else if(variablePhase != f.variablePhase) return variablePhase ? 1 : -1;
            else return 0;
        }
    }

    private boolean isWritten = false;
    private List<Frequency> frequencies = new LinkedList<Frequency>();

    @Override
    public String getClockPort(int frequency) {
        return getClockPort(frequency, true, false);
    }

    @Override
    public String getClockPort(int frequency, boolean buffered, boolean variablePhase) {
        Frequency f = new Frequency(frequency, buffered, variablePhase);

        if(! frequencies.contains(f)) {
            if(isWritten) throw new IllegalStateException("created new clock port after writing clock component");

            frequencies.add(f);
        }

        return f.print();
    }

    @Override
    public MHSFile getMHS(IPCoreVersions versions) {
        Block timer = Block("clock_generator",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("clock_generator_0"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.clock_generator))),
            Attribute(PARAMETER(), Assignment("C_CLKIN_FREQ", Number(200000000)))
            );

        Attributes ports = Attributes(
            Attribute(PORT(), Assignment("LOCKED", Ident("proc_sys_reset_0_Dcm_locked"))),
            Attribute(PORT(), Assignment("CLKIN", Ident("CLK"))),
            Attribute(PORT(), Assignment("PSCLK", Ident(getClockPort(200)))),
            Attribute(PORT(), Assignment("PSEN", Ident("psen"))),
            Attribute(PORT(), Assignment("PSINCDEC", Ident("psincdec"))),
            Attribute(PORT(), Assignment("PSDONE", Ident("psdone"))),
            Attribute(PORT(), Assignment("RST", Ident("RESET")))
            );

        int freqCounter = 0;

        Collections.sort(frequencies);
        for(Frequency frequency : frequencies) {
            timer = add(timer, Attribute(PARAMETER(),
                Assignment("C_CLKOUT" + freqCounter + "_FREQ", Number(frequency.frequency * 1000000))));
            timer = add(timer, Attribute(PARAMETER(),
                Assignment("C_CLKOUT" + freqCounter + "_GROUP", Ident("MMCM0"))));
            if(!frequency.buffered)
                timer = add(timer, Attribute(PARAMETER(),
                    Assignment("C_CLKOUT" + freqCounter + "_BUF", Ident("FALSE"))));
            if(frequency.variablePhase)
                timer = add(timer, Attribute(PARAMETER(),
                    Assignment("C_CLKOUT" + freqCounter + "_VARIABLE_PHASE", Ident("TRUE"))));
            ports = ports.add(Attribute(PORT(),
                Assignment("CLKOUT" + freqCounter, Ident(frequency.print()))));
            freqCounter++;
        }

        timer = add(timer, ports);

        isWritten = true;

        return MHSFile(Attributes(), timer);
    }
}
