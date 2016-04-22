package pysidekick;


import jep.Jep;
import jep.JepException;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PyEvaluator {

    static class JepEval {
        public final Jep jep;
        public final int indentation;

        JepEval(Jep jep, int indentation) {
            this.jep = jep;
            this.indentation = indentation;
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

    public static String expressionVarType(Set<String> importStatements, String expression) {
        String type = "None";
        try (Jep jep = new Jep(true, "C:\\Python34\\Scripts");) {
            for (String importStatement : importStatements ) {
                jep.eval(importStatement);
            }
            jep.eval("temp_type = type(" + expression + ")");
            jep.eval("temp_module = temp_type.__module__ if temp_type else None");
            jep.eval("temp_name = temp_type.__name__ if temp_type else None");

            String module = (String) jep.getValue("temp_module");
            String name = (String) jep.getValue("temp_name");
            if ( "None".equals(name) ) {
                type = (String) jep.getValue("temp_type");
            } else if ("None".equals(module) ) {
                type = name;
            } else {
                if ( "builtins".equals(module) ) {
                    type = name;
                } else {
                    type = module + "." + name;
                }
            }
        } catch (JepException e) {
            e.printStackTrace();
        }
        return type;
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

    public static List<String> namespaceNames(Set<String> importStatements, String namespace ) {
        try ( Jep jep = new Jep(true, "C:\\Python34\\Scripts"); ){
            for (String importStatement : importStatements ) {
                jep.eval(importStatement);
            }
        } catch (JepException e) {
            e.printStackTrace();
        }
        return null;
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

    private static void indent(StringBuilder builder, int indentation) {
        for(int i = 0; i < indentation; i++) {
            builder.append(' ');
        }
    }

    public static String moduleLevelVarType(JepEval jepEval, String variableName) throws JepException {
        StringBuilder ps = new StringBuilder();
        indent(ps, jepEval.indentation);
        ps.append("temp_type = type(" + variableName + ")");
        String type = "None";
        Jep jep = jepEval.jep;
        try {
            jep.eval(ps.toString());
            jep.eval(null);
            jep.eval("temp_module = temp_type.__module__ if temp_type else None");
            jep.eval("temp_name = temp_type.__name__ if temp_type else None");
            String module = (String) jep.getValue("temp_module");
            String name = (String) jep.getValue("temp_name");
            if ( "None".equals(name) ) {
                type = (String) jep.getValue("temp_type");
            } else if ("None".equals(module) ) {
                type = name;
            } else {
                if ( "builtins".equals(module) ) {
                    type = name;
                } else {
                    type = module + "." + name;
                }
            }
            jep.close();
        } catch (JepException e) {
            e.printStackTrace();
        }
        return type;
    }

}
