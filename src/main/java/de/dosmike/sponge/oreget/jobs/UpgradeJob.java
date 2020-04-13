package de.dosmike.sponge.oreget.jobs;

import de.dosmike.sponge.oreget.cache.PluginCache;
import de.dosmike.sponge.oreget.cache.ProjectContainer;
import de.dosmike.sponge.oreget.multiplatform.JobManager;
import de.dosmike.sponge.oreget.multiplatform.SharedInstances;
import de.dosmike.sponge.oreget.oreapi.RateLimiter;
import de.dosmike.sponge.oreget.oreapi.v2.OreDependency;
import de.dosmike.sponge.oreget.oreapi.v2.OreProject;
import de.dosmike.sponge.oreget.oreapi.v2.OreReviewState;
import de.dosmike.sponge.oreget.oreapi.v2.OreVersion;
import de.dosmike.sponge.oreget.utils.version.Version;
import de.dosmike.sponge.oreget.utils.version.VersionFilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UpgradeJob extends PluginJob {

    public UpgradeJob(boolean autoremove) {
        PluginCache.get().getProjects().forEach(project->{
            if (project.isAuto()) {
                manual.add(project.getPluginId());
            } else if (!PluginCache.get().isStubbed(project))
                pluginsToCheck.add(project.getPluginId());
        });
        removeStubbed = autoremove;
    }
    boolean removeStubbed;
    Set<String> manual = new HashSet<>();
    Set<String> pluginsToCheck = new HashSet<>();
    Set<String> pluginsUpToDate = new HashSet<>(); // ignore list for further cycles
    Set<String> squishedDependencies = new HashSet<String>();

    private void progressScan(int toDownloadsize, int toCheckSize, int checkIterator) {
        JobManager.get().println("Resolving dependencies... ["+(checkIterator+toDownloadsize)+"/"+(toCheckSize+toDownloadsize)+"]");
    }

    @Override
    ResolveResult resolveProjects() {
        ResolveResult resolveResult = new ResolveResult();
        resolveResult.pluginsRequestedManually.addAll(manual);

        while (!pluginsToCheck.isEmpty()) {
            progressScan(resolveResult.toDownload.size(), pluginsToCheck.size(), 0);
            setProgress( (float)(resolveResult.toDownload.size()) / (pluginsToCheck.size()+resolveResult.toDownload.size()) );
            setMessage("Fetching Projects");
            //fetch all required projects from ore
            Set<OreProject> remoteProjects = RateLimiter.waitForAll( pluginsToCheck.stream()
                    .map(missing -> SharedInstances.getOre().getRateLimiter().enqueue(() -> SharedInstances.getOre().getProject(missing)))
                    .collect(Collectors.toSet()))
                    .stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());

            //collect relevant dependencies
            Set<String> pluginsOnOre = new HashSet<>();
            int i = 0;
            for (OreProject project : remoteProjects) {
                if (project.getPromotedVersions().length > 0) {
                    VersionFilter.getLatestPromotedVersion(project)
                            .ifPresent(version -> {
                                pluginsOnOre.add(project.getPluginId());
                                if (project.isInstalled()) {
                                    //requires update?
                                    ProjectContainer installedData = PluginCache.get().findProject(project.getPluginId()).get();
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
                                        squishedDependencies.addAll(Arrays.stream(version.getDependencies()).map(OreDependency::getPluginId).collect(Collectors.toSet()));
                                    } else {
                                        pluginsUpToDate.add(project.getPluginId());
                                        squishedDependencies.addAll(installedData.getCachedVersion().isPresent()
                                                ? installedData.getCachedDependencies()
                                                : installedData.getActiveDependencies().keySet());
                                    }
                                } else {
                                    resolveResult.pluginsToInstall.add(project.getPluginId());
                                    resolveResult.toDownload.put(project, version);
                                    if (!version.getReviewState().equals(OreReviewState.REVIEWED)) {
                                        resolveResult.pluginsNotReviewed.add(project.getPluginId());
                                    }
                                    squishedDependencies.addAll(Arrays.stream(version.getDependencies()).map(OreDependency::getPluginId).collect(Collectors.toSet()));
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
        JobManager.get().println("Resolving dependencies... ["+(resolveResult.toDownload.size())+"/"+(resolveResult.toDownload.size())+"]");

        //collect plugins that can be removed
        if (removeStubbed) {
            resolveResult.pluginsToRemove.addAll(PluginCache.get().getProjects().stream()
                    .filter(container->!container.doDelete() && container.isAuto()) //collect all installed plugin ids, that were not installed manually
                    .map(ProjectContainer::getPluginId)
                    .filter(id->!squishedDependencies.contains(id)) //from all auto plugins remove current dependencies; remain with stubbed ones
                    .collect(Collectors.toSet()));
        }

        return resolveResult;
    }

}
