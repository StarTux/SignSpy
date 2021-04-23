package com.cavetale.signspy;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
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
        List<Component> lines = event.lines();
        int count = 0;
        for (Component line : lines) {
            if (!Component.empty().equals(line)) count += 1;
        }
        if (count == 0) return;
        Block at = event.getBlock();
        String coords = at.getX() + " " + at.getY() + " " + at.getZ();
        String sat = at.getWorld().getName() + " " + coords;
        getLogger().info(author.getName() + " wrote sign at " + sat + ":");
        for (Component line : lines) {
            getLogger().info("> " + PlainComponentSerializer.plain().serialize(line));
        }
        Component tagHover = Component.text("/signspy", NamedTextColor.GOLD);
        Component nameHover = Component.text()
            .append(Component.text(author.getName(), NamedTextColor.WHITE))
            .append(Component.text("\n" + author.getUniqueId(), NamedTextColor.GRAY))
            .build();
        TextComponent.Builder signHover = Component.text()
            .append(Component.text(sat, NamedTextColor.DARK_GRAY))
            .append(Component.newline())
            .append(Component.join(Component.newline(), lines));
        Component adminMessage = Component.text()
            .append(Component.text().content("[SignSpy]").color(NamedTextColor.GRAY)
                    .hoverEvent(tagHover)
                    .clickEvent(ClickEvent.suggestCommand("/signspy"))
                    .build())
            .append(Component.space())
            .append(Component.text().content(author.getName()).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)
                    .clickEvent(ClickEvent.suggestCommand("/status " + author.getName()))
                    .hoverEvent(nameHover)
                    .insertion(author.getName())
                    .build())
            .append(Component.space())
            .append(Component.text().color(NamedTextColor.DARK_GRAY)
                    .append(Component.join(Component.space(), lines))
                    .clickEvent(ClickEvent.suggestCommand("/tp " + coords))
                    .hoverEvent(signHover.build())
                    .insertion(coords)
                    .build())
            .build();
        for (Player admin : getServer().getOnlinePlayers()) {
            if (!admin.hasPermission("signspy.signspy")) continue;
            if (optouts.contains(admin.getUniqueId())) continue;
            admin.sendMessage(adminMessage);
        }
    }
}
