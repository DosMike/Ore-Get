/**
 * This package contains a lot of wrapper to make sponge a `super-optional`
 * In Java implementing optional dependencies requires no mention of the dependencies in loaded classes
 * at runtime, thus every interaction has to be implemented using interfaces, that use different
 * implementations based on whether the dependency is available or not.
 */
package de.dosmike.sponge.oreget.decoupler;