package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Objects;

public class JumpScript extends BaseEveryFrameCombatPlugin {

    public final List<String> groundShipIDs;
    public final ShipAPI jumper;
    public final JumpSettings jumpSettings;
    public final List<Integer> jumpKeys;
    private boolean gravityIsReversed = false;
    private boolean isHoldingMouse = false;

    public static final float rotationSpeed = 1080f;  // for now, lets say deg/sec
    public static final float targetAngle = 0;  // looking to the right
    public static final float rotationSnapRange = rotationSpeed / 36f;
    public static final float timeMultiplier = 4f;
    public static final float realGravity = 3500f;

    public static final float initialVelocity = 600f;

    public JumpScript(ShipAPI jumper, List<String> groundShipIDs, JumpSettings settings, List<Integer> jumpKeys) {
        this.groundShipIDs = groundShipIDs;
        this.jumper = jumper;
        this.jumpSettings = settings;
        this.jumpKeys = jumpKeys;
        this.jumper.getMutableStats().getTimeMult().modifyMult("impossible_timemult", timeMultiplier);
    }



    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;
//        for (InputEventAPI event : events) {
//            event.logEvent();
//        }

        this.applyGravity(amount);
        this.maybeStopFall();
        this.maybeInitiateJump(events);
        this.maybeApplyRotation(amount);
        this.maybeApplyEngineEffect(amount);
    }

    private void maybeApplyEngineEffect(float timePassed) {

        for (ShipEngineControllerAPI.ShipEngineAPI shipEngine : this.jumper.getEngineController().getShipEngines()) {
            if (this.jumper.getFacing() == targetAngle) {
                // full speed ahead
                // wait, how do we """get""" the current flame level 💀
                this.jumper.getEngineController().setFlameLevel(shipEngine.getEngineSlot(), 1);
            } else {
                this.jumper.getEngineController().setFlameLevel(shipEngine.getEngineSlot(), 0.4f);
            }

        }

    }


    public void setGravityIsReversed(boolean reversed) {
        this.gravityIsReversed = reversed;
    }
    public boolean getGravityIsReversed() {
        return this.gravityIsReversed;
    }



    public void maybeInitiateJump(List<InputEventAPI> events) {
        if (!this.getJumpKeyPressed(events)) return;
        if (this.jumper == null) return;
        getLogger().info("successfully triggered jump with keypress");
        initiateJump2();
    }

    private void maybeApplyRotation(float timePassed) {
        if (this.jumper.getVelocity().y != 0) {
            this.applyRotation(timePassed);
        } else {
            this.applyRotation(timePassed, targetAngle);
        }

    }


    private void applyRotation(float timePassed) {
        float currentFacing = this.jumper.getFacing();
        int signum = !this.gravityIsReversed ? -1 : 1;  // rotate clockwise if gravity is normal
        this.jumper.setFacing(currentFacing + signum * timePassed * rotationSpeed);
    }

    private void applyRotation(float timePassed, float _targetAngle) {
        float currentFacing = this.jumper.getFacing();
        if (currentFacing == _targetAngle) return;
        if (Math.abs(currentFacing - _targetAngle) < rotationSnapRange) {
            this.jumper.setFacing(_targetAngle);
        } else {
            applyRotation(timePassed);
        }
    }


    public void initiateJump2() {
        if (this.getJumpingState() == JumpingState.GROUND) {
            int signum = !this.gravityIsReversed ? 1 : -1;
            Vector2f direction = new Vector2f(0, signum);
            direction.set(direction.x * initialVelocity, direction.y * initialVelocity);

            this.jumper.getVelocity().set(this.jumper.getVelocity().x + direction.x, this.jumper.getVelocity().y + direction.y);
            getLogger().info("initiating jump with initial Velocity: " + initialVelocity);
            getLogger().info("actual velocity: " + this.jumper.getVelocity());
        }

    }

    private void applyGravity(float amount) {

        if (this.jumper == null) return;
        int signum = !this.gravityIsReversed ? 1 : -1;

        float dV_y = - signum * amount * Math.abs(realGravity);
        float oldVelocity = this.jumper.getVelocity().y;
        float maybe_new_V_y = oldVelocity + dV_y;
        float new_V_y = maybe_new_V_y;
        jumper.getVelocity().setY(new_V_y);
    }


    private void maybeStopFall() {
        if (this.getIsNearGround()) {
            if (!this.gravityIsReversed) {
                this.jumper.getVelocity().setY(Math.max(0, this.jumper.getVelocity().y));
            } else {
                this.jumper.getVelocity().setY(Math.min(0, this.jumper.getVelocity().y));
            }
        };

    }


    public boolean getIsNearGround() {
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        for (ShipAPI ship : combatEngineAPI.getShips()) {
            for (String entityID : this.groundShipIDs) {
                if (Objects.equals(ship.getVariant().getHullVariantId(), entityID)) {
                    if (getIsJustAbove(ship)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean getIsJustAbove(ShipAPI groundShip) {
        Vector2f jumperLocation = this.jumper.getLocation();
        Vector2f groundLocation = groundShip.getLocation();
        float radius = groundShip.getCollisionRadius() + this.jumper.getCollisionRadius();
        boolean jumperIsInXRange = jumperLocation.x > groundLocation.x - radius - this.jumpSettings.groundTolerance && jumperLocation.x < groundLocation.x + radius + this.jumpSettings.groundTolerance;
        if (!jumperIsInXRange) {
            return false;
        }

        boolean jumperIsInYRange;
        if (!gravityIsReversed) {
            jumperIsInYRange  = jumperLocation.y > groundLocation.y + radius - this.jumpSettings.groundTolerance && jumperLocation.y < groundLocation.y + radius + this.jumpSettings.groundTolerance;
        } else {
            jumperIsInYRange  = jumperLocation.y < groundLocation.y - radius + this.jumpSettings.groundTolerance && jumperLocation.y > groundLocation.y - radius - this.jumpSettings.groundTolerance;
        }
        return jumperIsInYRange;
    }

    public JumpingState getJumpingState() {
        if (Math.abs(this.jumper.getVelocity().y) < 0.001f && getIsNearGround()) {
            return JumpingState.GROUND;
        }
        if (this.jumper.getVelocity().y > 0f) {
            return JumpingState.JUMPING;
        } else {
            return JumpingState.FALLING;
        }
    }


    public boolean getJumpKeyPressed(List<InputEventAPI> events) {
        boolean result = false;


        for (InputEventAPI event : events) {
            if ((event.getEventType() == InputEventType.MOUSE_DOWN && event.getEventValue() == 0) || isHoldingMouse) {
                this.isHoldingMouse = true;
                result = true;
            }

            if (event.getEventType() == InputEventType.MOUSE_UP && event.getEventValue() == 0) {
                this.isHoldingMouse = false;
                result = false;
            }

            for (Integer jumpKey : this.jumpKeys) {
                if (event.getEventValue() == jumpKey) {
                    result = true;
                }
            }

        }
        if (result) {
            getLogger().info("Jump Key Pressed!");
        }
        return result;
    }

    public static float computeInitialVelocity(float desiredJumpHeight, float gravity, float tileSize) {
        // solve s̈ = g;
        float c = 1;
        double t = Math.sqrt( 2 * gravity * desiredJumpHeight * tileSize);
        double initialVelocity =  c * gravity * t;
        return 2000f;
//        return (float) initialVelocity;
    }

    public static Logger getLogger() {
        Logger logger = Global.getLogger(JumpScript.class);
        logger.setLevel(Level.INFO);
        return logger;
    }

    public static float getTileSize(List<String> groundShipIDs) {
        // for now lets determine the tilesize using the first ground ship;
        if (groundShipIDs.isEmpty()) {
            return IGMisc.FALLBACK_VALUES.DEFAULT_TILE_SIZE;
        }

        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        for (ShipAPI ship : combatEngineAPI.getShips()) {
            if (ship.getVariant().getHullVariantId().equals(groundShipIDs.get(0))) {
                return ship.getCollisionRadius() * 2;
            }
        }
        getLogger().warn("trying to retrieve Tile Size while no existing ships match the first groundShipID. using fallback tilesize...");
        return IGMisc.FALLBACK_VALUES.DEFAULT_TILE_SIZE;

    }
}

