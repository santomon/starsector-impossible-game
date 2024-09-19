package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class KanseiDriftSteeringListener extends BaseEveryFrameCombatPlugin {

    private Logger log = Global.getLogger(KanseiDriftSteeringListener.class);

    private Integer leftKey;
    private Integer rightKey;

    private final List<KanseiDrift> kanseiDrifts = new ArrayList<KanseiDrift>();

    public List<KanseiDrift> getKanseiDrifts() {
        return kanseiDrifts;
    }




    public void init(CombatEngineAPI engine) {
        this.leftKey = LunaSettings.getInt(IGMisc.LunaLibKeys.IG_MOD_ID, IGMisc.LunaLibKeys.STEER_LEFT_KEY_ID);
        this.rightKey = LunaSettings.getInt(IGMisc.LunaLibKeys.IG_MOD_ID, IGMisc.LunaLibKeys.STEER_RIGHT_KEY_ID);
    }


    public void advance(float amount, List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (Objects.equals(event.getEventValue(), this.leftKey)) {
                if(event.isKeyDownEvent()) {
                    for (KanseiDrift kanseiDrift : kanseiDrifts) {
                        kanseiDrift.setIsSteeringLeft(true);
                    }
                    log.info("left key down");
                }
                if (event.isKeyUpEvent()) {
                    for (KanseiDrift kanseiDrift : kanseiDrifts) {
                        kanseiDrift.setIsSteeringLeft(false);
                    }
                    log.info("left key up");
                }
            }

            if (Objects.equals(event.getEventValue(), this.rightKey)) {
                if (event.isKeyDownEvent()) {
                    for (KanseiDrift kanseiDrift : kanseiDrifts) {
                        kanseiDrift.setIsSteeringRight(true);
                    }
                    log.info("right key down");
                }
                if (event.isKeyUpEvent()) {
                    for (KanseiDrift kanseiDrift : kanseiDrifts) {
                        kanseiDrift.setIsSteeringRight(false);
                    }
                    log.info("right key up");
                }
            }
        }





    }



}