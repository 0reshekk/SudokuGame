package model

class SudokuNotes(private val size: Int = 9) {

    private val notes: Array<Array<MutableSet<Int>>> =
        Array(size) { Array(size) { mutableSetOf() } }

    fun toggle(row: Int, col: Int, number: Int) {
        if (!row.isValidIndex() || !col.isValidIndex() || number !in 1..9) return
        val noteSet = notes[row][col]
        if (number in noteSet) noteSet.remove(number) else noteSet.add(number)
    }

    fun clear(row: Int, col: Int) {
        if (!row.isValidIndex() || !col.isValidIndex()) return
        notes[row][col].clear()
    }

    fun clearAll() {
        for (i in 0 until size) for (j in 0 until size) notes[i][j].clear()
    }

    fun set(row: Int, col: Int, values: Set<Int>) {
        if (!row.isValidIndex() || !col.isValidIndex()) return
        notes[row][col].apply {
            clear()
            addAll(values.filter { it in 1..9 })
        }
    }

    fun get(row: Int, col: Int): Set<Int> {
        if (!row.isValidIndex() || !col.isValidIndex()) return emptySet()
        return notes[row][col].toSet()
    }

    fun snapshot(): Array<Array<Set<Int>>> {
        return Array(size) { row ->
            Array(size) { col -> notes[row][col].toSet() }
        }
    }

    fun applySnapshot(notesSnapshot: Array<Array<Set<Int>>>) {
        for (i in 0 until size) for (j in 0 until size) set(i, j, notesSnapshot.getOrNull(i)?.getOrNull(j) ?: emptySet())
    }

    fun removeRelatedNotes(row: Int, col: Int, number: Int) {
        if (!row.isValidIndex() || !col.isValidIndex() || number !in 1..9) return
        for (i in 0 until size) {
            for (j in 0 until size) {
                val sameRow = i == row
                val sameCol = j == col
                val sameBox = i / 3 == row / 3 && j / 3 == col / 3
                if ((sameRow || sameCol || sameBox) && !(i == row && j == col)) {
                    notes[i][j].remove(number)
                }
            }
        }
    }

    private fun Int.isValidIndex(): Boolean = this in 0 until size
}