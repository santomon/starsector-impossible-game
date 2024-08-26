package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JumpScript extends BaseEveryFrameCombatPlugin {

    public List<String> groundShipIDs = new ArrayList<String>();
    public ShipAPI jumper;
    public JumpSettings settings;
    private boolean gravityIsReversed = false;
    private List<String> jumpInputs;

    public JumpScript(ShipAPI jumper, List<String> groundShipIDs, JumpSettings settings, List<Character> jumpInputs) {
        this.groundShipIDs = groundShipIDs;
        this.jumper = jumper;
        this.settings = settings;
    }



    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        // resolve jumping
        advanceJump(this.jumper, amount);
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
            float accelarationOrSth = this.settings.jumpForce / jumper.getMass() + 0.0000000000001f;
            getLogger().info("jumping with Starting Velocity: " + accelarationOrSth);

            jumper.getVelocity().setY(accelarationOrSth);
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
                    if (getIsJustAbove(this.jumper, ship)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean getIsJustAbove(ShipAPI jumper, ShipAPI groundShip) {
        Vector2f jumperLocation = jumper.getLocation();
        Vector2f groundLocation = groundShip.getLocation();
        boolean jumperIsInXRange = jumperLocation.x > groundLocation.x - this.settings.tileSize / 2 - this.settings.groundTolerance && jumperLocation.x < groundLocation.x + tileSize / 2 + this.settings.groundTolerance;
        if (!jumperIsInXRange) {
            return false;
        }
        boolean jumperIsInYRange  = jumperLocation.y > groundLocation.y + tileSize / 2 - this.settings.groundTolerance && jumperLocation.y < groundLocation.y + tileSize / 2 + this.settings.groundTolerance;
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

    public static Logger getLogger() {
        Logger logger = Global.getLogger(JumpScript.class);
        logger.setLevel(Level.INFO);
        return logger;
    }
}
