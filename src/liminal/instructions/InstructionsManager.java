package liminal.instructions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.LiminalSettings;
import liminal.LiminalStrings;
import liminal.LiminalSurvival;
import liminal.instructions.Instruction.Group;
import liminal.intel.LiminalCrisisIntel;

public class InstructionsManager {
  public static class HullmodLoadout {
    public final ArrayList<String> instructions = new ArrayList<>();
    private final int maxOP;
    private int capacityBought = 0;
    private transient int usedCapacity;
    private transient boolean overCapacity;
    private transient Map<String, Integer> partShares;

    public HullmodLoadout(int maxOP) {
      this.maxOP = maxOP;
    }

    public int getCapacityBought() {
      return capacityBought;
    }

    public int getUsedCapacity() {
      return usedCapacity;
    }

    public boolean isOverCapacity() {
      return overCapacity;
    }

    public int getMaxOP() {
      int maxOP = getBaseOP();
      if (isCrisisOPBonusActive()) {
        maxOP = (int) (maxOP * CRISIS_OP_MULT);
      }
      maxOP = (int) (maxOP * getSettingOPMult());
      return maxOP;
    }

    public static final float CRISIS_OP_MULT = 1.3f;

    public int getBaseOP() {
      int override = templateOPOverride();
      return override > 0 ? override : this.maxOP;
    }

    public boolean isCrisisOPBonusActive() {
      LiminalCrisisIntel lci = LiminalCrisisIntel.getIntel();
      return lci != null && lci.crisisEnded();
    }

    public float getCrisisOPMult() {
      return isCrisisOPBonusActive() ? CRISIS_OP_MULT : 1f;
    }

    public double getSettingOPMult() {
      return LiminalSettings.lunaDouble("liminal_templateopmult", 1.0);
    }

    public int getNumberOfInstructionsInGroupSelected(Group group) {
      if (LGlobals.ignoreRestrictions) {
        return 0;
      }
      int count = 0;
      for (Instruction instruction : resolveAll(instructions)) {
        if (instruction.getGroup().equals(group)) {
          count++;
        }
      }
      return count;
    }
  }

  private static final String KEY = "liminal_instructions";

  private SortedMap<String, HullmodLoadout> loadouts = new TreeMap<>();
  private Map<String, Integer> instructionLevels = new HashMap<>();
  public static final int STARTING_MINIMUM_RESEARCH_INVESTMENT = 1500;
  public static final int MAX_MINIMUM_RESEARCH_INVESTMENT = 300000;

  private int researchInvestmentPerMonth = STARTING_MINIMUM_RESEARCH_INVESTMENT;
  private int researchPoints = 0;
  private int minimumResearchInvestmentPerMonth = STARTING_MINIMUM_RESEARCH_INVESTMENT;
  private int totalRPGained = 0;
  private int totalEverResearchInvestment = 0;
  private int minimumIncreasedTimes = 0;
  private transient boolean capacityCounted;
  private transient int shopCapacity;

  public InstructionsManager() {
    addMissingTemplates();
  }

  private void addMissingTemplates() {
    for (HullModSpecAPI spec : Global.getSettings().getAllHullModSpecs()) {
      if (spec.getId().startsWith("liminal_instructions_") && !loadouts.containsKey(spec.getId())) {
        loadouts.put(spec.getId(), new HullmodLoadout(spec.getFrigateCost()));
      }
    }
  }

  public HullmodLoadout getLoadout(String modId) {
    return loadouts.get(modId);
  }

  public Set<String> getLoadoutIds() {
    return loadouts.keySet();
  }

  public ArrayList<Instruction> getInstructionsForLoadout(String modId) {
    return resolveAll(loadouts.get(modId).instructions);
  }

  private static ArrayList<Instruction> resolveAll(Collection<String> instructionNames) {
    ArrayList<Instruction> resolved = new ArrayList<>();
    for (String instructionName : instructionNames) {
      Instruction instruction = Instructions.getInstruction(instructionName);
      if (instruction != null) {
        resolved.add(instruction);
      }
    }
    return resolved;
  }

  public int getMaxOPForLoadout(String modId) {
    return loadouts.get(modId).getMaxOP();
  }

  public String getHullmodName(String modId) {
    HullModSpecAPI spec = Global.getSettings().getHullModSpec(modId);
    return spec != null ? spec.getDisplayName() : modId;
  }

  public int getInstructionLevel(String instructionName) {
    if (!instructionLevels.containsKey(instructionName)) {
      return 0;
    }
    return instructionLevels.get(instructionName);
  }

  public boolean isInstructionUnlocked(String instructionName) {
    return getInstructionLevel(instructionName) > 0;
  }

  public void setInstructionLevel(String instructionName, int level) {
    Instruction instruction = Instructions.getInstruction(instructionName);
    if (instruction == null) {
      return;
    }
    if (instruction.getMaxLevel() < level) {
      level = instruction.getMaxLevel();
    }
    instructionLevels.put(instructionName, level);
  }

  public int getResearchInvestmentPerMonth() {
    return researchInvestmentPerMonth;
  }

  public int getMinimumResearchInvestmentPerMonth() {
    return LiminalSurvival.price(minimumResearchInvestmentPerMonth);
  }

  public int getBaseMinimumResearchInvestmentPerMonth() {
    return minimumResearchInvestmentPerMonth;
  }

  public static int templateOPOverride() {
    return LiminalSettings.lunaInt("liminal_template_op_override", 0);
  }

  public void setMinimumResearchInvestmentPerMonth(int minimum) {
    minimumResearchInvestmentPerMonth = minimum;
    minimumIncreasedTimes++;
    if (minimumResearchInvestmentPerMonth >= MAX_MINIMUM_RESEARCH_INVESTMENT) {
      minimumResearchInvestmentPerMonth = MAX_MINIMUM_RESEARCH_INVESTMENT;
    }
  }

  public void setResearchInvestmentPerMonth(int upkeep) {
    if (upkeep <= 0) {
      researchInvestmentPerMonth = 0;
    } else if (upkeep < getMinimumResearchInvestmentPerMonth()) {
      researchInvestmentPerMonth = getMinimumResearchInvestmentPerMonth();
    } else {
      researchInvestmentPerMonth = upkeep;
    }
  }

  private static final float RESEARCH_PROGRESS_LOG = 1.4f;

  public float getResearchProgressPerMonth() {
    int minimum = getMinimumResearchInvestmentPerMonth();
    if (researchInvestmentPerMonth < minimum) {
      return 0;
    }
    return (float) (Math
        .log((float) researchInvestmentPerMonth / (minimum / RESEARCH_PROGRESS_LOG))
        / Math.log(RESEARCH_PROGRESS_LOG) * 5f + 5f);
  }

  public int getResearchPoints() {
    return researchPoints;
  }

  public void addResearchPoints(int points) {
    researchPoints += points;
    totalRPGained += points;
  }

  public void spendResearchPoints(int points) {
    researchPoints -= points;
  }

  public int getTotalRPGained() {
    return totalRPGained;
  }

  public int getResearchCost(String instructionName) {
    Instruction instruction = Instructions.getInstruction(instructionName);
    if (instruction == null) {
      return 0;
    }
    return ResearchCostMath.costFromLevel(getInstructionLevel(instructionName), instruction.getRpMult());
  }

  private transient ResearchVisit researchVisit;

  public ResearchVisit getResearchVisit() {
    if (researchVisit == null) {
      researchVisit = new ResearchVisit();
    }
    return researchVisit;
  }

  public void spendOnResearch(int cost) {
    getResearchVisit().beforePurchase(instructionLevels, cost);
    spendResearchPoints(cost);
  }

  public boolean undoResearchVisit() {
    ResearchVisit visit = getResearchVisit();
    if (!visit.canUndo()) {
      return false;
    }
    for (String hullModId : visit.getTemplatesUnlocked()) {
      Global.getSector().getCharacterData().removeHullMod(hullModId);
    }
    researchPoints += visit.undo(instructionLevels);
    return true;
  }

  public static void endResearchVisit() {
    Object manager = Global.getSector().getPersistentData().get(KEY);
    if (manager instanceof InstructionsManager) {
      ((InstructionsManager) manager).getResearchVisit().end();
    }
  }

  public int getCurrentOPForLoadout(String modId) {
    if (LGlobals.ignoreRestrictions) {
      return 0;
    }
    HullmodLoadout loadout = loadouts.get(modId);
    int currentOP = 0;
    for (Instruction instruction : resolveAll(loadout.instructions)) {
      currentOP += instruction.getOpCost();
    }
    return currentOP;
  }

  public int getCostForLoadout(String modId) {
    HullmodLoadout loadout = loadouts.get(modId);
    return (int) Math.pow(loadout.getMaxOP() / 5, 1.5);
  }

  public static final int LOADOUT_SLOTS = 2;

  private Map<String, ArrayList<ArrayList<String>>> savedLoadoutSlots = new HashMap<>();

  private ArrayList<ArrayList<String>> getSlotsFor(String modId) {
    ArrayList<ArrayList<String>> slots = savedLoadoutSlots.get(modId);
    if (slots == null) {
      slots = new ArrayList<>();
      for (int i = 0; i < LOADOUT_SLOTS; i++) {
        slots.add(new ArrayList<String>());
      }
      savedLoadoutSlots.put(modId, slots);
    }
    return slots;
  }

  public ArrayList<String> getSavedLoadoutSlot(String modId, int slot) {
    return getSlotsFor(modId).get(slot);
  }

  public boolean isSavedLoadoutSlotEmpty(String modId, int slot) {
    return getSavedLoadoutSlot(modId, slot).isEmpty();
  }

  public void saveLoadoutToSlot(String modId, int slot) {
    ArrayList<String> saved = getSlotsFor(modId).get(slot);
    saved.clear();
    saved.addAll(loadouts.get(modId).instructions);
  }

  public void loadLoadoutFromSlot(String modId, int slot) {
    HullmodLoadout loadout = loadouts.get(modId);
    ArrayList<String> saved = new ArrayList<>(getSavedLoadoutSlot(modId, slot));
    loadout.instructions.clear();
    for (String instructionName : saved) {
      Instruction instruction = Instructions.getInstruction(instructionName);
      if (instruction == null || !isInstructionUnlocked(instructionName)) {
        continue;
      }
      if (loadout.instructions.contains(instructionName)) {
        continue;
      }
      if (loadout.getNumberOfInstructionsInGroupSelected(instruction.getGroup()) >= Instructions
          .getMaxInstructionsInGroup(instruction.getGroup())) {
        continue;
      }
      if (isIncompatible(loadout, instruction)) {
        continue;
      }
      if (getCurrentOPForLoadout(modId) + instruction.getOpCost() > getMaxOPForLoadout(modId)) {
        continue;
      }
      loadout.instructions.add(instructionName);
    }
  }

  private static final String START_KEY = "liminal_capacity_start";
  private static final String PRICE_KEY = "liminal_capacity_price";
  private static final String PRICE_GROWTH_KEY = "liminal_capacity_price_growth";
  private static final String NOMAD_ID = "nomadic_fleet";
  private static final int START_FALLBACK = 3;
  private static final int PRICE_FALLBACK = 200000;
  private static final float PRICE_GROWTH_FALLBACK = 5f;

  public static int sizeWeight(HullSize size) {
    if (size == null) {
      return 0;
    }
    switch (size) {
      case FRIGATE:
        return 2;
      case DESTROYER:
        return 3;
      case CRUISER:
        return 4;
      case CAPITAL_SHIP:
        return 5;
      default:
        return 0;
    }
  }

  public static int getCapacityStart() {
    return Math.max(0, LiminalSettings.lunaInt(START_KEY, START_FALLBACK));
  }

  private static double capacityPrice() {
    return LiminalSettings.lunaInt(PRICE_KEY, PRICE_FALLBACK);
  }

  private static double capacityPriceGrowth() {
    return LiminalSettings.lunaDouble(PRICE_GROWTH_KEY, PRICE_GROWTH_FALLBACK);
  }

  public int getCapacity(String modId) {
    return getCapacityStart() + loadouts.get(modId).capacityBought + shopCapacity();
  }

  private int shopCapacity() {
    return capacityCounted ? shopCapacity : readShopCapacity();
  }

  private static int readShopCapacity() {
    if (!Global.getSettings().getModManager().isModEnabled(NOMAD_ID)) {
      return 0;
    }
    try {
      return liminal.crossmod.LinkageShopEffect.liveCapacity();
    } catch (LinkageError olderFlotilla) {
      return 0;
    }
  }

  public int getUsedCapacity(String modId) {
    return loadouts.get(modId).usedCapacity;
  }

  public int getCapacityBought(String modId) {
    return loadouts.get(modId).capacityBought;
  }

  public int getShopCapacity() {
    return shopCapacity();
  }

  public long getNextCapacityNormalPrice(String modId) {
    return TemplateCapacityMath.pointPrice(
        getCapacityStart() + loadouts.get(modId).capacityBought + 1, getCapacityStart(),
        capacityPrice(), capacityPriceGrowth(), 1.0);
  }

  public static float getCapacityPriceFactor() {
    return LiminalSurvival.creditMult();
  }

  public List<String> getUnlockedLoadoutIds() {
    List<String> unlocked = new ArrayList<>();
    for (String modId : loadouts.keySet()) {
      if (Global.getSector().getCharacterData().knowsHullMod(modId)) {
        unlocked.add(modId);
      }
    }
    return unlocked;
  }

  public static final class CapacityUse {
    public final String shipName;
    public final String hullClass;
    public final int weight;
    public final boolean module;

    CapacityUse(String shipName, String hullClass, int weight, boolean module) {
      this.shipName = shipName;
      this.hullClass = hullClass;
      this.weight = weight;
      this.module = module;
    }
  }

  public List<CapacityUse> getCapacityUses(String modId) {
    List<CapacityUse> uses = new ArrayList<>();
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    if (fleet == null || !loadouts.containsKey(modId)) {
      return uses;
    }
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
      ShipVariantAPI variant = member.getVariant();
      if (variant == null) {
        continue;
      }
      addCapacityUse(uses, modId, member, variant, false);
      for (String slotId : variant.getModuleSlots()) {
        ShipVariantAPI module = variant.getModuleVariant(slotId);
        if (module != null) {
          addCapacityUse(uses, modId, member, module, true);
        }
      }
    }
    return uses;
  }

  private static void addCapacityUse(List<CapacityUse> uses, String modId, FleetMemberAPI member,
      ShipVariantAPI variant, boolean module) {
    if (!variant.getHullMods().contains(modId)) {
      return;
    }
    String hullClass = variant.getHullSpec() == null ? ""
        : variant.getHullSpec().getHullNameWithDashClass();
    uses.add(new CapacityUse(member.getShipName(), hullClass, sizeWeight(variant.getHullSize()),
        module));
  }

  public long getNextCapacityPrice(String modId) {
    return capacityPointPrice(getCapacityStart() + loadouts.get(modId).capacityBought + 1);
  }

  public long getCapacityPriceAfterNext(String modId) {
    return capacityPointPrice(getCapacityStart() + loadouts.get(modId).capacityBought + 2);
  }

  public static long capacityPointPrice(int point) {
    return TemplateCapacityMath.pointPrice(point, getCapacityStart(), capacityPrice(),
        capacityPriceGrowth(), LiminalSurvival.creditMult());
  }

  public boolean buyCapacity(String modId) {
    HullmodLoadout loadout = loadouts.get(modId);
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    if (loadout == null || fleet == null) {
      return false;
    }
    long price = getNextCapacityPrice(modId);
    if (fleet.getCargo().getCredits().get() < price) {
      return false;
    }
    fleet.getCargo().getCredits().subtract(price);
    loadout.capacityBought++;
    refreshCapacity(true);
    return true;
  }

  public void refreshCapacity(boolean announce) {
    if (Global.getCurrentState() == GameState.COMBAT) {
      return;
    }
    Map<String, Integer> used = countUsedCapacity(null, null, true);
    shopCapacity = readShopCapacity();
    boolean firstCount = !capacityCounted;
    capacityCounted = true;
    Set<String> flipped = null;
    for (Map.Entry<String, HullmodLoadout> entry : loadouts.entrySet()) {
      HullmodLoadout loadout = entry.getValue();
      Integer u = used.get(entry.getKey());
      loadout.usedCapacity = u == null ? 0 : u;
      boolean over = loadout.usedCapacity > getCapacity(entry.getKey());
      if (over == loadout.overCapacity) {
        continue;
      }
      loadout.overCapacity = over;
      if (flipped == null) {
        flipped = new TreeSet<>();
      }
      flipped.add(entry.getKey());
      if (over && announce && !firstCount) {
        postOverCapacityMessage(entry.getKey());
      }
    }
    if (flipped != null) {
      reapplyStats(flipped);
    }
  }

  public static void refreshOnLoad() {
    if (Global.getSector().getPersistentData().containsKey(KEY)) {
      getInstance().refreshCapacity(false);
    }
  }

  public boolean fitsCapacity(String modId, ShipAPI ship) {
    return capacityLeftFor(modId, ship) >= sizeWeight(ship.getHullSize());
  }

  public int countUsedCapacity(String modId) {
    Integer used = countUsedCapacity(null, null, false).get(modId);
    return used == null ? 0 : used;
  }

  public Map<String, Integer> countUsedCapacityByTemplate() {
    return countUsedCapacity(null, null, false);
  }

  public int capacityLeftFor(String modId, ShipAPI ship) {
    String memberId = ship.getFleetMemberId();
    String slotId = null;
    if (ship.getParentStation() != null && ship.getStationSlot() != null) {
      memberId = ship.getParentStation().getFleetMemberId();
      slotId = ship.getStationSlot().getId();
    }
    return capacityLeftFor(modId, memberId, slotId);
  }

  public int capacityLeftFor(String modId, String memberId, String slotId) {
    if (!capacityCounted) {
      Integer used = countUsedCapacity(memberId, slotId, false).get(modId);
      return getCapacity(modId) - (used == null ? 0 : used);
    }
    HullmodLoadout loadout = loadouts.get(modId);
    Integer own = loadout.partShares == null || memberId == null ? null
        : loadout.partShares.get(partKey(memberId, slotId));
    return getCapacity(modId) - loadout.usedCapacity + (own == null ? 0 : own);
  }

  private static String partKey(String memberId, String slotId) {
    return slotId == null ? memberId : memberId + "/" + slotId;
  }

  private Map<String, Integer> countUsedCapacity(String skipMemberId, String skipSlotId,
      boolean cacheShares) {
    Map<String, Integer> used = new HashMap<>();
    if (cacheShares) {
      for (HullmodLoadout loadout : loadouts.values()) {
        if (loadout.partShares != null) {
          loadout.partShares.clear();
        }
      }
    }
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    if (fleet == null) {
      return used;
    }
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
      ShipVariantAPI variant = member.getVariant();
      if (variant == null) {
        continue;
      }
      boolean skipThis = skipMemberId != null && skipMemberId.equals(member.getId());
      if (!(skipThis && skipSlotId == null)) {
        addUse(used, variant, cacheShares ? member.getId() : null);
      }
      for (String slotId : variant.getModuleSlots()) {
        if (skipThis && slotId.equals(skipSlotId)) {
          continue;
        }
        ShipVariantAPI module = variant.getModuleVariant(slotId);
        if (module != null) {
          addUse(used, module, cacheShares ? partKey(member.getId(), slotId) : null);
        }
      }
    }
    return used;
  }

  private void addUse(Map<String, Integer> used, ShipVariantAPI variant, String partKey) {
    for (String hullmodId : variant.getHullMods()) {
      HullmodLoadout loadout = loadouts.get(hullmodId);
      if (loadout == null) {
        continue;
      }
      int weight = sizeWeight(variant.getHullSize());
      Integer sum = used.get(hullmodId);
      used.put(hullmodId, (sum == null ? 0 : sum) + weight);
      if (partKey != null) {
        if (loadout.partShares == null) {
          loadout.partShares = new HashMap<>();
        }
        loadout.partShares.put(partKey, weight);
      }
    }
  }

  private static void reapplyStats(Set<String> modIds) {
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    if (fleet == null) {
      return;
    }
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
      if (carriesAny(member.getVariant(), modIds)) {
        member.updateStats();
      }
    }
    fleet.getFleetData().setSyncNeeded();
    fleet.getFleetData().syncIfNeeded();
  }

  private static boolean carriesAny(ShipVariantAPI variant, Set<String> modIds) {
    if (variant == null) {
      return false;
    }
    for (String modId : modIds) {
      if (variant.hasHullMod(modId)) {
        return true;
      }
    }
    for (String slotId : variant.getModuleSlots()) {
      ShipVariantAPI module = variant.getModuleVariant(slotId);
      if (module != null && carriesAny(module, modIds)) {
        return true;
      }
    }
    return false;
  }

  private void postOverCapacityMessage(String modId) {
    MessageIntel intel = new MessageIntel(LiminalStrings.intel("msg_over_capacity"),
        Misc.getTextColor(), new String[] { getHullmodName(modId) }, Misc.getNegativeHighlightColor());
    intel.setIcon(Global.getSettings().getSpriteName("icons", "liminal_intel_research"));
    intel.setSound("liminal_monthly_income_negative");
    Global.getSector().getCampaignUI().addMessage(intel, CommMessageAPI.MessageClickAction.INTEL_TAB, null);
  }

  public void addOverCapacityLine(TooltipMakerAPI tooltip, String modId, int used, float pad) {
    tooltip.addPara(LiminalStrings.mgmt("err_over_capacity"), pad, Misc.getNegativeHighlightColor(),
        Misc.getHighlightColor(), getHullmodName(modId), "" + used, "" + getCapacity(modId));
  }

  public void addMonthlyCostTooltip(TooltipMakerAPI tooltip, float padding) {
    tooltip.setParaFontColor(Misc.getTextColor());
    tooltip.addPara("Research Investment: %s", padding * 2, Misc.getHighlightColor(),
        "" + Misc.getDGSCredits(getResearchInvestmentPerMonth()));
  }

  public void addTotalEverResearchInvestment(float fraction) {
    totalEverResearchInvestment += (int) (getResearchInvestmentPerMonth() * fraction);
  }

  public int getTotalEverResearchInvestment() {
    return totalEverResearchInvestment;
  }

  public boolean isIncompatible(HullmodLoadout loadout, Instruction i) {
    for (Instruction instruction : resolveAll(loadout.instructions)) {
      if (instruction.isIncompatible(i) || i.isIncompatible(instruction)) {
        return true;
      }
    }
    return false;
  }

  public int getLiminalIncreases() {
    return minimumIncreasedTimes;
  }

  public boolean hasNewDialogue() {
    if (!Global.getSector().getMemoryWithoutUpdate().getBoolean("$liminalSeenStarting")) {
      return true;
    }
    return false;
  }

  public static void reconcileOnLoad() {
    if (!Global.getSector().getPersistentData().containsKey(KEY)) {
      return;
    }
    getInstance().reconcileWithInstalledContent();
  }

  public void reconcileWithInstalledContent() {
    Set<String> dropped = new TreeSet<>();
    for (HullmodLoadout loadout : loadouts.values()) {
      dropUnknown(loadout.instructions, dropped);
    }
    for (ArrayList<ArrayList<String>> slots : savedLoadoutSlots.values()) {
      for (ArrayList<String> slot : slots) {
        dropUnknown(slot, dropped);
      }
    }
    dropUnknown(instructionLevels.keySet(), dropped);
    if (!dropped.isEmpty()) {
      Global.getLogger(InstructionsManager.class).warn("Liminal: no installed mod provides these"
          + " Instructions any more, so they were dropped from this save: " + dropped
          + ". Templates and research keep everything else; ships lose only those effects.");
    }
  }

  private static void dropUnknown(Collection<String> instructionNames, Set<String> dropped) {
    for (Iterator<String> it = instructionNames.iterator(); it.hasNext();) {
      String instructionName = it.next();
      if (Instructions.getInstruction(instructionName) == null) {
        dropped.add(instructionName);
        it.remove();
      }
    }
  }

  public static InstructionsManager getInstance() {
    if (!Global.getSector().getPersistentData().containsKey(KEY)) {
      Global.getLogger(InstructionsManager.class).info("Needed to make new persistent data for Liminal.");
      Global.getSector().getPersistentData().put(KEY, new InstructionsManager());
    }

    return (InstructionsManager) Global.getSector().getPersistentData().get(KEY);
  }
}
