package cellsim.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * An immune cell. It hunts the nearest bacterium it can sense, squeezing
 * between human cells to reach it, and damages any bacteria next to it.
 */
public class WhiteBloodCell extends Cell {
    static final int BASE_LIFESPAN = 45;
    static final int SENSE_RADIUS = 8;
    static final double WANDER_CHANCE = 0.5;

    public WhiteBloodCell(Random random) {
        super(CellType.WHITE_BLOOD_CELL, BASE_LIFESPAN, MAX_HP, random);
    }

    @Override
    protected int upkeep() {
        return 0;
    }

    @Override
    protected void act(Simulation sim) {
        Grid grid = sim.getGrid();
        Cell prey = grid.nearest(getX(), getY(), CellType.BACTERIA, SENSE_RADIUS);

        if (prey == null) {
            if (sim.random().nextDouble() < WANDER_CHANCE) {
                stepTo(sim, passableNeighbours(grid, Integer.MAX_VALUE, 0, 0));
            }
            return;
        }
        int distance = Grid.distance(getX(), getY(), prey.getX(), prey.getY());
        if (distance > 1) {
            // Only consider steps that bring us closer to the prey.
            stepTo(sim, passableNeighbours(grid, distance, prey.getX(), prey.getY()));
        }
        // At distance 1 we stay put: the interaction matrix does the fighting.
    }

    /** Neighbouring tiles we can enter (empty or human cell) that are closer than maxDistance to the target. */
    private List<int[]> passableNeighbours(Grid grid, int maxDistance, int targetX, int targetY) {
        List<int[]> options = new ArrayList<>();
        for (int[] offset : Grid.NEIGHBOUR_OFFSETS) {
            int nx = getX() + offset[0];
            int ny = getY() + offset[1];
            if (!grid.inBounds(nx, ny)) {
                continue;
            }
            Cell occupant = grid.get(nx, ny);
            boolean passable = occupant == null || occupant.getType() == CellType.HUMAN_CELL;
            if (passable && Grid.distance(nx, ny, targetX, targetY) < maxDistance) {
                options.add(new int[] {nx, ny});
            }
        }
        return options;
    }

    private void stepTo(Simulation sim, List<int[]> options) {
        if (options.isEmpty()) {
            return;
        }
        Random random = sim.random();
        int[] target = options.get(random.nextInt(options.size()));
        sim.moveOrSwap(this, target[0], target[1]);
    }
}
