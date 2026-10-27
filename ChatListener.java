package de.smpcore.listeners;

import de.smpcore.SMPCore;
import de.smpcore.rank.RankManager;
import de.smpcore.util.Msg;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Chat-Format mit Rang-Prefix. */
public final class ChatListener implements Listener {
    private final SMPCore plugin;

    public ChatListener(SMPCore plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent e) {
        if (!plugin.getConfig().getBoolean("chat.rank-format", true)) return;
        Player p = e.getPlayer();
        RankManager.Rank r = plugin.ranks().rankOf(p.getUniqueId());
        String prefix = r == null ? "" : r.prefix;
        e.renderer(ChatRenderer.viewerUnaware((source, name, message) ->
                Msg.c(prefix + Msg.esc(source.getName()) + "<dark_gray> » <white>").append(message)));
    }
}
