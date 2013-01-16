package de.hopp.generator;

import static de.hopp.generator.model.Model.MAttributes;
import static de.hopp.generator.model.Model.MClasses;
import static de.hopp.generator.model.Model.MCode;
import static de.hopp.generator.model.Model.MDefinitions;
import static de.hopp.generator.model.Model.MDocumentation;
import static de.hopp.generator.model.Model.MEnums;
import static de.hopp.generator.model.Model.MFile;
import static de.hopp.generator.model.Model.MInclude;
import static de.hopp.generator.model.Model.MMethod;
import static de.hopp.generator.model.Model.MMethods;
import static de.hopp.generator.model.Model.MModifiers;
import static de.hopp.generator.model.Model.MParameters;
import static de.hopp.generator.model.Model.MStructs;
import static de.hopp.generator.model.Model.MType;
import static de.hopp.generator.model.Model.QUOTES;
import static de.hopp.generator.model.Model.Strings;
import katja.common.NE;
import de.hopp.generator.board.*;
import de.hopp.generator.board.Board.Visitor;
import de.hopp.generator.model.MFile;
import de.hopp.generator.model.MMethod;

public class ClientVisitor extends Visitor<NE> {

    private Configuration config;
    
    private MFile file;
    
    private MMethod init;
//    private MMethod clean;
    private MMethod main;
    
    public ClientVisitor(Configuration config) {
        this.config = config;
        
        // setup basic methods
        file  = MFile("name", MDefinitions(), MStructs(), MEnums(), MAttributes(), MMethods(), MClasses());
        
        init  = MMethod(MDocumentation(Strings()), MModifiers(), MType("int"), "init", 
                MParameters(), MCode(Strings("")));
//        clean = MMethod(MDocumentation(Strings()), MModifiers(), MType("int"), "cleanup", 
//                MParameters(), MCode(Strings(""), MInclude("platform.h", QUOTES())));
        main  = MMethod(MDocumentation(Strings()), MModifiers(), MType("int"), "main", 
                MParameters(), MCode(Strings("", "// initialize board components", "init();")));
    }
    
    public MFile getFile() {
        return file;
    }
    
    public void visit(Board term) {
        // TODO Auto-generated method stub
    }
    public void visit(Components term) {
        // TODO Auto-generated method stub
    }
    public void visit(UART term) {
        // TODO Auto-generated method stub
    }
    public void visit(ETHERNET_LITE term) {
        
//        int setup(int *Data_SocketFD) {
//            struct sockaddr_in stSockAddr;
//            int Res;
//            char *ip = "131.246.92.144";
//        //  char *ip = "192.168.1.10";
//
//            if(DEBUG) printf("setting up data socket @%s:%d ...", ip, NW_DATA_PORT);
//
//            if (-1 == *Data_SocketFD){ //|| -1 == Config_SocketFD){
//                printf(" failed to create socket");
//                exit(EXIT_FAILURE);
//            }
//
//            // Initialize Socket memory
//            memset(&stSockAddr, 0, sizeof(stSockAddr));
//
//            // Connect the Input Socket
//            stSockAddr.sin_family = AF_INET;
//            stSockAddr.sin_port = htons(NW_DATA_PORT);
//            Res = inet_pton(AF_INET, ip, &stSockAddr.sin_addr);
//
//            if (0 > Res){
//                printf(" error: first parameter is not a valid address family");
//                close(*Data_SocketFD);
//                exit(EXIT_FAILURE);
//            }
//            else if (0 == Res){
//                printf(" char string (second parameter does not contain valid ip address)");
//                close(*Data_SocketFD);
//                exit(EXIT_FAILURE);
//            }
//
//            if (-1 == connect(*Data_SocketFD, (struct sockaddr *)&stSockAddr, sizeof(stSockAddr))){
//                printf(" connect failed: %s (%d)", strerror(errno), errno);
////              printf("errorcode: %d", errno);
//                close(*Data_SocketFD);
//                exit(EXIT_FAILURE);
//            }
//
//            printf(" done\n");
//
//            return 0;
//        }
    }
    public void visit(ETHERNET term) {
        // TODO Auto-generated method stub
    }
    public void visit(PCIE term) {
        // TODO Auto-generated method stub
    }
    public void visit(LEDS term) {
        // TODO Auto-generated method stub
    }
    public void visit(SWITCHES term) {
        // TODO Auto-generated method stub
    }
    public void visit(BUTTONS term) {
        // TODO Auto-generated method stub
    }
    public void visit(Integer term) { }

}
