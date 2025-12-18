package sudoku

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var sudokuBoard: SudokuBoard
    private lateinit var sudokuGrid: SudokuGrid
    private lateinit var generator: SudokuGenerator
    private lateinit var gameController: GameController

    private lateinit var remainingCountText: TextView
    private lateinit var timerTextView: TextView
    private lateinit var notesToggle: ImageButton
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
    private lateinit var pauseButton: ImageButton
    private var notesModeEnabled = false
    private lateinit var menuContainer: LinearLayout
    private lateinit var gameContainer: View
    private lateinit var startGameButton: Button
    private lateinit var continueGameButton: Button
    private lateinit var difficultyButton: Button
    private lateinit var menuHelpButton: Button
    private lateinit var menuLivesCheckbox: CheckBox
    private lateinit var livesTextView: TextView
    private lateinit var loseOverlay: View
    private lateinit var heartBeatAnimation: Animation

    private lateinit var prefs: SharedPreferences
    private lateinit var stateManager: GameStateManager
    private lateinit var gameTimer: GameTimer
    private var selectedDifficulty: Difficulty = Difficulty.ЛЕГКИЙ
    private var livesModeEnabled: Boolean = true
    private var isPaused: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("sudoku_prefs", MODE_PRIVATE)
        stateManager = GameStateManager(prefs)
        gameTimer = GameTimer { seconds -> timerTextView.text = formatTime(seconds) }
        setContentView(R.layout.activity_main)

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
        pauseButton = findViewById(R.id.pauseButton)
        menuContainer = findViewById(R.id.menuContainer)
        gameContainer = findViewById(R.id.gameContainer)
        startGameButton = findViewById(R.id.startGameButton)
        continueGameButton = findViewById(R.id.continueGameButton)
        difficultyButton = findViewById(R.id.difficultyButton)
        menuHelpButton = findViewById(R.id.menuHelpButton)
        menuLivesCheckbox = findViewById(R.id.menuLivesCheckbox)
        livesTextView = findViewById(R.id.livesText)
        loseOverlay = findViewById(R.id.loseOverlay)

        sudokuGrid.setSudokuBoard(sudokuBoard)

        sudokuGrid.setOnCellSelectedListener { row, col ->
            gameController.selectCell(row, col)
        }

        notesToggle.setOnClickListener { toggleNotesMode() }
        setNotesMode(false)

        findViewById<ImageButton>(R.id.eraseButton).setOnClickListener {
            gameController.eraseSelectedCell()?.let { metrics ->
                handleGameMetrics(metrics)
                saveGameState()
            }
        }

        findViewById<ImageButton>(R.id.undoButton).setOnClickListener {
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
                        gameController.handleDigitInput(index, notesModeEnabled, ::onMistake)?.let { metrics ->
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
        menuHelpButton.setOnClickListener { showRulesDialog() }
        menuLivesCheckbox.isChecked = livesModeEnabled
        menuLivesCheckbox.setOnCheckedChangeListener { _, isChecked ->
            heartBeatAnimation.reset()
            menuLivesCheckbox.startAnimation(heartBeatAnimation)
            livesModeEnabled = isChecked
            prefs.edit { putBoolean("lives_mode", livesModeEnabled) }
            gameController.setLivesMode(livesModeEnabled)
            updateLivesUI()
            saveGameState()
        }
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
        helpButton.setOnClickListener { showRulesDialog() }

        updateBestResultsUI()
        updateContinueButtonState()

        heartBeatAnimation = AnimationUtils.loadAnimation(this, R.anim.heart_pulse)
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
        menuLivesCheckbox.isChecked = livesModeEnabled
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

        val dialog = AlertDialog.Builder(this)
            .setTitle("Выбор сложности")
            .setSingleChoiceItems(titles, selectedIndex) { dialog, which ->
                selectedDifficulty = items[which]
                prefs.edit { putString("selected_difficulty", selectedDifficulty.name) }
                updateDifficultyLabel()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()

        styleDialogButtons(dialog)
    }


    @SuppressLint("UseKtx")
    private fun showRulesDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_rules, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Правила игры")
            .setView(view)
            .setPositiveButton("Понятно") { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        styleDialogButtons(dialog)
    }

    private fun handleBackNavigation() {
        if (gameContainer.visibility != View.VISIBLE) {
            finish()
            return
        }
        val dialog = AlertDialog.Builder(this)
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

        styleDialogButtons(dialog)
    }
    private fun exitToMenu() {
        pauseTimer()
        sudokuGrid.clearAllNotes()
        setNotesMode(false)
        gameController.clearSelection()
        showMenu()
    }
    private fun toggleNotesMode() {
        setNotesMode(!notesModeEnabled)
    }

    private fun setNotesMode(enabled: Boolean) {
        notesModeEnabled = enabled
        notesToggle.isSelected = enabled
        notesToggle.imageAlpha = if (enabled) 255 else 120
        sudokuGrid.setNotesMode(enabled)
        sudokuGrid.updateUI()
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
    private fun styleDialogButtons(dialog: AlertDialog) {
        val margin = 8.dpToPx()
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(margin, margin / 2, margin, margin / 2) }
        val paddingHorizontal = 12.dpToPx()
        val paddingVertical = 8.dpToPx()
        val textColor = ContextCompat.getColor(this, android.R.color.white)

        listOf(
            dialog.getButton(AlertDialog.BUTTON_POSITIVE),
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE),
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
        ).forEach { button ->
            button?.let {
                it.layoutParams = params
                it.setBackgroundResource(R.drawable.menu_button_background)
                it.setTextColor(textColor)
                it.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
                it.isAllCaps = false
            }
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).roundToInt()
}