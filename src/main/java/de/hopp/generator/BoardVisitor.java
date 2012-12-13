package de.hopp.generator;

import katja.common.NE;
import de.hopp.generator.board.*;
import de.hopp.generator.board.Board.Visitor;
import de.hopp.generator.model.*;

import static de.hopp.generator.model.Model.*;

public class BoardVisitor extends Visitor<NE>{

    private boolean debug;
    private boolean includedGPIOChannel;
    private boolean includedInterruptSetup;
    
    private MFile file;
    
    private MMethod init;
    private MMethod intr;
    private MMethod main;
    private MMethod clean;
    
    public BoardVisitor(boolean debug) {
        
        this.debug = debug;

        includedGPIOChannel    = false;
        includedInterruptSetup = false;
        
        file  = MFile("name", MDefinitions(), MStructs(), MEnums(), MAttributes(), MMethods(), MClasses());
        
        init  = MMethod(MModifiers(), MType("int"), "init",      MParameters(), MCode(Strings("", "init_platform();"),
                MInclude("platform.h", QUOTES())));
        intr  = MMethod(MModifiers(), MType("int"), "intrSetup", MParameters(), MCode(Strings("")));
        clean = MMethod(MModifiers(), MType("int"), "cleanup",   MParameters(), MCode(Strings(""),
                MInclude("platform.h", QUOTES())));
        main  = MMethod(MModifiers(), MType("int"), "main",      MParameters(), MCode(Strings("", 
                "// initialize board components", "init();")));
        
    }
    
    public MFile getFile() {
        return file;
    }
    
    public void visit(Board term) {
        
        // add initial code to default methods
        if(debug) {
            main  = appendCode(main,  MCode(Strings("", "xil_printf(\"\\nstarting up\\n\");")));
            clean = appendCode(clean, MCode(Strings("", "xil_printf(\"\\ncleaning up\\n\");")));
        }
        
        // visit all components
        visit(term.components());
        
        // add final code to default methods
        intr  = appendCode(intr,  MCode(Strings("", "return XST_SUCCESS;")));
        init  = appendCode(init,  MCode(Strings("", "return XST_SUCCESS;")));
        clean = appendCode(clean, MCode(Strings(
                "",
                "// cleanup platform",
                "cleanup_platform();",
                "",
                "return XST_SUCCESS;"
                )));
        main  = appendCode(main,  MCode(Strings(
                "",
                "// call cleanup",
                "if(cleanup() != XST_SUCCESS) return XST_FAILURE;",
                "",
                "return XST_SUCCESS;"
                )));
        
        // add default methods to file
        file = append(file, intr);
        file = append(file, init);
        file = append(file, clean);
        file = append(file, main);

    }
    
    public void visit(Components term) {
        for(Component c : term) visit(c); 
    }
    
    // communication interfaces (partially implemented)
    public void visit(UART term)          { }
    public void visit(ETHERNET_LITE term) { 
        
        // append port definition
        file = append(file, MDefinition("PORT", "8844"));
        
        // append attribute declarations for Ethernet connection
        file = append(file, MAttribute(MModifiers(), MArrayType(MType("unsigned char"), 6),
                "mac_ethernet_address", MCodeFragment("{ 0x00, 0x0a, 0x35, 0x00, 0x01, 0x02 }")));
        file = append(file, MAttribute(MModifiers(), MType("struct ip_addr"), "ipaddr, netmask, gw", MCodeFragment("")));
        file = append(file, MAttribute(MModifiers(), MType("struct netif"), "netif", MCodeFragment("")));
        
        // Initialise ip addresses in init method
        Strings initCode = Strings(
                "",
                "// initialize IP addresses to be used",
                "IP4_ADDR(&ipaddr,  " + unparseIP(term.ip())   + ");",
                "IP4_ADDR(&netmask, " + unparseIP(term.mask()) + ");",
                "IP4_ADDR(&gw,      " + unparseIP(term.gw())   + ");",
                "",
                "lwip_init();",
                "",
                "// add network interface to the netif_list, and set it as default",
                "if (!xemac_add(&netif, &ipaddr, &netmask, &gw, mac_ethernet_address, PLATFORM_EMAC_BASEADDR)) {",
                "    xil_printf(\"Error adding N/W interface\\r\\n\");",
                "    return -1;",
                "}",
                "",
                "// set network interface as default",
                "netif_set_default(&netif);",
                "",
                /* Create a new DHCP client for this interface.
                 * Note: you must call dhcp_fine_tmr() and dhcp_coarse_tmr() at
                 * the predefined regular intervals after starting the client.
                 */
                /* dhcp_start(netif); */
                "// now enable interrupts",
                "platform_enable_interrupts();",
                "",
                "// specify that the network if is up",
                "netif_set_up(&netif);",
                "",
                "// start the application",
                "start_application();"
                );
        
        init = appendCode(init, MCode(initCode,
                MInclude("lwip/init.h",       QUOTES()),
                MInclude("platform.h",        QUOTES()),
                MInclude("platform_config.h", QUOTES())));
    
        // append listening loop to main method
        main = appendCode(main, MCode(Strings(
                "",
                "while(1) {",
                "    xemacif_input(&netif);",
//                "    transfer_data();",
                "}"
                )));

        // add code block to interrupt methods
        file = addEthernetInterruptSetup();
        
        // add methods aligning memory addresses for Microblaze
        file = addAlignmentMethods();
        
        // add callback methods responding to incoming packages
        file = addCallbackMethods();
        
        // add startup method starting Ethernet service
        file = addStartApplicationMethod();
        
    }
    
    public void visit(ETHERNET term)      { }
    public void visit(PCIE term)          { }

    // GPIO components
    public void visit(LEDS term) {

        // add GPIO channel
        if(!includedGPIOChannel) {
            addGPIOChannel();
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
        
        init = appendCode(init, MCode(initCode,
                MInclude("xgpio.h", QUOTES()),
                MInclude("xintc.h", QUOTES())));
        
        // TODO sample application for LEDs
        main = appendCode(main, MCode(Strings(
                "",
                "// set LED state to Switch state",
                "setLED(getSwitches());"
                )));
        
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
            file = addBasicInterruptSetup();
            includedInterruptSetup = true;
        }
        
        // add GPIO channel
        if(!includedGPIOChannel) {
            file = addGPIOChannel();
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
        
        init = appendCode(init, MCode(initCode,
                MInclude("xgpio.h",         QUOTES()),
                MInclude("xil_exception.h", QUOTES()),
                MInclude("xintc.h",         QUOTES())));
        
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
                MParameter(VALUE(), MPointerType(MType("void")), "CallbackRef")),
                MCode(intrCode, MInclude("xgpio.h", QUOTES())));
        file = append(file, intr);
    }
    
    public void visit(BUTTONS term) {

        // add interrupt setup
        if(!includedInterruptSetup) {
            file = addBasicInterruptSetup();
            includedInterruptSetup = true;
        }
        
        // add GPIO channel
        if(!includedGPIOChannel) {
            file = addGPIOChannel();
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
        
        init = appendCode(init, MCode(initCode,
                MInclude("xgpio.h",         QUOTES()),
                MInclude("xil_exception.h", QUOTES()),
                MInclude("xintc.h",         QUOTES())));
        
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
                MParameter(VALUE(), MPointerType(MType("void")), "CallbackRef")),
                MCode(intrCode, MInclude("xgpio.h", QUOTES())));
        file = append(file, intr);
    }

    // literals
    public void visit(IP ip) { }
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
    
    private MFile addCallbackMethods() {
        
        MMethod recv = MMethod(MModifiers(), MType("err_t"), "recv_callback", MParameters(
                    MParameter(VALUE(), MPointerType(MType("void")), "arg"),
                    MParameter(VALUE(), MPointerType(MType("struct tcp_pcb")), "tpcb"),
                    MParameter(VALUE(), MPointerType(MType("struct pbuf")), "p"),
                    MParameter(VALUE(), MType("err_t"), "err")
                ), MCode(Strings(
                    "/* do not read the packet if we are not in ESTABLISHED state */",
                    "if (!p) {",
                    "    tcp_close(tpcb);",
                    "    tcp_recv(tpcb, NULL);",
                    "    return ERR_OK;",
                    "}",
                    "",
                    "/* indicate that the packet has been received */",
                    "tcp_recved(tpcb, p->len);",
                    "",
                    "printBuffer(*p);",
                    "",
                    "int s = sum(*p);",
                    "",
                    // unsigned int i;
                    // for (i=0; i<(p->len/4); i++){
                    //     data = get_unaligned((void*)(((int)(p->payload))+(4*i)));
                    //     xil_printf("received value: %d\n", readInt(p));
                    // TODO (later) Do something useful with the payload
                    // 
                    // }
                    // 
                    "/* echo back the payload */",
                    "/* in this case, we assume that the payload is < TCP_SND_BUF */",
                    "err = tcp_write(tpcb, &s, 4, 1);",
                    "if (! tcp_sndbuf(tpcb) > p->len) xil_printf(\"no space in tcp_sndbuf\\r\\n\");",
                    "",
                    "/* free the received pbuf */",
                    "pbuf_free(p);",
                    "",
                    "return ERR_OK;"
            ), MInclude("netif/xadapter.h", QUOTES()), MInclude("lwip/tcp.h", QUOTES())));

        MMethod accept = MMethod(MModifiers(), MType("err_t"), "accept_callback", MParameters(
                    MParameter(VALUE(), MPointerType(MType("void")), "arg"),
                    MParameter(VALUE(), MPointerType(MType("struct tcp_pcb")), "newpcb"),
                    MParameter(VALUE(), MType("err_t"), "err")
                ), MCode(Strings(
                    "static int connection = 1;",
                    "",
                    "/* set the receive callback for this connection */",
                    "tcp_recv(newpcb, recv_callback);",
                    "",
                    "/* just use an integer number indicating the connection id as the callback argument */",
                    "tcp_arg(newpcb, (void*)connection);",
                    "",
                    "/* increment for subsequent accepted connections */",
                    "connection++;",
                    "",
                    "return ERR_OK;"
                ), MInclude("netif/xadapter.h", QUOTES())));
        
        return append(append(file, recv), accept);
    }
    
    private MFile addStartApplicationMethod() {
        Strings startMethodCode = Strings(
                "struct tcp_pcb *pcb;",
                "err_t err;",
                "",
                "/* create new TCP PCB stucture */",
                "pcb = tcp_new();",
                "if (!pcb) {",
                "    xil_printf(\"Error creating PCB. Out of Memory\\n\\r\");",
                "    return -1;",
                "}",
                "",
                "/* bind to specified port */",
                "err = tcp_bind(pcb, IP_ADDR_ANY, PORT);",
                "if (err != ERR_OK) {",
                "    xil_printf(\"Unable to bind to port %d: err = %d\\n\\r\", PORT, err);",
                "    return -2;",
                "}",
                "",
                "/* we do not need any arguments to callback functions */",
                "tcp_arg(pcb, NULL);",
                "",
                "/* listen for connections */",
                "pcb = tcp_listen(pcb);",
                "if (!pcb) {",
                "    xil_printf(\"Out of memory while tcp_listen\\n\\r\");",
                "    return -3;",
                "}",
                "",
                "/* specify callback to use for incoming connections */",
                "tcp_accept(pcb, accept_callback);"
                );
        
        if(debug) startMethodCode = startMethodCode.addAll(Strings(
                "",
                "xil_printf(\"TCP echo server started @ port %d\\n\\r\", PORT);"
                ));
        startMethodCode = startMethodCode.addAll(Strings(
                "",
                "return 0;"
                ));
                
         return append(file, MMethod(MModifiers(), MType("int"), "start_application",
                 MParameters(), MCode(startMethodCode,
                         MInclude("netif/xadapter.h", QUOTES()),
                         MInclude("lwip/tcp.h",       QUOTES()))));       
    }
    
    private MFile addAlignmentMethods() {
        
        // write a 32bit value to an unaligned address
        MMethod setUnaligned = MMethod(MModifiers(INLINE()), MType("void"), "set_unaligned", MParameters(
                MParameter(VALUE(), MPointerType(MType("int")), "target"),
                MParameter(VALUE(), MPointerType(MType("int")), "data")
                ), MCode(Strings(
                        "int offset, i;",
                        "char *byte, *res;",
                        "",
                        "offset = ((int)target) % 4;",
                        "if (offset != 0) {",
                        "    byte = (void*)data;",
                        "    res = (void*)target;",
                        "    for (i=0; i<4; i++) *(res++) = ((*(byte++)) & 0xFF);",
                        "} else *target = *data;"
                        )));
        
        // read a 32bit value from an unaligned address
        MMethod getUnaligned = MMethod(MModifiers(INLINE()), MType("int"), "get_unaligned", MParameters(
                MParameter(VALUE(), MPointerType(MType("int")), "data")
                ), MCode(Strings( 
                        "unsigned int offset, res, tmp;",
                        "int i;",
                        "char *byte;",
                        "",
                        "offset = ((int)data) % 4;",
                        "if (offset != 0) {",
                        "    byte = (void*)data;",
                        "    res = 0;",
                        "    for (i=0; i<4; i++) {",
                        "        // make sure only rightmost 8bit are processed",
                        "        tmp = (*(byte++)) & 0xFF;",
                        "        // shift the value to the correct position",
                        "        tmp <<= (i*8);",
                        "        // sum up the 32bit value",
                        "        res += tmp;",
                        "    }",
                        "    return res;",
                        "}",
                        "return *data;"
                        )));
        
        return append(append(file, setUnaligned), getUnaligned);
    }

    private MFile addGPIOChannel() {
        return append(file, MDefinition("GPIO_CHANNEL1", "1"));
    }
    
    private MFile addBasicInterruptSetup() {
        
        // append interrupt setup to init method
        init = appendCode(init, MCode(Strings("", "if (intrSetup() != XST_SUCCESS) return XST_FAILURE;")));
        
        // add interrupt attribute
        file = append(file, MAttribute(MModifiers(), MType("XIntc"), "intc", MCodeFragment("",
                MInclude("xintc.h", QUOTES()))));
        
        // add method for interrupt setup
        Strings intrSetupCode = Strings();
        if(debug) intrSetupCode = intrSetupCode.addAll(Strings(
                "xil_printf(\"\\n  initialising interrupt controller driver ...\");", ""));
        intrSetupCode = intrSetupCode.addAll(Strings(
                "// initialize interrupt controller driver so that it is ready to use",
                "int Status = XIntc_Initialize(&intc, XPAR_INTC_0_DEVICE_ID);",
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
//        intrSetupCode = intrSetupCode.addAll(Strings("", "return XST_SUCCESS;"));
        
        intr = appendCode(intr, MCode(intrSetupCode,
                MInclude("xil_exception.h", QUOTES()),
                MInclude("xintc.h",         QUOTES())));
        
//        file = append(file, MMethod(MModifiers(), MType("int"), "IntcInterruptSetup", 
//                MParameters(), MCode(intrSetupCode)));
        
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
                ), MCode(intrSetupCode,
                        MInclude("xgpio.h",         QUOTES()),
                        MInclude("xil_exception.h", QUOTES()),
                        MInclude("xintc.h",         QUOTES())));
        
        includedInterruptSetup = true;
        
        return append(file, gpioIntrSetup);
    }
    
    private MFile addEthernetInterruptSetup() {
        MFile file = this.file;
        
        if(!includedInterruptSetup) {
            file = addBasicInterruptSetup();
        }
        
        intr = appendCode(intr, MCode(Strings(
                "",
                "// THE FOLLOWING PARTS ARE REQUIRED FOR ETHERNET COMMUNICATION",
                "/* Start the interrupt controller */",
                "XIntc_MasterEnable(XPAR_INTC_0_BASEADDR);",
                "",
                "platform_setup_timer();",
                "",
                "// TODO I have no real idea what is happening here",
                "#ifdef XPAR_ETHERNET_MAC_IP2INTC_IRPT_MASK",
                "/* Enable timer and EMAC interrupts in the interrupt controller */",
                "XIntc_EnableIntr(XPAR_INTC_0_BASEADDR,",
                "#ifdef __MICROBLAZE__",
                "PLATFORM_TIMER_INTERRUPT_MASK |",
                "#endif",
                "XPAR_ETHERNET_MAC_IP2INTC_IRPT_MASK);",
                "#endif",
                "",
                "",
                "#ifdef XPAR_INTC_0_LLTEMAC_0_VEC_ID",
                "#ifdef __MICROBLAZE__",
                "XIntc_Enable(&intc, PLATFORM_TIMER_INTERRUPT_INTR);",
                "#endif",
                "XIntc_Enable(&intc, XPAR_INTC_0_LLTEMAC_0_VEC_ID);",
                "#endif",
                "",
                "",
                "#ifdef XPAR_INTC_0_AXIETHERNET_0_VEC_ID",
                "XIntc_Enable(&intc, PLATFORM_TIMER_INTERRUPT_INTR);",
                "XIntc_Enable(&intc, XPAR_INTC_0_AXIETHERNET_0_VEC_ID);",
                "#endif",
                "",
                "",
                "#ifdef XPAR_INTC_0_EMACLITE_0_VEC_ID",
                "#ifdef __MICROBLAZE__",
                "XIntc_Enable(&intc, PLATFORM_TIMER_INTERRUPT_INTR);",
                "#endif",
                "XIntc_Enable(&intc, XPAR_INTC_0_EMACLITE_0_VEC_ID);",
                "#endif",
                "",
                "xil_printf(\" done\");"
                ), MInclude("platform.h",        QUOTES()),
                   MInclude("platform_config.h", QUOTES()),
                   MInclude("xintc.h",           QUOTES())));
        
        return file;
    }
    
    private static String unparseIP (IP ip) {
        return (unparseIP(ip.child0()) + ", " + unparseIP(ip.child1()) + ", " +
                unparseIP(ip.child2()) + ", " + unparseIP(ip.child3()));
    }
    private static String unparseIP(int val) {
        // exclude invalid ip addresses 
        if(val > 255 || val < 0) throw new IllegalStateException();

        String rslt = new String();
        
        // add spaces for better formatting
        if(val < 100) {
            rslt += " ";
            if(val < 10) rslt += " ";
        }
        // append value
        return rslt + val;
    }
}