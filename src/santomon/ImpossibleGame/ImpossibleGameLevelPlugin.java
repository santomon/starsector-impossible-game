package santomon.ImpossibleGame;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;

public class ImpossibleGameLevelPlugin extends BaseEveryFrameCombatPlugin {

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
//        System.out.println("ImpossibleGameLevelPlugin advance");

    }

    @Override
    public void init(CombatEngineAPI engine) {
//        System.out.println("ImpossibleGameLevelPlugin init");
    }
}
