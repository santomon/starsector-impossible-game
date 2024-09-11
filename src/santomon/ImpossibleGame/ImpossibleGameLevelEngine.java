package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;


public class ImpossibleGameLevelEngine extends BaseEveryFrameCombatPlugin {
    // a lot of the logic is put here to enable pausing maybe?

    public float currentSecondStack = 0f;  // 1f === 1 sec; advanced amount is in the ~0.017 range usually
    public int currentLevelStage = 000;

    public final int[][] levelData;
    public final int[] gravityData;
    public final HashMap<Integer, Color> colorData;
    public final HashMap<Integer, String> objectLookUpTable;

    private boolean[] previouslyCreated;
    private final HashMap<String, List<ShipAPI>> availableEntitiesForSpawning;
    private final List<JumpScript> jumpScripts = new ArrayList<>();
    private boolean gravityIsReversed = false;
    private ShipAPI spawnMarker;


    // i think the target speed is around 15 tiles per second.
    // with object velocity of 600f and tilesize of 120f that puts us at timemult of 3;
    public static final float timeMultiplier = 2.5f;

    public static final float objectVelocity = 600f;
    public static final float tileSize = 120f;  // kite has  a collision radius of 64
    public static final Vector2f targetVelocity = new Vector2f(-objectVelocity, 0);
    public static final float spawnInterval = tileSize / 2f / objectVelocity / timeMultiplier;
    public static final float topPadding = 100f;
    public static final float rightPadding = 100f;



    public ImpossibleGameLevelEngine(int[][] levelData, int[] gravityData, HashMap<Integer, Color> colorData, final HashMap<Integer, String> objectLookUpTable) {

        // spawn the jumper; 3 tiles away from the middle
        this.levelData = levelData;
        this.objectLookUpTable = objectLookUpTable;
        this.gravityData = gravityData;
        this.colorData = colorData;

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

    public List<JumpScript> getJumpScripts() {
        return this.jumpScripts;
    }



    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        if (combatEngineAPI.isPaused()) return;


        this.currentSecondStack += amount;
        if (this.currentSecondStack >= spawnInterval) {
            this.currentSecondStack = currentSecondStack - spawnInterval;
            if (this.currentLevelStage >= this.levelData.length) return;  // we are finished with the level
            this.previouslyCreated = spawnColumn(this.levelData[this.currentLevelStage], this.previouslyCreated);
            maybeFlipGravity();
            maybeChangeBackgroundColor();
            this.currentLevelStage += 1;

            // prob enough if we do it only when we spawn shit
            markObsoleteShipsForTeleportation();
        }
    }

    private void maybeChangeBackgroundColor() {
        if (this.colorData.containsKey(this.currentLevelStage))
            Global.getCombatEngine().setBackgroundColor(colorData.get(this.currentLevelStage));

    }

    private void maybeFlipGravity() {
        for (int g : gravityData) {
            if (g > this.currentLevelStage) return;
            if (g == this.currentLevelStage) {
                this.gravityIsReversed = !this.gravityIsReversed;
                for (JumpScript jumpScript : jumpScripts) {
                    jumpScript.setGravityIsReversed(!jumpScript.getGravityIsReversed());
                }
            }
        }

    }


    public void positionJumper(ShipAPI jumper) {
        Vector2f newPosition = new Vector2f(- tileSize * 3,3 * tileSize);
        jumper.getLocation().set(newPosition);
    }


    private void spawnInitialRow() {
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        CombatFleetManagerAPI enemyFleetManagerAPI = combatEngineAPI.getFleetManager(FleetSide.ENEMY);

        if (this.levelData == null) return;

        int i = this.levelData[0].length - 1;

        Vector2f lowestPointSpawnPosition = calculateSpawnPosition(i);
        Vector2f currentSpawnPosition = new Vector2f().set(lowestPointSpawnPosition);

        while (currentSpawnPosition.x >= - combatEngineAPI.getMapWidth() / 2) {
            ShipAPI groundShip = spawnEntity(1, currentSpawnPosition);
            currentSpawnPosition.setX(currentSpawnPosition.x - tileSize);
        }

    }

    public boolean[] spawnColumn(int[] column, boolean[] previouslyCreated) {
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();

        boolean[] spawnedShit = new boolean[column.length];

        int columnSize = column.length;

        for (int i = 0; i < columnSize; i++) {
            int key = column[i];
            String entityID = objectLookUpTable.get(key);
            if (entityID != null) {

                if (previouslyCreated[i]) continue;

                Vector2f spawnPosition = calculateSpawnPosition(i);
                List<ShipAPI> availableShips = availableEntitiesForSpawning.get(entityID);

                ShipAPI entity;
                if (!availableShips.isEmpty()) {
                    entity = availableShips.remove(availableShips.size() - 1);
                    teleportEntityFromSafety(entity, spawnPosition);
                } else {
                    entity = spawnEntity(key, spawnPosition);
                }
                spawnedShit[i] = true;
            }
        }
        return spawnedShit;
    }


    public Vector2f calculateSpawnPosition(int i) {
        float Y = Global.getCombatEngine().getMapHeight() / 2f - topPadding - i * tileSize;
        float X;
        X = Global.getCombatEngine().getMapWidth() / 2f - rightPadding;
        return new Vector2f(X, Y);
    }

    public static void despawnObsoleteShips() {
        // not sure if this is even necessary, as the engine seems to despawn out of  bounds ships by itself or sth
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        CombatFleetManagerAPI fleetManagerAPI = Global.getCombatEngine().getFleetManager(1);

        for (ShipAPI ship : combatEngineAPI.getShips()) {
            FleetMemberAPI fleetMemberAPI = ship.getFleetMember();
            if (ship.getOwner() == 1 && !fleetMemberAPI.isFlagship() && getIsShipOutOfBounds(ship)
            ) {
                combatEngineAPI.removeEntity(ship);
            }
        }

    }

    public void markObsoleteShipsForTeleportation() {
        // not sure if this is even necessary, as the engine seems to despawn out of  bounds ships by itself or sth
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        CombatFleetManagerAPI fleetManagerAPI = Global.getCombatEngine().getFleetManager(1);

        for (ShipAPI ship : combatEngineAPI.getShips()) {
            FleetMemberAPI fleetMemberAPI = ship.getFleetMember();
            if (ship.getOwner() == 1 && !fleetMemberAPI.isFlagship() && getIsShipOutOfBounds(ship)
            ) {
                String shipVariantID = ship.getVariant().getHullVariantId();
                if (!this.availableEntitiesForSpawning.containsKey(shipVariantID)) continue;
                if (this.availableEntitiesForSpawning.get(shipVariantID).contains(ship)) continue;
                this.availableEntitiesForSpawning.get(shipVariantID).add(ship);
                teleportEntityToSafety(ship);
            }
        }
    }

    public void teleportEntityToSafety(ShipAPI entity) {
        entity.setPhased(true);
        entity.getLocation().set(100, 100);
        entity.getVelocity().set(0, 0);

    }

    public void teleportEntityFromSafety(ShipAPI entity, Vector2f targetLocation) {
        entity.getLocation().set(targetLocation);
        entity.getVelocity().set(targetVelocity);

        int signum = !this.gravityIsReversed ? 1 : -1;
        entity.setFacing(signum * 90f);

        entity.setPhased(false);
    }

    public ShipAPI spawnEntity(int key, Vector2f spawnPosition) {
        String entityID = this.objectLookUpTable.get(key);
        getLogger().info("Spawning Entity: " + entityID);
        CombatFleetManagerAPI enemyFleetManagerAPI = Global.getCombatEngine().getFleetManager(1);
        int signum = !this.gravityIsReversed ? 1 : -1;
        ShipAPI entity = enemyFleetManagerAPI.spawnShipOrWing(entityID, spawnPosition, signum * 90f);
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        entity.getVelocity().set(targetVelocity);
        entity.getMutableStats().getTimeMult().modifyMult("impossible_timemult", timeMultiplier);
        entity.makeLookDisabled();
        return entity;

    }


    public static boolean getIsShipOutOfBounds(ShipAPI ship) {
        Vector2f shipLocation  = ship.getLocation();
        float mapSizeX = Global.getCombatEngine().getMapWidth();
        float mapSizeY = Global.getCombatEngine().getMapHeight();
        return shipLocation.x < -mapSizeX / 2 || shipLocation.x > mapSizeX / 2 || shipLocation.y < -mapSizeY / 2 || shipLocation.y > mapSizeY / 2;
    }


    public static Logger getLogger() {
        Logger logger = Global.getLogger(ImpossibleGameLevelEngine.class);
        logger.setLevel(Level.INFO);
        return logger;
    }
}
