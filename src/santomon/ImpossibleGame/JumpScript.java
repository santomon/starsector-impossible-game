package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JumpScript extends BaseEveryFrameCombatPlugin {

    public final List<String> groundShipIDs;
    public final ShipAPI jumper;
    public final JumpSettings settings;
    public final List<Integer> jumpKeys;
    private boolean gravityIsReversed = false;
    public static final float rotationSpeed = 1080f;  // for now, lets say deg/sec
    public static final float targetAngle = 0;  // looking to the right
    public static final float rotationSnapRange = rotationSpeed / 36f;

    public JumpScript(ShipAPI jumper, List<String> groundShipIDs, JumpSettings settings, List<Integer> jumpKeys) {
        this.groundShipIDs = groundShipIDs;
        this.jumper = jumper;
        this.settings = settings;
        this.jumpKeys = jumpKeys;
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
        getLogger().info("successfully triggered jump with keypress");
        if (this.jumper == null) return;
        initiateJump();
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


    public void initiateJump() {
        if (this.getJumpingState() == JumpingState.GROUND) {
            getLogger().info("jumping with Force: " + this.settings.jumpForce);
            int signum = !this.gravityIsReversed ? 1 : -1;
            Vector2f direction = new Vector2f(0, signum);
            CombatUtils.applyForce(this.jumper, direction, this.settings.jumpForce );
        }
    }

    private void applyGravity(float amount) {
        if (this.jumper == null) return;
        int signum = !this.gravityIsReversed ? 1 : -1;

        float dV_y = - signum * amount * Math.abs(this.settings.gravity);
        Vector2f oldVelocity = jumper.getVelocity();
        float maybe_new_V_y = oldVelocity.y + dV_y;
        float new_V_y = Math.abs(this.settings.maxVelocity) < Math.abs(maybe_new_V_y) ? Math.signum(maybe_new_V_y) * Math.abs(this.settings.maxVelocity) : maybe_new_V_y;
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
        boolean jumperIsInXRange = jumperLocation.x > groundLocation.x - radius - this.settings.groundTolerance && jumperLocation.x < groundLocation.x + radius + this.settings.groundTolerance;
        if (!jumperIsInXRange) {
            return false;
        }

        boolean jumperIsInYRange;
        if (!gravityIsReversed) {
            jumperIsInYRange  = jumperLocation.y > groundLocation.y + radius - this.settings.groundTolerance && jumperLocation.y < groundLocation.y + radius + this.settings.groundTolerance;
        } else {
            jumperIsInYRange  = jumperLocation.y < groundLocation.y - radius + this.settings.groundTolerance && jumperLocation.y > groundLocation.y - radius - this.settings.groundTolerance;
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
            if (event.getEventType() == InputEventType.MOUSE_DOWN && event.getEventValue() == 0) {
                result = true;
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

    public static Logger getLogger() {
        Logger logger = Global.getLogger(JumpScript.class);
        logger.setLevel(Level.INFO);
        return logger;
    }
}
