package state

import logic.SudokuGenerator
import model.SudokuBoard
import sudoku.SudokuGrid
import utils.Difficulty
import utils.GameHistory

/**
 * Координирует игровую логику и хранит внутреннее состояние текущей партии.
 */
class GameController(
    private val board: SudokuBoard,
    private val grid: SudokuGrid,
    private val generator: SudokuGenerator,
    private val history: GameHistory
) {
    var selectedRow: Int = -1
        private set
    var selectedCol: Int = -1
        private set

    var livesRemaining: Int = 0
        private set
    var livesModeEnabled: Boolean = true
        private set
    var gameOver: Boolean = false
        private set
    var hasChanges: Boolean = false
        private set

    fun startNewGame(difficulty: Difficulty, livesMode: Boolean): GameMetrics {
        generator.generate(difficulty)
        grid.clearAllNotes()
        grid.updateUI()
        grid.highlightDigit(-1)
        selectedRow = -1
        selectedCol = -1
        hasChanges = false
        gameOver = false
        history.clear()
        setLivesMode(livesMode)
        return buildMetrics()
    }

    fun restoreFrom(state: GameState): GameMetrics {
        board.restoreState(
            boardData = state.boardData,
            solutionData = state.solutionData,
            fixedData = state.fixedData,
            autoFixedData = state.autoFixedData
        )
        grid.applyNotesSnapshot(state.notesData)
        grid.highlightDigit(-1)
        selectedRow = -1
        selectedCol = -1
        hasChanges = false
        gameOver = state.gameOver
        livesRemaining = state.livesRemaining
        livesModeEnabled = state.livesModeEnabled
        history.clear()
        grid.updateUI()
        return buildMetrics()
    }

    fun selectCell(row: Int, col: Int) {
        selectedRow = row
        selectedCol = col
        val value = board.getCell(row, col)
        grid.highlightDigit(if (value in 1..9) value else -1)
    }

    fun clearSelection() {
        selectedRow = -1
        selectedCol = -1
        grid.highlightDigit(-1)
        grid.updateUI()
    }

    fun eraseSelectedCell(): GameMetrics? {
        if (!hasSelection() || gameOver) return null
        val beforeValue = board.getCell(selectedRow, selectedCol)
        val beforeNotes = grid.getNotes(selectedRow, selectedCol)

        grid.setNumber(selectedRow, selectedCol, 0)
        grid.clearNotes(selectedRow, selectedCol)

        val afterValue = board.getCell(selectedRow, selectedCol)
        val afterNotes = grid.getNotes(selectedRow, selectedCol)
        history.record(selectedRow, selectedCol, beforeValue, beforeNotes, afterValue, afterNotes)
        markIfChanged(beforeValue, beforeNotes, afterValue, afterNotes)
        return buildMetrics()
    }

    fun handleDigitInput(index: Int, notesMode: Boolean, onMistake: () -> Unit): GameMetrics? {
        if (!hasSelection() || gameOver) return null

        val beforeValue = board.getCell(selectedRow, selectedCol)
        val beforeNotes = grid.getNotes(selectedRow, selectedCol)

        if (notesMode) {
            when (index) {
                in 0..8 -> grid.setNumber(selectedRow, selectedCol, index + 1)
                9 -> grid.clearNotes(selectedRow, selectedCol)
            }
        } else {
            val value = if (index == 9) 0 else index + 1
            grid.setNumber(selectedRow, selectedCol, value)
            val solutionValue = board.getSolution(selectedRow, selectedCol)
            if (value != 0 && solutionValue != 0 && value != solutionValue) {
                onMistake()
            }
        }

        val afterValue = board.getCell(selectedRow, selectedCol)
        val afterNotes = grid.getNotes(selectedRow, selectedCol)
        history.record(selectedRow, selectedCol, beforeValue, beforeNotes, afterValue, afterNotes)
        grid.highlightDigit(if (afterValue in 1..9) afterValue else -1)
        markIfChanged(beforeValue, beforeNotes, afterValue, afterNotes)
        return buildMetrics()
    }

    fun undo(): GameMetrics? {
        val undone = history.undo()
        if (undone) {
            clearSelection()
            hasChanges = true
            return buildMetrics()
        }
        return null
    }

    fun buildGameState(difficulty: Difficulty, elapsedSeconds: Long): GameState {
        return GameState(
            boardData = board.getBoardCopy(),
            solutionData = board.getSolutionCopy(),
            fixedData = board.getFixedCopy(),
            autoFixedData = board.getAutoFixedCopy(),
            notesData = grid.getNotesSnapshot(),
            difficulty = difficulty,
            livesRemaining = livesRemaining,
            livesModeEnabled = livesModeEnabled,
            elapsedSeconds = elapsedSeconds,
            gameOver = gameOver
        )
    }

    fun loseLife(): Boolean {
        if (!livesModeEnabled || gameOver) return false
        livesRemaining--
        return livesRemaining <= 0
    }

    fun setLivesMode(enabled: Boolean) {
        livesModeEnabled = enabled
        livesRemaining = if (enabled) 3 else 0
    }

    fun markGameOver() {
        gameOver = true
        hasChanges = false
    }

    fun hasSelection(): Boolean = selectedRow in 0..8 && selectedCol in 0..8

    private fun buildMetrics(): GameMetrics {
        val remaining = remainingCells()
        return GameMetrics(
            remainingCells = remaining,
            digitCounts = countDigits(),
            isBoardFull = remaining == 0
        )
    }

    private fun remainingCells(): Int =
        (0 until 9).sumOf { i -> (0 until 9).count { j -> board.getCell(i, j) == 0 } }

    private fun countDigits(): IntArray {
        val counts = IntArray(9)
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                val value = board.getCell(i, j)
                if (value in 1..9) counts[value - 1]++
            }
        }
        return counts
    }

    private fun markIfChanged(
        beforeValue: Int,
        beforeNotes: Set<Int>,
        afterValue: Int,
        afterNotes: Set<Int>
    ) {
        if (beforeValue != afterValue || beforeNotes != afterNotes) {
            hasChanges = true
        }
    }
}

data class GameMetrics(
    val remainingCells: Int,
    val digitCounts: IntArray,
    val isBoardFull: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameMetrics

        if (remainingCells != other.remainingCells) return false
        if (isBoardFull != other.isBoardFull) return false
        if (!digitCounts.contentEquals(other.digitCounts)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = remainingCells
        result = 31 * result + isBoardFull.hashCode()
        result = 31 * result + digitCounts.contentHashCode()
        return result
    }
}