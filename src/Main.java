import java.util.NoSuchElementException;
import java.util.Scanner;

/** Command-line interface for the cellular life simulation. */
public class Main {
    private static final int MAX_CELLS_TO_LIST = 30;
    private static final String SEPARATOR = "---------------------------------------------";

    public static void main(String[] args) {
        Simulation sim = new Simulation();
        Scanner in = new Scanner(System.in);

        try {
            int bacteria = readInt(in, "Number of bacteria: ", 0);
            int whiteBloodCells = readInt(in, "Number of white blood cells: ", 0);
            int humanCells = readInt(in, "Number of human cells: ", 0);
            addMany(sim, CellType.BACTERIA, bacteria);
            addMany(sim, CellType.WHITE_BLOOD_CELL, whiteBloodCells);
            addMany(sim, CellType.HUMAN_CELL, humanCells);

            sim.run(readInt(in, "Number of turns to run: ", 0));

            while (true) {
                printState(sim);

                if (sim.isExtinct()) {
                    System.out.println("The ecosystem has died out.");
                    break;
                }

                System.out.println("""
                        (1) Next turn
                        (2) Run several turns
                        (3) Add bacteria
                        (4) Add human cell
                        (5) Add white blood cell
                        (6) Exit""");
                int command = readInt(in, "> ", 1);

                switch (command) {
                    case 1 -> sim.step();
                    case 2 -> sim.run(readInt(in, "How many turns: ", 0));
                    case 3 -> sim.add(new Bacteria());
                    case 4 -> sim.add(new HumanCell());
                    case 5 -> sim.add(new WhiteBloodCell());
                    case 6 -> {
                        System.out.println("Thanks for playing!");
                        return;
                    }
                    default -> System.out.println("Unknown option, choose 1-6.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println();
            System.out.println("Input ended, exiting.");
        } finally {
            in.close();
        }
    }

    private static void addMany(Simulation sim, CellType type, int count) {
        for (int i = 0; i < count; i++) {
            sim.add(switch (type) {
                case BACTERIA -> new Bacteria();
                case HUMAN_CELL -> new HumanCell();
                case WHITE_BLOOD_CELL -> new WhiteBloodCell();
            });
        }
    }

    /** Reads an integer >= min, re-prompting on invalid input. */
    private static int readInt(Scanner in, String prompt, int min) {
        while (true) {
            System.out.print(prompt);
            if (!in.hasNext()) {
                throw new NoSuchElementException();
            }
            if (in.hasNextInt()) {
                int value = in.nextInt();
                if (value >= min) {
                    return value;
                }
                System.out.println("Please enter a number >= " + min + ".");
            } else {
                System.out.println("'" + in.next() + "' is not a number, try again.");
            }
        }
    }

    private static void printState(Simulation sim) {
        System.out.println(SEPARATOR);
        System.out.println("Turn " + sim.getTurn() + " - living cells:");
        if (sim.getCells().size() <= MAX_CELLS_TO_LIST) {
            for (Cell cell : sim.getCells()) {
                System.out.println("  " + cell);
            }
        } else {
            System.out.println("  (" + sim.getCells().size() + " cells, too many to list individually)");
        }
        System.out.println(SEPARATOR);
        for (CellType type : CellType.values()) {
            System.out.printf("  %-18s %4d living / %4d total%n",
                    type.getDisplayName() + ":", sim.countLiving(type), sim.countCreated(type));
        }
        System.out.println(SEPARATOR);
    }
}
