package com.gree.greeg.genlib

import com.gree.greeg.getPackageName
import com.intellij.openapi.actionSystem.AnAction
import org.apache.commons.io.FileUtils
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

fun genViewModel(action: AnAction, path: String, name: String) {
    val vmName = if (name.contains("Activity")) {
        name.replace("Activity", "ViewModel")
    } else {
        name + "ViewModel"
    }
    var viewModelFile = File("$path/$vmName.kt")
    if (viewModelFile.exists()) {
        viewModelFile = File("$path/$vmName" + "_NeedToRename.kt")
    }
    FileUtils.writeStringToFile(viewModelFile, getViewModelInjectContent(action, path, vmName), StandardCharsets.UTF_8)
}

fun getViewModelInjectContent(action: AnAction, path: String, name: String): String {
    try {
        val writeContent = StringBuilder("")
        val tempFileStream = action.javaClass.getResourceAsStream("/TempViewModel.txt")
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