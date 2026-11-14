package liminal.rules;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Voices;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

public class Liminal_ShowAndCreatePerson extends BaseCommandPlugin {
  public static final String ELNA = "liminal_elna";
  public static final String JARN = "liminal_jarn";

  public static PersonAPI getOrCreatePerson(String personId) {
    ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
    PersonAPI person = ip.getPerson(personId);
    if (person == null) {
      if (personId.equals(ELNA)) {
        person = Global.getFactory().createPerson();
        person.setId(ELNA);
        person.setImportance(PersonImportance.HIGH);
        person.setVoice(Voices.SCIENTIST);
        person.setFaction(Factions.INDEPENDENT);
        person.setGender(FullName.Gender.FEMALE);
        person.setRankId(Ranks.CITIZEN);
        person.setPostId(Ranks.POST_SCIENTIST);
        person.getName().setFirst("Elna");
        person.getName().setLast("Rousseau");
        person.setPortraitSprite(Global.getSettings().getSpriteName("characters", "liminal_elna"));
        ip.addPerson(person);
      } else if (personId.equals(JARN)) {
        person = Global.getFactory().createPerson();
        person.setId(JARN);
        person.setImportance(PersonImportance.HIGH);
        person.setVoice(Voices.SCIENTIST);
        person.setFaction(Factions.INDEPENDENT);
        person.setGender(FullName.Gender.MALE);
        person.setRankId(Ranks.CITIZEN);
        person.setPostId(Ranks.POST_SCIENTIST);
        person.getName().setFirst("Jarn");
        person.getName().setLast("Rousseau");
        person.setPortraitSprite(Global.getSettings().getSpriteName("characters", "liminal_jarn"));
        ip.addPerson(person);
      } else {
        throw new RuntimeException("Unknown person: " + personId);
      }
    }
    return person;
  }

  @Override
  public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
      Map<String, MemoryAPI> memoryMap) {
    String personId = params.get(0).getString(memoryMap);
    PersonAPI person = getOrCreatePerson(personId);
    dialog.getVisualPanel().showPersonInfo(person, true);
    return true;
  }
}
