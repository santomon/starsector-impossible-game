package data.missions.xddmission;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import org.json.JSONObject;
import org.lwjgl.Sys;
import santomon.ImpossibleGame.ImpossibleGameLevelPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		
		// Set up the fleets so we can add ships and fighter wings to them.
		// In this scenario, the fleets are attacking each other, but
		// in other scenarios, a fleet may be defending or trying to escape
		api.initFleet(FleetSide.PLAYER, "xdd", FleetGoal.ATTACK, false, 5);
		api.initFleet(FleetSide.ENEMY, "fumo", FleetGoal.ATTACK, true);



		// Set a small blurb for each fleet that shows up on the mission detail and
		// mission results screens to identify each side.
		api.setFleetTagline(FleetSide.PLAYER, "Tri-Tachyon phase group Gamma III");
		api.setFleetTagline(FleetSide.ENEMY, "Hegemony special anti-raider patrol force");
		
		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("Defeat all enemy forces");
		api.defeatOnShipLoss("TTS Invisible Hand");
		
		// Set up the enemy fleet.
		api.addToFleet(FleetSide.ENEMY, "enforcer_Elite", FleetMemberType.SHIP, "HSS Judicature", true);

		// Set up the map.
		float width = 4000f;
		float height = 4000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);

		ImpossibleGameLevelPlugin plugin = new ImpossibleGameLevelPlugin();
		api.addPlugin(plugin);

		int[][] data = ImpossibleGameLevelPlugin.loadLevelData("xdd");
        for (int[] datum : data) {
            for (int i : datum) {
                System.out.print(i + " ");
            }
            System.out.println();
        }


	}

}




