package com.gree.greeg

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.StandardCharsets

class GenerateActivity : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        val path = LangDataKeys.VIRTUAL_FILE.getData(event.dataContext)?.path

//        val resultCode = Messages.showYesNoCancelDialog(project, "New Template class BaseActivity", "new Class", null)
//
//        if (resultCode == 0) {
//
//        }

        val modelName =
            Messages.showInputDialog(project, "please input model name", "New Class Model", Messages.getQuestionIcon())
            genModelActivity(path, modelName)
    }

    override fun update(event: AnActionEvent) {
        //获取工程对象
        val project: Project? = event.project
        //设置不可用且不可见
        event.presentation.isEnabledAndVisible = project != null
    }

    private fun genModelActivity(path: String?, name: String?) {
        if (path.isNullOrBlank() || name.isNullOrBlank()) {
            return
        }
        val file = File("$path\\$name.kt")
        val packageStr = TEMP_PACKAGE.replace("&package&", getPackageName(path))
        val importStr = TEMP_IMPORT
        val nameStr = TEMP_ACTIVITY.replace("&name&", name)

        FileUtils.writeStringToFile(file, packageStr + "\n" + "\n" + importStr + "\n" + "\n" + nameStr, StandardCharsets.UTF_8)
    }

    /**
     * 获取包名
     */
    private fun getPackageName(path: String): String {
        return path.substring(path.indexOf("java") + 5, path.length).replace("/", ".")
    }


}