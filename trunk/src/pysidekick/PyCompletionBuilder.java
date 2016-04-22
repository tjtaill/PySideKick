package pysidekick;


import java.util.*;

public class PyCompletionBuilder {
    public static PyCompletion CURRENT;
    private Map<String, List<String>> typeCompletionCache = new HashMap<>();
    private Map<String, List<String>> namespaceToCompletionsCache = new HashMap<>();
    private Set<String> importStatements = new HashSet<>();




    public void addImportStatement(String importStatement) {
        importStatements.add(importStatement);
    }


    public void addNamespace(String namespaceName, String type) {
        List<String> completions = typeCompletionCache.get(type);
        if (completions == null ) {
            // TODO : build completions for type and store it
            typeCompletionCache.put(type, completions);
        }
        namespaceToCompletionsCache.put(namespaceName, completions);
    }

    public List<String> moduleNameCompletion(String modulePrefix) {
        // TODO : build list of modules based on module prefix
        return null;
    }

    public List<String> namespaceCompletion(String namespace) {
        List<String> completions = namespaceToCompletionsCache.get(namespace);
        if ( completions == null ) {
            completions = Collections.EMPTY_LIST;
        }
        return completions;
    }

}
