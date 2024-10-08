package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import org.lwjgl.Sys;
import org.lwjgl.util.vector.Vector2f;

public class KillPlayerWhenAnyPlayerDamageIsTaken implements DamageListener {

    public final float damage = 1000000;
    public final float graceTimeInSeconds = 1;
    public final String killScriptID = "impossible_KillPlayerWhenAnyPlayerDamageIsTaken";
    private final float atInstantiationTimeStamp;

    KillPlayerWhenAnyPlayerDamageIsTaken() {
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        atInstantiationTimeStamp = combatEngineAPI.getTotalElapsedTime(false);
    }


    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        if (combatEngineAPI.getTotalElapsedTime(false) - atInstantiationTimeStamp < graceTimeInSeconds) return;

        if (target.getOwner() == 0) {
            for (ShipAPI ship : combatEngineAPI.getShips()) {
                if (ship.getOwner() == 0 && source != killScriptID) {
                    combatEngineAPI.applyDamage(ship, ship.getLocation(),
                            damage, DamageType.HIGH_EXPLOSIVE,
                            0, false,
                            false,
                            killScriptID,
                            false
                    );
                }
            }
        }

    }
}
