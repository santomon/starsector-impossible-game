package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

public class ImpossibleGameEngine implements AdvanceableListener {
    // a lot of the logic is put here to enable pausing maybe?

    public int[][] levelData;
    public ShipAPI playerShip;
    public float currentSecondStack = 0f;  // 1f === 1 sec; advanced amount is in the ~0.017 range usually
    public final float spawnInterval = 1f;
    public final float objectVelocity = 10f;


    public ImpossibleGameEngine(int[][] levelData, float mapSizeX, float mapSizeY) {
        System.out.println("ImpossibleGameEngine constructor");
        this.levelData = levelData;
        this.playerShip = Global.getCombatEngine().getPlayerShip( );
    }


    @Override
    public void advance(float amount) {
        System.out.println("ImpossibleGameEngine advance" + amount);
    }
}
