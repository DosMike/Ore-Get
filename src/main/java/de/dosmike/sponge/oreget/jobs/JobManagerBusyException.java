package de.dosmike.sponge.oreget.jobs;

/** This is a runtime exception because some implementations do not have to worry
 * about running multiple tasks at once */
public class JobManagerBusyException extends RuntimeException {
    public JobManagerBusyException() {
        super();
    }

    public JobManagerBusyException(String message) {
        super(message);
    }

    public JobManagerBusyException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobManagerBusyException(Throwable cause) {
        super(cause);
    }
}
