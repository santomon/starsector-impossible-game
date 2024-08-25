package santomon.ImpossibleGame;

public class JumpSettings {
    public final float gravity;
    public final float jumpForce;
    public final float maxVelocity;
    public final float groundTolerance;

//    public static final float defaultGravity = 300f;
//    public static final float defaultJumpForce = 100000f;
//    public static final float defaultMaxVelocity = 10000f;
//    public static final float defaultGroundTolerance = 10f;  //

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
