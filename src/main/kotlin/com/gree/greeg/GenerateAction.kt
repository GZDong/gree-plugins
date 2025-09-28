package com.gree.greeg

import com.gree.greeg.genlib.getXmlInjectContent
import com.gree.greeg.ui.GenDialog
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.generation.actions.BaseGenerateAction
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.actionSystem.AnAction
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
            override fun onFinished(
                    tempActivityName: String,
                    tempPath: String,
                    chooseRep: Boolean,
                    isComposeVersion: Boolean,
                    isPageSelected: Boolean,
                    followStructure: Boolean
            ) {
                project?.run {
                    // 1. Normalize names based on mode
                    val tempName =
                            if (tempActivityName.endsWith("Activity")) tempActivityName.removeSuffix("Activity") else tempActivityName
                    val modelName = tempName // for INPUT_NAME
                    val isPage = isComposeVersion && isPageSelected
                    // normalizedName now uses isPage defined below
                    val normalizedName: String
                    if (isComposeVersion && isPage) {
                        var name = modelName.replaceFirstChar { it.uppercaseChar() }
                        normalizedName = if (name.endsWith("Page", ignoreCase = true)) {
                            name.removeSuffix("page").removeSuffix("Page") + "Page"
                        } else {
                            name + "Page"
                        }
                    } else {
                        val name = modelName.replaceFirstChar { it.uppercaseChar() }
                        normalizedName = if (name.endsWith("Activity", ignoreCase = true)) {
                            name.removeSuffix("activity").removeSuffix("Activity") + "Activity"
                        } else {
                            name + "Activity"
                        }
                    }
                    // 2. Base name
                    val baseName = if (normalizedName.endsWith("Page")) {
                        normalizedName.removeSuffix("Page")
                    } else {
                        normalizedName.removeSuffix("Activity")
                    }
                    val modelPath = getModelName(path)
                    val activityProperties = Properties()
                    activityProperties.putAll(FileTemplateManager.getInstance(this).defaultProperties)
                    activityProperties["PACKAGE_NAME"] = getPackageName(path)
                    activityProperties["ROUTER_PATH"] = tempPath
                    activityProperties["INPUT_NAME"] = baseName
                    // 3. File/template selection and target file name (refactored)
                    val activityTemp: FileTemplate
                    val targetFileName: String
                    if (isComposeVersion) {
                        if (isPage) {
                            activityTemp = FileTemplateManager.getInstance(this).getInternalTemplate("TempCompPage")
                            targetFileName = "$normalizedName.kt"
                        } else {
                            activityTemp = FileTemplateManager.getInstance(this).getInternalTemplate("TempCompActivity")
                            targetFileName = "$normalizedName.kt"
                        }
                    } else {
                        val bindingName = modelPath.replaceFirst(
                                modelPath.first(),
                                modelPath.first().uppercase().toCharArray().first(),
                                false
                        )
                        activityProperties["MODEL_NAME"] = modelPath
                        activityProperties["BINDING"] = bindingName
                        activityProperties["L_MODEL_NAME"] = modelPath.lowercase()
                        activityProperties["LAYOUT"] = getXmlEndName(baseName)
                        activityTemp = FileTemplateManager.getInstance(this).getInternalTemplate("TempActivity")
                        targetFileName = "$normalizedName.kt"
                    }
                    createTempCode(
                            activityTemp,
                            targetFileName,
                            LangDataKeys.IDE_VIEW.getData(dataContext)!!.orChooseDirectory!!,
                            activityProperties
                    )

                    // 如果不是Compose版本，生成XML布局文件
                    if (!isComposeVersion) {
                        // 生成XML布局文件
                        val xmlPath = getXmlPath(path)
                        val xmlFile = File(xmlPath)
                        if (!xmlFile.exists()) {
                            xmlFile.parentFile?.mkdirs()
                            val xmlContent = getXmlInjectContent(this as AnAction, path, baseName)
                            FileUtils.writeStringToFile(xmlFile, xmlContent, StandardCharsets.UTF_8)
                            LocalFileSystem.getInstance().refresh(false)
                        }
                    }
                    val psiManager = PsiManager.getInstance(project)
                    // 5. Repository/service/DI generation
                    if (chooseRep) {
                        if (followStructure) {
                            // 原有逻辑：遵循目录结构，可能追加
                            val diPath = getModelPath(modelPath, path) + "/di"
                            val diDir = File(diPath)
                            if (!diDir.exists()) {
                                diDir.mkdir()
                            }
                            val subModelName = baseName.replaceFirst(
                                baseName.first(),
                                baseName.first().lowercase().toCharArray()[0],
                                false
                            )
                            val dataPath = getModelPath(modelPath, path) + "/data"
                            val dataDir = File(dataPath)
                            if (!dataDir.exists()) {
                                dataDir.mkdir()
                            }
                            val repositoryPath = "$dataPath/repository"
                            val repositoryDir = File(repositoryPath)
                            if (!repositoryDir.exists()) {
                                repositoryDir.mkdir()
                            }
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
                                // 保持原有的 Repository/Service/DataModule/ServiceModule 生成与追加逻辑
                                if (!File(repositoryPath + baseName + "Repository.kt").exists()) {
                                    val repositoryTemp =
                                        FileTemplateManager.getInstance(project).getInternalTemplate("TempRepository")
                                    val repositoryProperties = Properties()
                                    repositoryProperties["PACKAGE_NAME"] = getPackageName(repositoryPath)
                                    repositoryProperties["INPUT_NAME"] = baseName
                                    val repositoryPsiDir = psiManager.findDirectory(repsVirtualFile)
                                    createTempCode(
                                        repositoryTemp,
                                        baseName + "Repository.kt",
                                        repositoryPsiDir!!,
                                        repositoryProperties
                                    )
                                }
                                if (!File(repositoryPath + baseName + "RepositoryImpl.kt").exists()) {
                                    val repositoryImplTemp =
                                        FileTemplateManager.getInstance(project).getInternalTemplate("TempRepositoryImpl")
                                    val repositoryImplProperties = Properties()
                                    repositoryImplProperties["PACKAGE_NAME"] = getPackageName(repositoryPath)
                                    repositoryImplProperties["INPUT_NAME"] = baseName
                                    repositoryImplProperties["INPUT_NAME_PARAM"] = subModelName
                                    repositoryImplProperties["SERVICE_PATH"] = getPackageName(servicePath)
                                    val repositoryPsiDir = psiManager.findDirectory(repsVirtualFile)
                                    createTempCode(
                                        repositoryImplTemp,
                                        baseName + "RepositoryImpl.kt",
                                        repositoryPsiDir!!,
                                        repositoryImplProperties
                                    )
                                }
                                if (!File(servicePath + baseName + "Service.kt").exists()) {
                                    val serviceTemp =
                                        FileTemplateManager.getInstance(project).getInternalTemplate("TempService")
                                    val serviceProperties = Properties()
                                    serviceProperties["PACKAGE_NAME"] = getPackageName(servicePath)
                                    serviceProperties["INPUT_NAME"] = baseName
                                    val servicePsiDir = psiManager.findDirectory(serviceVirtualFile)
                                    createTempCode(
                                        serviceTemp,
                                        baseName + "Service.kt",
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
                                        getPackageName(repositoryPath) + "." + baseName + "Repository"
                                    dataModuleProperties["REPOSITORY_IMPL_PATH"] =
                                        getPackageName(repositoryPath) + "." + baseName + "RepositoryImpl"
                                    dataModuleProperties["INPUT_NAME"] = baseName
                                    dataModuleProperties["INPUT_NAME_PARAM"] = subModelName
                                    val dataModulePsiDir = psiManager.findDirectory(diVirtualFile)
                                    createTempCode(
                                        dataModuleTemp,
                                        "DataModule.kt",
                                        dataModulePsiDir!!,
                                        dataModuleProperties
                                    )
                                } else {
                                    val currentDataModel =
                                        FileUtils.readFileToString(dataModuleFile, StandardCharsets.UTF_8)
                                    val linesList = currentDataModel.replace("\r", "").split("\n")
                                    if (linesList.first().contains("package")) {
                                        (linesList as ArrayList).add(
                                            2, "import " +
                                                getPackageName(repositoryPath) + "." + baseName + "Repository" + "\n"
                                        )
                                        linesList.add(
                                            3, "import " +
                                                getPackageName(repositoryPath) + "." + baseName + "RepositoryImpl" + "\n"
                                        )
                                        var lastLineIndex = 0
                                        val newContent = StringBuilder()
                                        for (i in linesList.size - 1 downTo 0) {
                                            if (linesList[i].trim() == "}") {
                                                lastLineIndex = i
                                                break
                                            }
                                        }
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
                                                baseName,
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
                                        getPackageName(servicePath) + "." + baseName + "Service"
                                    serviceModuleProperties["INPUT_NAME"] = baseName
                                    val serviceModulePsiDir = psiManager.findDirectory(diVirtualFile)
                                    createTempCode(
                                        serviceModuleTemp,
                                        "ServiceModule.kt",
                                        serviceModulePsiDir!!,
                                        serviceModuleProperties
                                    )
                                } else {
                                    val currentServiceModel =
                                        FileUtils.readFileToString(serviceModuleFile, StandardCharsets.UTF_8)
                                    val linesList = currentServiceModel.replace("\r", "").split("\n")
                                    if (linesList.first().contains("package")) {
                                        (linesList as ArrayList).add(
                                            2, "import " +
                                                getPackageName(servicePath) + "." + baseName + "Service" + "\n"
                                        )
                                        var lastIndex = 0
                                        val newContent = StringBuilder()
                                        for (i in linesList.size - 1 downTo 0) {
                                            if (linesList[i].trim() == "}") {
                                                lastIndex = i
                                                break
                                            }
                                        }
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
                                                baseName
                                            ),
                                            StandardCharsets.UTF_8
                                        )
                                    }
                                }
                            }
                        } else {
                            // 未遵循目录结构逻辑：全部新建，放在当前目录
                            val targetDir = LangDataKeys.IDE_VIEW.getData(dataContext)!!.orChooseDirectory!!
                            val subModelName = baseName.replaceFirst(
                                baseName.first(),
                                baseName.first().lowercase().toCharArray()[0],
                                false
                            )

                            // Repository
                            val repositoryTemp = FileTemplateManager.getInstance(project).getInternalTemplate("TempRepository")
                            val repositoryProperties = Properties()
                            repositoryProperties["PACKAGE_NAME"] = getPackageName(path)
                            repositoryProperties["INPUT_NAME"] = baseName
                            createTempCode(repositoryTemp, baseName + "Repository.kt", targetDir, repositoryProperties)

                            // RepositoryImpl
                            val repositoryImplTemp = FileTemplateManager.getInstance(project).getInternalTemplate("TempRepositoryImpl")
                            val repositoryImplProperties = Properties()
                            repositoryImplProperties["PACKAGE_NAME"] = getPackageName(path)
                            repositoryImplProperties["INPUT_NAME"] = baseName
                            repositoryImplProperties["INPUT_NAME_PARAM"] = subModelName
                            repositoryImplProperties["SERVICE_PATH"] = getPackageName(path)
                            createTempCode(repositoryImplTemp, baseName + "RepositoryImpl.kt", targetDir, repositoryImplProperties)

                            // Service
                            val serviceTemp = FileTemplateManager.getInstance(project).getInternalTemplate("TempService")
                            val serviceProperties = Properties()
                            serviceProperties["PACKAGE_NAME"] = getPackageName(path)
                            serviceProperties["INPUT_NAME"] = baseName
                            createTempCode(serviceTemp, baseName + "Service.kt", targetDir, serviceProperties)

                            // DataModule
                            val dataModuleTemp = FileTemplateManager.getInstance(project).getInternalTemplate("TempDataModule")
                            val dataModuleProperties = Properties()
                            dataModuleProperties["PACKAGE_NAME"] = getPackageName(path)
                            dataModuleProperties["REPOSITORY_PATH"] = getPackageName(path) + "." + baseName + "Repository"
                            dataModuleProperties["REPOSITORY_IMPL_PATH"] = getPackageName(path) + "." + baseName + "RepositoryImpl"
                            dataModuleProperties["INPUT_NAME"] = baseName
                            dataModuleProperties["INPUT_NAME_PARAM"] = subModelName
                            createTempCode(dataModuleTemp, "DataModule.kt", targetDir, dataModuleProperties)

                            // ServiceModule
                            val serviceModuleTemp = FileTemplateManager.getInstance(project).getInternalTemplate("TempServiceModule")
                            val serviceModuleProperties = Properties()
                            serviceModuleProperties["PACKAGE_NAME"] = getPackageName(path)
                            serviceModuleProperties["SERVICE_PATH"] = getPackageName(path) + "." + baseName + "Service"
                            serviceModuleProperties["INPUT_NAME"] = baseName
                            createTempCode(serviceModuleTemp, "ServiceModule.kt", targetDir, serviceModuleProperties)
                        }
                    }
                    // 6. ViewModel/UiState generation (refactored)
                    val targetDir = LangDataKeys.IDE_VIEW.getData(dataContext)!!.orChooseDirectory!!
                    val viewModelProperties = Properties()
                    viewModelProperties.putAll(FileTemplateManager.getInstance(this).defaultProperties)
                    viewModelProperties["PACKAGE_NAME"] = getPackageName(path)
                    // Compose/Page/Activity/OldActivity unified branching for ViewModel/UiState
                    if (isComposeVersion) {
                        if (isPage) {
                            // Page mode: ViewModel + UiState
                            val vmName = baseName + "ViewModel"
                            val viewModelTemp = FileTemplateManager.getInstance(this).getInternalTemplate(
                                    if (chooseRep) "TempCompViewModelWithRepository" else "TempCompViewModel"
                            )
                            viewModelProperties["INPUT_NAME"] = baseName
                            if (chooseRep) {
                                val repositoryPath = getModelPath(modelPath, path) + "/data/repository"
                                val subModelName = baseName.replaceFirst(
                                        baseName.first(),
                                        baseName.first().lowercase().toCharArray()[0],
                                        false
                                )
                                viewModelProperties["REPOSITORY_PATH"] = getPackageName(repositoryPath) + "." + baseName + "Repository"
                                viewModelProperties["INPUT_NAME_PARAM"] = subModelName
                            }
                            createTempCode(viewModelTemp, "$vmName.kt", targetDir, viewModelProperties)

                            val uiStateName = baseName + "UiState"
                            val uiStateProperties = Properties()
                            uiStateProperties["INPUT_NAME"] = uiStateName
                            val uiStateTemp = FileTemplateManager.getInstance(this).getInternalTemplate("TempCompUiState")
                            createTempCode(uiStateTemp, "$uiStateName.kt", targetDir, uiStateProperties)
                        } else {
                            // Compose Activity (旧逻辑，不生成 UiState)
                            val vmName = baseName + "ViewModel"
                            val viewModelTemp = FileTemplateManager.getInstance(this).getInternalTemplate(
                                    if (chooseRep) "TempCompViewModelWithRepository" else "TempCompViewModel"
                            )
                            viewModelProperties["INPUT_NAME"] = baseName
                            if (chooseRep) {
                                val repositoryPath = getModelPath(modelPath, path) + "/data/repository"
                                val subModelName = baseName.replaceFirst(
                                        baseName.first(),
                                        baseName.first().lowercase().toCharArray()[0],
                                        false
                                )
                                viewModelProperties["REPOSITORY_PATH"] = getPackageName(repositoryPath) + "." + baseName + "Repository"
                                viewModelProperties["INPUT_NAME_PARAM"] = subModelName
                            }
                            createTempCode(viewModelTemp, "$vmName.kt", targetDir, viewModelProperties)
                        }
                    } else {
                        // 传统 Activity (旧逻辑)
                        val vmName = baseName + "ViewModel"
                        val viewModelTemp = FileTemplateManager.getInstance(this).getInternalTemplate(
                                if (chooseRep) "TempViewModelWithRepository" else "TempViewModel"
                        )
                        viewModelProperties["INPUT_NAME"] = baseName
                        if (chooseRep) {
                            val repositoryPath = getModelPath(modelPath, path) + "/data/repository"
                            val subModelName = baseName.replaceFirst(
                                    baseName.first(),
                                    baseName.first().lowercase().toCharArray()[0],
                                    false
                            )
                            viewModelProperties["REPOSITORY_PATH"] = getPackageName(repositoryPath) + "." + baseName + "Repository"
                            viewModelProperties["INPUT_NAME_PARAM"] = subModelName
                        }
                        createTempCode(viewModelTemp, "$vmName.kt", targetDir, viewModelProperties)
                    }

                    // 7. 非Compose版本才生成XML模板
                    if (!isComposeVersion) {
                        val xmlFile = File(getXmlPath(path) + "/" + modelPath + "_activity_" + getXmlEndName(baseName) + ".xml")
                        if (!xmlFile.exists()) {
                            val xmlTemp = FileTemplateManager.getInstance(this).getInternalTemplate("TempXml")
                            val xmlProperties = Properties()
                            xmlProperties["PACKAGE_NAME"] = getPackageName(path)
                            xmlProperties["INPUT_NAME"] = baseName
                            val xmlPsiDir = psiManager
                                    .findDirectory(VirtualFileManager.getInstance().findFileByUrl("file://" + getXmlPath(path))!!)
                            createTempCode(
                                    xmlTemp,
                                    modelPath + "_activity_" + getXmlEndName(baseName),
                                    xmlPsiDir!!,
                                    xmlProperties
                            )
                        }
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