package de.dosmike.sponge.oreget.decoupler.standalone;

import de.dosmike.sponge.oreget.decoupler.IDependency;
import org.spongepowered.plugin.meta.PluginDependency;

public class JarDependency implements IDependency {

    String version = null;
    String id;
    boolean optional = false;
    public JarDependency(String definition) {
        int i = definition.indexOf('@');
        if (i>=0) {
            version = definition.substring(i+1);
            definition = definition.substring(0,i);
        }
        i = definition.indexOf(':');
        if (i>=0) {
            String left = definition.substring(0,i);
            id = definition.substring(i+1);
            optional = !left.toLowerCase().contains("required");
        } else {
            id = definition;
        }
        if (id.isEmpty()) throw new IllegalArgumentException("Plugin ID is empty in "+definition);
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
