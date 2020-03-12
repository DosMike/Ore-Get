package de.dosmike.sponge.oreget.decoupler.standalone;

import com.google.common.collect.Iterators;
import com.google.gson.*;
import de.dosmike.sponge.oreget.decoupler.IDependency;
import de.dosmike.sponge.oreget.decoupler.IPlugin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class JarPlugin implements IPlugin {

    List<String> authors;
    Set<IDependency> deps;
    String desc;
    String name;
    String id;
    boolean loaded;
    Path source;
    String url;
    String version;

    public JarPlugin(JsonObject mcmod, Path source) {
        authors = new LinkedList<>();
        authors.addAll(Arrays.asList((mcmod.has("authorList") && mcmod.get("authorList").isJsonArray() && mcmod.get("authorList").getAsJsonArray().size() > 0)
                ? Iterators.toArray(Iterators.transform(mcmod.get("authorList").getAsJsonArray().iterator(), it -> it.getAsString()), String.class)
                : new String[0]));

        deps = new HashSet<>();
        {
            if (mcmod.has("useDependencyInformation") && mcmod.get("useDependencyInformation").getAsBoolean()) {

                Set<String> required = new HashSet<>();
                Set<String> dependencies = new HashSet<>();
                if (mcmod.has("requiredMods"))
                    required.addAll(Arrays.asList(Iterators.toArray(Iterators.transform(mcmod.get("requiredMods").getAsJsonArray().iterator(), it -> it.getAsString()), String.class)));
                if (mcmod.has("dependencies"))
                    dependencies.addAll(Arrays.asList(Iterators.toArray(Iterators.transform(mcmod.get("dependencies").getAsJsonArray().iterator(), it -> it.getAsString()), String.class)));
                //remove all dependencies that are listed as required
                dependencies.removeIf(o -> {
                    String oid = o.indexOf('@') > 0 ? o.substring(0, o.indexOf('@')) : o;
                    return required.stream().anyMatch(r -> {
                        String rid = r.indexOf('@') > 0 ? r.substring(0, r.indexOf('@')) : r;
                        return oid.equalsIgnoreCase(rid);
                    });
                });
                //now add required as such to deps;
                dependencies.stream().map(JarDependency::new).forEach(deps::add);
                required.stream().map(r -> "required:" + r).map(JarDependency::new).forEach(deps::add);
            }
        }

        desc = mcmod.has("description") ? mcmod.get("description").getAsString() : null ;
        id = mcmod.get("modid").getAsString();
        loaded = false;
        this.source = source;
        name = mcmod.has("name") ? mcmod.get("name").getAsString() : null;
        url = mcmod.has("url") ? mcmod.get("url").getAsString() : null;
        version = mcmod.has("version") ? mcmod.get("version").getAsString() : null;
    }
    public static Collection<JarPlugin> fromJar(Path jarFile) throws IOException {
        Set<JarPlugin> entries = new HashSet<>();
        try {
            JarFile pluginJar = new JarFile(jarFile.toFile());
            ZipEntry entry = pluginJar.getEntry("mcmod.info");
            //we can't load the class to inspect the @Plugin annotation in standalone mode
            // so we have to rely on the mcmod.info that the sponge gradle-plugin generates.
            // afaik Ore relies on this files as well, so we should be good
            if (entry == null) throw new IOException(jarFile.getFileName()+ " does not contain a mcmod.info");
            JsonArray mcmod = new JsonParser().parse(new InputStreamReader(pluginJar.getInputStream(entry))).getAsJsonArray();

            for (int i = 0; i < mcmod.size(); i++) {
                try {
                    entries.add(new JarPlugin(mcmod.get(i).getAsJsonObject(), jarFile));
                } catch (Exception e) {
                    System.err.println("Could not parse plugin "+(i+1)+" in "+jarFile.getFileName()+":");
                    e.printStackTrace();
                }
            }
        } catch (JsonIOException | JsonSyntaxException e) {
            throw new IOException(e);
        }
        return entries;
    }

    @Override
    public List<String> getAuthors() {
        return authors;
    }

    @Override
    public Set<IDependency> getDependencies() {
        return deps;
    }

    @Override
    public Optional<IDependency> getDependency(String id) {
        return deps.stream().filter(dep->dep.getId().equalsIgnoreCase(id)).findFirst();
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(desc);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name != null ? name : id;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public Optional<Path> getSource() {
        return Optional.ofNullable(source);
    }

    @Override
    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }
}
