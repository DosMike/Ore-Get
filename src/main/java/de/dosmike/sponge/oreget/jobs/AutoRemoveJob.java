package de.dosmike.sponge.oreget.jobs;

import de.dosmike.sponge.oreget.OreGet;
import de.dosmike.sponge.oreget.cache.ProjectContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

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
        int i = 0; int amount = OreGet.getPluginCache().getProjects().size();
        for (ProjectContainer container : OreGet.getPluginCache().getProjects()) {
            if (OreGet.getPluginCache().isStubbed(container))
                stubbed.add(container);
            i++;
            progress = (float)i/amount;
        }
        if (stubbed.size()>0) {
            stubbed.forEach(project -> OreGet.getPluginCache().markForRemoval(project, false));
            JobManager.get().println(Text.of(TextColors.YELLOW, "Found ", stubbed.size(), " stubbed plugins:"));
            JobManager.get().println(Text.of(
                    stubbed.stream()
                    .map(ProjectContainer::getPluginId)
                    .collect(Collectors.joining(", "))
            ));
            JobManager.get().println(Text.of(TextColors.RED, "The plugins will be removed with the next /stop"));
        } else {
            JobManager.get().println(Text.of("Could not find any stubbed dependencies"));
        }
    }
}