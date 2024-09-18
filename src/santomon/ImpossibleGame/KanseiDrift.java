package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.apache.log4j.Logger;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class KanseiDrift extends BaseShipSystemScript {
    private static final float ANGULAR_ACCELERATION = 200f;
    Logger log = Global.getLogger(KanseiDrift.class);
    boolean hasBeenActivated = false;
    Float initialFacing;
    Vector2f initialCursorLocation;
    Vector2f initialShipLocation;

    static final float MAX_ACCELERATION = 300f;
    static final float MAX_DECELERATION = 300f;
    static final float NORMAL_SPEED = 30f;

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
            this.initialFacing = ship.getFacing();
            this.initialCursorLocation = getCursorLocation();
            this.initialShipLocation = ship.getLocation();
//            combatEngineAPI.getViewport().setExternalControl(true);
        }

        // lock on for the duration of the drift
//        if (Objects.equals(combatEngineAPI.getPlayerShip(), ship)) combatEngineAPI.getViewport().setCenter(ship.getLocation());

        log.info("effectLevel: " + effectLevel);

        if (state == State.IN) {
//            boolean right = ship.getVariant().getHullMods().contains(StarboardAssault.tag);
//            if (!right) {
//                if (!ship.getVariant().getHullMods().contains(PortAssault.tag)) {
//                    return;
//                }
//            }
//
            float timePassed = combatEngineAPI.getElapsedInLastFrame();
            kanseiDriftPhase1(ship, timePassed, effectLevel);


        }
        ;

        if (state == State.ACTIVE) {
            // actual drifting

        }


        if (state == State.OUT) {
            // state of extreme deceleration
        }


    }

    private void kanseiDriftPhase1(ShipAPI ship, float timePassed, float effectLevel ) {
        // Fetch current positions
        Vector2f shipPos = ship.getLocation();
        Vector2f cursorPos = getCursorLocation();

        // Calculate direction vectors
        Vector2f toCursor = Vector2f.sub(cursorPos, shipPos, null);
        toCursor.normalise();

        float shipFacingAngle = ship.getFacing();
        Vector2f shipFacingVector = new Vector2f(
                (float) Math.cos(Math.toRadians(shipFacingAngle)),
                (float) Math.sin(Math.toRadians(shipFacingAngle))
        );

        // Weighted average direction (more weight towards ship's facing)
        float weightFacing = 0.7f;
        float weightCursor = 0.3f;
        Vector2f accelerationDir = new Vector2f(
                shipFacingVector.x * weightFacing + toCursor.x * weightCursor,
                shipFacingVector.y * weightFacing + toCursor.y * weightCursor
        );
        accelerationDir.normalise();


        log.info("cursorLocation: " + cursorPos);
        log.info("shipLocation: " + shipPos);
        log.info("shipFacingAngle: " + shipFacingAngle);
        log.info("isCursorLeftOfShip: " + isCursorLeftOfShip(ship, true));

        // Apply acceleration
        float accelerationMagnitude = MAX_ACCELERATION * timePassed * (1 - effectLevel);
        ship.getVelocity().x += accelerationDir.x * accelerationMagnitude;
        ship.getVelocity().y += accelerationDir.y * accelerationMagnitude;

        // Rotate ship to face away from the initial cursor position
        // maybe its better if we dont care about the actual cursor position, but only whether the cursor is left or right of us
        float direction = isCursorLeftOfShip(ship, true) ? -1 : 1;
        float acceleratingOrDecelerating = effectLevel < 0.5 ? 1 : -1;
        float dAngle = direction * timePassed * acceleratingOrDecelerating * ANGULAR_ACCELERATION;
        ship.setAngularVelocity(ship.getAngularVelocity() + dAngle);

    }

    private Vector2f getCursorLocation() {
        // think this works as intended
        ViewportAPI viewportAPI = Global.getCombatEngine().getViewport();
        float cursorX = viewportAPI.convertScreenXToWorldX(Mouse.getX());
        float cursorY = viewportAPI.convertScreenYToWorldY(Mouse.getY());

        return new Vector2f(cursorX, cursorY);
    }

    private boolean isCursorLeftOfShip(ShipAPI ship, boolean useInitialParams) {
        Vector2f shipPos = useInitialParams ? this.initialShipLocation : ship.getLocation();
        float shipFacingAngle = useInitialParams ? this.initialFacing : ship.getFacing();
        float leftAngle = shipFacingAngle + 90f;

        Vector2f cursorLocation = useInitialParams? this.initialCursorLocation : getCursorLocation();
        Vector2f toCursor = Vector2f.sub(cursorLocation, shipPos, null);

        Vector2f leftVector = new Vector2f(
                (float) Math.cos(Math.toRadians(leftAngle)),
                (float) Math.sin(Math.toRadians(leftAngle))
        );
        return Vector2f.dot(toCursor, leftVector) > 0;
    }


    private void kanseiDriftPhase2() {

    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        // called once at the end ig; maybe even once at combat start or sth..
        this.hasBeenActivated = false;
        this.initialFacing = null;
        this.initialCursorLocation = null;
        this.initialShipLocation = null;
//        if (Objects.equals(Global.getCombatEngine().getPlayerShip(), (ShipAPI) stats.getEntity())) {
//            Global.getCombatEngine().getViewport().setExternalControl(false);
//        }
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