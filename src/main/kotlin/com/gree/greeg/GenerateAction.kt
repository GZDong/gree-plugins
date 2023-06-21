package com.gree.greeg

import com.gree.greeg.genlib.genModelActivity
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem

class GenerateAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        val path = LangDataKeys.VIRTUAL_FILE.getData(event.dataContext)?.path

        val modelName =
            Messages.showInputDialog(project, "输入类名", "类名", Messages.getQuestionIcon())
        genModelActivity(this, path, modelName, "test/$modelName")
        LocalFileSystem.getInstance().refresh(false)
    }

    override fun update(event: AnActionEvent) {
        //获取工程对象
        val project: Project? = event.project
        //设置不可用且不可见
        event.presentation.isEnabledAndVisible = project != null
    }

}