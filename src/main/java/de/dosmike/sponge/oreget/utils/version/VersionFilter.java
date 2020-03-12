package de.dosmike.sponge.oreget.utils.version;

import de.dosmike.sponge.oreget.OreGet;
import de.dosmike.sponge.oreget.oreapi.v2.*;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;

import java.util.Optional;

public class VersionFilter {

    static boolean testSpongeVersion(OreTag[] tags) {
        for (OreTag tag : tags) {
            if (tag.getName().equalsIgnoreCase("Sponge")) {
                String version = Sponge.getPlatform().getContainer(Platform.Component.API).getVersion().get();
                //Implementation version is formatted GameVersion-SpongeImplVersion
                //  With each being a Major.Minor.Revision
                //Api version is formatted SpongeApiVersion-CommitHash <-- used for recommended filters
                int i = version.indexOf('-');
                if (i >= 0) version = version.substring(0,i);
                String range = tag.getData();
                // ORE DOES NOT PROMOTE VERSIONS WITHOUT SPONGE API VERSION RANGE - i disagree on that but won't force that:
                // if range is missing, this should indicate that the plugin should run on any API (otherwise author would set it)
                if (range == null || range.isEmpty()) return true; // let versions without range pass
                VersionRange.VersionTest test = VersionRange.parseString(range);
                // i won't follow the maven version in that soft requirements (lenient/bound less versions)
                // will not allow any version or try best match (hard to implement). Instead I will limit these version
                // to have to match the same major version. I rely on promoted version listing the latest soft
                // requirement first, guaranteeing that we find updates.
                if (test.lenient()) {
                    // since the range is lenient it has to be formatted like a single version.
                    // this we can do the following:
                    return (new Version(version).getMajor() == new Version(range).getMajor());
                } else {
                    return test.test(version);
                }
            }
        }
        return false; // No plugin version specifies a sponge version or is within the requires sponge version
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

    private static VersionRange.VersionTest toVersionTest(OrePartialVersion v) {
        return VersionRange.parseString(v.getVersion());
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

    public static Optional<OrePartialVersion> getLatestPromotedVersionPartial(OreProject project) {
        return getAnyLatest(project.getPromotedVersions());
    }
    public static Optional<OreVersion> getLatestPromotedVersion(OreProject project) {
        return getAnyLatest(project.getPromotedVersions()).flatMap((v)->OreGet.getOre().waitFor(()->OreGet.getOre().getVersion(project.getPluginId(), v.getVersion())));
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
