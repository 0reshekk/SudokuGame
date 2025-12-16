package state

import utils.Difficulty

/**
 * Полное состояние игры для восстановления.
 */
data class GameState(
    val boardData: Array<IntArray>,
    val solutionData: Array<IntArray>,
    val fixedData: Array<BooleanArray>,
    val autoFixedData: Array<BooleanArray>,
    val notesData: Array<Array<Set<Int>>>,
    val difficulty: Difficulty,
    val livesRemaining: Int,
    val livesModeEnabled: Boolean,
    val elapsedSeconds: Long,
    val gameOver: Boolean
)