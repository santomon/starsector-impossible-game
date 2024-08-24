package santomon.ImpossibleGame;

public class JumpSettings {
    public float gravity;
    public float jumpForce;
    public float maxVelocity;
    public float groundTolerance;

    JumpSettings(
            float gravity,
            float jumpForce,
            float maxVelocity,
            float groundTolerance
    ) {
        this.gravity = gravity;
        this.jumpForce = jumpForce;
        this.maxVelocity = maxVelocity;
        this.groundTolerance = groundTolerance;
    }
}
