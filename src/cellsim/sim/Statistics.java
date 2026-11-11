package cellsim.sim;

import java.util.ArrayList;
import java.util.List;

/** Population history and birth/death totals for one simulation run. */
public final class Statistics {
    private static final int TYPES = CellType.values().length;

    private final int[] born = new int[TYPES];
    private final int[] died = new int[TYPES];
    private final int[] peak = new int[TYPES];
    private final List<int[]> history = new ArrayList<>();

    void recordBirth(CellType type) {
        born[type.ordinal()]++;
    }

    void recordDeath(CellType type) {
        died[type.ordinal()]++;
    }

    /** Stores the population counts for a turn, replacing them if that turn was already recorded. */
    void snapshot(int turn, Grid grid) {
        int[] counts = new int[TYPES];
        for (Cell cell : grid.cells()) {
            counts[cell.getType().ordinal()]++;
        }
        if (turn < history.size()) {
            history.set(turn, counts);
        } else {
            history.add(counts);
        }
        for (int i = 0; i < TYPES; i++) {
            peak[i] = Math.max(peak[i], counts[i]);
        }
    }

    /** Number of recorded turns, including turn 0. */
    public int turns() {
        return history.size();
    }

    public int countAt(int turn, CellType type) {
        return history.get(turn)[type.ordinal()];
    }

    public int living(CellType type) {
        return countAt(history.size() - 1, type);
    }

    public int peak(CellType type) {
        return peak[type.ordinal()];
    }

    public int born(CellType type) {
        return born[type.ordinal()];
    }

    public int died(CellType type) {
        return died[type.ordinal()];
    }
}
