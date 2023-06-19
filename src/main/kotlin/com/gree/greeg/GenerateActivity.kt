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
            Messages.showInputDialog(project, "输入类名", "类名", Messages.getQuestionIcon())
        genModelActivity(path, modelName, "test/$modelName")
    }

    override fun update(event: AnActionEvent) {
        //获取工程对象
        val project: Project? = event.project
        //设置不可用且不可见
        event.presentation.isEnabledAndVisible = project != null
    }

    private fun genModelActivity(path: String?, name: String?, routePath: String?) {
        if (path.isNullOrBlank() || name.isNullOrBlank() || routePath.isNullOrBlank()) {
            return
        }
        val activityFile = File("$path/$name.kt")
        FileUtils.writeStringToFile(activityFile, getActivityInjectContent(path, name, routePath), UTF_8)
        val vmName = if (name.contains("Activity")) {
            name.replace("Activity", "ViewModel")
        } else {
            name + "ViewModel"
        }
        val viewModelFile = File("$path/$vmName.kt")
        FileUtils.writeStringToFile(viewModelFile, getViewModelInjectContent(path, vmName), UTF_8)
    }

    /**
     * 获取包名
     */
    private fun getPackageName(path: String): String {
        return path.substring(path.indexOf("java") + 5, path.length).replace("/", ".")
    }

    private fun getActivityInjectContent(path: String, name: String, routerPath: String): String {
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
                        if (line.contains("&routerPath&")) {
                            line = line.replace("&routerPath&", routerPath)
                        }
                        if (line.contains("&ViewModel&")) {
                            val vmName = if (name.contains("Activity")) {
                                name.replace("Activity", "ViewModel")
                            } else {
                                name + "ViewModel"
                            }
                            line = line.replace("&ViewModel&", vmName)
                        }
                        if (line.contains("&Binding&")) {
                            val bindingName = if (name.contains("Activity")) {
                                name.replace("Activity", "Binding")
                            } else {
                                name + "Binding"
                            }
                            line = line.replace("&Binding&", "Activity$bindingName")
                        }
                        if (line.contains("&layout&")) {
                            val layoutName = if (name.contains("Activity")) {
                                name.replace("Activity", "")
                            } else {
                                name
                            }.lowercase()
                            line = line.replace("&layout&", "R.layout.activity_$layoutName")
                        }
                    }) != null) {
                    writeContent.append(line.ifEmpty { "\n" + "\n" })
                }
            }
            return writeContent.toString().replace("&n", "\n")
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    private fun getViewModelInjectContent(path: String, name: String): String {
        try {
            val writeContent = StringBuilder("")
            val tempFileStream = javaClass.getResourceAsStream("/TempViewModel.txt")
            val reader = tempFileStream?.let { InputStreamReader(it) }?.let { BufferedReader(it) }
            var line: String
            reader?.run {
                while ((reader.readLine().also {
                        line = it ?: ""
                        if (line.contains("&package&")) {
                            line = line.replace("&package&", getPackageName(path))
                        }
                        if (line.contains("&ViewModel&")) {
                            line = line.replace("&ViewModel&", name)
                        }
                    }) != null) {
                    writeContent.append(line.ifEmpty { "\n" + "\n" })
                }
            }
            return writeContent.toString().replace("&n", "\n")
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

}