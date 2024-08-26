package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventType;
import com.fs.starfarer.coreui.refit.auto.SavedVariantData;
import data.missions.xddmission.IGMisc;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;
import lunalib.lunaSettings.LunaSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ImpossibleGameLevelPlugin extends BaseEveryFrameCombatPlugin {

    public final int[][] levelData;
    public final float mapSizeX;
    public final float mapSizeY;
    public final JumpSettings jumpSettings;
    public final KeyBindings keyBindings;
    public final String jumperVariantID;

    public ImpossibleGameLevelEngine impossibleGameLevelEngine;
    public static final Character jumpKey = ' ';  // ok this works
    public static final Character alternativeJumpKey = 'M';
    private ShipAPI jumper;
    private JumpScript jumpScript;


    public static final HashMap<Integer, String> objectLookUpTable = new HashMap<Integer, String>() {{
        put(0, null);
        put(1, IGMisc.LunaLibKeys.IG_DEFENDER_VARIANT_ID);  // block
        put(2, IGMisc.LunaLibKeys.IG_KITE_VARIANT_ID);  // spikes
    }
    };
    public static final List<String> groundShipIDs = new ArrayList<String>() {{
        add(objectLookUpTable.get(1));
    }};


    public ImpossibleGameLevelPlugin(String levelName, float mapSizeX, float mapSizeY) {

        System.out.println("ImpossibleGameLevelPlugin constructor");
        this.levelData = loadLevelData(levelName);
        this.mapSizeX = mapSizeX;
        this.mapSizeY = mapSizeY;
        this.jumpSettings = new JumpSettings(
                LunaSettings.getFloat(IGMisc.LunaLibKeys.IG_MOD_ID, IGMisc.LunaLibKeys.GRAVITY_FORCE_ID),
                LunaSettings.getFloat(IGMisc.LunaLibKeys.IG_MOD_ID, IGMisc.LunaLibKeys.JUMP_FORCE_ID),
                LunaSettings.getFloat(IGMisc.LunaLibKeys.IG_MOD_ID, IGMisc.LunaLibKeys.MAX_JUMP_VELOCITY_ID),
                LunaSettings.getFloat(IGMisc.LunaLibKeys.IG_MOD_ID, IGMisc.LunaLibKeys.GROUND_TOLERANCE_ID)
        );

        this.keyBindings = new KeyBindings(
                safelyRetrieveCharactersFromLunalib(new ArrayList<String>() {{
                    add(IGMisc.LunaLibKeys.JUMP_KEY_ID);
                    add(IGMisc.LunaLibKeys.ALTERNATIVE_JUMP_KEY_ID);
                }}),
                safelyRetrieveCharactersFromLunalib(
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

        ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();
        playerShip.setControlsLocked(true);
        playerShip.setPhased(true);
        playerShip.getVelocity().set(new Vector2f(0, 0));
        playerShip.getLocation().set(new Vector2f(0, 0));
    }

    @Override
    public void init(CombatEngineAPI engine) {
//        System.out.println("ImpossibleGameLevelPlugin init");
    }

    public Boolean hasCalledFakeInit = false;
    public void fakeInit(CombatEngineAPI engine) {
        System.out.println("ImpossibleGameLevelPlugin fakeInit");
        this.impossibleGameLevelEngine = new ImpossibleGameLevelEngine(this.levelData, this.mapSizeX, this.mapSizeY, objectLookUpTable);
        engine.addPlugin(this.impossibleGameLevelEngine);

        KillPlayerWhenAnyPlayerDamageIsTaken killPlayerWhenAnyPlayerDamageIsTaken = new KillPlayerWhenAnyPlayerDamageIsTaken();
        engine.getListenerManager().addListener(killPlayerWhenAnyPlayerDamageIsTaken);

        this.createJumper();
    }

   public void createJumper() {
        CombatEngineAPI engine = Global.getCombatEngine();
       this.jumper = engine.getFleetManager(0).spawnShipOrWing(jumperVariantID, new Vector2f(0,0), 90);
       this.jumper.makeLookDisabled();
       this.jumper.getVelocity().set(0, 0);

       this.jumpScript = new JumpScript(this.jumper, groundShipIDs, this.jumpSettings, this.keyBindings.jumpKeys);
       engine.addPlugin(this.jumpScript);
       if (this.impossibleGameLevelEngine != null) {
           this.impossibleGameLevelEngine.positionJumper(this.jumper);
       }
   }



    public static int[][] loadLevelData(String levelName){
        try{
            String levelDataRaw = Global.getSettings().loadText("data/missions/"+levelName+"/level_data.txt");
            String[] Q = levelDataRaw.split("\n");
            String firstRow = Q[0].endsWith(",") ? Q[0].substring(0, Q[0].length()-1) : Q[0];
            int[][] data = new int[Q.length][firstRow.split(",").length];

            for (int i = 0; i < Q.length; i++) {
                String charactersTMP = Q[i].endsWith(",") ? Q[i].substring(0, Q[i].length()-1) : Q[i];
                String[] characters = charactersTMP.split(",");
                for (int j = 0; j < characters.length; j++) {
                    data[i][j] = Integer.parseInt(characters[j]);
                }
            }
            int[][] result = IGMisc.transposeMatrix(data);
            return result;
        } catch (Exception e){
            throw new RuntimeException("Failed to load level data", e);
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

    public static List<Character> safelyRetrieveCharactersFromLunalib(List<String> ids) {
        List<Character> characters = new ArrayList<>();
        for (String id : ids) {
            String value = LunaSettings.getString(IGMisc.LunaLibKeys.IG_MOD_ID, id);
            if (value != null && !value.isEmpty()) {
                characters.add(value.charAt(0));
            }
        }
        return characters;
    }
    public static Logger getLogger() {
        Logger logger = Global.getLogger(ImpossibleGameLevelPlugin.class);
        logger.setLevel(Level.INFO);
        return logger;
    }
}

