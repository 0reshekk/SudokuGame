package model
// Логика доски (хранение, проверка правил)

class SudokuBoard {
    val SIZE = 9
    val board = Array(SIZE) { IntArray(SIZE) }

    fun getCell(row: Int, col: Int): Int {
        return board[row][col]
    }

    fun setCell(row: Int, col: Int, value: Int) {
        board[row][col] = value
    }

    fun isValid(row: Int, col: Int, num: Int): Boolean {
        for (i in 0 until SIZE) {
            if (board[row][i] == num) return false
            if (board[i][col] == num) return false
        }
        val boxRowStart = row - row % 3
        val boxColStart = col - col % 3
        for (r in boxRowStart until boxRowStart + 3) {
            for (c in boxColStart until boxColStart + 3) {
                if (board[r][c] == num) return false
            }
        }
        return true
    }

    fun clear() {
        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                board[i][j] = 0
            }
        }
    }
}
