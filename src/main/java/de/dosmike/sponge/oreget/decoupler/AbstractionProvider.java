package de.dosmike.sponge.oreget.decoupler;

import de.dosmike.sponge.oreget.decoupler.sponge.SpongeProvider;
import de.dosmike.sponge.oreget.decoupler.standalone.StandaloneProvider;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Optional;

public abstract class AbstractionProvider {

    abstract public Collection<IPlugin> getPlugins();
    abstract public Optional<IPlugin> getPlugin(String id);
    /**
     * Use sponge-esque input with &escape codes and [markdown-like](command embeds)
     * returns either a Text-Object or an ansi escaped sequence
     */
    abstract public Object string2native(String string);
    abstract public Logger log();

    private static AbstractionProvider implementation = null;
    public static AbstractionProvider get() {
        if (implementation == null) try {
            ClassLoader.getSystemClassLoader().loadClass("org.spongepowered.api.Sponge");
            implementation = new SpongeProvider();
        } catch (ClassNotFoundException e) {
            implementation = new StandaloneProvider();
        }
        return implementation;
    }

}
