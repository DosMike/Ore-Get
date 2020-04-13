package de.dosmike.sponge.oreget.multiplatform.sponge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextRepresentable;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class SpongeLogging extends de.dosmike.sponge.oreget.multiplatform.Logging {

    @Override
    public Object _toPlatform(@NotNull de.dosmike.sponge.oreget.multiplatform.Logging.Color color) {
        switch (color) {
            case AQUA: return TextColors.AQUA;
            case BLACK: return TextColors.BLACK;
            case BLUE: return TextColors.BLUE;
            case DARK_AQUA: return TextColors.DARK_AQUA;
            case DARK_BLUE: return TextColors.DARK_BLUE;
            case DARK_GRAY: return TextColors.DARK_GRAY;
            case DARK_GREEN: return TextColors.DARK_GREEN;
            case DARK_PURPLE: return TextColors.DARK_PURPLE;
            case DARK_RED: return TextColors.DARK_RED;
            case GOLD: return TextColors.GOLD;
            case GRAY: return TextColors.GRAY;
            case GREEN: return TextColors.GREEN;
            case LIGHT_PURPLE: return TextColors.LIGHT_PURPLE;
            case RED: return TextColors.RED;
            case RESET: return TextColors.RESET;
            case WHITE: return TextColors.WHITE;
            case YELLOW: return TextColors.YELLOW;
            default: throw new IllegalStateException();
        }
    }

    @Override
    public Object _toPlatform(Style style) {
        switch (style) {
            case BOLD: return TextStyles.BOLD;
            case ITALIC: return TextStyles.ITALIC;
            case UNDERLINE: return TextStyles.UNDERLINE;
            case STRIKETHROUGH: return TextStyles.STRIKETHROUGH;
            case RESET: return TextStyles.RESET;
            default: throw new IllegalStateException();
        }
    }

    @Override
    public Object _compose(Object... parts) {
        Object[] spongeNative = new Object[parts.length];
        for (int i=0;i<parts.length;i++) {
            if (parts[i] instanceof Color) {
                spongeNative[i] = _toPlatform((Color)parts[i]);
            } else if (parts[i] instanceof Style) {
                spongeNative[i] = _toPlatform((Style) parts[i]);
            } else if (parts[i] instanceof Clickable) {
                spongeNative[i] = _toPlatform((Clickable) parts[i]);
            } else if (parts[i] instanceof TextRepresentable) {
                spongeNative[i] = parts[i];
            } else {
                spongeNative[i] = Text.of(parts[i]);
            }
        }
        return Text.of(spongeNative);
    }

    @Override
    public Object _toPlatform(Clickable clickable) {
        Text.Builder builder =  Text.builder()
                .append((Text) _compose(clickable.getParts()));
        if (clickable.getCommandAction().startsWith("https://"))
            try {
                builder.onClick(TextActions.openUrl(new URL(clickable.getCommandAction())));
            } catch (MalformedURLException ignore) {}
        else
            builder.onClick(TextActions.runCommand(clickable.getCommandAction()));
        if (clickable.getTooltip()!=null)
            builder.onHover(TextActions.showText(Text.of(clickable.getTooltip())));
        return builder.build();
    }

    @Override
    public synchronized void _log(@Nullable Object target, Object... parts) {
        CommandSource out = (target instanceof CommandSource) ? (CommandSource)target : Sponge.getServer().getConsole();
        out.sendMessage(Text.of(_compose(parts)));
    }

    @Override
    protected synchronized void _logLines(@Nullable Object target, Collection<?> nativeLines) {
        CommandSource out = (target instanceof CommandSource) ? (CommandSource)target : Sponge.getServer().getConsole();
        List<Text> texts = new LinkedList<>();
        for (Object line : nativeLines) texts.add((Text)line);
        PaginationList.builder().title(Text.of("Ore Search"))
                .contents(texts)
                .padding(Text.of("="))
                .linesPerPage(14)
                .sendTo(out);
    }
}
