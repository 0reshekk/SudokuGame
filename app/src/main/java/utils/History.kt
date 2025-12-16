package utils

import sudoku.SudokuGrid
import model.SudokuBoard

class GameHistory(private val board: SudokuBoard, private val grid: SudokuGrid) {
    private val moves = ArrayDeque<Move>()

    data class Move(
        val row: Int,
        val col: Int,
        val beforeValue: Int,
        val beforeNotes: Set<Int>,
        val afterValue: Int,
        val afterNotes: Set<Int>
    )

    fun record(row: Int, col: Int, beforeValue: Int, beforeNotes: Set<Int>, afterValue: Int, afterNotes: Set<Int>) {
        if (beforeValue == afterValue && beforeNotes == afterNotes) return
        moves.addLast(Move(row, col, beforeValue, beforeNotes, afterValue, afterNotes))
    }

    fun undo(): Boolean {
        while (moves.isNotEmpty()) {
            val move = moves.removeLast()
            val isCorrectAndFixed = board.isFixed(move.row, move.col) && board.getCell(move.row, move.col) == board.getSolution(move.row, move.col)
            if (isCorrectAndFixed) continue
            grid.applyState(move.row, move.col, move.beforeValue, move.beforeNotes)
            return true
        }
        return false
    }

    fun clear() {
        moves.clear()
    }
}