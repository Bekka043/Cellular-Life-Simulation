package cellsim.ui;

import cellsim.sim.Scenario;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Dimension;
import java.util.Random;

/** Decides between the GUI and the text mode based on the command line. */
public final class Launcher {
    private static final int DEFAULT_TURNS = 300;

    private Launcher() {
    }

    public static void launch(String[] args) {
        if (args.length == 0) {
            SwingUtilities.invokeLater(Launcher::openWindow);
            return;
        }
        switch (args[0]) {
            case "--cli" -> runConsole(args);
            case "--list" -> listScenarios();
            default -> printHelp();
        }
    }

    private static void openWindow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // The default look and feel works fine too.
        }
        JFrame frame = new JFrame("Cellular Life Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new SimulationPanel());
        frame.pack();
        frame.setMinimumSize(new Dimension(820, 560));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void runConsole(String[] args) {
        try {
            Scenario scenario = args.length > 1 ? Scenario.find(args[1]) : Scenario.PRESETS.get(0);
            if (scenario == null) {
                System.out.println("Unknown scenario '" + args[1] + "'.");
                listScenarios();
                return;
            }
            int turns = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_TURNS;
            long seed = args.length > 3 ? Long.parseLong(args[3]) : new Random().nextInt(100_000);
            if (turns < 0) {
                throw new NumberFormatException();
            }
            ConsoleRunner.run(scenario, turns, seed);
        } catch (NumberFormatException e) {
            System.out.println("Turns and seed must be whole numbers, and turns can't be negative.");
            printHelp();
        }
    }

    private static void listScenarios() {
        System.out.println("Scenarios:");
        for (int i = 0; i < Scenario.PRESETS.size(); i++) {
            Scenario scenario = Scenario.PRESETS.get(i);
            System.out.printf("  %d. %-26s %s%n", i + 1, scenario.name(), scenario.description());
        }
    }

    private static void printHelp() {
        System.out.println("""
                Cellular Life Simulation

                Usage:
                  java -cp out cellsim.Main                                open the GUI
                  java -cp out cellsim.Main --cli [scenario] [turns] [seed]  run in the terminal
                  java -cp out cellsim.Main --list                         list the scenarios

                The scenario can be given by number or by name in quotes.""");
    }
}
