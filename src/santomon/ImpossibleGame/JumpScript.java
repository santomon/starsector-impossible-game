package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JumpScript extends BaseEveryFrameCombatPlugin {

    public final List<String> groundShipIDs;
    public final ShipAPI jumper;
    public final JumpSettings settings;
    public final List<Character> jumpKeys;
    private boolean gravityIsReversed = false;

    public JumpScript(ShipAPI jumper, List<String> groundShipIDs, JumpSettings settings, List<Character> jumpKeys) {
        this.groundShipIDs = groundShipIDs;
        this.jumper = jumper;
        this.settings = settings;
        this.jumpKeys = jumpKeys;
    }



    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        // resolve jumping
        advanceJump(this.jumper, amount);

        if (this.getJumpKeyPressed(events)) {
            maybeInitiateJump();
        }
    }


    public void setGravityIsReversed(boolean reversed) {
        this.gravityIsReversed = reversed;
    }
    public boolean getGravityIsReversed() {
        return this.gravityIsReversed;
    }



    public void maybeInitiateJump() {
        if (this.jumper == null) return;
        initiateJump();
    }


    public void initiateJump() {
        if (this.getJumpingState() == JumpingState.GROUND) {
            getLogger().info("jumping with Force: " + this.settings.jumpForce);
            int signum = !this.gravityIsReversed ? 1 : -1;
            Vector2f direction = new Vector2f(0, signum);
            CombatUtils.applyForce(this.jumper, direction, this.settings.jumpForce );
        }
    }

    public void advanceJump(ShipAPI jumper, float amount) {
        if (this.jumper == null) return;
        if (this.getJumpingState() == JumpingState.GROUND) return;

        if (this.getIsNearGround()) {
            jumper.getVelocity().setY(0);
        }


        float dV_y = - amount * Math.abs(this.settings.gravity);
        Vector2f oldVelocity = jumper.getVelocity();
        float maybe_new_V_y = oldVelocity.y + dV_y;
        float new_V_y = Math.abs(this.settings.maxVelocity) < Math.abs(maybe_new_V_y) ? Math.signum(maybe_new_V_y) * Math.abs(this.settings.maxVelocity) : maybe_new_V_y;
        jumper.getVelocity().setY(new_V_y);
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
        float radius = groundShip.getCollisionRadius();
        boolean jumperIsInXRange = jumperLocation.x > groundLocation.x - radius - this.settings.groundTolerance && jumperLocation.x < groundLocation.x + radius + this.settings.groundTolerance;
        if (!jumperIsInXRange) {
            return false;
        }
        boolean jumperIsInYRange  = jumperLocation.y > groundLocation.y + radius - this.settings.groundTolerance && jumperLocation.y < groundLocation.y + radius + this.settings.groundTolerance;
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
        for (InputEventAPI event : events) {
            if (event.getEventType() == InputEventType.MOUSE_DOWN && event.getEventValue() == 0) {
                return true;
            }
            for (Character jumpKey : this.jumpKeys) {
                if (event.getEventChar() == jumpKey) {
                    return true;
                }
            }

        }
        return false;
    }

    public static Logger getLogger() {
        Logger logger = Global.getLogger(JumpScript.class);
        logger.setLevel(Level.INFO);
        return logger;
    }
}
