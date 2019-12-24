package de.dosmike.sponge.oreget;

import com.google.inject.Inject;
import de.dosmike.sponge.oreget.cache.PluginCache;
import de.dosmike.sponge.oreget.oreapi.OreApiV2;
import de.dosmike.sponge.oreget.utils.ExitHandler;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
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
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

@Plugin(id = "oreget", name = "Ore-Get", version = "1.0.2")
public class OreGet {

    public static void main(String[] args) {
        System.err.println("This plugin can not be run as executable!");
    }

    static OreGet instance;
    public static OreGet getInstance() {
        return instance;
    }

    static SpongeExecutorService async;
    public static SpongeExecutorService async() {
        return async;
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

    private OreApiV2 oreApi = new OreApiV2();
    public static OreApiV2 getOre() {
        return instance.oreApi;
    }

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;
    private static TypeSerializerCollection customSerializer = TypeSerializers.getDefaultSerializers().newChild();

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path privateConfigDir;

    private Path pluginDirectory;
    public Path getPluginDirectory() {
        return pluginDirectory;
    }

    private PluginCache pluginCache = new PluginCache();
    public static PluginCache getPluginCache() {
        return instance.pluginCache;
    }

    @Listener
    public void onServerInit(GameInitializationEvent event) {
        instance = this;
        async = Sponge.getScheduler().createAsyncExecutor(OreGet.getInstance());
        l("Registering events&commands...");
        Sponge.getEventManager().registerListeners(this, new EventListener());
        CommandRegister.register();
        pluginDirectory = getContainer().getSource().get().getParent();
        loadConfigs();
        l("Rebuilding plugin cache...");
        pluginCache.scanLoaded();
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        ExitHandler.attach();
        l("OreGet is now ready!");
    }

    @Listener
    public void onServerStopping(GameStoppingEvent event) {

    }

    public void loadConfigs() {
        //settings.conf
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .setPath(privateConfigDir.resolve("settings.conf"))
                .build();
        try {
            CommentedConfigurationNode root = loader.load(ConfigurationOptions.defaults());

        } catch (IOException e) {
            new RuntimeException("Could not load settings.conf", e).printStackTrace();
        }
    }

}
