package de.dosmike.sponge.oreget.multiplatform.terminal;

import de.dosmike.sponge.oreget.Commands;
import de.dosmike.sponge.oreget.cache.PluginCache;
import de.dosmike.sponge.oreget.multiplatform.JobManager;
import de.dosmike.sponge.oreget.multiplatform.Logging;
import de.dosmike.sponge.oreget.multiplatform.PlatformProbe;
import de.dosmike.sponge.oreget.multiplatform.SharedInstances;
import de.dosmike.sponge.oreget.utils.ExitHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TerminalArgsParser {

    private static final Path oreDataCache = PlatformProbe.getCacheDirectory().resolve("terminal.session");
    public static void main(String[] args) {
        if (!System.getProperty("java.version").startsWith("1.8.")) {
            String JAVA_VERSION = System.getProperty("java.version");
            String JAVA_API_VERSION = JAVA_VERSION.split("\\.")[(JAVA_VERSION.startsWith("1.")) ? 1 : 0];
            System.out.println("Your Java Version ("+JAVA_API_VERSION+") is not supported. Please install and run OreGet with Java 8");
        }
        if (args.length < 1) { usage(); return; } //Provide interactive mode?
        boolean printStacktrace =
                "true".equalsIgnoreCase(System.getProperty("og_debug")) ||
                "debug".equalsIgnoreCase(System.getProperty("og_debug")) ;
        if (Files.exists(oreDataCache)) try {
            SharedInstances.getOre().importState(Files.newInputStream(oreDataCache));
        } catch (IOException|ClassNotFoundException e) {
            /*ignore*/
        }
        if (hasFlag("--dry-run", args, 0)) {
            args = withoutFlag("--dry-run", args, 0);
            PlatformProbe.FLAG_DRYRUN = true;
        }
        PluginCache.get().scanLoaded();
        ExitHandler.attach();
        int exitCode = 0;
        Future<?> busyWait = null;
        try {
            switch (args[0]) {
                case "search": {
                    busyWait = Commands.search(null, restAsJoined(args, 1));
                    break;
                }
                case "list": {
                    expectedEnd(args, 1);
                    busyWait = Commands.list(null);
                    break;
                }
                case "show": {
                    busyWait = Commands.show(null, nextAsLast(args, 1));
                    break;
                }
                case "upgrade": {
                    expectedEnd(args, 1);
                    busyWait = Commands.upgrade(null);
                    break;
                }
                case "full-upgrade": {
                    expectedEnd(args, 1);
                    busyWait = Commands.fullUpgrade(null);
                    break;
                }
                case "install": {
                    expectedMore(args, 1);
                    busyWait = Commands.install(null, hasFlag("--only-upgrade", args, 1), withoutFlags(args, 1));
                    break;
                }
                case "remove": {
                    expectedMore(args, 1);
                    busyWait = Commands.remove(null, false, Arrays.copyOfRange(args, 1, args.length));
                    break;
                }
                case "purge": {
                    expectedMore(args, 1);
                    busyWait = Commands.remove(null, true, Arrays.copyOfRange(args, 1, args.length));
                    break;
                }
                case "autoremove": {
                    expectedEnd(args, 1);
                    busyWait = Commands.autoremove(null);
                    break;
                }
                case "mark": {
                    expectedMore(args, 1);
                    for (int i = 1; i < args.length; i++)
                        Commands.mark(null, args[i], true);
                    break;
                }
                case "unmark": {
                    expectedMore(args, 1);
                    for (int i = 1; i < args.length; i++)
                        Commands.mark(null, args[i], false);
                    break;
                }
                default:
                    Logging.log(null, Logging.Color.RED, "Unknown sub-command: "+args[0]);
                    usage();
            }
        } catch (Exception e) {
            Logging.log(null, Logging.Color.RED, e.getMessage());
            if (printStacktrace) {
                e.printStackTrace();
            }
            exitCode=-1;
        }
        //optional 'async' input reading, that'll cancel when a job times out
        if (busyWait != null) {
            while (!JobManager.get().isIdle() || !busyWait.isDone()) {
                try { Thread.sleep(100); } catch (InterruptedException ignore) {}
                while (JobManager.get().isAwaitingConfirmation()) {
                    //get line of input unless the job times out
                    Future<String> asyncInput = SharedInstances.getAsyncExecutor().submit(()->{
                        try {
                            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                            return br.readLine();
                        } catch (IOException e) {
                            Logging.log(null, Logging.Color.RED, e.getMessage());
                            return "";
                        }
                    });
                    while (!asyncInput.isDone() && JobManager.get().isAwaitingConfirmation()) {
                        try { Thread.sleep(100); } catch (InterruptedException ignore) {}
                    }
                    //process input
                    if (asyncInput.isDone()) try {
                        String line = asyncInput.get();
                        if (line.isEmpty()) continue;
                        Pattern commandPrefix = Pattern.compile("^/?ore(?:-?get)?");
                        Matcher matcher = commandPrefix.matcher(line);
                        if (!matcher.find()) {
                            Commands.reject(null);
                        } else {
                            String argsJoined = line.substring(matcher.end()).trim();
                            if (argsJoined.equalsIgnoreCase("confirm")) {
                                Commands.confirm(null);
                            } else {
                                Commands.reject(null);
                            }
                        }
                    } catch (CancellationException | InterruptedException | ExecutionException ignore) {}
                }
            }
        }
        SharedInstances.getAsyncExecutor().shutdown();
        try {
            SharedInstances.getOre().exportState(Files.newOutputStream(oreDataCache));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(exitCode);
    }

    private static String restAsJoined(String[] args, int first) {
        if (first >= args.length) throw new IllegalArgumentException("Not enough arguments");
        return String.join(" ", Arrays.copyOfRange(args, first, args.length));
    }
    /** additionally throws if next starts with a dash (denoting flag) */
    private static String nextAsLast(String[] args, int index) {
        if (index < args.length-1) throw new IllegalArgumentException("Too many arguments, end expected after '"+args[index]+"'");
        else if (index >= args.length) throw new IllegalArgumentException("Not enough arguments");
        return args[index];
    }
    /** Checks if args contains a long flag (--name). short flags can be tested if specified separately (-a -b ...) */
    private static boolean hasFlag(String flag, String[] args, int first) {
        for (int i=first;i<args.length;i++) if (args[i].equalsIgnoreCase(flag)) return true; return false;
    }
    /** Removed a single flag from args, if present. Additionally shifts arguments by first = 0 */
    private static String[] withoutFlag(String flag, String[] args, int first) {
        List<String> temporary = new LinkedList<>(Arrays.asList(Arrays.copyOfRange(args, first, args.length)));
        temporary.removeIf(element->element.startsWith("-") && element.equalsIgnoreCase(flag));
        return temporary.toArray(new String[0]);
    }
    /** Note: this will reset the index to first = 0 */
    private static String[] withoutFlags(String[] args, int first) {
        List<String> temporary = new LinkedList<>(Arrays.asList(Arrays.copyOfRange(args, first, args.length)));
        temporary.removeIf(element->element.startsWith("-"));
        return temporary.toArray(new String[0]);
    }
    /** will throw if index is not at least one element after the last (pass index >= length) */
    private static void expectedEnd(String[] args, int index) {
        if (index < args.length) throw new IllegalArgumentException("Too many arguments, end expected after '"+args[index]+"'");
    }
    /** will throw if index is outside of range (pass index < length) */
    private static void expectedMore(String[] args, int index) {
        if (index >= args.length) throw new IllegalArgumentException("Not enough arguments");
    }

    private static void usage() {
        Logging.log(null, "You are using ",Logging.Color.GOLD,"OreGet");
        Logging.log(null, "Syntax: ",Logging.Color.DARK_AQUA,"java -jar oreget.jar ",Logging.Color.DARK_GREEN,"command ",Logging.Color.GOLD,"flags ",Logging.Color.DARK_PURPLE,"arguments");
        Logging.log(null, "The following commands are available:");
        Logging.log(null, "   ",Logging.Color.DARK_GREEN,"search ",Logging.Color.DARK_PURPLE,"text",Logging.Color.RESET,"         Search projects on Ore using the specified ",Logging.Color.DARK_PURPLE,"query",Logging.Color.RESET,".");
        Logging.log(null, "   ",Logging.Color.DARK_GREEN,"list ",Logging.Color.RESET,"               List all plugins found on the server.");
        Logging.log(null, "   ",Logging.Color.DARK_GREEN,"show ",Logging.Color.DARK_PURPLE,"plugin id",Logging.Color.RESET,"      Show local and remote information about the ",Logging.Color.DARK_PURPLE,"plugin",Logging.Color.RESET,".");
        Logging.log(null, "   ",Logging.Color.DARK_GREEN,"upgrade",Logging.Color.RESET,"             Update all plugins and install new dependencies if");
        Logging.log(null, "                       necessary.");
        Logging.log(null, "   ",Logging.Color.DARK_GREEN,"full-upgrade",Logging.Color.RESET,"        Like upgrade, but obsolete plugins will be removed");
        Logging.log(null, "                       (like with autoremove).");
        Logging.log(null, "   ",Logging.Color.DARK_GREEN,"install ",Logging.Color.DARK_PURPLE,"plugin ids",Logging.Color.RESET,"  Install ",Logging.Color.DARK_PURPLE,"plugins",Logging.Color.RESET," by id. You can install multiple");
        Logging.log(null, "                       plugins at once by separating the ids with space.");
        Logging.log(null, "                       Use ",Logging.Color.GOLD,"--only-upgrade",Logging.Color.RESET," to prevent installing the plugin.");
        Logging.log(null, "   ",Logging.Color.DARK_GREEN,"remove ",Logging.Color.DARK_PURPLE,"plugin ids",Logging.Color.RESET,"   The opposite to install. Will remove all ",Logging.Color.DARK_PURPLE,"plugins",Logging.Color.RESET,".");
        Logging.log(null, "   ",Logging.Color.DARK_GREEN,"purge ",Logging.Color.DARK_PURPLE,"plugin ids",Logging.Color.RESET,"    Will not only remove the ",Logging.Color.DARK_PURPLE,"plugins",Logging.Color.RESET,", but also try to");
        Logging.log(null, "                       remove config folders as well.");
        Logging.log(null, "   ",Logging.Color.DARK_GREEN,"autoremove",Logging.Color.RESET,"          Remove all dependencies that are no longer required.");
        Logging.log(null, "                       Dependencies are plugins installed automatically with");
        Logging.log(null, "                       other plugins, required to make the plugin function.");
        Logging.log(null, "   ",Logging.Color.DARK_GREEN,"mark ",Logging.Color.DARK_PURPLE,"plugin ids",Logging.Color.RESET,"     Marks all ",Logging.Color.DARK_PURPLE,"plugins",Logging.Color.RESET," as automatically installed.");
        Logging.log(null, "                       This means that autoremove can delete this plugin.");
        Logging.log(null, "   ",Logging.Color.DARK_GREEN,"unmark ",Logging.Color.DARK_PURPLE,"plugin ids",Logging.Color.RESET,"   Marks all ",Logging.Color.DARK_PURPLE,"plugins",Logging.Color.RESET," as manually installed.");
        Logging.log(null, "                       Autoremove is not allowed to touch these.");
        Logging.log(null, "You can use the global flag ",Logging.Color.GOLD,"--dry-run",Logging.Color.RESET," to prevent the post-script from running");
        Logging.log(null, "in case you want to see what updates would be available.");
    }

}
