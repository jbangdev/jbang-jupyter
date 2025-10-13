package dev.jbang.jupyter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;

public class JBangHelper {

    final static Logger logger = Logger.getLogger(JBangHelper.class.getName());
    
    /**
     * Looks for nearest JBang executable in the following priority order:
     * 
     * Note: on windows it will look for jbang.cmd not jbang which is a shell script.
     * 
     * 1. /bin/jbang
     * 2. jbang in 
     * 3. ~/.jbang/bin/jbang
     *
     * If none of the above are found, it will return null.
     * 
     * @return absolute patht to a JBang executable or null if none is found
     * @throws IOException
     */
    public static String findJBangExecutable() throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        String jbangExecutable = isWindows ? "jbang.cmd" : "jbang";
            
        // 1. /bin/jbang
        String jbangHome = System.getenv("JBANG_HOME");
        if (jbangHome != null) {
            var homePath = Path.of(jbangHome).resolve("bin/" + jbangExecutable);
            if (Files.exists(homePath)) {
                return homePath.toString();
            }
        }

        // 2. jbang in 
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(File.pathSeparator)) {
                Path execPath = Path.of(dir).resolve(jbangExecutable);
                if (Files.exists(execPath)) {
                    return execPath.toString();
                }
            }
        }

        // 3. ~/.jbang/bin/jbang
        var homePath = Path.of(System.getProperty("user.home")).resolve(".jbang/bin/" + jbangExecutable);
        if (Files.exists(homePath)) {
            return homePath.toString();
        }

        // 4. none found
        return null;
    }

    public static class JBangInfo {
        List<String> resolvedDependencies;
        List<String> dependencies;
        List<String> newResolvedDependencies;

        public JBangInfo(List<String> resolvedDependencies, List<String> dependencies) {
            this.resolvedDependencies = resolvedDependencies;
            this.dependencies = dependencies;
        }

        public void resolve(Set<String> existingClasspath) {
            this.newResolvedDependencies = getNewResolvedDependencies(existingClasspath);
        }

        public List<String> getNewResolvedDependencies(Set<String> existingClasspath) {
            return resolvedDependencies.stream()
                    .filter(dependency -> !existingClasspath.contains(dependency))
                    .collect(Collectors.toList());
        }

        public List<String> getDependencies() {
            return dependencies;
        }
        public List<String> getResolvedDependencies() {
            return resolvedDependencies;
        }
        public List<String> getNewResolvedDependencies() {
            return newResolvedDependencies;
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder("JBangInfo{");
            sb.append("dependencies=").append(dependencies.size());
            sb.append(",resolvedDependencies=").append(resolvedDependencies.size());
            if (newResolvedDependencies != null) {
                sb.append(",newResolvedDependencies=").append(newResolvedDependencies.size());
            }
            sb.append('}');
            return sb.toString();
        }

        public static void registerAllRenderers(Renderer renderer) {
            renderer
    .createRegistration(dev.jbang.jupyter.JBangHelper.JBangInfo.class)
    .preferring(MIMEType.TEXT_HTML)
    .register((info, ctx) -> {
        ctx.renderIfRequested(MIMEType.TEXT_HTML, () -> {
            StringBuilder html = new StringBuilder();
            
            // Create set of new dependencies for quick lookup
            Set<String> newDepsSet = new HashSet<>();
            if (info.getNewResolvedDependencies() != null) {
                newDepsSet.addAll(info.getNewResolvedDependencies());
            }
            
            // Summary section
            int totalRequested = (info.getDependencies() != null) ? info.getDependencies().size() : 0;
            int totalResolved = (info.getResolvedDependencies() != null) ? info.getResolvedDependencies().size() : 0;
            int totalNew = (info.getNewResolvedDependencies() != null) ? info.getNewResolvedDependencies().size() : 0;
            
            // Styles
            html.append("<style>\n");
            html.append(".jbang-compact { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif; font-size: 12px; margin: 4px 0; }\n");
            html.append(".jbang-inline { display: inline-block; margin: 0; padding: 0; vertical-align: top; }\n");
            html.append(".jbang-inline summary { display: inline; cursor: pointer; user-select: none; padding: 2px 6px; border-radius: 3px; transition: background 0.15s; }\n");
            html.append(".jbang-inline summary:hover { background: #f1f3f5; }\n");
            html.append(".jbang-inline-content { margin-top: 6px; padding: 8px; background: #f8f9fa; border-radius: 4px; border-left: 2px solid #667eea; }\n");
            html.append(".jbang-inline-new .jbang-inline-content { border-left-color: #10b981; }\n");
            html.append(".jbang-inline-coord .jbang-inline-content { border-left-color: #764ba2; }\n");
            html.append(".jbang-list { list-style: none; padding: 0; margin: 0; font-size: 11px; }\n");
            html.append(".jbang-list-item { padding: 4px 6px; margin: 2px 0; background: white; border-radius: 2px; border-left: 2px solid #667eea; font-family: 'Monaco', 'Menlo', monospace; color: #2d3748; word-break: break-all; cursor: help; }\n");
            html.append(".jbang-list-item:hover { background: #f8f9fa; }\n");
            html.append(".jbang-list-item-new { border-left-color: #10b981; }\n");
            html.append(".jbang-list-item-old { opacity: 0.4; border-left-color: #adb5bd; }\n");
            html.append(".jbang-list-item-old:hover { opacity: 0.6; }\n");
            html.append(".jbang-list-item-coord { border-left-color: #764ba2; cursor: default; }\n");
            html.append("</style>\n");
            
            if (totalRequested > 0 || totalResolved > 0) {
                html.append("<div class='jbang-compact'>\n");
                html.append("  🚀 JBang: ");
            
                // Requested dependencies - always show, inline expandable if > 0
                if (info.getDependencies() != null && !info.getDependencies().isEmpty()) {
                    html.append("<details class='jbang-inline jbang-inline-coord'><summary title='Requested Maven dependencies (click to expand)'>📦 <strong>").append(totalRequested).append("</strong></summary>");
                    html.append("<div class='jbang-inline-content'><ul class='jbang-list'>");
                    for (String dep : info.getDependencies()) {
                        html.append("<li class='jbang-list-item jbang-list-item-coord'>").append(dep).append("</li>");
                    }
                    html.append("</ul></div></details>");
                } else {
                    html.append("<span title='Requested Maven dependencies'>📦 <strong>").append(totalRequested).append("</strong></span>");
                }
                
                html.append(" | ");
                
                // New dependencies - always show, inline expandable if > 0
                if (info.getNewResolvedDependencies() != null && !info.getNewResolvedDependencies().isEmpty()) {
                    html.append("<details class='jbang-inline jbang-inline-new'><summary title='Newly added to classpath (click to expand)'>✨ <strong>").append(totalNew).append("</strong></summary>");
                    html.append("<div class='jbang-inline-content'><ul class='jbang-list'>");
                    for (String dep : info.getNewResolvedDependencies()) {
                        String fileName = dep.substring(dep.lastIndexOf('/') + 1);
                        html.append("<li class='jbang-list-item jbang-list-item-new' title='").append(dep).append("'>").append(fileName).append("</li>");
                    }
                    html.append("</ul></div></details>");
                } else {
                    html.append("<span title='Newly added to classpath'>✨ <strong>").append(totalNew).append("</strong></span>");
                }
                
                html.append(" | ");
                
                // All resolved dependencies - always show, inline expandable if > 0
                if (info.getResolvedDependencies() != null && !info.getResolvedDependencies().isEmpty()) {
                    html.append("<details class='jbang-inline'><summary title='All resolved JARs (click to expand, grayed = cached)'>📚 <strong>").append(totalResolved).append("</strong></summary>");
                    html.append("<div class='jbang-inline-content'><ul class='jbang-list'>");
                    for (String dep : info.getResolvedDependencies()) {
                        String fileName = dep.substring(dep.lastIndexOf('/') + 1);
                        boolean isNew = newDepsSet.contains(dep);
                        String itemClass = isNew ? "jbang-list-item jbang-list-item-new" : "jbang-list-item jbang-list-item-old";
                        html.append("<li class='").append(itemClass).append("' title='").append(dep).append("'>").append(fileName).append("</li>");
                    }
                    html.append("</ul></div></details>");
                } else {
                    html.append("<span title='All resolved JARs'>📚 <strong>").append(totalResolved).append("</strong></span>");
                }
                
                html.append("</div>\n");
            } else {
                html.append("<div class='jbang-compact'>🚀 JBang: <em>No dependencies</em></div>\n");
            }
            
            return html.toString();
        });
    });
        }
    }
    public static JBangInfo getJBangResolvedDependencies(String scriptRef, String body, boolean inclAppJar) throws IOException {
        try {
            String jbangExecutable = findJBangExecutable();
            if (jbangExecutable == null) {
                throw new RuntimeException("JBang executable not found in $JBANG_HOME, $PATH, or ~/.jbang/bin. Please install JBang.");
            }

            ProcessBuilder pb = new ProcessBuilder(jbangExecutable, "info", "tools", scriptRef); // TODO: locate and install if need be
            pb.redirectErrorStream(false);
            Process process = pb.start();

            if (body != null) {
                try (java.io.OutputStream os = process.getOutputStream()) {
                    os.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    os.flush();
                }
            }

            StringBuilder output = new StringBuilder();
            try (java.io.InputStream is = process.getInputStream();
                 java.util.Scanner scanner = new java.util.Scanner(is, StandardCharsets.UTF_8)) {
                while (scanner.hasNextLine()) {
                    output.append(scanner.nextLine()).append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("jbang info tools failed with exit code " + exitCode + ":\n" + output);
            }


            logger.fine("jbang info tools output: " + output.toString());
            JsonObject json = JsonParser.parseString(output.toString()).getAsJsonObject();

            List<String> resolvedDependencies = new ArrayList<>();
            if (json.has("resolvedDependencies") && json.get("resolvedDependencies").isJsonArray()) {
                resolvedDependencies = StreamSupport.stream(json.getAsJsonArray("resolvedDependencies").spliterator(), false)
                        .map(JsonElement::getAsString)
                        .collect(Collectors.toList());
            }

            List<String> dependencies = new ArrayList<>();
            if (json.has("dependencies") && json.get("dependencies").isJsonArray()) {
                dependencies = StreamSupport.stream(json.getAsJsonArray("dependencies").spliterator(), false)
                        .map(JsonElement::getAsString)
                        .collect(Collectors.toList());
            }

            if (inclAppJar) {
                resolvedDependencies.add(json.get("applicationJar").getAsString());
            }

            return new JBangInfo(resolvedDependencies, dependencies);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
