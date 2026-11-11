package cellsim.sim;

import java.util.Random;

/**
 * Base class for every cell in the simulation.
 *
 * A cell sits on one tile of the grid and has an age, a lifespan and health
 * points (HP). It dies when its HP reaches 0 or when it outlives its lifespan.
 * Subclasses define what the cell does each turn in {@link #act(Simulation)}.
 */
public abstract class Cell {
    public static final int MAX_HP = 100;

    private final CellType type;
    private final int lifespan;
    private int age;
    private int hp;
    private int x;
    private int y;

    /**
     * @param baseLifespan typical lifespan in turns; each cell gets its own
     *                     lifespan within 20% of this so that cells born
     *                     together don't all die on the same turn
     */
    protected Cell(CellType type, int baseLifespan, int hp, Random random) {
        this.type = type;
        this.lifespan = (int) Math.round(baseLifespan * (0.8 + 0.4 * random.nextDouble()));
        this.hp = hp;
    }

    /** HP gained (or lost, if negative) every turn regardless of neighbours. */
    protected abstract int upkeep();

    /** What the cell does after surviving a turn: move, divide, call for help... */
    protected abstract void act(Simulation sim);

    public CellType getType() {
        return type;
    }

    public int getAge() {
        return age;
    }

    public int getLifespan() {
        return lifespan;
    }

    public int getHp() {
        return hp;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isAlive() {
        return hp > 0 && age < lifespan;
    }

    /**
     * How strongly this cell affects its neighbours, from 0.2 to 1.0.
     * Follows a parabola over the cell's life: weak when newborn, strongest at
     * mid-life, weak again when old.
     */
    public double vitality() {
        double lifeFraction = (double) age / lifespan;
        return 0.5 + 2 * lifeFraction * (1 - lifeFraction);
    }

    void changeHp(int delta) {
        hp = Math.min(MAX_HP, hp + delta);
    }

    void growOlder() {
        age++;
    }

    void setAge(int age) {
        this.age = age;
    }

    void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("%s (age %d/%d, HP %d)", type.getDisplayName(), age, lifespan, hp);
    }
}
