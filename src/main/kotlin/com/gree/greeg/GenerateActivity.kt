package com.gree.greeg

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.apache.commons.io.FileUtils
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets.UTF_8

class GenerateActivity : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        val path = LangDataKeys.VIRTUAL_FILE.getData(event.dataContext)?.path

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
        val file = File("$path/$name.kt")
        FileUtils.writeStringToFile(file, getInjectContent(path, name), UTF_8)
    }

    /**
     * 获取包名
     */
    private fun getPackageName(path: String): String {
        return path.substring(path.indexOf("java") + 5, path.length).replace("/", ".")
    }

    private fun getInjectContent(path: String, name: String): String {
        try {
            val writeContent = StringBuilder("")
            val tempFileStream = javaClass.getResourceAsStream("/TempActivity.txt")
            val reader = tempFileStream?.let { InputStreamReader(it) }?.let { BufferedReader(it) }
            var line: String
            reader?.run {
                while ((reader.readLine().also {
                        line = it ?: ""
                        if (line.contains("&package&")) {
                            line = line.replace("&package&", getPackageName(path))
                        }
                        if (line.contains("&Activity&")) {
                            line = line.replace("&Activity&", name)
                        }
                    }) != null) {
                    writeContent.append(line.ifEmpty { "\n" + "\n" })
                }
            }
            return writeContent.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

}