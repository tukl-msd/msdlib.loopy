package de.hopp.generator.backends.unparser;

import static de.hopp.generator.model.Model.*;

import de.hopp.generator.exceptions.InvalidConstruct;
import de.hopp.generator.model.*;

import katja.common.NE;

/**
 * C++ unparser. Generates C++ code out of the given model.
 * Everything specifiable using the model will be unparsed.
 * @author Thomas Fischer
 */
public class CppUnparser extends CUnparser {

    /**
     * Create a MFile unparser
     * @param buffer the buffer to unparse into
     */
    public CppUnparser(StringBuffer buffer, String name) {
        super(buffer, name);
    }
    
    
    protected String qualifiedName(MMethodInFile method) {
        return method.Switch(new MMethodInFile.Switch<String, NE>() {
            public String CaseMProcedureInFile(MProcedureInFile proc) {
                if(proc.parent().parent() instanceof MFileInFile)
                    return proc.name().term();
                else if(proc.parent().parent() instanceof MClassInFile)
                    return qualifiedName((MClassInFile)proc.parent().parent()) + "::" + proc.name().term();
                throw new RuntimeException();
            }
            public String CaseMConstrInFile(MConstrInFile constr) {
                String name = constr.parent().parent().name().term();
                return name + "::" + name;
            }
            public String CaseMDestrInFile(MDestrInFile destr) {
                String name = destr.parent().parent().name().term();
                return name + "::~" + name;
            }
        });
    }
    
    protected String qualifiedName(MClassInFile mclass) {
        if(mclass.parent().parent() instanceof MFileInFile)
            return mclass.name().term();
        else if(mclass.parent().parent() instanceof MClassInFile)
            return qualifiedName((MClassInFile)mclass.parent().parent()) + "::" + mclass.name().term();
        throw new RuntimeException();
    }
    
    @Override
    public void visit(MClassesInFile classes) throws InvalidConstruct {
        if(classes.size() > 0) {
            // unparse "private" classes first
            for(MClassInFile mclass : filter(classes, PRIVATE()))
                visit(mclass);
            // unparse "public" classes afterwards
            for(MClassInFile mclass : classes.removeAll(filter(classes, PRIVATE()).term()))
                visit(mclass);
            buffer.append('\n');
        }
    }
    
    @Override
    public void visit(MClassInFile mclass) throws InvalidConstruct {
        visit(mclass.methods());
        visit(mclass.nested());
    }

    @Override
    public void visit(MVoidInFile mvoid) { buffer.append("void"); }
    
//    @Override
//    public void visit(MInitInFile init) {
//        visit(init.con());
//        visit(init.vals());
//    }
    
    @Override
    public void visit(MMemberInitsInFile inits) {
        if(!inits.isEmpty()) {
            buffer.append(" : ");
            for(MMemberInitInFile init : inits) visit(init);
        }
    }
    
    @Override
    public void visit(MInitListInFile list) {
        buffer.append('(');
        
        for(StringInFile s : list.params()) {
            if(s.position() != 0) buffer.append(", ");
            buffer.append(s.term());
        }
        
        buffer.append(')');
    }
//    
//    public void visit(MConstrInFile constr) throws InvalidConstruct {
//        visit(constr.doc());
//        super.visit(constr);
//    }
//    
//    public void visit(MDestrInFile destr) throws InvalidConstruct {
//        visit(destr.doc());
//        super.visit(destr);
//    }
//    
//    @Override
    // Do not unparse attributes in sourcefile
//    public void visit(MAttributesInFile attributes) { }
}
