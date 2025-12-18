package model

class SudokuBoard {

    val size = 9
    private val board = Array(size) { IntArray(size) }
    private val solutionBoard = Array(size) { IntArray(size) }
    private val fixed = Array(size) { BooleanArray(size) }
    private val autoFixed = Array(size) { BooleanArray(size) }

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
        for (i in 0 until size) {
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
        for (i in 0 until size) {
            for (j in 0 until size) {
                board[i][j] = 0
                solutionBoard[i][j] = 0
                fixed[i][j] = false
                autoFixed[i][j] = false
            }
        }
    }

    fun setSolution(solution: Array<IntArray>) {
        for (i in 0 until size) for (j in 0 until size) solutionBoard[i][j] = solution[i][j]
    }

    fun getSolution(row: Int, col: Int): Int = solutionBoard[row][col]

    fun getBoardCopy(): Array<IntArray> = Array(size) { row -> board[row].clone() }

    fun getSolutionCopy(): Array<IntArray> = Array(size) { row -> solutionBoard[row].clone() }

    fun getFixedCopy(): Array<BooleanArray> = Array(size) { row -> fixed[row].clone() }

    fun getAutoFixedCopy(): Array<BooleanArray> = Array(size) { row -> autoFixed[row].clone() }

    fun restoreState(
        boardData: Array<IntArray>,
        solutionData: Array<IntArray>,
        fixedData: Array<BooleanArray>,
        autoFixedData: Array<BooleanArray>
    ) {
        for (i in 0 until size) {
            for (j in 0 until size) {
                board[i][j] = boardData.getOrNull(i)?.getOrNull(j) ?: 0
                solutionBoard[i][j] = solutionData.getOrNull(i)?.getOrNull(j) ?: 0
                fixed[i][j] = fixedData.getOrNull(i)?.getOrNull(j) ?: false
                autoFixed[i][j] = autoFixedData.getOrNull(i)?.getOrNull(j) ?: false
            }
        }
    }
}
