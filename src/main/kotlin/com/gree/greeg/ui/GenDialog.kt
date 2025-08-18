@file:Suppress("PrivatePropertyName")

package com.gree.greeg.ui

import com.gree.greeg.OnFillListener
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.ConfigurableName
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
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
        // 初始化contentPanel和UI组件
        contentPanel = JPanel()
        contentPanel?.layout = GridBagLayout()
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = java.awt.Insets(5, 10, 5, 10)
        gbc.weightx = 1.0

        // 初始化UI组件
        title = JLabel("输入相关信息")
        title?.font = java.awt.Font("SimHei", java.awt.Font.BOLD, 16)
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.anchor = GridBagConstraints.CENTER
        contentPanel?.add(title, gbc)

        // 重置GridBagConstraints
        gbc.gridwidth = 1
        gbc.anchor = GridBagConstraints.WEST

        activityTips = JLabel("输入activity的类名 (比如TestActivity, 或者Test、testActivity、test)")
        gbc.gridy = 1
        contentPanel?.add(activityTips, gbc)

        v_activity = JTextField(20)
        gbc.gridy = 2
        gbc.gridwidth = GridBagConstraints.REMAINDER
        contentPanel?.add(v_activity, gbc)

        pathTips = JLabel("输入router path (比如app/order) 用于Router跳转")
        gbc.gridy = 3
        gbc.gridwidth = 1
        contentPanel?.add(pathTips, gbc)

        v_path = JTextField(20)
        gbc.gridy = 4
        gbc.gridwidth = GridBagConstraints.REMAINDER
        contentPanel?.add(v_path, gbc)

        repositoryTips = JCheckBox("是否自动生成repository?")
        repositoryTips?.isSelected = true
        gbc.gridy = 5
        gbc.gridwidth = GridBagConstraints.REMAINDER
        contentPanel?.add(repositoryTips, gbc)

        // 创建按钮并设置为拉满宽度
        btnCreate = JButton("创建")
        gbc.gridy = 6
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = java.awt.Insets(15, 30, 5, 30)  // 增加左右边距
        contentPanel?.add(btnCreate, gbc)

        // 错误提示标签
        errorTips = JLabel("*请输入完整信息!")
        errorTips?.foreground = java.awt.Color.RED
        errorTips?.isVisible = false
        gbc.gridy = 7
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.CENTER
        gbc.insets = java.awt.Insets(5, 10, 25, 10)  // 增加下边距
        contentPanel?.add(errorTips, gbc)
    }

    fun initView() {
        // 设置contentPane
        contentPane = contentPanel
        // 设置对话框位置和大小
        val screen = toolkit.screenSize
        val width = screen.width / 3
        val height = 280
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
            var tempName = v_activity?.text
            val routerPath = v_path?.text
            val createRepository = repositoryTips?.isSelected ?: true
            if (tempName.isNullOrBlank() || routerPath.isNullOrBlank()) {
                errorTips?.isVisible = true
                errorTips?.text = "*请输入完整信息!"
                errorTips?.repaint()  // 强制重绘以确保显示
                return@addActionListener
            }
            if (tempName.first().isLowerCase()) {
                tempName =
                        tempName.replaceFirst(tempName.first(), tempName.first().uppercase().toCharArray().first(), false)
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