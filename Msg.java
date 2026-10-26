package de.smpcore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public final class Msg {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static String prefix = "<dark_gray>[</dark_gray><aqua>SMP</aqua><dark_gray>]</dark_gray> <gray>";

    private Msg() {}

    public static void setPrefix(String p) { prefix = p; }
    public static Component c(String s) { return MM.deserialize(s); }
    public static Component item(String s) { return MM.deserialize("<!italic>" + s); }
    public static String esc(String s) { return MM.escapeTags(s); }

    public static void send(CommandSender to, String msg) { to.sendMessage(c(prefix + msg)); }
    public static void ok(CommandSender to, String msg) { to.sendMessage(c(prefix + "<green>" + msg)); }
    public static void error(CommandSender to, String msg) { to.sendMessage(c(prefix + "<red>" + msg)); }
}
