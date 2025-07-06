package fr.seven7c.orealert;

import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class OreThreshold implements ConfigurationSerializable {
    private final Material material;
    private final int suspiciousThreshold;
    private final int verySuspiciousThreshold;
    private final int maxRealistic;

    public OreThreshold(Material material, int suspiciousThreshold, int verySuspiciousThreshold, int maxRealistic) {
        this.material = material;
        this.suspiciousThreshold = suspiciousThreshold;
        this.verySuspiciousThreshold = verySuspiciousThreshold;
        this.maxRealistic = maxRealistic;
    }

    public Material getMaterial() {
        return material;
    }

    public int getSuspiciousThreshold() {
        return suspiciousThreshold;
    }

    public int getVerySuspiciousThreshold() {
        return verySuspiciousThreshold;
    }

    public int getMaxRealistic() {
        return maxRealistic;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("material", material.name());
        map.put("suspicious", suspiciousThreshold);
        map.put("very-suspicious", verySuspiciousThreshold);
        map.put("max-realistic", maxRealistic);
        return map;
    }

    public static OreThreshold deserialize(Map<String, Object> map) {
        String materialName = (String) map.get("material");
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            throw new IllegalArgumentException("Matériel invalide: " + materialName);
        }
        int suspicious = (int) map.get("suspicious");
        int verySuspicious = (int) map.get("very-suspicious");
        int maxRealistic = (int) map.get("max-realistic");
        return new OreThreshold(material, suspicious, verySuspicious, maxRealistic);
    }
}
