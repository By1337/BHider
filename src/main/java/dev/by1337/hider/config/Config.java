package dev.by1337.hider.config;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import org.by1337.blib.geom.Vec3d;

import java.util.*;

public class Config {
    public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ArmorHideSettings.CODEC.fieldOf("armor_hide").forGetter(v -> v.armorHide)
    ).apply(instance, Config::new));

    public final ArmorHideSettings armorHide;

    public Config(ArmorHideSettings armorHide) {
        this.armorHide = armorHide;
    }

    public static class ArmorHideSettings {
        public static final Codec<ArmorHideSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("field_of_view").forGetter(v -> v.fieldOfView),
                Vec3d.CODEC.fieldOf("expand_aabb").forGetter(v -> v.expandAabb),
                Codec.BOOL.fieldOf("hide_meta").forGetter(v -> v.hideMeta),
                Codec.STRING.listOf().fieldOf("disable_worlds").forGetter(v -> new ArrayList<>(v.disableWorlds))
        ).apply(instance, ArmorHideSettings::new));

        public final double fieldOfView;
        public final Vec3d expandAabb;
        public final boolean hideMeta;
        public final Set<String> disableWorlds;

        public ArmorHideSettings(double fieldOfView, Vec3d expandAabb, boolean hideMeta, Collection<String> disableWorlds) {
            this.fieldOfView = fieldOfView;
            this.expandAabb = expandAabb;
            this.hideMeta = hideMeta;
            this.disableWorlds = Collections.unmodifiableSet(new HashSet<>(disableWorlds));
        }
    }
}
