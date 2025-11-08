package com.example.sudoku
// Класс-View для отображения сетки Судоку (много TextView и ввод)

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import androidx.core.widget.doAfterTextChanged
import model.SudokuBoard
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.graphics.Typeface
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import com.example.sudoku.R
import androidx.core.widget.addTextChangedListener
import androidx.core.graphics.toColorInt

class SudokuGrid @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : GridLayout(context, attrs) {

    private val SIZE = 9
    private val cells = Array(SIZE) { arrayOfNulls<EditText>(SIZE) }
    private lateinit var board: SudokuBoard
    private var onCellChangedListener: ((row: Int, col: Int, value: Int) -> Unit)? = null
    private var onCellSelectedListener: ((row: Int, col: Int) -> Unit)? = null

    private val textWatchers = Array(SIZE) { arrayOfNulls<TextWatcher>(SIZE) }

    private var selectedRow: Int = -1
    private var selectedCol: Int = -1

    init {
        rowCount = SIZE
        columnCount = SIZE
        initGrid()
    }

    fun setSudokuBoard(board: SudokuBoard) {
        this.board = board
        updateUI()
    }

    private fun initGrid() {
        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                val editText = EditText(context)

                val params = LayoutParams(
                    spec(i, 1),
                    spec(j, 1)
                ).apply {
                    width = 110
                    height = 110

                    val thinMargin = 1
                    val thickMargin = 6

                    leftMargin = if (j % 3 == 0) thickMargin else thinMargin
                    topMargin = if (i % 3 == 0) thickMargin else thinMargin
                    rightMargin = if (j == SIZE - 1) thickMargin else 0
                    bottomMargin = if (i == SIZE - 1) thickMargin else 0
                }

                if (params is MarginLayoutParams) {
                    params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin)
                }

                editText.layoutParams = params

                editText.gravity = Gravity.CENTER
                editText.filters = arrayOf(InputFilter.LengthFilter(1))
                editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                editText.showSoftInputOnFocus = false
                editText.isCursorVisible = false

                editText.setBackgroundColor(Color.WHITE)
                editText.setTextColor(Color.BLACK)
                editText.textSize = 20f
                editText.typeface = Typeface.DEFAULT_BOLD
                editText.imeOptions = EditorInfo.IME_ACTION_DONE
                editText.setPadding(0, 0, 0, 0)

                // ВАЖНО: по клику всегда обновлять подсветку и выбранную ячейку
                editText.setOnClickListener {
                    selectedRow = i
                    selectedCol = j
                    editText.requestFocus()
                    onCellSelectedListener?.invoke(i, j)
                    highlightRelatedCells(i, j)
                }

                editText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        selectedRow = i
                        selectedCol = j
                        highlightRelatedCells(i, j)
                        onCellSelectedListener?.invoke(i, j)
                    } else {
                        clearHighlights()
                    }
                }

                val watcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        editText.removeTextChangedListener(this)

                        val valueStr = s?.toString() ?: ""
                        val value = valueStr.toIntOrNull() ?: 0

                        if (valueStr.isEmpty()) {
                            onCellChangedListener?.invoke(i, j, 0)
                        } else if (value in 1..9) {
                            onCellChangedListener?.invoke(i, j, value)
                        } else {
                            editText.setText("")
                        }

                        editText.addTextChangedListener(this)
                    }
                }

                editText.addTextChangedListener(watcher)
                textWatchers[i][j] = watcher

                cells[i][j] = editText
                addView(editText)
            }
        }
    }

    private fun highlightRelatedCells(row: Int, col: Int) {
        val selectedValue = board.getCell(row, col)
        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                val editText = cells[i][j] ?: continue

                val inRow = i == row
                val inCol = j == col
                val inBox = (i / 3 == row / 3) && (j / 3 == col / 3)

                if (selectedValue == 0) {
                    // Если выбрана пустая клетка, ничего не подсвечиваем
                    editText.setBackgroundColor(Color.WHITE)
                    editText.setTypeface(null, Typeface.NORMAL)
                    continue
                }

                if (i == row && j == col) {
                    editText.setBackgroundColor(Color.parseColor("#ADD8E6")) // Выделенная клетка
                } else if (inRow || inCol || inBox) {
                    editText.setBackgroundColor(Color.parseColor("#E0F0FF")) // Связанные клетки
                } else {
                    editText.setBackgroundColor(Color.WHITE) // Обычный фон
                }

                val currentValue = board.getCell(i, j)
                if (currentValue == selectedValue && selectedValue != 0) {
                    // Жирным выделяем все клетки с таким же числом, кроме выделенной
                    editText.setTypeface(null, if (i == row && j == col) Typeface.BOLD_ITALIC else Typeface.BOLD)
                } else {
                    editText.setTypeface(null, Typeface.NORMAL)
                }

                // Ошибочные числа красным по-прежнему подсвечиваем
                if (currentValue != 0 && !board.isValid(i, j, currentValue)) {
                    editText.setTextColor(Color.RED)
                } else {
                    editText.setTextColor(Color.BLACK)
                }
            }
        }
    }


    fun updateUI() {
        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                val editText = cells[i][j] ?: continue
                val value = board.getCell(i, j)

                editText.removeTextChangedListener(textWatchers[i][j])

                if (value == 0) {
                    editText.setText("")
                    editText.isFocusable = true
                    editText.isClickable = true
                    editText.setTextColor(Color.parseColor("#00008B"))  // тёмно-синий для пустых
                    editText.background = ContextCompat.getDrawable(context, R.drawable.cell_background_normal)
                    editText.typeface = Typeface.DEFAULT_BOLD
                } else {
                    editText.setText(value.toString())
                    editText.isFocusable = true
                    editText.isClickable = true
                    editText.setTextColor(Color.parseColor("#00008B"))  // тёмно-синий для заполненных
                    editText.background = ContextCompat.getDrawable(context, R.drawable.cell_background_fixed)
                    editText.typeface = Typeface.DEFAULT
                }

                // Красным подсвечиваем ошибочные заполненные
                if (value != 0 && !board.isValid(i, j, value)) {
                    editText.setTextColor(Color.parseColor("#8B0000"))  // тёмно-красный для ошибок
                }

                editText.addTextChangedListener(textWatchers[i][j])
            }
        }
        updateHighlights()
    }


    private fun updateHighlights() {
        if (selectedRow == -1 || selectedCol == -1) {
            clearHighlights()
            return
        }
        val editTextSelected = cells[selectedRow][selectedCol] ?: return
        val selectedValue = board.getCell(selectedRow, selectedCol)

        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                val editText = cells[i][j] ?: continue

                val inRow = i == selectedRow
                val inCol = j == selectedCol
                val inBox = (i / 3 == selectedRow / 3) && (j / 3 == selectedCol / 3)

                // Фоновая подсветка для выбранной заполненной ячейки
                val isSelectedCell = (i == selectedRow && j == selectedCol)
                if (selectedValue != 0 && isSelectedCell) {
                    editText.setBackgroundColor(Color.parseColor("#ADD8E6"))
                } else if (selectedValue != 0 && (inRow || inCol || inBox)) {
                    editText.setBackgroundColor(Color.parseColor("#E0F0FF"))
                } else {
                    editText.setBackgroundColor(Color.WHITE)
                }

                // Если выбранная ячейка заполнена, жирным выделяем все такие же числа
                val currentValue = board.getCell(i, j)
                if (selectedValue != 0 && currentValue == selectedValue) {
                    editText.setTypeface(null, Typeface.BOLD)
                } else {
                    editText.setTypeface(null, Typeface.NORMAL)
                }

                // Не подсвечиваем пустые ячейки (цвет текста черный)
                if (currentValue == 0) {
                    editText.setTextColor(Color.BLACK)
                    // Также сделать ячейку активной (можно вводить)
                    editText.isEnabled = true
                }

                // Ошибочные числа красным по-прежнему подсвечиваем, даже если жирный шрифт
                if (currentValue != 0 && !board.isValid(i, j, currentValue)) {
                    editText.setTextColor(Color.RED)
                }
            }
        }
    }

    private fun clearHighlights() {
        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                val editText = cells[i][j] ?: continue
                editText.setBackgroundColor(Color.WHITE)
                editText.setTypeface(null, Typeface.NORMAL)
            }
        }
        selectedRow = -1
        selectedCol = -1
    }

    fun setOnCellChangedListener(listener: (row: Int, col: Int, value: Int) -> Unit) {
        onCellChangedListener = listener
    }

    fun setOnCellSelectedListener(listener: (row: Int, col: Int) -> Unit) {
        onCellSelectedListener = listener
    }
}
