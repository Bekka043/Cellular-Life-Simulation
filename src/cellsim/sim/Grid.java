package cellsim.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The 2D tissue. Each tile is either empty or holds exactly one cell. */
public final class Grid {
    /** Offsets of the 8 tiles surrounding a tile. */
    static final int[][] NEIGHBOUR_OFFSETS = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1, 0}, {1, 0},
            {-1, 1}, {0, 1}, {1, 1},
    };

    private final int width;
    private final int height;
    private final Cell[][] tiles;

    Grid(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Cell[height][width];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /** The cell at (x, y), or null if the tile is empty. */
    public Cell get(int x, int y) {
        return tiles[y][x];
    }

    public int count(CellType type) {
        int count = 0;
        for (Cell[] row : tiles) {
            for (Cell cell : row) {
                if (cell != null && cell.getType() == type) {
                    count++;
                }
            }
        }
        return count;
    }

    /** All living cells, row by row. */
    public List<Cell> cells() {
        List<Cell> cells = new ArrayList<>();
        for (Cell[] row : tiles) {
            for (Cell cell : row) {
                if (cell != null) {
                    cells.add(cell);
                }
            }
        }
        return cells;
    }

    /** Number of steps between two tiles when diagonal moves are allowed. */
    static int distance(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    void put(Cell cell, int x, int y) {
        tiles[y][x] = cell;
        if (cell != null) {
            cell.setPosition(x, y);
        }
    }

    int countNeighbours(int x, int y, CellType type) {
        int count = 0;
        for (int[] offset : NEIGHBOUR_OFFSETS) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            if (inBounds(nx, ny) && tiles[ny][nx] != null && tiles[ny][nx].getType() == type) {
                count++;
            }
        }
        return count;
    }

    /** A random empty tile next to (x, y) as {x, y}, or null if there is none. */
    int[] randomEmptyNeighbour(int x, int y, Random random) {
        int[] choice = null;
        int seen = 0;
        for (int[] offset : NEIGHBOUR_OFFSETS) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            if (inBounds(nx, ny) && tiles[ny][nx] == null) {
                seen++;
                // Reservoir sampling: every empty neighbour is equally likely.
                if (random.nextInt(seen) == 0) {
                    choice = new int[] {nx, ny};
                }
            }
        }
        return choice;
    }

    /**
     * The empty tile next to (x, y) that has the most neighbours of the given
     * type, as {x, y}. Ties are broken at random. Null if no neighbour is empty.
     */
    int[] emptyNeighbourWithMost(int x, int y, CellType type, Random random) {
        int[] choice = null;
        int bestCount = -1;
        int ties = 0;
        for (int[] offset : NEIGHBOUR_OFFSETS) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            if (!inBounds(nx, ny) || tiles[ny][nx] != null) {
                continue;
            }
            int count = countNeighbours(nx, ny, type);
            if (count > bestCount) {
                bestCount = count;
                ties = 1;
                choice = new int[] {nx, ny};
            } else if (count == bestCount && random.nextInt(++ties) == 0) {
                choice = new int[] {nx, ny};
            }
        }
        return choice;
    }

    /** The closest cell of the given type within the radius, or null. */
    Cell nearest(int x, int y, CellType type, int radius) {
        Cell best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int ny = Math.max(0, y - radius); ny <= Math.min(height - 1, y + radius); ny++) {
            for (int nx = Math.max(0, x - radius); nx <= Math.min(width - 1, x + radius); nx++) {
                Cell cell = tiles[ny][nx];
                if (cell != null && cell.getType() == type) {
                    int d = distance(x, y, nx, ny);
                    if (d < bestDistance) {
                        best = cell;
                        bestDistance = d;
                    }
                }
            }
        }
        return best;
    }
}
