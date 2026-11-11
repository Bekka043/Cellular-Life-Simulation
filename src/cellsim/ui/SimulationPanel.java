package cellsim.ui;

import cellsim.sim.CellType;
import cellsim.sim.Scenario;
import cellsim.sim.Simulation;
import cellsim.sim.Statistics;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Random;

/** The whole user interface: grid, population chart and the control sidebar. */
public final class SimulationPanel extends JPanel {
    private static final int SIDEBAR_WIDTH = 250;
    private static final int FRAME_DELAY_MS = 33;

    private Simulation simulation;
    private final GridView gridView;
    private final PopulationChart chart;
    private final Timer timer;

    private final JComboBox<Scenario> scenarioBox = new JComboBox<>(Scenario.PRESETS.toArray(new Scenario[0]));
    private final JTextArea scenarioDescription = new JTextArea();
    private final JTextField seedField = new JTextField();
    private final JButton playButton = new JButton("Play");
    private final JSlider speedSlider = new JSlider(1, 60, 12);
    private final JLabel speedLabel = new JLabel();
    private final JSlider immuneSlider = new JSlider(0, 200, 100);
    private final JLabel immuneLabel = new JLabel();
    private final JLabel turnLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JLabel[] bornDiedLabels = new JLabel[CellType.values().length];

    /** Fractional turns carried over between timer ticks, so slow speeds work too. */
    private double pendingTurns;

    public SimulationPanel() {
        super(new BorderLayout());
        simulation = new Simulation(Scenario.PRESETS.get(0), new Random().nextInt(100_000));
        gridView = new GridView(simulation);
        chart = new PopulationChart(simulation);
        gridView.setOnEdit(this::refresh);

        JPanel view = new JPanel(new BorderLayout());
        view.add(gridView, BorderLayout.CENTER);
        view.add(chart, BorderLayout.SOUTH);
        add(view, BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);

        timer = new Timer(FRAME_DELAY_MS, e -> tick());
        loadSimulation(simulation);
    }

    // ---------------------------------------------------------------- layout

    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Scenario
        sidebar.add(heading("Scenario"));
        sidebar.add(row(scenarioBox));
        scenarioDescription.setLineWrap(true);
        scenarioDescription.setWrapStyleWord(true);
        scenarioDescription.setEditable(false);
        scenarioDescription.setFocusable(false);
        scenarioDescription.setOpaque(false);
        scenarioDescription.setFont(scenarioDescription.getFont().deriveFont(Font.PLAIN, 12f));
        scenarioDescription.setRows(3);
        sidebar.add(row(scenarioDescription));
        scenarioBox.addActionListener(e -> restart(randomSeed()));

        JPanel seedRow = new JPanel(new BorderLayout(6, 0));
        seedRow.add(new JLabel("Seed"), BorderLayout.WEST);
        seedRow.add(seedField, BorderLayout.CENTER);
        sidebar.add(row(seedRow));
        seedField.setToolTipText("The same scenario and seed always replay the same run");
        seedField.addActionListener(e -> restart(seedFromField()));

        JButton restartButton = new JButton("Restart");
        restartButton.setToolTipText("Replay this run from the start with the same seed");
        restartButton.addActionListener(e -> restart(seedFromField()));
        JButton newRunButton = new JButton("New run");
        newRunButton.setToolTipText("Start again with a new random seed");
        newRunButton.addActionListener(e -> restart(randomSeed()));
        sidebar.add(row(pair(restartButton, newRunButton)));

        // Playback
        sidebar.add(heading("Playback"));
        JButton stepButton = new JButton("Step");
        playButton.addActionListener(e -> setPlaying(!timer.isRunning()));
        stepButton.addActionListener(e -> {
            setPlaying(false);
            simulation.step();
            refresh();
        });
        sidebar.add(row(pair(playButton, stepButton)));
        sidebar.add(row(speedLabel));
        sidebar.add(row(speedSlider));
        speedSlider.addChangeListener(e -> updateSliderLabels());

        // Immune system
        sidebar.add(heading("Immune system"));
        sidebar.add(row(immuneLabel));
        sidebar.add(row(immuneSlider));
        immuneSlider.setToolTipText("How readily tissue under attack calls in white blood cells");
        immuneSlider.addChangeListener(e -> {
            simulation.setImmuneStrength(immuneSlider.getValue() / 100.0);
            updateSliderLabels();
        });

        // Paint tools
        sidebar.add(heading("Click or drag on the grid to"));
        ButtonGroup tools = new ButtonGroup();
        JPanel toolGrid = new JPanel(new GridLayout(2, 2, 6, 6));
        for (GridView.Tool tool : GridView.Tool.values()) {
            JToggleButton button = new JToggleButton(tool == GridView.Tool.ERASE ? "Erase" : "Add " + shortName(tool));
            button.setToolTipText(tool.label());
            button.setSelected(tool == GridView.Tool.BACTERIA);
            button.addActionListener(e -> gridView.setTool(tool));
            tools.add(button);
            toolGrid.add(button);
        }
        sidebar.add(row(toolGrid));

        // Statistics
        sidebar.add(heading("Statistics"));
        turnLabel.setFont(turnLabel.getFont().deriveFont(Font.BOLD, 14f));
        sidebar.add(row(turnLabel));
        sidebar.add(row(statusLabel));
        sidebar.add(Box.createVerticalStrut(6));
        for (CellType type : new CellType[] {CellType.HUMAN_CELL, CellType.BACTERIA, CellType.WHITE_BLOOD_CELL}) {
            JLabel label = new JLabel();
            label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
            bornDiedLabels[type.ordinal()] = label;
            sidebar.add(row(label));
        }

        sidebar.add(Box.createVerticalGlue());
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 10));
        return sidebar;
    }

    private static String shortName(GridView.Tool tool) {
        return switch (tool) {
            case BACTERIA -> "bacteria";
            case WHITE_BLOOD_CELL -> "WBC";
            case HUMAN_CELL -> "tissue";
            case ERASE -> "";
        };
    }

    private static JComponent heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setBorder(BorderFactory.createEmptyBorder(14, 0, 4, 0));
        return row(label);
    }

    /** Makes a component fill the sidebar's width without stretching vertically. */
    private static JComponent row(JComponent component) {
        JPanel row = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.add(component, BorderLayout.CENTER);
        row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private static JComponent pair(JComponent left, JComponent right) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 6, 0));
        panel.add(left);
        panel.add(right);
        return panel;
    }

    // ------------------------------------------------------------ simulation

    private void restart(long seed) {
        setPlaying(false);
        loadSimulation(new Simulation((Scenario) scenarioBox.getSelectedItem(), seed));
    }

    private void loadSimulation(Simulation newSimulation) {
        simulation = newSimulation;
        pendingTurns = 0;
        gridView.setSimulation(simulation);
        chart.setSimulation(simulation);
        scenarioDescription.setText(simulation.getScenario().description());
        seedField.setText(String.valueOf(simulation.getSeed()));
        immuneSlider.setValue((int) Math.round(simulation.getImmuneStrength() * 100));
        updateSliderLabels();
        refresh();
    }

    private void tick() {
        pendingTurns += speedSlider.getValue() * FRAME_DELAY_MS / 1000.0;
        boolean stepped = false;
        while (pendingTurns >= 1) {
            simulation.step();
            pendingTurns--;
            stepped = true;
        }
        if (stepped) {
            refresh();
        }
    }

    private void setPlaying(boolean playing) {
        if (playing) {
            timer.start();
        } else {
            timer.stop();
        }
        playButton.setText(playing ? "Pause" : "Play");
    }

    private long seedFromField() {
        try {
            return Long.parseLong(seedField.getText().trim());
        } catch (NumberFormatException e) {
            // Any text works as a seed: the same text always gives the same run.
            return seedField.getText().trim().hashCode();
        }
    }

    private static long randomSeed() {
        return new Random().nextInt(100_000);
    }

    // --------------------------------------------------------------- display

    private void updateSliderLabels() {
        speedLabel.setText("Speed: " + speedSlider.getValue() + " turns per second");
        immuneLabel.setText("Strength: " + immuneSlider.getValue() + "%");
    }

    private void refresh() {
        Statistics stats = simulation.getStatistics();
        turnLabel.setText("Turn " + simulation.getTurn());
        statusLabel.setText(describeStatus(stats));
        for (CellType type : CellType.values()) {
            bornDiedLabels[type.ordinal()].setText(String.format("%s: %d alive, %d died",
                    type.getDisplayName(), stats.living(type), stats.died(type)));
        }
        gridView.repaint();
        chart.repaint();
    }

    static String describeStatus(Statistics stats) {
        int bacteria = stats.living(CellType.BACTERIA);
        int tissue = stats.living(CellType.HUMAN_CELL);
        if (tissue == 0) {
            return "Tissue destroyed";
        }
        if (bacteria > 0) {
            return "Infection active";
        }
        if (stats.peak(CellType.BACTERIA) > 0) {
            return tissue < 0.9 * stats.peak(CellType.HUMAN_CELL) ? "Infection cleared, tissue regrowing" : "Infection cleared";
        }
        return "Healthy";
    }
}
