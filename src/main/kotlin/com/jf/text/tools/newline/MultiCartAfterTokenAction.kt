package com.jf.text.tools.newline

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.ui.Messages

class MultiCartAfterTokenAction : AnAction(){
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor != null
    }


    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)?: return
        val project = e.project?: return
        val doc = editor.document

        val token = Messages.showInputDialog(
            project,
            "Enter token: ",
            "Add Carets After Token",
            null,
            ":",
            null
        )?: return

        if(token.isEmpty()) return

        val text = doc.charsSequence

        val offsets = findAllEndOffsets(text, token)


        if(offsets.isEmpty()) return

        WriteCommandAction.runWriteCommandAction(project) {
            applyCarets(editor, offsets)
        }
    }

    private fun findAllEndOffsets(text: CharSequence, token: String): IntArray{
        val result = ArrayList<Int>(64)
        var fromIndex = 0
        while (fromIndex <= text.length-token.length) {
            val idx = indexOf(text,token,fromIndex)
            if(idx<0) break
            result.add(idx+token.length)
            fromIndex = idx+token.length
        }
        return result.toIntArray()

    }

    private fun indexOf(text: CharSequence, token: String, fromIndex: Int): Int {
        if(token.isEmpty()) return -1
        val max = text.length - token.length
        for (i in fromIndex until max) {
            var j = 0
            while (j < token.length && text[j + i] == token[j]) {
                j++

            }
            if(j==token.length) return i
        }
        return -1

    }

    private fun applyCarets(editor: Editor, offsets: IntArray) {

        val selectionModel = editor.selectionModel
        if (selectionModel.hasSelection()) {
            selectionModel.removeSelection()
        }
        val caretModel = editor.caretModel
        caretModel.removeSecondaryCarets()

        caretModel.primaryCaret.moveToOffset(offsets[0])

        for (i in 1 until offsets.size) {
            caretModel.addCaret(editor.offsetToVisualPosition(offsets[i]))

        }

        editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }


}