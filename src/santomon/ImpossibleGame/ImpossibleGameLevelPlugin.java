package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;
import lunalib.lunaSettings.LunaSettings;

import java.util.*;
import java.util.List;

public class ImpossibleGameLevelPlugin extends BaseEveryFrameCombatPlugin {

    public final int[][] levelData;
    public final int[] gravityData;
    public final HashMap<Integer, Color> colorData;
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
    private MoveVerticalRelative moveRelative;
    public Boolean hasCalledFakeInit = false;


    public ImpossibleGameLevelPlugin(String levelName) {

        System.out.println("ImpossibleGameLevelPlugin constructor");
        this.levelData = loadLevelData(levelName);
        this.gravityData = loadGravityData(levelName);
        this.colorData = loadColorData(levelName);
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

        maybeStopMusic();


        // freeze enemy flagship
        ShipAPI enemyFlagship = getEnemyFlagship();
        if (enemyFlagship != null) {
            enemyFlagship.setPhased(true);
            enemyFlagship.getVelocity().set(new Vector2f(0, 0));
            enemyFlagship.getLocation().set(new Vector2f(-10, 0));
        }
     }

    private void maybeStopMusic() {
        if (Global.getCombatEngine().getPlayerShip() == null) {
            Global.getSoundPlayer().pauseCustomMusic();
        }
        if (!Global.getCombatEngine().getPlayerShip().isAlive()) {
            Global.getSoundPlayer().pauseCustomMusic();
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
//        engine.setBackgroundColor(new Color(255, 0, 0));
    }

    public void fakeInit(CombatEngineAPI engine) {
        this.impossibleGameLevelEngine = new ImpossibleGameLevelEngine(this.levelData, this.gravityData, this.colorData, objectLookUpTable);
        Global.getSoundPlayer().playCustomMusic(1, 1, "chaoz_fantasy");
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


        ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();
        if (playerShip != null) {
            playerShip.setControlsLocked(true);
            playerShip.setPhased(true);
            playerShip.getVelocity().set(new Vector2f(0, 0));
            playerShip.getLocation().set(new Vector2f(0, 0));
        }

    }

   public void createJumper() {
        CombatEngineAPI engine = Global.getCombatEngine();
       this.jumper = engine.getFleetManager(0).spawnShipOrWing(jumperVariantID, new Vector2f(0,0), 0);  // facing of 0 === looking to the right
       this.jumper.getVelocity().set(0, 0);
       this.jumper.setShipAI(new DontMoveAI());
       this.jumper.addListener(new DamageWhenOutOfBounds(this.jumper));

       this.jumpScript = new JumpScript(this.jumper, groundShipIDs, this.jumpSettings, this.keyBindings.jumpKeys);
       this.impossibleGameLevelEngine.getJumpScripts().add(this.jumpScript);
       engine.addPlugin(this.jumpScript);
       if (this.impossibleGameLevelEngine != null) {
           this.impossibleGameLevelEngine.positionJumper(this.jumper);
       }

       ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();
       this.moveRelative = new MoveVerticalRelative(playerShip, this.jumper);
       playerShip.addListener(moveRelative);

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
            getLogger().error("Failed to load gravity data. make sure, it is exactly 1 integer per line and nothing else.", e);
            return new int[0];
        }
    }

    public static HashMap<Integer, Color> loadColorData(String levelName){
        try {
            HashMap<Integer, Color> data = new HashMap<>();
            String colorDataRaw = Global.getSettings().loadText("data/missions/"+levelName+"/color_data.txt");
            String[] Q = colorDataRaw.split("\n");
            for (int i = 0; i < Q.length; i++) {
                int[] tmp = new int[4];
                String[] splits = Q[i].split(",");
                for (int j = 0; j < tmp.length; j++) {
                    tmp[j] = Integer.parseInt(splits[j]);
                }
                Color color = new Color(tmp[1], tmp[2], tmp[3]);
                data.put(tmp[0], color);
            }
            return data;
        } catch (Exception e) {
            getLogger().error("Failed to load color data.", e);
            return new HashMap<>();
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

