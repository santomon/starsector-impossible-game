package santomon.ImpossibleGame;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;

import javax.swing.text.View;
import java.util.List;

public class CameraControl extends BaseEveryFrameCombatPlugin {

    static final float horizontalOffset = 600f;
    static final float verticalTolerance = 200f;
    static final float cameraSpeed = 1000f;


    ShipAPI target;

    boolean shouldMove = false;
    private boolean isActive = false;
    private float aspectRatio;


    private final Vector2f center;


    CameraControl(ShipAPI target ) {
        this.target = target;

        ViewportAPI viewportAPI = Global.getCombatEngine().getViewport();
        float X = viewportAPI.getCenter().x - viewportAPI.getLLX();
        float Y = viewportAPI.getCenter().y - viewportAPI.getLLY();
        this.aspectRatio = X / Y;



        center = new Vector2f(0,0);
        center.setY(target.getLocation().y);
        center.setX(target.getLocation().x + horizontalOffset);
    }

    public void setIsActive(boolean isActive) {
        Global.getCombatEngine().getViewport().setExternalControl(isActive);

        if (isActive) {
            ViewportAPI viewportAPI = Global.getCombatEngine().getViewport();
            viewportAPI.setViewMult(2f);
        }
        this.isActive = isActive;

    }

    public boolean isActive() {
        return this.isActive;
    }



    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        ViewportAPI viewportAPI = Global.getCombatEngine().getViewport();
        if (!this.isActive) return;
        if (this.target == null) return;

        if (Math.abs(this.center.y - this.target.getLocation().y) > verticalTolerance) {
            this.shouldMove = true;
        }
        maybeMove(amount);
        setViewportParameters();
    }

    private void setViewportParameters() {
        // try to have the jumper at around 25% of the horizon
        // vertical will have to be calculated from the aspect ratio / resolution or some shit? (MonkaS)
        ViewportAPI viewportAPI = Global.getCombatEngine().getViewport();
        float LLX = this.center.x - horizontalOffset * 2;
        float width = 4 * horizontalOffset;
        float height = width / this.aspectRatio;
        float LLY = this.center.y - height / 2;
        viewportAPI.set(LLX, LLY, width, height);
    }

    private void maybeMove(float timePassed) {
        if (!this.shouldMove) return;

        if (Math.abs(this.center.y - this.target.getLocation().y) < verticalTolerance) {
            this.shouldMove = false;
            return;
        }

        // for now lets have camera move at constant speed
        int signum = this.target.getLocation().y - this.center.y > 0 ? 1 : -1;
        this.center.setY(this.center.getY() + signum * cameraSpeed * timePassed);
        this.center.setX(this.target.getLocation().x + horizontalOffset);
    }

}
