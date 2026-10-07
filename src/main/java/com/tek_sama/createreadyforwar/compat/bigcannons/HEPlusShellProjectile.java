package com.tek_sama.createreadyforwar.compat.bigcannons;

import net.minecraft.core.Position;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.config.CBCConfigs;
import rbasamoyai.createbigcannons.munitions.ShellExplosion;
import rbasamoyai.createbigcannons.munitions.big_cannon.config.BigCannonCommonShellProperties;

/** A heavier HE shell: same explosion as Create Big Cannons' HE, with the bigger powers set in its JSON. */
public class HEPlusShellProjectile extends ShellProjectile {

    public HEPlusShellProjectile(EntityType<? extends HEPlusShellProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected ShellKind kind() {
        return ShellKind.HE_PLUS;
    }

    @Override
    protected void detonate(Position position) {
        BigCannonCommonShellProperties properties = getAllProperties();
        ShellExplosion explosion = new ShellExplosion(level(), this, indirectArtilleryFire(false), position.x(),
            position.y(), position.z(), properties.explosion().blockDamagePower(),
            properties.explosion().entityDamagePower(), false,
            CBCConfigs.server().munitions.damageRestriction.get().explosiveInteraction());
        CreateBigCannons.handleCustomExplosion(level(), explosion);
    }
}
