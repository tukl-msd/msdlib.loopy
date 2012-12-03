package de.hopp.generator.unparser;

import static de.hopp.generator.model.Model.CONSTANT;
import static de.hopp.generator.model.Model.Strings;
import de.hopp.generator.exceptions.InvalidConstruct;
import de.hopp.generator.model.*;

/**
 * Unparser for header files. Can be used for C headers as well
 * as for C++ headers, consequently does not filter for constructs
 * invalid in plain C.
 * 
 * @author Thomas Fischer
 * 
 */

// TODO nesting of stuff in header file??
// TODO generally nesting of stuff in C? stucts? classes? methods?
// TODO attributes / enums in header file?? what to put there, what not to put there?
public class HUnparser extends MFileInFile.Visitor<InvalidConstruct> {

    // the buffer to fill with this unparsing
    protected IndentStringBuffer buffer;
    protected String typeIdent;
    protected String name;
    
    public HUnparser(StringBuffer buffer, String name) {
        this.buffer = new IndentStringBuffer(buffer);
        this.typeIdent = new String();
        this.name = name;
    }
    
    @Override
    public void visit(MFileInFile file) throws InvalidConstruct {

        // create the list of all needed import names needed for types used in this file
        Strings importLines = Strings();
        
        // go through the file and gather all imports / includes required
        for(Model.SortInFile pos = file; pos != null; pos = pos.preOrder()) {

            // only add if current position is a MType and we do not already have this type
            if(pos instanceof MTypeInFile) {

                // get the type
                MType type = (MType) pos.term();

                for(String imp : ((MType) type).importNames()) {

                    // do not import if is already imported
                    if(!importLines.contains(imp)) importLines = importLines.add(imp);
                }
            }
        }
        
        // TODO output the import statements, if any
        if(!importLines.isEmpty()) {
            // TODO is this possible / necessary in C? or only ++?
            buffer.append("#ifndef PROG1_H_\n"
                        + "#define PROG1_H_\n");
            
            // TODO i have no idea how this can be handled,
            //      esp. to work with c as well as c++
            //      are there even imports in plain c??
            //      only of complete other files i guess??
            for(String importName : importLines) {
                buffer.append("\n#include " + importName);
            }
            
            buffer.append('\n');
        }
        
        // unparse components
        visit(file.structs());
        visit(file.enums());
        visit(file.attributes());
        visit(file.methods());
        visit(file.classes());
    }

    @Override
    public void visit(MStructsInFile structs) throws InvalidConstruct {
        if(structs.size() > 0) {
            buffer.append("\n// structs of " + name + "\n");
            for(MStructInFile struct : structs) visit(struct);
            buffer.append('\n');
        }
    }

    @Override
    public void visit(MEnumsInFile enums) throws InvalidConstruct {
        if(enums.size() > 0) {
            buffer.append("\n// enums of " + name);
            for(MEnumInFile mEnum : enums) {
                buffer.append("\n\n");
                visit(mEnum);
            }
            buffer.append('\n');
        }
    }

    @Override
    public void visit(MAttributesInFile attributes) throws InvalidConstruct {
        if(attributes.size() > 0) {
            buffer.append("\n// fields of " + name);
            for(MAttributeInFile attribute : attributes) {
                buffer.append("\n\n");
                visit(attribute);
            }
            buffer.append('\n');
        }
    }

    @Override
    public void visit(MMethodsInFile methods) throws InvalidConstruct {
        if(methods.size() > 0) {
            buffer.append("\n// procedures of " + name);
            for(MMethodInFile attribute : methods) {
                buffer.append("\n\n");
                visit(attribute);
            }
            buffer.append('\n');
        }  
    }

    @Override
    public void visit(MClassesInFile classes) throws InvalidConstruct {
        if(classes.size() > 0) {
            buffer.append("\n// classes of " + name);
            for(MClassInFile mclass : classes) {
                buffer.append("\n\n");
                visit(mclass);
            }
            buffer.append('\n');
        }
    }
    
    // TODO this is how it is done by paddy... maybe apply this as well?
//    public void visit(MClassesInFile classes) {
//
//        // if no class at all return
//        if(classes.isEmpty()) return;
//
//        // we go through the classes using the rsib method
//        for(MClassInFile mclass = classes.first(); mclass != null; mclass = mclass.rsib()) {
//
//            // generate the class
//            visit(mclass);
//
//            // if it wasn't the last class we need a seperating line
//            if(mclass.rsib() != null) buffer.append("\n");
//        }
//    }

    @Override
    public void visit(MStructInFile struct) throws InvalidConstruct {
        buffer.append("struct " + struct.name().term() + " {\n");
        buffer.indent();
        
        // unparse components
        visit(struct.structs());
        visit(struct.enums());
        visit(struct.attributes());
        visit(struct.methods());
        
        buffer.unindent();
        buffer.append("}\n");
    }

    @Override
    public void visit(MEnumInFile mEnum) throws InvalidConstruct {
        buffer.append("enum " + mEnum.name().term() + " { ");
        if(mEnum.size() > 0) {
            for(StringInFile value : mEnum.values()) buffer.append(value.term());
            buffer.append(", ");
        }
        buffer.append(" };\n");
    }

    @Override
    public void visit(MAttributeInFile attribute) throws InvalidConstruct {
        buffer.append("\n\n");
        if(attribute.modifiers().term().contains(CONSTANT())) buffer.append(" const");
        typeIdent = attribute.name().term();
        visit(attribute.type());
        visit(attribute.initial());
        buffer.append(";\n");
    }

    @Override
    public void visit(MMethodInFile method) throws InvalidConstruct {
        visit(method.returnType());
        buffer.append(method.name().term());
        visit(method.parameter());
        visit(method.body());
    }

    @Override
    public void visit(MClassInFile mclass) throws InvalidConstruct {
        // should never go here
        throw new InvalidConstruct("encountered class in C unparser");
    }

    @Override
    public void visit(MModifiersInFile modifiers) throws InvalidConstruct {
        // should never go here
    }

    @Override
    public void visit(MCodeFragmentInFile codefragment) throws InvalidConstruct {
        if(! codefragment.part().term().equals(""))
            buffer.append(" = " + codefragment.part().term());
    }

    @Override
    public void visit(MParametersInFile parameters) throws InvalidConstruct {
        buffer.append(" (");
        for(MParameterInFile parameter : parameters) {
            visit(parameter);
            if(! parameter.equals(parameters.last())) buffer.append(',');
        }
        buffer.append(" )");
    }

    @Override
    public void visit(MCodeInFile code) throws InvalidConstruct {
        // no body in the header file
        buffer.append(';');
    }

    @Override
    public void visit(MTypeInFile type) throws InvalidConstruct {
        buffer.append(type.name().term() + " " + typeIdent);
        typeIdent = new String();
    }

    @Override
    public void visit(MArrayTypeInFile arrayType) throws InvalidConstruct {
        typeIdent = "(" + typeIdent + ") [" + arrayType.length().term() + "]";
        visit(arrayType.type());
    }

    @Override
    public void visit(MPointerTypeInFile pointerType) throws InvalidConstruct {
        typeIdent = "*" + typeIdent;
        visit(pointerType.type());
    }

    @Override
    public void visit(MConstPointerTypeInFile constPointerType) throws InvalidConstruct {
        typeIdent = "*const" + typeIdent;
        visit(constPointerType.type());
    }

    @Override
    public void visit(MNoneInFile none) throws InvalidConstruct { 
        // do nothing - constructor method
    }

    @Override
    public void visit(MVoidInFile mvoid) throws InvalidConstruct {
        buffer.append("void ");
    }

    @Override
    public void visit(MTypesInFile types) throws InvalidConstruct {
        for(MTypeInFile type : types) {
            visit(type);
            if(! type.equals(types.last())) {
                buffer.append(',');
            }
        }
    }

    @Override
    public void visit(MParameterInFile parameter) throws InvalidConstruct {
        buffer.append(' ');
        typeIdent = parameter.name().term();
        if(parameter.refType().term() instanceof CONSTREF) buffer.append("const ");
        if(parameter.refType().term() instanceof CONSTREF || 
           parameter.refType().term() instanceof REFERENCE) typeIdent = "&" + typeIdent ;
        visit(parameter.type());
    }
    
    // should never go here
    public void visit(PRIVATEInFile term)   { }
    public void visit(PUBLICInFile term)    { }
    public void visit(CONSTANTInFile term)  { }
    public void visit(VALUEInFile term)     { }
    public void visit(REFERENCEInFile term) { }
    public void visit(CONSTREFInFile term)  { }
    public void visit(StringsInFile term)   { }
    public void visit(StringInFile term)    { }
    public void visit(IntegerInFile term)   { }
    public void visit(MIncludesInFile term) { }
    public void visit(MIncludeInFile term)  { }
    public void visit(QUOTESInFile arg0)    { }
    public void visit(BRACKETSInFile term)  { }
}
