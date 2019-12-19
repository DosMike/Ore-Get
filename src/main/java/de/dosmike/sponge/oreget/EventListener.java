package de.dosmike.sponge.oreget;

import de.dosmike.sponge.oreget.jobs.JobManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

public class EventListener {

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        JobManager.get().notifyMessageReceiverParted(event.getTargetEntity());
    }

}
