package data.missions.reward_test;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import santomon.ImpossibleGame.ImpossibleGameLevelPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		api.initFleet(FleetSide.PLAYER, "xdd", FleetGoal.ATTACK, false, 5);
		api.initFleet(FleetSide.ENEMY, "FUMO", FleetGoal.ATTACK, true);

		api.setFleetTagline(FleetSide.PLAYER, "Player");
		api.setFleetTagline(FleetSide.ENEMY, "Spikey Bois");

		FleetMemberAPI hermes = api.addToFleet(FleetSide.PLAYER, "impossible_conquest_Standard", FleetMemberType.SHIP, "xdd PlayerShip", true);
		FleetMemberAPI enemy = api.addToFleet(FleetSide.ENEMY, "impossible_conquest_Standard", FleetMemberType.SHIP, "xdd PlayerShip", true);

		float width = 4000f;
		float height = 4000f;
		api.initMap(-width/2f, width/2f, -height/2f, height/2f);

	}

}




