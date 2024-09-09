package santomon.ImpossibleGame;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import org.lwjgl.util.vector.Vector2f;

public class ConstantSpeedOverride implements AdvanceableListener {

    ShipAPI ship;
    Vector2f targetSpeed;

    ConstantSpeedOverride(ShipAPI ship, Vector2f targetSpeed) {
        this.ship = ship;
        this.targetSpeed = targetSpeed;
    }

    @Override
    public void advance(float amount) {
        if (ship != null && targetSpeed != null) {
            ship.getVelocity().set(targetSpeed);
        }
    }
}
