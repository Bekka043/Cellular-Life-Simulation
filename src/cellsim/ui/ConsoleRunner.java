package cellsim.ui;

import cellsim.sim.Cell;
import cellsim.sim.CellType;
import cellsim.sim.Grid;
import cellsim.sim.Scenario;
import cellsim.sim.Simulation;
import cellsim.sim.Statistics;

/** Text mode: runs a scenario without the GUI and prints what happened. */
final class ConsoleRunner {
    private static final int TABLE_ROWS = 20;

    private ConsoleRunner() {
    }

    static void run(Scenario scenario, int turns, long seed) {
        Simulation simulation = new Simulation(scenario, seed);
        Statistics stats = simulation.getStatistics();
        int interval = Math.max(1, turns / TABLE_ROWS);

        System.out.printf("Scenario: %s (seed %d)%n%s%n%n", scenario.name(), seed, scenario.description());
        System.out.printf("%6s %12s %10s %18s%n", "turn", "human cells", "bacteria", "white blood cells");
        printRow(simulation);
        for (int turn = 1; turn <= turns; turn++) {
            simulation.step();
            if (turn % interval == 0 || turn == turns) {
                printRow(simulation);
            }
        }

        System.out.println();
        printMap(simulation.getGrid());
        System.out.println();
        System.out.println("Result after " + turns + " turns: " + SimulationPanel.describeStatus(stats));
        for (CellType type : CellType.values()) {
            System.out.printf("  %-18s %5d alive, %5d born, %5d died, peak %d%n", type.getDisplayName() + ":",
                    stats.living(type), stats.born(type), stats.died(type), stats.peak(type));
        }
    }

    private static void printRow(Simulation simulation) {
        Statistics stats = simulation.getStatistics();
        System.out.printf("%6d %12d %10d %18d%n", simulation.getTurn(), stats.living(CellType.HUMAN_CELL),
                stats.living(CellType.BACTERIA), stats.living(CellType.WHITE_BLOOD_CELL));
    }

    private static void printMap(Grid grid) {
        StringBuilder map = new StringBuilder();
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                Cell cell = grid.get(x, y);
                map.append(cell == null ? ' ' : cell.getType().getSymbol());
            }
            map.append('\n');
        }
        System.out.print(map);
        System.out.println("Legend: . human cell   B bacteria   W white blood cell   (blank) empty");
    }
}
