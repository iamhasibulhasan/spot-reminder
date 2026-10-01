package com.spotreminder.app

/**
 * A cost category: shown as a colored circular badge with an emoji glyph. Using emoji instead of
 * custom vector art per category keeps this maintainable while still giving each one a distinct,
 * recognizable look (matching the categorized-with-icons pattern common to travel expense trackers).
 */
data class CostCategory(val name: String, val emoji: String, val colorHex: String)

val COST_CATEGORIES = listOf(
    CostCategory("Transportation", "🚌", "#F2994A"),
    CostCategory("Restaurants", "🍴", "#27AE60"),
    CostCategory("Accommodation", "🏨", "#EB5757"),
    CostCategory("Groceries", "🛒", "#2D9CDB"),
    CostCategory("Shopping", "🛍", "#219653"),
    CostCategory("Activities", "🎫", "#EB5784"),
    CostCategory("Drinks", "🍸", "#9B51E0"),
    CostCategory("Coffee", "☕", "#8D6E63"),
    CostCategory("Flights", "✈", "#2F80ED"),
    CostCategory("Sightseeing", "🏞", "#43A047"),
    CostCategory("Entertainment", "🎬", "#FF5722"),
    CostCategory("Laundry", "🧺", "#009688"),
    CostCategory("Exchange Fees", "💱", "#3F51B5"),
    CostCategory("Fees & Charges", "🧾", "#C2185B"),
    CostCategory("General", "🏷", "#F2C94C"),
    CostCategory("Other", "💵", "#8A8FA3")
)

/** Falls back to "Other" for any custom/legacy category text that isn't in the fixed list. */
fun findCostCategory(name: String): CostCategory =
    COST_CATEGORIES.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: COST_CATEGORIES.last()

val PAYMENT_METHODS = listOf("Cash", "Credit Card", "Debit Card", "Mobile Payment", "Other")

/** Evaluates a simple calculator expression typed into an amount field, e.g. "12.5+3*2". */
object Calc {
    fun eval(expr: String): Double? {
        val cleaned = expr.replace(Regex("\\s+"), "").replace('×', '*').replace('÷', '/')
        if (cleaned.isEmpty()) return null
        return try {
            val tokens = tokenize(cleaned) ?: return null
            if (tokens.isEmpty()) return null
            evalAddSub(tokens)
        } catch (e: Exception) {
            null
        }
    }

    private sealed class Tok {
        data class Num(val v: Double) : Tok()
        data class Op(val c: Char) : Tok()
    }

    private fun tokenize(s: String): List<Tok>? {
        val out = mutableListOf<Tok>()
        var i = 0
        var expectNumber = true
        while (i < s.length) {
            val c = s[i]
            if (c == '+' || c == '-' || c == '*' || c == '/') {
                if (expectNumber && c == '-') {
                    // unary minus: fold into the following number
                    i++
                    val numStart = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    if (i == numStart) return null
                    out.add(Tok.Num(-(s.substring(numStart, i).toDoubleOrNull() ?: return null)))
                    expectNumber = false
                    continue
                }
                if (expectNumber) return null
                out.add(Tok.Op(c))
                expectNumber = true
                i++
            } else if (c.isDigit() || c == '.') {
                val start = i
                while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                out.add(Tok.Num(s.substring(start, i).toDoubleOrNull() ?: return null))
                expectNumber = false
            } else {
                return null
            }
        }
        if (expectNumber) return null
        return out
    }

    /** First collapses all * and / (left to right), then sums the remaining +/- chain. */
    private fun evalAddSub(tokens: List<Tok>): Double {
        val collapsed = mutableListOf<Double>()
        val addSubOps = mutableListOf<Char>()
        var current = (tokens[0] as Tok.Num).v
        var i = 1
        while (i < tokens.size) {
            val op = (tokens[i] as Tok.Op).c
            val next = (tokens[i + 1] as Tok.Num).v
            if (op == '*' || op == '/') {
                current = if (op == '*') current * next else current / next
            } else {
                collapsed.add(current)
                addSubOps.add(op)
                current = next
            }
            i += 2
        }
        collapsed.add(current)
        var result = collapsed[0]
        for (j in addSubOps.indices) {
            result = if (addSubOps[j] == '+') result + collapsed[j + 1] else result - collapsed[j + 1]
        }
        return result
    }
}
