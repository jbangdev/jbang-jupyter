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
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class JBangHelper {

    final static Logger logger = Logger.getLogger(JBangHelper.class.getName());
    
    /**
     * Looks for nearest JBang executable in the following priority order:
     * 
     * Note: on windows it will look for jbang.cmd not jbang which is a shell script.
     * 
     * 1. $JBANG_HOME/bin/jbang
     * 2. jbang in $PATH
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
            
        // 1. $JBANG_HOME/bin/jbang
        String jbangHome = System.getenv("JBANG_HOME");
        if (jbangHome != null) {
            var homePath = Path.of(jbangHome).resolve("bin/" + jbangExecutable);
            if (Files.exists(homePath)) {
                return homePath.toString();
            }
        }

        // 2. jbang in $PATH
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

    public static List<String> getJBangResolvedDependencies(String scriptRef, String body, boolean inclAppJar) throws IOException {
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

            if (inclAppJar) {
                resolvedDependencies.add(json.get("applicationJar").getAsString());
            }

            return resolvedDependencies;


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
