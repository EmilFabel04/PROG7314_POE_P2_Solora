package dev.solora.quote

import kotlin.math.ceil

data class QuoteInputs(
    val monthlyUsageKwh: Double?,
    val monthlyBillRands: Double?,
    val tariffRPerKwh: Double,
    val panelWatt: Int,
    val sunHoursPerDay: Double
)

data class QuoteOutputs(
    val panels: Int,
    val systemKw: Double,
    val inverterKw: Double,
    val estimatedMonthlySavingsR: Double
)

object QuoteCalculator {
    fun calculate(inputs: QuoteInputs): QuoteOutputs {
        val usageKwh = inputs.monthlyUsageKwh ?: run {
            val bill = inputs.monthlyBillRands ?: 0.0
            if (bill <= 0) 0.0 else bill / inputs.tariffRPerKwh
        }

        val averageDailyKwh = usageKwh / 30.0
        val systemKw = if (inputs.sunHoursPerDay <= 0) 0.0 else averageDailyKwh / inputs.sunHoursPerDay
        val panelKw = inputs.panelWatt / 1000.0
        val panels = if (panelKw <= 0) 0 else ceil(systemKw / panelKw).toInt()
        val inverterKw = (systemKw * 0.8).coerceAtLeast(1.0)
        val savings = usageKwh * inputs.tariffRPerKwh * 0.8
        return QuoteOutputs(panels = panels, systemKw = round2(systemKw), inverterKw = round2(inverterKw), estimatedMonthlySavingsR = round2(savings))
    }

    private fun round2(x: Double): Double = kotlin.math.round(x * 100.0) / 100.0
}


