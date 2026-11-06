import java.util.List;

/**
 * Base class for every living cell in the simulation.
 * A cell has a type, an age (in turns) and health points (HP).
 * It dies when its HP drops to 0 or when it reaches {@link #MAX_AGE}.
 */
public abstract class Cell {
    public static final int MAX_AGE = 10;
    public static final int STARTING_HP = 100;

    private final CellType type;
    private int age;
    private int hp;

    protected Cell(CellType type) {
        this.type = type;
        this.age = 0;
        this.hp = STARTING_HP;
    }

    public CellType getType() {
        return type;
    }

    public int getAge() {
        return age;
    }

    public int getHp() {
        return hp;
    }

    void changeHp(int delta) {
        hp += delta;
    }

    void growOlder() {
        age++;
    }

    public boolean isAlive() {
        return hp > 0 && age < MAX_AGE;
    }

    /**
     * Called once per turn on cells that survived the turn.
     * Returns the cells that replace this one if it divides,
     * or an empty list if it doesn't reproduce.
     */
    public List<Cell> reproduce() {
        return List.of();
    }

    @Override
    public String toString() {
        return String.format("%-16s age: %2d   HP: %4d", type.getDisplayName(), age, hp);
    }
}
