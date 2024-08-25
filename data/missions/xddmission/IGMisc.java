package data.missions.xddmission;

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


    public static class Constants {
        public static final String IG_DEFENDER_VARIANT_ID = "defender_ImpossibleGame";
        public static final String IG_KITE_VARIANT_ID = "kite_ImpossibleGame";
        public static final String IG_HERMES_VARIANT_ID = "hermes_ImpossibleGame";

        public static final String IG_MOD_ID = "impossibleGame";
    }


}
