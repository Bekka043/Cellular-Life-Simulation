/** The kinds of cells that can live in the simulation. */
public enum CellType {
    WHITE_BLOOD_CELL("White Blood Cell"),
    HUMAN_CELL("Human Cell"),
    BACTERIA("Bacteria");

    private final String displayName;

    CellType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
