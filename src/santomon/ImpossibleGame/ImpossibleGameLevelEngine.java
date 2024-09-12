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
    public static final String IMPOSSIBLE_VICTORY_FLAG = "impossible_victory_flag";
    // a lot of the logic is put here to enable pausing maybe?

    public float currentSecondStack = 0f;  // 1f === 1 sec; advanced amount is in the ~0.017 range usually
    public int currentLevelStage = 0;


    public final int[][] levelData;
    public final int[] gravityData;
    public final HashMap<Integer, Color> colorData;
    public final HashMap<Integer, String> objectLookUpTable;

    private boolean[] previouslyCreated;
    private final HashMap<String, List<ShipAPI>> availableEntitiesForSpawning;
    private final List<JumpScript> jumpScripts = new ArrayList<>();
    private boolean gravityIsReversed = false;
    private ShipAPI spawnMarker;
    private ShipAPI victoryMarker;
    private final Vector2f safeSpot;


    // i think the target speed is around 15 tiles per second.
    // with object velocity of 600f and tilesize of 120f that puts us at timemult of 3;
    public static final float timeMultiplier = 2.5f;

    public static final float objectVelocity = 600f;
    public static final float tileSize = 120f;  // kite has  a collision radius of 64  // prob should make this inferable, based on collision radius of ground ship
    public static final Vector2f jumperPosition = new Vector2f(-tileSize * 3, 3 * tileSize);
    public static final Vector2f targetVelocity = new Vector2f(-objectVelocity, 0);
    public static final float spawnPrecision = 2f;
    public static final float spawnInterval = tileSize / spawnPrecision / objectVelocity / timeMultiplier;
    public static final float topPadding = 100f;
    public static final float rightPadding = 100f;
    private boolean hasALreadyInitiatedVictoryRoutine = false;


    public ImpossibleGameLevelEngine(int[][] levelData, int[] gravityData, HashMap<Integer, Color> colorData, final HashMap<Integer, String> objectLookUpTable) {

        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        // spawn the jumper; 3 tiles away from the middle
        this.safeSpot = new Vector2f(combatEngineAPI.getMapWidth() / 2 - 500, combatEngineAPI.getMapHeight() / 2 - 500);
//        this.safeSpot = new Vector2f(0, 0);
        this.levelData = levelData;
        this.objectLookUpTable = objectLookUpTable;
        this.gravityData = gravityData;
        this.colorData = colorData;

        this.availableEntitiesForSpawning = new HashMap<String, List<ShipAPI>>() {{
            for (String entityID : objectLookUpTable.values()) {
                if (entityID != null) {
                    put(entityID, new ArrayList<ShipAPI>());
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
            teleportObsoleteShipsToSafety();
            if (checkVictory()) {
                maybeInitiateVictoryRoutine();
            }
        }
    }

    private void maybeInitiateVictoryRoutine() {
        if (hasALreadyInitiatedVictoryRoutine) return;
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        combatEngineAPI.addPlugin(new VictoryCelebration(3f, 5f));
        hasALreadyInitiatedVictoryRoutine = true;
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
        jumper.getLocation().set(jumperPosition);
    }


    private void spawnInitialRow() {
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        CombatFleetManagerAPI enemyFleetManagerAPI = combatEngineAPI.getFleetManager(FleetSide.ENEMY);

        if (this.levelData == null) return;

        int i = this.levelData[0].length - 1;

        Vector2f lowestPointSpawnPosition = calculateSpawnPosition(i);
        Vector2f currentSpawnPosition = new Vector2f().set(lowestPointSpawnPosition);

        while (currentSpawnPosition.x >= -combatEngineAPI.getMapWidth() / 2) {
            ShipAPI groundShip = spawnEntity(1, currentSpawnPosition);
            currentSpawnPosition.setX(currentSpawnPosition.x - tileSize);
        }

    }

    public boolean[] spawnColumn(int[] column, boolean[] previouslyCreated) {
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();

        boolean[] spawnedShit = new boolean[column.length];

        int columnSize = column.length;
        spawnOrRelocateSpawnMarker();

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

    private void spawnOrRelocateSpawnMarker() {
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        Vector2f spawnPosition = calculateSpawnPosition(10);
        if (this.spawnMarker == null) {
            this.spawnMarker = combatEngineAPI.getFleetManager(1).spawnShipOrWing("defender_PD", spawnPosition, 90f);
            this.spawnMarker.setPhased(true);
            this.spawnMarker.makeLookDisabled();
            this.spawnMarker.getVelocity().set(targetVelocity);
            this.spawnMarker.getMutableStats().getTimeMult().modifyMult("impossible_timemult", timeMultiplier);
        } else {
            this.spawnMarker.getLocation().setX(this.spawnMarker.getLocation().x + tileSize / spawnPrecision);
        }

    }

    private boolean checkVictory() {
        if (this.victoryMarker == null) return false;
        return victoryMarker.getLocation().x < jumperPosition.x;
    }


    public Vector2f calculateSpawnPosition(int i) {
        float Y = Global.getCombatEngine().getMapHeight() / 2f - topPadding - i * tileSize;
        float X;
        if (this.spawnMarker == null) X = Global.getCombatEngine().getMapWidth() / 2f - rightPadding;
        else X = this.spawnMarker.getLocation().x;
        return new Vector2f(X, Y);
    }


    public void teleportObsoleteShipsToSafety() {
        // not sure if this is even necessary, as the engine seems to despawn out of  bounds ships by itself or sth
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();

        for (ShipAPI ship : combatEngineAPI.getShips()) {
            FleetMemberAPI fleetMemberAPI = ship.getFleetMember();
            if (shipIsRegistered(ship) && !fleetMemberAPI.isFlagship() && getIsShipOutOfBounds(ship)) {
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
        entity.getLocation().set(this.safeSpot);
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
        int owner = key == 1 ? 0 : 1;
        CombatFleetManagerAPI enemyFleetManagerAPI = Global.getCombatEngine().getFleetManager(owner);
        int signum = !this.gravityIsReversed ? 1 : -1;
        ShipAPI entity = enemyFleetManagerAPI.spawnShipOrWing(entityID, spawnPosition, signum * 90f);
        entity.setShipAI(new DontMoveAI());
        if (key == 3) {
            entity.addTag(IMPOSSIBLE_VICTORY_FLAG);
            this.victoryMarker = entity;
            this.victoryMarker.setPhased(true);
        }
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        entity.getVelocity().set(targetVelocity);
        entity.getMutableStats().getTimeMult().modifyMult("impossible_timemult", timeMultiplier);
        entity.makeLookDisabled();
        return entity;

    }

    private boolean shipIsRegistered(ShipAPI ship) {
        for (String entityID : this.objectLookUpTable.values()) {
            if (Objects.equals(entityID, ship.getVariant().getHullVariantId())) return true;
        }
        return false;
    }


    public static boolean getIsShipOutOfBounds(ShipAPI ship) {
        Vector2f shipLocation = ship.getLocation();
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
