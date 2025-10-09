package dev.jbang.jupyter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dflib.jjava.jupyter.ExtensionLoader;
import org.dflib.jjava.jupyter.kernel.HelpLink;
import org.dflib.jjava.jupyter.kernel.JupyterIO;
import org.dflib.jjava.jupyter.kernel.LanguageInfo;
import org.dflib.jjava.jupyter.kernel.comm.CommManager;
import org.dflib.jjava.jupyter.kernel.display.DisplayDataRenderable;
import org.dflib.jjava.jupyter.kernel.display.RenderContext;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.common.Image;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;
import org.dflib.jjava.jupyter.kernel.history.HistoryManager;
import org.dflib.jjava.jupyter.kernel.magic.MagicParser;
import org.dflib.jjava.jupyter.kernel.magic.MagicTranspiler;
import org.dflib.jjava.jupyter.kernel.magic.MagicsRegistry;
import org.dflib.jjava.jupyter.kernel.util.StringStyler;
import org.dflib.jjava.kernel.JavaKernel;
import org.dflib.jjava.kernel.JavaKernelBuilder;
import org.dflib.jjava.kernel.execution.CodeEvaluator;
import org.dflib.jjava.kernel.execution.JJavaExecutionControlProvider;

import jdk.jshell.EvalException;
import jdk.jshell.JShell;

/**
 * A Jupyter kernel for Java programming language.
 */
public class JBangKernel extends JavaKernel {

    Logger logger = Logger.getLogger(JBangKernel.class.getName());
    
    private final CodeEvaluator evaluator; // TODO: javakernel should expose this

    @Override
    public Object evalRaw(String source) {
        try {
        if(evaluator == null) { // TODO: this happens because javakernel inits extensions that calls evals
            return super.evalRaw(source);
        }

        source = magicParser.resolveMagics(source);
        // hook in and call jbang if seems relevant/needed
        if(source.contains("//DEPS")) { //TODO: use pattern to spot other directives
            try {
                List<String> deps = JBangHelper.getJBangResolvedDependencies("-", source, false);
                addToClasspath(String.join(File.pathSeparator,deps));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return evaluator.eval(source);  
    } catch(Exception e) {
        logger.log(Level.SEVERE, "Error evaluating source: " + source,e);
        throw e;
    }
    }

    @Override
    public void addToClasspath(String paths) {
        System.out.println("Adding to classpath: " + paths);
        super.addToClasspath(paths);
    }

    /**
     * Starts a builder for a new JJavaKernel.
     */
    public static JBangKernelBuilder jbangBuilder() {
        return new JBangKernelBuilder();
    }

    protected JBangKernel(
            String name,
            String version,
            LanguageInfo languageInfo,
            List<HelpLink> helpLinks,
            HistoryManager historyManager,
            JupyterIO io,
            CommManager commManager,
            Renderer renderer,
            MagicParser magicParser,
            MagicsRegistry magicsRegistry,
            ExtensionLoader extensionLoader,
            boolean extensionsEnabled,
            StringStyler errorStyler,
            JShell jShell,
            CodeEvaluator evaluator) {

        super(
                name,
                version,
                languageInfo,
                helpLinks,
                historyManager,
                io,
                commManager,
                renderer,
                magicParser,
                magicsRegistry,
                extensionLoader,
                extensionsEnabled,
                errorStyler, jShell, evaluator);
        this.evaluator = evaluator;
    }



    public static class JBangKernelBuilder extends JavaKernelBuilder<JBangKernelBuilder, JBangKernel> {
        private JBangKernelBuilder() {
        }

        @Override
        public JBangKernel build() {

            String name = buildName();
            Charset jupyterEncoding = buildJupyterIOEncoding();
            JJavaExecutionControlProvider jShellExecutionControlProvider = buildJShellExecControlProvider(name);
            JShell jShell = buildJShell(jShellExecutionControlProvider);
            LanguageInfo langInfo = buildLanguageInfo();
            MagicTranspiler magicTranspiler = buildMagicTranspiler();

            return new JBangKernel(
                    name,
                    buildVersion(),
                    langInfo,
                    buildHelpLinks(),
                    buildHistoryManager(),
                    buildJupyterIO(jupyterEncoding),
                    buildCommManager(),
                    buildRenderer(),
                    buildMagicParser(magicTranspiler),
                    buildMagicsRegistry(),
                    buildExtensionLoader(),
                    buildExtensionsEnabled(),
                    buildErrorStyler(),
                    jShell,
                    buildCodeEvaluator(jShell, jShellExecutionControlProvider));
        }

        @Override
        protected Renderer buildRenderer() {
            return new DelegateRenderer(super.buildRenderer());
        }
        protected List<HelpLink> buildHelpLinks() {
            return List.of(
                    new HelpLink("JBang homepage", "https://www.jbang.dev/"),
                    new HelpLink("JJava homepage", "https://github.com/dflib/jjava"));
        }

    }

  
}
