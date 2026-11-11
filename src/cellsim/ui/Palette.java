package cellsim.ui;

import cellsim.sim.CellType;

import java.awt.Color;

/** Colours shared by the grid and the chart. */
final class Palette {
    static final Color BACKGROUND = new Color(0x0F1117);
    static final Color EMPTY_TILE = new Color(0x181B24);
    static final Color AXIS = new Color(0x3A4050);
    static final Color TEXT = new Color(0xC9D1D9);
    static final Color MUTED_TEXT = new Color(0x8B949E);

    static final Color HUMAN = new Color(0xE0707A);
    static final Color HUMAN_DAMAGED = new Color(0x4D2A30);
    static final Color BACTERIA = new Color(0xA6D96A);
    static final Color BACTERIA_WEAK = new Color(0x4C6B2F);
    static final Color WHITE_BLOOD_CELL = new Color(0xF2F6FA);
    static final Color WHITE_BLOOD_CELL_RING = new Color(0x58A6FF);

    private Palette() {
    }

    /** The colour that identifies a cell type in legends and chart lines. */
    static Color of(CellType type) {
        return switch (type) {
            case HUMAN_CELL -> HUMAN;
            case BACTERIA -> BACTERIA;
            case WHITE_BLOOD_CELL -> WHITE_BLOOD_CELL_RING;
        };
    }

    /** Linear blend between two colours; t = 0 gives a, t = 1 gives b. */
    static Color blend(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
