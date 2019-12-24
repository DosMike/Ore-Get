package de.dosmike.sponge.oreget.jobs;

import de.dosmike.sponge.oreget.OreGet;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.channel.impl.SimpleMutableMessageChannel;
import org.spongepowered.api.text.format.TextColors;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JobManager {

    private static JobManager instance = new JobManager();
    public static JobManager get() {
        return instance;
    }

    private AbstractJob currentJob = null;
    Future<?> watchdog = null; //keeping track of termination with isDone()
    private final Object jobMutex = new Object();
    /**
     * Who will be notified about the current job. If a commandsource tries to start a job, while one is running
     * they'll be added to the list, getting notified about the jobs progress
     */
    SimpleMutableMessageChannel jobChannel = new SimpleMutableMessageChannel();
    public MessageChannel getViewers() {
        return jobChannel;
    }
    public void notifyMessageReceiverParted(MessageReceiver receiver) {
        jobChannel.removeMember(receiver);
    }
    ArrayList<Text> chatTerminal;
    public void println(Text line) {
        chatTerminal.remove(0);
        chatTerminal.add(line);
        Sponge.getServer().getConsole().sendMessage(Text.of("OreJob ", line));
    }
    public void clear() {
        for (int i = 0; i < 10; i++) chatTerminal.set(i, Text.EMPTY);
    }
    public void notifyMessageReceiverAdd(MessageReceiver receiver) {
        if (receiver instanceof Player)
            jobChannel.addMember(receiver);
    }

    private static int spindex = 0;
    private static String[] spinner = {" \u2591 ", " \u2592 ", " \u2593 ", " \u2588 ", " \u2593 ", " \u2592 "};
    private JobManager() {
        chatTerminal = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) chatTerminal.add(Text.EMPTY);
        OreGet.async().scheduleAtFixedRate(()->{
            float prog;
            String more;
            synchronized (jobMutex) {
                if (watchdog.isDone()) currentJob = null;
                if (currentJob == null) return;
                prog = currentJob.getProgress();
                more = currentJob.getMessage();
            }
            synchronized (hangMutex) {
                // suppress progress updates while hung
                // makes eventual confirmation message more readable
                if (hangResult != null) return;
            }
            printUpdate(prog, more);
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void printUpdate(float prog, String more) {
        Text.Builder tb = Text.builder();
        {
            String tmp = String.format("%d", (int)prog);
            String padDummy = "   ";
            tb.append(Text.of(" [", TextColors.AQUA,padDummy.substring(tmp.length()),tmp,"%",TextColors.RESET,"]"));
        }
        tb.append(Text.of(spinner[spindex++])); if (spindex>=spinner.length) spindex=0;
        {
            String left = "####################";
            String right = "--------------------";
            int a = (int)(prog/5);
            int b = 20-a;
            tb.append(Text.of(TextColors.DARK_GREEN, left.substring(0,a)));
            if (prog%1>0.5) {
                tb.append(Text.of(TextColors.GREEN, ':'));
                b--;
            }
            tb.append(Text.of(TextColors.RESET));
            if (b>0)
                tb.append(Text.of(right.substring(0,b)));
        }
        if (more != null && !more.isEmpty())
            tb.append(Text.of(" ", more));
        Text bottom = tb.build();
        jobChannel.send(Text.of("===================== OreGet Job ====================="));
        chatTerminal.forEach(text->jobChannel.send(text));
        jobChannel.send(bottom);
        Sponge.getServer().getConsole().sendMessage(Text.of("OreJob ", bottom));
    }

    public boolean runJob(AbstractJob job) {
        synchronized (jobMutex) {
            if (currentJob != null && !watchdog.isDone()) return false;
            jobChannel.clearMembers();
            currentJob = job;
            watchdog = CompletableFuture.runAsync(job, OreGet.async())
                    .thenAccept(ignore->{
                        printUpdate(100f, "DONE"); //display final note
                        jobChannel.clearMembers(); //kick all members
                        clear(); //start "fresh" with next job
                    });
            return true;
        }
    }

    public boolean isIdle() {
        synchronized (jobMutex) {
            return currentJob == null || watchdog.isDone();
        }
    }

    private CompletableFuture<Boolean> hangResult = null;
    private final Object hangMutex = new Object();
    CompletableFuture<Boolean> hang() {
        synchronized (hangMutex) {
            if (hangResult != null)
                throw new IllegalStateException("There's already a hung job!");
            printUpdate(currentJob.getProgress(), currentJob.getMessage()); //display chat users
            return hangResult = new CompletableFuture<>();
        }
    }
    /** to be called on timeout or result */
    void unhang() {
        synchronized (hangMutex) {
            if (hangResult != null) {
                hangResult.completeExceptionally(new TimeoutException("The job confirmation timed out"));
                hangResult = null;
            }
        }
    }
    /**
     * resume a currently hanging job, if confirm is false
     * the job is supposed to terminate. Since this is supposed to
     * be called from a command, this will throw a CommandException if
     * there is currently no job hanging
     * @param confirm true if the job shall continue
     */
    public void resumeJob(boolean confirm) throws CommandException {
        synchronized (hangMutex) {
            if (hangResult == null)
                throw new CommandException(Text.of(TextColors.RED, "There's currently no Job awaiting confirmation"));
            hangResult.complete(confirm);
        }
    }
}
