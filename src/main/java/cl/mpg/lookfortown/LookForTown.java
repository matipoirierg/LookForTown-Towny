package cl.mpg.lookfortown;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LookForTown extends JavaPlugin implements CommandExecutor {

    private FileConfiguration messagesConfig;
    private translateHexColorCodes hexTranslator;
    private int cooldownDuration;
    private String cooldownMessage;
    private Map<UUID, Long> cooldowns;

    @Override
    public void onEnable() {
        getCommand("lft").setExecutor(this);
        getCommand("lftreload").setExecutor(this);
        hexTranslator = new translateHexColorCodes();
        loadMessagesConfig();

        cooldownDuration = getConfig().getInt("cooldown-duration");
        String cooldownMessageRaw = getConfig().getString("wait-message");
        cooldownMessage = (cooldownMessageRaw != null) ? ChatColor.translateAlternateColorCodes('&', hexTranslator.translateHexColorCodes("&#", "", cooldownMessageRaw)) : "";

        cooldowns = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("lftreload")) {
            reloadConfig();
            messagesConfig = getConfig();
            sender.sendMessage(getMessage("reload"));
            return true;
        }

        if (!command.getName().equalsIgnoreCase("lft")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        if (hasCooldown(player)) {
            long remainingTime = getCooldownTime(player);
            String formattedTime = formatTime(remainingTime);
            player.sendMessage(cooldownMessage.replace("{time}", formattedTime));
            return true;
        }

        String townName = PlaceholderAPI.setPlaceholders(player, "%townyadvanced_town%");
        if (townName.equalsIgnoreCase("")) {
            getServer().broadcastMessage(getMessage("broadcast-town").replace("{player}", player.getName()));
            applyCooldown(player);
        } else {
            player.sendMessage(getMessage("in-town"));
        }

        return true;
    }

    private void loadMessagesConfig() {
        File configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    private String getMessage(String key) {
        if (messagesConfig == null) {
            return null;
        }

        String message = messagesConfig.getString(key);
        if (message != null && message.contains("&#")) {
            message = hexTranslator.translateHexColorCodes("&#", "", message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private boolean hasCooldown(Player player) {
        return cooldowns.containsKey(player.getUniqueId());
    }

    private long getCooldownTime(Player player) {
        long currentTime = System.currentTimeMillis();
        long cooldownTime = cooldowns.get(player.getUniqueId());
        return Math.max(0, (cooldownTime - currentTime) / 1000);
    }

    private void applyCooldown(Player player) {
        long cooldownTime = System.currentTimeMillis() + (cooldownDuration * 1000);
        cooldowns.put(player.getUniqueId(), cooldownTime);
        Bukkit.getScheduler().runTaskLater(this, () -> cooldowns.remove(player.getUniqueId()), cooldownDuration * 20L);
    }

    private String formatTime(long time) {
        long seconds = time % 60;
        return String.valueOf(seconds);
    }
}