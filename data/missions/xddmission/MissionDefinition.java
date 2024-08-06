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
		api.initFleet(FleetSide.PLAYER, "TTS", FleetGoal.ATTACK, false, 5);
		api.initFleet(FleetSide.ENEMY, "HSS", FleetGoal.ATTACK, true);

		// Set a small blurb for each fleet that shows up on the mission detail and
		// mission results screens to identify each side.
		api.setFleetTagline(FleetSide.PLAYER, "Tri-Tachyon phase group Gamma III");
		api.setFleetTagline(FleetSide.ENEMY, "Hegemony special anti-raider patrol force");
		
		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("Defeat all enemy forces");

		// Set up the player's fleet.  Variant names come from the
		// files in data/variants and data/variants/fighters
		//api.addToFleet(FleetSide.PLAYER, "harbinger_Strike", FleetMemberType.SHIP, "TTS Invisible Hand", true, CrewXPLevel.VETERAN);
		api.addToFleet(FleetSide.PLAYER, "doom_Strike", FleetMemberType.SHIP, "TTS Invisible Hand", true);
		api.addToFleet(FleetSide.PLAYER, "afflictor_Strike", FleetMemberType.SHIP, "TTS Antithesis", false);
		api.addToFleet(FleetSide.PLAYER, "shade_Assault", FleetMemberType.SHIP, "TTS Blind Consequence", false);

		api.defeatOnShipLoss("TTS Invisible Hand");
		
		// Set up the enemy fleet.
		api.addToFleet(FleetSide.ENEMY, "enforcer_Elite", FleetMemberType.SHIP, "HSS Judicature", true);

		// Set up the map.
		float width = 24000f;
		float height = 18000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;
		
		api.addNebula(minX + width * 0.5f - 300, minY + height * 0.5f, 1000);
		api.addNebula(minX + width * 0.5f + 300, minY + height * 0.5f, 1000);
		
		for (int i = 0; i < 5; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/2;
			float radius = 100f + (float) Math.random() * 400f; 
			api.addNebula(x, y, radius);
		}
		
		api.addPlugin(new ImpossibleGameLevelPlugin());
		int[][] data = loadLevelData("xdd");
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				System.out.print(data[i][j] + " ");
			}
			System.out.println();
		}


	}
	public int[][] loadLevelData(String levelName){
		try{
			String levelDataRaw = Global.getSettings().loadText("data/missions/"+levelName+"/level_data.txt");
			String[] Q = levelDataRaw.split("\n");
			String firstRow = Q[0].endsWith(",") ? Q[0].substring(0, Q[0].length()-1) : Q[0];
			int[][] data = new int[Q.length][firstRow.split(",").length];

            for (int i = 0; i < Q.length; i++) {
				String charactersTMP = Q[i].endsWith(",") ? Q[i].substring(0, Q[i].length()-1) : Q[i];
				String[] characters = charactersTMP.split(",");
				for (int j = 0; j < characters.length; j++) {
					data[i][j] = Integer.parseInt(characters[j]);
				}
            }
			return data;
		} catch (Exception e){
			throw new RuntimeException("Failed to load level data", e);
		}
	}


}




