package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class ImpossibleGameLevelEngine extends BaseEveryFrameCombatPlugin {
    // a lot of the logic is put here to enable pausing maybe?

    public float currentSecondStack = 0f;  // 1f === 1 sec; advanced amount is in the ~0.017 range usually
    public int currentLevelStage = 0;
    private boolean[] previouslyCreated;

    public final int[][] levelData;
    public final float mapSizeX;
    public final float mapSizeY;
    public final HashMap<Integer, String> objectLookUpTable;
    private final HashMap<String, List<ShipAPI>> availableEntitiesForSpawning;



    public static final float objectVelocityI = 600f;
    public static final float objectVelocity = 1000f;
    public static final float timeMultiplier = 2f;
    public static final float tileSize = 120f;  // kite has  a collision radius of 64
    public static final Vector2f targetVelocity = new Vector2f(-objectVelocity, 0);
    public static final float spawnInterval = tileSize / 2 / objectVelocityI / timeMultiplier;
    public static final float topPadding = 100f;
    public static final float rightPadding = 100f;



    public ImpossibleGameLevelEngine(int[][] levelData, float mapSizeX, float mapSizeY, final HashMap<Integer, String> objectLookUpTable) {

        // spawn the jumper; 3 tiles away from the middle
        this.levelData = levelData;
        this.mapSizeX = mapSizeX;
        this.mapSizeY = mapSizeY;
        this.objectLookUpTable = objectLookUpTable;

        this.availableEntitiesForSpawning = new HashMap<String, List<ShipAPI>>() {{
            for (String entityID : objectLookUpTable.values()) {
                if (entityID != null) {
                    put(entityID, new ArrayList<ShipAPI>() );
                }
            }
        }};

        this.spawnInitialRow();
        previouslyCreated = new boolean[levelData[0].length];
        previouslyCreated[previouslyCreated.length - 1] = true;
    }


    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        if (combatEngineAPI.isPaused()) return;


        this.currentSecondStack += amount;
        if (this.currentSecondStack >= spawnInterval) {
            this.currentSecondStack = 0;
            if (this.currentLevelStage >= this.levelData.length) return;  // we are finished with the level
            this.previouslyCreated = spawnColumn(this.levelData[this.currentLevelStage], this.previouslyCreated);
            this.currentLevelStage += 1;

            // prob enough if we do it only when we spawn shit
            markObsoleteShipsForTeleportation(this.mapSizeX, this.mapSizeY, this.availableEntitiesForSpawning);
        }
    }


    public void positionJumper(ShipAPI jumper) {
        Vector2f newPosition = new Vector2f(- tileSize * 3,0);
        jumper.getLocation().set(newPosition);
    }


    private void spawnInitialRow() {
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        CombatFleetManagerAPI enemyFleetManagerAPI = combatEngineAPI.getFleetManager(FleetSide.ENEMY);

        if (this.levelData == null) return;

        int i = this.levelData[0].length - 1;

        Vector2f lowestPointSpawnPosition = calculateSpawnPosition(i, this.mapSizeX, this.mapSizeY);
        Vector2f currentSpawnPosition = new Vector2f().set(lowestPointSpawnPosition);

        while (currentSpawnPosition.x >= - mapSizeX / 2) {
            ShipAPI groundShip = enemyFleetManagerAPI.spawnShipOrWing(objectLookUpTable.get(1), currentSpawnPosition, 90f);
            groundShip.getVelocity().set(targetVelocity);
            groundShip.getMutableStats().getTimeMult().modifyMult("impossible_timemult", timeMultiplier);
            groundShip.makeLookDisabled();
            currentSpawnPosition.setX(currentSpawnPosition.x - tileSize);
        }

    }

    public boolean[] spawnColumn(int[] column, boolean[] previouslyCreated) {
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        CombatFleetManagerAPI enemyFleetManagerAPI = combatEngineAPI.getFleetManager(FleetSide.ENEMY);

        boolean[] spawnedShit = new boolean[column.length];

        int columnSize = column.length;

        for (int i = 0; i < columnSize; i++) {
            int key = column[i];
            String entityID = objectLookUpTable.get(key);
            if (entityID != null) {

                if (previouslyCreated[i]) continue;

                Vector2f spawnPosition = calculateSpawnPosition(i, mapSizeX, mapSizeY);
                List<ShipAPI> availableShips = availableEntitiesForSpawning.get(entityID);

                ShipAPI entity;
                if (!availableShips.isEmpty()) {
                    entity = availableShips.remove(availableShips.size() - 1);
                    entity.getLocation().set(spawnPosition);

                } else {
                    getLogger().info("Spawning Entity: " + entityID);
                    entity = enemyFleetManagerAPI.spawnShipOrWing(entityID, spawnPosition, 90f);
                    entity.getVelocity().set(targetVelocity);
                    entity.getMutableStats().getTimeMult().modifyMult("impossible_timemult", timeMultiplier);
                }
                spawnedShit[i] = true;
                entity.makeLookDisabled();

            }
        }
        return spawnedShit;
    }

    private void overrideVelocities() {
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        for (ShipAPI ship : combatEngineAPI.getShips()) {
            for (String entityID : objectLookUpTable.values()) {
                if (Objects.equals(ship.getVariant().getHullVariantId(), entityID)) {
                    ship.getVelocity().set(- objectVelocity, 0);
                }
            }
        }
    }

    public static Vector2f calculateSpawnPosition(int i, float mapSizeX, float mapSizeY) {
        float Y = mapSizeY / 2f - topPadding - i * tileSize;
        float X = mapSizeX / 2f - rightPadding;
        return new Vector2f(X, Y);
    }

    public static void despawnObsoleteShips(float mapSizeX, float mapSizeY) {
        // not sure if this is even necessary, as the engine seems to despawn out of  bounds ships by itself or sth
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        CombatFleetManagerAPI fleetManagerAPI = Global.getCombatEngine().getFleetManager(1);

        for (ShipAPI ship : combatEngineAPI.getShips()) {
            FleetMemberAPI fleetMemberAPI = ship.getFleetMember();
            if (ship.getOwner() == 1 && !fleetMemberAPI.isFlagship() && getIsShipOutOfBounds(ship, mapSizeX, mapSizeY )
            ) {
                combatEngineAPI.removeEntity(ship);
            }
        }

    }

    public static void markObsoleteShipsForTeleportation(float mapSizeX, float mapSizeY, HashMap<String, List<ShipAPI>> availableShips) {
        // not sure if this is even necessary, as the engine seems to despawn out of  bounds ships by itself or sth
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        CombatFleetManagerAPI fleetManagerAPI = Global.getCombatEngine().getFleetManager(1);

        for (ShipAPI ship : combatEngineAPI.getShips()) {
            FleetMemberAPI fleetMemberAPI = ship.getFleetMember();
            if (ship.getOwner() == 1 && !fleetMemberAPI.isFlagship() && getIsShipOutOfBounds(ship, mapSizeX, mapSizeY )
            ) {
                String shipVariantID = ship.getVariant().getHullVariantId();
                if (!availableShips.containsKey(shipVariantID)) continue;
                if (availableShips.get(shipVariantID).contains(ship)) continue;
                availableShips.get(shipVariantID).add(ship);
            }
        }

    }


    public static void selfDamage() {

        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        ShipAPI playerShip = combatEngineAPI.getPlayerShip();

        combatEngineAPI.applyDamage(playerShip, playerShip.getLocation(), 10, DamageType.HIGH_EXPLOSIVE, 0, false, false, null, false);
    }

    public static boolean getIsShipOutOfBounds(ShipAPI ship, float mapSizeX, float mapSizeY) {
        Vector2f shipLocation  = ship.getLocation();
        return shipLocation.x < -mapSizeX / 2 || shipLocation.x > mapSizeX / 2 || shipLocation.y < -mapSizeY / 2 || shipLocation.y > mapSizeY / 2;
    }


    public static Logger getLogger() {
        Logger logger = Global.getLogger(ImpossibleGameLevelEngine.class);
        logger.setLevel(Level.INFO);
        return logger;
    }
}
