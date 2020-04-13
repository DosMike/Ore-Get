package de.dosmike.sponge.oreget.jobs;

import de.dosmike.sponge.oreget.cache.PluginCache;
import de.dosmike.sponge.oreget.cache.ProjectContainer;
import de.dosmike.sponge.oreget.multiplatform.JobManager;
import de.dosmike.sponge.oreget.multiplatform.Logging;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class AutoRemoveJob implements AbstractJob {

    float progress = 0f;
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
        Set<ProjectContainer> stubbed = new HashSet<>();
        int i = 0; int amount = PluginCache.get().getProjects().size();
        for (ProjectContainer container : PluginCache.get().getProjects()) {
            if (PluginCache.get().isStubbed(container))
                stubbed.add(container);
            i++;
            progress = (float)i/amount;
        }
        if (stubbed.size()>0) {
            stubbed.forEach(project -> PluginCache.get().markForRemoval(project, false));
            JobManager.get().println(Logging.Color.YELLOW, "Found ", stubbed.size(), " stubbed plugins:");
            JobManager.get().println(
                    stubbed.stream()
                    .map(ProjectContainer::getPluginId)
                    .collect(Collectors.joining(", ")
            ));
            JobManager.get().println(Logging.Color.RED, "The plugins will be removed with the next /stop");
        } else {
            JobManager.get().println("Could not find any stubbed dependencies");
        }
    }
}
