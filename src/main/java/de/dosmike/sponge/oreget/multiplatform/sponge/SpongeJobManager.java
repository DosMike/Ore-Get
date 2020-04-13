package de.dosmike.sponge.oreget.multiplatform.sponge;

import de.dosmike.sponge.oreget.multiplatform.JobManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.channel.impl.SimpleMutableMessageChannel;

import java.util.Collection;

/**
 * knows if a job is running, holds "viewers" of the job and logs into both
 * the server console and to the "viewers". also handles hand/unhang for confirmations
 */
public class SpongeJobManager extends JobManager {

    /**
     * Who will be notified about the current job. If a commandsource tries to start a job, while one is running
     * they'll be added to the list, getting notified about the jobs progress
     */
    SimpleMutableMessageChannel jobChannel = new SimpleMutableMessageChannel();

    @Override
    public void addViewer(@Nullable Object viewer) {
        if (viewer == null) {
            jobChannel.addMember(Sponge.getServer().getConsole());
            return;
        }
        if (!(viewer instanceof MessageReceiver))
            throw new IllegalArgumentException("AddViewer only accepts MessageReceiver");
        jobChannel.addMember((MessageReceiver) viewer);
    }

    @Override
    public void removeViewer(@Nullable Object viewer) {
        if (viewer == null) {
            jobChannel.removeMember(Sponge.getServer().getConsole());
            return;
        }
        if (!(viewer instanceof MessageReceiver))
            throw new IllegalArgumentException("RemoveViewer only accepts MessageReceiver");
        jobChannel.removeMember((MessageReceiver) viewer);
    }

    @Override
    public void clearViewers() {
        jobChannel.clearMembers();
    }

    @Override
    public Collection<?> getViewers() {
        return jobChannel.getMembers();
    }

    @Override
    protected void _sendLine(Object platformNative) {
        jobChannel.send((Text)platformNative);
    }

    public SpongeJobManager() {
        super();
    }

}
