package sudoku

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.Gravity
import android.widget.EditText
import android.widget.GridLayout
import model.SudokuBoard
import androidx.core.graphics.toColorInt
import com.example.sudoku.R

class SudokuGrid @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : GridLayout(context, attrs) {

    private val size = 9
    private val cells = Array(size) { arrayOfNulls<EditText>(size) }
    private lateinit var board: SudokuBoard
    private var onCellSelectedListener: ((row: Int, col: Int) -> Unit)? = null
    private var isNotesMode = false
    private val notes = Array(size) { Array(size) { mutableSetOf<Int>() } }

    private var selectedRow = -1
    private var selectedCol = -1
    private var highlightedDigit = -1

    init {
        rowCount = size
        columnCount = size
        initGrid()
    }

    fun setSudokuBoard(board: SudokuBoard) {
        this.board = board
        for (i in 0 until size) for (j in 0 until size) notes[i][j].clear()
        highlightedDigit = -1
        updateUI()
    }

    fun setNotesMode(enabled: Boolean) {
        isNotesMode = enabled
        updateUI()
    }

    fun toggleNote(row: Int, col: Int, number: Int) {
        val noteSet = notes[row][col]
        if (number in noteSet) noteSet.remove(number) else noteSet.add(number)
        updateCellUI(row, col)
    }

    fun highlightDigit(digit: Int) {
        highlightedDigit = if (digit in 1..9) digit else -1
        updateUI()
    }

    private fun initGrid() {
        for (i in 0 until size) {
            for (j in 0 until size) {
                val editText = EditText(context).apply {
                    layoutParams = LayoutParams(
                        spec(i, 1f),
                        spec(j, 1f)
                    ).apply {
                        width = 0
                        height = 0
                        val thick = 6
                        val thin = 1
                        setMargins(
                            if (j % 3 == 0) thick else thin,
                            if (i % 3 == 0) thick else thin,
                            if ((j + 1) % 3 == 0) thick else thin,
                            if ((i + 1) % 3 == 0) thick else thin
                        )
                    }
                    gravity = Gravity.CENTER
                    textSize = 20f
                    setPadding(6, 6, 6, 6)
                    includeFontPadding = false
                    setTypeface(null, Typeface.BOLD)
                    setBackgroundResource(R.drawable.cell_background_normal)
                    isCursorVisible = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    isClickable = true
                    isEnabled = true
                }

                editText.setOnClickListener {
                    if (!::board.isInitialized) return@setOnClickListener
                    selectedRow = i
                    selectedCol = j
                    val value = board.getCell(i, j)
                    highlightDigit(if (value in 1..9) value else -1)
                    onCellSelectedListener?.invoke(i, j)
                    updateUI()
                }

                cells[i][j] = editText
                addView(editText)
            }
        }
    }

    fun getNotesSnapshot(): Array<Array<Set<Int>>> {
        return Array(size) { row ->
            Array(size) { col -> notes[row][col].toSet() }
        }
    }

    fun applyNotesSnapshot(notesSnapshot: Array<Array<Set<Int>>>) {
        if (!::board.isInitialized) return
        for (i in 0 until size) {
            for (j in 0 until size) {
                notes[i][j].clear()
                notes[i][j].addAll(notesSnapshot.getOrNull(i)?.getOrNull(j) ?: emptySet())
            }
        }
        updateUI()
    }
    fun applyState(row: Int, col: Int, value: Int, notesSet: Set<Int>) {
        if (!::board.isInitialized) return
        if (row !in 0 until size || col !in 0 until size) return
        if (board.isFixed(row, col)) return

        board.setCell(row, col, value)
        notes[row][col].clear()
        notes[row][col].addAll(notesSet)
        updateCellUI(row, col)
    }

    fun updateUI() {
        if (!::board.isInitialized) return
        for (i in 0 until size) for (j in 0 until size) updateCellUI(i, j)
    }

    private fun updateCellUI(row: Int, col: Int) {
        val cell = cells[row][col] ?: return
        val value = board.getCell(row, col)
        val isFixed = board.isFixed(row, col)
        val solutionValue = board.getSolution(row, col)
        val hasError = value != 0 && solutionValue != 0 && value != solutionValue && !isFixed
        val isSelected = row == selectedRow && col == selectedCol
        val isRelated = selectedRow >= 0 && selectedCol >= 0 && !isSelected &&
                (row == selectedRow || col == selectedCol || (row / 3 == selectedRow / 3 && col / 3 == selectedCol / 3))
        val isHighlighted = highlightedDigit != -1 && (value == highlightedDigit || highlightedDigit in notes[row][col])

        if (value != 0) {
            cell.setText(value.toString())
            cell.textSize = 18f
            cell.setTypeface(null, Typeface.BOLD)
            cell.gravity = Gravity.CENTER
            cell.setTextColor(
                when {
                    hasError -> Color.RED
                    isFixed -> "#07283F".toColorInt()
                    else -> "#085590".toColorInt()
                }
            )
            if (!isFixed && !hasError && value == solutionValue) board.markAutoFixed(row, col)
        } else if (notes[row][col].isNotEmpty()) {
            cell.setText(formatNotes3x3(notes[row][col].sorted()))
            cell.textSize = 11f
            cell.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
            cell.setLineSpacing(0f, 0.9f)
            cell.gravity = Gravity.CENTER
            cell.setTextColor(Color.DKGRAY)
        } else {
            cell.setText("")
            cell.gravity = Gravity.CENTER
        }

        when {
            hasError -> cell.setBackgroundResource(R.drawable.cell_background_conflict)
            isSelected -> cell.setBackgroundResource(R.drawable.cell_background_selected)
            isHighlighted -> cell.setBackgroundResource(R.drawable.cell_background_highlight)
            isRelated -> cell.setBackgroundResource(R.drawable.cell_background_related)
            else -> cell.setBackgroundResource(R.drawable.cell_background_normal)
        }
    }

    private fun formatNotes3x3(notesList: List<Int>): CharSequence {
        val builder = SpannableStringBuilder()
        val arr = CharArray(9) { ' ' }
        for (n in notesList) if (n in 1..9) arr[n - 1] = ('0' + n)
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                val start = builder.length
                val char = arr[r * 3 + c]
                builder.append(char)
                val end = builder.length
                if (highlightedDigit in 1..9 && char.digitToIntOrNull() == highlightedDigit) {
                    builder.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (c < 2) builder.append(' ')
            }
            if (r < 2) builder.append('\n')
        }
        return builder
    }

    fun setOnCellSelectedListener(listener: (row: Int, col: Int) -> Unit) {
        onCellSelectedListener = listener
    }

    fun setNumber(row: Int, col: Int, number: Int) {
        if (!::board.isInitialized) return
        if (row !in 0..8 || col !in 0..8) return
        if (board.isFixed(row, col)) return

        if (isNotesMode) {
            if (number == 0) notes[row][col].clear() else toggleNote(row, col, number)
        } else {
            if (number == 0) {
                board.setCell(row, col, 0)
            } else {
                board.setCell(row, col, number)
                notes[row][col].clear()
                removeNotesForNumber(row, col, number)
            }
            if (row == selectedRow && col == selectedCol) {
                highlightDigit(number)
            }
        }
        updateUI()
    }

    private fun removeNotesForNumber(row: Int, col: Int, number: Int) {
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
    fun clearNotes(row: Int, col: Int) {
        if (row !in 0 until size || col !in 0 until size) return
        notes[row][col].clear()
        updateUI()
    }
    fun getNotes(row: Int, col: Int): Set<Int> {
        if (row !in 0 until size || col !in 0 until size) return emptySet()
        return notes[row][col].toSet()
    }

    fun clearAllNotes() {
        for (i in 0 until size) {
            for (j in 0 until size) {
                notes[i][j].clear()
            }
        }
        updateUI()
    }
}
