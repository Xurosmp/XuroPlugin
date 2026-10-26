package de.smpcore.gui;

import de.smpcore.economy.EconomyManager;
import de.smpcore.shop.CrystalShopManager;
import de.smpcore.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CrystalShopMenu extends PagedMenu {
    public CrystalShopMenu(Player p) { super(p, "<light_purple><bold>Kristall-Shop"); }

    @Override
    protected List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        for (CrystalShopManager.Entry en : plugin.crystalShop().entries()) {
            List<String> lore = new ArrayList<>(en.lore());
            lore.add("");
            lore.add("<gray>Preis: <light_purple>" + EconomyManager.fmtC(en.cost()));
            lore.add("<yellow>Klicken zum Kaufen");
            ItemStack shown = Items.make(en.icon(), en.amount() > 1 && en.amount() <= 64 && en.type().equals("ITEM") ? en.amount() : 1, en.name(), lore);
            out.add(new Entry(shown, e -> go(new ConfirmMenu(viewer, "<light_purple>Kauf bestätigen", shown,
                    () -> plugin.crystalShop().purchase(viewer, en), () -> new CrystalShopMenu(viewer)))));
        }
        return out;
    }

    @Override
    protected void controls() {
        set(49, Items.make(Material.AMETHYST_SHARD, "<light_purple>Deine Kristalle",
                "<white>" + EconomyManager.fmtC(plugin.economy().crystals(viewer.getUniqueId()))));
    }
}
