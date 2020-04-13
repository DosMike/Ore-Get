package de.dosmike.sponge.oreget.multiplatform.terminal;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class TerminalLogging extends de.dosmike.sponge.oreget.multiplatform.Logging {

    public TerminalLogging() {
        AnsiConsole.systemInstall();
    }

    private static final Character ESC = (char) 0x1B;
    @Override
    protected Object _toPlatform(@NotNull de.dosmike.sponge.oreget.multiplatform.Logging.Color color) {
        switch (color) {
            case AQUA: return ESC+"[96m";
            case BLACK: return ESC+"[30m";
            case BLUE: return ESC+"[94m";
            case DARK_AQUA: return ESC+"[36m";
            case DARK_BLUE: return ESC+"[34m";
            case DARK_GRAY: return ESC+"[90m";
            case DARK_GREEN: return ESC+"[32m";
            case DARK_PURPLE: return ESC+"[35m";
            case DARK_RED: return ESC+"[31m";
            case GOLD: return ESC+"[33m";
            case GRAY: return ESC+"[37m";
            case GREEN: return ESC+"[92m";
            case LIGHT_PURPLE: return ESC+"[95m";
            case RED: return ESC+"[91m";
            case RESET: return ESC+"[0m";
            case WHITE: return ESC+"[97m";
            case YELLOW: return ESC+"[93m";
            default: throw new IllegalStateException();
        }
    }

    @Override
    public Object _toPlatform(de.dosmike.sponge.oreget.multiplatform.Logging.Style style) {
        switch (style) {
            case BOLD: return "";
            case ITALIC: return ESC+"[3m";
            case UNDERLINE: return ESC+"[4m";
            case STRIKETHROUGH: return ESC+"[9m";
            case RESET: return ESC+"[0m";
            default: throw new IllegalStateException();
        }
    }

    @Override
    public Object _compose(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object part : parts) {
            if (part instanceof Color) {
                sb.append(_toPlatform((Color)part));
            } else if (part instanceof Style) {
                sb.append(_toPlatform((Style)part));
            } else if (part instanceof Clickable) {
                sb.append(_toPlatform((Clickable)part));
            } else {
                sb.append(part.toString());
            }
        }
        return sb.toString();
    }

    @Override
    public Object _toPlatform(de.dosmike.sponge.oreget.multiplatform.Logging.Clickable clickable) {
        return _compose(clickable.getParts());
    }

    private boolean lastLineCR = false;
    @Override
    public synchronized void _log(@Nullable Object target, Object... parts) {
        String composed = _compose(parts).toString();
        if (lastLineCR) { AnsiConsole.out.print(ESC+"[K"); lastLineCR = false; }
        AnsiConsole.out.print(composed+_toPlatform(Color.RESET).toString());
        if (!composed.endsWith("\r")) AnsiConsole.out.println();
        else lastLineCR = true;
    }
    @Override
    public synchronized void _logLines(@Nullable Object target, Collection<?> nativeLines) {
        for (Object line : nativeLines) {
            String composed = line.toString();
            if (lastLineCR) { AnsiConsole.out.print(ESC+"[K"); lastLineCR = false; }
            AnsiConsole.out.print(composed+_toPlatform(Color.RESET).toString());
            if (!composed.endsWith("\r")) AnsiConsole.out.println();
            else lastLineCR = true;
        }
    }

}
