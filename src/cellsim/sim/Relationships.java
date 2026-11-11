package cellsim.sim;

/**
 * Defines how neighbouring cells affect each other's HP every turn.
 *
 * EFFECT[source][target] is the HP change that one source cell at full
 * vitality applies to an adjacent target cell per turn. Positive values heal,
 * negative values damage. The real effect is scaled by the source cell's
 * {@link Cell#vitality() vitality}.
 */
public final class Relationships {

    private static final double[][] EFFECT = {
            //                   target:  WBC   Human  Bacteria
            /* White blood cell */     {   0,     0,   -18 },  // white blood cells attack bacteria only
            /* Human cell       */     {   0,     0,     5 },  // human cells are food for bacteria
            /* Bacteria         */     {  -4,   -20,    -1 },  // bacteria damage tissue, fight back, and crowd each other
    };

    private Relationships() {
    }

    public static double effect(CellType source, CellType target) {
        return EFFECT[source.ordinal()][target.ordinal()];
    }
}
