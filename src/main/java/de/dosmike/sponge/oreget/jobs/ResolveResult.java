package de.dosmike.sponge.oreget.jobs;

import de.dosmike.sponge.oreget.oreapi.v2.OreProject;
import de.dosmike.sponge.oreget.oreapi.v2.OreVersion;

import java.util.*;

public class ResolveResult {

    //These lists are meant to be read by the user
    Set<String> pluginsToInstall = new TreeSet<>(String::compareToIgnoreCase);
    Set<String> pluginsToUpdate = new TreeSet<>(String::compareToIgnoreCase);
    Set<String> pluginsToRemove = new TreeSet<>(String::compareToIgnoreCase);
    Set<String> pluginsNotOnOre = new TreeSet<>(String::compareToIgnoreCase);
    Set<String> pluginsNotReviewed = new TreeSet<>(String::compareToIgnoreCase);
    Set<String> pluginsRequestedManually = new HashSet<>();
    //This is an internal list for downloads
    Map<OreProject, OreVersion> toDownload = new HashMap<>();

    Set<String> getPluginsToDownload() {
        Set<String> set = new TreeSet<>(String::compareToIgnoreCase);
        set.addAll(pluginsToInstall);
        set.addAll(pluginsToUpdate);
        return set;
    }

}
