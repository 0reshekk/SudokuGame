package model

class SudokuBoard {

    val SIZE = 9
    private val board = Array(SIZE) { IntArray(SIZE) }
    private val solutionBoard = Array(SIZE) { IntArray(SIZE) }
    private val fixed = Array(SIZE) { BooleanArray(SIZE) }
    private val autoFixed = Array(SIZE) { BooleanArray(SIZE) }

    fun getCell(row: Int, col: Int): Int = board[row][col]

    fun setCell(row: Int, col: Int, value: Int) {
        board[row][col] = value
        if (value == 0) {
            fixed[row][col] = false
            autoFixed[row][col] = false
        }
    }

    fun isFixed(row: Int, col: Int): Boolean = fixed[row][col] || autoFixed[row][col]

    fun markFixed(row: Int, col: Int) {
        fixed[row][col] = true
    }

    fun markAutoFixed(row: Int, col: Int) {
        autoFixed[row][col] = true
    }

    fun isValid(row: Int, col: Int, num: Int): Boolean {
        for (i in 0 until SIZE) {
            if (i != col && board[row][i] == num) return false
            if (i != row && board[i][col] == num) return false
        }
        val boxRowStart = row - row % 3
        val boxColStart = col - col % 3
        for (r in boxRowStart until boxRowStart + 3) {
            for (c in boxColStart until boxColStart + 3) {
                if (!(r == row && c == col) && board[r][c] == num) return false
            }
        }
        return true
    }

    fun clear() {
        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                board[i][j] = 0
                solutionBoard[i][j] = 0
                fixed[i][j] = false
                autoFixed[i][j] = false
            }
        }
    }

    fun setSolution(solution: Array<IntArray>) {
        for (i in 0 until SIZE) for (j in 0 until SIZE) solutionBoard[i][j] = solution[i][j]
    }

    fun getSolution(row: Int, col: Int): Int = solutionBoard[row][col]
}
