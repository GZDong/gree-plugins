package com.gree.greeg

/**
 * 获取包名
 */
fun getPackageName(path: String): String {
    return path.substring(path.indexOf("java") + 5, path.length).replace("/", ".")
}

fun getXmlPath(path: String): String {
    return path.substring(0, path.indexOf("main") + 5) + "res/layout"
}