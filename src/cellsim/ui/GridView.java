package cellsim.ui;

import cellsim.sim.Cell;
import cellsim.sim.CellType;
import cellsim.sim.Grid;
import cellsim.sim.Simulation;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Draws the tissue and lets the user paint cells onto it.
 * Human cells are squares, bacteria and white blood cells are circles;
 * the dimmer a cell, the lower its HP.
 */
final class GridView extends JComponent {
    /** What a click on the grid does. */
    enum Tool {
        BACTERIA("Bacteria", CellType.BACTERIA),
        WHITE_BLOOD_CELL("White blood cell", CellType.WHITE_BLOOD_CELL),
        HUMAN_CELL("Human cell", CellType.HUMAN_CELL),
        ERASE("Erase", null);

        private final String label;
        private final CellType cellType;

        Tool(String label, CellType cellType) {
            this.label = label;
            this.cellType = cellType;
        }

        String label() {
            return label;
        }
    }

    private static final int PREFERRED_TILE = 14;
    /** The brush covers the clicked tile and the four tiles touching it. */
    private static final int[][] BRUSH = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private Simulation simulation;
    private Tool tool = Tool.BACTERIA;
    private Runnable onEdit = () -> { };

    GridView(Simulation simulation) {
        this.simulation = simulation;
        setOpaque(true);
        ToolTipManager.sharedInstance().registerComponent(this);

        MouseAdapter painter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                applyTool(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                applyTool(e);
            }
        };
        addMouseListener(painter);
        addMouseMotionListener(painter);
    }

    void setSimulation(Simulation simulation) {
        this.simulation = simulation;
        repaint();
    }

    void setTool(Tool tool) {
        this.tool = tool;
    }

    /** Called after the user paints on the grid. */
    void setOnEdit(Runnable onEdit) {
        this.onEdit = onEdit;
    }

    @Override
    public Dimension getPreferredSize() {
        Grid grid = simulation.getGrid();
        return new Dimension(grid.getWidth() * PREFERRED_TILE, grid.getHeight() * PREFERRED_TILE);
    }

    // -------------------------------------------------------------- geometry

    private int tileSize() {
        Grid grid = simulation.getGrid();
        return Math.max(2, Math.min(getWidth() / grid.getWidth(), getHeight() / grid.getHeight()));
    }

    private int originX() {
        return (getWidth() - tileSize() * simulation.getGrid().getWidth()) / 2;
    }

    private int originY() {
        return (getHeight() - tileSize() * simulation.getGrid().getHeight()) / 2;
    }

    /** The grid tile under a pixel as {x, y}, or null if the pixel is outside the grid. */
    private int[] tileAt(int px, int py) {
        int tile = tileSize();
        int x = Math.floorDiv(px - originX(), tile);
        int y = Math.floorDiv(py - originY(), tile);
        return simulation.getGrid().inBounds(x, y) ? new int[] {x, y} : null;
    }

    // -------------------------------------------------------------- painting

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Palette.BACKGROUND);
        g.fillRect(0, 0, getWidth(), getHeight());

        Grid grid = simulation.getGrid();
        int tile = tileSize();
        int gap = tile >= 6 ? 1 : 0;
        int ox = originX();
        int oy = originY();

        g.setColor(Palette.EMPTY_TILE);
        g.fillRect(ox, oy, tile * grid.getWidth(), tile * grid.getHeight());

        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                Cell cell = grid.get(x, y);
                if (cell != null) {
                    paintCell(g, cell, ox + x * tile, oy + y * tile, tile, gap);
                }
            }
        }
        g.dispose();
    }

    private void paintCell(Graphics2D g, Cell cell, int px, int py, int tile, int gap) {
        double health = cell.getHp() / (double) Cell.MAX_HP;
        switch (cell.getType()) {
            case HUMAN_CELL -> {
                g.setColor(Palette.blend(Palette.HUMAN_DAMAGED, Palette.HUMAN, health));
                g.fillRect(px, py, tile - gap, tile - gap);
            }
            case BACTERIA -> {
                g.setColor(Palette.blend(Palette.BACTERIA_WEAK, Palette.BACTERIA, health));
                g.fillOval(px, py, tile - gap, tile - gap);
            }
            case WHITE_BLOOD_CELL -> {
                g.setColor(Palette.WHITE_BLOOD_CELL);
                g.fillOval(px, py, tile - gap, tile - gap);
                if (tile >= 8) {
                    g.setColor(Palette.WHITE_BLOOD_CELL_RING);
                    g.setStroke(new BasicStroke(Math.max(1.5f, tile / 7f)));
                    g.drawOval(px + 1, py + 1, tile - gap - 2, tile - gap - 2);
                }
            }
        }
    }

    // ----------------------------------------------------------- interaction

    private void applyTool(MouseEvent e) {
        int[] tile = tileAt(e.getX(), e.getY());
        if (tile == null) {
            return;
        }
        for (int[] offset : BRUSH) {
            int x = tile[0] + offset[0];
            int y = tile[1] + offset[1];
            if (tool.cellType == null) {
                simulation.clear(x, y);
            } else {
                simulation.place(tool.cellType, x, y);
            }
        }
        repaint();
        onEdit.run();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int[] tile = tileAt(e.getX(), e.getY());
        if (tile == null) {
            return null;
        }
        Cell cell = simulation.getGrid().get(tile[0], tile[1]);
        return cell == null ? "Empty" : cell.toString();
    }
}
