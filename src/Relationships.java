/**
 * Defines how cells affect each other's HP every turn.
 *
 * EFFECT[source][target] is the HP change a source cell at full strength
 * applies to each target cell per turn. Positive values heal, negative values damage.
 */
public final class Relationships {

    private static final double[][] EFFECT = {
            //                 target:  WBC   Human  Bacteria
            /* White Blood Cell */   {  0.0,   0.0,  -3.0 },  // WBCs attack bacteria only
            /* Human Cell       */   {  0.5,   0.0,   1.0 },  // humans support WBCs and are food for bacteria
            /* Bacteria         */   { -1.0,  -2.0,  -0.5 },  // bacteria damage humans, fight back against WBCs, compete with each other
    };

    private Relationships() {
    }

    public static double effect(CellType source, CellType target) {
        return EFFECT[source.ordinal()][target.ordinal()];
    }

    /**
     * How strongly a cell of the given age influences others, from 0.2 (newborn
     * or very old) up to 1.0 (age 5). Same curve as the original
     * reverse polynomial -(x^2 - 10x), scaled to [0, 1] with a floor of 0.2
     * so newborns aren't completely inert.
     */
    public static double vitality(int age) {
        double curve = -(age * age - 10.0 * age) / 25.0;
        return Math.max(0.2, curve);
    }
}
