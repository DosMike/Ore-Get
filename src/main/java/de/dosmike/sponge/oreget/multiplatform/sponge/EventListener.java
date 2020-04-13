package de.dosmike.sponge.oreget.multiplatform.sponge;

import de.dosmike.sponge.oreget.multiplatform.JobManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

public class EventListener {

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        JobManager.get().removeViewer(event.getTargetEntity());
    }

}
