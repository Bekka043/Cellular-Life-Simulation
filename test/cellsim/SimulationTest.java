package cellsim;

import cellsim.sim.Cell;
import cellsim.sim.CellType;
import cellsim.sim.Grid;
import cellsim.sim.Scenario;
import cellsim.sim.Simulation;
import cellsim.sim.Statistics;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tests for the simulation engine. Plain Java with no test framework, so
 * they run anywhere with:
 *
 *   javac -d out -sourcepath src test/cellsim/SimulationTest.java
 *   java -cp out cellsim.SimulationTest
 */
public final class SimulationTest {
    private static final Scenario SMALL_INFECTION = Scenario.find("Small infection");
    private static final Scenario SEVERE_INFECTION = Scenario.find("Severe infection");
    private static final Scenario HEALTHY = Scenario.find("Healthy tissue");

    public static void main(String[] args) {
        Map<String, Runnable> tests = new LinkedHashMap<>();
        tests.put("same seed replays the same run", SimulationTest::sameSeedReplaysTheSameRun);
        tests.put("different seeds give different runs", SimulationTest::differentSeedsGiveDifferentRuns);
        tests.put("grid and statistics stay consistent", SimulationTest::gridAndStatisticsStayConsistent);
        tests.put("healthy tissue stays healthy", SimulationTest::healthyTissueStaysHealthy);
        tests.put("bacteria starve without tissue", SimulationTest::bacteriaStarveWithoutTissue);
        tests.put("untreated infection destroys the tissue", SimulationTest::untreatedInfectionDestroysTissue);
        tests.put("immune system clears a small infection", SimulationTest::immuneSystemClearsSmallInfection);
        tests.put("tissue regrows after an infection", SimulationTest::tissueRegrowsAfterInfection);
        tests.put("painting cells updates the grid", SimulationTest::paintingCellsUpdatesTheGrid);

        int failures = 0;
        for (Map.Entry<String, Runnable> test : tests.entrySet()) {
            try {
                test.getValue().run();
                System.out.println("PASS  " + test.getKey());
            } catch (AssertionError | RuntimeException e) {
                failures++;
                System.out.println("FAIL  " + test.getKey() + ": " + e);
            }
        }
        System.out.printf("%n%d of %d tests passed%n", tests.size() - failures, tests.size());
        if (failures > 0) {
            System.exit(1);
        }
    }

    // ----------------------------------------------------------------- tests

    private static void sameSeedReplaysTheSameRun() {
        Simulation first = run(SEVERE_INFECTION, 42, 150);
        Simulation second = run(SEVERE_INFECTION, 42, 150);
        for (int turn = 0; turn <= 150; turn++) {
            for (CellType type : CellType.values()) {
                check(first.getStatistics().countAt(turn, type) == second.getStatistics().countAt(turn, type),
                        "populations differ at turn " + turn);
            }
        }
    }

    private static void differentSeedsGiveDifferentRuns() {
        Simulation first = run(SEVERE_INFECTION, 1, 50);
        Simulation second = run(SEVERE_INFECTION, 2, 50);
        check(first.getStatistics().living(CellType.BACTERIA) != second.getStatistics().living(CellType.BACTERIA)
                        || first.getStatistics().living(CellType.HUMAN_CELL) != second.getStatistics().living(CellType.HUMAN_CELL),
                "two seeds produced identical populations");
    }

    private static void gridAndStatisticsStayConsistent() {
        Simulation simulation = new Simulation(SEVERE_INFECTION, 7);
        Grid grid = simulation.getGrid();
        Statistics stats = simulation.getStatistics();
        for (int turn = 0; turn < 200; turn++) {
            simulation.step();
            for (int y = 0; y < grid.getHeight(); y++) {
                for (int x = 0; x < grid.getWidth(); x++) {
                    Cell cell = grid.get(x, y);
                    if (cell == null) {
                        continue;
                    }
                    check(cell.getX() == x && cell.getY() == y, "cell at (" + x + "," + y + ") thinks it is elsewhere");
                    check(cell.isAlive(), "dead cell left on the grid: " + cell);
                    check(cell.getHp() <= Cell.MAX_HP, "HP above the maximum: " + cell);
                }
            }
            for (CellType type : CellType.values()) {
                check(grid.count(type) == stats.living(type), "statistics out of date for " + type);
                check(stats.born(type) - stats.died(type) == stats.living(type),
                        "born - died != living for " + type + " at turn " + simulation.getTurn());
            }
        }
    }

    private static void healthyTissueStaysHealthy() {
        Simulation simulation = run(HEALTHY, 3, 500);
        int tiles = HEALTHY.width() * HEALTHY.height();
        check(simulation.getStatistics().living(CellType.HUMAN_CELL) > 0.95 * tiles, "tissue thinned out on its own");
        check(simulation.getStatistics().peak(CellType.BACTERIA) == 0, "bacteria appeared from nowhere");
    }

    private static void bacteriaStarveWithoutTissue() {
        Scenario noTissue = new Scenario("No tissue", "", 30, 30, 0.0, 2, 3, 0, 0.0);
        Simulation simulation = run(noTissue, 5, 60);
        check(simulation.getStatistics().peak(CellType.BACTERIA) > 0, "scenario placed no bacteria");
        check(simulation.getStatistics().living(CellType.BACTERIA) == 0, "bacteria survived with nothing to eat");
    }

    private static void untreatedInfectionDestroysTissue() {
        Scenario noImmuneSystem = new Scenario("No immune system", "", 64, 40, 0.95, 1, 2, 0, 0.0);
        for (long seed = 1; seed <= 5; seed++) {
            Simulation simulation = run(noImmuneSystem, seed, 300);
            Statistics stats = simulation.getStatistics();
            check(stats.living(CellType.HUMAN_CELL) < 0.2 * stats.countAt(0, CellType.HUMAN_CELL),
                    "tissue survived an untreated infection (seed " + seed + ")");
        }
    }

    private static void immuneSystemClearsSmallInfection() {
        for (long seed = 1; seed <= 10; seed++) {
            Simulation simulation = run(SMALL_INFECTION, seed, 300);
            check(simulation.getStatistics().living(CellType.BACTERIA) == 0,
                    "infection not cleared after 300 turns (seed " + seed + ")");
        }
    }

    private static void tissueRegrowsAfterInfection() {
        for (long seed = 1; seed <= 5; seed++) {
            Simulation simulation = run(SEVERE_INFECTION, seed, 1000);
            Statistics stats = simulation.getStatistics();
            int lowest = Integer.MAX_VALUE;
            for (int turn = 0; turn <= 1000; turn++) {
                lowest = Math.min(lowest, stats.countAt(turn, CellType.HUMAN_CELL));
            }
            check(stats.living(CellType.BACTERIA) == 0, "infection still active (seed " + seed + ")");
            check(stats.living(CellType.HUMAN_CELL) > lowest + 300, "tissue did not regrow (seed " + seed + ")");
        }
    }

    private static void paintingCellsUpdatesTheGrid() {
        Simulation simulation = new Simulation(HEALTHY, 1);
        Grid grid = simulation.getGrid();

        simulation.place(CellType.BACTERIA, 10, 10);
        check(grid.get(10, 10).getType() == CellType.BACTERIA, "bacteria was not placed");
        check(simulation.getStatistics().living(CellType.BACTERIA) == 1, "statistics missed the new bacteria");

        simulation.clear(10, 10);
        check(grid.get(10, 10) == null, "tile was not cleared");
        check(simulation.getStatistics().living(CellType.BACTERIA) == 0, "statistics missed the removal");

        simulation.place(CellType.BACTERIA, -1, 500);   // outside the grid: ignored, no exception
        simulation.clear(-1, 500);
    }

    // --------------------------------------------------------------- helpers

    private static Simulation run(Scenario scenario, long seed, int turns) {
        Simulation simulation = new Simulation(scenario, seed);
        for (int turn = 0; turn < turns; turn++) {
            simulation.step();
        }
        return simulation;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
