package de.dosmike.sponge.oreget.jobs;

import de.dosmike.sponge.oreget.cache.PluginCache;
import de.dosmike.sponge.oreget.cache.ProjectContainer;
import de.dosmike.sponge.oreget.multiplatform.JobManager;
import de.dosmike.sponge.oreget.multiplatform.Logging;
import de.dosmike.sponge.oreget.multiplatform.PlatformProbe;
import de.dosmike.sponge.oreget.oreapi.v2.OreProject;
import de.dosmike.sponge.oreget.oreapi.v2.OreVersion;
import de.dosmike.sponge.oreget.utils.PluginDownloader;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class PluginJob implements AbstractJob {

    private String message=null;
    private float progress=0;
    protected void setMessage(String message) { this.message = message; }
    protected void setProgress(float progress) { this.progress = progress; }

    @Override
    public void run() {
        ResolveResult result = resolveProjects();

        if (result.pluginsNotOnOre.size()>0) {
            JobManager.get().println("The following plugins/mods are not available on Ore: ", String.join(", ", result.pluginsNotOnOre));
        }
        if (result.toDownload.isEmpty() && result.pluginsToRemove.isEmpty()) {
            JobManager.get().println("Seems like everything's up to date.");
            return;
        }
        progress = 0f;

        // Print big notification block
        message = "Downloading Plugins";
        if (!result.toDownload.isEmpty()) { //ignore intellij, this can be empty
            JobManager.get().println(Logging.Color.YELLOW, "The following projects dependencies will be installed: ");
            List<String> tmpList = new LinkedList<>(result.getPluginsToDownload());
            tmpList.removeAll(result.pluginsRequestedManually);
            JobManager.get().println(String.join(", ", tmpList));
            JobManager.get().println(Logging.Color.YELLOW, "The following NEW projects will be installed: ");
            JobManager.get().println(String.join(", ", result.pluginsToInstall));
        }
        if (!result.pluginsToRemove.isEmpty()) {
            JobManager.get().println(Logging.Color.YELLOW, "The following plugins will be removed:");
            JobManager.get().println(String.join(", ", result.pluginsToRemove));
        }
        if (result.pluginsNotReviewed.size()>0) {
            JobManager.get().println(Logging.Color.YELLOW, String.format("%d to install, %d to upgrade, %d to remove", result.pluginsToInstall.size(), result.pluginsToUpdate.size(), result.pluginsToRemove.size()));
            JobManager.get().println(Logging.Color.GOLD, "The following Plugins have not been reviewed by the Ore moderation staff yet and may not be safe to use:");
            JobManager.get().println(String.join(", ", result.pluginsNotReviewed));
            JobManager.get().println(Logging.Color.GOLD, "The Sponge teams as well as the OreGet Developer disclaim all responsibility for any harm to your server or system should you choose not to heed this warning");
        }
        JobManager.get().println("Type ", Logging.Color.AQUA, "/ore-get confirm", Logging.Color.RESET, " to continue");
        if (!confirm()) return; // require confirmation

        // Proceed to install
        float i = 0, j = result.toDownload.size();
        float progressPerPlugin = 1/j;
        for (Map.Entry<OreProject, OreVersion> entry : result.toDownload.entrySet()) {
            PluginDownloader pdl = new PluginDownloader(entry.getKey(), entry.getValue());
            pdl.start();
            long previous=0;
            while (!pdl.isDone()) { //print progress every 10% or 1 second
                if (System.currentTimeMillis()-previous > 1000) {
                    previous = System.currentTimeMillis();
                    JobManager.get().println(Logging.Color.GRAY, "Downloading ", Logging.Color.WHITE, entry.getKey().getName(), ": ", entry.getValue().getName(), Logging.Color.GRAY,"... ", (int)(pdl.getProgress()*100), "%");
                }
                progress = i/j + pdl.getProgress()*progressPerPlugin;
                Thread.yield();
            }
            JobManager.get().println(Logging.Color.GRAY, "Downloading ", Logging.Color.WHITE, entry.getKey().getName(), ": ", entry.getValue().getName(), Logging.Color.GRAY,"... 100%");
            if (!pdl.target().isPresent()) {
                JobManager.get().println(Logging.Color.RED, "Could not download plugin "+entry.getKey().getName()+" - Job cancelled!");
                return;
            }
            ProjectContainer container = PluginCache.get().findProject(entry.getKey().getPluginId()).orElseGet(()->{
                ProjectContainer newContainer = new ProjectContainer(entry.getKey().getPluginId().toLowerCase());
                newContainer.markAuto(!result.pluginsRequestedManually.contains(entry.getKey().getPluginId())); //if it's not the requested, mark auto
                return newContainer;
            });
            container.load(entry.getKey(), entry.getValue(), pdl.target().get().getFileName().toString());
            PluginCache.get().registerPlugin(container);
            i++;
        }
        //remove plugins
        for (String entry : result.pluginsToRemove) {
            PluginCache.get().markForRemoval(entry, false);
        }
        if (PlatformProbe.isSponge())
            JobManager.get().println("DONE ", Logging.Color.RED, "To complete the installation, please restart the server");
        else
            JobManager.get().println("DONE ", Logging.Color.RED, "Run the post-script to complete installation");
    }

    abstract ResolveResult resolveProjects();

    @Override public float getProgress() { return progress*100; }
    @Override public String getMessage() { return message; }
}
