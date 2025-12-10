package model

/**
 * Класс для хранения и управления заметками на судоку доске.
 */
class SudokuNotes(private val size: Int = 9) {

    enum class Source { USER, AUTO }

    private val notes = Array(size) { Array(size) { mutableMapOf<Int, Source>() } }

    /** Получить все числа для заметок в ячейке */
    fun getNotes(row: Int, col: Int): Set<Int> = notes[row][col].keys.toSet()

    /** Проверка наличия заметки */
    fun hasNote(row: Int, col: Int, number: Int) = number in notes[row][col]

    /** Переключение заметки USER */
    fun toggleNote(row: Int, col: Int, number: Int) {
        val cellNotes = notes[row][col]
        if (cellNotes.containsKey(number) && cellNotes[number] == Source.USER) {
            cellNotes.remove(number)
        } else {
            cellNotes[number] = Source.USER
        }
    }

    /** Добавить AUTO заметку */
    fun addAutoNote(row: Int, col: Int, number: Int) {
        notes[row][col][number] = Source.AUTO
    }

    /** Удалить конкретную заметку */
    fun removeNote(row: Int, col: Int, number: Int) {
        notes[row][col].remove(number)
    }

    /** Очистить все заметки в ячейке */
    fun clearCell(row: Int, col: Int) {
        notes[row][col].clear()
    }

    /** Очистить только AUTO заметки в ячейке */
    fun clearAutoNotes(row: Int, col: Int) {
        notes[row][col].entries.removeIf { it.value == Source.AUTO }
    }

    /** Удаляем AUTO заметки, которые конфликтуют с введённым значением в row,col */
    fun removeAutoNotesConflictingWith(value: Int, row: Int, col: Int) {
        for (i in 0 until size) {
            // в строке
            notes[row][i].entries.removeIf { it.key == value && it.value == Source.AUTO && i != col }
            // в столбце
            notes[i][col].entries.removeIf { it.key == value && it.value == Source.AUTO && i != row }
        }
        // в блоке 3x3
        val boxRowStart = row - row % 3
        val boxColStart = col - col % 3
        for (r in boxRowStart until boxRowStart + 3) {
            for (c in boxColStart until boxColStart + 3) {
                if (r == row && c == col) continue
                notes[r][c].entries.removeIf { it.key == value && it.value == Source.AUTO }
            }
        }
    }

    /** Проверка есть ли в ячейке заметки */
    fun hasNotes(row: Int, col: Int) = notes[row][col].isNotEmpty()

    /** Форматирование для UI в виде 3x3 */
    fun formatForDisplay(row: Int, col: Int): String {
        val notesList = notes[row][col].keys.sorted()
        val sb = StringBuilder()
        val arr = CharArray(9) { ' ' }
        for ((idx, n) in notesList.withIndex()) if (idx in 0 until 9) arr[idx] = ('0' + n)
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                val ch = arr[r * 3 + c]
                sb.append(if (ch != ' ') ch else ' ')
                if (c < 2) sb.append(' ')
            }
            if (r < 2) sb.append('\n')
        }
        return sb.toString()
    }
}
