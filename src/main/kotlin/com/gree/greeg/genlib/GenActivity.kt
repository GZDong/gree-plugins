package com.gree.greeg.genlib

import com.gree.greeg.getPackageName
import com.intellij.openapi.actionSystem.AnAction
import org.apache.commons.io.FileUtils
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

fun genModelActivity(action: AnAction, path: String?, name: String?, routePath: String?) {
    if (path.isNullOrBlank() || name.isNullOrBlank() || routePath.isNullOrBlank()) {
        return
    }
    val activityFile = File("$path/$name.kt")
    FileUtils.writeStringToFile(
        activityFile, getActivityInjectContent(action, path, name, routePath),
        StandardCharsets.UTF_8
    )
    genViewModel(action, path, name)
}

fun getActivityInjectContent(action: AnAction, path: String, name: String, routerPath: String): String {
    try {
        val writeContent = StringBuilder("")
        val tempFileStream = action.javaClass.getResourceAsStream("/TempActivity.txt")
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