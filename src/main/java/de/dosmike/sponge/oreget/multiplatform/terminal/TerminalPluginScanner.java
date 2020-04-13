package de.dosmike.sponge.oreget.multiplatform.terminal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import de.dosmike.sponge.oreget.cache.ProjectContainer;
import de.dosmike.sponge.oreget.multiplatform.PlatformProbe;
import de.dosmike.sponge.oreget.multiplatform.PluginScanner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class TerminalPluginScanner extends PluginScanner {

    private List<Path> getAllPluginCandidates(Path basePath) throws IOException {
        List<Path> jarFiles = new LinkedList<>();
        for (Path p : Files.list(basePath).collect(Collectors.toSet())) {
            if (Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".jar")) {
                jarFiles.add(p);
            } else if (Files.isDirectory(p)) {
                jarFiles.addAll(getAllPluginCandidates(p));
            }
        };
        return jarFiles;
    }

    private List<ProjectContainer> scanJar(Path jarFile) {
        List<ProjectContainer> containers = new LinkedList<>();
        try (JarFile jar = new JarFile(jarFile.toFile())) {
            JarEntry entry = jar.getJarEntry("mcmod.info");
            if (entry == null) return containers; // Not a plugin, does not contain metadata
            InputStream jis = jar.getInputStream(entry);
            JsonElement metaEntries = new JsonParser().parse(new JsonReader(new InputStreamReader(jis)));
            for (JsonElement descriptor : metaEntries.getAsJsonArray()) {
                mapDescriptor(jarFile, descriptor.getAsJsonObject()).ifPresent(containers::add);
            }
        } catch (IOException ignore) {
            return Collections.emptyList(); // Not a Jar .jar, but something else .jar?
        }
        return containers;
    }

    private Optional<ProjectContainer> mapDescriptor(Path source, JsonObject object) {
        try {
            ProjectContainer.Builder builder = ProjectContainer.builder(object.get("modid").getAsString())
                    .setSource(source)
                    .setName(object.get("name").getAsString())
                    .setVersion(object.has("version") ? object.get("version").getAsString() : "");
            if (object.has("requiredMods")) {
                JsonArray deps = object.getAsJsonArray("requiredMods");
                for (JsonElement dep : deps) builder.addDependency(dep.getAsString());
            }
            if (object.has("authorList")) {
                JsonArray authors = object.getAsJsonArray("authorList");
                String[] authorList = new String[authors.size()];
                for (int i=0; i<authors.size(); i++) authorList[i] = authors.get(i).getAsString();
                builder.setAuthors(authorList);
            }
            if (object.has("description")) {
                builder.setDescription(object.get("description").getAsString());
            }
            return Optional.of(builder.build());
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    @Override
    public List<ProjectContainer> getProjects() {
        try {
            List<Path> candidates = getAllPluginCandidates(PlatformProbe.getModsDirectory());
            Files.list(PlatformProbe.getServerRoot())
                    .filter(path->Files.isRegularFile(path)&&path.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .forEach(candidates::add);
            List<ProjectContainer> containers = new LinkedList<>();
            for (Path jar : candidates) {
                containers.addAll(scanJar(jar));
            }
            return containers;
        } catch (IOException e) {
            throw new RuntimeException("Error while parsing jar-files", e);
        }
    }

}
