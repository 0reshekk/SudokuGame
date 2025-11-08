package logic

import model.SudokuBoard

class SudokuSolver(private val board: SudokuBoard) {

    fun solve(): Boolean {
        for (row in 0 until board.SIZE) {
            for (col in 0 until board.SIZE) {
                if (board.getCell(row, col) == 0) {
                    for (num in 1..board.SIZE) {
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
}
