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
            help =help +"\n\nJBang executable not found in $JBANG_HOME, $PATH, or ~/.jbang/bin. Please install JBang.";
         } else {
            help = help +"\n\nJBang executable found at " + jbangExecutable;
         } 

         kernel.display(kernel.getRenderer().render(help));
         return null;
    }

}
