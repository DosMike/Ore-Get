package de.dosmike.sponge.oreget.cache;

import de.dosmike.sponge.oreget.oreapi.v2.OreProject;
import de.dosmike.sponge.oreget.oreapi.v2.OreVersion;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectContainer {
    private String pluginId;
    private String currentVersion=null;
    private String cachedVersion=null; //downloaded but inactive
    private Map<String, String> currentDependencies=new HashMap<>(); //id -> version
    private Set<String> cachedDependencies=new HashSet<>(); //dependencies might change, so list for each state
    private Path installedSource=null;
    private String cachedFilename=null;
    private boolean isAuto=false; //installed as dependency, not by user
    private boolean delete=false; //marked for removal
    private boolean purge=false; //remove configs as well
    private boolean holdVersion = false; //ignore when updating
    private Set<String> forbiddenVersions = new HashSet<>(); //if plugin would update to one of these versions, ignore

    //meta information that's extracted from PluginContainers for display purposes
    private String name = "";
    private String[] authors = new String[0];
    private String description = "";

    public ProjectContainer(String pluginId) {
        this.pluginId = pluginId;
    }
    /** after download, this is the information we have */
    public ProjectContainer(OreProject project, OreVersion version, String filename) {
        load(project, version, filename);
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
    public Map<String,String> getActiveDependencies() {
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
    public Optional<Path> getSource() {
        return Optional.ofNullable(installedSource);
    }

    /** get all projects that directly depend on this project within the supplied list */
    public Collection<ProjectContainer> getDependentProjectsFrom(Collection<ProjectContainer> containers) {
        Set<ProjectContainer> dependent = new HashSet<>();
        for (ProjectContainer parent : containers) {
            Collection<String> dependencies = parent.getCachedVersion().isPresent() ? parent.getCachedDependencies() : parent.getActiveDependencies().keySet();
            //add the container if it's not removed and depends on this project
            if (!parent.doDelete() && dependencies.contains(pluginId))
                dependent.add(parent);
        }
        return dependent;
    }

    public String getName() {
        return name;
    }

    public String[] getAuthors() {
        return authors;
    }

    public String getDescription() {
        return description;
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

    public void setVersionHold(boolean holdVersion) {
        this.holdVersion = holdVersion;
    }
    public boolean doHoldVersion() {
        return holdVersion;
    }
    /** @return true if the set of forbidden versions changed trough this command */
    public boolean setForbiddenVersion(String version, boolean forbidden) {
        if (forbidden) return forbiddenVersions.add(version);
        else return forbiddenVersions.remove(version);
    }
    public boolean isVersionForbidden(String version) {
        return forbiddenVersions.contains(version);
    }
    public Set<String> getForbiddenVersions() {
        return forbiddenVersions;
    }

    //Region builder
    public static class Builder {
        ProjectContainer container;
        private Builder(String id) {
            container = new ProjectContainer(id);
        }
        /** @param pluginId the pluginId, might contain <code>@VERSION</code> */
        public Builder addDependency(String pluginId) {
            Pattern idVersionPattern = Pattern.compile("(\\w+)(?:@(.*))?");
            Matcher idVersion = idVersionPattern.matcher(pluginId);
            if (!idVersion.matches()) throw new IllegalArgumentException();
            String version = idVersion.group(2);
            container.currentDependencies.put(idVersion.group(1), version==null?"":version);
            return Builder.this;
        }
        /** @param version arbitrary version string */
        public Builder setVersion(String version) {
            container.currentVersion = version;
            return Builder.this;
        }
        /** @param where the location from where this plugin is currently loaded from, if any */
        public Builder setSource(Path where) {
            container.installedSource = where;
            return Builder.this;
        }
        public Builder setDescription(String description) {
            container.description = description;
            return Builder.this;
        }
        public Builder setAuthors(String[] authors) {
            container.authors = Arrays.copyOf(authors, authors.length);
            return Builder.this;
        }
        /** is actually required my mcmod.info specs, but I'll have it optional emptystring */
        public Builder setName(String name) {
            container.name = name==null?"":name;
            return Builder.this;
        }
        public ProjectContainer build() {
            if (container.currentVersion == null) throw new IllegalStateException("No version was set");
            return container;
        }
    }

    public static Builder builder(String pluginId) {
        return new Builder(pluginId);
    }
    //endregion

}
