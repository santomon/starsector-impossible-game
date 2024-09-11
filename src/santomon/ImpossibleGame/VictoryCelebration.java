package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class VictoryCelebration extends BaseEveryFrameCombatPlugin {
    private static final float explosionMinSize = 150f;
    private static final float explosionMaxSize = 600f;
    public static final Color explosionColor = new Color(255, 255, 255);
    public static final float explosionDuration = 2f;

    private float rate;  // λ, spikes per second
    private float cumulativeTime = 0;  // To track the total time passed
    private float timeToNextSpike;  // Time until the next spike happens
    private final float maxDuration;
    private float totalElapsedTime;

    public VictoryCelebration(float explosionsPerSecond, float maxDuration) {
        this.rate = explosionsPerSecond;  // Set the rate (λ)
        this.timeToNextSpike = generateNextSpikeTime();  // Generate the initial time to next spike
        this.maxDuration = maxDuration;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        cumulativeTime += amount;  // Update the cumulative time
        totalElapsedTime += amount;

        // Check if it's time to trigger a spike
        if (cumulativeTime >= timeToNextSpike) {
            // Trigger a spike!
            triggerExplosion();

            // Reset cumulative time and calculate time for the next spike
            cumulativeTime -= timeToNextSpike;  // Subtract the time to the spike
            timeToNextSpike = generateNextSpikeTime();  // Generate the next spike time
        }
        if (maxDuration < totalElapsedTime) {
            CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
            combatEngineAPI.endCombat(2f, FleetSide.PLAYER);
            combatEngineAPI.removePlugin(this);
        }
    }

    private void triggerExplosion() {
        System.out.println("EKUSUPLOOOOSION");
        ViewportAPI viewportAPI = Global.getCombatEngine().getViewport();
        float randomX = (float) Math.random();
        float randomY = (float) Math.random();
        System.out.println("center " + viewportAPI.getCenter());
        System.out.println("LLX " + viewportAPI.getLLX());
        float LRX = (viewportAPI.getCenter().x - viewportAPI.getLLX()) * 2 + viewportAPI.getLLX();
        float ULY = (viewportAPI.getCenter().y - viewportAPI.getLLY()) * 2 + viewportAPI.getLLY();

        float locationX = (LRX - viewportAPI.getLLX()) * randomX + viewportAPI.getLLX();
        float locationY = (ULY - viewportAPI.getLLY()) * randomY + viewportAPI.getLLY();

        Vector2f location = new Vector2f( locationX, locationY );

        float explosionSize = explosionMaxSize + (float) Math.random() * (explosionMaxSize - explosionMinSize);

        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        combatEngineAPI.spawnExplosion(location, new Vector2f(0, 0), explosionColor, explosionSize, explosionDuration);

    }


    private float generateNextSpikeTime() {
        return (float) (-Math.log(Math.random()) / rate);  // Exponential distribution formula
    }


}
