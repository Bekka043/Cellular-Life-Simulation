package cellsim.sim;

import java.util.Random;

/**
 * A tissue cell. It slowly heals itself, divides into empty space to regrow
 * damaged tissue, and calls in white blood cells when bacteria attack it.
 */
public class HumanCell extends Cell {
    static final int BASE_LIFESPAN = 240;
    static final int REGENERATION = 2;
    static final int DIVISION_MIN_HP = 80;
    static final double DIVISION_CHANCE = 0.10;
    static final double RECRUIT_CHANCE = 0.012;

    public HumanCell(Random random) {
        this(MAX_HP, random);
    }

    private HumanCell(int hp, Random random) {
        super(CellType.HUMAN_CELL, BASE_LIFESPAN, hp, random);
    }

    @Override
    protected int upkeep() {
        return REGENERATION;
    }

    @Override
    protected void act(Simulation sim) {
        Grid grid = sim.getGrid();
        if (grid.countNeighbours(getX(), getY(), CellType.BACTERIA) > 0) {
            // Under attack: signal the immune system instead of growing.
            if (sim.random().nextDouble() < RECRUIT_CHANCE * sim.getImmuneStrength()) {
                sim.recruitWhiteBloodCellNear(getX(), getY());
            }
            return;
        }
        if (getHp() >= DIVISION_MIN_HP && sim.random().nextDouble() < DIVISION_CHANCE) {
            int[] target = grid.randomEmptyNeighbour(getX(), getY(), sim.random());
            if (target != null) {
                // The daughter cell takes half of the parent's HP.
                int half = getHp() / 2;
                changeHp(-half);
                sim.spawn(new HumanCell(half, sim.random()), target[0], target[1]);
            }
        }
    }
}
