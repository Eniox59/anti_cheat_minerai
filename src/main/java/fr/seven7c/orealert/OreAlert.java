package fr.seven7c.orealert;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.BanList;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class OreAlert extends JavaPlugin implements Listener {
    private final Map<UUID, Map<Material, Integer>> playerOreCounts = new HashMap<>();
    private final Map<UUID, Long> lastOreMined = new HashMap<>();
    private final Map<Material, OreThreshold> oreThresholds = new HashMap<>();
    private final long SUSPICIOUS_WINDOW = TimeUnit.MINUTES.toMillis(20); // 20 minutes
    
    @Override
    public void onEnable() {
        // Enregistrement de la classe de sérialisation
        this.getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        
        // Chargement des seuils
        loadOreThresholds();
        
        // Enregistrement des événements
        getServer().getPluginManager().registerEvents(this, this);
        
        // Enregistrement de la commande
        getCommand("7s7core").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("7s7core.admin")) {
                sender.sendMessage("§cTu n'as pas la permission d'utiliser cette commande !");
                return true;
            }
            
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                loadOreThresholds();
                sender.sendMessage("§aConfiguration rechargée avec succès !");
            } else {
                sender.sendMessage("§6=== 7S7C Ore Alert ===");
                sender.sendMessage("§7/7s7core reload §f- Recharge la configuration");
            }
            return true;
        });
        
        // Enregistrement des permissions
        getServer().getPluginManager().addPermission(new Permission("7s7core.admin", "Permet d'utiliser les commandes d'administration du plugin 7S7C Ore Alert"));
        getServer().getPluginManager().addPermission(new Permission("7s7core.alerts", "Permet de recevoir les alertes de minage suspect"));
        getServer().getPluginManager().addPermission(new Permission("7s7core.bypass", "Permet de contourner la détection de minage suspect"));
        
        getLogger().info("7S7C_Ore_Alert activé avec succès !");
    }
    
    private void loadOreThresholds() {
        oreThresholds.clear();
        ConfigurationSection oresSection = getConfig().getConfigurationSection("ores");
        
        if (oresSection != null) {
            for (String key : oresSection.getKeys(false)) {
                ConfigurationSection oreSection = oresSection.getConfigurationSection(key);
                if (oreSection != null) {
                    Material material = Material.getMaterial(key.toUpperCase());
                    if (material != null) {
                        int suspicious = oreSection.getInt("suspicious");
                        int verySuspicious = oreSection.getInt("very-suspicious");
                        int maxRealistic = oreSection.getInt("max-realistic");
                        oreThresholds.put(material, new OreThreshold(material, suspicious, verySuspicious, maxRealistic));
                    }
                }
            }
        }
        
        // Si aucun seuil n'a été chargé, on utilise les valeurs par défaut
        if (oreThresholds.isEmpty()) {
            setupDefaultThresholds();
        }
    }
    
    private void setupDefaultThresholds() {
        // Ajout des seuils par défaut
        addDefaultThreshold(Material.DIAMOND_ORE, 20, 30, 15);
        addDefaultThreshold(Material.DEEPSLATE_DIAMOND_ORE, 20, 30, 15);
        addDefaultThreshold(Material.ANCIENT_DEBRIS, 6, 8, 4);
        addDefaultThreshold(Material.EMERALD_ORE, 5, 8, 3);
        addDefaultThreshold(Material.DEEPSLATE_EMERALD_ORE, 5, 8, 3);
        addDefaultThreshold(Material.GOLD_ORE, 30, 40, 24);
        addDefaultThreshold(Material.DEEPSLATE_GOLD_ORE, 30, 40, 24);
        addDefaultThreshold(Material.NETHER_GOLD_ORE, 30, 40, 24);
        addDefaultThreshold(Material.NETHER_QUARTZ_ORE, 30, 40, 64);
        addDefaultThreshold(Material.IRON_ORE, 80, 100, 64);
        addDefaultThreshold(Material.DEEPSLATE_IRON_ORE, 80, 100, 64);
        addDefaultThreshold(Material.REDSTONE_ORE, 160, 200, 128);
        addDefaultThreshold(Material.DEEPSLATE_REDSTONE_ORE, 160, 200, 128);
        addDefaultThreshold(Material.LAPIS_ORE, 30, 40, 25);
        addDefaultThreshold(Material.DEEPSLATE_LAPIS_ORE, 30, 40, 25);
        addDefaultThreshold(Material.COAL_ORE, 220, 300, 192);
        addDefaultThreshold(Material.DEEPSLATE_COAL_ORE, 220, 300, 192);
        addDefaultThreshold(Material.COPPER_ORE, 160, 200, 128);
        addDefaultThreshold(Material.DEEPSLATE_COPPER_ORE, 160, 200, 128);
        
        // Sauvegarde des valeurs par défaut dans la config
        saveDefaultConfig();
    }
    
    private void addDefaultThreshold(Material material, int suspicious, int verySuspicious, int maxRealistic) {
        oreThresholds.put(material, new OreThreshold(material, suspicious, verySuspicious, maxRealistic));
        getConfig().addDefault("ores." + material.name().toLowerCase() + ".suspicious", suspicious);
        getConfig().addDefault("ores." + material.name().toLowerCase() + ".very-suspicious", verySuspicious);
        getConfig().addDefault("ores." + material.name().toLowerCase() + ".max-realistic", maxRealistic);
    }

    private int alertThreshold;
    private long timeWindow;

    private void loadConfig() {
        getConfig().addDefault("alert-threshold", 3);
        getConfig().addDefault("time-window-seconds", 10);
        
        getConfig().options().copyDefaults(true);
        saveConfig();

        alertThreshold = getConfig().getInt("alert-threshold");
        timeWindow = getConfig().getLong("time-window-seconds") * 1000L;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Material blockType = event.getBlock().getType();

        // Vérifier si ce type de bloc est surveillé
        if (!oreThresholds.containsKey(blockType)) {
            return;
        }

        Player player = event.getPlayer();

        // Vérifier les permissions de bypass
        if (player.hasPermission("7s7core.bypass")) {
            return;
        }

        UUID playerId = player.getUniqueId();
        OreThreshold threshold = oreThresholds.get(blockType);
        long currentTime = System.currentTimeMillis();

        // Initialiser les compteurs si nécessaire
        playerOreCounts.putIfAbsent(playerId, new HashMap<>());
        Map<Material, Integer> playerOres = playerOreCounts.get(playerId);

        // Vérifier si la fenêtre de temps est dépassée et réinitialiser si nécessaire
        if (lastOreMined.containsKey(playerId) &&
                (currentTime - lastOreMined.get(playerId)) > SUSPICIOUS_WINDOW) {
            playerOres.clear();
        }

        // Incrémenter le compteur pour ce type de minerai
        int oreCount = playerOres.getOrDefault(blockType, 0) + 1;
        playerOres.put(blockType, oreCount);
        lastOreMined.put(playerId, currentTime);

        // Vérifier les seuils d'alerte
        checkThresholds(player, blockType, oreCount, threshold);
    }

    private void checkThresholds(Player player, Material blockType, int oreCount, OreThreshold threshold) {
        String playerName = player.getName();
        String oreName = formatOreName(blockType);
        
        // Récupérer les messages de configuration
        String warningMessage = getConfig().getString("settings.warning-message", "&cAttention: Vous avez miné %amount% %ore% en peu de temps. Soyez prudent !")
                .replace("%amount%", String.valueOf(oreCount))
                .replace("%ore%", oreName);
        String banMessage = getConfig().getString("ban-message", "§cVous avez été banni pour minage suspect.")
                .replace("%player%", player.getName())
                .replace("%ore%", formatOreName(blockType))
                .replace("%amount%", String.valueOf(oreCount));
        
        // Vérifier les seuils
        if (oreCount >= threshold.getVerySuspiciousThreshold()) {
            // Seuil critique - Bannissement
            String alertMessage = String.format(
                "§8[§c7S7C§8] §4§lALERTE CRITIQUE §8» §e%s §7a miné §c%d %s §7(Seuil critique: %d)",
                playerName, oreCount, oreName, threshold.getVerySuspiciousThreshold()
            );
            
            // Envoyer l'alerte aux administrateurs
            alertAdmins(alertMessage);
            
            // Bannir le joueur
            String banReason = banMessage.replace("§c", ""); // Retirer les codes de couleur pour la raison
            Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), banReason, null, null);
            player.kickPlayer(banMessage);
            
        } else if (oreCount >= threshold.getSuspiciousThreshold()) {
            // Seuil d'alerte - Avertissement
            String alertMessage = String.format(
                "§8[§c7S7C§8] §c§lALERTE §8» §e%s §7a miné §6%d %s §7(Seuil d'alerte: %d)",
                playerName, oreCount, oreName, threshold.getSuspiciousThreshold()
            );
            
            // Envoyer l'alerte aux administrateurs
            alertAdmins(alertMessage);
            
            // Avertir le joueur
            player.sendMessage(warningMessage);
        }
    }

    private String formatOreName(Material material) {
        // Formater le nom du minerai pour l'affichage
        String name = material.name().toLowerCase().replace('_', ' ');
        // Enlever les préfixes inutiles
        name = name.replace("deepslate ", "");
        name = name.replace("nether ", "");
        return name;
    }
    
    private void alertAdmins(String message) {
        // Envoyer l'alerte à tous les administrateurs en ligne
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("7s7core.alerts")) {
                admin.sendMessage("");
                admin.sendMessage("§8§m--------------------------------");
                admin.sendMessage(message);
                admin.sendMessage("§8§m--------------------------------");
                admin.sendMessage("");
            }
        }
        
        // Logger dans la console
        getLogger().warning("[7S7C] " + message.replaceAll("§[0-9a-fk-or]", ""));
    }
    
    @Override
    public void onDisable() {
        // Nettoyage des ressources si nécessaire
        playerOreCounts.clear();
        lastOreMined.clear();
        getLogger().info("7S7C_Ore_Alert désactivé avec succès !");
    }
}
