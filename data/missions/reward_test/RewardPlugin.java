package data.missions.reward_test;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Logger;
import santomon.ImpossibleGame.DontMoveAI;

import java.util.List;

public class RewardPlugin extends BaseEveryFrameCombatPlugin {

    Logger logger = Logger.getLogger(RewardPlugin.class);

    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() == 1) {
                for(WeaponAPI weaponAPI : ship.getAllWeapons()) {
                    weaponAPI.disable(true);
                }

            }

        }

    }

}
