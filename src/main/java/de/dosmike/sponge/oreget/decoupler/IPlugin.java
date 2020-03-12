package de.dosmike.sponge.oreget.decoupler;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Wrapper for Sponge PluginContainer in Sponge, Element wrapper for mcmod.info for Standalone */
public interface IPlugin {

    List<String> getAuthors();
    Set<? extends IDependency> getDependencies();
    Optional<? extends IDependency> getDependency(String id);
    Optional<String> getDescription();
    String getId();
    /** @return the plugin name, or plugin id if unknown */
    String getName();
    boolean isLoaded();
    Optional<Path> getSource();
    Optional<String> getUrl();
    Optional<String> getVersion();

}
