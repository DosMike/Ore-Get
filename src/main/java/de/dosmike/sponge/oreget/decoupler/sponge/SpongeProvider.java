package de.dosmike.sponge.oreget.decoupler.sponge;

import de.dosmike.sponge.oreget.OreGetPlugin;
import de.dosmike.sponge.oreget.decoupler.AbstractionProvider;
import de.dosmike.sponge.oreget.decoupler.IPlugin;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpongeProvider extends AbstractionProvider {

    @Override
    public Collection<IPlugin> getPlugins() {
        return Sponge.getPluginManager().getPlugins().stream().map(PluginContainerWrapper::new).collect(Collectors.toSet());
    }

    @Override
    public Optional<IPlugin> getPlugin(String id) {
        return Sponge.getPluginManager().getPlugin(id).map(PluginContainerWrapper::new);
    }

    final Pattern link = Pattern.compile("\\[(.*)\\]\\((.*)\\)");
    @Override
    public Object string2native(String string) {
        Text.Builder accu = Text.builder();
        Matcher m = link.matcher(string);
        int lastEnd = 0;
        while (m.find()) {
            accu.append(TextSerializers.FORMATTING_CODE.deserialize(string.substring(lastEnd, m.start())));
            accu.append(Text.builder().append(TextSerializers.FORMATTING_CODE.deserialize(m.group(1))).onClick(TextActions.runCommand(m.group(2))).build());
            lastEnd = m.end();
        }
        if (lastEnd < string.length())
            accu.append(TextSerializers.FORMATTING_CODE.deserialize(string.substring(lastEnd)));
        return accu.build();
    }

    @Override
    public Logger log() {
        return OreGetPlugin.getLogger();
    }
}
