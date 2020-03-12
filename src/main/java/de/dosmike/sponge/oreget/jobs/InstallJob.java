package de.dosmike.sponge.oreget.jobs;

import de.dosmike.sponge.oreget.OreGetPlugin;
import de.dosmike.sponge.oreget.cache.ProjectContainer;
import de.dosmike.sponge.oreget.oreapi.RateLimiter;
import de.dosmike.sponge.oreget.oreapi.v2.OreDependency;
import de.dosmike.sponge.oreget.oreapi.v2.OreProject;
import de.dosmike.sponge.oreget.oreapi.v2.OreReviewState;
import de.dosmike.sponge.oreget.oreapi.v2.OreVersion;
import de.dosmike.sponge.oreget.utils.version.Version;
import de.dosmike.sponge.oreget.utils.version.VersionFilter;
import org.spongepowered.api.text.Text;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class InstallJob extends PluginJob {

    public InstallJob(boolean updateOnly, String... pluginIds) {
        manual.addAll(Arrays.asList(pluginIds));
        if (updateOnly) {
            for (String pluginId : pluginIds)
                if (OreGetPlugin.getPluginCache().findProject(pluginId).isPresent())
                    pluginsToCheck.add(pluginId);
        } else
            pluginsToCheck.addAll(manual);
    }
    Set<String> manual = new HashSet<>();
    Set<String> pluginsToCheck = new HashSet<>();
    Set<String> pluginsUpToDate = new HashSet<>(); // ignore list for further cycles

    private void progressScan(int toDownloadsize, int toCheckSize, int checkIterator) {
        JobManager.get().println(Text.of("Resolving dependencies... ["+(checkIterator+toDownloadsize)+"/"+(toCheckSize+toDownloadsize)+"]"));
    }

    @Override
    ResolveResult resolveProjects() {
        ResolveResult resolveResult = new ResolveResult();
        for (String id : manual) resolveResult.pluginsRequestedManually.add(id.toLowerCase());

        while (!pluginsToCheck.isEmpty()) {
            progressScan(resolveResult.toDownload.size(), pluginsToCheck.size(), 0);
            setProgress( (float)(resolveResult.toDownload.size()) / (pluginsToCheck.size()+resolveResult.toDownload.size()) );
            JobManager.get().println(Text.of("Resolving dependencies... ["+(resolveResult.toDownload.size())+"/"+(pluginsToCheck.size()+resolveResult.toDownload.size())+"]"));
            setMessage("Fetching Projects");
            //fetch all required projects from ore
            Set<OreProject> remoteProjects = RateLimiter.waitForAll( pluginsToCheck.stream()
                            .map(missing -> OreGetPlugin.getOre().getRateLimiter().enqueue(() -> OreGetPlugin.getOre().getProject(missing)))
                            .collect(Collectors.toSet()))
                    .stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toSet());

            //collect relevant dependencies
            Set<String> pluginsOnOre = new HashSet<>();
            int i=0;
            for (OreProject project : remoteProjects) {
                if (project.getPromotedVersions().length > 0) {
                    VersionFilter.getLatestStableVersion(project).ifPresent(version -> {
                            pluginsOnOre.add(project.getPluginId());
                            if (project.isInstalled()) {
                                //requires update?
                                ProjectContainer installedData = OreGetPlugin.getPluginCache().findProject(project.getPluginId()).get();
                                boolean canUpdate = false;
                                String localVersion = installedData.getCachedVersion().isPresent() ? installedData.getCachedVersion().get() :
                                        ( installedData.getInstalledVersion().isPresent() ? installedData.getInstalledVersion().get() :
                                                "N/A" );
                                String promotedVersion = version.getName();
                                try {
                                    canUpdate = new Version(promotedVersion).compareTo(new Version(localVersion)) > 0;
                                } catch (IllegalArgumentException e) { //some version is not a mave-like version
                                    //do string comparison
                                    canUpdate = !promotedVersion.equalsIgnoreCase(localVersion);
                                }
                                if (canUpdate) {
                                    resolveResult.pluginsToUpdate.add(project.getPluginId());
                                    resolveResult.toDownload.put(project, version);
                                    if (!version.getReviewState().equals(OreReviewState.REVIEWED)) {
                                        resolveResult.pluginsNotReviewed.add(project.getPluginId());
                                    }
                                } else pluginsUpToDate.add(project.getPluginId());
                            } else {
                                resolveResult.pluginsToInstall.add(project.getPluginId());
                                resolveResult.toDownload.put(project, version);
                                if (!version.getReviewState().equals(OreReviewState.REVIEWED)) {
                                    resolveResult.pluginsNotReviewed.add(project.getPluginId());
                                }
                            }
                        });
                }
                progressScan(resolveResult.toDownload.size(), pluginsToCheck.size(), ++i);
            }
            setMessage("Updating index");
            { //to notify the user that there are plugins required, but not available
                HashSet<String> notOnOre = new HashSet<>(pluginsToCheck);
                notOnOre.removeAll(pluginsOnOre);
                resolveResult.pluginsNotOnOre.addAll(notOnOre);
            }
            //update plugins list to check
            Set<String> newDeps = new HashSet<>();
            Set<String> toDownloadIds = resolveResult.toDownload.keySet().stream().map(OreProject::getPluginId).collect(Collectors.toSet());
            for (OreVersion version : resolveResult.toDownload.values()) {
                for (OreDependency dep : version.getDependencies()) {
                    if (!toDownloadIds.contains(dep.getPluginId())) //already marked for download
                        newDeps.add(dep.getPluginId());
                }
            }
            newDeps.remove("spongeapi"); //not retrievable through ore
            newDeps.removeAll(resolveResult.pluginsNotOnOre);
            newDeps.removeAll(resolveResult.pluginsToInstall);
            newDeps.removeAll(resolveResult.pluginsToUpdate);
            newDeps.removeAll(pluginsUpToDate);
            pluginsToCheck = newDeps;

            setProgress( (float)(resolveResult.toDownload.size()) / (pluginsToCheck.size()+resolveResult.toDownload.size()) );
        }
        setProgress(1f);
        JobManager.get().println(Text.of("Resolving dependencies... ["+(resolveResult.toDownload.size())+"/"+(resolveResult.toDownload.size())+"]"));

        return resolveResult;
    }
}
