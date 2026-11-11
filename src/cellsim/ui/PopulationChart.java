package cellsim.ui;

import cellsim.sim.CellType;
import cellsim.sim.Simulation;
import cellsim.sim.Statistics;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/**
 * Line chart of the three populations over time.
 *
 * Human cells outnumber the others by an order of magnitude, so they are
 * plotted against the left axis (labelled in their colour) while bacteria and
 * white blood cells share the right axis.
 */
final class PopulationChart extends JComponent {
    private static final int WINDOW = 400;      // turns shown at most
    private static final int MIN_SPAN = 100;    // turns shown at least, so early lines aren't stretched
    private static final int LEFT = 52;
    private static final int RIGHT = 52;
    private static final int TOP = 26;
    private static final int BOTTOM = 22;

    private Simulation simulation;

    PopulationChart(Simulation simulation) {
        this.simulation = simulation;
        setOpaque(true);
        setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
    }

    void setSimulation(Simulation simulation) {
        this.simulation = simulation;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(600, 170);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Palette.BACKGROUND);
        g.fillRect(0, 0, getWidth(), getHeight());

        Statistics stats = simulation.getStatistics();
        int lastTurn = stats.turns() - 1;
        int firstTurn = Math.max(0, lastTurn - WINDOW);
        int span = Math.max(MIN_SPAN, lastTurn - firstTurn);

        int plotWidth = getWidth() - LEFT - RIGHT;
        int plotHeight = getHeight() - TOP - BOTTOM;
        if (plotWidth < 20 || plotHeight < 20) {
            g.dispose();
            return;
        }

        int leftMax = niceCeiling(maxInWindow(stats, firstTurn, lastTurn, CellType.HUMAN_CELL));
        int rightMax = niceCeiling(Math.max(
                maxInWindow(stats, firstTurn, lastTurn, CellType.BACTERIA),
                maxInWindow(stats, firstTurn, lastTurn, CellType.WHITE_BLOOD_CELL)));

        paintAxes(g, plotWidth, plotHeight, leftMax, rightMax, firstTurn, span);
        paintLine(g, stats, CellType.HUMAN_CELL, firstTurn, lastTurn, span, leftMax, plotWidth, plotHeight);
        paintLine(g, stats, CellType.WHITE_BLOOD_CELL, firstTurn, lastTurn, span, rightMax, plotWidth, plotHeight);
        paintLine(g, stats, CellType.BACTERIA, firstTurn, lastTurn, span, rightMax, plotWidth, plotHeight);
        paintLegend(g, stats);
        g.dispose();
    }

    private void paintAxes(Graphics2D g, int plotWidth, int plotHeight, int leftMax, int rightMax,
                           int firstTurn, int span) {
        FontMetrics metrics = g.getFontMetrics();
        int baseline = TOP + plotHeight;

        for (int i = 0; i <= 2; i++) {
            int y = baseline - plotHeight * i / 2;
            g.setColor(Palette.AXIS);
            g.setStroke(new BasicStroke(1f));
            g.drawLine(LEFT, y, LEFT + plotWidth, y);

            String left = String.valueOf(leftMax * i / 2);
            g.setColor(Palette.of(CellType.HUMAN_CELL));
            g.drawString(left, LEFT - 6 - metrics.stringWidth(left), y + 4);

            g.setColor(Palette.MUTED_TEXT);
            g.drawString(String.valueOf(rightMax * i / 2), LEFT + plotWidth + 6, y + 4);
        }

        g.setColor(Palette.MUTED_TEXT);
        g.drawString("turn " + firstTurn, LEFT, baseline + 15);
        String end = "turn " + (firstTurn + span);
        g.drawString(end, LEFT + plotWidth - metrics.stringWidth(end), baseline + 15);
    }

    private void paintLine(Graphics2D g, Statistics stats, CellType type, int firstTurn, int lastTurn,
                           int span, int max, int plotWidth, int plotHeight) {
        Path2D path = new Path2D.Double();
        for (int turn = firstTurn; turn <= lastTurn; turn++) {
            double x = LEFT + plotWidth * (turn - firstTurn) / (double) span;
            double y = TOP + plotHeight * (1 - stats.countAt(turn, type) / (double) max);
            if (turn == firstTurn) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        g.setColor(Palette.of(type));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
    }

    private void paintLegend(Graphics2D g, Statistics stats) {
        FontMetrics metrics = g.getFontMetrics();
        int x = LEFT;
        CellType[] order = {CellType.HUMAN_CELL, CellType.BACTERIA, CellType.WHITE_BLOOD_CELL};
        for (CellType type : order) {
            String label = type.getDisplayName() + ": " + stats.living(type);
            g.setColor(Palette.of(type));
            g.fillRoundRect(x, 8, 10, 10, 3, 3);
            g.setColor(Palette.TEXT);
            g.drawString(label, x + 15, 17);
            x += 15 + metrics.stringWidth(label) + 22;
        }
    }

    private static int maxInWindow(Statistics stats, int firstTurn, int lastTurn, CellType type) {
        int max = 0;
        for (int turn = firstTurn; turn <= lastTurn; turn++) {
            max = Math.max(max, stats.countAt(turn, type));
        }
        return max;
    }

    /** Rounds up to a readable axis maximum such as 20, 30, 50, 100, 150, 200... */
    private static int niceCeiling(int value) {
        int[] steps = {10, 15, 20, 30, 40, 50, 80};
        for (int magnitude = 1; ; magnitude *= 10) {
            for (int step : steps) {
                if (step * magnitude >= value) {
                    return step * magnitude;
                }
            }
        }
    }
}
