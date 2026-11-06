import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the population and advances it turn by turn.
 *
 * Each turn is synchronous (like the Game of Life): every cell's HP change is
 * computed from the same snapshot of the population, then all changes are
 * applied at once, then deaths and divisions are resolved.
 */
public class Simulation {
    private final List<Cell> cells = new ArrayList<>();
    private final Map<CellType, Integer> totalCreated = new EnumMap<>(CellType.class);
    private int turn = 0;

    public void add(Cell cell) {
        cells.add(cell);
        totalCreated.merge(cell.getType(), 1, Integer::sum);
    }

    public void step() {
        turn++;
        CellType[] types = CellType.values();

        // 1. Total influence of each cell type, from the current snapshot.
        double[] influence = new double[types.length];
        for (Cell cell : cells) {
            influence[cell.getType().ordinal()] += Relationships.vitality(cell.getAge());
        }

        // 2. HP change for every cell (a cell does not affect itself).
        int[] deltas = new int[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            double delta = 0;
            for (CellType source : types) {
                double sourceInfluence = influence[source.ordinal()];
                if (source == cell.getType()) {
                    sourceInfluence -= Relationships.vitality(cell.getAge());
                }
                delta += Relationships.effect(source, cell.getType()) * sourceInfluence;
            }
            deltas[i] = (int) Math.round(delta);
        }

        // 3. Apply all changes at once and age everyone.
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).changeHp(deltas[i]);
            cells.get(i).growOlder();
        }

        // 4. Build the next generation: drop the dead, replace dividing cells with their offspring.
        List<Cell> next = new ArrayList<>();
        for (Cell cell : cells) {
            if (!cell.isAlive()) {
                continue;
            }
            List<Cell> offspring = cell.reproduce();
            if (offspring.isEmpty()) {
                next.add(cell);
            } else {
                for (Cell child : offspring) {
                    next.add(child);
                    totalCreated.merge(child.getType(), 1, Integer::sum);
                }
            }
        }
        cells.clear();
        cells.addAll(next);
    }

    public void run(int turns) {
        for (int i = 0; i < turns && !cells.isEmpty(); i++) {
            step();
        }
    }

    public int getTurn() {
        return turn;
    }

    public boolean isExtinct() {
        return cells.isEmpty();
    }

    public List<Cell> getCells() {
        return Collections.unmodifiableList(cells);
    }

    public int countLiving(CellType type) {
        int count = 0;
        for (Cell cell : cells) {
            if (cell.getType() == type) {
                count++;
            }
        }
        return count;
    }

    public int countCreated(CellType type) {
        return totalCreated.getOrDefault(type, 0);
    }
}
