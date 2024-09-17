package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class KanseiDrift extends BaseShipSystemScript {
    Logger log = Global.getLogger(KanseiDrift.class);

    static final float targetInitialAngle = 45f;
    static final float explosiveAcceleration = 900f;


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();

        if (!ship.hasListenerOfClass(DelayedFlux.class)) {
            ship.addListener(new DelayedFlux(ship));
        }

        log.info("effectLevel: " + effectLevel);

        if (state == State.IN) ;



    }

    private void kanseiDriftPhase1() {

    }



    public void unapply(MutableShipStatsAPI stats, String id) {
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }


    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return true;
    }

    public float getActiveOverride(ShipAPI ship) {
        return -1;
    }

    public float getInOverride(ShipAPI ship) {
        return -1;
    }

    public float getOutOverride(ShipAPI ship) {
        return -1;
    }

    public float getRegenOverride(ShipAPI ship) {
        return -1;
    }

    public int getUsesOverride(ShipAPI ship) {
        return -1;
    }


}


class DelayedFlux implements AdvanceableListener {

    final ShipAPI ship;
    final List<Float> fluxToApply = new ArrayList<Float>();

    final float fluxRate = 400; // flux/s
    final boolean hardFlux = true;

    DelayedFlux (ShipAPI ship) {
        this.ship = ship;
    }

    public void addFlux(float amount) {
        this.fluxToApply.add(amount);
    }

    @Override
    public void advance(float amount) {
        if (ship == null) return;

        applyFlux(amount);
        pruneFluxToApply();
    }

    private void applyFlux(float timePassed) {

        float targetAmount = fluxRate * timePassed;
        float stack = 0;
        for (int i = 0; i < fluxToApply.size(); i++) {
            float amount = Math.min(fluxToApply.get(i), targetAmount);
            amount = Math.max(0, amount);
            stack += amount;
            fluxToApply.set(i, fluxToApply.get(i) - amount);
        }
        ship.getFluxTracker().increaseFlux(stack, hardFlux);
    }

    private void pruneFluxToApply() {
        for (int i = fluxToApply.size() - 1; i >= 0 ; i--) {
            if (fluxToApply.get(i) <= 0) fluxToApply.remove(i);
        }

    }


}