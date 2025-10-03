package dev.jbang.jupyter;

import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.kernel.JavaKernel;

public class HelpMagic implements LineMagic<Void, JavaKernel> {

    @Override
    public Void eval(JavaKernel kernel, java.util.List<String> args) throws Exception {
        
        String help = """
        Welcome to the JBang Jupyter kernel.

        This is still a work in progress. Please provide feedback at
        https://github.com/jbangdev/jbang-jupyter

        Available magics:
        %jbang <scriptRef> - Load a JBang script
        %help - Show this help

        """;

        String jbangExecutable = JBangHelper.findJBangExecutable();
        if (jbangExecutable == null) {
            help =help +"JBang executable not found in $JBANG_HOME, $PATH, or ~/.jbang/bin. Please install JBang.";
         } else {
            help = help +"JBang executable found at " + jbangExecutable;
         } 

         help = help + "\nJava version: " + System.getProperty("java.version");
         help = help + "\nJava vendor: " + System.getProperty("java.vendor");
         help = help + "\nJava vendor URL: " + System.getProperty("java.vendor.url");
         help = help + "\nJava home: " + System.getProperty("java.home");
         help = help + "\nJava class path: " + System.getProperty("java.class.path");
         help = help + "\nJava library path: " + System.getProperty("java.library.path");
         kernel.display(kernel.getRenderer().render(help));
         return null;
    }

}
