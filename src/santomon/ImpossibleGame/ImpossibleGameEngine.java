package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;
import data.missions.xddmission.IGMisc;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;

public class ImpossibleGameEngine implements AdvanceableListener {
    // a lot of the logic is put here to enable pausing maybe?

    public int[][] levelData;
    public ShipAPI playerShip;
    public float currentSecondStack = 0f;  // 1f === 1 sec; advanced amount is in the ~0.017 range usually
    public int currentLevelStage = 0;
    public float mapSizeX;
    public float mapSizeY;


    public static final float spawnInterval = 1f;
    public static final float objectVelocity = 1000f;
    public static final float topPadding = 100f;
    public static final float rightPadding = 100f;
    public static final float tileSize = 128f;  // kite has  a collision radius of 64
    public static final HashMap<Integer, String> objectLookUpTable = new HashMap<Integer, String>() {{
        put(0, null);
        put(1, IGMisc.Constants.IG_DEFENDER_VARIANT_ID);  // block
        put(2, IGMisc.Constants.IG_KITE_VARIANT_ID);  // spikes
    }
    };


    public ImpossibleGameEngine(int[][] levelData, float mapSizeX, float mapSizeY) {
        System.out.println("ImpossibleGameEngine constructor");
        this.levelData = levelData;
        this.playerShip = Global.getCombatEngine().getPlayerShip( );
        this.mapSizeX = mapSizeX;
        this.mapSizeY = mapSizeY;
    }


    @Override
    public void advance(float amount) {
        this.currentSecondStack += amount;

        if (this.currentSecondStack >= spawnInterval) {
            this.currentSecondStack = 0;
            spawnColumn(this.levelData[this.currentLevelStage], this.mapSizeX, this.mapSizeY);
            this.currentLevelStage += 1;

            // prob enough if we do it only when we spawn shit
            despawnObsoleteShips(this.mapSizeX, this.mapSizeY);
        }

    }

    public void jump() {

    }

    public static void spawnColumn(int[] column, float mapSizeX, float mapSizeY) {
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        CombatFleetManagerAPI enemyFleetManagerAPI = combatEngineAPI.getFleetManager(FleetSide.ENEMY);

        int columnSize = column.length;

        for (int i = 0; i < columnSize; i++) {
            String entityID = objectLookUpTable.get(column[i]);
            if (entityID != null) {
                System.out.println("Spawning Entity: " + entityID);
                Vector2f spawnPosition = calculateSpawnPosition(i, mapSizeX, mapSizeY);
                ShipAPI entity = enemyFleetManagerAPI.spawnShipOrWing(entityID, spawnPosition, 90f);
                entity.getVelocity().set(- objectVelocity, 0);
                entity.makeLookDisabled();
            }
        }
    }

    public static Vector2f calculateSpawnPosition(int i, float mapSizeX, float mapSizeY) {
        float Y = mapSizeY / 2f - topPadding - i * tileSize;
        float X = mapSizeX / 2f - rightPadding;
        return new Vector2f(X, Y);
    }

    public static void despawnObsoleteShips(float mapSizeX, float mapSizeY) {
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        CombatFleetManagerAPI fleetManagerAPI = Global.getCombatEngine().getFleetManager(1);

        for (ShipAPI ship : combatEngineAPI.getShips()) {
            FleetMemberAPI fleetMemberAPI = ship.getFleetMember();
            if (ship.getOwner() == 1 && !fleetMemberAPI.isFlagship() && getIsShipOutOfBounds(ship, mapSizeX, mapSizeY )
            ) {
                Logger logger  =Global.getLogger(ImpossibleGameEngine.class);
                logger.setLevel(Level.INFO);
                logger.info("triggering shit");
                combatEngineAPI.removeEntity(ship);
            }
        }

    }

    public static boolean getIsShipOutOfBounds(ShipAPI ship, float mapSizeX, float mapSizeY) {
        Vector2f shipLocation  = ship.getLocation();
        return shipLocation.x < -mapSizeX / 2 || shipLocation.x > mapSizeX / 2 || shipLocation.y < -mapSizeY / 2 || shipLocation.y > mapSizeY / 2;
    }
}
