package me.manhumor.mmine;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MMine extends JavaPlugin implements Listener {
    private static Economy econ = null;
    private static FileConfiguration config;

    private Set<String> disabledWorlds;
    private Boolean replaceEnable;
    private String replaceBlock;
    private Long recoveryTime;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        config = getConfig();

        disabledWorlds = new HashSet<>(config.getStringList("disable-worlds"));
        replaceEnable = config.getBoolean("replace-enable");
        replaceBlock = config.getString("replace-block");
        recoveryTime = config.getLong("recovery-time");

        getLogger().info("§c. . . . . . . . . . . .");
        getLogger().info("§c| §fPlugin §cM§fMine");
        getLogger().info("§c| §f- §cSuccessful §floaded");
        getLogger().info("§c| §f- §cI wish you §fluck!!!");
        getLogger().info("§c˙ ˙ ˙ ˙ ˙ ˙ ˙ ˙ ˙ ˙ ˙ ˙");

        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("§c. . . . . . . . . . . .");
        getLogger().info("§c| §fPlugin §cM§fMine");
        getLogger().info("§c| §f- §cSuccessful §funloaded");
        getLogger().info("§c| §f- §cI wish you §fluck!!!");
        getLogger().info("§c˙ ˙ ˙ ˙ ˙ ˙ ˙ ˙ ˙ ˙ ˙ ˙");
    }

    @EventHandler()
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();

        if (!config.contains(blockType.name()) || disabledWorlds.contains(block.getWorld().getName())) return;
        if (!player.hasPermission("mmine.use")) return;
        event.setCancelled(true);
        ConfigurationSection blockSection = config.getConfigurationSection(blockType.name());

        double randomNumber = 0;
        double chance = blockSection.getDouble("chance");
        if (Math.random() * 100.0 <= chance) {
            double min = blockSection.getDouble("min");
            double max = blockSection.getDouble("max");

            randomNumber = min + (Math.random() * ((max - min) + 1));
            EconomyResponse response = econ.depositPlayer(player, randomNumber);
            if (!response.transactionSuccess()) {
                getLogger().warning("§c| §fУ вaс возниклa ошибкa при нaчислении денег игроку");
                getLogger().warning("§c| §fНaпишите к создaтелю плaгинa в дискорд: §cman_humor");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        }

        List<String> messageList = blockSection.getStringList("message");
        String actionbarMessage = blockSection.getString("action-bar");

        String message = "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messageList.size(); i++) {
            String line = messageList.get(i);
            sb.append(ColorParser.parseString(line)
                    .replaceAll("%randomNumber", MessageFormat.format("{0,number,#.##}", randomNumber)));
            if (i < messageList.size() - 1) {
                sb.append("\n");
            }
        }
        message = sb.toString();
        if (!message.trim().isEmpty()) player.sendMessage(message);

        if (!actionbarMessage.trim().isEmpty()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(ColorParser.parseString(actionbarMessage)
                            .replaceAll("%randomNumber", MessageFormat.format("{0,number,#.##}", randomNumber))));
        }

        if (replaceEnable) {
            block.setType(Material.valueOf(replaceBlock));
            new BukkitRunnable() {
                @Override
                public void run() {
                    block.setType(blockType);
                }
            }.runTaskLater(this, recoveryTime);
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return true;
    }
}
