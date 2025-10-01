package dev.jbang.jupyter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class JBangHelper {
    public static List<String> getJBangResolvedDependencies(String scriptRef, String body, boolean inclAppJar) throws IOException {
        try {
            ProcessBuilder pb = new ProcessBuilder("jbang", "info", "tools", scriptRef); // TODO: locate and install if need be
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
