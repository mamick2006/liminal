package liminal.faction;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Planets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;

public class LiminalFaction {

  public static void initializeSystems() {
    SectorAPI sector = Global.getSector();
    float[] xOffsets = { -1, 1, 0, 0 };
    float[] yOffsets = { 0, 0, 1, -1 };
    String[] name = { "West", "East", "North", "South" };
    float w = Global.getSettings().getFloat("sectorWidth");
    float h = Global.getSettings().getFloat("sectorHeight");
    Random random = new Random();

    for (int i = 0; i < 4; i++) {
      addSystem(sector, w / 2f * xOffsets[i] + random.nextFloat() * 4000f - 2000f,
          h / 2f * yOffsets[i] + random.nextFloat() * 4000f - 2000f, name[i]);
    }

    FactionAPI liminal = sector.getFaction(LGlobals.liminalFactionId);
    List<FactionAPI> factionList = sector.getAllFactions();

    for (FactionAPI faction : factionList) {
      if (!faction.getId().equals(LGlobals.liminalFactionId)) {
        liminal.setRelationship(faction.getId(), RepLevel.VENGEFUL);
      }
    }
  }

  public static void addSystem(SectorAPI sector, float x, float y, String name) {
    StarSystemAPI system = sector.createStarSystem("Liminal Invasion Point " + name);
    system.setName("Liminal Invasion Point " + name);
    system.setType(StarSystemType.DEEP_SPACE);
    system.addTag(Tags.THEME_SPECIAL);
    system.setBackgroundTextureFilename("graphics/backgrounds/background5.jpg");

    PlanetAPI star = system.initStar("liminal_invasion_star_" + name,
        "star_orange",
        650f,
        500);
    system.setLightColor(LGlobals.liminalityDarkColor);
    system.getLocation().set(x, y);

    PlanetAPI planet = system.addPlanet("liminal_invasion_planet_" + name, star, name, Planets.PLANET_TERRAN, 0, 100f,
        1800f,
        365);
    planet.setCustomDescriptionId("planet_liminal_invasion_planet");
    planet.addTag(LGlobals.liminalInvasionTag);

    EconomyAPI globalEconomy = Global.getSector().getEconomy();
    String marketID = planet.getId() + "_market";

    MarketAPI market = Global.getFactory().createMarket(marketID, name, 10);
    market.setFactionId(LGlobals.liminalFactionId);
    market.setPrimaryEntity(planet);
    market.getTariff().setBaseValue(0.15f);
    market.setAdmin(null);
    market.setHidden(true);
    market.addTag(LGlobals.liminalInvasionTag);

    market.addCondition("liminal_market_condition");
    market.addCondition(Conditions.POPULATION_10);
    market.addCondition(Conditions.REGIONAL_CAPITAL);
    market.addCondition(Conditions.AI_CORE_ADMIN);
    market.addCondition(Conditions.RUINS_VAST);
    market.addCondition(Conditions.HABITABLE);
    market.addCondition(Conditions.ORE_ULTRARICH);
    market.addCondition(Conditions.RARE_ORE_ULTRARICH);
    market.addCondition(Conditions.VOLATILES_PLENTIFUL);
    market.addCondition(Conditions.ORGANICS_PLENTIFUL);
    market.addCondition(Conditions.FARMLAND_BOUNTIFUL);

    market.addIndustry(Industries.POPULATION);
    market.addIndustry(Industries.MEGAPORT);
    market.addIndustry(Industries.MINING);
    market.addIndustry(Industries.HEAVYBATTERIES);
    market.addIndustry(Industries.HIGHCOMMAND);
    market.addIndustry(Industries.FARMING);
    market.addIndustry(Industries.REFINING);
    market.addIndustry(Industries.LIGHTINDUSTRY);
    market.addIndustry(Industries.ORBITALWORKS, Collections.singletonList(Items.PRISTINE_NANOFORGE));
    market.addIndustry(Industries.FUELPROD, Collections.singletonList(Items.SYNCHROTRON));
    market.addIndustry(Industries.TECHMINING);
    market.getIndustry(Industries.HIGHCOMMAND).setAICoreId(Commodities.GAMMA_CORE);
    market.getIndustry(Industries.POPULATION).setAICoreId(Commodities.GAMMA_CORE);
    market.getIndustry(Industries.MEGAPORT).setAICoreId(Commodities.GAMMA_CORE);

    market.setFreePort(false);

    globalEconomy.addMarket(market, true);
    planet.setMarket(market);
    planet.setFaction(LGlobals.liminalFactionId);

    system.autogenerateHyperspaceJumpPoints(true, true, true);
    system.setEnteredByPlayer(true);
    Misc.setAllPlanetsSurveyed(system, true);
    for (MarketAPI m : Global.getSector().getEconomy().getMarkets(system)) {
      m.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
    }
  }
}
