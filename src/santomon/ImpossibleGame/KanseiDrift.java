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
import java.util.Objects;

public class KanseiDrift extends BaseShipSystemScript {

    private static final float ANGULAR_ACCELERATION = 200f;
    private static final float STAGE_1_ACCELERATION_ANGLE = 30f;
    static final float MAX_ACCELERATION = 900f;
    static final float MAX_DECELERATION = 300f;

    static final float STAGE_2_VELOCITY_TURNING_SPEED = 45f;
    static final float STAGE_2_ANGULAR_VELOCITY_PROPORTIONALITY_CONSTANT = 6f;
    static final float STAGE_2_MAXIMUM_ANGULAR_VELOCITY = 90f;

    static final float STAGE_3_NORMAL_SPEED_FACTOR = 1f;  // automatic deceleration target speed (factor * ship's max speed);

    static final float fluxCost = 5000;
    private static final float STAGE_1_ACCELERATION_MAX_EFFECT_LEVEL = 0.8f;
    final Logger log = Global.getLogger(KanseiDrift.class);


    boolean hasBeenActivated = false;
    Float initialFacing;
    Boolean initialCursorIsLeft;
    Vector2f initialCursorLocation;
    Vector2f initialShipLocation;
    Vector2f initialCameraCenter;
    float stage2AngularVelocity;
    boolean stage2ReachedTargetAngleOnce = false;
    boolean stage3ReachedTargetSpeedOnce = false;

    private boolean _isSteeringLeft = false;
    private boolean _isSteeringRight = false;

    private final KanseiDriftSteeringListener kanseiDriftSteeringListener;

    public KanseiDrift() {
        this.kanseiDriftSteeringListener = new KanseiDriftSteeringListener();
        this.kanseiDriftSteeringListener.getKanseiDrifts().add(this);
    }


    public void setIsSteeringLeft(boolean isSteeringLeft) {
        this._isSteeringLeft = isSteeringLeft;
    }

    public void setIsSteeringRight(boolean isSteeringRight) {
        this._isSteeringRight = isSteeringRight;
    }

    public boolean isSteeringLeft() {
        return _isSteeringLeft;
    }
    public boolean isSteeringRight() {
        return _isSteeringRight;
    }


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
//        ShipSystemAPI shipSystemAPI;
//        ShipSystemSpecAPI shipSystemSpecAPI;
//        shipSystemSpecAPI.getActive();

        {
            // need to add a plugin to know if the user is steering anywhere
        }



        ShipAPI ship = (ShipAPI) stats.getEntity();
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();

        if (!ship.hasListenerOfClass(DelayedFlux.class)) {
            ship.addListener(new DelayedFlux(ship));
        }
        if (!this.hasBeenActivated) {
            log.info("setting new initial Params");
            this.hasBeenActivated = true;
            List<DelayedFlux> delayedFluxes = ship.getListeners(DelayedFlux.class);
            if (delayedFluxes.isEmpty()) return;  // impossible, right???
            delayedFluxes.get(0).addFlux(fluxCost);
            this.initialFacing = ship.getFacing();
            this.initialCursorLocation = getCursorLocation();
            this.initialShipLocation = new Vector2f().set(ship.getLocation());
            this.initialCursorIsLeft = isCursorLeftOfShip(ship);
            this.stage2ReachedTargetAngleOnce = false;
            this.stage3ReachedTargetSpeedOnce = false;

            combatEngineAPI.addPlugin(kanseiDriftSteeringListener);
//            if (Objects.equals(combatEngineAPI.getPlayerShip(), ship)) {
//                this.initialCameraCenter = new Vector2f().set(combatEngineAPI.getViewport().getCenter());
//                log.info("initial camera center we retrieved: " + this.initialCameraCenter);
//                combatEngineAPI.getViewport().setExternalControl(true);
//            }
        }

        float timePassed = combatEngineAPI.getElapsedInLastFrame();
        // lock on for the duration of the drift

        if (state == State.IN) {
//            boolean right = ship.getVariant().getHullMods().contains(StarboardAssault.tag);
//            if (!right) {
//                if (!ship.getVariant().getHullMods().contains(PortAssault.tag)) {
//                    return;
//                }
//            }
//
            kanseiDriftPhase1(ship, timePassed, effectLevel);
        }
        ;

        if (state == State.ACTIVE) {
            kanseiDriftPhase2(ship, timePassed);

        }


        if (state == State.OUT) {
            // state of extreme deceleration
            kanseiDriftPhase3(ship, timePassed, effectLevel);
        }


    }

    private void kanseiDriftPhase1(ShipAPI ship, float timePassed, float effectLevel ) {
//        if (Objects.equals(ship, Global.getCombatEngine().getPlayerShip())){
//            // smoothly taking over control of the camera
//            ViewportAPI viewportAPI = Global.getCombatEngine().getViewport();
//            float rescaledEffectLevel = Math.min(2 * effectLevel, 1); // finished centering camera at 50% effectLevel
//            viewportAPI.setCenter(
//                    new Vector2f(
//                            this.initialCameraCenter.x * (1 - rescaledEffectLevel) + ship.getLocation().x * rescaledEffectLevel,
//                            this.initialCameraCenter.y * (1 - rescaledEffectLevel) + ship.getLocation().y * rescaledEffectLevel
//                    )
//            );
//        }

        float direction = this.initialCursorIsLeft ? 1 : -1;

        float accelerationAngle = direction * STAGE_1_ACCELERATION_ANGLE + initialFacing;
        Vector2f accelerationDir = new Vector2f(
                (float) Math.cos(Math.toRadians(accelerationAngle)),
                (float) Math.sin(Math.toRadians(accelerationAngle))
        );


        // Apply acceleration
        if (effectLevel < STAGE_1_ACCELERATION_MAX_EFFECT_LEVEL) {
            float accelerationMagnitude = MAX_ACCELERATION * timePassed * (1 - effectLevel);
            ship.getVelocity().setX(ship.getVelocity().x + accelerationDir.x * accelerationMagnitude);
            ship.getVelocity().setY(ship.getVelocity().y + accelerationDir.y * accelerationMagnitude);
        }

        // Rotate ship to face away from the initial cursor position
        // maybe its better if we dont care about the actual cursor position, but only whether the cursor is left or right of us
        float acceleratingOrDecelerating = effectLevel < 0.5 ? 1 : -1;
        float dAngle = - direction * timePassed * acceleratingOrDecelerating * ANGULAR_ACCELERATION;
        ship.setAngularVelocity(ship.getAngularVelocity() + dAngle);

    }

    private Vector2f getCursorLocation() {
        // think this works as intended
        ViewportAPI viewportAPI = Global.getCombatEngine().getViewport();
        float cursorX = viewportAPI.convertScreenXToWorldX(Mouse.getX());
        float cursorY = viewportAPI.convertScreenYToWorldY(Mouse.getY());
        return new Vector2f(cursorX, cursorY);
    }

    private boolean isCursorLeftOfShip(ShipAPI ship) {
        Vector2f shipLocation = ship.getLocation();
        float shipFacingAngle = ship.getFacing();
        float leftAngle = shipFacingAngle + 90f;

        Vector2f cursorLocation = getCursorLocation();
        Vector2f toCursor = Vector2f.sub(cursorLocation, shipLocation, null);

        Vector2f leftVector = new Vector2f(
                (float) Math.cos(Math.toRadians(leftAngle)),
                (float) Math.sin(Math.toRadians(leftAngle))
        );
        return Vector2f.dot(toCursor, leftVector) > 0;
    }


    private void kanseiDriftPhase2(ShipAPI ship, float timePassed) {
        // maybe it's much more
        // to note; getFacing returns angles from 0 - 360deg, counterclockwise starting at 0 == east.
//        if (Objects.equals(ship, Global.getCombatEngine().getPlayerShip())) {
//            Global.getCombatEngine().getViewport().setCenter(ship.getLocation());
//        }

        Vector2f cursorLocation = getCursorLocation();
        Vector2f toCursor = Vector2f.sub(cursorLocation, ship.getLocation(), null);
        {
            // first, update velocity direction
            // for now, maintain velocity, regardless of distance to cursor or any other factors;
            float angleToCover = KanseiDrift.angleBetween(ship.getVelocity(), toCursor, false);
            float signum = angleToCover > 0 ? 1 : -1;
            float rotationAngleSize = Math.min(Math.abs(angleToCover * timePassed), STAGE_2_VELOCITY_TURNING_SPEED * timePassed);
            Vector2f newVelocityVector = KanseiDrift.rotate(ship.getVelocity(), rotationAngleSize * signum);
            ship.getVelocity().set(newVelocityVector);
        }


        {
            // update angular velocity..
            float direction = initialCursorIsLeft ? 1 : -1;
            float targetFacing = as0to360Angle(toCursor) + direction * 90f;
            targetFacing = targetFacing < 0 ? targetFacing + 360f : targetFacing;
            targetFacing = targetFacing % 360;

            float newAngularVelocity = STAGE_2_ANGULAR_VELOCITY_PROPORTIONALITY_CONSTANT * (targetFacing - ship.getFacing());
//            float newAngularVelocity = ship.getAngularVelocity() + acceleration * timePassed;
            newAngularVelocity = (float) Math.max(- STAGE_2_MAXIMUM_ANGULAR_VELOCITY, Math.min(newAngularVelocity, STAGE_2_MAXIMUM_ANGULAR_VELOCITY));
//            log.info("newAngularVelocity: " + newAngularVelocity);
            ship.setAngularVelocity(newAngularVelocity);
            this.stage2AngularVelocity = newAngularVelocity;
        }


    }


    private void kanseiDriftPhase3(ShipAPI ship, float timePassed, float effectLevel){

        // think angular velocity should probably be automatically decelerating
        // for now let's use effectLevel to determine the angular velocity
        ship.setAngularVelocity(stage2AngularVelocity * effectLevel);


    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        // called once at the end ig; maybe even once at combat start or sth..
        this.hasBeenActivated = false;
        this.initialFacing = null;
        this.initialCursorLocation = null;
        this.initialShipLocation = null;
        this.initialCursorIsLeft = null;
        this.initialCameraCenter = null;
        this.stage2ReachedTargetAngleOnce = false;
        this.stage3ReachedTargetSpeedOnce = false;

        Global.getCombatEngine().removePlugin(this.kanseiDriftSteeringListener);

//        if (Objects.equals(Global.getCombatEngine().getPlayerShip(), (ShipAPI) stats.getEntity())) {
//            log.info("relinquishing camera control");
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

    public static float angleBetween(Vector2f v1, Vector2f v2, boolean as0to360Angle) {
        // the Vector2f.angle only returns positive values between 0 and pi...
        // Normalize the vectors
        Vector2f normV1 = new Vector2f(v1);
        normV1.normalise();
        Vector2f normV2 = new Vector2f(v2);
        normV2.normalise();

        float angleRadians = Vector2f.angle(normV1, normV2);

        // Convert to degrees
        float angleDegrees = (float) Math.toDegrees(angleRadians);
        float crossProduct = normV1.x * normV2.y - normV1.y * normV2.x;

        // Calculate the 2D cross product (determinant in 2D)
        if (as0to360Angle) {
            // If the cross product is negative, the angle is clockwise, so adjust the result
            if (crossProduct < 0) {
                angleDegrees = 360.0f - angleDegrees;
            }
        } else {
            angleDegrees = crossProduct < 0 ? -angleDegrees : angleDegrees;
        }

        return angleDegrees;
    }


    public static Vector2f rotate(Vector2f v, float angleDegrees) {
        // Convert the angle from degrees to radians
        float angleRadians = (float) Math.toRadians(angleDegrees);

        // Get the cosine and sine of the angle
        float cosTheta = (float) Math.cos(angleRadians);
        float sinTheta = (float) Math.sin(angleRadians);

        // Apply the rotation matrix
        float newX = v.x * cosTheta - v.y * sinTheta;
        float newY = v.x * sinTheta + v.y * cosTheta;

        // Return the rotated vector as a new Vector2f
        return new Vector2f(newX, newY);
    }

    public static float as0to360Angle(Vector2f v) {
        float angleRadians = (float) Math.atan2(v.y, v.x);

        // Convert radians to degrees
        float angleDegrees = (float) Math.toDegrees(angleRadians);

        // Ensure the angle is within the range [0, 360]
        if (angleDegrees < 0) {
            angleDegrees += 360.0f;
        }
        return angleDegrees;
    }

    public static Vector2f asVector2f(float angleInDeg) {
        return new Vector2f(
                (float) Math.cos(Math.toRadians(angleInDeg)),
                (float) Math.sin(Math.toRadians(angleInDeg))
        );
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