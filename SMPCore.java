package de.smpcore;

import de.smpcore.auction.AuctionManager;
import de.smpcore.bounty.BountyManager;
import de.smpcore.commands.*;
import de.smpcore.crate.CrateManager;
import de.smpcore.data.DataManager;
import de.smpcore.data.PlayerData;
import de.smpcore.economy.EconomyManager;
import de.smpcore.gui.MenuListener;
import de.smpcore.listeners.CrateListener;
import de.smpcore.listeners.PlayerListener;
import de.smpcore.scoreboard.ScoreboardManager;
import de.smpcore.shop.CrystalShopManager;
import de.smpcore.shop.ShopManager;
import de.smpcore.teleport.TeleportManager;
import de.smpcore.util.Msg;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SMPCore extends JavaPlugin {
    private static SMPCore instance;
    private DataManager data;
    private EconomyManager economy;
    private ShopManager shop;
    private CrystalShopManager crystalShop;
    private AuctionManager auctions;
    private BountyManager bounties;
    private CrateManager crates;
    private ScoreboardManager scoreboards;
    private TeleportManager teleports;

    public static SMPCore get() { return instance; }
    public DataManager data() { return data; }
    public EconomyManager economy() { return economy; }
    public ShopManager shop() { return shop; }
    public CrystalShopManager crystalShop() { return crystalShop; }
    public AuctionManager auctions() { return auctions; }
    public BountyManager bounties() { return bounties; }
    public CrateManager crates() { return crates; }
    public ScoreboardManager scoreboards() { return scoreboards; }
    public TeleportManager teleports() { return teleports; }

    public boolean feature(String name) { return getConfig().getBoolean("features." + name, true); }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        for (String f : new String[]{"shop.yml", "crates.yml", "crystalshop.yml"}) {
            if (!new File(getDataFolder(), f).exists()) saveResource(f, false);
        }
        Msg.setPrefix(getConfig().getString("prefix", "<dark_gray>[</dark_gray><aqua>SMP</aqua><dark_gray>]</dark_gray> <gray>"));

        data = new DataManager(this);
        data.load();
        economy = new EconomyManager(data);
        shop = new ShopManager(this);
        shop.load();
        crystalShop = new CrystalShopManager(this);
        crystalShop.load();
        auctions = new AuctionManager(this);
        auctions.load();
        bounties = new BountyManager(this);
        bounties.load();
        crates = new CrateManager(this);
        crates.load();
        scoreboards = new ScoreboardManager(this);
        teleports = new TeleportManager(this);

        EconomyCommands eco = new EconomyCommands(this);
        for (String n : new String[]{"balance", "pay", "eco", "crystals", "top"}) reg(n, eco);
        ShopCommands sc = new ShopCommands(this);
        for (String n : new String[]{"shop", "sell", "crystalshop"}) reg(n, sc);
        reg("ah", new AuctionCommand(this));
        reg("bounty", new BountyCommand(this));
        reg("crate", new CrateCommand(this));
        AdminCommand ac = new AdminCommand(this);
        reg("adminpanel", ac);
        reg("smpcore", ac);
        TeleportCommands tc = new TeleportCommands(this);
        for (String n : new String[]{"spawn", "setspawn", "sethome", "home", "delhome", "homes", "tpa", "tpaccept", "tpdeny", "rtp"}) reg(n, tc);
        MiscCommands mc = new MiscCommands(this);
        for (String n : new String[]{"daily", "stats", "scoreboard"}) reg(n, mc);

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CrateListener(this), this);

        scoreboards.start();
        crates.startParticles();
        getServer().getScheduler().runTaskTimer(this, () -> saveAll(true), 20L * 300, 20L * 300);
        getServer().getScheduler().runTaskTimer(this, this::playtimeTick, 20L * 60, 20L * 60);
        getLogger().info("SMPCore aktiviert – " + data.all().size() + " Spieler, " + crates.all().size() + " Crates geladen.");
    }

    @Override
    public void onDisable() {
        if (scoreboards != null) scoreboards.shutdown();
        if (data != null) saveAll(false);
    }

    private void reg(String name, CommandExecutor ex) {
        PluginCommand c = getCommand(name);
        if (c == null) return;
        c.setExecutor(ex);
        if (ex instanceof TabCompleter t) c.setTabCompleter(t);
    }

    public void saveAll(boolean async) {
        data.save(async);
        auctions.save(async);
        bounties.save(async);
    }

    public void reloadAll() {
        reloadConfig();
        Msg.setPrefix(getConfig().getString("prefix", "<dark_gray>[</dark_gray><aqua>SMP</aqua><dark_gray>]</dark_gray> <gray>"));
        shop.load();
        crystalShop.load();
        crates.load();
    }

    private void playtimeTick() {
        int every = getConfig().getInt("crystals.playtime-minutes", 30);
        long amount = getConfig().getLong("crystals.playtime-amount", 1);
        for (Player p : getServer().getOnlinePlayers()) {
            PlayerData d = data.get(p.getUniqueId());
            if (d == null) continue;
            d.playtime += 60;
            if (every > 0 && amount > 0 && (d.playtime / 60) % every == 0) {
                economy.addCrystals(p.getUniqueId(), amount);
                Msg.send(p, "<light_purple>+" + amount + " Kristall(e) <gray>für deine Spielzeit!");
            }
        }
    }
}
