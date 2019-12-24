package de.dosmike.sponge.oreget;

import de.dosmike.sponge.oreget.cache.ProjectContainer;
import de.dosmike.sponge.oreget.jobs.*;
import de.dosmike.sponge.oreget.oreapi.v2.OreDependency;
import de.dosmike.sponge.oreget.oreapi.v2.OreProject;
import de.dosmike.sponge.oreget.oreapi.v2.OreVersion;
import de.dosmike.sponge.oreget.utils.version.Version;
import de.dosmike.sponge.oreget.utils.version.VersionFilter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.plugin.meta.PluginDependency;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class CommandRegister {

    static CommandSpec subcmdSearch() {
        return CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE_SEARCH.getId())
                .description(Text.of("Search plugins on ore"))
                .arguments(GenericArguments.remainingJoinedStrings(Text.of("query")))
                .executor((src, args) -> {
                    String query = args.<String>getOne("query").get();
                    OreGet.async().execute(()->{ // Do networking async
                        List<OreProject> projects = OreGet.getOre().waitFor(()->OreGet.getOre().projectSearch(query, null))
                                .map(oreProjectOreResultList -> Arrays.asList(oreProjectOreResultList.getResult()))
                                .orElseGet(LinkedList::new);
                        if (projects.isEmpty()) {
                            src.sendMessage(Text.of(TextColors.RED, "There were no results for " + query));
                            return;
                        }
                        List<Text> elements = new LinkedList<>();
                        projects.forEach(project -> {
                            Text.Builder builder = Text.builder()
                                    .append(Text.builder(project.getPluginId())
                                            .color(TextColors.DARK_GREEN)
                                            .onHover(TextActions.showText(Text.of("Click to show more info")))
                                            .onClick(TextActions.runCommand("/oreget show " + project.getPluginId()))
                                            .build())
                                    .append(Text.of("/" + project.getName() + " "));
//                            if (project.getPromotedVersions().length > 0) {
//                                builder.append(Text.of(project.getPromotedVersions()[0].getVersion()));
                            Optional<OreVersion> promotedVersion = VersionFilter.getLatestStableVersion(project);
                            if (promotedVersion.isPresent()) {
                                builder.append(Text.of(promotedVersion.get().getName()));
                            } else {
                                builder.append(Text.of("N/A"));
                            }
                            if (project.isInstalled()) {
                                builder.append(Text.of(" [installed]"));
                            }
                            elements.add(builder.build());
                            elements.add(Text.of("  " + project.getCategory().name().toLowerCase() + ", " + project.getViews() + " views, " + project.getDownloads() + " dls, " + project.getStars() + " stars"));
                            elements.add(Text.EMPTY);
                        });
                        PaginationList.builder().title(Text.of("Ore Search"))
                                .contents(elements)
                                .footer(Text.of("/ore search " + query))
                                .padding(Text.of("="))
                                .linesPerPage(15)
                                .sendTo(src);
                    });
                    return CommandResult.success();
                })
                .build();
    }

    static CommandSpec subcmdShow() {
        return CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE_SHOW.getId())
                .description(Text.of("Show all available information for the given plugin"))
                .arguments(GenericArguments.string(Text.of("pluginId")))
                .executor((src, args) -> {
                    String pluginId = args.<String>getOne("pluginId").get();
                    OreGet.async().execute(()->{ //Do networking async
                        Optional<ProjectContainer> localData = OreGet.getPluginCache().findProject(pluginId);
                        Optional<OreProject> remoteData = OreGet.getOre().waitFor(()->OreGet.getOre().getProject(pluginId));
                        if (remoteData.isPresent()) {
//                            String pv = remoteData.get().getPromotedVersions().length>0?
//                                    remoteData.get().getPromotedVersions()[0].getVersion():"none";
                            Optional<OreVersion> versionData = Optional.empty();
//                            if (!pv.equals("none"))
//                                versionData = OreGet.getOre().waitFor(()->OreGet.getOre().getVersion(remoteData.get().getPluginId(), pv));
                                versionData = VersionFilter.getLatestStableVersion(remoteData.get());
                            String pv = versionData.map(OreVersion::getName).orElse("N/A");

                            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, src.getLocale());
                            printKV(src, "Plugin ID", remoteData.get().getPluginId());
                            printKV(src, "Name", remoteData.get().getName());
                            printKV(src, "Create At", format.format(new Date(remoteData.get().getCreatedAt())));
                            printKV(src, "Last Update", remoteData.get().getLastUpdate()>0?format.format(new Date(remoteData.get().getLastUpdate())):"never");
                            printKV(src, "Stats", String.format("%d views, %d dls, %d stars", remoteData.get().getViews(), remoteData.get().getDownloads(), remoteData.get().getStars()));
                            printKV(src, "Category", remoteData.get().getCategory().name());
                            printKV(src, "Promoted Version", pv);
                            if (versionData.isPresent()) {
                                printKV(src, "  Created At", format.format(new Date(versionData.get().getCreatedAt())));
                                long sz = versionData.get().getFileSize();
                                String[] units = { "kiB", "MiB", "GiB" };
                                int unit=0;
                                for (;sz>=1024 && unit<units.length;unit++,sz/=1024) {/*empty*/}
                                printKV(src, "  Status", versionData.get().getDownloads()+" dls, "+versionData.get().getReviewState().toString());
                                printKV(src, "  Download Size", String.format("%d %s", sz, units[unit]));
                                printKV(src, "  File MD5", versionData.get().getFileMD5());
                                Text.Builder tb = Text.builder(); boolean first = true;
                                for (OreDependency dep : versionData.get().getDependencies()) {
                                    if (first) first = false;
                                    else tb.append(Text.of(", "));
                                    tb.append(Text.builder(dep.getPluginId())
                                            .onHover(TextActions.showText(Text.of("Version: " + dep.getVersion())))
                                            .onClick(TextActions.runCommand("/oreget show " + dep.getPluginId()))
                                            .color(TextColors.AQUA)
                                            .style(TextStyles.UNDERLINE)
                                            .build());
                                }
                                printKV(src, "  Dependencies", tb.build());
                                printKV(src, "  Tags", Arrays.stream(versionData.get().getTags()).map(t->t.getName()+ "("+t.getData()+")").collect(Collectors.joining(", ")));
                                printKV(src, "  Authors", versionData.get().getAuthor());
                                String desc = versionData.get().getDescription();
                                if (desc.length()>500) desc = desc.substring(0,500)+" [...]";
                                printKV(src, "  Changes", "\n"+desc);
                            }
                            if (localData.isPresent()) {
                                printKV(src, "Installed Version", localData.get().getInstalledVersion().orElse("Not Active"));
                                Optional<PluginContainer> spongeData = localData.get().getPluginContainer();
                                if (spongeData.isPresent()) {
                                    Text.Builder tb = Text.builder(); boolean first = true;
                                    for (PluginDependency dep : spongeData.get().getDependencies()) {
                                        if (first) first = false;
                                        else tb.append(Text.of(", "));
                                        tb.append(Text.builder(dep.getId())
                                                .onHover(TextActions.showText(Text.of("Version: " + dep.getVersion())))
                                                .onClick(TextActions.runCommand("/oreget show " + dep.getId()))
                                                .color(TextColors.AQUA)
                                                .style(TextStyles.UNDERLINE)
                                                .build());
                                    }
                                    printKV(src, "  Dependencies", tb.build());
                                    printKV(src, "  Authors", String.join(", ", spongeData.get().getAuthors()));
                                }
                                printKV(src, "Downloaded Version", localData.get().getCachedVersion().orElse("No Update"));
                                printKV(src, "Manual install", localData.get().isAuto() ? "no" : "yes");
                                printKV(src, "Remove on /stop", localData.get().doDelete() ? (localData.get().doPurge() ? "all" : "jar-file") : "no");
                            }
                            String surl = "https://ore.spongepowered.org/"+remoteData.get().getNamespace().getOwner()+"/"+remoteData.get().getNamespace().getSlug();
                            URL url = null; try { url = new URL(surl); } catch (MalformedURLException ignore) {}
                            printKV(src, "Ore Page", Text.builder(surl).onClick(TextActions.openUrl(url)).build());
                            surl = remoteData.get().getUrlIssues();
                            if (surl != null && !surl.isEmpty()) {
                                url = null;
                                try { url = new URL(surl); } catch (MalformedURLException ignore) {}
                                printKV(src, "Issue tracker", Text.builder(surl).onClick(TextActions.openUrl(url)).build());
                            }
                            printKV(src, "Description", remoteData.get().getDescription());
                            if (!localData.isPresent()) {
                                src.sendMessage(Text.of("Click ", Text.builder("here")
                                        .onClick(TextActions.runCommand("/oreget install "+pluginId))
                                        .color(TextColors.AQUA)
                                        .style(TextStyles.UNDERLINE)
                                        .build(), " to install"));
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
                                    src.sendMessage(Text.of(TextColors.YELLOW, "Requires Restart to remove"));
                                } else if (!localData.get().getInstalledVersion().isPresent()) {
                                    src.sendMessage(Text.of(TextColors.YELLOW, "Requires Restart to load"));
                                } else if (localData.get().getCachedVersion().isPresent()) {
                                    src.sendMessage(Text.of(TextColors.YELLOW, "Requires Restart to update"));
                                } else if (canUpdate) {
                                    src.sendMessage(Text.of(TextColors.GREEN, "Looks like this plugin can be upgraded!"));
                                }
                            }
                        } else if (localData.isPresent()) { //only local data is available
                            Optional<PluginContainer> spongeData = localData.get().getPluginContainer();
                            printKV(src, "Plugin ID", localData.get().getPluginId());
                            spongeData.ifPresent(pluginContainer -> printKV(src, "Name", pluginContainer.getName()));
                            printKV(src, "Installed Version", localData.get().getInstalledVersion().orElse("Not Active"));
                            if (spongeData.isPresent()) {
                                Text.Builder tb = Text.builder(); boolean first = true;
                                for (PluginDependency dep : spongeData.get().getDependencies()) {
                                    if (first) first = false;
                                    else tb.append(Text.of(", "));
                                    tb.append(Text.builder(dep.getId())
                                            .onHover(TextActions.showText(Text.of("Version: " + dep.getVersion())))
                                            .onClick(TextActions.runCommand("oreget show " + dep.getId()))
                                            .build());
                                }
                                printKV(src, "  Dependencies", tb.build());
                                printKV(src, "  Authors", String.join(", ", spongeData.get().getAuthors()));
                            }
                            printKV(src, "Downloaded Version", localData.get().getCachedVersion().orElse("No Update"));
                            printKV(src, "Manual install", localData.get().isAuto()?"no":"yes");
                            printKV(src, "Remove on /stop", localData.get().doDelete()?(localData.get().doPurge()?"all":"jar-file"):"no");
                            printKV(src, "Description", spongeData.flatMap(PluginContainer::getDescription).orElse("empty"));
                        } else {
                            src.sendMessage(Text.of(TextColors.RED, "This plugin does not seem to exist"));
                        }
                    });
                    return CommandResult.success();
                })
                .build();
    }

    static CommandSpec subcmdUpgrade() {
        return CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE_UPGRADE.getId())
                .description(Text.of("Install the latest promoted version for each plugin, if required new dependencies are installed"))
                .arguments(GenericArguments.none())
                .executor((src, args) -> {
                    if (!JobManager.get().runJob(new UpgradeJob(false))) {
                        JobManager.get().notifyMessageReceiverAdd(src);
                        throw new CommandException(Text.of(TextColors.RED, "There's currently another task running:"));
                    } else {
                        JobManager.get().notifyMessageReceiverAdd(src);
                        return CommandResult.success();
                    }
                })
                .build();
    }

    static CommandSpec subcmdFullUpgrade() {
        return CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE_FULL_UPGRADE.getId())
                .description(Text.of("Like upgrade, but can also remove plugins (like autoremove)"))
                .arguments(GenericArguments.none())
                .executor((src, args) -> {
                    if (!JobManager.get().runJob(new UpgradeJob(true))) {
                        JobManager.get().notifyMessageReceiverAdd(src);
                        throw new CommandException(Text.of(TextColors.RED, "There's currently another task running:"));
                    } else {
                        JobManager.get().notifyMessageReceiverAdd(src);
                        return CommandResult.success();
                    }
                })
                .build();
    }

    static CommandSpec subcmdInstall() {
        return CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE_INSTALL.getId())
                .description(Text.of("Install the specified plugin"))
                .arguments(GenericArguments.flags().flag("-only-upgrade").buildWith(GenericArguments.allOf(GenericArguments.string(Text.of("pluginId")))))
                .executor((src, args) -> {
                    Collection<String> pluginId = args.<String>getAll("pluginId");
                    boolean onlyUpgrade = args.hasAny("only-upgrade");
                    src.sendMessage(Text.of("Meant to ", onlyUpgrade?"only upgrade":"install"));
                    if (!JobManager.get().runJob(new InstallJob(onlyUpgrade, pluginId.toArray(new String[0])))) {
                        JobManager.get().notifyMessageReceiverAdd(src);
                        throw new CommandException(Text.of(TextColors.RED, "There's currently another task running:"));
                    } else {
                        JobManager.get().notifyMessageReceiverAdd(src);
                        return CommandResult.success();
                    }
                })
                .build();
    }

    static CommandSpec subcmdRemove() {
        return CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE_REMOVE.getId())
                .description(Text.of("Uninstall the specified plugin"))
                .arguments(GenericArguments.allOf(GenericArguments.string(Text.of("pluginId"))))
                .executor((src, args) -> {
                    Collection<String> pluginId = args.<String>getAll("pluginId");
                    if (!JobManager.get().runJob(new RemoveJob(false, pluginId.toArray(new String[0])))) {
                        JobManager.get().notifyMessageReceiverAdd(src);
                        throw new CommandException(Text.of(TextColors.RED, "There's currently another task running:"));
                    } else {
                        JobManager.get().notifyMessageReceiverAdd(src);
                        return CommandResult.success();
                    }
                })
                .build();
    }

    static CommandSpec subcmdPurge() {
        return CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE_PURGE.getId())
                .description(Text.of("Like uninstall, but will also try to delete plugin configurations"))
                .arguments(GenericArguments.allOf(GenericArguments.string(Text.of("pluginId"))))
                .executor((src, args) -> {
                    Collection<String> pluginId = args.<String>getAll("pluginId");
                    if (!JobManager.get().runJob(new RemoveJob(true, pluginId.toArray(new String[0])))) {
                        JobManager.get().notifyMessageReceiverAdd(src);
                        throw new CommandException(Text.of(TextColors.RED, "There's currently another task running:"));
                    } else {
                        JobManager.get().notifyMessageReceiverAdd(src);
                        return CommandResult.success();
                    }
                })
                .build();
    }

    static CommandSpec subcmdAutoremove() {
        return CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE_AUTOREMOVE.getId())
                .description(Text.of("Uninstall all dependencies that are no longer necessary"))
                .arguments(GenericArguments.none())
                .executor((src, args) -> {
                    if (!JobManager.get().runJob(new AutoRemoveJob())) {
                        JobManager.get().notifyMessageReceiverAdd(src);
                        throw new CommandException(Text.of(TextColors.RED, "There's currently another task running:"));
                    } else {
                        JobManager.get().notifyMessageReceiverAdd(src);
                        return CommandResult.success();
                    }
                })
                .build();
    }

    static CommandSpec subcmdMark() {
        return CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE_MARK.getId())
                .description(Text.of("Mark this plugin as automatically installed; autoremove can now delete this plugin"))
                .arguments(GenericArguments.string(Text.of("pluginId")))
                .executor((src, args) -> {
                    String pluginId = args.<String>getOne("pluginId").get();
                    Optional<ProjectContainer> project = OreGet.getPluginCache().findProject(pluginId);
                    if (project.isPresent() && project.get().getInstalledVersion().isPresent() || project.get().getCachedVersion().isPresent()) {
                        project.get().markAuto(true);
                        src.sendMessage(Text.of("The plugin is now marked as auto installed dependency"));
                    } else
                        throw new CommandException(Text.of(TextColors.RED, "There is no plugin with id "+pluginId+" installed"));
                    return CommandResult.success();
                })
                .build();
    }

    static CommandSpec subcmdUnmark() {
        return CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE_MARK.getId())
                .description(Text.of("Some jobs require additional confirmation, this will continue such jobs"))
                .arguments(GenericArguments.string(Text.of("pluginId")))
                .executor((src, args) -> {
                    String pluginId = args.<String>getOne("pluginId").get();
                    Optional<ProjectContainer> project = OreGet.getPluginCache().findProject(pluginId);
                    if (project.isPresent() && project.get().getInstalledVersion().isPresent() || project.get().getCachedVersion().isPresent()) {
                        project.get().markAuto(false);
                        src.sendMessage(Text.of("The plugin is now marked as manually installed"));
                    } else
                        throw new CommandException(Text.of(TextColors.RED, "There is no plugin with id "+pluginId+" installed"));
                    return CommandResult.success();
                })
                .build();
    }

    static CommandSpec subcmdConfirm() {
        return CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE_CONFIRM.getId())
                .description(Text.of("Confirm OreGet job when requested"))
                .arguments(GenericArguments.none())
                .executor((src, args) -> {
                    JobManager.get().notifyMessageReceiverAdd(src);
                    JobManager.get().resumeJob(true);
                    return CommandResult.success();
                })
                .build();
    }
    static CommandSpec subcmdReject() {
        return CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE_CONFIRM.getId())
                .description(Text.of("Reject OreGet job when requested"))
                .arguments(GenericArguments.none())
                .executor((src, args) -> {
                    JobManager.get().notifyMessageReceiverAdd(src);
                    Set<MessageReceiver> viewer = new HashSet<>(JobManager.get().getViewers().getMembers());
                    viewer.add(Sponge.getServer().getConsole());
                    JobManager.get().resumeJob(false);
                    viewer.forEach(v->v.sendMessage(Text.of(TextColors.RED, "The job was cancelled.")));
                    return CommandResult.success();
                })
                .build();
    }

    private static void printKV(CommandSource src, Object k, Object v) {
        src.sendMessage(Text.of(TextColors.WHITE,k,": ",TextColors.GRAY,v));
    }

    static void register() {
        Sponge.getGame().getCommandManager().register(OreGet.getInstance(), CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE.getId())
                .child(subcmdSearch(), "search")
                .child(subcmdShow(), "show")
                .child(subcmdUpgrade(), "upgrade")
                .child(subcmdFullUpgrade(), "full-upgrade")
                .child(subcmdInstall(), "install")
                .child(subcmdRemove(), "remove")
                .child(subcmdPurge(), "purge")
                .child(subcmdAutoremove(), "autoremove")
                .child(subcmdMark(), "mark")
                .child(subcmdUnmark(), "unmark")
                .child(subcmdConfirm(), "confirm")
                .child(subcmdReject(), "reject", "deny", "cancel")
                .build(), "oreget", "ore-get", "ore");
    }

}
