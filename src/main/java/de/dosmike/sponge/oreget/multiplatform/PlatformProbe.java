package de.dosmike.sponge.oreget.multiplatform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PlatformProbe {

    public static boolean isSponge() {
        try {
            Class.forName("org.spongepowered.api.text.Text");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static <T> T createInstance(String spongeClassName, Class<? extends T> fallback) {
        try { //totally non-confusing brackets ;P
            if (isSponge()) try {
                Class<?> clazz = Class.forName(spongeClassName);
                return (T)clazz.newInstance();
            } catch (ClassNotFoundException|LinkageError platformMissmatch) {
                throw new RuntimeException(platformMissmatch);
            } else {
                return fallback.newInstance();
            }
        } catch (IllegalAccessException|InstantiationException critical) {
            throw new RuntimeException(critical);
        }
    }

    private static Path PATH_MODS=null, PATH_CACHE=null, PATH_PLUGINS=null, PATH_ROOT=null;
    public static Path getModsDirectory() {
        if (PATH_MODS != null) return PATH_MODS;
        boolean debug =
                "true".equalsIgnoreCase(System.getProperty("og_debug")) ||
                "debug".equalsIgnoreCase(System.getProperty("og_debug")) ;
        try {
            Path modsPath = Paths.get(PlatformProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getParent().toAbsolutePath();
            if (modsPath.equals(getServerRoot())) {
                modsPath = modsPath.resolve("mods");
                if (!Files.isDirectory(modsPath)) throw new IllegalStateException(); //trap below
            } else if (modsPath.getParent().getFileName().toString().equalsIgnoreCase("mods")) {
                return modsPath.getParent();
            }
            if (debug) System.out.println("Mods directory is located at "+modsPath.toString());
            return PATH_MODS = modsPath;
        } catch (Exception e) {
            throw new RuntimeException("Could not get mods directory");
        }
    }
    public static Path getPluginsDirectory() {
        if (PATH_PLUGINS != null) return PATH_PLUGINS;
        Path path = getModsDirectory();
        Path testPath = path.resolve("plugins");
        if (Files.isDirectory(testPath)) return testPath;
        return PATH_PLUGINS = path;
    }
    public static Path getCacheDirectory() {
        if (PATH_CACHE != null) return PATH_CACHE;
        Path path = getServerRoot().resolve("oreget_cache");
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException("Could not create cache directory in "+path.toString());
        }
        return PATH_CACHE = path;
    }

    public static Path getServerRoot() {
        if (PATH_ROOT != null) return PATH_ROOT;
        boolean debug =
                "true".equalsIgnoreCase(System.getProperty("og_debug")) ||
                "debug".equalsIgnoreCase(System.getProperty("og_debug")) ;
        Path path = Paths.get(".").toAbsolutePath();
        while (true) {
            try {
                boolean hasServerJar = Files.list(path).anyMatch(sub -> Files.isRegularFile(sub) &&
                        sub.getFileName().toString().toLowerCase().endsWith(".jar") &&
                        sub.getFileName().toString().toLowerCase().contains("minecraft_server"));
                if (hasServerJar) break;
            } catch (IOException e) {
                throw new RuntimeException("Could not read directory (looking for minecraft_server*.jar)");
            }
            path = path.getParent();
            if (path == null) throw new RuntimeException("Could not find server directory (looking for minecraft_server*.jar)");
        }
        if (debug) Logging.log(null, "Found server at ", path);
        return PATH_ROOT = path;
    }

}
