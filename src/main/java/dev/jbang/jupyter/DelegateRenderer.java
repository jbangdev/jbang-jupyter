package dev.jbang.jupyter;

import java.util.Map;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.RenderContext;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;

public class DelegateRenderer extends Renderer {

    private final Renderer renderer;

    public DelegateRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public <T> RenderRegistration<T> createRegistration(Class<T> type) {
        return renderer.createRegistration(type);
    }

    @Override
    public <T> void register(
            java.util.Set<org.dflib.jjava.jupyter.kernel.display.mime.MIMEType> supported,
            java.util.Set<org.dflib.jjava.jupyter.kernel.display.mime.MIMEType> preferred,
            java.util.Set<Class<? extends T>> types,
            org.dflib.jjava.jupyter.kernel.display.RenderFunction<T> function) {
        renderer.register(supported, preferred, types, function);
    }

    @Override
    public DisplayData render(Object value, Map<String, Object> params) {
        return renderer.render(value, params);
    }

    @Override
    public DisplayData render(Object value) {
        return renderer.render(value);
    }

    @Override
    public DisplayData renderAs(Object value, String... types) {
        return fixit(value,renderer.renderAs(value, types), types);
    }

    @Override
    public DisplayData renderAs(Object value, Map<String, Object> arg1, String... types) {
        return fixit(value,super.renderAs(value, arg1, types), types);
    }

    DisplayData fixit(Object value, DisplayData data, String[] types) {
         // allow string to be returned as any specific type
         if(value instanceof String) {
            for (String type : types) {
                MIMEType wanted = MIMEType.parse(type);
                if ((!wanted.isWildcard() && !wanted.subtypeIsWildcard()) && !data.hasDataForType(wanted)) {
                    data.putData(wanted,String.valueOf(value));
                }
            }
        }
        return data;
    }

}
