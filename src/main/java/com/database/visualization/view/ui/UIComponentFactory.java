package com.database.visualization.view.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * UI组件工厂类，负责创建界面组件
 */
public class UIComponentFactory {

    private boolean isDarkTheme;

    public UIComponentFactory(boolean isDarkTheme) {
        this.isDarkTheme = isDarkTheme;
    }

    /**
     * 创建菜单栏
     */
    public JMenuBar createMenuBar(ActionListener... listeners) {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        JMenuItem newConnectionItem = new JMenuItem("新建连接");
        JMenuItem importItem = new JMenuItem("导入连接");
        JMenuItem exportItem = new JMenuItem("导出连接");
        JMenuItem exitItem = new JMenuItem("退出");

        if (listeners.length > 0) {
            newConnectionItem.addActionListener(listeners[0]);
            importItem.addActionListener(listeners[0]);
            exportItem.addActionListener(listeners[0]);
            exitItem.addActionListener(listeners[0]);
        }

        fileMenu.add(newConnectionItem);
        fileMenu.addSeparator();
        fileMenu.add(importItem);
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // 编辑菜单
        JMenu editMenu = new JMenu("编辑");
        JMenuItem formatItem = new JMenuItem("格式化SQL");
        JMenuItem formatSqlItem = new JMenuItem("格式化SQL");
        JMenuItem clearItem = new JMenuItem("清空编辑器");
        JMenuItem executeItem = new JMenuItem("执行SQL");

        if (listeners.length > 0) {
            formatItem.addActionListener(listeners[0]);
            formatSqlItem.addActionListener(listeners[0]);
            clearItem.addActionListener(listeners[0]);
            executeItem.addActionListener(listeners[0]);
        }

        editMenu.add(formatItem);

        // 工具菜单
        JMenu toolsMenu = new JMenu("工具");
        JMenuItem monitoringItem = new JMenuItem("性能监控");
        JMenuItem securityItem = new JMenuItem("安全设置");

        if (listeners.length > 0) {
            monitoringItem.addActionListener(listeners[0]);
            securityItem.addActionListener(listeners[0]);
        }

        toolsMenu.add(monitoringItem);
        toolsMenu.add(securityItem);

        // 设置菜单
        JMenu settingsMenu = new JMenu("设置");
        JMenu themeMenu = new JMenu("主题");
        JMenuItem darkThemeItem = new JMenuItem("深色主题");
        JMenuItem lightThemeItem = new JMenuItem("浅色主题");
        JMenu fontSizeMenu = new JMenu("字体大小");

        // 创建字体大小选项 (14-33)
        for (int fontSize = 14; fontSize <= 33; fontSize++) {
            final int size = fontSize;
            JMenuItem fontSizeItem = new JMenuItem(String.valueOf(fontSize));

            // 标记当前选中的字体大小
            if (size == com.database.visualization.DataBaseVisualizer.fontSizeValue) {
                fontSizeItem.setFont(fontSizeItem.getFont().deriveFont(Font.BOLD));
                fontSizeItem.setForeground(new Color(75, 110, 175));
            }

            if (listeners.length > 0) {
                fontSizeItem.addActionListener(listeners[0]);
            }
            fontSizeMenu.add(fontSizeItem);
        }

        if (listeners.length > 0) {
            darkThemeItem.addActionListener(listeners[0]);
            lightThemeItem.addActionListener(listeners[0]);
        }

        themeMenu.add(darkThemeItem);
        themeMenu.add(lightThemeItem);
        settingsMenu.add(themeMenu);
        settingsMenu.add(fontSizeMenu);

        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");

        if (listeners.length > 0) {
            aboutItem.addActionListener(listeners[0]);
        }

        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(toolsMenu);
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    /**
     * 创建工具栏
     */
    public JToolBar createToolBar(ActionListener... listeners) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton newConnButton = new JButton("新建连接");
        JButton executeButton = new JButton("执行SQL");
        JButton formatButton = new JButton("格式化SQL");
        JButton refreshButton = new JButton("刷新");
        JButton monitorButton = new JButton("性能监控");
        JButton exportButton = new JButton("导出数据");

        if (listeners.length > 0) {
            newConnButton.addActionListener(listeners[0]);
            executeButton.addActionListener(listeners[0]);
            formatButton.addActionListener(listeners[0]);
            refreshButton.addActionListener(listeners[0]);
            monitorButton.addActionListener(listeners[0]);
            exportButton.addActionListener(listeners[0]);
        }

        toolBar.add(newConnButton);
        toolBar.add(executeButton);
        toolBar.add(formatButton);
        toolBar.add(refreshButton);
        toolBar.add(monitorButton);
        toolBar.add(exportButton);

        return toolBar;
    }

    /**
     * 创建分页控制面板
     */
    public JPanel createPaginationPanel(ActionListener... listeners) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        panel.setBackground(isDarkTheme ? new Color(60, 63, 65) : new Color(240, 240, 240));

        JLabel pageLabel = new JLabel("页码:");
        pageLabel.setForeground(isDarkTheme ? new Color(187, 187, 187) : Color.BLACK);
        JTextField pageField = new JTextField("1", 3);
        pageField.setBackground(isDarkTheme ? new Color(69, 73, 74) : Color.WHITE);
        pageField.setForeground(isDarkTheme ? new Color(187, 187, 187) : Color.BLACK);
        pageField.setCaretColor(isDarkTheme ? Color.WHITE : Color.BLACK);

        JLabel pageSizeLabel = new JLabel("每页行数:");
        pageSizeLabel.setForeground(isDarkTheme ? new Color(187, 187, 187) : Color.BLACK);
        JTextField pageSizeField = new JTextField("500", 4);
        pageSizeField.setBackground(isDarkTheme ? new Color(69, 73, 74) : Color.WHITE);
        pageSizeField.setForeground(isDarkTheme ? new Color(187, 187, 187) : Color.BLACK);
        pageSizeField.setCaretColor(isDarkTheme ? Color.WHITE : Color.BLACK);

        JButton prevPageButton = new JButton("上一页");
        prevPageButton.setBackground(isDarkTheme ? new Color(60, 63, 65) : new Color(230, 230, 230));
        prevPageButton.setForeground(isDarkTheme ? new Color(187, 187, 187) : Color.BLACK);

        JButton nextPageButton = new JButton("下一页");
        nextPageButton.setBackground(isDarkTheme ? new Color(60, 63, 65) : new Color(230, 230, 230));
        nextPageButton.setForeground(isDarkTheme ? new Color(187, 187, 187) : Color.BLACK);

        JLabel totalPagesLabel = new JLabel("共 1 页");
        totalPagesLabel.setForeground(isDarkTheme ? new Color(187, 187, 187) : Color.BLACK);

        if (listeners.length > 0) {
            prevPageButton.addActionListener(listeners[0]);
            nextPageButton.addActionListener(listeners[0]);
            pageField.addActionListener(listeners[0]);
            pageSizeField.addActionListener(listeners[0]);
        }

        panel.add(pageLabel);
        panel.add(pageField);
        panel.add(pageSizeLabel);
        panel.add(pageSizeField);
        panel.add(prevPageButton);
        panel.add(nextPageButton);
        panel.add(totalPagesLabel);

        return panel;
    }

    /**
     * 创建数据编辑控制面板
     */
    public JPanel createDataEditPanel(ActionListener... listeners) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        panel.setBackground(isDarkTheme ? new Color(60, 63, 65) : new Color(240, 240, 240));

        JCheckBox editableCheckBox = new JCheckBox("启用编辑");
        editableCheckBox.setForeground(isDarkTheme ? new Color(187, 187, 187) : Color.BLACK);
        editableCheckBox.setBackground(isDarkTheme ? new Color(60, 63, 65) : new Color(240, 240, 240));

        JButton addRowButton = new JButton("添加行");
        addRowButton.setBackground(isDarkTheme ? new Color(60, 63, 65) : new Color(230, 230, 230));
        addRowButton.setForeground(isDarkTheme ? new Color(187, 187, 187) : Color.BLACK);
        addRowButton.setEnabled(false);

        JButton deleteRowButton = new JButton("删除行");
        deleteRowButton.setBackground(isDarkTheme ? new Color(60, 63, 65) : new Color(230, 230, 230));
        deleteRowButton.setForeground(isDarkTheme ? new Color(187, 187, 187) : Color.BLACK);
        deleteRowButton.setEnabled(false);

        JButton submitChangesButton = new JButton("提交更改");
        submitChangesButton.setBackground(isDarkTheme ? new Color(60, 63, 65) : new Color(230, 230, 230));
        submitChangesButton.setForeground(isDarkTheme ? new Color(187, 187, 187) : Color.BLACK);
        submitChangesButton.setEnabled(false);

        if (listeners.length > 0) {
            editableCheckBox.addActionListener(listeners[0]);
            addRowButton.addActionListener(listeners[0]);
            deleteRowButton.addActionListener(listeners[0]);
            submitChangesButton.addActionListener(listeners[0]);
        }

        panel.add(editableCheckBox);
        panel.add(addRowButton);
        panel.add(deleteRowButton);
        panel.add(submitChangesButton);

        return panel;
    }

    /**
     * 创建表格单元格渲染器
     */
    public DefaultTableCellRenderer createTableCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (isSelected) {
                    c.setBackground(new Color(75, 110, 175)); // 蓝色选中背景
                    c.setForeground(Color.WHITE);
                } else {
                    if (isDarkTheme) {
                        c.setBackground(row % 2 == 0 ? new Color(43, 43, 43) : new Color(49, 51, 53)); // 交替行颜色
                        c.setForeground(new Color(187, 187, 187)); // 浅灰色文字
                    } else {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240)); // 交替行颜色
                        c.setForeground(Color.BLACK);
                    }
                }

                // 设置单元格边框
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, isDarkTheme ? new Color(60, 63, 65) : new Color(200, 200, 200)),
                        BorderFactory.createEmptyBorder(1, 4, 1, 4)
                ));

                return c;
            }
        };
    }

    /**
     * 创建表头渲染器
     */
    public DefaultTableCellRenderer createHeaderRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JComponent c = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (isDarkTheme) {
                    c.setBackground(new Color(43, 43, 43));
                    c.setForeground(new Color(187, 187, 187));
                    c.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(60, 63, 65)),
                            BorderFactory.createEmptyBorder(1, 4, 1, 4)
                    ));
                } else {
                    c.setBackground(new Color(230, 230, 230));
                    c.setForeground(Color.BLACK);
                    c.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(200, 200, 200)),
                            BorderFactory.createEmptyBorder(1, 4, 1, 4)
                    ));
                }

                setHorizontalAlignment(JLabel.LEFT);
                setFont(getFont().deriveFont(Font.BOLD));

                return c;
            }
        };
    }

    /**
     * 设置主题
     */
    public void setTheme(boolean isDarkTheme) {
        this.isDarkTheme = isDarkTheme;
    }
} 