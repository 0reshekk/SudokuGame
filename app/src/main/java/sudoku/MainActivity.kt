package sudoku

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.ToggleButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.sudoku.R
import logic.SudokuGenerator
import model.SudokuBoard
import utils.Difficulty
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var sudokuBoard: SudokuBoard
    private lateinit var sudokuGrid: SudokuGrid
    private lateinit var generator: SudokuGenerator

    private var selectedRow = -1
    private var selectedCol = -1

    private lateinit var remainingCountText: TextView
    private lateinit var timerTextView: TextView
    private lateinit var notesToggle: ToggleButton
    private lateinit var digitButtons: List<Button>
    private lateinit var pauseOverlay: View
    private lateinit var resumeButton: Button
    private lateinit var returnToMenuButton: Button
    private lateinit var winOverlay: View
    private lateinit var winOkButton: Button
    private lateinit var winTimeText: TextView
    private lateinit var winRecordText: TextView
    private lateinit var winLivesText: TextView
    private lateinit var helpButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var pauseButton: ImageButton
    private lateinit var menuContainer: LinearLayout
    private lateinit var gameContainer: View
    private lateinit var startGameButton: Button
    private lateinit var difficultyButton: Button
    private lateinit var menuSettingsButton: Button
    private lateinit var livesTextView: TextView
    private lateinit var loseOverlay: View

    private lateinit var prefs: SharedPreferences
    private var selectedDifficulty: Difficulty = Difficulty.ЛЕГКИЙ
    private var hasReadRules: Boolean = false
    private var livesModeEnabled: Boolean = true
    private var hasChanges: Boolean = false
    private var livesRemaining: Int = 0
    private var gameOver: Boolean = false

    private var startTime: Long = 0L
    private var accumulatedSeconds: Long = 0L
    private var isTimerRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var isPaused: Boolean = false

    private val timerRunnable = object : Runnable {
        override fun run() {
            timerTextView.text = formatTime(getElapsedSeconds())
            if (isTimerRunning) handler.postDelayed(this, 1000)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("sudoku_prefs", MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(
            prefs.getInt(
                "theme_mode",
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
        )
        setContentView(R.layout.activity_main)

        hasReadRules = prefs.getBoolean("help_read", false)
        livesModeEnabled = prefs.getBoolean("lives_mode", true)
        selectedDifficulty = Difficulty.valueOf(
            prefs.getString("selected_difficulty", Difficulty.ЛЕГКИЙ.name) ?: Difficulty.ЛЕГКИЙ.name
        )

        sudokuBoard = SudokuBoard()
        generator = SudokuGenerator(sudokuBoard)
        sudokuGrid = findViewById(R.id.sudokuGrid)
        remainingCountText = findViewById(R.id.remainingCount)
        timerTextView = findViewById(R.id.timerText)
        notesToggle = findViewById(R.id.notesToggle)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        resumeButton = findViewById(R.id.resumeButton)
        returnToMenuButton = findViewById(R.id.returnToMenuButton)
        winOverlay = findViewById(R.id.winOverlay)
        winOkButton = findViewById(R.id.winOkButton)
        winTimeText = findViewById(R.id.winTimeText)
        winRecordText = findViewById(R.id.winRecordText)
        winLivesText = findViewById(R.id.winLivesText)
        helpButton = findViewById(R.id.helpButton)
        settingsButton = findViewById(R.id.settingsButton)
        pauseButton = findViewById(R.id.pauseButton)
        menuContainer = findViewById(R.id.menuContainer)
        gameContainer = findViewById(R.id.gameContainer)
        startGameButton = findViewById(R.id.startGameButton)
        difficultyButton = findViewById(R.id.difficultyButton)
        menuSettingsButton = findViewById(R.id.menuSettingsButton)
        livesTextView = findViewById(R.id.livesText)
        loseOverlay = findViewById(R.id.loseOverlay)

        sudokuGrid.setSudokuBoard(sudokuBoard)

        sudokuGrid.setOnCellSelectedListener { row, col ->
            selectedRow = row
            selectedCol = col
            val value = sudokuBoard.getCell(row, col)
            sudokuGrid.highlightDigit(if (value in 1..9) value else -1)
        }

        notesToggle.setOnCheckedChangeListener { _, isChecked ->
            sudokuGrid.setNotesMode(isChecked)
            sudokuGrid.updateUI()
        }

        findViewById<Button>(R.id.eraseButton).setOnClickListener {
            eraseSelectedCell()
        }

        findViewById<Button>(R.id.undoButton).setOnClickListener {
            clearSelection()
        }

        digitButtons = findViewById<GridLayout>(R.id.numberPad).let { grid ->
            (0 until grid.childCount).map { index -> grid.getChildAt(index) as Button }
        }

        digitButtons.forEachIndexed { index, button ->
            button.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        handleDigitInput(index)
                        true
                    }
                    MotionEvent.ACTION_UP -> true
                    else -> false
                }
            }
        }

        startGameButton.setOnClickListener {
            startGameButton.visibility = View.GONE
            openGameScreen()
            startNewGame()
        }

        difficultyButton.setOnClickListener { showDifficultyDialog() }
        menuSettingsButton.setOnClickListener { showSettingsDialog() }
        updateDifficultyLabel()

        findViewById<ImageButton>(R.id.backToMenuButton).setOnClickListener {
            handleBackNavigation()
        }

        pauseButton.setOnClickListener { togglePause() }
        resumeButton.setOnClickListener { togglePause() }
        returnToMenuButton.setOnClickListener { exitToMenu() }
        winOkButton.setOnClickListener { exitToMenu() }
        settingsButton.setOnClickListener { showSettingsDialog() }
        helpButton.setOnClickListener { showRulesDialog() }
        helpButton.visibility = if (hasReadRules) View.GONE else View.VISIBLE

        updateBestResultsUI()
    }

    private fun openGameScreen() {
        menuContainer.visibility = View.GONE
        gameContainer.visibility = View.VISIBLE
        isPaused = false
        pauseOverlay.visibility = View.GONE
        loseOverlay.visibility = View.GONE
    }

    private fun showMenu() {
        gameContainer.visibility = View.GONE
        menuContainer.visibility = View.VISIBLE
        startGameButton.visibility = View.VISIBLE
        pauseOverlay.visibility = View.GONE
        loseOverlay.visibility = View.GONE
        isPaused = false
    }

    private fun updateDifficultyLabel() {
        difficultyButton.text = "Сложность: ${formatDifficulty(selectedDifficulty)}"
    }

    private fun formatDifficulty(difficulty: Difficulty): String {
        return difficulty.name.lowercase(Locale.getDefault())
            .replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }
    }

    private fun startNewGame() {
        generator.generate(selectedDifficulty)
        sudokuGrid.updateUI()
        sudokuGrid.highlightDigit(-1)
        selectedRow = -1
        selectedCol = -1
        updateRemainingCount()
        startTimer(reset = true)
        hasChanges = false
        pauseButton.setImageResource(android.R.drawable.ic_media_pause)
        gameOver = false
        livesRemaining = if (livesModeEnabled) 3 else 0
        updateLivesUI()
        loseOverlay.visibility = View.GONE
        pauseOverlay.visibility = View.GONE
        winOverlay.visibility = View.GONE
        winRecordText.visibility = View.GONE
        winLivesText.visibility = View.GONE
    }

    private fun eraseSelectedCell() {
        if (selectedRow !in 0..8 || selectedCol !in 0..8) return
        val beforeValue = sudokuBoard.getCell(selectedRow, selectedCol)
        val beforeNotes = sudokuGrid.getNotes(selectedRow, selectedCol)

        sudokuGrid.setNumber(selectedRow, selectedCol, 0)
        sudokuGrid.clearNotes(selectedRow, selectedCol)
        sudokuGrid.highlightDigit(-1)
        updateRemainingCount()
        markIfChanged(beforeValue, beforeNotes)
    }

    private fun clearSelection() {
        selectedRow = -1
        selectedCol = -1
        sudokuGrid.highlightDigit(-1)
        sudokuGrid.updateUI()
    }

    private fun updateRemainingCount() {
        val remaining = (0 until 9).sumOf { i -> (0 until 9).count { j -> sudokuBoard.getCell(i, j) == 0 } }
        remainingCountText.text = "Осталось чисел: $remaining"
        updateInputButtons(remaining == 0, countDigits())
        if (remaining == 0 && !gameOver) handleWin()
    }

    private fun handleDigitInput(index: Int) {
        if (gameOver || selectedRow !in 0..8 || selectedCol !in 0..8) return

        val notesMode = notesToggle.isChecked
        val beforeValue = sudokuBoard.getCell(selectedRow, selectedCol)
        val beforeNotes = sudokuGrid.getNotes(selectedRow, selectedCol)
        if (notesMode) {
            when {
                index in 0..8 -> sudokuGrid.setNumber(selectedRow, selectedCol, index + 1)
                index == 9 -> sudokuGrid.clearNotes(selectedRow, selectedCol)
            }
        } else {
            val value = if (index == 9) 0 else index + 1
            sudokuGrid.setNumber(selectedRow, selectedCol, value)
            val solutionValue = sudokuBoard.getSolution(selectedRow, selectedCol)
            if (value != 0 && solutionValue != 0 && value != solutionValue) {
                recordMistake()
            }
        }
        val currentValue = sudokuBoard.getCell(selectedRow, selectedCol)
        sudokuGrid.highlightDigit(if (currentValue in 1..9) currentValue else -1)
        updateRemainingCount()
        markIfChanged(beforeValue, beforeNotes)
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

    private fun startTimer(reset: Boolean) {
        if (reset) {
            accumulatedSeconds = 0L
        } else {
            accumulatedSeconds = getElapsedSeconds()
        }
        startTime = System.currentTimeMillis()
        isTimerRunning = true
        handler.post(timerRunnable)
    }

    private fun togglePause() {
        if (gameOver) return
        if (!isTimerRunning && !isPaused) return

        if (isPaused) {
            pauseOverlay.visibility = View.GONE
            isPaused = false
            startTimer(reset = false)
            pauseButton.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            pauseTimer()
            pauseOverlay.visibility = View.VISIBLE
            isPaused = true
            pauseButton.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun pauseTimer() {
        if (!isTimerRunning) return
        accumulatedSeconds = getElapsedSeconds()
        isTimerRunning = false
        handler.removeCallbacks(timerRunnable)
    }

    private fun stopTimer() {
        accumulatedSeconds = getElapsedSeconds()
        isTimerRunning = false
        handler.removeCallbacks(timerRunnable)
    }

    private fun getElapsedSeconds(): Long {
        val now = System.currentTimeMillis()
        return if (isTimerRunning) {
            accumulatedSeconds + (now - startTime) / 1000
        } else {
            accumulatedSeconds
        }
    }

    private fun showDifficultyDialog() {
        val items = Difficulty.entries.toTypedArray()
        val titles = items.map { formatDifficulty(it) }.toTypedArray()
        val selectedIndex = items.indexOf(selectedDifficulty)

        AlertDialog.Builder(this)
            .setTitle("Выбор сложности")
            .setSingleChoiceItems(titles, selectedIndex) { dialog, which ->
                selectedDifficulty = items[which]
                prefs.edit().putString("selected_difficulty", selectedDifficulty.name).apply()
                updateDifficultyLabel()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val themeGroup = dialogView.findViewById<RadioGroup>(R.id.themeGroup)
        val themeLight = dialogView.findViewById<RadioButton>(R.id.themeLight)
        val themeDark = dialogView.findViewById<RadioButton>(R.id.themeDark)
        val themeSystem = dialogView.findViewById<RadioButton>(R.id.themeSystem)
        val livesCheckbox = dialogView.findViewById<CheckBox>(R.id.livesModeCheckbox)
        val showRulesButton = dialogView.findViewById<Button>(R.id.showRulesButton)

        when (prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)) {
            AppCompatDelegate.MODE_NIGHT_NO -> themeLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> themeDark.isChecked = true
            else -> themeSystem.isChecked = true
        }

        livesCheckbox.isChecked = livesModeEnabled
        showRulesButton.visibility = if (hasReadRules) View.VISIBLE else View.GONE

        val dialog = AlertDialog.Builder(this)
            .setTitle("Настройки")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val selectedMode = when (themeGroup.checkedRadioButtonId) {
                    R.id.themeLight -> AppCompatDelegate.MODE_NIGHT_NO
                    R.id.themeDark -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                prefs.edit().putInt("theme_mode", selectedMode).apply()
                AppCompatDelegate.setDefaultNightMode(selectedMode)

                livesModeEnabled = livesCheckbox.isChecked
                livesRemaining = if (livesModeEnabled) 3 else 0
                prefs.edit().putBoolean("lives_mode", livesModeEnabled).apply()
                updateLivesUI()
                Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .create()

        showRulesButton.setOnClickListener {
            showRulesDialog()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showRulesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Правила игры")
            .setMessage(
                "Заполните сетку 9x9 цифрами от 1 до 9 так, чтобы каждая цифра встречалась в каждой строке, столбце и каждом блоке 3x3 только один раз. Используйте заметки для черновых чисел и стирайте ошибочные варианты."
            )
            .setPositiveButton("Понятно") { dialog, _ ->
                if (!hasReadRules) {
                    hasReadRules = true
                    prefs.edit().putBoolean("help_read", true).apply()
                    helpButton.visibility = View.GONE
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun handleBackNavigation() {
        if (hasChanges) {
            AlertDialog.Builder(this)
                .setMessage("Вы действительно хотите выйти в главное меню? Несохраненные изменения будут потеряны.")
                .setPositiveButton("Да") { _, _ ->
                    exitToMenu()
                }
                .setNegativeButton("Нет", null)
                .show()
        } else {
            exitToMenu()
        }
    }

    private fun exitToMenu() {
        pauseTimer()
        sudokuGrid.clearAllNotes()
        notesToggle.isChecked = false
        hasChanges = false
        showMenu()
    }

    private fun updateBestResultsUI() {
        val difficultyViews = mapOf(
            Difficulty.ЛЕГКИЙ to findViewById<TextView>(R.id.bestEasy),
            Difficulty.СРЕДНИЙ to findViewById<TextView>(R.id.bestMedium),
            Difficulty.СЛОЖНЫЙ to findViewById<TextView>(R.id.bestHard),
            Difficulty.ЭКСТРИМ to findViewById<TextView>(R.id.bestExtreme)
        )

        difficultyViews.forEach { (difficulty, view) ->
            val key = "best_${difficulty.name}"
            val stored = prefs.getLong(key, -1)
            view.text = if (stored > -1) formatTime(stored) else "—"
        }
    }

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainder = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainder)
    }

    private fun updateLivesUI() {
        if (livesModeEnabled) {
            livesTextView.visibility = View.VISIBLE
            val hearts = if (livesRemaining > 0) "❤".repeat(livesRemaining.coerceAtLeast(0)) else "—"
            livesTextView.text = "Жизни: $hearts"
        } else {
            livesTextView.visibility = View.GONE
        }
    }

    private fun recordMistake() {
        if (!livesModeEnabled || gameOver) return
        livesRemaining--
        updateLivesUI()
        if (livesRemaining <= 0) {
            loseGame()
        }
    }

    private fun loseGame() {
        gameOver = true
        stopTimer()
        pauseOverlay.visibility = View.GONE
        loseOverlay.visibility = View.VISIBLE
        winOverlay.visibility = View.GONE
        hasChanges = false
    }

    private fun handleWin() {
        val elapsedSeconds = getElapsedSeconds()
        gameOver = true
        stopTimer()
        pauseOverlay.visibility = View.GONE
        loseOverlay.visibility = View.GONE

        val bestKey = "best_${selectedDifficulty.name}"
        val previous = prefs.getLong(bestKey, -1)
        val isRecord = previous == -1L || elapsedSeconds < previous
        if (isRecord) {
            prefs.edit().putLong(bestKey, elapsedSeconds).apply()
            updateBestResultsUI()
        }

        winTimeText.text = "ВАШ РЕЗУЛЬТАТ: ${formatTime(elapsedSeconds)}"
        winRecordText.visibility = if (isRecord) View.VISIBLE else View.GONE
        if (livesModeEnabled) {
            val hearts = if (livesRemaining > 0) "❤".repeat(livesRemaining.coerceAtLeast(0)) else "—"
            winLivesText.visibility = View.VISIBLE
            winLivesText.text = "ВАШИ ЖИЗНИ: $hearts"
        } else {
            winLivesText.visibility = View.GONE
        }

        winOverlay.visibility = View.VISIBLE
        hasChanges = false
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
    }

    override fun onBackPressed() {
        if (gameContainer.visibility == View.VISIBLE) {
            handleBackNavigation()
        } else {
            super.onBackPressed()
        }
    }

    private fun markIfChanged(beforeValue: Int, beforeNotes: Set<Int>) {
        val afterValue = sudokuBoard.getCell(selectedRow, selectedCol)
        val afterNotes = sudokuGrid.getNotes(selectedRow, selectedCol)
        if (beforeValue != afterValue || beforeNotes != afterNotes) {
            hasChanges = true
        }
    }
}