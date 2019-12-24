package de.dosmike.sponge.oreget.cache;

import com.itwookie.inireader.INIConfig;
import de.dosmike.sponge.oreget.OreGet;
import de.dosmike.sponge.oreget.utils.ExitHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class PluginCache {

    private Set<ProjectContainer> cache = new HashSet<>();
    private INIConfig cacheInfo;
    private File cacheInfoFile;
    public Optional<ProjectContainer> findProject(String pluginId) {
        return cache.stream().filter(p->p.getPluginId().equalsIgnoreCase(pluginId)).findFirst();
    }
    public Collection<ProjectContainer> getProjects() {
        return cache;
    }
    public static final File DIRECTORY = new File(".", "oreget_cache");

    public PluginCache() {
        cacheInfo = new INIConfig();
        cacheInfoFile = new File(DIRECTORY, "plugin.cache");
        if (!cacheInfoFile.exists()) cacheInfoFile.getParentFile().mkdirs();
        else cacheInfo.loadFrom(cacheInfoFile);
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
        Sponge.getPluginManager().getPlugins().forEach(pc->{
            ProjectContainer cont = findProject(pc.getId()).orElseGet(()->new ProjectContainer(pc));

            boolean auto = false;
            if (!cacheInfo.hasKey("auto install", pc.getId()))
                cacheInfo.set(pc.getId(), "false"); //we haven't seen this plugin before, assume manual install
            else
                auto = "true".equals(cacheInfo.get(pc.getId()));
            cont.markAuto(auto);

            if (cacheInfo.hasKey("hold version", pc.getId()))
                cont.setVersionHold("true".equalsIgnoreCase(cacheInfo.get(pc.getId())));
            if (cacheInfo.hasKey("forbidden versions", pc.getId())) {
                String value = cacheInfo.get(pc.getId());
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
            Optional<PluginContainer> spongeData = container.getPluginContainer();
            if (container.doDelete() && container.getInstalledVersion().isPresent()) {
                spongeData
                        .flatMap(PluginContainer::getSource)
                        .ifPresent(p->ExitHandler.deleteOnExit(p.toFile()));
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
                    container.getPluginContainer()
                            .flatMap(PluginContainer::getSource)
                            .ifPresent(p->ExitHandler.deleteOnExit(p.toFile()));
                }
                ExitHandler.moveOnExit(new File(DIRECTORY, container.getCachedFilename()), OreGet.getInstance().getPluginDirectory().resolve(container.getCachedFilename()).toFile());
            }
        }
    }
}
