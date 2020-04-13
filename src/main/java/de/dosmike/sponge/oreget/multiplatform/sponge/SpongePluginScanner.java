package de.dosmike.sponge.oreget.multiplatform.sponge;

import de.dosmike.sponge.oreget.cache.ProjectContainer;
import de.dosmike.sponge.oreget.multiplatform.PluginScanner;
import org.spongepowered.api.Sponge;
import org.spongepowered.plugin.meta.PluginDependency;

import java.util.List;
import java.util.stream.Collectors;

public class SpongePluginScanner extends PluginScanner {

    @Override
    public List<ProjectContainer> getProjects() {
        return Sponge.getPluginManager().getPlugins().stream().map(plugin->{
            ProjectContainer.Builder builder = ProjectContainer.builder(plugin.getId())
                .setSource(plugin.getSource().orElse(null))
                .setDescription(plugin.getDescription().orElse(""))
                .setAuthors(plugin.getAuthors().toArray(new String[0]))
                .setVersion(plugin.getVersion().orElse(""));
            plugin.getDependencies().stream().map(dep->dep.getId()+"@"+dep.getVersion()).forEach(builder::addDependency);
            return builder.build();
        }).collect(Collectors.toList());
    }

}
