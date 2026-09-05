package com.naze.files.util

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

@Immutable
data class SyntaxColors(
    val keyword: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
)

private data class LangSpec(
    val lineComment: String?,
    val blockCommentStart: String?,
    val blockCommentEnd: String?,
    val keywords: Set<String>,
)

/**
 * Deliberately simple: a single left-to-right scan that recognizes comments,
 * string literals, numbers, and a per-language keyword set. Good enough to
 * make code readable without pulling in a full grammar/lexer dependency.
 * Highlighting is skipped for large files by the caller to keep scrolling smooth.
 */
object SyntaxHighlighter {

    private val cLikeKeywords = setOf(
        "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "return",
        "class", "struct", "enum", "public", "private", "protected", "static", "final", "void",
        "int", "float", "double", "long", "short", "char", "bool", "boolean", "new", "this",
        "super", "import", "package", "interface", "extends", "implements", "try", "catch",
        "finally", "throw", "throws", "const", "let", "var", "function", "null", "true", "false",
    )

    private val pythonKeywords = setOf(
        "def", "class", "if", "elif", "else", "for", "while", "try", "except", "finally", "with",
        "as", "import", "from", "return", "yield", "break", "continue", "pass", "lambda", "None",
        "True", "False", "and", "or", "not", "in", "is", "global", "nonlocal", "assert", "raise", "del",
    )

    private val specs = mapOf(
        "kt" to LangSpec("//", "/*", "*/", cLikeKeywords + setOf("fun", "val", "object", "companion", "when", "is", "data", "sealed", "override", "suspend", "init", "in", "as")),
        "java" to LangSpec("//", "/*", "*/", cLikeKeywords),
        "js" to LangSpec("//", "/*", "*/", cLikeKeywords),
        "ts" to LangSpec("//", "/*", "*/", cLikeKeywords + setOf("interface", "type", "readonly", "export", "default")),
        "c" to LangSpec("//", "/*", "*/", cLikeKeywords),
        "cpp" to LangSpec("//", "/*", "*/", cLikeKeywords + setOf("namespace", "template", "typename")),
        "h" to LangSpec("//", "/*", "*/", cLikeKeywords),
        "py" to LangSpec("#", null, null, pythonKeywords),
        "json" to LangSpec(null, null, null, setOf("true", "false", "null")),
        "xml" to LangSpec(null, "<!--", "-->", emptySet()),
        "html" to LangSpec(null, "<!--", "-->", emptySet()),
        "htm" to LangSpec(null, "<!--", "-->", emptySet()),
        "css" to LangSpec(null, "/*", "*/", emptySet()),
        "yaml" to LangSpec("#", null, null, emptySet()),
        "yml" to LangSpec("#", null, null, emptySet()),
        "sh" to LangSpec("#", null, null, setOf("if", "then", "else", "fi", "for", "do", "done", "while", "function", "echo", "export")),
        "gradle" to LangSpec("//", "/*", "*/", setOf("plugins", "dependencies", "implementation", "android", "def", "val", "true", "false")),
    )

    fun highlight(text: String, extension: String, colors: SyntaxColors): AnnotatedString {
        val spec = specs[extension.lowercase()] ?: return AnnotatedString(text)
        val n = text.length

        return buildAnnotatedString {
            var i = 0
            while (i < n) {
                val lc = spec.lineComment
                val bcs = spec.blockCommentStart
                val bce = spec.blockCommentEnd

                if (lc != null && text.startsWith(lc, i)) {
                    val end = text.indexOf('\n', i).let { if (it == -1) n else it }
                    withStyle(SpanStyle(color = colors.comment)) { append(text.substring(i, end)) }
                    i = end
                    continue
                }
                if (bcs != null && bce != null && text.startsWith(bcs, i)) {
                    val closeIdx = text.indexOf(bce, i + bcs.length)
                    val end = if (closeIdx == -1) n else closeIdx + bce.length
                    withStyle(SpanStyle(color = colors.comment)) { append(text.substring(i, end)) }
                    i = end
                    continue
                }

                val c = text[i]
                if (c == '"' || c == '\'') {
                    val start = i
                    i++
                    while (i < n && text[i] != c) {
                        if (text[i] == '\\' && i + 1 < n) i++
                        i++
                    }
                    if (i < n) i++
                    withStyle(SpanStyle(color = colors.string)) { append(text.substring(start, i)) }
                    continue
                }

                if (c.isDigit()) {
                    val start = i
                    while (i < n && (text[i].isDigit() || text[i] == '.')) i++
                    withStyle(SpanStyle(color = colors.number)) { append(text.substring(start, i)) }
                    continue
                }

                if (c.isLetter() || c == '_') {
                    val start = i
                    while (i < n && (text[i].isLetterOrDigit() || text[i] == '_')) i++
                    val word = text.substring(start, i)
                    if (word in spec.keywords) {
                        withStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold)) { append(word) }
                    } else {
                        append(word)
                    }
                    continue
                }

                append(c)
                i++
            }
        }
    }
}
