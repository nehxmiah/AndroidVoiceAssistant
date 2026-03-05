package com.example.voiceassistant.models

class CommandProcessor(
    private val calculator: Calculator,
    private val clock: Clock
) {

    fun process(text: String): String {
        val textLower = text.lowercase()

        return when {
            textLower.contains("time") || textLower.contains("clock") -> handleTime()
            textLower.contains("date") -> handleDate()
            textLower.contains("calculate") ||
                    textLower.contains("what is") ||
                    textLower.contains("what's") ||
                    textLower.contains("compute") -> handleCalculation(text)
            else -> handleUnknown()
        }
    }

    private fun handleTime(): String {
        val timeStr = clock.getCurrentTime(format12hr = true)
        return "The time is $timeStr"
    }

    private fun handleDate(): String {
        val dateStr = clock.getCurrentDate(fullFormat = true)
        return "Today is $dateStr"
    }

    private fun handleCalculation(text: String): String {
        val expr = calculator.parseSpokenExpression(text)

        if (expr.isNotEmpty()) {
            return try {
                val result = calculator.evaluate(expr)
                val resultText = if (result % 1.0 == 0.0) {
                    result.toInt().toString()
                } else {
                    String.format("%.6f", result).trimEnd('0').trimEnd('.')
                }
                "The answer is $resultText"
            } catch (e: Exception) {
                "I couldn't calculate that: ${e.message}"
            }
        }

        return "I couldn't find a calculation in your request"
    }

    private fun handleUnknown(): String {
        return "I can help with time, date, and calculations. " +
                "Try asking 'what time is it?' or 'calculate 5 plus 3'"
    }
}