package pysidekick;


import java.util.*;

public class PyCompletionBuilder {
    public static PyCompletion CURRENT;
    private Map<String, List<String>> typeCompletionCache = new HashMap<>();
    private Map<String, List<String>> namespaceToCompletionsCache = new HashMap<>();
    private static List<String> topLevelModules = PyEvaluator.importableModules();


    public void addVar(String varName, String type) {
        List<String> completions = null;

        completions = typeCompletionCache.get(type);

        if (completions == null) {
            if ("None".equals(type) ) {
                completions = Collections.EMPTY_LIST;
            } else {
                completions = PyEvaluator.typeNames(type);
            }
            typeCompletionCache.put(type, completions);
        }
        namespaceToCompletionsCache.put(varName, completions);
    }

    public void addModule(String moduleName) {
        List<String> completions = null;

        completions = namespaceToCompletionsCache.get(moduleName);

        if (completions == null) {
            completions = PyEvaluator.moduleNames(moduleName);
        }
        namespaceToCompletionsCache.put(moduleName, completions);
    }

    public List<String> moduleCompletion() {
        return topLevelModules;
    }

    public List<String> namespaceCompletion(String namespace) {
        List<String> completions = namespaceToCompletionsCache.get(namespace);
        if ( completions == null ) {
            completions = Collections.EMPTY_LIST;
        }
        return completions;
    }

}
