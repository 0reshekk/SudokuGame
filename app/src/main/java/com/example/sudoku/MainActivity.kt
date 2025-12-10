package com.example.sudoku

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
        }

        notesModeCheckbox.setOnCheckedChangeListener { _, isChecked ->
            sudokuGrid.setNotesMode(isChecked)
            sudokuGrid.updateUI()
        }

        val spinnerDifficulty = findViewById<Spinner>(R.id.difficultySpinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, Difficulty.values())
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

        val digitButtons = listOf(
            findViewById<Button>(R.id.btn1),
            findViewById<Button>(R.id.btn2),
            findViewById<Button>(R.id.btn3),
            findViewById<Button>(R.id.btn4),
            findViewById<Button>(R.id.btn5),
            findViewById<Button>(R.id.btn6),
            findViewById<Button>(R.id.btn7),
            findViewById<Button>(R.id.btn8),
            findViewById<Button>(R.id.btn9),
            findViewById<Button>(R.id.btnErase)
        )

        digitButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (selectedRow !in 0..8 || selectedCol !in 0..8) return@setOnClickListener

                val notesMode = notesModeCheckbox.isChecked
                if (notesMode) {
                    if (index in 0..8) sudokuGrid.setNumber(selectedRow, selectedCol, index + 1)
                } else {
                    val value = if (index == 9) 0 else index + 1
                    sudokuGrid.setNumber(selectedRow, selectedCol, value)
                }
                updateRemainingCount()
            }
        }
    }

    private fun updateRemainingCount() {
        val remaining = (0 until 9).sumOf { i -> (0 until 9).count { j -> sudokuBoard.getCell(i, j) == 0 } }
        remainingCountText.text = "Осталось чисел: $remaining"
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
