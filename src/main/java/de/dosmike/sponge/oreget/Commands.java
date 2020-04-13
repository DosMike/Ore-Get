package de.dosmike.sponge.oreget;

import de.dosmike.sponge.oreget.cache.PluginCache;
import de.dosmike.sponge.oreget.cache.ProjectContainer;
import de.dosmike.sponge.oreget.jobs.*;
import de.dosmike.sponge.oreget.multiplatform.JobManager;
import de.dosmike.sponge.oreget.multiplatform.Logging;
import de.dosmike.sponge.oreget.multiplatform.PlatformProbe;
import de.dosmike.sponge.oreget.multiplatform.SharedInstances;
import de.dosmike.sponge.oreget.oreapi.v2.OreDependency;
import de.dosmike.sponge.oreget.oreapi.v2.OrePartialVersion;
import de.dosmike.sponge.oreget.oreapi.v2.OreProject;
import de.dosmike.sponge.oreget.oreapi.v2.OreVersion;
import de.dosmike.sponge.oreget.utils.version.Version;
import de.dosmike.sponge.oreget.utils.version.VersionFilter;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class Commands {

    public static Future<?> search(@Nullable Object sender, String query) {
        return SharedInstances.getAsyncExecutor().submit(()->{ // Do networking async
            List<OreProject> projects = SharedInstances.getOre().waitFor(()->SharedInstances.getOre().projectSearch(query, null))
                    .map(oreProjectOreResultList -> Arrays.asList(oreProjectOreResultList.getResult()))
                    .orElseGet(LinkedList::new);
            if (projects.isEmpty()) {
                Logging.log(sender, Logging.Color.RED, "There were no results for " + query);
                return;
            }
            List<Object> elements = new LinkedList<>();
            projects.forEach(project -> {
                Logging.MessageBuilder builder = Logging.builder();
                        builder.append(new Logging.Clickable(
                                "/oreget show " + project.getPluginId(),
                                Logging.Color.DARK_GREEN, project.getPluginId(), Logging.Color.RESET)
                                .withTooltip("Click for more info"))
                        .append("/" + project.getName() + " ");
                Optional<OrePartialVersion> promotedVersion = VersionFilter.getLatestPromotedVersionPartial(project);
                if (promotedVersion.isPresent()) {
                    builder.append(promotedVersion.get().getVersion());
                } else {
                    builder.append("N/A");
                }
                if (project.isInstalled()) {
                    builder.append(" [installed]");
                }
                elements.add(builder.toPlatform());
                elements.add(Logging.builder().append("  " + project.getCategory().name().toLowerCase() + ", " + project.getViews() + " views, " + project.getDownloads() + " dls, " + project.getStars() + " stars").toPlatform());
            });
            Logging.logLines(sender, elements);
        });
    }

    public static Future<?> show(@Nullable Object sender, String pluginId) {
        return SharedInstances.getAsyncExecutor().submit(()->{ //Do networking async
            Optional<ProjectContainer> localData = PluginCache.get().findProject(pluginId);
            Optional<OreProject> remoteData = SharedInstances.getOre().waitFor(()->SharedInstances.getOre().getProject(pluginId));
            if (remoteData.isPresent()) {
                Optional<OreVersion> versionData = VersionFilter.getLatestPromotedVersion(remoteData.get());
                String pv = versionData.map(OreVersion::getName).orElse("N/A");

                DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                printKV(sender, "Plugin ID", remoteData.get().getPluginId());
                printKV(sender, "Name", remoteData.get().getName());
                printKV(sender, "Create At", format.format(new Date(remoteData.get().getCreatedAt())));
                printKV(sender, "Last Update", remoteData.get().getLastUpdate()>0?format.format(new Date(remoteData.get().getLastUpdate())):"never");
                printKV(sender, "Stats", String.format("%d views, %d dls, %d stars", remoteData.get().getViews(), remoteData.get().getDownloads(), remoteData.get().getStars()));
                printKV(sender, "Category", remoteData.get().getCategory().name());
                printKV(sender, "Promoted Version", pv);
                if (versionData.isPresent()) {
                    printKV(sender, "  Created At", format.format(new Date(versionData.get().getCreatedAt())));
                    long sz = versionData.get().getFileSize();
                    String[] units = { "kiB", "MiB", "GiB" };
                    int unit=0;
                    for (;sz>=1024 && unit<units.length;unit++,sz/=1024) {/*empty*/}
                    printKV(sender, "  Status", versionData.get().getDownloads()+" dls, "+versionData.get().getReviewState().toString());
                    printKV(sender, "  Download Size", String.format("%d %s", sz, units[unit]));
                    printKV(sender, "  File MD5", versionData.get().getFileMD5());
                    Logging.MessageBuilder mb = Logging.builder(); boolean first = true;
                    for (OreDependency dep : versionData.get().getDependencies()) {
                        if (first) first = false;
                        else mb.append(", ");
                        mb.append(new Logging.Clickable("/oreget show " + dep.getPluginId(),
                                Logging.Color.AQUA, Logging.Style.UNDERLINE, dep.getPluginId(), Logging.Color.RESET)
                                .withTooltip("Version: "+dep.getVersion()));
                    }
                    printKV(sender, "  Dependencies", mb.toPlatform());
                    printKV(sender, "  Tags", Arrays.stream(versionData.get().getTags()).map(t->t.getName()+ "("+t.getData()+")").collect(Collectors.joining(", ")));
                    printKV(sender, "  Authors", versionData.get().getAuthor());
                    String desc = versionData.get().getDescription();
                    if (desc.length()>500) desc = desc.substring(0,500)+" [...]";
                    printKV(sender, "  Changes", "\n"+desc);
                }
                if (localData.isPresent()) {
                    printKV(sender, "Installed Version", localData.get().getInstalledVersion().orElse("Not Active"));
                    Logging.MessageBuilder mb = Logging.builder(); boolean first = true;
                    for (Map.Entry<String,String> dep : localData.get().getActiveDependencies().entrySet()) {
                        if (first) first = false;
                        else mb.append(", ");
                        mb.append(new Logging.Clickable("/oreget show " + dep.getKey(),
                                Logging.Color.AQUA, Logging.Style.UNDERLINE, dep.getKey(), Logging.Color.RESET)
                                .withTooltip("Version: "+dep.getValue()));
                    }
                    printKV(sender, "  Dependencies", mb.toPlatform());
                    printKV(sender, "  Authors", String.join(", ", localData.get().getAuthors()));

                    printKV(sender, "Downloaded Version", localData.get().getCachedVersion().orElse("No Update"));
                    printKV(sender, "Manual install", localData.get().isAuto() ? "no" : "yes");
                    printKV(sender, "Remove on /stop", localData.get().doDelete() ? (localData.get().doPurge() ? "all" : "jar-file") : "no");
                }
                String surl = "https://ore.spongepowered.org/"+remoteData.get().getNamespace().getOwner()+"/"+remoteData.get().getNamespace().getSlug();
                printKV(sender, "Ore Page", Logging.builder().append(new Logging.Clickable(surl, surl)).toPlatform());
                surl = remoteData.get().getUrlIssues();
                if (surl != null && !surl.isEmpty()) {
                    printKV(sender, "Issue tracker", Logging.builder().append(new Logging.Clickable(surl, surl)).toPlatform());
                }
                printKV(sender, "Description", remoteData.get().getDescription());
                if (!localData.isPresent() && PlatformProbe.isSponge()) {
                    Logging.builder().append("Click ").append(new Logging.Clickable(
                            "/oreget install "+pluginId),
                            Logging.Color.AQUA, Logging.Style.UNDERLINE, "here", Logging.Color.RESET
                            ).append(" to install").log(sender);
                } else {
                    boolean canUpdate = false;
                    if (!pv.equalsIgnoreCase("N/A")) //if we have a promoted version
                        try {
                            canUpdate = new Version(pv).compareTo(new Version(localData.get().getInstalledVersion().orElse("N/A"))) > 0;
                        } catch (IllegalArgumentException e) { //some version is not a mave-like version
                            //do string comparison
                            canUpdate = !pv.equalsIgnoreCase(localData.get().getInstalledVersion().orElse("N/A"));
                        }
                    if (localData.get().doDelete()) {
                        Logging.log(sender, Logging.Color.YELLOW, "Requires Restart to remove");
                    } else if (!localData.get().getInstalledVersion().isPresent()) {
                        Logging.log(sender, Logging.Color.YELLOW, "Requires Restart to load");
                    } else if (localData.get().getCachedVersion().isPresent()) {
                        Logging.log(sender, Logging.Color.YELLOW, "Requires Restart to update");
                    } else if (canUpdate) {
                        Logging.log(sender, Logging.Color.GREEN, "Looks like this plugin can be upgraded!");
                    }
                }
            } else if (localData.isPresent()) { //only local data is available
                printKV(sender, "Plugin ID", localData.get().getPluginId());
                printKV(sender, "Name", localData.get().getName());
                printKV(sender, "Installed Version", localData.get().getInstalledVersion().orElse("Not Active"));
                Logging.MessageBuilder mb = Logging.builder(); boolean first = true;
                for (Map.Entry<String,String> dep : localData.get().getActiveDependencies().entrySet()) {
                    if (first) first = false;
                    else mb.append(", ");
                    mb.append(new Logging.Clickable("oreget show " + dep.getKey(),
                            dep.getKey()).withTooltip("Version: "+dep.getValue()));
                }
                printKV(sender, "  Dependencies", mb.toPlatform());
                printKV(sender, "  Authors", String.join(", ", localData.get().getAuthors()));

                printKV(sender, "Downloaded Version", localData.get().getCachedVersion().orElse("No Update"));
                printKV(sender, "Manual install", localData.get().isAuto()?"no":"yes");
                printKV(sender, "Remove on /stop", localData.get().doDelete()?(localData.get().doPurge()?"all":"jar-file"):"no");
                printKV(sender, "Description", localData.map(ProjectContainer::getDescription).orElse("empty"));
            } else {
                Logging.log(sender, Logging.Color.RED, "This plugin does not seem to exist");
            }
        });
    }

    public static Future<?> upgrade(@Nullable Object sender) throws JobManagerBusyException {
        if (!JobManager.get().runJob(sender, new UpgradeJob(false))) {
            throw new JobManagerBusyException("There's currently another task running");
        }
        return CompletableFuture.runAsync(()->{while(!JobManager.get().isIdle())Thread.yield();},SharedInstances.getAsyncExecutor());
    }
    public static Future<?> fullUpgrade(@Nullable Object sender) throws JobManagerBusyException {
        if (!JobManager.get().runJob(sender, new UpgradeJob(true))) {
            throw new JobManagerBusyException("There's currently another task running");
        }
        return CompletableFuture.runAsync(()->{while(!JobManager.get().isIdle())Thread.yield();},SharedInstances.getAsyncExecutor());
    }

    public static Future<?> install(@Nullable Object sender, boolean onlyUpgrade, String... artifactSpecs) throws JobManagerBusyException {
        if (!JobManager.get().runJob(sender, new InstallJob(onlyUpgrade, artifactSpecs))) {
            throw new JobManagerBusyException("There's currently another task running");
        }
        return CompletableFuture.runAsync(()->{while(!JobManager.get().isIdle())Thread.yield();},SharedInstances.getAsyncExecutor());
    }

    public static Future<?> remove(@Nullable Object sender, boolean purge, String... pluginIds) throws JobManagerBusyException {
        if (!JobManager.get().runJob(sender, new RemoveJob(purge, pluginIds))) {
            throw new JobManagerBusyException("There's currently another task running");
        }
        return CompletableFuture.runAsync(()->{while(!JobManager.get().isIdle())Thread.yield();},SharedInstances.getAsyncExecutor());
    }
    public static Future<?> autoremove(@Nullable Object sender) throws JobManagerBusyException {
        if (!JobManager.get().runJob(sender, new AutoRemoveJob())) {
            throw new JobManagerBusyException("There's currently another task running");
        }
        return CompletableFuture.runAsync(()->{while(!JobManager.get().isIdle())Thread.yield();},SharedInstances.getAsyncExecutor());
    }
    /** @throws IllegalArgumentException if the plugin was not found (installed or cached) */
    public static void mark(@Nullable Object sender, String pluginId, boolean asManagedDependency) throws IllegalArgumentException {
        Optional<ProjectContainer> project = PluginCache.get().findProject(pluginId);
        if (project.isPresent() && project.get().getInstalledVersion().isPresent() || project.get().getCachedVersion().isPresent()) {
            project.get().markAuto(asManagedDependency);
            Logging.log(sender, asManagedDependency?"The plugin is now marked as auto installed dependency":"The plugin is now marked as manually installed");
        } else
            throw new IllegalArgumentException("There is no plugin with id "+pluginId+" installed");
    }

    public static void confirm(@Nullable Object sender) {
        JobManager.get().addViewer(sender);
        JobManager.get().resumeJob(true);
    }
    public static void reject(@Nullable Object sender) {
        JobManager.get().addViewer(sender);
        Set<Object> viewer = new HashSet<>(JobManager.get().getViewers());
        viewer.add(null);
        JobManager.get().resumeJob(false);
        viewer.forEach(v-> Logging.log(v, Logging.Color.RED, "The job was cancelled."));
    }

    public static Future<?> list(@Nullable Object sender) {
        return SharedInstances.getAsyncExecutor().submit(()->{
            SortedMap<String, String> sorted = new TreeMap<>();
            PluginCache.get().getProjects().forEach(project->{
                if (project.getSource().isPresent())
                    sorted.put(
                            project.getPluginId(), //sort by
                            project.getName() + " " + project.getInstalledVersion().orElse("N/A")); //more info
            });
            sorted.forEach((id,more)->{
                Logging.log(sender, Logging.Color.DARK_GREEN, id, Logging.Color.RESET, "/", more);
            });
        });
    }

    private static void printKV(@Nullable Object receiver, Object k, Object v) {
        Logging.log(receiver, Logging.Color.WHITE, k, ": ", Logging.Color.GRAY, v);
    }

}
