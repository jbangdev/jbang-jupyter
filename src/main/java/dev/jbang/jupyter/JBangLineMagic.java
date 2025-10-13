package dev.jbang.jupyter;

import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicsArgs;
import org.dflib.jjava.kernel.JavaKernel;

import dev.jbang.jupyter.JBangHelper.JBangInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Builds a JBang script and adds the resolved dependencies to the classpath.
 */
public class JBangLineMagic implements LineMagic<Void, JBangKernel> {


    public Void eval(JBangKernel kernel, List<String> args) throws IOException, InterruptedException {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Loading from JBang requires at least a path to a JBang script reference.");
        }

        MagicsArgs schema = MagicsArgs.builder()
                .required("scriptRef")
                .onlyKnownKeywords().onlyKnownFlags().build();

        Map<String, List<String>> vals = schema.parse(args);
        String scriptRef = vals.get("scriptRef").get(0);

        ProcessBuilder pb = new ProcessBuilder("jbang", "build", scriptRef);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Building failed with exit code " + exitCode);
        }

        try {
            JBangInfo jbangInfo = JBangHelper.getJBangResolvedDependencies(scriptRef, null, true);
            kernel.addToClasspath(jbangInfo);
            return null;
            // let the kernel evaluate the body after the dependencies are resolved
            // TODO: this is not right way as extensions can't give callback
            //JavaKernel kernel = JJava.getKernproelInstance();
            //kernel.eval(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
