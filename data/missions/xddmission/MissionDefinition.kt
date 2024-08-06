package data.missions.xddmission

import java.util.List

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.fleet.FleetGoal
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.mission.MissionDefinitionAPI
import com.fs.starfarer.api.mission.MissionDefinitionPlugin
import com.fs.starfarer.api.util.Misc

class MissionDefinition: MissionDefinitionPlugin {

    override fun defineMission(api: MissionDefinitionAPI) {


        // Set up the fleets so we can add ships and fighter wings to them.
        // In this scenario, the fleets are attacking each other, but
        // in other scenarios, a fleet may be defending or trying to escape
        api.initFleet(FleetSide.PLAYER, "TTS", FleetGoal.ATTACK, false, 5)
        api.initFleet(FleetSide.ENEMY, "HSS", FleetGoal.ATTACK, true)

        // Set a small blurb for each fleet that shows up on the mission detail and
        // mission results screens to identify each side.
        api.setFleetTagline(FleetSide.PLAYER, "Tri-Tachyon phase group Gamma III")
        api.setFleetTagline(FleetSide.ENEMY, "Hegemony special anti-raider patrol force")

        // These show up as items in the bulleted list under
        // "Tactical Objectives" on the mission detail screen
        api.addBriefingItem("Defeat all enemy forces")
        api.addBriefingItem("Use Sabot SRMs to overload tough targets before finishing them off with Reaper torpedos")
        api.addBriefingItem("Remember: Your armor can safely absorb hits from anti-fighter missiles")

        // Set up the player's fleet.  Variant names come from the
        // files in data/variants and data/variants/fighters
        //api.addToFleet(FleetSide.PLAYER, "harbinger_Strike", FleetMemberType.SHIP, "TTS Invisible Hand", true, CrewXPLevel.VETERAN);
        api.addToFleet(FleetSide.PLAYER, "doom_Strike", FleetMemberType.SHIP, "TTS Invisible Hand", true)
        api.addToFleet(FleetSide.PLAYER, "afflictor_Strike", FleetMemberType.SHIP, "TTS Antithesis", false)
        api.addToFleet(FleetSide.PLAYER, "shade_Assault", FleetMemberType.SHIP, "TTS Blind Consequence", false)

        api.defeatOnShipLoss("TTS Invisible Hand")

        // Set up the enemy fleet.
        //api.addToFleet(FleetSide.ENEMY, "mule_Standard", FleetMemberType.SHIP, false);
        //api.addToFleet(FleetSide.ENEMY, "tarsus_Standard", FleetMemberType.SHIP, false);
        //api.addToFleet(FleetSide.ENEMY, "buffalo2_FS", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "kite_hegemony_Interceptor", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "kite_hegemony_Interceptor", FleetMemberType.SHIP, false)

        api.addToFleet(FleetSide.ENEMY, "enforcer_Elite", FleetMemberType.SHIP, "HSS Judicature", true)
        api.addToFleet(FleetSide.ENEMY, "enforcer_Assault", FleetMemberType.SHIP, "HSS Executor", false)
        api.addToFleet(FleetSide.ENEMY, "lasher_CS", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "lasher_CS", FleetMemberType.SHIP, false)

        api.addToFleet(FleetSide.ENEMY, "condor_Strike", FleetMemberType.SHIP, false)

        // Set up the map.
        val width = 24000f
        val height = 18000f
        api.initMap(-width/2f, width/2f, -height/2f, height/2f)

        val minX = -width/2
        val minY = -height/2

        api.addNebula(minX + width * 0.5f - 300, minY + height * 0.5f, 1000f)
        api.addNebula(minX + width * 0.5f + 300, minY + height * 0.5f, 1000f)


        for ( i in 1..5) {
        val x =  Math.random().toFloat() * width - width/2f
            val y =  Math.random().toFloat() * height - height/2f
            val radius = 100f + Math.random().toFloat() * 400f
            api.addNebula(x, y, radius)
        }

        // Add an asteroid field
        api.addAsteroidField(minX + width/2f, minY + height/2f, 0f, 8000f,
            20f, 70f, 100)

        api.addPlugin(object : BaseEveryFrameCombatPlugin() {
            override fun init(engine: CombatEngineAPI) {
                engine.context.standoffRange = 6000f
            }
        })
    }

}




