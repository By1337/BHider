package dev.by1337.hider.config;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import org.bukkit.Material;
import org.by1337.blib.configuration.serialization.BukkitCodecs;
import org.by1337.blib.geom.Vec3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class Config {
    public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ArmorHideSettings.CODEC.fieldOf("armor_hide").forGetter(v -> v.armorHide),
            BukkitCodecs.MATERIAL.listOf().fieldOf("ignore_blocks").forGetter(v -> v.ignoreBlocks),
            HideSettings.CODEC.fieldOf("hide").forGetter(v -> v.hideSettings),
            Codec.DOUBLE.fieldOf("field_of_view").forGetter(v -> v.fieldOfView),
            Vec3d.CODEC.fieldOf("expand_aabb").forGetter(v -> v.expandAabb)
    ).apply(instance, Config::new));

    public final ArmorHideSettings armorHide;
    public final List<Material> ignoreBlocks;
    public final HideSettings hideSettings;
    public final double fieldOfView;
    public final Vec3d expandAabb;

    public Config(ArmorHideSettings armorHide, List<Material> ignoreBlocks, HideSettings hideSettings, double fieldOfView, Vec3d expandAabb) {
        this.armorHide = armorHide;
        this.ignoreBlocks = ignoreBlocks;
        this.hideSettings = hideSettings;
        this.fieldOfView = fieldOfView;
        this.expandAabb = expandAabb;
    }

    public static class ArmorHideSettings {
        public static final Codec<ArmorHideSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("hide_meta").forGetter(v -> v.hideMeta),
                Codec.STRING.listOf().fieldOf("disable_worlds").forGetter(v -> new ArrayList<>(v.disableWorlds)),
                Codec.STRING.fieldOf("bypass_permission").forGetter(v -> v.bypassPermission)
        ).apply(instance, ArmorHideSettings::new));


        public final boolean hideMeta;
        public final Set<String> disableWorlds;
        public final String bypassPermission;

        public ArmorHideSettings(boolean hideMeta, Collection<String> disableWorlds, String bypassPermission) {
            this.hideMeta = hideMeta;
            this.disableWorlds = Set.copyOf(disableWorlds);
            this.bypassPermission = bypassPermission;
        }
    }

    public static class HideSettings {
        public static Codec<HideSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().fieldOf("disable_worlds").forGetter(v -> new ArrayList<>(v.disableWorlds)),
                Codec.STRING.fieldOf("bypass_permission").forGetter(v -> v.bypassPermission)
        ).apply(instance, HideSettings::new));
        public final Set<String> disableWorlds;
        public final String bypassPermission;

        public HideSettings(Collection<String> disableWorlds, String bypassPermission) {
            this.disableWorlds = Set.copyOf(disableWorlds);
            this.bypassPermission = bypassPermission;
        }
    }
}
