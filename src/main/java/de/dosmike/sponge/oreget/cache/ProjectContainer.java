package de.dosmike.sponge.oreget.cache;

import de.dosmike.sponge.oreget.OreGet;
import de.dosmike.sponge.oreget.oreapi.v2.OreProject;
import de.dosmike.sponge.oreget.oreapi.v2.OreVersion;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.*;

public class ProjectContainer {
    private String pluginId;
    private String currentVersion=null;
    private String cachedVersion=null; //downloaded but inactive
    private Set<String> currentDependencies=new HashSet<>();
    private Set<String> cachedDependencies=new HashSet<>(); //dependencies might change, so list for each state
    private String cachedFilename=null;
    private boolean isAuto=false; //installed as dependency, not by user
    private boolean delete=false; //marked for removal
    private boolean purge=false; //remove configs as well

    public ProjectContainer(String pluginId) {
        this.pluginId = pluginId;
    }
    public ProjectContainer(PluginContainer container) {
        load(container);
    }
    /** after download, this is the information we have */
    public ProjectContainer(OreProject project, OreVersion version, String filename) {
        load(project, version, filename);
    }

    public void load(PluginContainer container) {
        pluginId = container.getId();
        currentVersion = container.getVersion().orElse("N/A");
        currentDependencies.clear();
        container.getDependencies().forEach(dep->currentDependencies.add(dep.getId()));
    }
    /** after download, this is the information we have */
    public void load(OreProject project, OreVersion version, String filename) {
        pluginId = project.getPluginId();
        cachedVersion = version.getName();
        cachedDependencies.clear();
        cachedFilename = filename;
        Arrays.stream(version.getDependencies()).forEach(dep->cachedDependencies.add(dep.getPluginId()));
    }

    public void markAuto(boolean auto) {
        isAuto = auto;
    }
    public void flagForRemoval(boolean remove, boolean purge) {
        this.delete = remove;
        this.purge = remove && purge;
    }

    /** Version currently in mods folder and loaded */
    public Optional<String> getInstalledVersion() {
        return Optional.ofNullable(currentVersion);
    }
    /** Version that is downloaded, but inactive */
    public Optional<String> getCachedVersion() {
        return Optional.ofNullable(cachedVersion);
    }
    /** true if this was downloaded as dependency, not by user */
    public boolean isAuto() {
        return isAuto;
    }
    public Set<String> getActiveDependencies() {
        return currentDependencies;
    }
    public Set<String> getCachedDependencies() {
        return cachedDependencies;
    }

    public String getPluginId() {
        return pluginId;
    }

    public boolean doDelete() {
        return delete;
    }
    public boolean doPurge() {
        return delete && purge;
    }
    public String getCachedFilename() {
        return cachedFilename;
    }

    public Optional<PluginContainer> getPluginContainer() {
        return Sponge.getPluginManager().getPlugin(getPluginId());
    }

    /** get all projects that directly depend on this project within the supplied list */
    public Collection<ProjectContainer> getDependentProjectsFrom(Collection<ProjectContainer> containers) {
        Set<ProjectContainer> dependent = new HashSet<>();
        for (ProjectContainer parent : containers) {
            Collection<String> dependencies = parent.getCachedVersion().isPresent() ? parent.getCachedDependencies() : parent.getActiveDependencies();
            //add the container if it's not removed and depends on this project
            if (!parent.doDelete() && dependencies.contains(pluginId))
                dependent.add(parent);
        }
        return dependent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectContainer that = (ProjectContainer) o;
        return pluginId.equals(that.pluginId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginId);
    }
}
