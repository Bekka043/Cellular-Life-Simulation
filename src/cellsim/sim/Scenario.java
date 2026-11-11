package cellsim.sim;

import java.util.List;

/**
 * A starting setup for the simulation.
 *
 * @param tissueDensity   fraction of tiles that start as human cells (0 to 1)
 * @param infectionSites  number of bacterial colonies placed at random
 * @param infectionRadius radius of each colony, in tiles
 * @param whiteBloodCells number of white blood cells patrolling the tissue
 * @param immuneStrength  multiplier on how readily attacked tissue calls for white blood cells
 */
public record Scenario(
        String name,
        String description,
        int width,
        int height,
        double tissueDensity,
        int infectionSites,
        int infectionRadius,
        int whiteBloodCells,
        double immuneStrength) {

    private static final int WIDTH = 64;
    private static final int HEIGHT = 40;

    public static final List<Scenario> PRESETS = List.of(
            new Scenario("Small infection",
                    "One bacterial colony in healthy tissue with a normal immune system.",
                    WIDTH, HEIGHT, 0.95, 1, 2, 8, 1.0),
            new Scenario("Multiple infection sites",
                    "Four colonies at once. The immune system has to split its forces.",
                    WIDTH, HEIGHT, 0.95, 4, 2, 8, 1.0),
            new Scenario("Weak immune system",
                    "The same small infection, but tissue is slow to call for help.",
                    WIDTH, HEIGHT, 0.95, 1, 2, 2, 0.35),
            new Scenario("Severe infection",
                    "Three large, established colonies against a normal immune system.",
                    WIDTH, HEIGHT, 0.95, 3, 5, 8, 1.0),
            new Scenario("Healthy tissue",
                    "No bacteria. A sandbox: paint your own infection onto the grid.",
                    WIDTH, HEIGHT, 0.95, 0, 0, 8, 1.0));

    /** Finds a preset by (case-insensitive) name or by 1-based index, or returns null. */
    public static Scenario find(String nameOrIndex) {
        for (int i = 0; i < PRESETS.size(); i++) {
            Scenario preset = PRESETS.get(i);
            if (preset.name().equalsIgnoreCase(nameOrIndex) || String.valueOf(i + 1).equals(nameOrIndex)) {
                return preset;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
