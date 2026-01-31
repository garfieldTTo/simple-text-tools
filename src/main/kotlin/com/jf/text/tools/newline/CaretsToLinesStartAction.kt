package com.jf.text.tools.newline

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor

class CaretsToLinesStartAction : AnAction() {
    override fun update(e: AnActionEvent){
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)?: return
        val project = e.project?: return
        val doc = editor.document

        val offsets = IntArray(doc.lineCount){line->
            doc.getLineStartOffset(line)

        }

        WriteCommandAction.runWriteCommandAction(project) {
            applyCarets(editor, offsets)
        }

    }

    private fun applyCarets(editor: Editor, offsets: IntArray) {
        val selectionModel = editor.selectionModel
        if (selectionModel.hasSelection()) {
            selectionModel.removeSelection()
        }

        val caretModel = editor.caretModel
        caretModel.removeSecondaryCarets()

        if(offsets.isEmpty())  return

        caretModel.primaryCaret.moveToOffset(offsets[0])
        for(i in 1 until  offsets.size){
            caretModel.addCaret(editor.offsetToVisualPosition(offsets[i]))
        }
    }
}