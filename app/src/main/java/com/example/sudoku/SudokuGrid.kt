package com.example.sudoku

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.EditText
import android.widget.GridLayout
import model.SudokuBoard
class SudokuGrid @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : GridLayout(context, attrs) {

    private val SIZE = 9
    private val cells = Array(SIZE) { arrayOfNulls<EditText>(SIZE) }
    private lateinit var board: SudokuBoard
    private var onCellSelectedListener: ((row: Int, col: Int) -> Unit)? = null
    private var isNotesMode = false
    private val notes = Array(SIZE) { Array(SIZE) { mutableSetOf<Int>() } }

    private var selectedRow = -1
    private var selectedCol = -1
    private var highlightedDigit = -1

    init {
        rowCount = SIZE
        columnCount = SIZE
        initGrid()
    }

    fun setSudokuBoard(board: SudokuBoard) {
        this.board = board
        for (i in 0 until SIZE) for (j in 0 until SIZE) notes[i][j].clear()
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
        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                val editText = EditText(context).apply {
                    layoutParams = LayoutParams(
                        GridLayout.spec(i, 1f),
                        GridLayout.spec(j, 1f)
                    ).apply {
                        width = 0
                        height = 0
                        setMargins(
                            if (j % 3 == 0) 4 else 1,
                            if (i % 3 == 0) 4 else 1,
                            if (j == 8) 4 else 1,
                            if (i == 8) 4 else 1
                        )
                    }
                    gravity = Gravity.CENTER
                    textSize = 20f
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
                    onCellSelectedListener?.invoke(i, j)
                    updateUI()
                }

                cells[i][j] = editText
                addView(editText)
            }
        }
    }

    fun updateUI() {
        if (!::board.isInitialized) return
        for (i in 0 until SIZE) for (j in 0 until SIZE) updateCellUI(i, j)
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

        if (value != 0) {
            cell.setText(value.toString())
            cell.textSize = 20f
            cell.setTypeface(null, Typeface.BOLD)
            cell.gravity = Gravity.CENTER
            cell.setTextColor(
                when {
                    hasError -> Color.RED
                    isFixed -> Color.parseColor("#07283F")
                    else -> Color.parseColor("#085590")
                }
            )
            if (!isFixed && !hasError && value == solutionValue) board.markAutoFixed(row, col)
        } else if (isNotesMode && notes[row][col].isNotEmpty()) {
            cell.setText(formatNotes3x3(notes[row][col].sorted()))
            cell.textSize = 10f
            cell.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
            cell.setLineSpacing(0f, 0.9f)
            cell.gravity = Gravity.START or Gravity.TOP
            cell.setTextColor(Color.DKGRAY)
        } else {
            cell.setText("")
            cell.gravity = Gravity.CENTER
        }

        when {
            hasError -> cell.setBackgroundResource(R.drawable.cell_background_conflict)
            isSelected -> cell.setBackgroundResource(R.drawable.cell_background_selected)
            highlightedDigit != -1 && value == highlightedDigit -> cell.setBackgroundResource(R.drawable.cell_background_highlight)
            isRelated -> cell.setBackgroundResource(R.drawable.cell_background_related)
            else -> cell.setBackgroundResource(R.drawable.cell_background_normal)
        }
    }

    private fun formatNotes3x3(notesList: List<Int>): String {
        val sb = StringBuilder()
        val arr = CharArray(9) { ' ' }
        for ((idx, n) in notesList.withIndex()) if (idx in 0 until 9) arr[idx] = ('0' + n)
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                sb.append(arr[r * 3 + c])
                if (c < 2) sb.append(' ')
            }
            if (r < 2) sb.append('\n')
        }
        return sb.toString()
    }

    fun setOnCellSelectedListener(listener: (row: Int, col: Int) -> Unit) {
        onCellSelectedListener = listener
    }

    fun setNumber(row: Int, col: Int, number: Int) {
        if (!::board.isInitialized) return
        if (row !in 0..8 || col !in 0..8) return
        if (board.isFixed(row, col)) return

        if (isNotesMode) toggleNote(row, col, number)
        else {
            if (number == 0) {
                board.setCell(row, col, 0)
            } else {
                board.setCell(row, col, number)
            }
            if (number != 0) notes[row][col].clear()
        }
        updateUI()
    }
}
