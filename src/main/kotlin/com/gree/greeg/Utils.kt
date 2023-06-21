package com.gree.greeg

/**
 * 获取包名
 */
fun getPackageName(path: String): String {
    return path.substring(path.indexOf("java") + 5, path.length).replace("/", ".")
}