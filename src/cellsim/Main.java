package cellsim;

import cellsim.ui.Launcher;

/** Entry point. Opens the GUI by default; see --help for the text mode. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Launcher.launch(args);
    }
}
