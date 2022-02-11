import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class SudokuSolver {
    private static final char ONE = '1';
    private static final char NINE = '9';
    private static final char A = 'A';
    private static final char Z = 'Z';
    private static final int ASCII_OFFSET_A = 65;
    private static final Character EMPTY = '.';
    private static final int gridSize = 9;
    private static final boolean ALLOW_GUESS = false;
    private static Set<Character> allowedCharacters;

    private final List<Character>[][] grid;
    private final int blockSize;
    private final Set<Character>[][] gridPossibilities;
    private final Set<Character>[][] gridPossibilitiesAsRows;
    private final int[][] originalGridRowIndex;
    private final int[][] originalGridColIndex;

    boolean gridChanged = false;

    @SuppressWarnings("unchecked")
    public SudokuSolver(List<Character>[][] problemGrid) {
        blockSize = (int) Math.sqrt(gridSize);
        grid = new ArrayList[gridSize][gridSize];
        gridPossibilities = new TreeSet[gridSize][gridSize];
        int expandedGridSize = (int) (gridSize * Math.sqrt(gridSize));
        gridPossibilitiesAsRows = new TreeSet[expandedGridSize][gridSize];
        originalGridRowIndex = new int[expandedGridSize][gridSize];
        originalGridColIndex = new int[expandedGridSize][gridSize];

        for (int row = 0; row < gridSize; ++row) {
            for (int col = 0; col < gridSize; ++col) {
                grid[row][col] = new ArrayList<>();
                gridPossibilities[row][col] = new TreeSet<>();
                gridPossibilities[row][col].addAll(allowedCharacters);
            }
        }
        loadGridAsRows();

        for (int row = 0; row < gridSize; ++row) {
            for (int col = 0; col < gridSize; ++col) {
                if (!problemGrid[row][col].isEmpty()) {
                    setValueInGrid(row, col, problemGrid[row][col].get(0));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        allowedCharacters = new TreeSet<>();
        int charCount = 0;
        for (char c = ONE; c <= Z && charCount < gridSize; ++c) {
            if (c > NINE && c < A)
                continue;
            allowedCharacters.add(c);
            ++charCount;
        }

        List<Character>[][] problemGrid;
        problemGrid = new ArrayList[gridSize][gridSize];

        System.out.println("Enter the problem with dot notation, row by row, such as ..3..47..");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        for (int row = 0; row < gridSize; ++row) {
            String line = reader.readLine();
            if (line.length() < gridSize) {
                throw new IOException("line length is not " + gridSize + ", is " + line.length());
            }
            for (int col = 0; col < gridSize; ++col) {
                char val = line.charAt(col);
                problemGrid[row][col] = new ArrayList<>();
                if (allowedCharacters.contains(val)) {
                    problemGrid[row][col].add(val);
                } else if (val != EMPTY) {
                    throw new IOException(
                            "Invalid character, must be " + EMPTY + " or one of the following:" + allowedCharacters);
                }
            }
        }

        SudokuSolver solver = new SudokuSolver(problemGrid);
        solver.solve();
    }

    private void printGrid() {
        for (int i = 0; i < gridSize; ++i) {
            for (int j = 0; j < gridSize; ++j) {
                System.out.print(grid[i][j]);
            }
            System.out.println();
        }
    }

    private boolean solve() throws IOException {
        for (int i = 0; i < 1000; ++i) {
            System.out.println("Iteration:" + i);
            gridChanged = false;
            printGridPossibilities();
            System.out.println("scanForPossibilitiesOfLengthOne");
            scanForPossibilitiesOfLengthOne();
            if (gridChanged) {
                System.out.println("changed");
                printGridPossibilities();
                continue;
            }
            System.out.println("findBlockLimit");
            findBlockRestriction();
            if (gridChanged) {
                System.out.println("changed");
                printGridPossibilities();
                continue;
            }
            System.out.println("findClosedSet");
            findClosedSet();
            if (gridChanged) {
                System.out.println("changed");
                printGridPossibilities();
                continue;
            }
            System.out.println("findClosedMatrix");
            findNXWing();
            if (gridChanged) {
                System.out.println("changed");
                printGridPossibilities();
                continue;
            }
            System.out.println("nothing changed in this iteration");
            break;
        }
        if (gridNotSolved()) {
            System.out.println("GRID NOT SOLVED");
            printGridPossibilities();
            if (gridNotSolvable()) {
                System.out.println("GRID became unsolvable");
                return false;
            }
            if (ALLOW_GUESS) {
                System.out.println("ENTERING GUESSWORK");
                return guessAndCheck();
            }
            else {
                return false;
            }
        } else {
            System.out.println("SOLUTION");
            printGrid();
            return true;
        }
    }

    // So we couldn't eliminate enough to solve, let's guess
    private boolean guessAndCheck() throws IOException {
        for (int row = 0; row < gridSize; ++row) {
            for (int col = 0; col < gridSize; ++col) {
                if (gridPossibilities[row][col].size() == 2) {
                    for (Character val : gridPossibilities[row][col]) {
                        SudokuSolver guessSolver = new SudokuSolver(grid);
                        guessSolver.setValueInGrid(row, col, val);
                        if (guessSolver.solve()) {
                            return true;
                        } else {
                            System.out.println("guess failed");
                        }
                    }
                    return false;
                }
            }
        }
        System.out.println("TERRIBLE - couldn't find a cell with only two possibilities, must be really rotten!");
        return false;
    }

    private boolean gridNotSolvable() {
        for (int row = 0; row < gridSize; ++row) {
            for (int col = 0; col < gridSize; ++col) {
                if (grid[row][col].isEmpty() && gridPossibilitiesAsRows[row][col].isEmpty()) {
                    System.out.println("Grid is unsolvable, either a mistake in data entry, or the algorithm, or in guesswork");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean gridNotSolved() {
        for (int i = 0; i < gridSize; ++i) {
            for (int j = 0; j < gridSize; ++j) {
                if (grid[i][j].isEmpty()) {
                    return true;
                }
            }
        }
        // grid is solved
        System.out.println("===SOLUTION===");
        printGrid();
        System.exit(1);
        return false;
    }

    private void printGridPossibilities() {
        System.out.print(" :");
        for (int j = 1; j <= gridSize; ++j) {
            System.out.printf("%10s", j);
        }
        System.out.println();
        for (int i = 0; i < gridSize; ++i) {
            System.out.print((char) (ASCII_OFFSET_A + i) + ":");
            for (int j = 0; j < gridSize; ++j) {
                if (!grid[i][j].isEmpty()) {
                    System.out.printf("%10s", grid[i][j]);
                } else {
                    System.out.printf("%10s", prettyPrint(gridPossibilities[i][j]));
                }
            }
            System.out.println();
        }
    }

    private String prettyPrint(Set<Character> set) {
        StringBuilder sb = new StringBuilder();
        for (Character c : set) {
            sb.append(c);
        }
        return sb.toString();
    }

    private void scanForPossibilitiesOfLengthOne() {
        for (int i = 0; i < gridSize; ++i) {
            for (int j = 0; j < gridSize; ++j) {
                if (gridPossibilities[i][j].size() == 1) {
                    setValueInGrid(i, j, gridPossibilities[i][j].iterator().next());
                    return;
                }
            }
        }
    }

    // Makes other iterations easier with this data structure
    // And since all Character values are references, creation and modification
    // of those values will be in sync with the main grid.
    private void loadGridAsRows() {
        int gridAsRowsRowIndex = 0;
        // Rows
        for (int row = 0; row < gridSize; ++row) {
            for (int col = 0; col < gridSize; ++col) {
                gridPossibilitiesAsRows[gridAsRowsRowIndex][col] = gridPossibilities[row][col];
                originalGridRowIndex[gridAsRowsRowIndex][col] = row;
                originalGridColIndex[gridAsRowsRowIndex][col] = col;
            }
            gridAsRowsRowIndex++;
        }
        // Cols
        for (int col = 0; col < gridSize; ++col) {
            for (int row = 0; row < gridSize; ++row) {
                gridPossibilitiesAsRows[gridAsRowsRowIndex][row] = gridPossibilities[row][col];
                originalGridRowIndex[gridAsRowsRowIndex][row] = row;
                originalGridColIndex[gridAsRowsRowIndex][row] = col;
            }
            gridAsRowsRowIndex++;
        }
        // Blocks
        for (int row = 0; row < gridSize; row += blockSize) {
            for (int col = 0; col < gridSize; col += blockSize) {
                for (int i = 0; i < blockSize; ++i) {
                    for (int j = 0; j < blockSize; ++j) {
                        gridPossibilitiesAsRows[gridAsRowsRowIndex][i * blockSize + j] = gridPossibilities[row + i][col + j];
                        originalGridRowIndex[gridAsRowsRowIndex][i * blockSize + j] = row + i;
                        originalGridColIndex[gridAsRowsRowIndex][i * blockSize + j] = col + j;

                    }
                }
                gridAsRowsRowIndex++;
            }
        }
    }

    // This is not private because we invoke this in the guess and check flow
    void setValueInGrid(int row, int col, Character val) {
        System.out.println("grid value " + val + " set at: " + (char) (row + ASCII_OFFSET_A) + (col + 1));
        grid[row][col].add(val);
        gridPossibilities[row][col].clear();
        for (int i = 0; i < originalGridRowIndex.length; ++i) {
            for (int j = 0; j < gridSize; ++j) {
                if (originalGridRowIndex[i][j] == row && originalGridColIndex[i][j] == col) {
                    for (int k = 0; k < gridSize; ++k) {
                        gridPossibilitiesAsRows[i][k].remove(val);
                    }
                }
            }
        }
        gridChanged = true;
    }

    // In a block, if a number n occurs only within
    // a row or col, n cannot exist elsewhere in that rol,
    // col on the main grid
    private void findBlockRestriction() {
        for (int row = gridSize * 2; row < gridPossibilitiesAsRows.length; ++row) {
            for (char c : allowedCharacters) {
                Set<Integer> blockRowOccurrences = new TreeSet<>();
                Set<Integer> blockColOccurrences = new TreeSet<>();
                for (int col = 0; col < gridPossibilitiesAsRows[row].length; ++col) {
                    if (gridPossibilitiesAsRows[row][col].contains(c)) {
                        blockRowOccurrences.add(originalGridRowIndex[row][col]);
                        blockColOccurrences.add(originalGridColIndex[row][col]);
                    }
                }
                if (blockRowOccurrences.size() == 1) {
                    int blockRow = blockRowOccurrences.iterator().next();
                    for (int col2 = 0; col2 < gridSize; ++col2) {
                        if (!blockColOccurrences.contains(col2) && gridPossibilities[blockRow][col2].remove(c)) {
                            gridChanged = true;
                            System.out.println("grid changed at: " + (char) (blockRow + ASCII_OFFSET_A) + (col2 + 1));

                        }
                    }
                } else if (blockColOccurrences.size() == 1) {
                    int blockCol = blockColOccurrences.iterator().next();
                    for (int row2 = 0; row2 < gridSize; ++row2) {
                        if (!blockRowOccurrences.contains(row2) && gridPossibilities[row2][blockCol].remove(c)) {
                            gridChanged = true;
                            System.out.println("grid changed at: " + (char) (row2 + ASCII_OFFSET_A) + (blockCol + 1));

                        }
                    }

                }
                if (gridChanged) {
                    return;
                }
            }
        }
    }

    // A closed set is a set of n numbers in n cells
    // A closed set eliminates those n numbers from the remaining cells
    // This is the main sudoku logic that solves the majority of problems
    private void findClosedSet() {
        for (int row = 0; row < gridPossibilitiesAsRows.length; ++row) {
            for (int setSize = 2; setSize < gridSize; ++setSize) {
                Set<Integer> cells = new TreeSet<>();
                for (int col = 0; col < gridSize; ++col) {
                    if (!gridPossibilitiesAsRows[row][col].isEmpty() && gridPossibilitiesAsRows[row][col].size() <= setSize) {
                        cells.add(col);
                    }
                }
                if (cells.size() >= setSize) {
                    Subsets subsets = new Subsets(cells, setSize);
                    for (Set<Integer> subset : subsets.getSubSets()) {
                        Set<Character> allPossibilities = new TreeSet<>();
                        for (int index : subset) {
                            allPossibilities.addAll(gridPossibilitiesAsRows[row][index]);
                        }
                        if (allPossibilities.size() == setSize) {
                            // Got a closed loop
                            for (int col_2 = 0; col_2 < gridSize; ++col_2) {
                                if (!subset.contains(col_2)) {
                                    if (gridPossibilitiesAsRows[row][col_2].removeAll(allPossibilities)) {
                                        gridChanged = true;
                                        System.out.println("grid changed at: " + (char) (originalGridRowIndex[row][col_2] + ASCII_OFFSET_A)
                                                + (originalGridColIndex[row][col_2] + 1));
                                    }
                                }
                            }
                            if (gridChanged) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    // It is a generalization of the "X-Wing" logic to beyond 2 occurrences
    // The algorithm is:
    // For a given character c, find its possible col occurrences in every row
    // if those occurrences lie within a nxn square matrix spanning n blocks
    // the character c cannot occur in those cols on the remaining rows
    // Repeat the algorithm in the transpose grid as well
    private void findNXWing() {
        for (int rowStart = 0; rowStart < gridSize * 2; rowStart += gridSize) {
            for (int rectSize = 2; rectSize < gridSize; ++rectSize) {
                for (char c : allowedCharacters) {
                    Map<Integer, Set<Integer>> row2ColsMap = new HashMap<>();
                    for (int row = rowStart; row < rowStart + gridSize; ++row) {
                        for (int col = 0; col < gridSize; ++col) {
                            if (gridPossibilitiesAsRows[row][col].contains(c)) {
                                if (!row2ColsMap.containsKey(row)) {
                                    row2ColsMap.put(row, new TreeSet<>());
                                }
                                row2ColsMap.get(row).add(col);
                            }
                        }
                    }
                    Set<Integer> rowsContainingRecSize = new TreeSet<>();
                    for (Integer i : row2ColsMap.keySet()) {
                        if (row2ColsMap.get(i).size() <= rectSize) {
                            rowsContainingRecSize.add(i);
                        }
                    }
                    if (rowsContainingRecSize.size() >= rectSize) {
                        Subsets subsets = new Subsets(rowsContainingRecSize, rectSize);
                        for (Set<Integer> subset : subsets.getSubSets()) {
                            Set<Integer> colIndices = new TreeSet<>();
                            for (int i : subset) {
                                colIndices.addAll(row2ColsMap.get(i));
                            }
                            if (colIndices.size() == rectSize) {
                                Set<Integer> blocks = new TreeSet<>();
                                for (Integer rectRowIndex : row2ColsMap.keySet()) {
                                    for (Integer rectColIndex : colIndices) {
                                        blocks.add(getBlockNum(rectRowIndex, rectColIndex));
                                    }
                                }
                                if (blocks.size() >= rectSize) {
                                    for (int row = rowStart; row < rowStart + gridSize; ++row) {
                                        if (!subset.contains(row)) {
                                            for (int col : colIndices) {
                                                if (gridPossibilitiesAsRows[row][col].remove(c)) {
                                                    gridChanged = true;
                                                    System.out
                                                            .println("grid changed at: " + (char) (originalGridRowIndex[row][col] + ASCII_OFFSET_A)
                                                                    + (originalGridColIndex[row][col] + 1));
                                                }
                                            }
                                        }
                                    }
                                    if (gridChanged) {
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int getBlockNum(int row, int col) {
        return col / blockSize + (row / blockSize) * blockSize;
    }
}
