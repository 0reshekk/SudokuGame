package state

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import utils.Difficulty
import androidx.core.content.edit

private const val SAVED_GAME_KEY = "saved_game_state"

class GameStateManager(private val prefs: SharedPreferences) {

    fun save(state: GameState) {
        val json = JSONObject().apply {
            put("difficulty", state.difficulty.name)
            put("livesRemaining", state.livesRemaining)
            put("livesModeEnabled", state.livesModeEnabled)
            put("elapsedSeconds", state.elapsedSeconds)
            put("gameOver", state.gameOver)
            put("board", state.boardData.toJson())
            put("solution", state.solutionData.toJson())
            put("fixed", state.fixedData.toJson())
            put("autoFixed", state.autoFixedData.toJson())
            put("notes", state.notesData.toJsonNotes())
        }
        prefs.edit { putString(SAVED_GAME_KEY, json.toString()) }
    }

    fun load(): GameState? {
        val stored = prefs.getString(SAVED_GAME_KEY, null) ?: return null
        return runCatching {
            val json = JSONObject(stored)
            GameState(
                boardData = json.getJSONArray("board").toIntArray2d(),
                solutionData = json.getJSONArray("solution").toIntArray2d(),
                fixedData = json.getJSONArray("fixed").toBooleanArray2d(),
                autoFixedData = json.getJSONArray("autoFixed").toBooleanArray2d(),
                notesData = json.getJSONArray("notes").toNotesArray(),
                difficulty = Difficulty.valueOf(json.getString("difficulty")),
                livesRemaining = json.getInt("livesRemaining"),
                livesModeEnabled = json.getBoolean("livesModeEnabled"),
                elapsedSeconds = json.getLong("elapsedSeconds"),
                gameOver = json.getBoolean("gameOver")
            )
        }.getOrNull()
    }

    fun hasSavedState(): Boolean = load()?.let { !it.gameOver && !it.isBoardFull() } == true

    fun clear() {
        prefs.edit { remove(SAVED_GAME_KEY) }
    }

    private fun GameState.isBoardFull(): Boolean = boardData.all { row -> row.all { it != 0 } }
}

private fun Array<IntArray>.toJson(): JSONArray = JSONArray().apply {
    forEach { row ->
        put(JSONArray().apply { row.forEach { put(it) } })
    }
}

private fun Array<BooleanArray>.toJson(): JSONArray = JSONArray().apply {
    forEach { row ->
        put(JSONArray().apply { row.forEach { put(it) } })
    }
}

private fun JSONArray.toIntArray2d(): Array<IntArray> = Array(length()) { r ->
    val rowArray = getJSONArray(r)
    IntArray(rowArray.length()) { c -> rowArray.getInt(c) }
}

private fun JSONArray.toBooleanArray2d(): Array<BooleanArray> = Array(length()) { r ->
    val rowArray = getJSONArray(r)
    BooleanArray(rowArray.length()) { c -> rowArray.getBoolean(c) }
}

private fun Array<Array<Set<Int>>>.toJsonNotes(): JSONArray = JSONArray().apply {
    forEach { row ->
        put(JSONArray().apply {
            row.forEach { cellNotes ->
                put(JSONArray().apply { cellNotes.sorted().forEach { put(it) } })
            }
        })
    }
}

private fun JSONArray.toNotesArray(): Array<Array<Set<Int>>> = Array(length()) { r ->
    val rowArray = getJSONArray(r)
    Array(rowArray.length()) { c ->
        val cellArray = rowArray.getJSONArray(c)
        mutableSetOf<Int>().apply {
            for (i in 0 until cellArray.length()) add(cellArray.getInt(i))
        }
    }
}