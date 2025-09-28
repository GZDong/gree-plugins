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
    private var composeOrTraditionalComposeRadio: JRadioButton? = null
    private var composeOrTraditionalTraditionalRadio: JRadioButton? = null
    private var composeOrTraditionalGroup: ButtonGroup? = null
    private var composeTypePageRadio: JRadioButton? = null
    private var composeTypeActivityRadio: JRadioButton? = null
    private var composeTypeGroup: ButtonGroup? = null
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
        // Default insets for most items (will be overridden below as needed)
        gbc.insets = java.awt.Insets(5, 20, 5, 20)
        gbc.weightx = 1.0

        // 初始化UI组件
        title = JLabel("输入相关信息")
        title?.font = java.awt.Font("SimHei", java.awt.Font.BOLD, 16)
        // Center the text in the label itself
        title?.horizontalAlignment = SwingConstants.CENTER
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.anchor = GridBagConstraints.CENTER
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.insets = java.awt.Insets(10, 0, 10, 0) // Top, left, bottom, right for header
        contentPanel?.add(title, gbc)

        // 重置GridBagConstraints
        gbc.gridwidth = 1
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = java.awt.Insets(5, 20, 5, 20) // set default for content area

        // 添加 Compose版本 和 传统Activity界面 单选按钮组
        composeOrTraditionalComposeRadio = JRadioButton("Compose版本")
        composeOrTraditionalTraditionalRadio = JRadioButton("传统Activity界面")
        composeOrTraditionalGroup = ButtonGroup()
        composeOrTraditionalGroup?.add(composeOrTraditionalComposeRadio)
        composeOrTraditionalGroup?.add(composeOrTraditionalTraditionalRadio)
        composeOrTraditionalComposeRadio?.isSelected = true
        gbc.gridy = 1
        gbc.insets = java.awt.Insets(5, 20, 5, 20)
        contentPanel?.add(composeOrTraditionalComposeRadio, gbc)
        gbc.gridx = 1
        contentPanel?.add(composeOrTraditionalTraditionalRadio, gbc)
        gbc.gridx = 0

        // 添加 Compose/Page/Activity 单选按钮组
        composeTypePageRadio = JRadioButton("1. Page（单Activity页面跳转，推荐）")
        composeTypeActivityRadio = JRadioButton("2. 创建的独立Activity")
        composeTypeGroup = ButtonGroup()
        composeTypeGroup?.add(composeTypePageRadio)
        composeTypeGroup?.add(composeTypeActivityRadio)
        composeTypePageRadio?.isSelected = true
        gbc.gridy = 2
        gbc.insets = java.awt.Insets(5, 20, 5, 20)
        contentPanel?.add(composeTypePageRadio, gbc)
        gbc.gridy = 3
        contentPanel?.add(composeTypeActivityRadio, gbc)
        gbc.gridx = 0

        // 添加监听，控制 composeTypeGroup 显示与隐藏
        val toggleComposeTypeVisibility = {
            val isComposeSelected = composeOrTraditionalComposeRadio?.isSelected ?: true
            composeTypePageRadio?.isVisible = isComposeSelected
            composeTypeActivityRadio?.isVisible = isComposeSelected
        }
        composeOrTraditionalComposeRadio?.addActionListener { toggleComposeTypeVisibility() }
        composeOrTraditionalTraditionalRadio?.addActionListener { toggleComposeTypeVisibility() }
        toggleComposeTypeVisibility()

        // 输入 activity 名称
        // 输入名称标签
        val activityNameLabel = JLabel("输入名称")
        gbc.gridy = 4
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = java.awt.Insets(5, 20, 0, 20) // reduce bottom margin, closer to input
        contentPanel?.add(activityNameLabel, gbc)

        v_activity = JTextField(20)
        gbc.gridy = 5
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.insets = java.awt.Insets(0, 20, 5, 20) // no top margin, closer to label
        contentPanel?.add(v_activity, gbc)

        // activityTips 标签，更新为两行描述，放置在 v_activity 之后
        activityTips = JLabel("<html><i>1. 创建Compose Page，请输入想要的页面名称即可，比如MainPage、mainPage、main。<br/>2. 创建Activity版本，请输入Activity名称，比如TestActivity，或者Test、testActivity、test。</i></html>")
        gbc.gridy = 6
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = java.awt.Insets(0, 20, 10, 20)
        contentPanel?.add(activityTips, gbc)

        // 输入路由地址标签
        val pathLabel = JLabel("输入路由地址")
        gbc.gridy = 7
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = java.awt.Insets(5, 20, 0, 20) // reduce bottom margin, closer to input
        contentPanel?.add(pathLabel, gbc)

        v_path = JTextField(20)
        gbc.gridy = 8
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.insets = java.awt.Insets(0, 20, 5, 20) // no top margin, closer to label
        contentPanel?.add(v_path, gbc)

        // pathTips 标签，放置在 v_path 之后
        pathTips = JLabel("<html><i>输入router path (比如app/order) 用于Router跳转</i></html>")
        gbc.gridy = 9
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.insets = java.awt.Insets(0, 20, 10, 20)
        contentPanel?.add(pathTips, gbc)

        repositoryTips = JCheckBox("是否自动生成repository?")
        repositoryTips?.isSelected = true
        gbc.gridy = 10
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.insets = java.awt.Insets(5, 20, 5, 20)
        contentPanel?.add(repositoryTips, gbc)

        // 创建按钮并设置为拉满宽度
        btnCreate = JButton("创建")
        gbc.gridy = 11
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = java.awt.Insets(10, 30, 5, 30)
        contentPanel?.add(btnCreate, gbc)

        // 错误提示标签
        errorTips = JLabel("*请输入完整信息!")
        errorTips?.foreground = java.awt.Color.RED
        errorTips?.isVisible = false
        gbc.gridy = 12
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.CENTER
        gbc.insets = java.awt.Insets(5, 10, 15, 10)
        contentPanel?.add(errorTips, gbc)
    }

    fun initView() {
        // 设置contentPane
        contentPane = contentPanel
        // 设置对话框位置和大小
        val screen = toolkit.screenSize
        val width = screen.width / 3
        val height = 470
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
            try {
                closeListener?.onFinished(
                        v_activity?.text ?: "",
                        v_path?.text ?: "",
                        repositoryTips?.isSelected ?: true,
                        composeOrTraditionalComposeRadio?.isSelected ?: true,
                        composeTypePageRadio?.isSelected ?: true
                )
            } finally {
                onCancel()
            }
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
        private var lastIsCompose: Boolean = true
        private var lastIsPage: Boolean = true

        fun show(listener: OnFillListener?) {
            val genDialog = GenDialog()
            genDialog.closeListener = listener
            genDialog.isModal = true
            genDialog.initView()
            genDialog.pack()
            genDialog.isVisible = true
        }

        fun getIsCompose(): Boolean = lastIsCompose
        fun getIsPage(): Boolean = lastIsPage
    }
}