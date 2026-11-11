package cellsim.sim;

import java.util.Random;

/**
 * A bacterium. It feeds on neighbouring human cells (damaging them), starves
 * without them, crawls towards tissue when it has nothing to eat, and splits in
 * two when it is mature, well fed and has room.
 */
public class Bacteria extends Cell {
    static final int BASE_LIFESPAN = 20;
    static final int STARVATION = -4;
    static final int MATURE_AGE = 3;
    static final int DIVISION_MIN_HP = 70;

    public Bacteria(Random random) {
        this(MAX_HP, random);
    }

    private Bacteria(int hp, Random random) {
        super(CellType.BACTERIA, BASE_LIFESPAN, hp, random);
    }

    @Override
    protected int upkeep() {
        return STARVATION;
    }

    @Override
    protected void act(Simulation sim) {
        Grid grid = sim.getGrid();
        if (getAge() >= MATURE_AGE && getHp() > DIVISION_MIN_HP) {
            int[] target = grid.randomEmptyNeighbour(getX(), getY(), sim.random());
            if (target != null) {
                // Binary fission: this cell becomes one newborn daughter, the other takes the free tile.
                int half = getHp() / 2;
                changeHp(-half);
                setAge(0);
                sim.spawn(new Bacteria(half, sim.random()), target[0], target[1]);
                return;
            }
        }
        if (grid.countNeighbours(getX(), getY(), CellType.HUMAN_CELL) == 0) {
            // Nothing to feed on here: crawl towards the nearest tissue.
            int[] target = grid.emptyNeighbourWithMost(getX(), getY(), CellType.HUMAN_CELL, sim.random());
            if (target != null) {
                sim.move(this, target[0], target[1]);
            }
        }
    }
}
