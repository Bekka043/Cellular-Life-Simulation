package cellsim.sim;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Runs the simulation: owns the grid and advances it one turn at a time.
 *
 * Each turn has two phases. First, every cell's HP change is computed from
 * the same snapshot of the grid and applied at once, so the order in which
 * cells are visited doesn't matter. Then the survivors act (move, divide,
 * call for help) one by one in random order.
 *
 * A run is fully determined by its scenario and seed.
 */
public final class Simulation {
    private static final double PATROL_ARRIVAL_CHANCE = 0.2;
    private static final int RECRUIT_RADIUS = 4;
    private static final int PLACEMENT_ATTEMPTS = 20;

    private final Scenario scenario;
    private final long seed;
    private final Random random;
    private final Grid grid;
    private final Statistics statistics = new Statistics();
    private double immuneStrength;
    private int turn;

    public Simulation(Scenario scenario, long seed) {
        this.scenario = scenario;
        this.seed = seed;
        this.random = new Random(seed);
        this.grid = new Grid(scenario.width(), scenario.height());
        this.immuneStrength = scenario.immuneStrength();
        populate();
        statistics.snapshot(0, grid);
    }

    // ---------------------------------------------------------------- setup

    private void populate() {
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                if (random.nextDouble() < scenario.tissueDensity()) {
                    HumanCell cell = new HumanCell(random);
                    // Mixed ages, so the tissue renews gradually instead of all at once.
                    cell.setAge(random.nextInt(cell.getLifespan()));
                    spawn(cell, x, y);
                }
            }
        }
        for (int site = 0; site < scenario.infectionSites(); site++) {
            int cx = random.nextInt(grid.getWidth());
            int cy = random.nextInt(grid.getHeight());
            int r = scenario.infectionRadius();
            for (int y = cy - r; y <= cy + r; y++) {
                for (int x = cx - r; x <= cx + r; x++) {
                    boolean insideCircle = (x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r;
                    if (insideCircle && grid.inBounds(x, y)) {
                        Bacteria bacteria = new Bacteria(random);
                        bacteria.setAge(random.nextInt(Bacteria.MATURE_AGE + 1));
                        replace(bacteria, x, y);
                    }
                }
            }
        }
        for (int i = 0; i < scenario.whiteBloodCells(); i++) {
            int x = random.nextInt(grid.getWidth());
            int y = random.nextInt(grid.getHeight());
            Cell occupant = grid.get(x, y);
            if (occupant == null || occupant.getType() == CellType.HUMAN_CELL) {
                WhiteBloodCell cell = new WhiteBloodCell(random);
                cell.setAge(random.nextInt(cell.getLifespan() / 2));
                replace(cell, x, y);
            }
        }
    }

    // ----------------------------------------------------------------- turn

    /** Advances the simulation by one turn. */
    public void step() {
        turn++;
        List<Cell> cells = grid.cells();

        // Phase 1: interactions, computed from one snapshot and applied together.
        int[] hpChange = new int[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            hpChange[i] = hpChangeFor(cells.get(i));
        }
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            cell.changeHp(hpChange[i]);
            cell.growOlder();
        }
        for (Cell cell : cells) {
            if (!cell.isAlive()) {
                remove(cell);
            }
        }

        // Phase 2: survivors act in random order.
        Collections.shuffle(cells, random);
        for (Cell cell : cells) {
            if (grid.get(cell.getX(), cell.getY()) == cell) {
                cell.act(this);
            }
        }

        maintainPatrol();
        statistics.snapshot(turn, grid);
    }

    private int hpChangeFor(Cell cell) {
        double change = cell.upkeep();
        for (int[] offset : Grid.NEIGHBOUR_OFFSETS) {
            int nx = cell.getX() + offset[0];
            int ny = cell.getY() + offset[1];
            if (grid.inBounds(nx, ny) && grid.get(nx, ny) != null) {
                Cell neighbour = grid.get(nx, ny);
                change += Relationships.effect(neighbour.getType(), cell.getType()) * neighbour.vitality();
            }
        }
        return (int) Math.round(change);
    }

    /** Keeps a small number of white blood cells circulating even when there is no infection. */
    private void maintainPatrol() {
        int target = (int) Math.round(scenario.whiteBloodCells() * Math.min(1.0, immuneStrength));
        if (grid.count(CellType.WHITE_BLOOD_CELL) < target && random.nextDouble() < PATROL_ARRIVAL_CHANCE) {
            int x = random.nextInt(grid.getWidth());
            int y = random.nextInt(grid.getHeight());
            whiteBloodCellArrives(x, y, Math.max(grid.getWidth(), grid.getHeight()));
        }
    }

    /**
     * A new white blood cell enters the tissue within the given distance of
     * (x, y). It prefers a free tile; if the tissue is packed it pushes a
     * human cell aside, which costs that cell its life.
     */
    private void whiteBloodCellArrives(int x, int y, int radius) {
        int[] fallback = null;
        for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
            int nx = x + random.nextInt(2 * radius + 1) - radius;
            int ny = y + random.nextInt(2 * radius + 1) - radius;
            if (!grid.inBounds(nx, ny)) {
                continue;
            }
            Cell occupant = grid.get(nx, ny);
            if (occupant == null) {
                spawn(new WhiteBloodCell(random), nx, ny);
                return;
            }
            if (fallback == null && occupant.getType() == CellType.HUMAN_CELL) {
                fallback = new int[] {nx, ny};
            }
        }
        if (fallback != null) {
            replace(new WhiteBloodCell(random), fallback[0], fallback[1]);
        }
    }

    // ------------------------------------------- operations used by the cells

    Random random() {
        return random;
    }

    /** Puts a new cell on an empty tile. */
    void spawn(Cell cell, int x, int y) {
        grid.put(cell, x, y);
        statistics.recordBirth(cell.getType());
    }

    void remove(Cell cell) {
        grid.put(null, cell.getX(), cell.getY());
        statistics.recordDeath(cell.getType());
    }

    /** Moves a cell to an empty tile. */
    void move(Cell cell, int x, int y) {
        grid.put(null, cell.getX(), cell.getY());
        grid.put(cell, x, y);
    }

    /** Moves a cell to a tile, trading places with whatever cell is already there. */
    void moveOrSwap(Cell cell, int x, int y) {
        Cell other = grid.get(x, y);
        int oldX = cell.getX();
        int oldY = cell.getY();
        grid.put(cell, x, y);
        grid.put(other, oldX, oldY);
    }

    /** Called by tissue under attack: a new white blood cell arrives close to (x, y). */
    void recruitWhiteBloodCellNear(int x, int y) {
        whiteBloodCellArrives(x, y, RECRUIT_RADIUS);
    }

    // ------------------------------------------------------------ public API

    /** Places a new cell of the given type at (x, y), replacing whatever was there. */
    public void place(CellType type, int x, int y) {
        if (!grid.inBounds(x, y)) {
            return;
        }
        replace(switch (type) {
            case HUMAN_CELL -> new HumanCell(random);
            case BACTERIA -> new Bacteria(random);
            case WHITE_BLOOD_CELL -> new WhiteBloodCell(random);
        }, x, y);
        statistics.snapshot(turn, grid);
    }

    /** Empties the tile at (x, y). */
    public void clear(int x, int y) {
        if (grid.inBounds(x, y) && grid.get(x, y) != null) {
            remove(grid.get(x, y));
            statistics.snapshot(turn, grid);
        }
    }

    private void replace(Cell cell, int x, int y) {
        if (grid.get(x, y) != null) {
            remove(grid.get(x, y));
        }
        spawn(cell, x, y);
    }

    public Grid getGrid() {
        return grid;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public long getSeed() {
        return seed;
    }

    public int getTurn() {
        return turn;
    }

    public double getImmuneStrength() {
        return immuneStrength;
    }

    public void setImmuneStrength(double immuneStrength) {
        this.immuneStrength = Math.max(0, immuneStrength);
    }
}
