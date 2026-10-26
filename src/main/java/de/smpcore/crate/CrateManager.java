package de.smpcore.crate;

import de.smpcore.SMPCore;
import de.smpcore.data.PlayerData;
import de.smpcore.gui.CrateMenus;
import de.smpcore.util.Items;
import de.smpcore.util.Msg;
import de.smpcore.util.Num;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class CrateManager {
    public record Reward(String type, ItemStack icon, double weight, double amount, String command, ItemStack give, String label) {}

    public static final class Crate {
        public final String id, name, keyName;
        public final Material icon;
        public final List<Reward> rewards = new ArrayList<>();
        public double total;

        public Crate(String id, String name, String keyName, Material icon) {
            this.id = id; this.name = name; this.keyName = keyName; this.icon = icon;
        }

        public double chance(Reward r) { return total <= 0 ? 0 : r.weight() / total * 100.0; }

        public Reward pick() {
            double r = ThreadLocalRandom.current().nextDouble() * total;
            for (Reward x : rewards) {
                r -= x.weight();
                if (r < 0) return x;
            }
            return rewards.get(rewards.size() - 1);
        }
    }

    private final SMPCore plugin;
    private final Map<String, Crate> crates = new LinkedHashMap<>();

    public CrateManager(SMPCore plugin) { this.plugin = plugin; }

    public void load() {
        crates.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "crates.yml"));
        ConfigurationSection cs = y.getConfigurationSection("crates");
        if (cs == null) return;
        for (String id : cs.getKeys(false)) {
            ConfigurationSection c = cs.getConfigurationSection(id);
            if (c == null) continue;
            Material icon = Material.matchMaterial(c.getString("icon", "CHEST"));
            Crate crate = new Crate(id, c.getString("name", id), c.getString("key-name", "<white>" + id + " Key"),
                    icon == null ? Material.CHEST : icon);
            for (Map<?, ?> m : c.getMapList("rewards")) {
                Reward r = parseReward(m);
                if (r != null) { crate.rewards.add(r); crate.total += r.weight(); }
            }
            if (crate.rewards.isEmpty()) {
                plugin.getLogger().warning("Crate '" + id + "' hat keine gültigen Rewards.");
                continue;
            }
            crates.put(id.toLowerCase(Locale.ROOT), crate);
        }
    }

    private static String str(Map<?, ?> m, String k, String def) {
        Object o = m.get(k);
        return o == null ? def : String.valueOf(o);
    }
    private static double num(Object o) { return o instanceof Number n ? n.doubleValue() : 0; }

    private Reward parseReward(Map<?, ?> m) {
        String type = str(m, "type", "ITEM").toUpperCase(Locale.ROOT);
        double chance = num(m.get("chance"));
        if (chance <= 0) return null;
        double amount = num(m.get("amount"));
        if (amount <= 0) amount = 1;
        String custom = m.get("name") == null ? null : String.valueOf(m.get("name"));
        if (type.equals("ITEM")) {
            Material mt = Material.matchMaterial(str(m, "material", "STONE"));
            if (mt == null || !mt.isItem()) return null;
            ItemStack give = new ItemStack(mt, (int) amount);
            if (custom != null) give = Items.named(give, custom);
            String label = custom != null ? custom : (int) amount + "x " + Items.pretty(mt);
            return new Reward(type, give.clone(), chance, amount, null, give, label);
        }
        if (type.equals("MONEY")) {
            String label = "$" + Num.format(amount);
            return new Reward(type, Items.make(Material.GOLD_INGOT, "<gold>" + label), chance, amount, null, null, label);
        }
        if (type.equals("CRYSTALS")) {
            String label = Num.format(amount) + " Kristalle";
            return new Reward(type, Items.make(Material.AMETHYST_SHARD, "<light_purple>" + label), chance, amount, null, null, label);
        }
        if (type.equals("COMMAND")) {
            String cmd = str(m, "command", null);
            if (cmd == null) return null;
            Material mt = Material.matchMaterial(str(m, "icon", "PAPER"));
            String label = custom != null ? custom : cmd;
            return new Reward(type, Items.make(mt == null ? Material.PAPER : mt, label), chance, amount, cmd, null, label);
        }
        return null;
    }

    public Collection<Crate> all() { return crates.values(); }
    public Crate get(String id) { return id == null ? null : crates.get(id.toLowerCase(Locale.ROOT)); }

    // ---- Keys ----
    public int keys(UUID id, String crate) {
        PlayerData d = plugin.data().get(id);
        return d == null ? 0 : d.keys.getOrDefault(crate.toLowerCase(Locale.ROOT), 0);
    }

    public void addKeys(UUID id, String crate, int amount) {
        PlayerData d = plugin.data().get(id);
        if (d == null) return;
        String k = crate.toLowerCase(Locale.ROOT);
        int n = Math.max(0, d.keys.getOrDefault(k, 0) + amount);
        if (n == 0) d.keys.remove(k);
        else d.keys.put(k, n);
    }

    // ---- Öffnen ----
    public void open(Player p, Crate c) {
        if (keys(p.getUniqueId(), c.id) <= 0) {
            Msg.error(p, "Du hast keinen " + "Key für diese Crate! Keys gibt es im Kristall-Shop (/crystalshop).");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        addKeys(p.getUniqueId(), c.id, -1);
        Reward r = c.pick();
        give(p, r);
        new CrateMenus.Roll(p, c, r).start();
    }

    public void give(Player p, Reward r) {
        switch (r.type()) {
            case "ITEM" -> p.getInventory().addItem(r.give().clone())
                    .values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
            case "MONEY" -> plugin.economy().add(p.getUniqueId(), r.amount());
            case "CRYSTALS" -> plugin.economy().addCrystals(p.getUniqueId(), (long) r.amount());
            case "COMMAND" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), r.command().replace("%player%", p.getName()));
            default -> { }
        }
    }

    // ---- Partikel an Crate-Blöcken ----
    public void startParticles() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (String key : plugin.data().crateBlocks().keySet()) {
                String[] s = key.split(";");
                if (s.length < 4) continue;
                World w = Bukkit.getWorld(s[0]);
                if (w == null) continue;
                try {
                    int x = Integer.parseInt(s[1]), y = Integer.parseInt(s[2]), z = Integer.parseInt(s[3]);
                    if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
                    w.spawnParticle(Particle.END_ROD, x + 0.5, y + 1.2, z + 0.5, 2, 0.25, 0.25, 0.25, 0.01);
                } catch (NumberFormatException ignored) { }
            }
        }, 40L, 20L);
    }
}
