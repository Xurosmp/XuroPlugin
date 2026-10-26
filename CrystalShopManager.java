package de.smpcore.shop;

import de.smpcore.SMPCore;
import de.smpcore.economy.EconomyManager;
import de.smpcore.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public final class CrystalShopManager {
    public record Entry(Material icon, String name, long cost, String type, String value, int amount, List<String> lore) {}

    private final SMPCore plugin;
    private final List<Entry> entries = new ArrayList<>();

    public CrystalShopManager(SMPCore plugin) { this.plugin = plugin; }

    public void load() {
        entries.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "crystalshop.yml"));
        for (Map<?, ?> m : y.getMapList("items")) {
            Material icon = Material.matchMaterial(String.valueOf(m.get("icon")));
            if (icon == null) icon = Material.AMETHYST_SHARD;
            Object cost = m.get("cost");
            if (!(cost instanceof Number cn)) continue;
            String type = String.valueOf(m.get("type")).toUpperCase(Locale.ROOT);
            String value = String.valueOf(m.get("value"));
            int amount = m.get("amount") instanceof Number a ? a.intValue() : 1;
            List<String> lore = new ArrayList<>();
            if (m.get("lore") instanceof List<?> l) for (Object o : l) lore.add(String.valueOf(o));
            entries.add(new Entry(icon, String.valueOf(m.get("name")), cn.longValue(), type, value, Math.max(1, amount), lore));
        }
    }

    public List<Entry> entries() { return entries; }

    public void purchase(Player p, Entry e) {
        UUID id = p.getUniqueId();
        // Validierung vor dem Bezahlen
        switch (e.type()) {
            case "KEY" -> {
                if (plugin.crates().get(e.value()) == null) { Msg.error(p, "Diese Crate existiert nicht."); return; }
            }
            case "ITEM" -> {
                if (Material.matchMaterial(e.value()) == null) { Msg.error(p, "Ungültiges Item im Shop."); return; }
            }
            case "MONEY" -> {
                try { Double.parseDouble(e.value()); } catch (NumberFormatException ex) { Msg.error(p, "Ungültiger Betrag im Shop."); return; }
            }
            case "COMMAND" -> { }
            default -> { Msg.error(p, "Ungültiger Shop-Eintrag."); return; }
        }
        if (!plugin.economy().takeCrystals(id, e.cost())) {
            Msg.error(p, "Nicht genug Kristalle! Du brauchst " + EconomyManager.fmtC(e.cost()));
            return;
        }
        switch (e.type()) {
            case "KEY" -> plugin.crates().addKeys(id, e.value(), e.amount());
            case "MONEY" -> plugin.economy().add(id, Double.parseDouble(e.value()) * e.amount());
            case "ITEM" -> {
                ItemStack it = new ItemStack(Material.matchMaterial(e.value()), e.amount());
                p.getInventory().addItem(it).values().forEach(l -> p.getWorld().dropItemNaturally(p.getLocation(), l));
            }
            case "COMMAND" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), e.value().replace("%player%", p.getName()));
            default -> { }
        }
        Msg.send(p, "<light_purple>Gekauft: <white>" + e.name() + " <gray>für <light_purple>" + EconomyManager.fmtC(e.cost()));
    }
}
