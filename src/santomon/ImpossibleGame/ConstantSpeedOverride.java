package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class ConstantSpeedOverride extends BaseEveryFrameCombatPlugin {

    ShipAPI ship;
    Vector2f targetSpeed;

    ConstantSpeedOverride(ShipAPI ship, Vector2f targetSpeed) {
        this.ship = ship;
        this.targetSpeed = targetSpeed;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        if (ship != null && targetSpeed != null) {
            ship.getVelocity().set(targetSpeed);
        }
        if (ship == null) {
            CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
            combatEngineAPI.removePlugin(this);
        }
    }
}
