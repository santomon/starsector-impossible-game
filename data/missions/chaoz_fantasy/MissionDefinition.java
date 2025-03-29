package data.missions.chaoz_fantasy;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import santomon.ImpossibleGame.ImpossibleGameLevelPlugin;

import static santomon.ImpossibleGame.IGMisc.IDs.IG_HERMES_VARIANT_ID;
import static santomon.ImpossibleGame.IGMisc.IDs.IG_KITE_VARIANT_ID;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		api.initFleet(FleetSide.PLAYER, "Square", FleetGoal.ATTACK, false, 5);
		api.initFleet(FleetSide.ENEMY, "Spikey Boi", FleetGoal.ATTACK, true);

		api.setFleetTagline(FleetSide.PLAYER, "Player");
		api.setFleetTagline(FleetSide.ENEMY, "Spikey Bois");

		FleetMemberAPI hermes = api.addToFleet(FleetSide.PLAYER, IG_HERMES_VARIANT_ID, FleetMemberType.SHIP, "Square", true);
		api.addToFleet(FleetSide.ENEMY, IG_KITE_VARIANT_ID, FleetMemberType.SHIP, "Spikey Boi", true);

		float width = 4000f;
		float height = 4000f;
		api.initMap(-width/2f, width/2f, -height/2f, height/2f);

		ImpossibleGameLevelPlugin plugin = new ImpossibleGameLevelPlugin("chaoz_fantasy");
		api.addPlugin(plugin);
	}

}




