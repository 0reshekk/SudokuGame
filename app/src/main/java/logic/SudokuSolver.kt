package logic

import model.SudokuBoard

class SudokuSolver(private val board: SudokuBoard) {

    fun solve(): Boolean {
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.getCell(row, col) == 0) {
                    for (num in 1..board.size) {
                        if (board.isValid(row, col, num)) {
                            board.setCell(row, col, num)
                            if (solve()) return true
                            board.setCell(row, col, 0)
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    fun getSolutionBoard(): Array<IntArray> {
        val solution = Array(board.size) { IntArray(board.size) }
        for (i in 0 until board.size) {
            for (j in 0 until board.size) {
                solution[i][j] = board.getCell(i, j)
            }
        }
        return solution
    }
}
