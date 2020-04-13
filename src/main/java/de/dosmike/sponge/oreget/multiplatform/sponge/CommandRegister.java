package de.dosmike.sponge.oreget.multiplatform.sponge;

import de.dosmike.sponge.oreget.Commands;
import de.dosmike.sponge.oreget.jobs.JobManagerBusyException;
import de.dosmike.sponge.oreget.multiplatform.JobManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Collection;

public class CommandRegister {

    static CommandSpec subcmdSearch() {
        return CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE_SEARCH.getId())
                .description(Text.of("Search plugins on ore"))
                .arguments(GenericArguments.remainingJoinedStrings(Text.of("query")))
                .executor((src, args) -> {
                    String query = args.<String>getOne("query").get();
                    Commands.search(src, query);
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
                    Commands.show(src, pluginId);
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
                    try {
                        Commands.upgrade(src);
                        return CommandResult.success();
                    } catch (JobManagerBusyException e) {
                        JobManager.get().addViewer(src);
                        throw new CommandException(Text.of(TextColors.RED, e.getMessage()));
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
                    try {
                        Commands.fullUpgrade(src);
                        return CommandResult.success();
                    } catch (JobManagerBusyException e) {
                        JobManager.get().addViewer(src);
                        throw new CommandException(Text.of(TextColors.RED, e.getMessage()));
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
                    try {
                        Commands.install(src, onlyUpgrade, pluginId.toArray(new String[0]));
                        return CommandResult.success();
                    } catch (JobManagerBusyException e) {
                        JobManager.get().addViewer(src);
                        throw new CommandException(Text.of(TextColors.RED, e.getMessage()));
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
                    try {
                        Commands.remove(src, false, pluginId.toArray(new String[0]));
                        return CommandResult.success();
                    } catch (JobManagerBusyException e) {
                        JobManager.get().addViewer(src);
                        throw new CommandException(Text.of(TextColors.RED, e.getMessage()));
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
                    try {
                        Commands.remove(src, true, pluginId.toArray(new String[0]));
                        return CommandResult.success();
                    } catch (JobManagerBusyException e) {
                        JobManager.get().addViewer(src);
                        throw new CommandException(Text.of(TextColors.RED, e.getMessage()));
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
                    try {
                        Commands.autoremove(src);
                        return CommandResult.success();
                    } catch (JobManagerBusyException e) {
                        JobManager.get().addViewer(src);
                        throw new CommandException(Text.of(TextColors.RED, e.getMessage()));
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
                    try {
                        Commands.mark(src, pluginId, true);
                        return CommandResult.success();
                    } catch (JobManagerBusyException e) {
                        JobManager.get().addViewer(src);
                        throw new CommandException(Text.of(TextColors.RED, e.getMessage()));
                    }
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
                    try {
                        Commands.mark(src, pluginId, false);
                        return CommandResult.success();
                    } catch (JobManagerBusyException e) {
                        JobManager.get().addViewer(src);
                        throw new CommandException(Text.of(TextColors.RED, e.getMessage()));
                    }
                })
                .build();
    }

    static CommandSpec subcmdConfirm() {
        return CommandSpec.builder()
                .permission(PermissionRegister.CMD_ORE_CONFIRM.getId())
                .description(Text.of("Confirm OreGet job when requested"))
                .arguments(GenericArguments.none())
                .executor((src, args) -> {
                    Commands.confirm(src);
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
                    Commands.reject(src);
                    return CommandResult.success();
                })
                .build();
    }

    static void register() {
        Sponge.getGame().getCommandManager().register(OreGetPlugin.getInstance(), CommandSpec.builder()
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
