package de.hopp.generator.backends.workflow.ise.xps;

import static de.hopp.generator.backends.workflow.ise.xps.MHSUtils.add;
import static de.hopp.generator.parser.MHS.*;
import static de.hopp.generator.utils.BoardUtils.getClockPort;
import static de.hopp.generator.utils.BoardUtils.getCore;
import static de.hopp.generator.utils.BoardUtils.getDirection;
import static de.hopp.generator.utils.BoardUtils.getHWQueueSize;
import static de.hopp.generator.utils.BoardUtils.getResetPort;
import static de.hopp.generator.utils.BoardUtils.getWidth;

import java.util.HashSet;
import java.util.Set;

import katja.common.NE;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.workflow.ise.ISEBoard;
import de.hopp.generator.backends.workflow.ise.gpio.GpioComponent;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.exceptions.UsageError;
import de.hopp.generator.frontend.*;
import de.hopp.generator.frontend.BDLFilePos.Visitor;
import de.hopp.generator.parser.AndExp;
import de.hopp.generator.parser.Attribute;
import de.hopp.generator.parser.Block;
import de.hopp.generator.parser.MHSFile;

/**
 * Abstract XPS generation backend, providing some utility methods.
 * Also defines basic constants, that have to be instantiated by all extending versions.
 * @author Thomas Fischer
 *
 *
 * The cores described here are assumed to be compatible with all versions of XPS.
 * If this proves not to be the case in some earlier or later, unsupported version,
 * introduce a new abstract visitor subclass with adjusted cores and
 * rename this file accordingly to the earliest version number,
 * the core description is compatible with (e.g. XPS14).
 */
public abstract class MHSGenerator extends Visitor<NE> implements MHS {
    protected ErrorCollection errors;

    // the generated mhs file
    protected MHSFile mhs;
    protected ISEBoard board;

    // core versions of Xilinx standard catalogue
    protected IPCoreVersions versions;

    // temporary variables used to build up the mhs file
    protected Block curBlock;

    // counter variables
    protected int axiStreamIdMaster;
    protected int axiStreamIdSlave;

    // queue sizes
    protected int globalHWQueueSize;
    protected int globalSWQueueSize;

    protected Set<Integer> frequencies = new HashSet<Integer>();

    protected AndExp intrCntrlPorts = AndExp();

    // note, that the ISEBoard and IPCoreVersions may depend on the ISE versions.
    // a corresponding board / version pack must be selected in the actual mhs instances
    public MHSGenerator(ISEBoard board, IPCoreVersions versions, ErrorCollection errors) {
        this.board    = board;
        this.versions = versions;
        this.errors   = errors;
    }

    public MHSFile generateMHSFile(BDLFilePos file) {
        // initialise / reset variables
        mhs = MHSFile(Attributes());

        axiStreamIdMaster = 0;
        axiStreamIdSlave  = 0;

        // visit the provided bdl file
        visit(file);

        // return the generated mhs file
        return mhs;
    }

    public void visit(BDLFilePos term) {
        for(OptionPos opt : term.opts())
            if(opt instanceof HWQUEUEPos)
                globalHWQueueSize = ((HWQUEUEPos)opt).qsize().term();
            else if(opt instanceof SWQUEUEPos)
                globalSWQueueSize = ((SWQUEUEPos)opt).qsize().term();

        // add default blocks
        mhs = add(mhs, getDefaultParts());

        // visit boards components
        visit(term.gpios());
        visit(term.insts());
        visit(term.medium());

        // TODO this would remove the necessity for the AndExp and go over the complete file once more ;)
        // addINTC();

        // add clock generator
        mhs = add(mhs, getClk());

        // add processor and required cores for its AXI4 connection to the design
        mhs = add(mhs, getProcessorConnection());

    }

    public void visit(GPIOPos term) {
        GpioComponent gpio;

        try {
            gpio = board.getGpio(term.name().term());
        } catch (IllegalArgumentException e) {
            errors.addError(new ParserError(e.getMessage(), term.pos().term()));
            return;
        }

        mhs = add(mhs, gpio.getMHSAttribute());
        mhs = add(mhs, gpio.getMHSBlock(versions));

        addPortToInterruptController(gpio.getINTCPort());
    }

    public void visit(InstancePos term) {
        // begin a new instance using the instances name
        curBlock = Block(term.core().term());

        // reference core and version
        curBlock = add(curBlock, Attribute(PARAMETER(), Assignment("INSTANCE", Ident(term.name().term()))));
        curBlock = add(curBlock, Attribute(PARAMETER(), Assignment("HW_VER",   Ident(term.version().term()))));

        // define bus interfaces
        visit(term.bind());

        // append clock and reset ports
        CLK clk = getClockPort(getCore(term));
        curBlock = add(curBlock, Attribute(PORT(), Assignment(
                clk.name(), Ident("clk_" + clk.frequency() + "_0000MHzMMCM0"))));
        RST rst = getResetPort(getCore(term));
        curBlock = add(curBlock, Attribute(PORT(), Assignment(
                rst.name(), Ident("proc_sys_reset_0_Peripheral_" + (rst.polarity() ? "reset": "aresetn")))));

        // add the block to the file
        mhs = add(mhs, curBlock);
    }

    public void visit(AxisPos term) {
        curBlock = add(curBlock, Attribute(BUS_IF(), Assignment(term.port().term(), Ident(term.axis().term()))));
    }

    public void visit(CPUAxisPos axis) {
        try {
            curBlock = add(curBlock, createCPUAxisBinding(axis));
        } catch (UsageError e) {
            errors.addError(e);
        }
    }

    // loops for relevant list types
    public void visit(GPIOsPos     term) { for(GPIOPos     g : term) visit(g); }
    public void visit(CoresPos     term) { for(CorePos     c : term) visit(c); }
    public void visit(PortsPos     term) { for(PortPos     p : term) visit(p); }
    public void visit(InstancesPos term) { for(InstancePos i : term) visit(i); }
    public void visit(BindingsPos  term) { for(BindingPos  b : term) visit(b); }

    // clock frequencies
    public void visit(CorePos  term) { visit(term.ports()); }
    public void visit(CLKPos   term) { frequencies.add(term.frequency().term()); }

    // imports and backends should be handled before this visitor
    public void visit(ImportsPos  term) { }
    public void visit(ImportPos   term) { }
    public void visit(BackendsPos term) { }
    public void visit(BackendPos  term) { }

    // positions and directions are handled in their surrounding blocks
    public void visit(PositionPos term) { }
    public void visit(INPos       term) { }
    public void visit(OUTPos      term) { }
    public void visit(DUALPos     term) { }

    // nothing to add for no selected medium (should be an error anyway)
    public void visit(NONEPos term) { }

    // The following types are only relevant for SDK generation, not XPS.

    // ports
    public void visit(AXIPos   term) { }
    public void visit(RSTPos   term) { }

    // options
    public void visit(OptionsPos  term) { }
    public void visit(HWQUEUEPos  arg0) { }
    public void visit(SWQUEUEPos  arg0) { }
    public void visit(BITWIDTHPos term) { }
    public void visit(POLLPos     term) { }

    // logger options
    public void visit(LogsPos    term) { }
    public void visit(NOLOGPos   term) { }
    public void visit(CONSOLEPos term) { }
    public void visit(FILEPos    term) { }

    public void visit(ERRORPos   term) { }
    public void visit(WARNPos    term) { }
    public void visit(INFOPos    term) { }
    public void visit(FINEPos    term) { }
    public void visit(FINERPos   term) { }
    public void visit(FINESTPos  term) { }

    // Ethernet options
    public void visit(MOptionsPos term) { }
    public void visit(MACPos      term) { }
    public void visit(IPPos       term) { }
    public void visit(MASKPos     term) { }
    public void visit(GATEPos     term) { }
    public void visit(PORTIDPos   term) { }
    public void visit(DHCPPos     term) { }
    public void visit(TOUTPos     term) { }

    // code blocks
    public void visit(DEFAULTPos      term) { }
    public void visit(USER_DEFINEDPos term) { }
    public void visit(SchedulerPos    term) { }

    // literals
    public void visit(IntegerPos term) { }
    public void visit(BooleanPos term) { }
    public void visit(StringsPos term) { }
    public void visit(StringPos  term) { }

    // helpers creating parts of the file
    /**
     * Translates the direction attribute of the port in a boolean value.
     * @param direction Direction attribute of the port.
     * @return true if in-going, false otherwise.
     * @throws UsageError If the port is bi-directional, since those
     *                          ports are not supported using AXI4 stream interfaces.
     */
    private static boolean direction(Direction direction) throws UsageError {
        return direction.Switch(new Direction.Switch<Boolean, UsageError>() {
            public Boolean CaseIN  (  IN term) { return true;  }
            public Boolean CaseOUT ( OUT term) { return false; }
            public Boolean CaseDUAL(DUAL term) throws UsageError {
                throw new UsageError("invalid direction for virtex6");
            }
        });
    }

    /**
     * Generate an attribute representing the binding of an axis directly to the virtex6 processors
     * AXI4 stream interface.
     * @param axis The model representation of the binding.
     * @return The generated attribute representing the binding.
     * @throws UsageError If a parameter of the port is invalid for this context.
     */
    protected Attribute createCPUAxisBinding(final CPUAxisPos axis) throws UsageError {

        boolean direct = direction(getDirection(axis).termDirection());
        int width = getWidth(axis);
        int queueSize = getHWQueueSize(axis);

        String axisGroup   = direct ? "M" + axiStreamIdMaster++ : "S" + axiStreamIdSlave++;
        String currentAxis = axisGroup + "_AXIS";

        // add multiplexing, if required
        currentAxis = addMux(axisGroup, currentAxis, direct, width);

        // add queueing, if required
        currentAxis = addQueue(axisGroup, currentAxis, direct, width, queueSize);

        // connect the component to the last axis
        return Attribute(BUS_IF(), Assignment(axis.port().term(), Ident(currentAxis)));
    }

    /**
     * Adds a queue to an AXI stream interface.
     *
     * One end of the queue (depending on the direction attribute) is connected to the provided axis.
     * The identifier of the axis connected to the other end is returned by the method and has to be
     * connected to some other component.
     *
     * @param axisGroup Basic identifier used to construct all axis identifiers of the corresponding port.
     *   Usually consists of the direction of the port and a number.
     * @param currentAxis Axis, which will be connected to one end of the queue (depending on direction).
     * @param d Direction of the connected port. The corresponding end of the queue is connected
     *   to the provided axis.
     * @param width Bitwidth of values to be stored in the queue.
     * @param depth Number of values that can be stored in the queue. If set to 0, no queue is added.
     * @return Open axis of the queue.
     * @throws UsageError If any parameter for the queue is invalid (e.g. negative width).
     */
    private String addQueue(String axisGroup, String currentAxis, boolean d, int width, int depth) throws UsageError {

        if(depth == 0) return currentAxis;
        if(depth < 0) throw new UsageError("negative queue size");

        String queueAxis = axisGroup + "_QUEUE_AXIS";

        // add a queue component in between the component and the microblaze
        mhs = add(mhs, Block("Queue",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident(axisGroup.toLowerCase() + "_queue"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident("1.00.a"))),
            Attribute(PARAMETER(), Assignment("G_DEPTH", Number(depth))),
            Attribute(PARAMETER(), Assignment("G_BW", Number(width))),
            Attribute(BUS_IF(), Assignment("in", Ident(d ? currentAxis : queueAxis))),
            Attribute(BUS_IF(), Assignment("out", Ident(d ? queueAxis : currentAxis))),
            Attribute(PORT(), Assignment("clk", Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("rst", Ident("proc_sys_reset_0_Peripheral_reset")))
            ));

        // return the axis identifier of the queues port, that should be attached to the components port
        return queueAxis;
    }

    /**
     * Adds a bitwidth resizer to an AXI stream interface.
     *
     * One end of the resizer (depending on the direction attribute) is connected to the provided axis.
     * The identifier of the axis connected to the other end is returned by the method and has to be
     * connected to some other component.
     *
     * The direction and bitwidth also determines, if an upsizer or downsizer is used.
     *
     * @param axisGroup Basic identifier used to construct all axis identifiers of the corresponding port.
     *   Usually consists of the direction of the port and a number.
     * @param currentAxis Axis, which will be connected to one end of the resizer (depending on direction).
     * @param d Direction of the connected port. The corresponding end of the resizer is connected
     *   to the provided axis.
     * @param width Depending on the direction, either source or target bitwidth. The other is always 32bit
     *   (the width of the processors AXI stream interface). If the width is also 32bit, no translation
     *   is required and no resizer is added.
     * @return Open axis of the resizer.
     * @throws UsageError If any parameter is invalid (e.g. negative width).
     */
    private String addMux(String axisGroup, String currentAxis, boolean d, int width) throws UsageError {

        if(width == 32) return currentAxis;
        if(width <= 0) throw new UsageError("encountered port with bitwidth of " + width);

        boolean up = ((width < 32) && !d) || ((width > 32) && d);

        String muxAxis  = axisGroup + "_MUX_AXIS";

        // depending on direction and bitwidth, we need an up- or downsizer
        mhs = add(mhs, Block(up ? "Upsizer" : "Downsizer",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident(axisGroup.toLowerCase() + "_mux"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident("1.00.a"))),
            Attribute(PARAMETER(), Assignment("WIDTH_IN",  Number(d ? 32 : width))),
            Attribute(PARAMETER(), Assignment("WIDTH_OUT", Number(d ? width : 32))),
            Attribute(BUS_IF(), Assignment("S_AXIS", Ident(d ? currentAxis : muxAxis))),
            Attribute(BUS_IF(), Assignment("M_AXIS", Ident(d ? muxAxis : currentAxis))),
            Attribute(PORT(), Assignment("CLK", Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("RST", Ident("proc_sys_reset_0_Peripheral_reset")))
            ));

        return muxAxis;
    }

    /**
     * Adds a port descriptor to the interrupt controller port list.
     * This list is later used add the interrupt controller core.
     * @param port Port to be added
     */
    protected void addPortToInterruptController(String port) {
        intrCntrlPorts = intrCntrlPorts.add(Ident(port));
    }

    /**
     * Generates the default parameters and blocks always required by the design.
     * These are typically board specific and should therefore be provided by the
     * board visitors.
     *
     * This procedure is called last of the generation phase. All variables provided
     * by the default generator are completely instantiated at this point.
     * @return Board-specific default parameters and blocks
     */
    protected abstract MHSFile getDefaultParts();

    /**
     * Generates attributes and blocks required to connect components to the boards
     * processor. If the board has no processor integrated, this will also have to
     * generate a soft-processor.
     * @return Board-specific processor connection attributes and blocks
     */
    // TODO exceptions?
    protected abstract MHSFile getProcessorConnection();


    /**
     * Adds the clock generator to the design.
     *
     * Depending on the described file, additional output frequencies might be required.
     * Per default, 100, 200, 400 (buffered and unbuffered) MHz are provided.
     *
     * The BDL allows all positive integer frequencies (though XPS will probably fail with some
     * higher frequencies...).
     *
     * @return Board-specific clock attributes and blocks
     */
    protected abstract MHSFile getClk();
}
