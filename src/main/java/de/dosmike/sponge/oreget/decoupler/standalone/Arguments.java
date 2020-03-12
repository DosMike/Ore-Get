package de.dosmike.sponge.oreget.decoupler.standalone;

import de.dosmike.sponge.oreget.decoupler.AbstractionProvider;
import de.dosmike.sponge.oreget.decoupler.IPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Arguments {

    private List<String> raw;
    private AbstractionProvider p = AbstractionProvider.get();

    public Arguments(String[] args_) {
        raw = Arrays.asList(args_);
        if (raw.isEmpty() || raw.contains("--help")) {
            usage(); System.exit(0);
        }
    }

    private void usage() {
        Optional<IPlugin> self = p.getPlugin("oreget");
        p.log().info((String)p.string2native("You are using &6OreGet &b"+
                self.flatMap(IPlugin::getVersion)
                    .orElse("Standalone")));
        p.log().info((String)p.string2native("Syntax: &3java -jar "+self.flatMap(p->p.getSource().map(s->s.getFileName().toString())).orElse("oreget.jar")+" &2command &6flags &5arguments"));
        p.log().info("The following commands are available:");
        p.log().info((String)p.string2native("   &2search &5text&r         Search projects on Ore using the specified &5query&r."));
        p.log().info((String)p.string2native("   &2show &5plugin id&r      Show local and remote information about the &5plugin&r."));
        p.log().info((String)p.string2native("   &2upgrade&r             Update all plugins and install new dependencies if"));
        p.log().info((String)p.string2native("                       necessary."));
        p.log().info((String)p.string2native("   &2full-upgrade&r        Like upgrade, but obsolete plugins will be removed"));
        p.log().info((String)p.string2native("                       (like with autoremove)."));
        p.log().info((String)p.string2native("   &2install &5plugin ids&r  Install &5plugins&r by id. You can install multiple"));
        p.log().info((String)p.string2native("                       plugins at once by separating the ids with space."));
        p.log().info((String)p.string2native("                       Use &6-only-upgrade&r to prevent installing the plugin."));
        p.log().info((String)p.string2native("   &2remove &5plugin ids&r   The opposite to install. Will remove all &5plugins&r."));
        p.log().info((String)p.string2native("   &2purge &5plugin ids&r    Will not only remove the &5plugins&r, but also try to"));
        p.log().info((String)p.string2native("                       remove config folders as well."));
        p.log().info((String)p.string2native("   &2autoremove&r          Remove all dependencies that are no longer required."));
        p.log().info((String)p.string2native("                       Dependencies are plugins installed automatically with"));
        p.log().info((String)p.string2native("                       other plugins, required to make the plugin function."));
        p.log().info((String)p.string2native("   &2mark &5plugin ids&r     Marks all &5plugins&r as automatically installed."));
        p.log().info((String)p.string2native("                       This means that autoremove can delete this plugin."));
        p.log().info((String)p.string2native("   &2unmark &5plugin ids&r   Marks all &5plugins&r as manually installed."));
        p.log().info((String)p.string2native("                       Autoremove is not allowed to touch these."));
    }

}
