package de.dosmike.sponge.oreget.multiplatform.sponge;

import de.dosmike.sponge.oreget.utils.Permission;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.text.Text;

public class PermissionRegister {

    public static Permission CMD_ORE = Permission.Registry.register("CMD_ORE",
                    "oreget.command.base",
                    Text.of("Allow basic usage of /oreget"),
                    PermissionDescription.ROLE_STAFF);
    public static Permission CMD_ORE_SEARCH = Permission.Registry.register("CMD_ORE_SEARCH",
            "oreget.command.search",
            Text.of("Allow a user to search plugins on Ore"),
            PermissionDescription.ROLE_STAFF);
    public static Permission CMD_ORE_SHOW = Permission.Registry.register("CMD_ORE_SHOW",
            "oreget.command.show",
            Text.of("Allow a user to view plugin details"),
            PermissionDescription.ROLE_STAFF);
    public static Permission CMD_ORE_UPGRADE = Permission.Registry.register("CMD_ORE_UPGRADE",
            "oreget.command.upgrade",
            Text.of("Allow a user to update all installed plugins"),
            PermissionDescription.ROLE_STAFF);
    public static Permission CMD_ORE_FULL_UPGRADE = Permission.Registry.register("CMD_ORE_FULL_UPGRADE",
            "oreget.command.fullupgrade",
            Text.of("Allow a user to update all installed plugins, with dependency management (remove old)"),
            PermissionDescription.ROLE_STAFF);
    public static Permission CMD_ORE_INSTALL = Permission.Registry.register("CMD_ORE_INSTALL",
            "oreget.command.install",
            Text.of("Allow a user to install new plugins with dependencies"),
            PermissionDescription.ROLE_STAFF);
    public static Permission CMD_ORE_REMOVE = Permission.Registry.register("CMD_ORE_REMOVE",
            "oreget.command.remove",
            Text.of("Allow a user to uninstall plugins"),
            PermissionDescription.ROLE_STAFF);
    public static Permission CMD_ORE_PURGE = Permission.Registry.register("CMD_ORE_PURGE",
            "oreget.command.purge",
            Text.of("Allow a user to purge a plugin (uninstall and remove configs)"),
            PermissionDescription.ROLE_STAFF);
    public static Permission CMD_ORE_AUTOREMOVE = Permission.Registry.register("CMD_ORE_AUTOREMOVE",
            "oreget.command.autoremove",
            Text.of("Allow a user to auto-remove plugins that are no longer necessary"),
            PermissionDescription.ROLE_STAFF);
    public static Permission CMD_ORE_MARK = Permission.Registry.register("CMD_ORE_MARK",
            "oreget.command.autoremove",
            Text.of("Allow a user to mark or unmark a plugins as automatically installed plugin"),
            PermissionDescription.ROLE_STAFF);
    public static Permission CMD_ORE_CONFIRM = Permission.Registry.register("CMD_ORE_CONFIRM",
            "oreget.command.confirm",
            Text.of("Some jobs require additional confirmation"),
            PermissionDescription.ROLE_STAFF);

}
