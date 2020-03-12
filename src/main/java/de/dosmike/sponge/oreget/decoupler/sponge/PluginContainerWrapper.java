package de.dosmike.sponge.oreget.decoupler.sponge;

import de.dosmike.sponge.oreget.decoupler.IDependency;
import de.dosmike.sponge.oreget.decoupler.IPlugin;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PluginContainerWrapper implements IPlugin {

    List<String> authors;
    Set<IDependency> deps;
    String desc;
    String id;
    String name;
    boolean loaded;
    Path source;
    String url;
    String version;

    public PluginContainerWrapper(PluginContainer container) {
        authors = container.getAuthors();
        deps = container.getDependencies().stream().map(PluginDependencyWrapper::new).collect(Collectors.toSet());
        desc = container.getDescription().orElse(null);
        id = container.getId();
        name = container.getName();
        loaded = Sponge.getPluginManager().isLoaded(container.getId());
        source = container.getSource().orElse(null);
        url = container.getUrl().orElse(null);
        version = container.getVersion().orElse(null);
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
        return name;
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
