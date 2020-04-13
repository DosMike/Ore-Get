package de.dosmike.sponge.oreget.multiplatform.terminal;

import de.dosmike.sponge.oreget.multiplatform.JobManager;
import de.dosmike.sponge.oreget.multiplatform.Logging;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * knows if a job is running, holds "viewers" of the job and logs into both
 * the server console and to the "viewers". also handles hand/unhang for confirmations
 */
public class TerminalJobManager extends JobManager {

    @Override
    public void addViewer(@Nullable Object viewer) {
        if (viewer != null)
            throw new UnsupportedOperationException();
    }

    @Override
    public void removeViewer(@Nullable Object viewer) {
        if (viewer != null)
            throw new UnsupportedOperationException();
    }

    @Override
    public void clearViewers() {
    }

    @Override
    public Collection<?> getViewers() {
        return Collections.emptyList();
    }

    @Override
    protected void _sendLine(Object platformNative) {
        Logging.log(null, platformNative);
    }

    public TerminalJobManager() {
        super();
    }

    public void println(Object... line) {
        _sendLine(Logging.builder().append(line).toPlatform());
        if (!statusLine.isEmpty()) _sendLine(statusLine);
    }

    private String statusLine = "";
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
        statusLine = msg.toPlatform().toString()+"\r";
        _sendLine(statusLine);
    }

    @Override
    public CompletableFuture<Boolean> hang() {
        try {
            return super.hang();
        } finally {
            _sendLine("\r"); // clear status-line for input
        }
    }
}
