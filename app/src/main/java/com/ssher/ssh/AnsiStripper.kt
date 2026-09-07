package com.ssher.ssh

object AnsiStripper {
    // Strip common ANSI CSI / OSC sequences so greyscale text stays readable.
    private val ansiRegex = Regex(
        pattern = buildString {
            append("""\u001B\[[0-9;?]*[ -/]*[@-~]""")
            append('|')
            append("""\u001B\][^\u0007]*(?:\u0007|\u001B\\)""")
            append('|')
            append("""\u001B[@-Z\\-_]""")
            append('|')
            append("""\r""")
        },
    )

    fun strip(input: String): String = ansiRegex.replace(input, "")
}
