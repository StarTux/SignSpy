package com.cavetale.signspy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class SignSpyPlugin extends JavaPlugin implements Listener {
    HashSet<UUID> optouts = new HashSet<>();
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length != 0) return false;
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        if (optouts.remove(uuid)) {
            player.sendMessage(Component.text("[SignSpy] ").color(NamedTextColor.GOLD)
                               .append(Component.text("Now spying on signs.").color(NamedTextColor.YELLOW)));
        } else {
            optouts.add(uuid);
            player.sendMessage(Component.text("[SignSpy] ").color(NamedTextColor.GOLD)
                               .append(Component.text("No longer spying on signs.").color(NamedTextColor.YELLOW)));
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onSignEdit(SignChangeEvent event) {
        Player author = event.getPlayer();
        if (author.hasPermission("signspy.privacy")) return;
        List<String> lines = new ArrayList<>(4);
        for (String line : event.getLines()) {
            line = line.trim();
            if (line.isEmpty()) continue;
            lines.add(line);
        }
        if (lines.isEmpty()) return;
        Block at = event.getBlock();
        String coords = at.getX() + " " + at.getY() + " " + at.getZ();
        String sat = at.getWorld().getName() + " " + coords;
        getLogger().info(author.getName() + " wrote sign at " + sat + ":");
        for (String line : lines) {
            getLogger().info("> " + line);
        }
        Component tagHover = Component.text("/signspy").color(NamedTextColor.GOLD);
        Component nameHover = Component.text(author.getName()).color(NamedTextColor.WHITE)
            .append(Component.text("\n" + author.getUniqueId()).color(NamedTextColor.GRAY));
        Component signHover = Component.text(sat).color(NamedTextColor.DARK_GRAY);
        for (String line : event.getLines()) {
            signHover = signHover.append(Component.text("\n" + line).color(NamedTextColor.WHITE));
        }
        Component adminMessage = Component.text("[SignSpy] ").color(NamedTextColor.GOLD)
            .hoverEvent(tagHover)
            .clickEvent(ClickEvent.suggestCommand("/signspy"))
            .append(Component.text(author.getName() + ": ").color(NamedTextColor.GRAY)
                    .clickEvent(ClickEvent.suggestCommand("/status " + author.getName()))
                    .hoverEvent(nameHover)
                    .insertion(author.getName()))
            .append(Component.text(String.join(" ", lines)).color(NamedTextColor.WHITE)
                    .clickEvent(ClickEvent.suggestCommand("/tp " + coords))
                    .hoverEvent(signHover)
                    .insertion(coords));
        for (Player admin : getServer().getOnlinePlayers()) {
            if (!admin.hasPermission("signspy.signspy")) continue;
            if (optouts.contains(admin.getUniqueId())) continue;
            admin.sendMessage(adminMessage);
        }
    }
}
