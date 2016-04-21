package pysidekick;


import jep.Jep;
import jep.JepException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PyEvaluator {
    static class JepEval {
        public final Jep jep;
        public final int identation;

        JepEval(Jep jep, int identation) {
            this.jep = jep;
            this.identation = identation;
        }
    }

    private static int identOf(CharSequence s) {
        int ident = 0;
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ')
                ident++;
            else
                break;
        }
        return ident;
    }

    public static JepEval addBlock(String pythonBlock) throws JepException {
        String[] pythonStatements = pythonBlock.split("\\r?\\n");
        Jep jep = new Jep(true, "C:\\Python34\\Scripts");
        int ident = 0;
        for(String pythonStatement : pythonStatements ) {
            try {
                if ( pythonStatement.matches("\\s+") ) continue;
                jep.eval(pythonStatement);
                ident = identOf(pythonStatement);
            } catch (JepException e) {
                e.printStackTrace();
            }
        }
        return new JepEval(jep, ident);
    }

    public static Set<String> builtinNames() throws JepException {
        Jep jep = new Jep(true, "C:\\Python34\\Scripts");
        jep.eval("names = dir(__builtins__)");
        Set<String> names = new HashSet<String>((Collection<? extends String>) jep.getValue("names"));
        jep.close();
        return names;
    }

    public static Set<String> builtinTypes() throws JepException {
        Jep jep = new Jep(true, "C:\\Python34\\Scripts");
        jep.eval("names = dir(__builtins__)");
        jep.eval("types = {for name in names if type == type(eval(name))}");
        Set<String> types = (Set<String>) jep.getValue("types");
        jep.close();
        return types;
    }



    public static Set<String> builtinModules() throws JepException {
        Jep jep = new Jep(true, "C:\\Python34\\Scripts");
        jep.eval("import sys");
        jep.eval("modules = sys.builtin_module_names");
        Set<String> modules = new HashSet<String>((Collection<? extends String>) jep.getValue("modules"));
        jep.close();
        return modules;
    }

    public static Set<String> moduleNames(String pythonModule) throws JepException {
        Jep jep = new Jep(true, "C:\\Python34\\Scripts");
        jep.eval("names = dir(" + pythonModule + ")");
        Set<String> names = new HashSet<String>((Collection<? extends String>) jep.getValue("names"));
        jep.close();
        return names;
    }

    public static String varType(JepEval jepEval, String variableName) throws JepException {
        StringBuilder ps = new StringBuilder();
        for(int i = 0; i < jepEval.identation; i++) {
            ps.append(' ');
        }
        ps.append("temp_type = type(" + variableName + ")");
        String type = "None";
        try {
            jepEval.jep.eval(ps.toString());
            jepEval.jep.eval(null);
            type = (String) jepEval.jep.getValue("temp_type");
        } catch (JepException e) {
            e.printStackTrace();
        }
        return type;
    }

}
