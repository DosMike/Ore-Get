package de.dosmike.sponge.oreget.decoupler.standalone;

import de.dosmike.sponge.oreget.decoupler.AbstractionProvider;
import de.dosmike.sponge.oreget.decoupler.IPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.Loggers;
import org.apache.logging.log4j.core.config.LoggersPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.item.inventory.Inventory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class StandaloneProvider extends AbstractionProvider {

    private Set<IPlugin> plugins = new HashSet<>();
    private Logger logger;
    public Logger log() { return logger; }

    public StandaloneProvider() {
//        AnsiConsole.systemInstall();
        logger = LoggerFactory.getLogger("OreGet");

        log().info("Seaching Plugins...");
        try {
            Files.newDirectoryStream(Paths.get("."),path->path.toString().endsWith(".jar")).forEach(path-> {
                try {
                    plugins.addAll(JarPlugin.fromJar(path));
                } catch (IOException e) {
                    log().error((String)string2native("&eCould not load "+path.getFileName()+": "+e.getMessage()));
                }
            });
        } catch (IOException e) {
            e.printStackTrace(); //unexpected
        }
        if (Files.notExists(Paths.get("..").resolve("server.properties"))) {
            try {
                Files.newDirectoryStream(Paths.get("."),path->path.toString().endsWith(".jar")).forEach(path-> {
                    try {
                        plugins.addAll(JarPlugin.fromJar(path));
                    } catch (IOException e) {
                        log().error((String)string2native("&eCould not load ..\\"+path.getFileName()+": "+e.getMessage()));
                    }
                });
            } catch (IOException e) {
                e.printStackTrace(); //unexpected
            }
        }
        for (IPlugin plugin : plugins) {
            log().info("Loaded "+plugin.getName()+" ("+plugin.getId()+plugin.getVersion().map(v->" version "+v+")").orElse(")"));
        }
        log().info("");
    }

    @Override
    public Collection<IPlugin> getPlugins() {
        return plugins;
    }
    @Override
    public Optional<IPlugin> getPlugin(String id) {
        return plugins.stream().filter(pl->pl.getId().equalsIgnoreCase(id)).findFirst();
    }

    @Override
    public Object string2native(String string) {
        string = string.replaceAll("\\[(.*)\\]\\(.*\\)", "$1");
        String[] sponge = {
                "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7",
                "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f",
                "&r", "&l", "&m", "&n", "&o", "&&"
        };
        String[] ansi = {
                "\u001B[0;30m", "\u001B[0;34m", "\u001B[0;32m", "\u001B[0;36m", "\u001B[0;31m", "\u001B[0;35m", "\u001B[0;33m", "\u001B[0;37m",
                "\u001B[0;90m", "\u001B[0;94m", "\u001B[0;92m", "\u001B[0;96m", "\u001B[0;91m", "\u001B[0;95m", "\u001B[0;93m", "\u001B[0;97m",
                "\u001B[0m", "\u001B[1m", "\u001B[9m", "\u001B[4m", "\u001B[3m", "&"
        };
        for (int i = 0; i < sponge.length; i++)
            string = string.replace(sponge[i], ansi[i]);

        return string+ansi[16]; //append reset
    }
}
