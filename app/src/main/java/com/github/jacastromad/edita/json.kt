package com.github.jacastromad.edita

/**
 * Encodes a string for use in JSON.
 *
 * This function escapes special characters and converts non-ASCII characters
 * to Unicode escape sequences, making the string safe for inclusion in JSON.
 *
 * @return A JSON-safe version of the string.
 */
fun String.toJSON(): String {
    val sb = StringBuilder()
    for (c in this) {
        when (c) {
            // Handle common escape characters
            '\\' -> sb.append("\\\\")
            '\"' -> sb.append("\\\"")
            '\b' -> sb.append("\\b")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\u000C' -> sb.append("\\f")
            else -> {
                if (c.code < 32 || c.code == 127 || c.isISOControl()) {
                    // Escape control characters and non-printable ASCII
                    sb.append(String.format("\\u%04x", c.code))
                } else {
                    // Append all other characters as-is
                    sb.append(c)
                }
            }
        }
    }
    return sb.toString()
}

/**
 * Decodes a JSON-encoded string.
 *
 * This function unescapes special characters and decodes Unicode escape sequences
 * in a JSON string. It assumes the input string is wrapped in quotes and skips
 * the first and last character.
 *
 * @return The original string without escaped sequences.
 */
fun String.fromJSON(): String {
    val sb = StringBuilder()
    var i = 1
    while (i < this.length-1) {
        val c = this[i]
        if (c == '\\' && i + 1 < this.length) {
            when (this[i + 1]) {
                // Handle common escape sequences
                '\\' -> {
                    sb.append('\\')
                    i++
                }
                '\"' -> {
                    sb.append('\"')
                    i++
                }
                'b' -> {
                    sb.append('\b')
                    i++
                }
                'n' -> {
                    sb.append('\n')
                    i++
                }
                'r' -> {
                    sb.append('\r')
                    i++
                }
                't' -> {
                    sb.append('\t')
                    i++
                }
                'f' -> {
                    sb.append('\u000C')
                    i++
                }
                // Handle Unicode escape sequences
                'u' -> {
                    if (i + 5 < this.length) {
                        val unicode = this.substring(i + 2, i + 6).toIntOrNull(16)
                        if (unicode != null) {
                            sb.append(unicode.toChar())
                            i += 5
                        } else {
                            sb.append(c)
                        }
                    } else {
                        sb.append(c)
                    }
                }
                else -> sb.append(c)
            }
        } else {
            sb.append(c)
        }
        i++
    }
    return sb.toString()
}