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
import java.util.*

class GenerateAction(handler: CodeInsightActionHandler? = null) : BaseGenerateAction(handler) {

    override fun isValidForClass(targetClass: PsiClass?): Boolean {
        return super.isValidForClass(targetClass)
    }

    override fun actionPerformed(event: AnActionEvent) {

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

                    //生成模板xml文件
                    val xmlTemp = FileTemplateManager.getInstance(this).getInternalTemplate("TempXml")
                    val xmlProperties = Properties()
                    xmlProperties["PACKAGE_NAME"] = getPackageName(path)
                    xmlProperties["INPUT_NAME"] = modelName
                    val xmlPsiDir = PsiManager.getInstance(project)
                        .findDirectory(VirtualFileManager.getInstance().findFileByUrl("file://" + getXmlPath(path))!!)
                    createTempCode(
                        xmlTemp,
                        modelPath + "_activity_" + getXmlEndName(modelName),
                        xmlPsiDir!!,
                        xmlProperties
                    )

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

}