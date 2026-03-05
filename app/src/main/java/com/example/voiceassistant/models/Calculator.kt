package com.example.voiceassistant.models

import kotlin.math.*

class Calculator {

    fun evaluate(expression: String): Double {
        var expr = expression.replace(" ", "").replace("^", "**")

        // Replace named constants
        expr = expr.replace("pi", PI.toString(), ignoreCase = true)
        expr = expr.replace("e", E.toString(), ignoreCase = true)

        try {
            return evaluateExpression(expr)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid expression: ${e.message}")
        }
    }

    private fun evaluateExpression(expr: String): Double {
        return parseAddSubtract(expr)
    }

    private fun parseAddSubtract(expr: String): Double {
        val parts = splitByOperators(expr, listOf('+', '-'))
        if (parts.size == 1) {
            return parseMultiplyDivide(parts[0].value)
        }

        var result = parseMultiplyDivide(parts[0].value)
        for (i in 1 until parts.size) {
            val value = parseMultiplyDivide(parts[i].value)
            result = when (parts[i].operator) {
                '+' -> result + value
                '-' -> result - value
                else -> result
            }
        }
        return result
    }

    private fun parseMultiplyDivide(expr: String): Double {
        val parts = splitByOperators(expr, listOf('*', '/'))
        if (parts.size == 1) {
            return parsePower(parts[0].value)
        }

        var result = parsePower(parts[0].value)
        for (i in 1 until parts.size) {
            val value = parsePower(parts[i].value)
            result = when (parts[i].operator) {
                '*' -> result * value
                '/' -> {
                    if (value == 0.0) throw ArithmeticException("Division by zero")
                    result / value
                }
                else -> result
            }
        }
        return result
    }

    private fun parsePower(expr: String): Double {
        if (expr.contains("**")) {
            val index = expr.indexOf("**")
            val base = parseUnary(expr.substring(0, index))
            val exponent = parsePower(expr.substring(index + 2))
            return base.pow(exponent)
        }
        return parseUnary(expr)
    }

    private fun parseUnary(expr: String): Double {
        // Handle parentheses
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return evaluateExpression(expr.substring(1, expr.length - 1))
        }

        // Handle functions
        val functions = mapOf(
            "sqrt" to { x: Double -> sqrt(x) },
            "sin" to { x: Double -> sin(x) },
            "cos" to { x: Double -> cos(x) },
            "tan" to { x: Double -> tan(x) },
            "log" to { x: Double -> ln(x) },
            "log10" to { x: Double -> log10(x) },
            "exp" to { x: Double -> exp(x) },
            "abs" to { x: Double -> abs(x) }
        )

        for ((name, func) in functions) {
            if (expr.startsWith(name)) {
                val arg = expr.substring(name.length).removePrefix("(").removeSuffix(")")
                return func(evaluateExpression(arg))
            }
        }

        // Handle negative numbers
        if (expr.startsWith("-")) {
            return -parseUnary(expr.substring(1))
        }

        return expr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $expr")
    }

    private fun splitByOperators(expr: String, operators: List<Char>): List<Part> {
        val parts = mutableListOf<Part>()
        var currentPart = StringBuilder()
        var parenthesesCount = 0
        var lastOperator: Char? = null

        for (char in expr) {
            when {
                char == '(' -> {
                    parenthesesCount++
                    currentPart.append(char)
                }
                char == ')' -> {
                    parenthesesCount--
                    currentPart.append(char)
                }
                char in operators && parenthesesCount == 0 -> {
                    if (currentPart.isNotEmpty()) {
                        parts.add(Part(currentPart.toString(), lastOperator))
                        currentPart = StringBuilder()
                    }
                    lastOperator = char
                }
                else -> currentPart.append(char)
            }
        }

        if (currentPart.isNotEmpty()) {
            parts.add(Part(currentPart.toString(), lastOperator))
        }

        return parts
    }

    private data class Part(val value: String, val operator: Char?)

    fun parseSpokenExpression(text: String): String {
        var textLower = text.lowercase()
            .replace(Regex("^(calculate|compute|what is|what's)\\s+"), "")
            .trim()

        // Convert spoken numbers to digits
        val numberWords = mapOf(
            "zero" to "0", "one" to "1", "two" to "2", "three" to "3",
            "four" to "4", "five" to "5", "six" to "6", "seven" to "7",
            "eight" to "8", "nine" to "9", "ten" to "10", "eleven" to "11",
            "twelve" to "12", "thirteen" to "13", "fourteen" to "14",
            "fifteen" to "15", "sixteen" to "16", "seventeen" to "17",
            "eighteen" to "18", "nineteen" to "19", "twenty" to "20",
            "thirty" to "30", "forty" to "40", "fifty" to "50",
            "sixty" to "60", "seventy" to "70", "eighty" to "80",
            "ninety" to "90", "hundred" to "100"
        )

        for ((word, digit) in numberWords) {
            textLower = textLower.replace(word, digit)
        }

        // Word-to-symbol replacements (order matters - longer phrases first)
        val replacements = mapOf(
            "multiplied by" to "*",
            "divided by" to "/",
            "to the power of" to "**",
            "square root of" to "sqrt(",
            "sine of" to "sin(",
            "cosine of" to "cos(",
            "tangent of" to "tan(",
            "plus" to "+",
            "minus" to "-",
            "times" to "*",
            "over" to "/",
            "squared" to "**2",
            "cubed" to "**3"
        )

        for ((word, symbol) in replacements) {
            textLower = textLower.replace(word, symbol)
        }

        // Keep only valid expression characters
        textLower = textLower.replace(Regex("[^0-9+\\-*/.()^]"), "")

        if (textLower.isEmpty()) return ""

        // Replace ^ with ** for power operations
        textLower = textLower.replace("^", "**")

        // Balance parentheses
        val openCount = textLower.count { it == '(' }
        val closeCount = textLower.count { it == ')' }
        if (openCount > closeCount) {
            textLower += ")".repeat(openCount - closeCount)
        }

        return textLower.trim()
    }
}