package de.dosmike.sponge.oreget.jobs;

import de.dosmike.sponge.oreget.multiplatform.JobManager;
import de.dosmike.sponge.oreget.multiplatform.Logging;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface AbstractJob extends Runnable {

    /** @return the progress in % (0-100) */
    float getProgress();
    /** Optional insight in what this job is currently doing, can return null.
     * The job might just message viewers directly
     * @return one-liner on what is happening */
    String getMessage();

    /**
     * Freezes the current job, awaiting manual confirmation
     * @return true if execution should continue
     */
    default boolean confirm() {
        try {
            return JobManager.get().hang().get(60, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            JobManager.get().println(Logging.Color.YELLOW, "Confirmation timed out!");
        } finally {
            JobManager.get().unhang();
        }
        return false;
    }

}
