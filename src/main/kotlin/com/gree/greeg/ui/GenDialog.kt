@file:Suppress("PrivatePropertyName")

package com.gree.greeg.ui

import com.gree.greeg.OnFillListener
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.ConfigurableName
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

/**
 * @Author GZDong
 * @Date 2023/7/3
 */
@Suppress("unused")
class GenDialog : JDialog(), Configurable {
    private var project: Project? = null
    private var dataContext: DataContext? = null
    private var contentPanel: JPanel? = null
    private var title: JLabel? = null
    private var activityTips: JLabel? = null
    private var v_activity: JTextField? = null
    private var pathTips: JLabel? = null
    private var v_path: JTextField? = null
    private var repositoryTips: JCheckBox? = null
    private var btnCreate: JButton? = null
    private var errorTips: JLabel? = null
    private var closeListener: OnFillListener? = null

    init {
        contentPane = contentPanel
    }

    fun initView() {
        val screen = toolkit.screenSize
        val width = screen.width / 3
        val height = 250
        val x = screen.width / 2 - width / 2
        val y = screen.height / 2 - height / 2
        setLocation(x, y)
        contentPanel!!.preferredSize = Dimension(width, height)
        setTitle("Create Template")
        // 点击 X 时调用 onCancel()
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        // 遇到 ESCAPE 时调用 onCancel()
        contentPanel!!.registerKeyboardAction(
            { onCancel() }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        )
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                onCancel()
            }
        })
        btnCreate?.addActionListener {
            val tempName = v_activity?.text
            val routerPath = v_path?.text
            val createRepository = repositoryTips?.isSelected ?: true
            if (tempName.isNullOrBlank() || routerPath.isNullOrBlank()) {
                errorTips?.isVisible = true
                return@addActionListener
            }
            closeListener?.onFinished(tempName, routerPath, createRepository)
            onCancel()
        }
        errorTips?.isVisible = false
    }

    private fun onCancel() {
        dispose()
    }

    @Suppress("UnstableApiUsage")
    override fun getDisplayName(): @ConfigurableName String {
        return "GenDisplayName"
    }

    override fun createComponent(): JComponent? {
        return contentPanel
    }

    override fun isModified(): Boolean {
        return true
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
    }

    companion object {
        fun show(listener: OnFillListener?) {
            val genDialog = GenDialog()
            genDialog.closeListener = listener
            genDialog.isModal = true
            genDialog.initView()
            genDialog.pack()
            genDialog.isVisible = true
        }
    }
}