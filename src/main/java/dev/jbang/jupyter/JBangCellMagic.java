package dev.jbang.jupyter;

import org.dflib.jjava.jupyter.kernel.magic.CellMagic;
import org.dflib.jjava.kernel.JavaKernel;

import java.util.List;

/**
 * Builds a JBang script and adds the resolved dependencies to the classpath.
 *
 * Currently NOT enabled in JBang Kernel as it will automatially use jbang if //DEPS present.
 */
public class JBangCellMagic implements CellMagic<Void, JavaKernel> {

    public Void eval(JavaKernel kernel, List<String> args, String body) throws Exception {
        try {
            List<String> resolvedDependencies = JBangHelper.getJBangResolvedDependencies("-", body,false);
            kernel.addToClasspath(resolvedDependencies);
            // let the kernel evaluate the body after the dependencies are resolved
            // TODO: this is not right way as extensions can't give callback
            //JavaKernel kernel = JJava.getKernelInstance();
            //kernel.eval(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
