package com.example.sudoku

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import logic.SudokuGenerator
import model.SudokuBoard
import utils.Difficulty

class MainActivity : AppCompatActivity() {

    private lateinit var sudokuBoard: SudokuBoard
    private lateinit var sudokuGrid: SudokuGrid
    private lateinit var generator: SudokuGenerator

    private var selectedRow = -1
    private var selectedCol = -1

    private lateinit var remainingCountText: TextView
    private lateinit var timerTextView: TextView
    private lateinit var notesModeCheckbox: CheckBox
    private lateinit var digitButtons: List<Button>

    private var startTime: Long = 0L
    private var isTimerRunning = false
    private val handler = Handler(Looper.getMainLooper())

    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = (System.currentTimeMillis() - startTime) / 1000
            val minutes = elapsed / 60
            val seconds = elapsed % 60
            timerTextView.text = String.format("%02d:%02d", minutes, seconds)
            if (isTimerRunning) handler.postDelayed(this, 1000)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sudokuBoard = SudokuBoard()
        generator = SudokuGenerator(sudokuBoard)
        sudokuGrid = findViewById(R.id.sudokuGrid)
        remainingCountText = findViewById(R.id.remainingCount)
        timerTextView = findViewById(R.id.timerText)
        notesModeCheckbox = findViewById(R.id.notesModeCheckbox)

        sudokuGrid.setSudokuBoard(sudokuBoard)

        sudokuGrid.setOnCellSelectedListener { row, col ->
            selectedRow = row
            selectedCol = col
            val value = sudokuBoard.getCell(row, col)
            sudokuGrid.highlightDigit(if (value in 1..9) value else -1)
        }

        notesModeCheckbox.setOnCheckedChangeListener { _, isChecked ->
            sudokuGrid.setNotesMode(isChecked)
            sudokuGrid.updateUI()
        }

        val spinnerDifficulty = findViewById<Spinner>(R.id.difficultySpinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            Difficulty.entries.toTypedArray()
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDifficulty.adapter = adapter

        findViewById<Button>(R.id.generateButton).setOnClickListener {
            val difficulty = spinnerDifficulty.selectedItem as Difficulty
            generator.generate(difficulty)
            sudokuGrid.updateUI()
            updateRemainingCount()
            startTimer()
            timerTextView.visibility = View.VISIBLE
        }

        digitButtons = listOf(
            findViewById<Button>(R.id.btn1),
            findViewById<Button>(R.id.btn2),
            findViewById<Button>(R.id.btn3),
            findViewById<Button>(R.id.btn4),
            findViewById<Button>(R.id.btn5),
            findViewById<Button>(R.id.btn6),
            findViewById<Button>(R.id.btn7),
            findViewById<Button>(R.id.btn8),
            findViewById<Button>(R.id.btn9),
//            findViewById<Button>(R.id.btnErase)
        )

        digitButtons.forEachIndexed { index, button ->
            button.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        handleDigitInput(index)
                        true
                    }
                    MotionEvent.ACTION_UP -> true

                    else -> false
                }
            }
//            button.setOnClickListener {
//                handleDigitInput(index)
//
//            }
        }
    }

    private fun updateRemainingCount() {
        val remaining = (0 until 9).sumOf { i -> (0 until 9).count { j -> sudokuBoard.getCell(i, j) == 0 } }
        remainingCountText.text = "Осталось чисел: $remaining"
        updateInputButtons(remaining == 0, countDigits())
        if (remaining == 0) stopTimer()
    }

    private fun handleDigitInput(index: Int) {
        if (selectedRow !in 0..8 || selectedCol !in 0..8) return

        val notesMode = notesModeCheckbox.isChecked
        if (notesMode) {
            when {
                index in 0..8 -> sudokuGrid.setNumber(selectedRow, selectedCol, index + 1)
                index == 9 -> sudokuGrid.clearNotes(selectedRow, selectedCol)
            }
        } else {
            val value = if (index == 9) 0 else index + 1
            sudokuGrid.setNumber(selectedRow, selectedCol, value)
        }
        val currentValue = sudokuBoard.getCell(selectedRow, selectedCol)
        sudokuGrid.highlightDigit(if (currentValue in 1..9) currentValue else -1)
        updateRemainingCount()
    }

    private fun updateInputButtons(isBoardFull: Boolean, digitCounts: IntArray) {
        digitButtons.forEachIndexed { index, button ->
            val isDigitFilled = digitCounts.getOrNull(index) == 9
            val isEnabled = !isBoardFull && !isDigitFilled
            button.isEnabled = isEnabled
            button.alpha = if (isEnabled) 1f else 0.5f
        }
    }

    private fun countDigits(): IntArray {
        val counts = IntArray(9)
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                val value = sudokuBoard.getCell(i, j)
                if (value in 1..9) counts[value - 1]++
            }
        }
        return counts
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        isTimerRunning = true
        handler.post(timerRunnable)
    }

    private fun stopTimer() {
        isTimerRunning = false
        handler.removeCallbacks(timerRunnable)
    }
}
