package de.hopp.generator.unparser;

import static de.hopp.generator.model.Model.*;
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
// TODO generally nesting of stuff in C? stucts? methods?
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
    
    protected MStructsInFile filter(MStructsInFile structs, MModifier modifier) {
        MStructsInFile rslt = structs;
        for(MStructInFile struct : structs)
            if(!struct.modifiers().term().contains(modifier))
                rslt.remove(struct.term());
        
        return rslt;
    }
    protected MEnumsInFile filter(MEnumsInFile menums, MModifier modifier) {
        MEnumsInFile rslt = menums;
        for(MEnumInFile menum : menums)
            if(!menum.modifiers().term().contains(modifier))
                rslt.remove(menum.term());
        
        return rslt;
    }
    protected MAttributesInFile filter(MAttributesInFile attributes, MModifier modifier) {
        MAttributesInFile rslt = attributes;
        for(MAttributeInFile attribute : attributes)
            if(!attribute.modifiers().term().contains(modifier))
                rslt.remove(attribute.term());
        
        return rslt;
    }
    protected MMethodsInFile filter(MMethodsInFile methods, MModifier modifier) {
        MMethodsInFile rslt = methods;
        for(MMethodInFile method : methods)
            if(!method.modifiers().term().contains(modifier))
                rslt.remove(method.term());
        
        return rslt;
    }
    protected MClassesInFile filter(MClassesInFile mclasses, MModifier modifier) {
        MClassesInFile rslt = mclasses;
        for(MClassInFile mclass : mclasses)
            if(!mclass.modifiers().term().contains(modifier))
                rslt.remove(mclass.term());
        
        return rslt;
    }
    protected boolean contains(MClassInFile mclass, MModifier mod) {
        for(MStructInFile struct : mclass.structs())
            if(struct.modifiers().term().contains(mod)) return true;
        for(MEnumInFile menum : mclass.enums())
            if(menum.modifiers().term().contains(mod)) return true;
        for(MAttributeInFile attribute : mclass.attributes())
            if(attribute.modifiers().term().contains(mod)) return true;
        for(MMethodInFile method : mclass.methods())
            if(method.modifiers().term().contains(mod)) return true;
        for(MClassInFile nestedClass : mclass.nested())
            if(nestedClass.modifiers().term().contains(mod)) return true;
        return false;
    }
    
    protected String qualifiedName(MMethodInFile method) {
        if(method.parent().parent() instanceof MFileInFile)
            return method.name().term();
        else if(method.parent().parent() instanceof MClassInFile)
            return qualifiedName((MClassInFile)method.parent().parent()) + "::" + method.name().term();
        throw new RuntimeException();
    }
    
    protected String qualifiedName(MClassInFile mclass) {
        if(mclass.parent().parent() instanceof MFileInFile)
            return mclass.name().term();
        else if(mclass.parent().parent() instanceof MClassInFile)
            return qualifiedName((MClassInFile)mclass.parent().parent()) + "::" + mclass.name().term();
        throw new RuntimeException();
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
        
        if(!importLines.isEmpty()) {
            // TODO is this possible / necessary in C? or only ++?
            buffer.append("#ifndef " + name.toUpperCase() + "_H_\n"
                        + "#define " + name.toUpperCase() + "_H_\n");
            
            // TODO i have no idea how this can be handled,
            //      esp. to work with c as well as c++
            //      are there even imports in plain c??
            //      only of complete other files i guess??
            for(String importName : importLines)
                buffer.append("\n#include " + importName);
            
            buffer.append('\n');
        }
        
        // unparse components
        visit(file.structs());
        visit(file.enums());
        visit(file.attributes());
        visit(file.methods());
        visit(file.classes());
        
        if(!importLines.isEmpty()) {
            buffer.append("#endif /* " + name.toUpperCase() + "_H_ */");
        }
    }

    @Override
    public void visit(MStructsInFile structs) throws InvalidConstruct {
        if(structs.size() > 0) {
            // set local name for the comment to file name
            String name = this.name;
            
            // if the parent is a struct or class, use this name instead
            if(structs.parent() instanceof MStructInFile)
                name = ((MStructInFile)structs.parent()).name().term();
            if(structs.parent() instanceof MClassInFile)
                name = ((MClassInFile)structs.parent()).name().term();
            
            // append comment
            buffer.append("\n// structs of " + name + "\n");
            
            // unparse the contained structs
            for(MStructInFile struct : structs) visit(struct);
            buffer.append('\n');
        }
    }

    @Override
    public void visit(MEnumsInFile enums) throws InvalidConstruct {
        if(enums.size() > 0) {
            // set local name for the comment to file name
            String name = this.name;
            
            // if the parent is a struct or class, use this name instead
            if(enums.parent() instanceof MStructInFile)
                name = ((MStructInFile)enums.parent()).name().term();
            if(enums.parent() instanceof MClassInFile)
                name = ((MClassInFile)enums.parent()).name().term();
            
            // append comment
            buffer.append("\n// enums of " + name);
            
            // unparse contained enums
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
            // set local name for the comment to file name
            String name = this.name;
            
            // if the parent is a struct or class, use this name instead
            if(attributes.parent() instanceof MStructInFile)
                name = ((MStructInFile)attributes.parent()).name().term();
            if(attributes.parent() instanceof MClassInFile)
                name = ((MClassInFile)attributes.parent()).name().term();
            
            // append comment
            buffer.append("\n// fields of " + name);
            
            // unparse contained attributes
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
            
            // append fitting comment, depending on parent
            if(methods.parent() instanceof MFileInFile) {
                buffer.append("\n// procedures of " + this.name);
            } else if(methods.parent() instanceof MStructInFile) {
                buffer.append("\n// procedures of " + ((MStructInFile)methods.parent()).name().term());
            } else if(methods.parent() instanceof MClassInFile) {
                buffer.append("\n// methods of " + ((MClassInFile)methods.parent()).name().term());
            }
            
            // unparse contained methods
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
            // set local name for the comment to file name
            String name = this.name;
            
            // if the parent is a struct or class, use this name instead
            if(classes.parent() instanceof MStructInFile)
                name = ((MStructInFile)classes.parent()).name().term();
            if(classes.parent() instanceof MClassInFile)
                name = qualifiedName((MClassInFile)classes.parent());
            
            // append comment
            buffer.append("\n// classes of " + name);
            
            // unparse contained classes
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
        if(attribute.modifiers().term().contains(CONSTANT())) buffer.append(" const");
        typeIdent = attribute.name().term();
        visit(attribute.type());
        visit(attribute.initial());
        buffer.append(";\n");
    }

    @Override
    public void visit(MMethodInFile method) throws InvalidConstruct {
        if(method.modifiers().term().contains(CONSTANT())) buffer.append("const ");
        visit(method.returnType());
        buffer.append(qualifiedName(method));
        visit(method.parameter());
        visit(method.body());
    }

    @Override
    public void visit(MClassInFile mclass) throws InvalidConstruct {
        buffer.append("class " + mclass.name().term() + " {\n");
        
        // print all public members of the class
        if(contains(mclass, PUBLIC())) {
            buffer.append("\npublic:\n");
            buffer.indent();

            visit(filter(mclass.structs(),    PUBLIC()));
            visit(filter(mclass.enums(),      PUBLIC()));
            visit(filter(mclass.attributes(), PUBLIC()));
            visit(filter(mclass.methods(),    PUBLIC()));
            visit(filter(mclass.nested(),     PUBLIC()));
            
            buffer.unindent();
        }

        // print all private members of the class
        if(contains(mclass, PRIVATE())) {
            buffer.append("\nprivate:\n");
            buffer.indent();
            
            visit(filter(mclass.structs(),    PRIVATE()));
            visit(filter(mclass.enums(),      PRIVATE()));
            visit(filter(mclass.attributes(), PRIVATE()));
            visit(filter(mclass.methods(),    PRIVATE()));
            visit(filter(mclass.nested(),     PRIVATE()));
            
            buffer.unindent();
        }
        
        buffer.append("};");
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
    public void visit(MModifiersInFile mods) { }
    public void visit(PRIVATEInFile term)    { }
    public void visit(PUBLICInFile term)     { }
    public void visit(CONSTANTInFile term)   { }
    public void visit(VALUEInFile term)      { }
    public void visit(REFERENCEInFile term)  { }
    public void visit(CONSTREFInFile term)   { }
    public void visit(StringsInFile term)    { }
    public void visit(StringInFile term)     { }
    public void visit(IntegerInFile term)    { }
    public void visit(MIncludesInFile term)  { }
    public void visit(MIncludeInFile term)   { }
    public void visit(QUOTESInFile arg0)     { }
    public void visit(BRACKETSInFile term)   { }
}
