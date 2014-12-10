package de.hopp.generator.backends.workflow.ise.xps;

import static de.hopp.generator.backends.workflow.ise.xps.MHSUtils.add;
import static de.hopp.generator.model.mhs.MHS.*;
import static de.hopp.generator.utils.BoardUtils.getClockPort;
import static de.hopp.generator.utils.BoardUtils.getCore;
import static de.hopp.generator.utils.BoardUtils.getDirection;
import static de.hopp.generator.utils.BoardUtils.getHWQueueSize;
import static de.hopp.generator.utils.BoardUtils.getWidth;
import katja.common.NE;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.Memory;
import de.hopp.generator.backends.workflow.ise.ISEBoard;
import de.hopp.generator.backends.workflow.ise.gpio.GpioComponent;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.exceptions.UsageError;
import de.hopp.generator.model.*;
import de.hopp.generator.model.BDLFilePos.Visitor;
import de.hopp.generator.model.mhs.AndExp;
import de.hopp.generator.model.mhs.Attribute;
import de.hopp.generator.model.mhs.Block;
import de.hopp.generator.model.mhs.MHSFile;
import de.hopp.generator.utils.BoardUtils;

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
        mhs = MHSFile(Attributes(
            Attribute(PARAMETER(), Assignment("VERSION", Ident(versions.mhs)))
        ));

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

        // visit boards components
        visit(term.checksum());
        visit(term.gpios());
        visit(term.insts());
        visit(term.medium());

        // add generic design-dependent mhs components (that use variables set up in mhs)
        mhs = add(mhs, getDefault());
    }

    public void visit(GPIOPos term) {
        GpioComponent gpio;

        try {
            gpio = board.getGpio(term.name().term());
        } catch (IllegalArgumentException e) {
            errors.addError(new ParserError(e.getMessage(), term.pos().term()));
            return;
        }

        // allocate a 0xffff block in the board memory model
        Memory.Range gpioMemRange = board.getMemory().allocateMemory(0xffff);

        mhs = add(mhs, MHSFile(Attributes(
            Attribute(PORT(),
                Assignment(gpio.portID(), Ident(gpio.portID())),
                Assignment("DIR", Ident((gpio.isGPI() ? "I" : "") + (gpio.isGPO() ? "O" : ""))),
                Assignment("VEC", Range(gpio.width()-1,0))
            )), Block("axi_gpio",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident(gpio.instID().toLowerCase()))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.gpio))),
                Attribute(PARAMETER(), Assignment("C_GPIO_WIDTH", Number(gpio.width()))),
                // this one might still be problematic... if there is some sort of hybrid I/O component where
                // not ALL pins are bi-directional (or none at all?)
                // the width and isGPI/isGPO parameters need some fundamental changes...
                Attribute(PARAMETER(), Assignment("C_ALL_INPUTS", Number(gpio.isGPI() ? 1 : 0))),
                Attribute(PARAMETER(), Assignment("C_INTERRUPT_PRESENT", Number(1))),
                Attribute(PARAMETER(), Assignment("C_IS_DUAL", Number(gpio.isGPI() && gpio.isGPO() ? 1 : 0))),
                Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr(gpioMemRange.getBaseAddress()))),
                Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr(gpioMemRange.getHighAddress()))),
                Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
                // the clock port is different for each board! only the clock itself remains the same...
                Attribute(PORT(), Assignment("S_AXI_ACLK", Ident(board.getClock().getClockPort(100)))),
                Attribute(PORT(), Assignment("GPIO_IO_" + (gpio.isGPI() ? "I" : "") + (gpio.isGPO() ? "O" : ""),
                    Ident(gpio.portID()))),
                Attribute(PORT(), Assignment("IP2INTC_Irpt", Ident(gpio.getINTCPort())))
            )));

        if(gpio.isGPI())
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
                clk.name(), Ident(board.getClock().getClockPort(clk.frequency())))));
        RST rst = BoardUtils.getResetPort(getCore(term));
        curBlock = add(curBlock, Attribute(PORT(), Assignment(
                rst.name(), Ident(rst.polarity() ? getResetPort(): getAResetNPort()))));

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
    public void visit(CLKPos   term) { } //frequencies.add(term.frequency().term()); }

    // the checksum of the file
    public void visit(ChecksumPos checksumPos) throws NE { }

    // imports and backends should be handled before this visitor
    public void visit(ImportsPos  term) { }
    public void visit(ImportPos   term) { }

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
    protected static boolean direction(Direction direction) throws UsageError {
        return direction.Switch(new Direction.Switch<Boolean, UsageError>() {
            public Boolean CaseIN  (  IN term) { return true;  }
            public Boolean CaseOUT ( OUT term) { return false; }
            public Boolean CaseDUAL(DUAL term) throws UsageError {
                throw new UsageError("no bi-directional components supported yet");
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
            Attribute(PORT(), Assignment("clk", Ident(board.getClock().getClockPort(100)))),
            Attribute(PORT(), Assignment("rst", Ident(getResetPort())))
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
            Attribute(PORT(), Assignment("CLK", Ident(board.getClock().getClockPort(100)))),
            Attribute(PORT(), Assignment("RST", Ident(getResetPort())))
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
     * Responsible for generating those parts of the .mhs file,
     * that are added independent of the board design.
     * They may however be parameterised depending on the
     * board design.
     * The values for these parameters are generated automatically
     * while visting.
     *
     * For example, some sort of connection between the board
     * processor and axi components is always required.
     * The number of components deployed on the board may still
     * influence this connection.
     * @return Parts of the .mhs file, that are required independently
     *   from the board design.
     */
    protected abstract MHSFile getDefault();

    /**
     * Returns the reset port of the .mhs visitor for the concrete board.
     * This is the peripheral reset port to be used by all programmable
     * logic components of the user. It does not necessarily reset
     * the complete board or its cpu.
     *
     * @return The reset port of the .mhs visitor for the concrete board.
     */
    protected abstract String getResetPort();

    /**
     * Returns the aresetn port of the .mhs visitor for the concrete board.
     * This is the peripheral reset port to be used by all programmable
     * logic components of the user. It does not necessarily reset
     * the complete board or its cpu.
     *
     * @return The aresetn port of the mhs visitor for the concrete board.
     */
    protected abstract String getAResetNPort();
}
