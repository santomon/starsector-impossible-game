package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.apache.log4j.Logger;

public class KanseiDrift extends BaseShipSystemScript {
    Logger log = Global.getLogger(KanseiDrift.class);
    ShipAPI ship;


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        log.info("effectLevel: " + effectLevel);
        if (this.ship == null) return;  // shouldnt be possible, but just in case;

        if (state == State.IN) ;



    }

    public void unapply(MutableShipStatsAPI stats, String id) {
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }


    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        // gets called like, every frame
        if (this.ship == null) this.ship = ship;
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
