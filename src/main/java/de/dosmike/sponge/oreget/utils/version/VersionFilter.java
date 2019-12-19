package de.dosmike.sponge.oreget.utils.version;

import de.dosmike.sponge.oreget.OreGet;
import de.dosmike.sponge.oreget.oreapi.v2.*;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.gen.populator.Ore;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class VersionFilter {

    static boolean testSpongeVersion(OreTag[] tags) {
        for (OreTag tag : tags) {
            if (tag.getName().equalsIgnoreCase("Sponge")) {
                String version = Sponge.getPlatform().getContainer(Platform.Component.IMPLEMENTATION).getVersion().get();
                //implementation version is formatted GameVersion-SpongeImplVersion
                //With each being a Major.Minor.Revision
                int i = version.indexOf('-');
                if (i >= 0) version = version.substring(i+1);
                String range = tag.getData();
                return VersionRange.parseString(range).test(version);
            }
        }
        return true;
    }
    static boolean testProjectStable(OreTag[] tags) {
        for (OreTag tag : tags) {
            if (tag.getName().equalsIgnoreCase("Unstable"))
                return false;
        }
        return true;
    }
    static boolean testProjectReleaseChannel(OreTag[] tags) {
        for (OreTag tag : tags) {
            if (tag.getName().equalsIgnoreCase("Channel"))
                return tag.getData().equalsIgnoreCase("Release");
        }
        return false; //is no channel possible?
    }

    public static Optional<OreVersion> getFirstStable(OreVersion[] versions) {
        for (OreVersion v : versions) {
            if (testSpongeVersion(v.getTags())) {
                if (testProjectStable(v.getTags()) && testProjectReleaseChannel(v.getTags()))
                    return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    public static Optional<OreVersion> getAnyLatest(OreVersion[] versions) {
        for (OreVersion v : versions) {
            if (testSpongeVersion(v.getTags())) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    /** partial only lists the Sponge tag */
    public static Optional<OrePartialVersion> getAnyLatest(OrePartialVersion[] versions) {
        for (OrePartialVersion v : versions) {
            if (testSpongeVersion(v.getTags())) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    public static Optional<OreVersion> getLatestStableVersion(OreProject project) {
        Optional<OreResultList<OreVersion>> page = OreGet.getOre().waitFor(()->OreGet.getOre().listVersions(project.getPluginId(), null));
        while(page.isPresent() && page.get().getResult().length>0) { // we can theoretically paginate beyond the last page, break
            //scan page
            Optional<OreVersion> stable = getFirstStable(page.get().getResult());
            //if version is stable return
            if (stable.isPresent()) return stable;
            //if page is last page break
            if (page.get().getPagination().getPage() == page.get().getPagination().getLastPage()) break;
            String nextPageQuery = page.get().getPagination().getQueryNext();
            page = OreGet.getOre().waitFor(()->OreGet.getOre().listVersions(project.getPluginId(), nextPageQuery));
        }
        return Optional.empty();
    }

}
