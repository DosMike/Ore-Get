package de.dosmike.sponge.oreget.multiplatform;

import de.dosmike.sponge.oreget.cache.ProjectContainer;
import de.dosmike.sponge.oreget.multiplatform.terminal.TerminalPluginScanner;

import java.util.List;

/** One time Construction-Wrapper, because PluginCache will only scan once */
public abstract class PluginScanner {

    public abstract List<ProjectContainer> getProjects();

    public static PluginScanner create() {
        return PlatformProbe.createInstance("de.dosmike.sponge.oreget.multiplatform.sponge.SpongePluginScanner", TerminalPluginScanner.class);
    }

}
