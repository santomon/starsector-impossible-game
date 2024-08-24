package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import org.lwjgl.util.vector.Vector2f;

public class KillPlayerWhenAnyPlayerDamageIsTaken implements DamageListener {

    public final float damage = 1000000;
    public final String killTag = "IG_Killed";

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {

        if (target.getOwner() == 0) {
            CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
            for (ShipAPI ship : combatEngineAPI.getShips()) {
                if (ship.getOwner() == 0 && !ship.getTags().contains(killTag)) {
                    combatEngineAPI.applyDamage(ship, ship.getLocation(),
                            damage, DamageType.HIGH_EXPLOSIVE,
                            0, false,
                            false,
                            Global.getCombatEngine().getPlayerShip(),
                            false
                    );
                    ship.addTag(killTag);
                }
            }
        }

    }
}
