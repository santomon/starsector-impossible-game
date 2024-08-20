package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

public class ImpossibleGameEngine implements AdvanceableListener {
    // a lot of the logic is put here to enable pausing maybe?

    public int[][] levelData;
    public ShipAPI playerShip;


    public ImpossibleGameEngine(int[][] levelData) {
        System.out.println("ImpossibleGameEngine constructor");
        this.levelData = levelData;
        this.playerShip = Global.getCombatEngine().getPlayerShip( );
    }


    @Override
    public void advance(float amount) {
        System.out.println("ImpossibleGameEngine advance");
    }
}
