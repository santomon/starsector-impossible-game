package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

public class DamageWhenOutOfBounds implements AdvanceableListener {

    final ShipAPI ship;
    final float boundMultiplier = 0.8f;

    DamageWhenOutOfBounds(ShipAPI ship) {
        this.ship = ship;
    }

    @Override
    public void advance(float amount) {

        float width = Global.getCombatEngine().getMapWidth();
        float height = Global.getCombatEngine().getMapHeight();

        if (Math.abs(ship.getLocation().x) < width / 2 * boundMultiplier && Math.abs(ship.getLocation().y) < height / 2 * boundMultiplier) return;
        Global.getCombatEngine().applyDamage(ship, ship.getLocation(), 1f, DamageType.HIGH_EXPLOSIVE, 0f, true, false, this, false);


    }
}
