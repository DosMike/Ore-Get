package de.dosmike.sponge.oreget.jobs;

import de.dosmike.sponge.oreget.OreGetPlugin;
import de.dosmike.sponge.oreget.cache.ProjectContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

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
            Optional<ProjectContainer> project = OreGetPlugin.getPluginCache().findProject(id);
            if (project.isPresent() && project.get().getInstalledVersion().isPresent() || project.get().getCachedVersion().isPresent()) {
                if (project.get().doDelete()) {
                    OreGetPlugin.getPluginCache().registerPlugin(project.get());
                    restore.add(id);
                } else {
                    OreGetPlugin.getPluginCache().markForRemoval(project.get(), doPurge);
                    remove.add(id);
                }
            } else {
                invalid.add(id);
            }
            i++;
            progress = (float)i/manual.size();
        }
        if (!remove.isEmpty()) {
            JobManager.get().println(Text.of(TextColors.YELLOW, "The following plugins will be removed with the next /stop:"));
            JobManager.get().println(Text.of(String.join(", ", remove)));
        }
        if (!restore.isEmpty()) {
            JobManager.get().println(Text.of(TextColors.YELLOW, "The following plugin will no longer be removed:"));
            JobManager.get().println(Text.of(String.join(", ", restore)));
        }
        if (!invalid.isEmpty())
            JobManager.get().println(Text.of(TextColors.RED, "The following plugin were not recognized:"));
        JobManager.get().println(Text.of(String.join(", ", invalid)));
    }

}
