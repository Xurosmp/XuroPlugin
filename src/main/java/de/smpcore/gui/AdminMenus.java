package de.smpcore.gui;

import de.smpcore.crate.CrateManager;
import de.smpcore.data.PlayerData;
import de.smpcore.economy.EconomyManager;
import de.smpcore.util.Items;
import de.smpcore.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public final class AdminMenus {
    private AdminMenus() {}

    private static boolean admin(Player p) { return p.hasPermission("smpcore.admin"); }

    private static void log(Player admin, String msg) {
        de.smpcore.SMPCore.get().getLogger().info("[Admin] " + admin.getName() + ": " + msg);
    }

    public static final class Main extends Menu {
        public Main(Player v) { super(v, 27, "<red><bold>Admin Panel"); }
        @Override protected boolean allowed() { return admin(viewer); }

        @Override
        protected void build() {
            set(10, Items.make(Material.PLAYER_HEAD, "<aqua>Spieler verwalten", "<gray>Geld, Kristalle, Keys, Teleport ..."), e -> go(new Players(viewer)));
            set(11, Items.make(Material.SKELETON_SKULL, "<red>Kopfgelder", "<gray>Anzeigen & entfernen"), e -> go(new Bounties(viewer)));
            set(12, Items.make(Material.CHEST, "<gold>Crates", "<gray>Keys an dich geben"), e -> go(new Crates(viewer)));
            set(13, Items.make(Material.COMMAND_BLOCK, "<light_purple>Server-Tools", "<gray>Features, Statistik, Reload"), e -> go(new Tools(viewer)));
            fill(Items.filler());
        }
    }

    public static final class Players extends PagedMenu {
        public Players(Player v) { super(v, "<red>Admin » Spieler"); }
        @Override protected boolean allowed() { return admin(viewer); }

        @Override
        protected List<Entry> entries() {
            List<Entry> out = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerData d = plugin.data().get(p.getUniqueId());
                if (d == null) continue;
                out.add(new Entry(Items.head(p.getUniqueId(), "<aqua>" + Msg.esc(p.getName()), List.of(
                        "<gray>Geld: <green>" + EconomyManager.fmt(d.money),
                        "<gray>Kristalle: <light_purple>" + d.crystals,
                        "", "<yellow>Klicken zum Verwalten")), e -> go(new Target(viewer, p.getUniqueId()))));
            }
            return out;
        }

        @Override
        protected void controls() { set(49, Items.make(Material.BARRIER, "<red>Zurück"), e -> go(new Main(viewer))); }
    }

    public static final class Target extends Menu {
        private final UUID target;

        private static String title(UUID id) {
            PlayerData d = de.smpcore.SMPCore.get().data().get(id);
            return "<red>Admin » <white>" + Msg.esc(d == null ? "?" : d.name);
        }

        public Target(Player v, UUID target) {
            super(v, 54, title(target));
            this.target = target;
        }
        @Override protected boolean allowed() { return admin(viewer); }

        @Override
        protected void build() {
            PlayerData d = plugin.data().get(target);
            if (d == null) { closeLater(); return; }
            Player online = Bukkit.getPlayer(target);
            set(4, Items.head(target, "<aqua>" + Msg.esc(d.name), List.of(
                    "<gray>Geld: <green>" + EconomyManager.fmt(d.money),
                    "<gray>Kristalle: <light_purple>" + d.crystals,
                    "<gray>Kills/Tode: <white>" + d.kills + "/" + d.deaths,
                    "<gray>Kopfgeld: <red>" + EconomyManager.fmt(plugin.bounties().get(target)),
                    "<gray>Online: " + (online != null ? "<green>ja" : "<red>nein"))));

            double[] money = {1_000, 10_000, 100_000, 1_000_000};
            String[] ml = {"1K", "10K", "100K", "1M"};
            for (int i = 0; i < 4; i++) {
                final double a = money[i];
                final String l = ml[i];
                set(9 + i, Items.make(Material.LIME_STAINED_GLASS_PANE, "<green>+$" + l), e -> {
                    plugin.economy().add(target, a); log(viewer, "+" + a + " Geld für " + d.name); refresh();
                });
                set(14 + i, Items.make(Material.RED_STAINED_GLASS_PANE, "<red>-$" + l), e -> {
                    plugin.economy().set(target, plugin.economy().balance(target) - a); log(viewer, "-" + a + " Geld von " + d.name); refresh();
                });
            }
            long[] cr = {1, 10, 100};
            for (int i = 0; i < 3; i++) {
                final long a = cr[i];
                set(18 + i, Items.make(Material.AMETHYST_SHARD, "<light_purple>+" + a + " Kristalle"), e -> {
                    plugin.economy().addCrystals(target, a); log(viewer, "+" + a + " Kristalle für " + d.name); refresh();
                });
                set(24 + i, Items.make(Material.REDSTONE, "<red>-" + a + " Kristalle"), e -> {
                    plugin.economy().setCrystals(target, plugin.economy().crystals(target) - a); log(viewer, "-" + a + " Kristalle von " + d.name); refresh();
                });
            }
            int slot = 27;
            for (CrateManager.Crate c : plugin.crates().all()) {
                if (slot > 35) break;
                set(slot++, Items.make(Material.TRIPWIRE_HOOK, c.keyName,
                        "<gray>Keys: <white>" + plugin.crates().keys(target, c.id), "<yellow>Klick: +1 Key"), e -> {
                    plugin.crates().addKeys(target, c.id, 1); log(viewer, "+1 Key " + c.id + " für " + d.name); refresh();
                });
            }
            set(45, Items.make(Material.ENDER_PEARL, "<aqua>Zu Spieler teleportieren"), e -> {
                Player t = Bukkit.getPlayer(target);
                if (t == null) { Msg.error(viewer, "Spieler ist offline."); return; }
                closeLater(); viewer.teleportAsync(t.getLocation());
            });
            set(46, Items.make(Material.ENDER_EYE, "<aqua>Spieler zu mir holen"), e -> {
                Player t = Bukkit.getPlayer(target);
                if (t == null) { Msg.error(viewer, "Spieler ist offline."); return; }
                t.teleportAsync(viewer.getLocation()); log(viewer, "holte " + d.name);
            });
            set(47, Items.make(Material.GOLDEN_APPLE, "<green>Heilen & Sättigen"), e -> {
                Player t = Bukkit.getPlayer(target);
                if (t == null) { Msg.error(viewer, "Spieler ist offline."); return; }
                healFeed(t); log(viewer, "heilte " + d.name);
            });
            set(48, Items.make(Material.SKELETON_SKULL, "<red>Kopfgeld löschen", "<gray>Aktuell: " + EconomyManager.fmt(plugin.bounties().get(target))), e -> {
                plugin.bounties().remove(target); log(viewer, "löschte Kopfgeld von " + d.name); refresh();
            });
            set(49, Items.make(Material.BARRIER, "<red>Zurück"), e -> go(new Players(viewer)));
            set(53, Items.make(Material.TNT, "<dark_red>Kicken"), e -> {
                Player t = Bukkit.getPlayer(target);
                if (t == null) { Msg.error(viewer, "Spieler ist offline."); return; }
                go(new ConfirmMenu(viewer, "<dark_red>Wirklich kicken?", Items.make(Material.TNT, "<red>" + Msg.esc(d.name) + " kicken"),
                        () -> { t.kick(Msg.c("<red>Vom Admin gekickt.")); log(viewer, "kickte " + d.name); },
                        () -> new Players(viewer)));
            });
            fill(Items.filler());
        }

        @SuppressWarnings("deprecation")
        private static void healFeed(Player t) {
            t.setHealth(t.getMaxHealth());
            t.setFoodLevel(20);
            t.setSaturation(20f);
        }
    }

    public static final class Bounties extends PagedMenu {
        public Bounties(Player v) { super(v, "<red>Admin » Kopfgelder"); }
        @Override protected boolean allowed() { return admin(viewer); }

        @Override
        protected List<Entry> entries() {
            List<Entry> out = new ArrayList<>();
            for (Map.Entry<UUID, Double> en : plugin.bounties().all().entrySet()) {
                PlayerData d = plugin.data().get(en.getKey());
                out.add(new Entry(Items.head(en.getKey(), "<red>" + Msg.esc(d == null ? "?" : d.name), List.of(
                        "<gray>Kopfgeld: <green>" + EconomyManager.fmt(en.getValue()), "", "<yellow>Klicken zum Entfernen")),
                        e -> { plugin.bounties().remove(en.getKey()); log(viewer, "entfernte Kopfgeld"); refresh(); }));
            }
            return out;
        }

        @Override
        protected void controls() { set(49, Items.make(Material.BARRIER, "<red>Zurück"), e -> go(new Main(viewer))); }
    }

    public static final class Crates extends PagedMenu {
        public Crates(Player v) { super(v, "<red>Admin » Crates"); }
        @Override protected boolean allowed() { return admin(viewer); }

        @Override
        protected List<Entry> entries() {
            List<Entry> out = new ArrayList<>();
            for (CrateManager.Crate c : plugin.crates().all()) {
                out.add(new Entry(Items.make(c.icon, c.name, "<gray>ID: <white>" + c.id,
                        "<gray>Deine Keys: <white>" + plugin.crates().keys(viewer.getUniqueId(), c.id), "", "<yellow>Klick: +1 Key für dich"),
                        e -> { plugin.crates().addKeys(viewer.getUniqueId(), c.id, 1); refresh(); }));
            }
            return out;
        }

        @Override
        protected void controls() {
            set(49, Items.make(Material.BARRIER, "<red>Zurück"), e -> go(new Main(viewer)));
            set(51, Items.make(Material.PAPER, "<gray>Crate platzieren", "<gray>Block anschauen:", "<white>/crate set <id>"));
        }
    }

    public static final class Tools extends Menu {
        private static final String[][] FEATURES = {
                {"shop", "Shop"}, {"auction", "Auktionshaus"}, {"bounty", "Kopfgelder"}, {"crates", "Crates"}, {"scoreboard", "Scoreboard"}};

        public Tools(Player v) { super(v, 36, "<red>Admin » Server-Tools"); }
        @Override protected boolean allowed() { return admin(viewer); }

        @Override
        protected void build() {
            int slot = 10;
            for (String[] f : FEATURES) {
                boolean on = plugin.feature(f[0]);
                set(slot++, Items.make(on ? Material.LIME_DYE : Material.GRAY_DYE, (on ? "<green>" : "<red>") + f[1] + (on ? " AN" : " AUS"),
                        "<gray>Klicken zum Umschalten"), e -> {
                    plugin.getConfig().set("features." + f[0], !plugin.feature(f[0]));
                    plugin.saveConfig();
                    log(viewer, "Feature " + f[0] + " umgeschaltet");
                    refresh();
                });
            }
            double money = 0;
            long crystals = 0;
            for (PlayerData d : plugin.data().all()) { money += d.money; crystals += d.crystals; }
            set(20, Items.make(Material.GOLD_BLOCK, "<gold>Economy-Übersicht",
                    "<gray>Spieler: <white>" + plugin.data().all().size(),
                    "<gray>Gesamt-Geld: <green>" + EconomyManager.fmt(money),
                    "<gray>Gesamt-Kristalle: <light_purple>" + crystals,
                    "<gray>Offene Kopfgelder: <red>" + plugin.bounties().all().size()));
            set(24, Items.make(Material.REDSTONE, "<yellow>Konfiguration neu laden", "<gray>config, shop, crates, crystalshop"), e -> {
                plugin.reloadAll();
                Msg.ok(viewer, "Neu geladen!");
            });
            set(25, Items.make(Material.WRITABLE_BOOK, "<aqua>Jetzt speichern"), e -> { plugin.saveAll(true); Msg.ok(viewer, "Gespeichert!"); });
            set(31, Items.make(Material.BARRIER, "<red>Zurück"), e -> go(new Main(viewer)));
            fill(Items.filler());
        }
    }
}
