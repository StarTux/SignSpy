package com.cavetale.signspy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
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
    public boolean onCommand(final CommandSender sender,
                             final Command command,
                             final String alias,
                             final String[] args) {
        if (args.length != 0) return false;
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        if (optouts.remove(uuid)) {
            player.sendMessage(ChatColor.GOLD + "[ChatSpy] "
                               + ChatColor.YELLOW + "Now spying on signs.");
        } else {
            optouts.add(uuid);
            player.sendMessage(ChatColor.GOLD + "[ChatSpy] "
                               + ChatColor.YELLOW + "No longer spying on signs.");
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onSignEdit(SignChangeEvent event) {
        List<String> lines = new ArrayList<>(4);
        for (String line : event.getLines()) {
            line = line.trim();
            if (line.isEmpty()) continue;
            lines.add(line);
        }
        if (lines.isEmpty()) return;
        Player author = event.getPlayer();
        Block at = event.getBlock();
        String sat = at.getWorld().getName() + ":"
            + at.getX() + "," + at.getY() + "," + at.getZ();
        // Console
        getLogger().info(author.getName() + " wrote sign at " + sat + ":");
        for (String line : lines) {
            getLogger().info("> " + line);
        }
        for (Player admin : getServer().getOnlinePlayers()) {
            if (!admin.hasPermission("signspy.signspy")) continue;
            if (optouts.contains(admin.getUniqueId())) continue;
            admin.sendMessage(ChatColor.GOLD + "[SignSpy] " + ChatColor.YELLOW
                              + author.getName() + " wrote sign at "
                              + ChatColor.UNDERLINE + sat + ChatColor.YELLOW + ":");
            for (String line : lines) {
                admin.sendMessage(ChatColor.RED + "> " + ChatColor.RESET + line);
            }
        }
    }
}
