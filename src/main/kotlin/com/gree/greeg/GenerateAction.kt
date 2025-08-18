package com.gree.greeg

import com.gree.greeg.ui.GenDialog
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.generation.actions.BaseGenerateAction
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import org.apache.commons.io.FileUtils
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*

class GenerateAction(handler: CodeInsightActionHandler? = null) : BaseGenerateAction(handler) {

    override fun isValidForClass(targetClass: PsiClass?): Boolean {
        return super.isValidForClass(targetClass)
    }

    override fun actionPerformed(event: AnActionEvent) {

        // 获取项目和当前路径信息
        val project = event.getData(PlatformDataKeys.PROJECT)
        val dataContext = event.dataContext
        val path = LangDataKeys.VIRTUAL_FILE.getData(event.dataContext)?.path!!.replace("\\", "/")

        GenDialog.show(object : OnFillListener {
            override fun onFinished(tempActivityName: String, tempPath: String, chooseRep: Boolean) {
                project?.run {
                    //生成模板activity文件
                    val modelName =
                        if (tempActivityName.endsWith("Activity")) tempActivityName.removeSuffix("Activity") else tempActivityName
                    val modelPath = getModelName(path)
                    val activityTemp = FileTemplateManager.getInstance(this).getInternalTemplate("TempActivity")
                    val activityProperties = Properties()
                    activityProperties.putAll(FileTemplateManager.getInstance(this).defaultProperties)
                    activityProperties["PACKAGE_NAME"] = getPackageName(path)
                    activityProperties["MODEL_NAME"] = modelPath
                    activityProperties["ROUTER_PATH"] = tempPath
                    activityProperties["BINDING"] =
                        modelPath.replaceFirst(
                            modelPath.first(),
                            modelPath.first().uppercase().toCharArray().first(),
                            false
                        )
                    activityProperties["INPUT_NAME"] = modelName
                    activityProperties["L_MODEL_NAME"] = modelPath.lowercase()
                    activityProperties["LAYOUT"] = getXmlEndName(modelName)
                    createTempCode(
                        activityTemp,
                        modelName + "Activity.kt",
                        LangDataKeys.IDE_VIEW.getData(dataContext)!!.orChooseDirectory!!,
                        activityProperties
                    )
                    val psiManager = PsiManager.getInstance(project)
                    if (chooseRep) {
                        //生成带resp的ViewModel，以及相关的resp文件
                        //di目录
                        val diPath = getModelPath(modelPath, path) + "/di"
                        val diDir = File(diPath)
                        if (!diDir.exists()) {
                            diDir.mkdir()
                        }
                        val subModelName = modelName.replaceFirst(
                            modelName.first(),
                            modelName.first().lowercase().toCharArray()[0],
                            false
                        )

                        //data目录
                        val dataPath = getModelPath(modelPath, path) + "/data"
                        val dataDir = File(dataPath)
                        if (!dataDir.exists()) {
                            dataDir.mkdir()
                        }
                        //repository目录
                        val repositoryPath = "$dataPath/repository"
                        val repositoryDir = File(repositoryPath)
                        if (!repositoryDir.exists()) {
                            repositoryDir.mkdir()
                        }
                        //service目录
                        val servicePath = "$dataPath/service"
                        val serviceDir = File(servicePath)
                        if (!serviceDir.exists()) {
                            serviceDir.mkdir()
                        }
                        LocalFileSystem.getInstance().refresh(false)
                        val repsVirtualFile =
                            VirtualFileManager.getInstance().refreshAndFindFileByUrl("file://$repositoryPath")
                        val serviceVirtualFile =
                            VirtualFileManager.getInstance().refreshAndFindFileByUrl("file://$servicePath")
                        val diVirtualFile = VirtualFileManager.getInstance().refreshAndFindFileByUrl("file://$diPath")
                        if (repsVirtualFile != null && serviceVirtualFile != null && diVirtualFile != null) {
                            //放入xxxRepository和它的实现类xxxRepositoryImpl
                            if (!File(repositoryPath + modelName + "Repository.kt").exists()) {
                                val repositoryTemp =
                                    FileTemplateManager.getInstance(project).getInternalTemplate("TempRepository")
                                val repositoryProperties = Properties()
                                repositoryProperties["PACKAGE_NAME"] = getPackageName(repositoryPath)
                                repositoryProperties["INPUT_NAME"] = modelName
                                val repositoryPsiDir = psiManager.findDirectory(repsVirtualFile)
                                createTempCode(
                                    repositoryTemp,
                                    modelName + "Repository.kt",
                                    repositoryPsiDir!!,
                                    repositoryProperties
                                )
                            }
                            if (!File(repositoryPath + modelName + "RepositoryImpl.kt").exists()) {
                                val repositoryImplTemp =
                                    FileTemplateManager.getInstance(project).getInternalTemplate("TempRepositoryImpl")
                                val repositoryImplProperties = Properties()
                                repositoryImplProperties["PACKAGE_NAME"] = getPackageName(repositoryPath)
                                repositoryImplProperties["INPUT_NAME"] = modelName
                                repositoryImplProperties["INPUT_NAME_PARAM"] = subModelName
                                repositoryImplProperties["SERVICE_PATH"] = getPackageName(servicePath)
                                val repositoryPsiDir = psiManager.findDirectory(repsVirtualFile)
                                createTempCode(
                                    repositoryImplTemp,
                                    modelName + "RepositoryImpl.kt",
                                    repositoryPsiDir!!,
                                    repositoryImplProperties
                                )
                            }

                            //放入xxxService类
                            if (!File(servicePath + modelName + "Service.kt").exists()) {
                                val serviceTemp =
                                    FileTemplateManager.getInstance(project).getInternalTemplate("TempService")
                                val serviceProperties = Properties()
                                serviceProperties["PACKAGE_NAME"] = getPackageName(servicePath)
                                serviceProperties["INPUT_NAME"] = modelName
                                val servicePsiDir = psiManager.findDirectory(serviceVirtualFile)
                                createTempCode(
                                    serviceTemp,
                                    modelName + "Service.kt",
                                    servicePsiDir!!,
                                    serviceProperties
                                )
                            }

                            val dataModulePath = "$diPath/DataModule.kt"
                            val dataModuleFile = File(dataModulePath)
                            if (!dataModuleFile.exists()) {
                                val dataModuleTemp =
                                    FileTemplateManager.getInstance(project).getInternalTemplate("TempDataModule")
                                val dataModuleProperties = Properties()
                                dataModuleProperties["PACKAGE_NAME"] = getPackageName(diPath)
                                dataModuleProperties["REPOSITORY_PATH"] =
                                    getPackageName(repositoryPath) + "." + modelName + "Repository"
                                dataModuleProperties["REPOSITORY_IMPL_PATH"] =
                                    getPackageName(repositoryPath) + "." + modelName + "RepositoryImpl"
                                dataModuleProperties["INPUT_NAME"] = modelName
                                dataModuleProperties["INPUT_NAME_PARAM"] = subModelName
                                val dataModulePsiDir = psiManager.findDirectory(diVirtualFile)
                                createTempCode(
                                    dataModuleTemp,
                                    "DataModule.kt",
                                    dataModulePsiDir!!,
                                    dataModuleProperties
                                )
                            } else {
                                //1.导包
                                val currentDataModel =
                                    FileUtils.readFileToString(dataModuleFile, StandardCharsets.UTF_8)
                                val linesList = currentDataModel.replace("\r", "").split("\n")
                                if (linesList.first().contains("package")) {
                                    (linesList as ArrayList).add(
                                        2, "import " +
                                                getPackageName(repositoryPath) + "." + modelName + "Repository" + "\n"
                                    )
                                    linesList.add(
                                        3, "import " +
                                                getPackageName(repositoryPath) + "." + modelName + "RepositoryImpl" + "\n"
                                    )
                                    var lastLineIndex = 0
                                    val newContent = StringBuilder()
                                    for (i in linesList.size - 1 downTo 0) {
                                        if (linesList[i].trim() == "}") {
                                            lastLineIndex = i
                                            break
                                        }
                                    }
                                    //2.最后一个大括号删除
                                    linesList.removeAt(lastLineIndex)
                                    linesList.forEach {
                                        newContent.append(it)
                                        if (it == "" || !it.contains("\n")) {
                                            newContent.append("\n")
                                        }
                                    }
                                    FileUtils.writeStringToFile(
                                        dataModuleFile,
                                        newContent.toString() + "\n" + getDataModuleInjectContent(
                                            modelName,
                                            subModelName
                                        ),
                                        StandardCharsets.UTF_8
                                    )
                                }
                            }
                            val serviceModulePath = "$diPath/ServiceModule.kt"
                            val serviceModuleFile = File(serviceModulePath)
                            if (!serviceModuleFile.exists()) {
                                val serviceModuleTemp =
                                    FileTemplateManager.getInstance(project)
                                        .getInternalTemplate("TempServiceModule")
                                val serviceModuleProperties = Properties()
                                serviceModuleProperties["PACKAGE_NAME"] = getPackageName(diPath)
                                serviceModuleProperties["SERVICE_PATH"] =
                                    getPackageName(servicePath) + "." + modelName + "Service"
                                serviceModuleProperties["INPUT_NAME"] = modelName
                                val serviceModulePsiDir = psiManager.findDirectory(diVirtualFile)
                                createTempCode(
                                    serviceModuleTemp,
                                    "ServiceModule.kt",
                                    serviceModulePsiDir!!,
                                    serviceModuleProperties
                                )
                            } else {
                                //1.导包
                                val currentServiceModel =
                                    FileUtils.readFileToString(serviceModuleFile, StandardCharsets.UTF_8)
                                val linesList = currentServiceModel.replace("\r", "").split("\n")
                                if (linesList.first().contains("package")) {
                                    (linesList as ArrayList).add(
                                        2, "import " +
                                                getPackageName(servicePath) + "." + modelName + "Service" + "\n"
                                    )
                                    var lastIndex = 0
                                    val newContent = StringBuilder()
                                    for (i in linesList.size - 1 downTo 0) {
                                        if (linesList[i].trim() == "}") {
                                            lastIndex = i
                                            break
                                        }
                                    }
                                    //2.最后一个大括号删除
                                    linesList.removeAt(lastIndex)
                                    linesList.forEach {
                                        newContent.append(it)
                                        if (it == "" || !it.contains("\n")) {
                                            newContent.append("\n")
                                        }
                                    }
                                    FileUtils.writeStringToFile(
                                        serviceModuleFile,
                                        newContent.toString() + "\n" + getServiceModuleInjectContent(
                                            modelName
                                        ),
                                        StandardCharsets.UTF_8
                                    )
                                }
                            }
                        }
                        //生成模板viewModel文件
                        val viewModelTemp =
                            FileTemplateManager.getInstance(this).getInternalTemplate("TempViewModelWithRepository")
                        val viewModelProperties = Properties()
                        viewModelProperties.putAll(FileTemplateManager.getInstance(this).defaultProperties)
                        viewModelProperties["PACKAGE_NAME"] = getPackageName(path)
                        viewModelProperties["REPOSITORY_PATH"] =
                            getPackageName(repositoryPath) + "." + modelName + "Repository"
                        viewModelProperties["INPUT_NAME"] = modelName
                        viewModelProperties["INPUT_NAME_PARAM"] = subModelName
                        createTempCode(
                            viewModelTemp,
                            modelName + "ViewModel.kt",
                            LangDataKeys.IDE_VIEW.getData(dataContext)!!.orChooseDirectory!!,
                            viewModelProperties
                        )
                    } else {
                        //生成模板viewModel文件
                        val viewModelTemp = FileTemplateManager.getInstance(this).getInternalTemplate("TempViewModel")
                        val viewModelProperties = Properties()
                        viewModelProperties.putAll(FileTemplateManager.getInstance(this).defaultProperties)
                        viewModelProperties["PACKAGE_NAME"] = getPackageName(path)
                        viewModelProperties["INPUT_NAME"] = modelName
                        createTempCode(
                            viewModelTemp,
                            modelName + "ViewModel.kt",
                            LangDataKeys.IDE_VIEW.getData(dataContext)!!.orChooseDirectory!!,
                            viewModelProperties
                        )
                    }

                    //生成模板xml文件
                    val xmlFile = File(getXmlPath(path) + "/" + modelPath + "_activity_" + getXmlEndName(modelName) + ".xml")
                    if (!xmlFile.exists()) {
                        val xmlTemp = FileTemplateManager.getInstance(this).getInternalTemplate("TempXml")
                        val xmlProperties = Properties()
                        xmlProperties["PACKAGE_NAME"] = getPackageName(path)
                        xmlProperties["INPUT_NAME"] = modelName
                        val xmlPsiDir = psiManager
                            .findDirectory(VirtualFileManager.getInstance().findFileByUrl("file://" + getXmlPath(path))!!)
                        createTempCode(
                            xmlTemp,
                            modelPath + "_activity_" + getXmlEndName(modelName),
                            xmlPsiDir!!,
                            xmlProperties
                        )
                    }

                    LocalFileSystem.getInstance().refresh(false)
                }
            }
        })
    }


    override fun update(event: AnActionEvent) {
        //获取工程对象
        val project: Project? = event.project
        //设置不可用且不可见
        event.presentation.isEnabledAndVisible = project != null
    }

    private fun createTempCode(
        temp: FileTemplate,
        fileName: String,
        psiDir: PsiDirectory,
        properties: Properties
    ) {
        ApplicationManager.getApplication().runWriteAction {
            FileTemplateUtil.createFromTemplate(
                temp,
                fileName,
                properties,
                psiDir
            )
            LocalFileSystem.getInstance().refresh(false)
        }
    }

    private fun getXmlEndName(name: String): String {
        val result = StringBuilder()
        name.toCharArray().forEachIndexed { index, it ->
            if (it.isUpperCase() && index > 0) {
                result.append("_$it")
            } else {
                result.append(it)
            }
        }
        return result.toString().lowercase()
    }

    private fun getDataModuleInjectContent(name: String, nameParam: String): String {
        try {
            val writeContent = StringBuilder("")
            val tempFileStream = javaClass.getResourceAsStream("/TempDataModuleParts.txt")
            val reader = tempFileStream?.let { InputStreamReader(it) }?.let { BufferedReader(it) }
            var line: String
            reader?.run {
                while ((reader.readLine().also {
                        line = it ?: ""
                        if (line.contains("&input_name&")) {
                            line = line.replace("&input_name&", name)
                        }
                        if (line.contains("&input_name_param&")) {
                            line = line.replace("&input_name_param&", nameParam)
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

    private fun getServiceModuleInjectContent(name: String): String {
        try {
            val writeContent = StringBuilder("")
            val tempFileStream = javaClass.getResourceAsStream("/TempServiceModuleParts.txt")
            val reader = tempFileStream?.let { InputStreamReader(it) }?.let { BufferedReader(it) }
            var line: String
            reader?.run {
                while ((reader.readLine().also {
                        line = it ?: ""
                        if (line.contains("&input_name&")) {
                            line = line.replace("&input_name&", name)
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