package com.gree.greeg

import com.intellij.icons.AllIcons
import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import javax.swing.Icon

/**
 * @Author GZDong
 * @Date 2023/7/3
 */
class FileTemplateGroup: FileTemplateGroupDescriptorFactory {


    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor? {
        val descriptor = FileTemplateGroupDescriptor("Module Template Plugin Descriptor", AllIcons.Nodes.Plugin)
        descriptor.run {
            addTemplate(FileTemplateDescriptor("TempActivity.kt", getFileIconByExt("kt")))
            addTemplate(FileTemplateDescriptor("TempViewModel.kt", getFileIconByExt("kt")))
            addTemplate(FileTemplateDescriptor("TempXml.xml", getFileIconByExt("xml")))
        }
        return null
    }

    private fun getFileIconByExt(ext: String): Icon {
        var icon = FileTypeManager.getInstance().getFileTypeByExtension(ext).icon
        if (icon == null) {
            icon = AllIcons.FileTypes.Unknown
        }
        return icon
    }
}