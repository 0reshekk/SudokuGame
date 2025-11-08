package com.example.sudoku

import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.sudoku.R
import logic.SudokuGenerator
import model.SudokuBoard
import utils.Difficulty
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import com.example.sudoku.SudokuGrid

class MainActivity : AppCompatActivity() {

    private lateinit var sudokuBoard: SudokuBoard
    private lateinit var sudokuGrid: SudokuGrid
    private lateinit var generator: SudokuGenerator

    private var selectedRow = -1
    private var selectedCol = -1

    private lateinit var remainingCountText: TextView
    private lateinit var timerTextView: TextView

    private var startTime: Long = 0L
    private var isTimerRunning = false
    private val handler = Handler(Looper.getMainLooper())

    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = (System.currentTimeMillis() - startTime) / 1000
            val minutes = elapsed / 60
            val seconds = elapsed % 60
            timerTextView.text = String.format("%02d:%02d", minutes, seconds)
            if (isTimerRunning) {
                handler.postDelayed(this, 1000)
            }
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

        sudokuGrid.setSudokuBoard(sudokuBoard)

        // Отключаем авто ввод (будет только ввод через панель цифр)
        sudokuGrid.setOnCellChangedListener { _, _, _ -> }

        sudokuGrid.setOnCellSelectedListener { row, col ->
            selectedRow = row
            selectedCol = col
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
                if (selectedRow in 0..8 && selectedCol in 0..8) {
                    val value = if (index == 9) 0 else index + 1
                    if (value == 0 || sudokuBoard.isValid(selectedRow, selectedCol, value)) {
                        sudokuBoard.setCell(selectedRow, selectedCol, value)
                    } else {
                        Toast.makeText(this, "Неверное значение", Toast.LENGTH_SHORT).show()
                    }
                    sudokuGrid.updateUI()
                    updateRemainingCount()
                    updateDigitButtonsTransparency()
                    if (isGameComplete()) {
                        stopTimer()
                        showCompletionMessage((spinnerDifficulty.selectedItem as Difficulty).name)
                    }
                }
            }
        }

    }

    private fun updateDigitButtonsTransparency() {
        val digitCountInBlocks = Array(9) { IntArray(9) { 0 } } // digitCountInBlocks[digit-1][blockIndex]

        // Посчитать количество каждой цифры в каждом блоке 3x3
        for (digit in 1..9) {
            for (blockRow in 0 until 3) {
                for (blockCol in 0 until 3) {
                    var count = 0
                    for (i in blockRow * 3 until blockRow * 3 + 3) {
                        for (j in blockCol * 3 until blockCol * 3 + 3) {
                            if (sudokuBoard.getCell(i, j) == digit) {
                                count++
                            }
                        }
                    }
                    digitCountInBlocks[digit - 1][blockRow * 3 + blockCol] = count
                }
            }
        }

        // Проверить, есть ли цифр с цифрой 'digit' в каждом блоке (т.е. каждая цифра должна быть минимум одна в каждом блоке)
        for (digit in 1..9) {
            val button = findViewById<Button>(resources.getIdentifier("btn$digit", "id", packageName))
            val allBlocksHaveDigit = digitCountInBlocks[digit - 1].all { it > 0 }
            button.alpha = if (allBlocksHaveDigit) 0.3f else 1.0f // полупрозрачная, если цифра расставлена во всех блоках
            button.isEnabled = !allBlocksHaveDigit
        }
    }


    private fun updateRemainingCount() {
        val remaining = (0 until 9).sumBy { i ->
            (0 until 9).count { j -> sudokuBoard.getCell(i, j) == 0 }
        }
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

    private fun isGameComplete(): Boolean {
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                val valCell = sudokuBoard.getCell(i, j)
                if (valCell == 0 || !sudokuBoard.isValid(i, j, valCell)) return false
            }
        }
        return true
    }

    private fun showCompletionMessage(difficultyName: String) {
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        val minutes = elapsed / 60
        val seconds = elapsed % 60
        timerTextView.text = "Вы решили судоку на уровне \"$difficultyName\" за: %02d:%02d!".format(minutes, seconds)
    }
}