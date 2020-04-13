package de.dosmike.sponge.oreget.jobs;

import de.dosmike.sponge.oreget.cache.PluginCache;
import de.dosmike.sponge.oreget.cache.ProjectContainer;
import de.dosmike.sponge.oreget.multiplatform.JobManager;
import de.dosmike.sponge.oreget.multiplatform.Logging;

import java.util.*;

public class RemoveJob implements AbstractJob {

    private Set<String> manual = new HashSet<>();
    private boolean doPurge;
    public RemoveJob(boolean purge, String... pluginIds) {
        this.doPurge = purge;
        manual.addAll(Arrays.asList(pluginIds));
    }

    float progress;

    @Override
    public float getProgress() {
        return progress;
    }

    @Override
    public String getMessage() {
        return null;
    }

    @Override
    public void run() {
        Set<String> remove = new TreeSet<>(String::compareToIgnoreCase);
        Set<String> restore = new TreeSet<>(String::compareToIgnoreCase);
        Set<String> invalid = new TreeSet<>(String::compareToIgnoreCase);
        int i = 0;
        for (String id : manual) {
            Optional<ProjectContainer> project = PluginCache.get().findProject(id);
            if (project.isPresent() && project.get().getInstalledVersion().isPresent() || project.get().getCachedVersion().isPresent()) {
                if (project.get().doDelete()) {
                    PluginCache.get().registerPlugin(project.get());
                    restore.add(id);
                } else {
                    PluginCache.get().markForRemoval(project.get(), doPurge);
                    remove.add(id);
                }
            } else {
                invalid.add(id);
            }
            i++;
            progress = (float)i/manual.size();
        }
        if (!remove.isEmpty()) {
            JobManager.get().println(Logging.Color.YELLOW, "The following plugins will be removed with the next /stop:");
            JobManager.get().println(String.join(", ", remove));
        }
        if (!restore.isEmpty()) {
            JobManager.get().println(Logging.Color.YELLOW, "The following plugin will no longer be removed:");
            JobManager.get().println(String.join(", ", restore));
        }
        if (!invalid.isEmpty())
            JobManager.get().println(Logging.Color.RED, "The following plugin were not recognized:");
        JobManager.get().println(String.join(", ", invalid));
    }

}
