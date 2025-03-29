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

    public static class IDs {
        public static final String IG_WARDEN_ID = "warden_Defense";
        public static final String IG_KITE_VARIANT_ID = "impossible_kite_variant";
        public static final String IG_HERMES_VARIANT_ID = "impossible_hermes_variant";

        public static final String IG_CHAOZ_FANTASY_OST = "impossible_chaoz_fantasy_ost";

    }


    public static class FALLBACK_VALUES {
        public static final Integer DEFAULT_TILE_SIZE = 100;
    }


}
