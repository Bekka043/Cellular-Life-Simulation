package cellsim.sim;

/** The kinds of cells that can live in the tissue. */
public enum CellType {
    WHITE_BLOOD_CELL("White blood cells", 'W'),
    HUMAN_CELL("Human cells", '.'),
    BACTERIA("Bacteria", 'B');

    private final String displayName;
    private final char symbol;

    CellType(String displayName, char symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Character used to draw this cell type in the text-mode map. */
    public char getSymbol() {
        return symbol;
    }
}
