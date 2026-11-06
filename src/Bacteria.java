import java.util.List;

/** A bacterium. Damages human cells and divides in two when mature and healthy. */
public class Bacteria extends Cell {
    public static final int REPRODUCTION_AGE = 6;
    public static final int REPRODUCTION_HP = 70;

    public Bacteria() {
        super(CellType.BACTERIA);
    }

    @Override
    public List<Cell> reproduce() {
        if (getAge() >= REPRODUCTION_AGE && getHp() > REPRODUCTION_HP) {
            return List.of(new Bacteria(), new Bacteria());
        }
        return List.of();
    }
}
