package com.csprout.sobercounter

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import kotlin.random.Random

enum class GoalUnit(val label: String) {
    DAYS("Days"), WEEKS("Weeks"), MONTHS("Months"), YEARS("Years")
}

data class Goal(val id: Int, val amount: Int, val unit: GoalUnit) {
    val label: String
        get() = "$amount ${if (amount == 1) unit.label.dropLast(1) else unit.label}"

    fun targetFrom(start: LocalDateTime): LocalDateTime = when (unit) {
        GoalUnit.DAYS -> start.plusDays(amount.toLong())
        GoalUnit.WEEKS -> start.plusWeeks(amount.toLong())
        GoalUnit.MONTHS -> start.plusMonths(amount.toLong())
        GoalUnit.YEARS -> start.plusYears(amount.toLong())
    }
}

/** All persisted state lives in one local SharedPreferences file. No accounts, no network. */
class Store(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("sober", Context.MODE_PRIVATE)

    var soberStart: LocalDateTime?
        get() = prefs.getString(KEY_START, null)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        set(value) {
            prefs.edit().putString(KEY_START, value?.toString()).apply()
        }

    var goals: List<Goal>
        get() {
            val raw = prefs.getString(KEY_GOALS, null) ?: return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).map {
                    val o = arr.getJSONObject(it)
                    Goal(o.getInt("id"), o.getInt("amount"), GoalUnit.valueOf(o.getString("unit")))
                }
            }.getOrDefault(emptyList())
        }
        set(value) {
            val arr = JSONArray()
            value.forEach {
                arr.put(JSONObject().put("id", it.id).put("amount", it.amount).put("unit", it.unit.name))
            }
            prefs.edit().putString(KEY_GOALS, arr.toString()).apply()
        }

    fun addGoal(amount: Int, unit: GoalUnit) {
        goals = goals + Goal(Random.nextInt(1, Int.MAX_VALUE), amount, unit)
    }

    fun removeGoal(id: Int) {
        goals = goals.filterNot { it.id == id }
    }

    private companion object {
        const val KEY_START = "start"
        const val KEY_GOALS = "goals"
    }
}
