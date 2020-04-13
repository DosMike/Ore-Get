package de.dosmike.sponge.oreget.cache;

import com.itwookie.inireader.INIConfig;
import de.dosmike.sponge.oreget.multiplatform.PlatformProbe;
import de.dosmike.sponge.oreget.multiplatform.PluginScanner;
import de.dosmike.sponge.oreget.utils.ExitHandler;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class PluginCache {

    private static PluginCache instance=null;
    public static PluginCache get() {
        if (instance == null) {
            instance = new PluginCache();
        }
        return instance;
    }

    private Set<ProjectContainer> cache = new HashSet<>();
    private INIConfig cacheInfo;
    private File cacheInfoFile;
    public Optional<ProjectContainer> findProject(String pluginId) {
        return cache.stream().filter(p->p.getPluginId().equalsIgnoreCase(pluginId)).findFirst();
    }
    public Collection<ProjectContainer> getProjects() {
        return cache;
    }
    public static final Path DIRECTORY = PlatformProbe.getCacheDirectory();

    public PluginCache() {
        cacheInfo = new INIConfig();
        cacheInfoFile = new File(DIRECTORY.toFile(), "plugin.cache");
        if (cacheInfoFile.exists()) cacheInfo.loadFrom(cacheInfoFile);
    }

    public void registerPlugin(ProjectContainer container) {
        if (!cache.add(container)) {
            //plugin already registered, don't remove anymore
            findProject(container.getPluginId()).get().flagForRemoval(false, false);
        };
    }
    public void markForRemoval(String pluginId, boolean purge) {
        findProject(pluginId).ifPresent(p->markForRemoval(p,purge));
    }
    public void markForRemoval(ProjectContainer container, boolean purge) {
        container.flagForRemoval(true, purge);
    }

    public void scanLoaded() {
        PluginScanner.create().getProjects().stream().filter(cont->!findProject(cont.getPluginId()).isPresent()).forEach(cont ->{
            boolean auto = false;
            if (!cacheInfo.hasKey("auto install", cont.getPluginId()))
                cacheInfo.set(cont.getPluginId(), "false"); //we haven't seen this plugin before, assume manual install
            else
                auto = "true".equals(cacheInfo.get(cont.getPluginId()));
            cont.markAuto(auto);

            if (cacheInfo.hasKey("hold version", cont.getPluginId()))
                cont.setVersionHold("true".equalsIgnoreCase(cacheInfo.get(cont.getPluginId())));
            if (cacheInfo.hasKey("forbidden versions", cont.getPluginId())) {
                String value = cacheInfo.get(cont.getPluginId());
                cont.getForbiddenVersions().clear();
                if (value != null && !value.trim().isEmpty()) {
                    for (String f : value.trim().split(" and "))
                        cont.setForbiddenVersion(f.trim(), true);
                }
            }

            cache.add(cont);
        });
    }

    public boolean isStubbed(ProjectContainer project) {
        return isStubbed(new HashSet<>(), project);
    }
    private boolean isStubbed(Set<ProjectContainer> ignored, ProjectContainer project) {
        //is manually installed and not deleted? -> required (not stubbed)
        if (!project.doDelete() && !project.isAuto()) return false;
        Collection<ProjectContainer> parents = project.getDependentProjectsFrom(cache);
        boolean required=false;
        for (ProjectContainer parent : parents) {
            if (ignored.contains(parent))
                continue; //prevent scanning cyclic dependencies (maybe in case of optionals)
            ignored.add(parent);
            if (!isStubbed(ignored, parent)) //parent not stubbed -> we're not stubbed
                required=true;
            ignored.remove(parent);
            if (required)
                break;
        }
        return !required;
    }

    public void save() {
        for (ProjectContainer cont : cache) {
            cacheInfo.set("auto install", cont.getPluginId(), cont.isAuto() ? "true" : "false");
            cacheInfo.set("hold version", cont.getPluginId(), cont.doHoldVersion() ? "true" : "false");
            cacheInfo.set("forbidden versions", cont.getPluginId(), String.join(" and ", cont.getForbiddenVersions()));
        }
        cacheInfo.saveFile(cacheInfoFile);
    }

    public void notifyExitHandler() {
        for (ProjectContainer container : cache) {
            if (container.doDelete() && container.getInstalledVersion().isPresent()) {
                container.getSource().ifPresent(cont->ExitHandler.deleteOnExit(cont.toFile()));
            }
            if (container.doPurge() && container.getInstalledVersion().isPresent()) {
                // configdir = config/sponge/general.conf->sponge.general.config-dir
                // delete configdir/pluginid.conf
                // delete configdir/pluginid/**
            }
            if (!container.doDelete() && container.getCachedVersion().isPresent()) {
                //we can install / update
                if (container.getInstalledVersion().isPresent()) {
                    //update, remove old version
                    container.getSource().ifPresent(cont->ExitHandler.deleteOnExit(cont.toFile()));
                }
                ExitHandler.moveOnExit(new File(DIRECTORY.toFile(), container.getCachedFilename()), PlatformProbe.getPluginsDirectory().resolve(container.getCachedFilename()).toFile());
            }
        }
    }
}
