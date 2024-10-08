package santomon.ImpossibleGame;

public class IGMisc {


    public static int[][] transposeMatrix(int[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;

        int[][] transposedMatrix = new int[cols][rows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transposedMatrix[j][i] = matrix[i][j];
            }
        }

        return transposedMatrix;
    }

    public static class LunaLibKeys {
        public static final String IG_WARDEN_ID = "warden_Defense";
        public static final String IG_KITE_VARIANT_ID = "kite_ImpossibleGame";
        public static final String IG_HERMES_VARIANT_ID = "impossible_hermes_variant";

        public static final String JUMPER_VARIANT_ID = "impossible_jumperVariantID";

        public static final String IG_MOD_ID = "impossibleGame";
        public static final String GRAVITY_FORCE_ID = "impossible_gravityForce";
        public static final String MAX_JUMP_VELOCITY_ID = "impossible_maxJumpVelocity";
        public static final String JUMP_FORCE_ID = "impossible_jumpForce";
        public static final String GROUND_TOLERANCE_ID = "impossible_groundTolerance";

        public static final String JUMP_KEY_ID = "impossible_jumpKey";
        public static final String ALTERNATIVE_JUMP_KEY_ID = "impossible_alternativeJumpKey";
        public static final String QUICK_RESTART_KEY_ID = "impossible_quickRestartKey";

    }


    public static class FALLBACK_VALUES {
        public static final Integer DEFAULT_TILE_SIZE = 100;
    }


}
