package com.naze.files.util

sealed class NameValidation {
    data object Valid : NameValidation()
    data class Invalid(val reason: String) : NameValidation()
}

object FileNameValidator {
    private val invalidChars = charArrayOf('/', '\u0000')

    fun validate(name: String, existingNames: Set<String>): NameValidation {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> NameValidation.Invalid("Name cannot be empty")
            trimmed == "." || trimmed == ".." -> NameValidation.Invalid("That name isn't allowed")
            trimmed.any { it in invalidChars } -> NameValidation.Invalid("Name cannot contain \"/\"")
            existingNames.any { it.equals(trimmed, ignoreCase = true) } ->
                NameValidation.Invalid("\"$trimmed\" already exists here")
            else -> NameValidation.Valid
        }
    }
}
