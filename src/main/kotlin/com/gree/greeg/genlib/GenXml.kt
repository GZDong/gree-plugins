package com.gree.greeg.genlib

import com.gree.greeg.getPackageName
import com.gree.greeg.getXmlPath
import com.intellij.openapi.actionSystem.AnAction
import org.apache.commons.io.FileUtils
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

fun genXml(action: AnAction, path: String, name: String) {
    val xmlName = if (name.contains("Activity")) {
        "activity_" + name.replace("Activity", "").lowercase()
    } else {
        "activity_" + name.lowercase()
    }
    var xmlFile = File("${getXmlPath(path)}/$xmlName.xml")
    if (xmlFile.exists()) {
        xmlFile = File("${getXmlPath(path)}/$xmlName" + "_1.xml")
    }
    FileUtils.writeStringToFile(xmlFile, getXmlInjectContent(action, path, name), StandardCharsets.UTF_8)
}

fun getXmlInjectContent(action: AnAction, path: String, name: String): String {
    try {
        val writeContent = StringBuilder("")
        val tempFileStream = action.javaClass.getResourceAsStream("/TempXml.txt")
        val reader = tempFileStream?.let { InputStreamReader(it) }?.let { BufferedReader(it) }
        var line: String
        reader?.run {
            while ((reader.readLine().also {
                    line = it ?: ""
                    if (line.contains("&class&")) {
                        val vmName = if (name.contains("Activity")) {
                            name.replace("Activity", "ViewModel")
                        } else {
                            name + "ViewModel"
                        }
                        line = line.replace("&class&", getPackageName(path) + "." + vmName)
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