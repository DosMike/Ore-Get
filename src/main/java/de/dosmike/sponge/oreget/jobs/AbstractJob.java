package de.dosmike.sponge.oreget.jobs;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

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
            JobManager.get().println(Text.of(TextColors.YELLOW, "Confirmation timed out!"));
        } finally {
            JobManager.get().unhang();
        }
        return false;
    }

}
