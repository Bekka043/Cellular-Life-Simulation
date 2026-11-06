# Cellular Life Simulation

A turn-based simulation of interactions between bacteria, human cells and white blood cells, written in Java and inspired by Conway's Game of Life. Built as an object-oriented programming project for the Programming 2 course at Sapienza University of Rome.

## How it works

Every turn, each cell's HP changes based on every other living cell in the population. The changes are computed from the same snapshot and applied all at once, so update order never matters.

The effect one cell has on another depends on their types:

| Source ↓ / Target → | White Blood Cell | Human Cell | Bacteria |
|---|---|---|---|
| **White Blood Cell** | 0 | 0 | −3 |
| **Human Cell** | +0.5 | 0 | +1 |
| **Bacteria** | −1 | −2 | −0.5 |

In words: white blood cells attack bacteria, bacteria damage human cells and fight back against white blood cells, human cells support the immune system but also feed bacteria, and bacteria compete with each other for resources.

Each effect is scaled by the source cell's **vitality**, which depends on its age. Vitality follows a parabola that peaks at age 5, so newborn and old cells have a weaker influence than mature ones.

Life cycle rules:

- Every cell starts with 100 HP.
- A cell dies when its HP reaches 0 or when it reaches age 10.
- A bacterium aged 6 or older with more than 70 HP divides into two new bacteria.

## Running it

You need Java 17 or newer.

```bash
javac -d out src/*.java
java -cp out Main
```

You enter the starting number of each cell type and how many turns to run. After that, an interactive menu lets you step through turns, run several turns at once, or add new cells to the population.

## Example

```
Turn 5 - living cells:
  Bacteria         age:  5   HP:   82
  Bacteria         age:  5   HP:   82
  Bacteria         age:  5   HP:   82
  White Blood Cell age:  5   HP:   97
  White Blood Cell age:  5   HP:   97
  White Blood Cell age:  5   HP:   97
  Human Cell       age:  5   HP:   82
  Human Cell       age:  5   HP:   82
  Human Cell       age:  5   HP:   82
  Human Cell       age:  5   HP:   82
---------------------------------------------
  White Blood Cell:     3 living /    3 total
  Human Cell:           4 living /    4 total
  Bacteria:             3 living /    3 total
---------------------------------------------
```

## Project structure

| File | Purpose |
|---|---|
| `Cell.java` | Abstract base class: age, HP, death and reproduction rules |
| `Bacteria.java`, `HumanCell.java`, `WhiteBloodCell.java` | The three cell types |
| `CellType.java` | Enum of cell types, used to index the relationship matrix |
| `Relationships.java` | Interaction matrix and age-based vitality curve |
| `Simulation.java` | Holds the population and advances it turn by turn |
| `Main.java` | Command-line interface |
