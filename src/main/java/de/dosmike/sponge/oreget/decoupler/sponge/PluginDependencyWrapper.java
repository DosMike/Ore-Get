package de.dosmike.sponge.oreget.decoupler.sponge;

import de.dosmike.sponge.oreget.decoupler.IDependency;
import org.spongepowered.plugin.meta.PluginDependency;

public class PluginDependencyWrapper implements IDependency {

    String id;
    String version;
    boolean optional;
    public PluginDependencyWrapper(PluginDependency dependency) {
        id = dependency.getId();
        version = dependency.getVersion();
        optional = dependency.isOptional();
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * <b>Copy from <a href="https://github.com/SpongePowered/plugin-meta/blob/185f5c2744c9147ead111342f5307c754765e5cf/src/main/java/org/spongepowered/plugin/meta/PluginDependency.java">SpongePowered/plugin-meta</a></b><hr>
     * Returns the version range this {@link PluginDependency} should match.
     *
     * <p>The version range should use the <b>Maven version range
     * syntax</b>:</p>
     *
     * <table>
     * <caption>Maven version range syntax</caption>
     * <tr><th>Range</th><th>Meaning</th></tr>
     * <tr><td>1.0</td><td>Any dependency version, 1.0 is recommended</td></tr>
     * <tr><td>[1.0]</td><td>x == 1.0</td></tr>
     * <tr><td>[1.0,)</td><td>x &gt;= 1.0</td></tr>
     * <tr><td>(1.0,)</td><td>x &gt; 1.0</td></tr>
     * <tr><td>(,1.0]</td><td>x &lt;= 1.0</td></tr>
     * <tr><td>(,1.0)</td><td>x &lt; 1.0</td></tr>
     * <tr><td>(1.0,2.0)</td><td>1.0 &lt; x &lt; 2.0</td></tr>
     * <tr><td>[1.0,2.0]</td><td>1.0 &lt;= x &lt;= 2.0</td></tr>
     * </table>
     *
     * @return The version range, or {@code null} if unspecified
     * @see <a href="https://goo.gl/edrup4">Maven version range specification</a>
     * @see <a href="https://goo.gl/WBsFIu">Maven version design document</a>
     */
    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }
}
