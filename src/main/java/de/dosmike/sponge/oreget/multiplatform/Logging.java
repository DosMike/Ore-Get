package de.dosmike.sponge.oreget.multiplatform;

import de.dosmike.sponge.oreget.multiplatform.terminal.TerminalLogging;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * The logging target <code>null</code> should on all Platforms be substituted with the console/terminal, since the
 * terminal implementation does not have any concept of a "Console"-Object, it's just stdout.
 */
public abstract class Logging {

    public enum Color {
        AQUA,
        BLACK,
        BLUE,
        DARK_AQUA,
        DARK_BLUE,
        DARK_GRAY,
        DARK_GREEN,
        DARK_PURPLE,
        DARK_RED,
        GOLD,
        GRAY,
        GREEN,
        LIGHT_PURPLE,
        RED,
        RESET,
        WHITE,
        YELLOW
    }
    public enum Style {
        BOLD,
        ITALIC,
        UNDERLINE,
        STRIKETHROUGH,
        RESET
    }
    public static class Clickable {
        Object[] parts;
        String commandAction;
        String hoverText = null;
        public Clickable(String action, Object... parts) {
            this.parts = parts;
            this.commandAction = action;
        }
        public Clickable withTooltip(String hoverText) {
            this.hoverText = hoverText;
            return this;
        }
        public Object[] getParts() {
            return parts;
        }
        public String getCommandAction() {
            return commandAction;
        }
        public String getTooltip() {
            return hoverText;
        }
    }

    protected abstract Object _toPlatform(Color color);
    protected abstract Object _toPlatform(Style style);
    protected abstract Object _toPlatform(Clickable clickable);
    protected abstract Object _compose(Object... parts);

    protected abstract void _log(@Nullable Object target, Object... parts);
    protected abstract void _logLines(@Nullable Object target, Collection<?> nativeLines);

    private static Logging _instance = null;
    private synchronized static Logging _getOrCreate() {
        if (_instance == null)
            _instance = PlatformProbe.createInstance("de.dosmike.sponge.oreget.multiplatform.sponge.SpongeLogging",
                TerminalLogging.class);
        return _instance;
    }
    public static void log(@Nullable Object target, Object... parts) {
        _getOrCreate()._log(target,parts);
    }
    public static void logLines(@Nullable Object target, Collection<?> nativeLines) {
        _getOrCreate()._logLines(target, nativeLines);
    }

    //Region builder
    public static class MessageBuilder {
        List<Object> nativeCache = new LinkedList<>();
        private MessageBuilder() {}

        public MessageBuilder append(Color color) {
            nativeCache.add(_getOrCreate()._toPlatform(color));
            return this;
        }
        public MessageBuilder append(Style style) {
            nativeCache.add(_getOrCreate()._toPlatform(style));
            return this;
        }
        public MessageBuilder append(Clickable clickable) {
            nativeCache.add(_getOrCreate()._toPlatform(clickable));
            return this;
        }
        public MessageBuilder append(Object... parts) {
            nativeCache.add(_getOrCreate()._compose(parts));
            return this;
        }

        public Object toPlatform(){
            return _getOrCreate()._compose(nativeCache.toArray(new Object[0]));
        }

        public void log(Object... targets) {
            if (targets.length==0) _getOrCreate()._log(null, nativeCache.toArray(new Object[0]));
            else for (Object target : targets) _getOrCreate()._log(target, nativeCache.toArray(new Object[0]));
        }
        public void log(Collection<Object> targets) {
            if (targets.size()==0) _getOrCreate()._log(null, nativeCache.toArray(new Object[0]));
            else for (Object target : targets) _getOrCreate()._log(target, nativeCache.toArray(new Object[0]));
        }
    }

    public static MessageBuilder builder() {
        return new MessageBuilder();
    }
    //endregion

}
