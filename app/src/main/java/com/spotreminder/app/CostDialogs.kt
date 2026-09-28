package com.spotreminder.app

import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/** The "Add a cost" popup, shared by the Spots list and a trip's history card, whether the trip is active or already finished. */
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

        val (catRow, catInput) = fieldRow("Category", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        catInput.hint = "e.g. Bus, Food"

        val chipsRow = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        for (cat in TRIP_COST_CATEGORIES) {
            chipsRow.addView(MaterialButton(activity, null, android.R.attr.borderlessButtonStyle).apply {
                text = cat
                isAllCaps = false
                textSize = 13f
                setTextColor(color(R.color.river_soft))
                setOnClickListener {
                    catInput.setText(cat)
                    catInput.setSelection(catInput.text?.length ?: 0)
                }
            })
        }
        val (amtRow, amtInput) = fieldRow("Amount", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)

        val fieldsLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(chipsRow)
            addView(catRow)
            addView(amtRow)
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
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val cat = catInput.text?.toString()?.trim().orEmpty()
                val amt = amtInput.text?.toString()?.trim()?.toDoubleOrNull()
                if (cat.isEmpty() || amt == null || amt <= 0) {
                    Toast.makeText(activity, "Enter a category and amount", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                onAdd(CostItem(Store.titleCase(cat), amt))
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}
