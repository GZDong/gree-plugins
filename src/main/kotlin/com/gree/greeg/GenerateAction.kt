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

        val chooseResult = Messages.showYesNoDialog(
            "选择项目，不同项目的生成内容不同",
            "选择项目",
            "配件项目",
            "mvp项目",
            Messages.getQuestionIcon()
        )
        if (chooseResult == 0) {
            var modelName = Messages.showInputDialog(
                project,
                "输入类名(比如MainActivity，或者Main)",
                "自动生成mvvm下的各个组件",
                Messages.getInformationIcon()
            )
            if (modelName != null) {
                if (!modelName.endsWith("Activity")) {
                    modelName = modelName.plus("Activity")
                }
                val result = Messages.showYesNoDialog(
                    "是否需要自动生成repository？",
                    "下一步",
                    "需要",
                    "不需要，下一步",
                    Messages.getQuestionIcon()
                )
                if (result == 0) {
                    genModelActivity(this, path, modelName, "test/$modelName")
                    LocalFileSystem.getInstance().refresh(false)
                } else {
                    genModelActivity(this, path, modelName, "test/$modelName")
                    LocalFileSystem.getInstance().refresh(false)
                }
            }
        } else if (chooseResult == 1) {
            var modelName = Messages.showInputDialog(
                project,
                "输入类名(比如MainActivity，或者Main)",
                "自动生成mvvm下的各个组件",
                Messages.getInformationIcon()
            )
            if (modelName != null) {
                if (!modelName.endsWith("Activity")) {
                    modelName = modelName.plus("Activity")
                }
//                genModelActivity(this, path, modelName, "test/$modelName")
//                LocalFileSystem.getInstance().refresh(false)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        //获取工程对象
        val project: Project? = event.project
        //设置不可用且不可见
        event.presentation.isEnabledAndVisible = project != null
    }

}