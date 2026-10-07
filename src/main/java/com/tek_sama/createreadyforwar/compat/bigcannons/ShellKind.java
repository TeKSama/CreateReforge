package com.tek_sama.createreadyforwar.compat.bigcannons;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.MapColor;

/** Our four shells. The name is the id of both the shell block/item and its projectile entity. */
public enum ShellKind implements StringRepresentable {
    HE_PLUS("he_plus_shell", MapColor.COLOR_RED),
    FLASHBANG("flashbang_shell", MapColor.SNOW),
    DISPERSAL("dispersal_shell", MapColor.COLOR_PURPLE),
    CLUSTER("cluster_shell", MapColor.COLOR_GREEN);

    public static final Codec<ShellKind> CODEC = StringRepresentable.fromEnum(ShellKind::values);

    private final String id;
    private final MapColor color;

    ShellKind(String id, MapColor color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public MapColor color() {
        return color;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
