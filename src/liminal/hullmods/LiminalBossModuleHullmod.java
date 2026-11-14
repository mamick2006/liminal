package liminal.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import liminal.faction.LUtils;

public class LiminalBossModuleHullmod extends BaseHullMod {

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
    float bossCompute = 1 / LUtils.getBossCompute();
    float percent = 100f * (bossCompute - 1f);
    stats.getBallisticWeaponDamageMult().modifyPercent(id, percent * 0.4f);
    stats.getEnergyWeaponDamageMult().modifyPercent(id, percent * 0.4f);
    stats.getMissileWeaponDamageMult().modifyPercent(id, percent * 0.4f);
    stats.getHullBonus().modifyPercent(id, percent * 0.2f);
    stats.getArmorBonus().modifyPercent(id, percent * 0.05f);
    stats.getPeakCRDuration().modifyPercent(id, 10000f);
    stats.getDynamic().getStat(Stats.EXPLOSION_DAMAGE_MULT).modifyMult(id, 0);
    stats.getDynamic().getStat(Stats.EXPLOSION_RADIUS_MULT).modifyMult(id, 0);
  }
}
