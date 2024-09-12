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

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		
		// Set up the fleets so we can add ships and fighter wings to them.
		// In this scenario, the fleets are attacking each other, but
		// in other scenarios, a fleet may be defending or trying to escape
		api.initFleet(FleetSide.PLAYER, "xdd", FleetGoal.ATTACK, false, 5);
		api.initFleet(FleetSide.ENEMY, "FUMO", FleetGoal.ATTACK, true);

		api.setFleetTagline(FleetSide.PLAYER, "Player");
		api.setFleetTagline(FleetSide.ENEMY, "Spikey Bois");

		FleetMemberAPI hermes = api.addToFleet(FleetSide.PLAYER, "impossible_hermes_variant", FleetMemberType.SHIP, "xdd PlayerShip", true);
		api.addToFleet(FleetSide.ENEMY, "impossible_hermes_variant", FleetMemberType.SHIP, "XDD EnemyFlagShip", true);

		float width = 4000f;
		float height = 4000f;
		api.initMap(-width/2f, width/2f, -height/2f, height/2f);

		ImpossibleGameLevelPlugin plugin = new ImpossibleGameLevelPlugin("chaoz_fantasy");
		api.addPlugin(plugin);
	}

}




