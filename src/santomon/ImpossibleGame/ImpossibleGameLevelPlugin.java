package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventType;
import data.missions.xddmission.IGMisc;
import org.lwjgl.util.vector.Vector2f;
import lunalib.lunaSettings.LunaSettings;

import java.util.List;

public class ImpossibleGameLevelPlugin extends BaseEveryFrameCombatPlugin {

    public final int[][] levelData;
    public final float mapSizeX;
    public final float mapSizeY;
    public final JumpSettings jumpSettings;
    public ImpossibleGameLevelEngine impossibleGameLevelEngine;
    public static final Character jumpKey = ' ';  // ok this works
    public static final Character alternativeJumpKey = 'M';
    private ShipAPI jumper;


    public ImpossibleGameLevelPlugin(String levelName, float mapSizeX, float mapSizeY) {

        System.out.println("ImpossibleGameLevelPlugin constructor");
        this.levelData = loadLevelData(levelName);
        this.mapSizeX = mapSizeX;
        this.mapSizeY = mapSizeY;
        this.jumpSettings = new JumpSettings(
                LunaSettings.getFloat(IGMisc.Constants.IG_MOD_ID, IGMisc.Constants.GRAVITY_FORCE_ID),
                LunaSettings.getFloat(IGMisc.Constants.IG_MOD_ID, IGMisc.Constants.JUMP_FORCE_ID),
                LunaSettings.getFloat(IGMisc.Constants.IG_MOD_ID, IGMisc.Constants.MAX_JUMP_VELOCITY_ID),
                LunaSettings.getFloat(IGMisc.Constants.IG_MOD_ID, IGMisc.Constants.GROUND_TOLERANCE_ID)
        );

    }


    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!hasCalledFakeInit){
            fakeInit(Global.getCombatEngine());
            hasCalledFakeInit = true;
        }

        if (getJumpKeyPressed(events) && this.impossibleGameLevelEngine != null) {
            this.impossibleGameLevelEngine.maybeInitiateJump();
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
        this.impossibleGameLevelEngine = new ImpossibleGameLevelEngine(IGMisc.Constants.IG_HERMES_VARIANT_ID, this.levelData, this.mapSizeX, this.mapSizeY);
        engine.addPlugin(this.impossibleGameLevelEngine);

        KillPlayerWhenAnyPlayerDamageIsTaken killPlayerWhenAnyPlayerDamageIsTaken = new KillPlayerWhenAnyPlayerDamageIsTaken();
        engine.getListenerManager().addListener(killPlayerWhenAnyPlayerDamageIsTaken);
    }

   public ShipAPI createJumper(String jumperVariantID, float tileSize) {
        CombatEngineAPI engine = Global.getCombatEngine();
       this.jumper = engine.getFleetManager(0).spawnShipOrWing(jumperVariantID, new Vector2f(- tileSize * 3,0), 90);
       this.jumper.makeLookDisabled();
       this.jumper.getVelocity().set(0, 0);
       return this.jumper;
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

    public static boolean getJumpKeyPressed(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (event.getEventChar() == jumpKey) {
                return true;
            }
            if (event.getEventType() == InputEventType.MOUSE_DOWN && event.getEventValue() == 0) {
                return true;
            }
        }
        return false;
    }

}

