package santomon.ImpossibleGame;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

public class MoveVerticalRelative implements AdvanceableListener {
    ShipAPI self;
    ShipAPI target;

    boolean shouldMove = false;

    final float verticalTolerance = 200f;
    final float cameraSpeed = 1000f;


    MoveVerticalRelative(ShipAPI self, ShipAPI target ) {
        this.self = self;
        this.target = target;
    }

    @Override
    public void advance(float amount) {
        if (this.self == null || this.target == null) return;

        if (Math.abs(this.self.getLocation().y - this.target.getLocation().y) > verticalTolerance) {
            this.shouldMove = true;
        }
        maybeMove(amount);
        lockSelf();
    }

    private void maybeMove(float timePassed) {
        if (!this.shouldMove) return;

        if (Math.abs(this.self.getLocation().y - this.target.getLocation().y) < verticalTolerance) {
            this.shouldMove = false;
            return;
        }

        // for now lets have camera move at constant speed
        int signum = this.target.getLocation().y - this.self.getLocation().y > 0 ? 1 : -1;
        this.self.getLocation().setY(this.self.getLocation().getY() + signum * cameraSpeed * timePassed);
    }

    private void lockSelf() {
        this.self.getVelocity().set(0, 0);
    }
}
