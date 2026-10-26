package de.smpcore.commands;

import de.smpcore.SMPCore;
import de.smpcore.gui.AdminMenus;
import de.smpcore.util.Msg;
import de.smpcore.util.Players;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

/** adminpanel, smpcore */
public final class AdminCommand implements TabExecutor {
    private final SMPCore plugin;

    public AdminCommand(SMPCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (c.getName().equalsIgnoreCase("adminpanel")) {
            if (!(s instanceof Player p)) { Msg.error(s, "Nur für Spieler."); return true; }
            if (!p.hasPermission("smpcore.admin")) { Msg.error(s, "Keine Rechte."); return true; }
            new AdminMenus.Main(p).open();
            return true;
        }
        // /smpcore
        if (a.length > 0 && a[0].equalsIgnoreCase("reload")) {
            if (!s.hasPermission("smpcore.admin")) { Msg.error(s, "Keine Rechte."); return true; }
            plugin.reloadAll();
            Msg.ok(s, "SMPCore neu geladen.");
        } else if (a.length > 0 && a[0].equalsIgnoreCase("save")) {
            if (!s.hasPermission("smpcore.admin")) { Msg.error(s, "Keine Rechte."); return true; }
            plugin.saveAll(true);
            Msg.ok(s, "Daten gespeichert.");
        } else {
            Msg.send(s, "SMPCore <white>v" + plugin.getPluginMeta().getVersion() + " <gray>– Admin: <white>/ap, /smpcore reload|save");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        if (c.getName().equalsIgnoreCase("smpcore") && a.length == 1) return Players.complete(a[0], List.of("reload", "save"));
        return List.of();
    }
}
