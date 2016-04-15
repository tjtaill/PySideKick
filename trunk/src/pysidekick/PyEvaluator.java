package pysidekick;


import jep.Jep;
import jep.JepException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PyEvaluator {

    public Jep addBlock(String pythonBlock) throws JepException {
        String[] pythonStatements = pythonBlock.split("\\\\r?\\\\n");
        Jep jep = new Jep(true);
        for(String pythonStatement : pythonStatements ) {
            jep.eval(pythonStatement);
        }
        return jep;
    }

    public static Set<String> builtinNames() throws JepException {
        Jep jep = new Jep(true);
        jep.eval("names = dir(__builtins__)");
        Set<String> names = new HashSet<String>((Collection<? extends String>) jep.getValue("names"));
        jep.close();
        return names;
    }

    public static Set<String> builtinTypes() throws JepException {
        Jep jep = new Jep(true);
        jep.eval("names = dir(__builtins__)");
        jep.eval("types = {for name in names if type == type(eval(name))}");
        Set<String> types = (Set<String>) jep.getValue("types");
        jep.close();
        return types;
    }



    public static Set<String> builtinModules() throws JepException {
        Jep jep = new Jep(true);
        jep.eval("import sys");
        jep.eval("modules = sys.builtin_module_names");
        Set<String> modules = new HashSet<String>((Collection<? extends String>) jep.getValue("modules"));
        jep.close();
        return modules;
    }

    public static Set<String> moduleNames(String pythonModule) throws JepException {
        Jep jep = new Jep(true);
        jep.eval("names = dir(" + pythonModule + ")");
        Set<String> names = new HashSet<String>((Collection<? extends String>) jep.getValue("names"));
        jep.close();
        return names;
    }

}
