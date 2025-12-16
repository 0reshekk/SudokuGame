package sudoku

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
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
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.sudoku.R
import logic.SudokuGenerator
import model.SudokuBoard
import state.GameController
import state.GameMetrics
import state.GameStateManager
import utils.Difficulty
import utils.GameHistory
import utils.GameTimer
import java.util.Locale
import androidx.core.content.edit
import androidx.core.view.isVisible

class MainActivity : AppCompatActivity() {

    private lateinit var sudokuBoard: SudokuBoard
    private lateinit var sudokuGrid: SudokuGrid
    private lateinit var generator: SudokuGenerator
    private lateinit var gameController: GameController

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
    private lateinit var continueGameButton: Button
    private lateinit var difficultyButton: Button
    private lateinit var menuSettingsButton: Button
    private lateinit var livesTextView: TextView
    private lateinit var loseOverlay: View

    private lateinit var prefs: SharedPreferences
    private lateinit var stateManager: GameStateManager
    private lateinit var gameTimer: GameTimer
    private var selectedDifficulty: Difficulty = Difficulty.ЛЕГКИЙ
    private var hasReadRules: Boolean = false
    private var livesModeEnabled: Boolean = true
    private var isPaused: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("sudoku_prefs", MODE_PRIVATE)
        stateManager = GameStateManager(prefs)
        gameTimer = GameTimer { seconds -> timerTextView.text = formatTime(seconds) }

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
        gameController = GameController(
            sudokuBoard,
            sudokuGrid,
            generator,
            GameHistory(sudokuBoard, sudokuGrid)
        )

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
        continueGameButton = findViewById(R.id.continueGameButton)
        difficultyButton = findViewById(R.id.difficultyButton)
        menuSettingsButton = findViewById(R.id.menuSettingsButton)
        livesTextView = findViewById(R.id.livesText)
        loseOverlay = findViewById(R.id.loseOverlay)

        sudokuGrid.setSudokuBoard(sudokuBoard)

        sudokuGrid.setOnCellSelectedListener { row, col ->
            gameController.selectCell(row, col)
        }

        notesToggle.setOnCheckedChangeListener { _, isChecked ->
            sudokuGrid.setNotesMode(isChecked)
            sudokuGrid.updateUI()
        }

        findViewById<Button>(R.id.eraseButton).setOnClickListener {
            gameController.eraseSelectedCell()?.let { metrics ->
                handleGameMetrics(metrics)
                saveGameState()
            }
        }

        findViewById<Button>(R.id.undoButton).setOnClickListener {
            gameController.undo()?.let { metrics ->
                handleGameMetrics(metrics)
                saveGameState()
            }
        }

        digitButtons = findViewById<GridLayout>(R.id.numberPad).let { grid ->
            (0 until grid.childCount).map { index -> grid.getChildAt(index) as Button }
        }

        digitButtons.forEachIndexed { index, button ->
            button.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        gameController.handleDigitInput(index, notesToggle.isChecked, ::onMistake)?.let { metrics ->
                            handleGameMetrics(metrics)
                            saveGameState()
                        }
                        true
                    }

                    MotionEvent.ACTION_UP -> true
                    else -> false
                }
            }
        }

        startGameButton.setOnClickListener {
            startGameButton.visibility = View.GONE
            startNewGame()
        }

        continueGameButton.setOnClickListener { continueSavedGame() }

        difficultyButton.setOnClickListener { showDifficultyDialog() }
        menuSettingsButton.setOnClickListener { showSettingsDialog() }
        updateDifficultyLabel()

        findViewById<ImageButton>(R.id.backToMenuButton).setOnClickListener {
            handleBackNavigation()
        }

        pauseButton.setOnClickListener { togglePause() }
        resumeButton.setOnClickListener { togglePause() }
        returnToMenuButton.setOnClickListener {
            loseOverlay.visibility = View.GONE
            exitToMenu()
        }
        winOkButton.setOnClickListener { exitToMenu() }
        settingsButton.setOnClickListener { showSettingsDialog() }
        helpButton.setOnClickListener { showRulesDialog() }
        helpButton.visibility = if (hasReadRules) View.GONE else View.VISIBLE

        updateBestResultsUI()
        updateContinueButtonState()
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
        updateContinueButtonState()
    }

    @SuppressLint("SetTextI18n")
    private fun updateDifficultyLabel() {
        difficultyButton.text = "Сложность: ${formatDifficulty(selectedDifficulty)}"
    }

    private fun formatDifficulty(difficulty: Difficulty): String {
        return difficulty.name.lowercase(Locale.getDefault())
            .replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }
    }

    private fun startNewGame() {
        openGameScreen()
        val metrics = gameController.startNewGame(selectedDifficulty, livesModeEnabled)
        resetOverlays()
        updateLivesUI()
        handleGameMetrics(metrics)
        startTimer(0L)
        saveGameState()
    }

    private fun continueSavedGame() {
        val saved = stateManager.load()
        if (saved == null) {
            Toast.makeText(this, "Нет сохранённой игры для продолжения", Toast.LENGTH_SHORT).show()
            updateContinueButtonState()
            return
        }
        openGameScreen()
        selectedDifficulty = saved.difficulty
        livesModeEnabled = saved.livesModeEnabled
        updateDifficultyLabel()
        val metrics = gameController.restoreFrom(saved)
        resetOverlays()
        updateLivesUI()
        handleGameMetrics(metrics)
        startTimer(saved.elapsedSeconds)
    }

    @SuppressLint("SetTextI18n")
    private fun handleGameMetrics(metrics: GameMetrics) {
        remainingCountText.text = "Осталось чисел: ${metrics.remainingCells}"
        updateInputButtons(metrics.isBoardFull, metrics.digitCounts)
        if (metrics.isBoardFull && !gameController.gameOver) handleWin()
    }

    private fun updateInputButtons(isBoardFull: Boolean, digitCounts: IntArray) {
        digitButtons.forEachIndexed { index, button ->
            val isDigitFilled = digitCounts.getOrNull(index) == 9
            val isEnabled = !isBoardFull && !isDigitFilled
            button.isEnabled = isEnabled
            button.alpha = if (isEnabled) 1f else 0.5f
        }
    }

    private fun togglePause() {
        if (gameController.gameOver) return
        if (!gameTimer.isActive() && !isPaused) return

        if (isPaused) {
            pauseOverlay.visibility = View.GONE
            isPaused = false
            startTimer(gameTimer.elapsedSeconds())
            pauseButton.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            pauseTimer()
            pauseOverlay.visibility = View.VISIBLE
            isPaused = true
            pauseButton.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun pauseTimer() {
        gameTimer.pause()
    }

    private fun stopTimer() {
        gameTimer.stop()
    }

    private fun startTimer(resetSeconds: Long) {
        gameTimer.start(resetSeconds)
    }

    private fun showDifficultyDialog() {
        val items = Difficulty.entries.toTypedArray()
        val titles = items.map { formatDifficulty(it) }.toTypedArray()
        val selectedIndex = items.indexOf(selectedDifficulty)

        AlertDialog.Builder(this)
            .setTitle("Выбор сложности")
            .setSingleChoiceItems(titles, selectedIndex) { dialog, which ->
                selectedDifficulty = items[which]
                prefs.edit { putString("selected_difficulty", selectedDifficulty.name) }
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
        val livesCheckbox = dialogView.findViewById<CheckBox>(R.id.livesModeCheckbox)
        val showRulesButton = dialogView.findViewById<Button>(R.id.showRulesButton)

        when (prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)) {
            AppCompatDelegate.MODE_NIGHT_NO -> themeLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> themeDark.isChecked = true
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
                prefs.edit { putInt("theme_mode", selectedMode) }
                AppCompatDelegate.setDefaultNightMode(selectedMode)

                livesModeEnabled = livesCheckbox.isChecked
                gameController.setLivesMode(livesModeEnabled)
                prefs.edit { putBoolean("lives_mode", livesModeEnabled) }
                updateLivesUI()
                saveGameState()
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

    @SuppressLint("UseKtx")
    private fun showRulesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Правила игры")
            .setMessage(
                "Заполните сетку 9x9 цифрами от 1 до 9 так, чтобы каждая цифра встречалась в каждой строке, столбце и каждом блоке 3x3 только один раз. Используйте заметки для черновых чисел и стирайте ошибочные варианты."
            )
            .setPositiveButton("Понятно") { dialog, _ ->
                if (!hasReadRules) {
                    hasReadRules = true
                    prefs.edit { putBoolean("help_read", true) }
                    helpButton.visibility = View.GONE
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun handleBackNavigation() {
        if (gameContainer.visibility != View.VISIBLE) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setMessage("Вы действительно хотите выйти в главное меню? Прогресс можно сохранить.")
            .setPositiveButton("Выйти без сохранения") { _, _ ->
                stateManager.clear()
                exitToMenu()
            }
            .setNegativeButton("Отмена", null)
            .setNeutralButton("Сохранить игру") { _, _ ->
                saveGameState()
                exitToMenu()
            }
            .show()
    }

    private fun exitToMenu() {
        pauseTimer()
        sudokuGrid.clearAllNotes()
        notesToggle.isChecked = false
        gameController.clearSelection()
        showMenu()
    }

    private fun updateBestResultsUI() {
        val difficultyViews = mapOf(
            Difficulty.ЛЕГКИЙ to findViewById(R.id.bestEasy),
            Difficulty.СРЕДНИЙ to findViewById(R.id.bestMedium),
            Difficulty.СЛОЖНЫЙ to findViewById(R.id.bestHard),
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

    @SuppressLint("SetTextI18n")
    private fun updateLivesUI() {
        if (gameController.livesModeEnabled) {
            livesTextView.visibility = View.VISIBLE
            val hearts = if (gameController.livesRemaining > 0) "❤".repeat(gameController.livesRemaining.coerceAtLeast(0)) else "—"
            livesTextView.text = "Жизни: $hearts"
        } else {
            livesTextView.visibility = View.GONE
        }
    }

    private fun onMistake() {
        if (gameController.loseLife()) {
            loseGame()
        } else {
            updateLivesUI()
        }
    }

    private fun loseGame() {
        gameController.markGameOver()
        stopTimer()
        pauseOverlay.visibility = View.GONE
        loseOverlay.visibility = View.VISIBLE
        winOverlay.visibility = View.GONE
        stateManager.clear()
        updateContinueButtonState()
    }

    @SuppressLint("SetTextI18n")
    private fun handleWin() {
        val elapsedSeconds = gameTimer.elapsedSeconds()
        gameController.markGameOver()
        stopTimer()
        pauseOverlay.visibility = View.GONE
        loseOverlay.visibility = View.GONE

        val bestKey = "best_${selectedDifficulty.name}"
        val previous = prefs.getLong(bestKey, -1)
        val isRecord = previous == -1L || elapsedSeconds < previous
        if (isRecord) {
            prefs.edit { putLong(bestKey, elapsedSeconds) }
            updateBestResultsUI()
        }

        winTimeText.text = "ВАШ РЕЗУЛЬТАТ: ${formatTime(elapsedSeconds)}"
        winRecordText.visibility = if (isRecord) View.VISIBLE else View.GONE
        if (gameController.livesModeEnabled) {
            val hearts = if (gameController.livesRemaining > 0) "❤".repeat(gameController.livesRemaining.coerceAtLeast(0)) else "—"
            winLivesText.visibility = View.VISIBLE
            winLivesText.text = "ВАШИ ЖИЗНИ: $hearts"
        } else {
            winLivesText.visibility = View.GONE
        }

        winOverlay.visibility = View.VISIBLE
        stateManager.clear()
        updateContinueButtonState()
    }

    override fun onPause() {
        super.onPause()
        pauseTimer()
        saveGameState()
    }

    override fun onBackPressed() {
        if (gameContainer.isVisible) {
            handleBackNavigation()
        } else {
            super.onBackPressed()
        }
    }

    private fun resetOverlays() {
        pauseOverlay.visibility = View.GONE
        loseOverlay.visibility = View.GONE
        winOverlay.visibility = View.GONE
        winRecordText.visibility = View.GONE
        winLivesText.visibility = View.GONE
    }

    private fun saveGameState() {
        if (gameContainer.visibility != View.VISIBLE || gameController.gameOver) {
            stateManager.clear()
            updateContinueButtonState()
            return
        }
        val state = gameController.buildGameState(selectedDifficulty, gameTimer.elapsedSeconds())
        stateManager.save(state)
        updateContinueButtonState()
    }

    private fun updateContinueButtonState() {
        val hasState = stateManager.hasSavedState()
        continueGameButton.isEnabled = hasState
        continueGameButton.alpha = if (hasState) 1f else 0.5f
    }
}