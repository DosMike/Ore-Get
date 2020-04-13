package de.dosmike.sponge.oreget.multiplatform;

import de.dosmike.sponge.oreget.jobs.AbstractJob;
import de.dosmike.sponge.oreget.multiplatform.terminal.TerminalJobManager;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * knows if a job is running, holds "viewers" of the job and logs into both
 * the server console and to the "viewers". also handles hand/unhang for confirmations
 */
public abstract class JobManager {

    private static JobManager instance = null;
    public static synchronized JobManager get() {
        if (instance == null) {
            instance = PlatformProbe.createInstance("de.dosmike.sponge.oreget.multiplatform.sponge.SpongeJobManager", TerminalJobManager.class);
        }
        return instance;
    }

    private AbstractJob currentJob = null;
    Future<?> watchdog = null; //keeping track of termination with isDone()
    private final Object jobMutex = new Object();

    public abstract void addViewer(@Nullable Object viewer);
    public abstract void removeViewer(@Nullable Object viewer);
    public abstract void clearViewers();
    public abstract Collection<?> getViewers();

    protected abstract void _sendLine(Object platformNative);
    public void println(Object... line) {
        _sendLine(Logging.builder().append(line).toPlatform());
    }

    protected static int spindex = 0;
    protected static String[] spinner = {" \u2591 ", " \u2592 ", " \u2593 ", " \u2588 ", " \u2593 ", " \u2592 "};
    protected JobManager() {
        SharedInstances.getAsyncExecutor().scheduleAtFixedRate(()->{
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
        Logging.MessageBuilder msg = Logging.builder();
        {
            String tmp = String.format("%d", (int)prog);
            String padDummy = "   ";
            msg.append(" [", Logging.Color.AQUA,padDummy.substring(tmp.length()),tmp,"%",Logging.Color.RESET,"]");
        }
        msg.append(spinner[spindex++]); if (spindex>=spinner.length) spindex=0;
        {
            String left = "####################";
            String right = "--------------------";
            int a = (int)(prog/5);
            int b = 20-a;
            msg.append(Logging.Color.DARK_GREEN, left.substring(0,a));
            if (prog%1>0.5) {
                msg.append(Logging.Color.GREEN, ':');
                b--;
            }
            msg.append(Logging.Color.RESET);
            if (b>0)
                msg.append(right.substring(0,b));
        }
        if (more != null && !more.isEmpty())
            msg.append(" ", more);
        _sendLine(msg.toPlatform());
    }

    public boolean runJob(@Nullable Object initialViewer, AbstractJob job) {
        synchronized (jobMutex) {
            if (currentJob != null && !watchdog.isDone()) return false;
            clearViewers();
            if (PlatformProbe.isSponge()) addViewer(initialViewer);
            currentJob = job;
            watchdog = CompletableFuture.runAsync(job, SharedInstances.getAsyncExecutor())
                    .thenAccept(ignore->{
                        printUpdate(100f, "DONE"); //display final note
                        clearViewers(); //kick all members
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
    public CompletableFuture<Boolean> hang() {
        synchronized (hangMutex) {
            if (hangResult != null)
                throw new IllegalStateException("There's already a hung job!");
//            printUpdate(currentJob.getProgress(), currentJob.getMessage()); //display chat users
            return hangResult = new CompletableFuture<>();
        }
    }
    /** to be called on timeout or result */
    public void unhang() {
        synchronized (hangMutex) {
            if (hangResult != null) {
                hangResult.completeExceptionally(new TimeoutException("The job confirmation timed out"));
                hangResult = null;
            }
        }
    }
    public boolean isAwaitingConfirmation() {
        synchronized (hangMutex) {
            return hangResult != null;
        }
    }
    /**
     * resume a currently hanging job, if confirm is false
     * the job is supposed to terminate. Since this is supposed to
     * be called from a command, this will throw a CommandException if
     * there is currently no job hanging
     * @param confirm true if the job shall continue
     */
    public void resumeJob(boolean confirm) throws IllegalStateException {
        synchronized (hangMutex) {
            if (hangResult == null)
                throw new IllegalStateException("There's currently no Job awaiting confirmation");
            hangResult.complete(confirm);
        }
    }
}
