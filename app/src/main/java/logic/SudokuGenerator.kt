package logic

import model.SudokuBoard
import utils.Difficulty
import kotlin.random.Random

class SudokuGenerator(private val board: SudokuBoard) {

    private val solver = SudokuSolver(board)

    fun generate(difficulty: Difficulty) {
        // Шаг 1: Очистить доску
        board.clear()

        // Шаг 2: Заполнить доску полностью
        fillDiagonalBoxes()
        solver.solve()

        // Шаг 3: Удалить клетки в зависимости от сложности
        removeCells(81 - difficulty.clues)
    }

    private fun fillDiagonalBoxes() {
        // Заполнить 3 диагональных 3x3 блока случайными числами (без конфликтов)
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
