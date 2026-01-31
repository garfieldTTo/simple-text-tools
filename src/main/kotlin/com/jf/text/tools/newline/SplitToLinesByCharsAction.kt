package com.jf.text.tools.newline

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import java.util.regex.Pattern
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class SplitToLinesDialog(project: Project) : DialogWrapper(project) {
    private val charsField = JTextField(",;|", 20)
    private val keepCharsCheckBox = JCheckBox("Keep Split Chars")

    init{
        title = "Split-To-Lines"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.add(JLabel("Splits:"))
        panel.add(charsField)
        panel.add(keepCharsCheckBox)
        return panel
    }

    fun getChars(): String = charsField.text
    fun shouldKeepChars(): Boolean = keepCharsCheckBox.isSelected

    override fun getPreferredFocusedComponent(): JComponent = charsField

    override fun doOKAction() {
        if (charsField.text.isEmpty()) {
            Messages.showErrorDialog("Chars cannot be empty", "ERROR")
            return
        }
        super.doOKAction()

    }
}

class SplitToLinesByCharsAction : AnAction("Splits New Lines") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val selection = editor.selectionModel

        val dialog = SplitToLinesDialog(project)
        if(!dialog.showAndGet()) return

        val chars = decodeSplitChars(dialog.getChars())

        val keepChars = dialog.shouldKeepChars()

        if(chars.isEmpty()) return

        val start = if(selection.hasSelection()) selection.selectionStart else 0
        val end = if(selection.hasSelection()) selection.selectionEnd else document.textLength
        val text = document.getText(TextRange(start, end))

        val replaced = splitToLines(text, chars, keepChars)

        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(start, end, replaced)
            val selectionModel = editor.selectionModel
            if (selectionModel.hasSelection()) {
                selectionModel.removeSelection()
            }
        }


    }

    private fun splitToLines(input: String, chars: String, keepChars: Boolean): String {
        val trimmed = input.trim()
        if(trimmed.isEmpty()) return trimmed

        val charClass = buildString {
            append('[')
            for(c in chars){
                when (c) {
                    '\\', '^', '-', ']' -> append('\\').append(c)
                    else -> append(c)
                }
            }
            append(']')

        }

        return if (keepChars) {
            val regex = Pattern.compile("\\s*($charClass)\\s*")
            val matcher = regex.matcher(trimmed)
            val result = StringBuilder()
            var lastEnd = 0

            while (matcher.find()) {
                val beforeSeparator = trimmed.substring(lastEnd, matcher.start()).trim()
                if (beforeSeparator.isNotEmpty()) {
                    result.append(beforeSeparator)
                }
                result.append(matcher.group(1)).append("\n")
                lastEnd = matcher.end()
            }

            val remaining = trimmed.substring(lastEnd).trim()
            if (remaining.isNotEmpty()) {
                result.append(remaining)

            } else if (result.isNotEmpty() && result.last() == '\n') {
                result.setLength(result.length - 1)

            }
            result.toString()

        } else {
            val regex = Pattern.compile("\\s*($charClass)\\s*")
            regex.split(trimmed)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")

        }

    }

    private fun decodeSplitChars(raw: String): String {
        if (raw.isEmpty()) return raw
        val result = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '\\' && i + 1 < raw.length) {
                when (raw[i + 1]) {
                    't' -> {
                        result.append('\t')
                        i += 2
                        continue
                    }
                    's' -> {
                        result.append(' ')
                        i += 2
                        continue
                    }
                }
            }
            result.append(c)
            i += 1
        }
        return result.toString()
    }
}
