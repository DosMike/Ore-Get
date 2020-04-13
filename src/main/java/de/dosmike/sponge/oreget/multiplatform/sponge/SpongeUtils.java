package de.dosmike.sponge.oreget.multiplatform.sponge;

import de.dosmike.sponge.oreget.cache.ProjectContainer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.Optional;

public class SpongeUtils {

    public static Optional<PluginContainer> toPlugin(ProjectContainer projectContainer) {
        return Sponge.getPluginManager().getPlugin(projectContainer.getPluginId());
    }

}
