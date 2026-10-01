package com.spotreminder.app

import android.content.res.ColorStateList
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/**
 * The "Add a cost" popup, shared by the Spots list and a trip's history card, whether the trip is
 * active or already finished. Modeled on the common travel-expense-tracker pattern: a categorized
 * picker (icon + color per category), a payment method, a calculator-capable amount field, and the
 * place the expense happened at (captured from the device's current location, if available).
 */
object CostDialogs {

    fun showAddCost(activity: AppCompatActivity, onAdd: (CostItem) -> Unit) {
        fun dp(v: Int) = (v * activity.resources.displayMetrics.density).toInt()
        fun color(id: Int) = ContextCompat.getColor(activity, id)

        fun fieldRow(caption: String, inputTypeFlags: Int): Pair<LinearLayout, TextInputEditText> {
            val input = TextInputEditText(activity).apply {
                setBackgroundColor(Color.TRANSPARENT)
                setTextColor(color(R.color.ink))
                setHintTextColor(color(R.color.river_soft))
                textSize = 16f
                inputType = inputTypeFlags
            }
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = ContextCompat.getDrawable(activity, R.drawable.bg_field_row)
                setPadding(dp(14), dp(4), dp(14), dp(4))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(56)
                ).apply { topMargin = dp(8) }
            }
            row.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(activity).apply {
                text = caption
                textSize = 12f
                setTextColor(color(R.color.river_soft))
                setPadding(dp(8), 0, 0, 0)
            })
            return row to input
        }

        var selectedCategory = COST_CATEGORIES.first()
        var selectedPayment = PAYMENT_METHODS.first()
        var foundLat: Double? = null
        var foundLon: Double? = null
        var foundLabel: String? = null

        // --- Category picker row ---
        val categoryBadge = TextView(activity).apply {
            text = selectedCategory.emoji
            textSize = 18f
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(activity, R.drawable.bg_icon_circle)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor(selectedCategory.colorHex))
            layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
        }
        val categoryLabel = TextView(activity).apply {
            text = selectedCategory.name
            textSize = 15f
            setTextColor(color(R.color.ink))
            setPadding(dp(10), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val categoryRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(activity, R.drawable.bg_field_row)
            setPadding(dp(10), dp(8), dp(14), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
            isClickable = true
            addView(categoryBadge)
            addView(categoryLabel)
            addView(ImageViewIcon(activity, R.drawable.ic_expand_more, color(R.color.river_soft)))
        }

        val (titleRow, titleInput) = fieldRow("Title (optional)", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)

        val (amountRow, amountInput) = fieldRow(
            "Amount",
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        )
        amountInput.hint = "e.g. 12.5 or 25+10"
        val operatorsRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(4) }
        }
        for (op in listOf("+", "−", "×", "÷")) {
            operatorsRow.addView(MaterialButton(activity, null, android.R.attr.borderlessButtonStyle).apply {
                text = op
                isAllCaps = false
                setTextColor(color(R.color.accent))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    val symbol = when (op) { "−" -> "-"; "×" -> "*"; "÷" -> "/"; else -> op }
                    val pos = amountInput.selectionStart.coerceAtLeast(0)
                    amountInput.text?.insert(pos, symbol)
                }
            })
        }

        // --- Payment method chips ---
        val paymentChips = mutableListOf<MaterialButton>()
        val paymentRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(10) }
        }
        fun refreshPaymentChips() {
            for (chip in paymentChips) {
                val isSel = chip.text == selectedPayment
                chip.setTextColor(if (isSel) Color.WHITE else color(R.color.ink))
                chip.backgroundTintList = ColorStateList.valueOf(
                    if (isSel) color(R.color.accent) else color(R.color.field)
                )
            }
        }
        for (pm in PAYMENT_METHODS) {
            val chip = MaterialButton(activity).apply {
                text = pm
                isAllCaps = false
                textSize = 12f
                cornerRadius = dp(16)
                insetTop = 0
                insetBottom = 0
                setPadding(dp(10), 0, dp(10), 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(32)
                ).apply { marginEnd = dp(6) }
                setOnClickListener {
                    selectedPayment = pm
                    refreshPaymentChips()
                }
            }
            paymentChips.add(chip)
            paymentRow.addView(chip)
        }
        refreshPaymentChips()

        // --- Location (captured from the device, optional) ---
        val locationText = TextView(activity).apply {
            text = "Finding your location…"
            textSize = 12f
            setTextColor(color(R.color.river_soft))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val locationClear = ImageViewIcon(activity, R.drawable.ic_delete, color(R.color.river_soft)).apply {
            visibility = View.GONE
        }
        val locationRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(10) }
            addView(ImageViewIcon(activity, R.drawable.ic_pin_small, color(R.color.river_soft)).apply {
                layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(6) }
            })
            addView(locationText)
            addView(locationClear)
        }
        locationClear.setOnClickListener {
            foundLat = null; foundLon = null; foundLabel = null
            locationText.text = "No location"
            locationClear.visibility = View.GONE
        }
        if (LocationChecker.hasLocationPermission(activity)) {
            Thread {
                val loc = LocationChecker.currentLocation(activity)
                val label = if (loc != null) Osm.reverseCity(loc.latitude, loc.longitude) else null
                activity.runOnUiThread {
                    if (loc == null) {
                        locationText.text = "Location unavailable"
                    } else {
                        foundLat = loc.latitude
                        foundLon = loc.longitude
                        foundLabel = label
                        locationText.text = label ?: "%.4f, %.4f".format(loc.latitude, loc.longitude)
                        locationClear.visibility = View.VISIBLE
                    }
                }
            }.start()
        } else {
            locationText.text = "Location is off"
        }

        val fieldsLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(categoryRow)
            addView(titleRow)
            addView(amountRow)
            addView(operatorsRow)
            addView(paymentRow)
            addView(locationRow)
        }
        val wrap = FrameLayout(activity).apply {
            val pad = dp(20)
            setPadding(pad, dp(8), pad, 0)
            addView(fieldsLayout)
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Add a cost")
            .setView(wrap)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        categoryRow.setOnClickListener {
            showCategoryPicker(activity) { picked ->
                selectedCategory = picked
                categoryBadge.text = picked.emoji
                categoryBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(picked.colorHex))
                categoryLabel.text = picked.name
            }
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val amt = Calc.eval(amountInput.text?.toString().orEmpty())
                if (amt == null || amt <= 0) {
                    Toast.makeText(activity, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                onAdd(
                    CostItem(
                        category = selectedCategory.name,
                        amount = amt,
                        title = titleInput.text?.toString()?.trim().orEmpty(),
                        paymentMethod = selectedPayment,
                        lat = foundLat,
                        lon = foundLon,
                        placeLabel = foundLabel
                    )
                )
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    /** A grid of category badges to pick from, with a search box to filter by name (matches the
     *  "Pick a Category" pattern from common expense trackers). */
    private fun showCategoryPicker(activity: AppCompatActivity, onPick: (CostCategory) -> Unit) {
        fun dp(v: Int) = (v * activity.resources.displayMetrics.density).toInt()
        fun color(id: Int) = ContextCompat.getColor(activity, id)

        val searchInput = EditText(activity).apply {
            hint = "Search categories"
            setHintTextColor(color(R.color.river_soft))
            setTextColor(color(R.color.ink))
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val searchWrap = FrameLayout(activity).apply {
            background = ContextCompat.getDrawable(activity, R.drawable.bg_field_row)
            addView(searchInput)
        }

        val grid = GridLayout(activity).apply {
            columnCount = 3
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }

        var dialogRef: AlertDialog? = null

        fun renderGrid(filter: String) {
            grid.removeAllViews()
            val q = filter.trim().lowercase()
            val items = if (q.isEmpty()) COST_CATEGORIES else COST_CATEGORIES.filter { it.name.lowercase().contains(q) }
            for (cat in items) {
                val cell = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(6), dp(10), dp(6), dp(10))
                    isClickable = true
                    background = ContextCompat.getDrawable(activity, android.R.drawable.list_selector_background)
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = GridLayout.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    }
                    setOnClickListener {
                        onPick(cat)
                        dialogRef?.dismiss()
                    }
                }
                cell.addView(TextView(activity).apply {
                    text = cat.emoji
                    textSize = 20f
                    gravity = Gravity.CENTER
                    background = ContextCompat.getDrawable(activity, R.drawable.bg_icon_circle)
                    backgroundTintList = ColorStateList.valueOf(Color.parseColor(cat.colorHex))
                    layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                })
                cell.addView(TextView(activity).apply {
                    text = cat.name
                    textSize = 11f
                    gravity = Gravity.CENTER
                    maxLines = 2
                    setTextColor(color(R.color.ink))
                    setPadding(0, dp(4), 0, 0)
                })
                grid.addView(cell)
            }
            if (items.isEmpty()) {
                grid.addView(TextView(activity).apply {
                    text = "No categories match \"$filter\"."
                    setTextColor(color(R.color.river_soft))
                    textSize = 13f
                    setPadding(dp(4), dp(10), dp(4), dp(10))
                })
            }
        }
        renderGrid("")

        val scroller = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(360))
            addView(grid)
        }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(16)
            setPadding(pad, dp(8), pad, 0)
            addView(searchWrap)
            addView(scroller)
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Pick a category")
            .setView(content)
            .setNegativeButton("Cancel", null)
            .create()
        dialogRef = dialog

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                renderGrid(s?.toString().orEmpty())
            }
        })

        dialog.show()
    }

    private fun ImageViewIcon(activity: AppCompatActivity, drawableRes: Int, tint: Int) =
        android.widget.ImageView(activity).apply {
            setImageResource(drawableRes)
            setColorFilter(tint)
            val pad = (6 * activity.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(
                (24 * activity.resources.displayMetrics.density).toInt(),
                (24 * activity.resources.displayMetrics.density).toInt()
            )
        }
}
