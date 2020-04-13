package de.dosmike.sponge.oreget.multiplatform.sponge;

import com.google.inject.Inject;
import de.dosmike.sponge.oreget.cache.PluginCache;
import de.dosmike.sponge.oreget.utils.ExitHandler;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;

import java.nio.file.Path;
import java.util.Optional;

@Plugin(id = "oreget", name = "Ore-Get", version = "1.1")
public class OreGetPlugin {

    public static void main(String[] args) {
        throw new UnsupportedOperationException("This main only exists to give the class a 'Runnable'-Type icon in your IDE");
    }

    static OreGetPlugin instance;
    public static OreGetPlugin getInstance() {
        return instance;
    }

    PluginContainer getContainer() { return Sponge.getPluginManager().fromInstance(this).orElseThrow(()->new InternalError("No plugin container for self returned")); }

    private static PermissionService permissions = null;
    public static Optional<PermissionService> getPermissions() {
        return Optional.ofNullable(permissions);
    }
    public static Optional<PermissionDescription.Builder> describePermission() {
        return getPermissions().map(p->p.newDescriptionBuilder(instance));
    }
    @Listener
    public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
        if (event.getService().equals(PermissionService.class)) {
            permissions = (PermissionService)event.getNewProvider();
        }
    }

    @Inject
    private Logger logger;
    public static void l(String format, Object... args) {
        instance.logger.info(String.format(format, args));
    }
    public static void w(String format, Object... args) {
        instance.logger.warn(String.format(format, args));
    }

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;
    private static TypeSerializerCollection customSerializer = TypeSerializers.getDefaultSerializers().newChild();

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path privateConfigDir;

    @Listener
    public void onServerInit(GameInitializationEvent event) {
        instance = this;
        l("Registering events&commands...");
        Sponge.getEventManager().registerListeners(this, new EventListener());
        CommandRegister.register();
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        l("Rebuilding plugin cache...");
        PluginCache.get().scanLoaded();

        ExitHandler.attach();
        l("OreGet is now ready!");
    }

    @Listener
    public void onServerStopping(GameStoppingEvent event) {

    }

}
