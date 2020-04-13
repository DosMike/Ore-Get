package de.dosmike.sponge.oreget.multiplatform;

import de.dosmike.sponge.oreget.oreapi.OreApiV2;
import de.dosmike.sponge.oreget.utils.TracingThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SharedInstances {

    private static final OreApiV2 oreApi = new OreApiV2();
    public static OreApiV2 getOre() {
        return oreApi;
    }

    private static final ScheduledExecutorService asyncExecutor = Executors.newScheduledThreadPool(4, new TracingThreadFactory());
    public static ScheduledExecutorService getAsyncExecutor() {
        return asyncExecutor;
    }

}
