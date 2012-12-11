package de.hopp.generator;

import katja.common.NE;
import de.hopp.generator.board.*;
import de.hopp.generator.board.Board.Visitor;
import de.hopp.generator.model.*;

import static de.hopp.generator.model.Model.*;

public class BoardVisitor extends Visitor<NE>{

    // TODO use these flags
    private boolean debug;
    private boolean includedGPIOChannel;
    private boolean includedInterruptSetup;
    
    private MFile file;
    
    private MMethod init;
    
    public BoardVisitor(boolean debug) {
        
        this.debug = debug;

        includedGPIOChannel    = false;
        includedInterruptSetup = false;
        
        file = MFile("name", MDefinitions(), MStructs(), MEnums(), MAttributes(), MMethods(), MClasses());
        
        init = MMethod(MModifiers(), MType("int"), "init", MParameters(), MCode(Strings()));
    }
    
    public MFile getFile() {
        return file;
    }
    
    public void visit(Board term) {
        
        // visit all components
        visit(term.components());
        
        // add the init method to the file
        init = appendCode(init, MCode(Strings("", "return XST_SUCCESS;")));
        file = append(file, init);
        
        // add the cleanup method to the file
//        MMethod debugMethod = MMethod(MModifiers(), MType("int"), "cleanup", MParameters(), MCode(Strings()));
//        if(debug) debugMethod = appendCode(debugMethod, MCode(Strings("if(DEBUG == 1) xil_printf(\"\\ncleaning up\");", "")));
//        debugMethod = appendCode(debugMethod, MCode(Strings(
//                "// cleanup platform",
//                "cleanup_platform();",
//                "",
//                "return XST_SUCCESS;"
//                )));
//        file = append(file, debugMethod);
        
        Strings debugCode = Strings();
        if(debug) debugCode = debugCode.addAll(Strings("xil_printf(\"\\ncleaning up\");", ""));
        debugCode = debugCode.addAll(Strings(
                "// cleanup platform",
                "cleanup_platform();",
                "",
                "return XST_SUCCESS;"
                ));
        file = append(file, MMethod(MModifiers(), MType("int"), "cleanup", MParameters(), MCode(debugCode)));
        
        // add the main method to the file
        Strings initCode = Strings();
        if(debug) initCode = initCode.addAll(Strings("xil_printf(\"\\nstarting up\\n\");", ""));
        initCode = initCode.addAll(Strings(
                "// initialize board components",
                "init();",
                "",
                "// set LED state to Switch state", // TODO sample application!
                "setLED(getSwitches());",
                "",
                "// cleanup",
                "cleanup_platform();",
                "",
                "return 0;"
                ));
        file = append(file, MMethod(MModifiers(), MType("int"), "main", MParameters(), MCode(initCode)));
        
    }
    
    public void visit(Components term) {
        for(Component c : term) visit(c); 
    }
    
    // TODO communication interfaces (not implemented yet)
    public void visit(UART term)          { }
    public void visit(ETHERNET_LITE term) { }
    public void visit(ETHERNET term)      { }
    public void visit(PCIE term)          { }

    // Gpio components
    public void visit(LEDS term) {

        // add GPIO channel
        if(!includedGPIOChannel) {
            addGPIOChannel(file);
            includedGPIOChannel = true;
        }
        
        // add an attribute
        file = append(file, MAttribute(MModifiers(), MType("XGpio"), "leds", MCodeFragment("")));
        
        // add led initialisation
        Strings initCode = Strings("", "// initialise LEDs");
            
        if(debug) initCode = initCode.add("xil_printf(\"\\ninitialising LEDs ...\");");
        
        initCode = initCode.addAll(Strings(
                "if (XGpio_Initialize(&leds, XPAR_LEDS_8BITS_DEVICE_ID) != XST_SUCCESS) return XST_FAILURE;",
                "XGpio_SetDataDirection(&leds, GPIO_CHANNEL1, 0x0);",
                "XGpio_DiscreteWrite(&leds, GPIO_CHANNEL1, 0x0);"));
        
        if(debug) initCode = initCode.add("xil_printf(\" done\");");
        
        init = appendCode(init, MCode(initCode));
        
        // add a method for setting the led state
        Strings setStateCode = Strings(
                "XGpio_DiscreteWrite(&leds, GPIO_CHANNEL1, value);",
                "return XST_SUCCESS;");
        
        MMethod setState = MMethod(MModifiers(), MType("int"), "setLED", MParameters(
                MParameter(VALUE(), MType("u32"), "value")), MCode(setStateCode));

        file = append(file, setState);
    }
    
    public void visit(SWITCHES term) {
        
        // add interrupt setup
        if(!includedInterruptSetup) {
            file = addInterruptSetup();
            includedInterruptSetup = true;
        }
        
        // add GPIO channel
        if(!includedGPIOChannel) {
            file = addGPIOChannel(file);
            includedGPIOChannel = true;
        }
        
        // add an attribute
        file = append(file, MAttribute(MModifiers(), MType("XGpio"), "switches", MCodeFragment("")));
        
        // add switch initialisation
        Strings initCode = Strings("", "// initialise Switches");
            
        if(debug) initCode = initCode.add("xil_printf(\"\\ninitialising Switches ...\");");
        
        initCode = initCode.addAll(Strings(
                "if (XGpio_Initialize(&switches, XPAR_DIP_SWITCHES_8BITS_DEVICE_ID) != XST_SUCCESS) return XST_FAILURE;",
                "XGpio_SetDataDirection(&switches, GPIO_CHANNEL1, 0xFFFFFFFF);",
                "if (GpioSetupIntrSystem(&switches, XPAR_MICROBLAZE_0_INTC_DIP_SWITCHES_8BITS_IP2INTC_IRPT_INTR,",
                "        (Xil_ExceptionHandler)GpioHandlerSwitches) != XST_SUCCESS) return XST_FAILURE;"
                ));
        
        init = appendCode(init, MCode(initCode));
        
        // add a method for reading the current switch state
        Strings getStateCode = Strings("return XGpio_DiscreteRead(&switches, GPIO_CHANNEL1);");
        
        MMethod getState = MMethod(MModifiers(), MType("u32"), "getSwitches", MParameters(), MCode(getStateCode));
        file = append(file, getState);
        
        // add the interrupt handler
        Strings intrCode = Strings(
                "XGpio *GpioPtr = (XGpio *)CallbackRef;",
                "",
                "// Test application: set LED state to Switch state", // TODO sample application!
                "setLED(getSwitches());",
                "",
                "// Clear the Interrupt",
                "XGpio_InterruptClear(GpioPtr, GPIO_CHANNEL1);"
                );
        
        MMethod intr = MMethod(MModifiers(), MType("void"), "GpioHandlerSwitches", MParameters(
                MParameter(VALUE(), MPointerType(MType("void")), "CallbackRef")), MCode(intrCode));
        file = append(file, intr);
    }
    
    public void visit(BUTTONS term) {

        // add interrupt setup
        if(!includedInterruptSetup) {
            file = addInterruptSetup();
            includedInterruptSetup = true;
        }
        
        // add GPIO channel
        if(!includedGPIOChannel) {
            file = addGPIOChannel(file);
            includedGPIOChannel = true;
        }
        
        // add an attribute
        file = append(file, MAttribute(MModifiers(), MType("XGpio"), "buttons", MCodeFragment("")));
        
        // add switch initialisation
        Strings initCode = Strings("", "// initialise Buttons");
            
        if(debug) initCode = initCode.add("xil_printf(\"\\ninitialising Buttons ...\");");
        
        initCode = initCode.addAll(Strings(
                "if (XGpio_Initialize(&buttons,  XPAR_PUSH_BUTTONS_5BITS_DEVICE_ID) != XST_SUCCESS) return XST_FAILURE;",
                "XGpio_SetDataDirection(&buttons,  GPIO_CHANNEL1, 0xFFFFFFFF);",
                "if (GpioSetupIntrSystem(&buttons,  XPAR_MICROBLAZE_0_INTC_PUSH_BUTTONS_5BITS_IP2INTC_IRPT_INTR,",
                "        (Xil_ExceptionHandler)GpioHandlerButtons) != XST_SUCCESS) return XST_FAILURE;"
                ));
        
        init = appendCode(init, MCode(initCode));
        
        // add a method for reading the current button state
        Strings getStateCode = Strings("return XGpio_DiscreteRead(&buttons, GPIO_CHANNEL1);");
        
        MMethod getState = MMethod(MModifiers(), MType("u32"), "getButtons", MParameters(), MCode(getStateCode));
        file = append(file, getState);
        
        // add the interrupt handler
        Strings intrCode = Strings(
                "XGpio *GpioPtr = (XGpio *)CallbackRef;",
                "",
                "// Test application: print out some text", // TODO sample application!
                "xil_printf(\"\\nhey - stop pushing!! %d\", getButtons());",
                "",
                "// Clear the Interrupt",
                "XGpio_InterruptClear(GpioPtr, GPIO_CHANNEL1);"
                );
        
        MMethod intr = MMethod(MModifiers(), MType("void"), "GpioHandlerButtons", MParameters(
                MParameter(VALUE(), MPointerType(MType("void")), "CallbackRef")), MCode(intrCode));
        file = append(file, intr);
    }

    public void visit(IP term) {
        // TODO Auto-generated method stub
    }

    // literals
    public void visit(String term)  { }
    public void visit(Integer term) { }

    // helper methods to append components to a file
    private static MFile append(MFile file, MDefinition def) {
        return file.replaceDefs(file.defs().add(def));
    }
    private static MFile append(MFile file, MAttribute attr) {
        return file.replaceAttributes(file.attributes().add(attr));
    }
    private static MFile append(MFile file, MStruct struct) {
        return file.replaceStructs(file.structs().add(struct));
    }
    private static MFile append(MFile file, MMethod method) {
        return file.replaceMethods(file.methods().add(method));
    }
    private static MFile append(MFile file, MEnum menum) {
        return file.replaceEnums(file.enums().add(menum));
    }
    private static MMethod appendCode(MMethod method, MCode code) {
        return method.replaceBody(MCode(
                method.body().lines().addAll(code.lines()),
                method.body().needed().addAll(code.needed())));
    }
    
    private static MFile addGPIOChannel(MFile file) {
        return append(file, MDefinition("GPIO_CHANNEL1", "1"));
    }
    
    private MFile addInterruptSetup() {
        
        // append interrupt setup to init method
        init = appendCode(init, MCode(Strings("", "if (IntcInterruptSetup() != XST_SUCCESS) return XST_FAILURE;")));
        
        // add interrupt attribute
        file = append(file, MAttribute(MModifiers(STATIC()), MType("XIntc"), "intc", MCodeFragment("")));
        
        // add method for interrupt setup
        Strings intrSetupCode = Strings();
        if(debug) intrSetupCode = intrSetupCode.addAll(Strings(
                "xil_printf(\"\\n  initialising interrupt controller driver ...\");", ""));
        intrSetupCode = intrSetupCode.addAll(Strings(
                "// initialize interrupt controller driver so that it is ready to use",
                "int Status = XIntc_Initialize(&intc, XPAR_MICROBLAZE_0_INTC_DEVICE_ID);",
                "if (Status != XST_SUCCESS) return Status;",
                "",
                "// Perform a self-test to ensure that the hardware was built  correctly.",
                "Status = XIntc_SelfTest(&intc);",
                "if (Status != XST_SUCCESS) return Status;",
                "",
                "// Initialize the exception table.",
                "Xil_ExceptionInit();",
                "",
                "// Register the interrupt controller handler with the exception table.",
                "// Xil_ExceptionRegisterHandler(XIL_EXCEPTION_ID_INT, (Xil_ExceptionHandler)XIntc_DeviceInterruptHandler, &intc);",
                "Xil_ExceptionRegisterHandler(XIL_EXCEPTION_ID_INT, (Xil_ExceptionHandler)XIntc_InterruptHandler, &intc);",
                "",
                "// Enable exceptions.",
                "Xil_ExceptionEnable();",
                "",
                "// Start the interrupt controller such that interrupts are enabled for all devices that cause interrupts.",
                "Status = XIntc_Start(&intc, XIN_REAL_MODE);",
                "if (Status != XST_SUCCESS) return Status;"
                ));
        if(debug) intrSetupCode = intrSetupCode.addAll(Strings(
                "", "xil_printf(\" done\");"
                ));
        intrSetupCode = intrSetupCode.addAll(Strings("", "return XST_SUCCESS;"));
        
        file = append(file, MMethod(MModifiers(), MType("int"), "IntcInterruptSetup", 
                MParameters(), MCode(intrSetupCode)));
        
        // add a convenience method for setup of components triggering interrupts
        intrSetupCode = Strings();
        if(debug) intrSetupCode = intrSetupCode.addAll(Strings(
                "xil_printf(\"\\n  hooking up interrupt service routines ...\");", ""));
        
        intrSetupCode = intrSetupCode.addAll(Strings(
                "// TODO I think THIS is the only interesting line and the rest can be removed (except perhaps exception management)",
                "// TODO provide better data than &buttons/&switches??",
                "// Hook up interrupt service routines",
                "XIntc_Connect(&intc, intrId,  handler, instancePtr);"
                ));
        if(debug) intrSetupCode = intrSetupCode.addAll(Strings("",
                "xil_printf(\" done\\n  enabling interrupt vectors at interrupt controller ...\");"
                ));
        
        intrSetupCode = intrSetupCode.addAll(Strings(
                "// Enable the interrupt vector at the interrupt controller",
                "XIntc_Enable(&intc, intrId);"
                ));
      
        if(debug) intrSetupCode = intrSetupCode.addAll(Strings(
                "", "xil_printf(\" done\\n  enabling GPIO channel interrupts ...\");"
                ));

        // Start the interrupt controller such that interrupts are recognized and handled by the processor
        // result = XIntc_Start(&intc, XIN_REAL_MODE);
        // if (result != XST_SUCCESS) return result;

        intrSetupCode = intrSetupCode.addAll(Strings(
                "/*",
                " * Enable the GPIO channel interrupts so that push button can be",
                " * detected and enable interrupts for the GPIO device",
                " */",
                "XGpio_InterruptEnable(instancePtr, GPIO_CHANNEL1);",
                "XGpio_InterruptGlobalEnable(instancePtr);"
                ));

        // maybe we need Xil_ExceptionInit() and Xil_ExceptionEnable() as well...
        
        if(debug) intrSetupCode = intrSetupCode.addAll(Strings(
                "", "xil_printf(\" done\");"
                ));
        
        intrSetupCode = intrSetupCode.addAll(Strings("", "return XST_SUCCESS;"));
        
        MMethod gpioIntrSetup = MMethod(MModifiers(), MType("int"), "GpioSetupIntrSystem", MParameters(
                MParameter(VALUE(), MPointerType(MType("XGpio")), "instancePtr"),
                MParameter(VALUE(), MType("u16"), "intrId"),
                MParameter(VALUE(), MType("Xil_ExceptionHandler"), "handler")
                ), MCode(intrSetupCode));
        
        return append(file, gpioIntrSetup);
    }
}