package de.dosmike.sponge.oreget.decoupler;

public interface IDependency {

    String getId();
    String getVersion();
    boolean isOptional();

}
