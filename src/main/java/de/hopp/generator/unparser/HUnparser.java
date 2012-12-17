package de.hopp.generator.unparser;

import static de.hopp.generator.model.Model.*;

import java.util.Comparator;

import katja.common.NE;
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
    protected String typeDecl;
    protected String name;
    
    public HUnparser(StringBuffer buffer, String name) {
        this.buffer = new IndentStringBuffer(buffer);
        this.typeDecl = new String();
        this.name = name;
    }
    
    protected MStructsInFile filter(MStructsInFile structs, MModifier modifier) {
        MStructsInFile rslt = structs;
        for(MStructInFile struct : structs)
            if(!struct.modifiers().term().contains(modifier))
                rslt = rslt.remove(struct.term());
        
        return rslt;
    }
    protected MEnumsInFile filter(MEnumsInFile menums, MModifier modifier) {
        MEnumsInFile rslt = menums;
        for(MEnumInFile menum : menums)
            if(!menum.modifiers().term().contains(modifier))
                rslt = rslt.remove(menum.term());
        
        return rslt;
    }
    protected MAttributesInFile filter(MAttributesInFile attributes, MModifier modifier) {
        MAttributesInFile rslt = attributes;
        for(MAttributeInFile attribute : attributes)
            if(!attribute.modifiers().term().contains(modifier))
                rslt = rslt.remove(attribute.term());
        
        return rslt;
    }
    protected MMethodsInFile filter(MMethodsInFile methods, MModifier modifier) {
        MMethodsInFile rslt = methods;
        for(MMethodInFile method : methods)
            if(!method.modifiers().term().contains(modifier))
                rslt = rslt.remove(method.term());
        
        return rslt;
    }
    protected MClassesInFile filter(MClassesInFile mclasses, MModifier modifier) {
        MClassesInFile rslt = mclasses;
        for(MClassInFile mclass : mclasses)
            if(!mclass.modifiers().term().contains(modifier))
                rslt = rslt.remove(mclass.term());
        
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
        return method.name().term();
    }
    
    protected String qualifiedName(MClassInFile mclass) {
        return mclass.name().term();
    }
    
    @Override
    public void visit(MFileInFile file) throws InvalidConstruct {

        // create the list of all needed import names needed for types used in this file
        MIncludes importLines = MIncludes();
        
        // go through the file and gather all imports / includes required
        for(Model.SortInFile pos = file; pos != null; pos = pos.preOrder()) {

            // only add if current position is a MType and we do not already have this type
            if(pos instanceof MIncludeInFile) {

                // get the type
                MInclude include = (MInclude) pos.term();

                // do not import if is already imported
                if(!importLines.contains(include)) importLines = importLines.add(include);
            }
        }
        
        if(!importLines.isEmpty()) {
            
            // TODO is this possible / necessary in C? or only ++?
            buffer.append("#ifndef " + name.toUpperCase() + "_H_\n"
                        + "#define " + name.toUpperCase() + "_H_\n");
            
            MInclude[] includes = importLines.toArray();
            java.util.Arrays.sort(includes, new Comparator<MInclude>() {
                // basically, duplicate includes should not appear, so equality should not occur here
                public int compare(MInclude o1, MInclude o2) {
                    // compare the names, if they are unequal return the result
                    int i = o1.name().compareTo(o2.name());
                    if(i != 0) return i;
                    // in case of equal names, compare the include type
                    if(o1.type().equals(o2.type()))  return 0;
                    // if they are not equal, print <> includes first
                    if(o1.type().equals(BRACKETS())) return -1;
                    return 1;
                }
            });
            
            for(final MInclude include : includes)
                buffer.append(include.type().Switch(new MIncludeType.Switch<String, NE>() {
                    public String CaseBRACKETS(BRACKETS term) {
                        return "\n#include <"  + include.name() + ">";
                    }
                    public String CaseQUOTES(QUOTES arg0) {
                        return "\n#include \"" + include.name() + "\"";
                    }
                }));
            
            buffer.append('\n');
        }
        
        // unparse components
        visit(file.structs());
        visit(file.enums());
        visit(file.attributes());
        visit(file.methods());
        visit(file.classes());
        
        if(!importLines.isEmpty()) {
            buffer.append("\n#endif /* " + name.toUpperCase() + "_H_ */");
        }
    }

    @Override
    public void visit(MDefinitionsInFile defs) throws InvalidConstruct { }

    // TODO make public/private distinction here to provide better comments?
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
            buffer.append("\n// structs of " + name + "");
            
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
                buffer.append('\n');
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
            buffer.append("\n// attributes of " + name);
            
            // unparse contained attributes
            for(MAttributeInFile attribute : attributes) {
//                buffer.append('\n');
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
                buffer.append("\n// procedures of struct " + ((MStructInFile)methods.parent()).name().term());
            } else if(methods.parent() instanceof MClassInFile) {
                buffer.append("\n// methods of class " + ((MClassInFile)methods.parent()).name().term());
            }
            
            // unparse contained methods
            for(MMethodInFile method : methods) {
//                buffer.append('\n');
                visit(method);
            }
            buffer.append('\n');
        }  
    }

    @Override
    public void visit(MClassesInFile classes) throws InvalidConstruct {
        
        // if no class at all return
        if(classes.isEmpty()) return;
        
        // set local name for the comment to file name
        String name = this.name;
            
        // if the parent is a struct or class, use this name instead
        if(classes.parent() instanceof MStructInFile)
            name = ((MStructInFile)classes.parent()).name().term();
        if(classes.parent() instanceof MClassInFile)
            name = qualifiedName((MClassInFile)classes.parent());
        
        // append comment
        buffer.append("\n// classes of " + name);
        
        // go through the classes using rsib method
        for(MClassInFile mclass = classes.first(); mclass != null; mclass = mclass.rsib()) {

            // unparse the class
            visit(mclass);
            
            // append newline if it isn't the last class
            if(mclass.rsib() != null) buffer.append('\n');
        }
        
//        // unparse contained classes
//        for(MClassInFile mclass : classes) {
//            buffer.append("\n\n");
//            visit(mclass);
//        }
        buffer.append('\n');
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
    public void visit(MDefinitionInFile term) throws InvalidConstruct {
        buffer.append("\n#define " + term.name().term() + " " + term.value().term());
    }
    
    @Override
    public void visit(MStructInFile struct) throws InvalidConstruct {
        
        // do not put "private" global structs in the header
        if(struct.parent().parent() instanceof MFileInFile 
                && struct.modifiers().term().contains(PRIVATE())) return;
        
        buffer.append("\nstruct " + struct.name().term() + " {\n");
        buffer.indent();
        
        // unparse contained attributes
        visit(struct.attributes());
        
        buffer.unindent();
        buffer.append("}");
    }

    @Override
    public void visit(MEnumInFile mEnum) throws InvalidConstruct {
        
        // do not put "private" global enums in the header
        if(mEnum.parent().parent() instanceof MFileInFile 
                && mEnum.modifiers().term().contains(PRIVATE())) return;
        
        buffer.append("enum " + mEnum.name().term() + " { ");
        if(mEnum.size() > 0) {
            for(StringInFile value : mEnum.values()) buffer.append(value.term());
            buffer.append(", ");
        }
        buffer.append(" };\n");
    }

    @Override
    public void visit(MAttributeInFile attribute) throws InvalidConstruct {

        // globals are treated differently
        if(attribute.parent().parent() instanceof MFileInFile) {
            
            // "private" globals should not appear int the header at all
            if(attribute.modifiers().term().contains(PRIVATE())) return;
            
            // "public" globals should always be declared "extern"
            buffer.append("\nextern ");
            
            // they maybe constant as well
            if(attribute.modifiers().term().contains(CONSTANT())) buffer.append("const ");
            
            // set the type declaration to the attribute name and unparse the type
            typeDecl = attribute.name().term();
            visit(attribute.type());
        } else {
            buffer.append("\n");
            // for attributes of classes, static has a different semantic and should not be set automatically
            if(attribute.modifiers().term().contains(CONSTANT())) buffer.append("const ");
            if(attribute.modifiers().term().contains(STATIC()))   buffer.append("static ");
            
            // set the type declaration to the attribute name and unparse the type
            typeDecl = attribute.name().term();
            visit(attribute.type());
            
            // for class attributes, the initial value is set in the header 
            // TODO wait... WHAT??? WHY??? This should (at best) make sense for static, but not always!
            visit(attribute.initial());
        }
        
        // append colon and newline
        buffer.append(";");
    }

    @Override
    public void visit(MMethodInFile method) throws InvalidConstruct {
        
        // do not put "private" global methods in the header
        if(method.parent().parent() instanceof MFileInFile 
                && method.modifiers().term().contains(PRIVATE())) return;
        
        buffer.append('\n');
        if(method.modifiers().term().contains(CONSTANT())) buffer.append("inline ");
        if(method.modifiers().term().contains(CONSTANT())) buffer.append("const ");
        if(method.modifiers().term().contains(STATIC()))   buffer.append("static ");
        visit(method.returnType());
        buffer.append(qualifiedName(method));
        visit(method.parameter());
        visit(method.body());
    }

    @Override
    public void visit(MClassInFile mclass) throws InvalidConstruct {
        
        // do not put "private" global classes in the header
        if(mclass.parent().parent() instanceof MFileInFile 
                && mclass.modifiers().term().contains(PRIVATE())) return;
        
        buffer.append("\nclass " + mclass.name().term() + " {");
        
        // TODO more efficient solution?
        //      This effectively runs three times over all components
        //      Could probably make this more efficient with usage of local buffers
        //      (yet this would mean circumventing list visitors)
        
        // print all public members of the class
        if(contains(mclass, PUBLIC())) {
            buffer.append("\npublic:");
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
            buffer.append("\nprivate:");
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
        buffer.append(type.name().term() + " " + typeDecl);
        typeDecl = new String();
    }

    @Override
    public void visit(MArrayTypeInFile arrayType) throws InvalidConstruct {
        typeDecl = "(" + typeDecl + ") [" + arrayType.length().term() + "]";
        visit(arrayType.type());
    }

    @Override
    public void visit(MPointerTypeInFile pointerType) throws InvalidConstruct {
        typeDecl = "*" + typeDecl;
        visit(pointerType.type());
    }

    @Override
    public void visit(MConstPointerTypeInFile constPointerType) throws InvalidConstruct {
        typeDecl = "*const" + typeDecl;
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
        typeDecl = parameter.name().term();
        if(parameter.refType().term() instanceof CONSTREF) buffer.append("const ");
        if(parameter.refType().term() instanceof CONSTREF || 
           parameter.refType().term() instanceof REFERENCE) typeDecl = "&" + typeDecl ;
        visit(parameter.type());
    }
    
    // should never go here
    public void visit(MModifiersInFile term) { }
    public void visit(PRIVATEInFile term)    { }
    public void visit(PUBLICInFile term)     { }
    public void visit(STATICInFile term)     { }
    public void visit(INLINEInFile term)     { }
    public void visit(CONSTANTInFile term)   { }
    public void visit(VALUEInFile term)      { }
    public void visit(REFERENCEInFile term)  { }
    public void visit(CONSTREFInFile term)   { }
    public void visit(StringsInFile term)    { }
    public void visit(StringInFile term)     { }
    public void visit(IntegerInFile term)    { }
    public void visit(MIncludesInFile term)  { }
    public void visit(MIncludeInFile term)   { }
    public void visit(QUOTESInFile term)     { }
    public void visit(BRACKETSInFile term)   { }

}
