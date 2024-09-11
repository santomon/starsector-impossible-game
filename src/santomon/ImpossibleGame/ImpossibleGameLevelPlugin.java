package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;
import lunalib.lunaSettings.LunaSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ImpossibleGameLevelPlugin extends BaseEveryFrameCombatPlugin {

    public final int[][] levelData;
    public final int[] gravityData;
    public final JumpSettings jumpSettings;
    public final KeyBindings keyBindings;
    public final String jumperVariantID;

    public ImpossibleGameLevelEngine impossibleGameLevelEngine;
    public ShipAPI jumper;
    public JumpScript jumpScript;
    public KillPlayerWhenAnyPlayerDamageIsTaken killPlayerWhenAnyPlayerDamageIsTakenScript;


    public static final HashMap<Integer, String> objectLookUpTable = new HashMap<Integer, String>() {{
        put(0, null);
        put(1, IGMisc.LunaLibKeys.IG_DEFENDER_VARIANT_ID);  // block
        put(2, IGMisc.LunaLibKeys.IG_KITE_VARIANT_ID);  // spikes
        put(9, null);  // some other null markers
    }
    };
    public static final List<String> groundShipIDs = new ArrayList<String>() {{
        add(objectLookUpTable.get(1));
    }};


    public ImpossibleGameLevelPlugin(String levelName) {

        System.out.println("ImpossibleGameLevelPlugin constructor");
        this.levelData = loadLevelData(levelName);
        this.gravityData = loadGravityData(levelName);
        this.jumpSettings = new JumpSettings(
                LunaSettings.getFloat(IGMisc.LunaLibKeys.IG_MOD_ID, IGMisc.LunaLibKeys.GRAVITY_FORCE_ID),
                LunaSettings.getFloat(IGMisc.LunaLibKeys.IG_MOD_ID, IGMisc.LunaLibKeys.JUMP_FORCE_ID),
                LunaSettings.getFloat(IGMisc.LunaLibKeys.IG_MOD_ID, IGMisc.LunaLibKeys.MAX_JUMP_VELOCITY_ID),
                LunaSettings.getFloat(IGMisc.LunaLibKeys.IG_MOD_ID, IGMisc.LunaLibKeys.GROUND_TOLERANCE_ID)
        );

        this.keyBindings = new KeyBindings(
                safelyRetrieveKeycodesFromLunalib(new ArrayList<String>() {{
                    add(IGMisc.LunaLibKeys.JUMP_KEY_ID);
                    add(IGMisc.LunaLibKeys.ALTERNATIVE_JUMP_KEY_ID);
                }}),
                safelyRetrieveKeycodesFromLunalib(
                        new ArrayList<String>() {{
                            add(IGMisc.LunaLibKeys.QUICK_RESTART_KEY_ID);
                        }}
                )
        );

        String maybeJumperVariantID = LunaSettings.getString(IGMisc.LunaLibKeys.IG_MOD_ID, IGMisc.LunaLibKeys.JUMPER_VARIANT_ID);
        if (maybeJumperVariantID == null || !Global.getSettings().doesVariantExist(maybeJumperVariantID)) {
            this.jumperVariantID = IGMisc.LunaLibKeys.IG_HERMES_VARIANT_ID;
        } else {
            this.jumperVariantID = maybeJumperVariantID;
        }

    }


    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!hasCalledFakeInit){
            fakeInit(Global.getCombatEngine());
            hasCalledFakeInit = true;
        }


        // maybe quickrestart
        if (this.quickRestartPressed(events)) {
            this.cleanUp();
        }

        // freeze playerShip
        ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();
        playerShip.setControlsLocked(true);
        playerShip.setPhased(true);
        playerShip.getVelocity().set(new Vector2f(0, 0));
        playerShip.getLocation().set(new Vector2f(0, 0));

        // freeze enemy flagship
        ShipAPI enemyFlagship = getEnemyFlagship();
        if (enemyFlagship != null) {
            enemyFlagship.setPhased(true);
            enemyFlagship.getVelocity().set(new Vector2f(0, 0));
            enemyFlagship.getLocation().set(new Vector2f(-10, 0));

        }
     }

    @Override
    public void init(CombatEngineAPI engine) {
//        System.out.println("ImpossibleGameLevelPlugin init");
    }

    public Boolean hasCalledFakeInit = false;
    public void fakeInit(CombatEngineAPI engine) {
        System.out.println("ImpossibleGameLevelPlugin fakeInit");
        this.impossibleGameLevelEngine = new ImpossibleGameLevelEngine(this.levelData, this.gravityData, objectLookUpTable);
        engine.addPlugin(this.impossibleGameLevelEngine);

        this.killPlayerWhenAnyPlayerDamageIsTakenScript = new KillPlayerWhenAnyPlayerDamageIsTaken();
        engine.getListenerManager().addListener(killPlayerWhenAnyPlayerDamageIsTakenScript);

        this.createJumper();

        // isolating enemy flagship
        ShipAPI enemyFlagship = getEnemyFlagship();
        if (enemyFlagship != null) {
            enemyFlagship.getLocation().set(new Vector2f(-10,  - 10));
            enemyFlagship.setShipAI(new DontMoveAI());
            enemyFlagship.getVelocity().set(0, 0);
            enemyFlagship.setPhased(true);
        }
    }

   public void createJumper() {
        CombatEngineAPI engine = Global.getCombatEngine();
       this.jumper = engine.getFleetManager(0).spawnShipOrWing(jumperVariantID, new Vector2f(0,0), 0);  // facing of 0 === looking to the right
//       this.jumper.makeLookDisabled();
       this.jumper.getVelocity().set(0, 0);
       this.jumper.setShipAI(new DontMoveAI());
       this.jumper.addListener(new DamageWhenOutOfBounds(this.jumper));

       this.jumpScript = new JumpScript(this.jumper, groundShipIDs, this.jumpSettings, this.keyBindings.jumpKeys);
       this.impossibleGameLevelEngine.getJumpScripts().add(this.jumpScript);
       engine.addPlugin(this.jumpScript);
       if (this.impossibleGameLevelEngine != null) {
           this.impossibleGameLevelEngine.positionJumper(this.jumper);
       }
   }



    public static int[][] loadLevelData(String levelName){
        try{
            String levelDataRaw = Global.getSettings().loadText("data/missions/"+levelName+"/level_data.txt");
            String[] Q = levelDataRaw.split("\n");
            String firstRow = Q[0];
            int[][] data = new int[Q.length][firstRow.length()];

            for (int i = 0; i < Q.length; i++) {
                String characters = Q[i];
                for (int j = 0; j < characters.length(); j++) {
                    data[i][j] = Integer.parseInt(String.valueOf(characters.charAt(j)));
                }
            }
            int[][] result = IGMisc.transposeMatrix(data);
            return result;
        } catch (Exception e){
            throw new RuntimeException("Failed to load level data", e);
        }
    }

    public static int[] loadGravityData(String levelName){
        try {
            String gravityDataRaw = Global.getSettings().loadText("data/missions/"+levelName+"/gravity_data.txt");
            String[] Q = gravityDataRaw.split("\n");
            int[] data = new int[Q.length];
            for (int i = 0; i < Q.length; i++) {
                data[i] = Integer.parseInt(Q[i]);
            }
            return data;
        } catch (Exception e ) {
            throw new RuntimeException("Failed to load gravity data. make sure, it is exactly 1 integer per line and nothing else.", e);
        }
    }

    public static void showLevelData(int[][] levelData){

        for (int[] row : levelData) {
            for (int i : row) {
                System.out.print(i + " ");
            }
            System.out.println();
        }
    }

    public static List<Integer> safelyRetrieveKeycodesFromLunalib(List<String> ids) {
        List<Integer> keycodes = new ArrayList<>();
        for (String id : ids) {
            Integer value = LunaSettings.getInt(IGMisc.LunaLibKeys.IG_MOD_ID, id);
            if (value == null || value == 0) continue;
            keycodes.add(value);
        }
        return keycodes;
    }
    public static Logger getLogger() {
        Logger logger = Global.getLogger(ImpossibleGameLevelPlugin.class);
        logger.setLevel(Level.INFO);
        return logger;
    }

    private boolean quickRestartPressed(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            for (int key : this.keyBindings.quickRestartKeys) {
                if (key == event.getEventValue()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void cleanUp() {
        CombatEngineAPI engine = Global.getCombatEngine();

        engine.removeEntity(this.jumper);
        engine.removePlugin(this.impossibleGameLevelEngine);
        engine.removePlugin(this.jumpScript);
        engine.getListenerManager().removeListener(this.killPlayerWhenAnyPlayerDamageIsTakenScript);

        for (ShipAPI ship : engine.getShips()) {
            if (Objects.equals(ship.getId(), engine.getPlayerShip().getId())) continue;
            engine.removeEntity(ship);
        }
        this.hasCalledFakeInit = false;
    }

    public static ShipAPI getEnemyFlagship() {
        CombatEngineAPI engine = Global.getCombatEngine();
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() == 1 && ship.getFleetMember().isFlagship()) {
                return ship;
            }
        }
        return null;
    }
}

