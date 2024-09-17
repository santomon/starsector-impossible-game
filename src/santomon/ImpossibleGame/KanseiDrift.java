package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.apache.log4j.Logger;
import santomon.ImpossibleGame.hullmods.PortAssault;
import santomon.ImpossibleGame.hullmods.StarboardAssault;

import java.util.ArrayList;
import java.util.List;

public class KanseiDrift extends BaseShipSystemScript {
    Logger log = Global.getLogger(KanseiDrift.class);
    boolean hasBeenActivated = false;
    float facingAtStart;

    static final float targetInitialAngle = 45f;
    static final float explosiveAcceleration = 900f;
    static final float fluxCost = 5000;


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {



        ShipAPI ship = (ShipAPI) stats.getEntity();
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();

        if (!ship.hasListenerOfClass(DelayedFlux.class)) {
            ship.addListener(new DelayedFlux(ship));
        }
        if (!this.hasBeenActivated) {
            this.hasBeenActivated = true;
            List<DelayedFlux> delayedFluxes = ship.getListeners(DelayedFlux.class);
            if (delayedFluxes.isEmpty()) return;  // impossible, right???
            delayedFluxes.get(0).addFlux(fluxCost);
            this.facingAtStart = ship.getFacing();
            combatEngineAPI.getViewport().setExternalControl(true);
        }

        // lock on for the duration of the drift
        combatEngineAPI.getViewport().setCenter(ship.getLocation());

        log.info("effectLevel: " + effectLevel);

        if (state == State.IN) {
            boolean right = ship.getVariant().getHullMods().contains(StarboardAssault.tag);
            if (!right) {
                if (!ship.getVariant().getHullMods().contains(PortAssault.tag)) {
                    return;
                }
            }

            float timePassed = combatEngineAPI.getElapsedInLastFrame();


        }
        ;

        if (state == State.ACTIVE) {
            // actual drifting

        }


        if (state == State.OUT) {
            // state of extreme deceleration
        }


    }

    private void kanseiDriftPhase1() {


    }


    private void kanseiDriftPhase2() {

    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        // called once at the end ig; maybe even once at combat start or sth..
        this.hasBeenActivated = false;
        Global.getCombatEngine().getViewport().setExternalControl(false);
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

    DelayedFlux(ShipAPI ship) {
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

        float ventingMultiplier = this.ship.getFluxTracker().isVenting() ? this.ship.getMutableStats().getVentRateMult().getModifiedValue() : 1;
        float targetAmount = ventingMultiplier * timePassed * fluxRate;
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
        for (int i = fluxToApply.size() - 1; i >= 0; i--) {
            if (fluxToApply.get(i) <= 0) fluxToApply.remove(i);
        }

    }


}