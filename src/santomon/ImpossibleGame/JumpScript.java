package santomon.ImpossibleGame;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

import java.util.ArrayList;
import java.util.List;

public class JumpScript implements AdvanceableListener {

    public List<String> groundShipIDs = new ArrayList<String>();
    public ShipAPI jumper;
    public JumpSettings settings;
    private boolean gravityIsReversed = false;

    public JumpScript(ShipAPI jumper, List<String> groundShipIDs, JumpSettings settings) {
        this.groundShipIDs = groundShipIDs;
        this.jumper = jumper;
        this.settings = settings;
    }


    @Override
    public void advance(float amount) {

    }

    public void maybeTriggerJump() {

    }

    public void setGravityIsReversed(boolean reversed) {
        this.gravityIsReversed = reversed;
    }
    public boolean getGravityIsReversed() {
        return this.gravityIsReversed;
    }
}
