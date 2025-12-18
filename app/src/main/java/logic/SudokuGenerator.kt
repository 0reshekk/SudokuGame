package logic

import model.SudokuBoard
import utils.Difficulty
import kotlin.random.Random

class SudokuGenerator(private val board: SudokuBoard) {

    private val solver = SudokuSolver(board)

    fun generate(difficulty: Difficulty) {
        board.clear()

        fillDiagonalBoxes()
        solver.solve()

        // сохраняем решение
        board.setSolution(solver.getSolutionBoard())
        removeCells(81 - difficulty.clues)

        // помечаем оставшиеся числа как фиксированные
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (board.getCell(r, c) != 0) {
                    board.markFixed(r, c)
                }
            }
        }
    }

    private fun fillDiagonalBoxes() {
        for (i in 0 until 9 step 3) {
            fillBox(i, i)
        }
    }

    private fun fillBox(row: Int, col: Int) {
        val numbers = (1..9).toMutableList()
        for (r in row until row + 3) {
            for (c in col until col + 3) {
                val numIndex = Random.nextInt(numbers.size)
                val num = numbers.removeAt(numIndex)
                board.setCell(r, c, num)
            }
        }
    }

    private fun removeCells(count: Int) {
        var removed = 0
        while (removed < count) {
            val r = Random.nextInt(9)
            val c = Random.nextInt(9)
            if (board.getCell(r, c) != 0) {
                board.setCell(r, c, 0)
                removed++
            }
        }
    }
}
