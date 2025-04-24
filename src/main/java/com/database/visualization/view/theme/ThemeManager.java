package com.database.visualization.view.theme;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * 主题管理器类，负责管理应用程序主题外观
 */
public class ThemeManager {
    private final JFrame mainFrame;
    private final JTree databaseTree;
    private final JTextArea sqlTextArea;
    private final JTable resultTable;
    private final JLabel statusLabel;
    private final JPanel paginationPanel;
    private final JPanel dataEditPanel;
    private final JSplitPane mainSplitPane;
    private final JSplitPane leftSplitPane;
    private boolean isDarkTheme;

    /**
     * 构造方法
     */
    public ThemeManager(JFrame mainFrame, JTree databaseTree, JTextArea sqlTextArea, JTable resultTable,
                        JLabel statusLabel, JPanel paginationPanel, JPanel dataEditPanel,
                        JSplitPane mainSplitPane, JSplitPane leftSplitPane) {
        this.mainFrame = mainFrame;
        this.databaseTree = databaseTree;
        this.sqlTextArea = sqlTextArea;
        this.resultTable = resultTable;
        this.statusLabel = statusLabel;
        this.paginationPanel = paginationPanel;
        this.dataEditPanel = dataEditPanel;
        this.mainSplitPane = mainSplitPane;
        this.leftSplitPane = leftSplitPane;

        this.isDarkTheme = com.database.visualization.DataBaseVisualizer.isDarkTheme;
    }

    /**
     * 应用主题
     */
    public void applyTheme() {
        if (isDarkTheme) {
            applyDarkTheme();
        } else {
            applyLightTheme();
        }

        // 刷新界面
        SwingUtilities.updateComponentTreeUI(mainFrame);
    }

    /**
     * 应用深色主题
     */
    private void applyDarkTheme() {
        // 设置主界面颜色
        mainFrame.getContentPane().setBackground(new Color(43, 43, 43));

        // 设置树的颜色
        databaseTree.setBackground(new Color(43, 43, 43));
        databaseTree.setForeground(new Color(187, 187, 187));

        // 设置文本区域的颜色
        sqlTextArea.setBackground(new Color(43, 43, 43));
        sqlTextArea.setForeground(new Color(187, 187, 187));
        sqlTextArea.setCaretColor(Color.WHITE);

        // 设置表格颜色
        resultTable.setBackground(new Color(43, 43, 43));
        resultTable.setForeground(new Color(187, 187, 187));
        resultTable.setGridColor(new Color(60, 63, 65));

        // 设置表头颜色
        JTableHeader header = resultTable.getTableHeader();
        header.setBackground(new Color(43, 43, 43));
        header.setForeground(new Color(187, 187, 187));
        header.setBorder(BorderFactory.createLineBorder(new Color(60, 63, 65)));

        // 设置状态栏颜色
        statusLabel.setForeground(new Color(187, 187, 187));

        // 设置分页面板颜色
        paginationPanel.setBackground(new Color(43, 43, 43));
        for (Component c : paginationPanel.getComponents()) {
            if (c instanceof JLabel) {
                c.setForeground(new Color(187, 187, 187));
            } else if (c instanceof JTextField) {
                JTextField tf = (JTextField) c;
                tf.setBackground(new Color(60, 63, 65));
                tf.setForeground(new Color(187, 187, 187));
                tf.setCaretColor(Color.WHITE);
                tf.setBorder(BorderFactory.createLineBorder(new Color(80, 83, 85)));
            } else if (c instanceof JButton) {
                JButton btn = (JButton) c;
                btn.setBackground(new Color(60, 63, 65));
                btn.setForeground(new Color(187, 187, 187));
                btn.setBorder(BorderFactory.createLineBorder(new Color(80, 83, 85)));
            }
        }

        // 设置数据编辑面板颜色
        dataEditPanel.setBackground(new Color(43, 43, 43));
        for (Component c : dataEditPanel.getComponents()) {
            if (c instanceof JButton) {
                JButton btn = (JButton) c;
                btn.setBackground(new Color(60, 63, 65));
                btn.setForeground(new Color(187, 187, 187));
                btn.setBorder(BorderFactory.createLineBorder(new Color(80, 83, 85)));
            } else if (c instanceof JCheckBox) {
                JCheckBox cb = (JCheckBox) c;
                cb.setBackground(new Color(43, 43, 43));
                cb.setForeground(new Color(187, 187, 187));
            }
        }

        // 设置滚动面板的颜色
        for (Component c : mainFrame.getContentPane().getComponents()) {
            if (c instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) c;
                scrollPane.getViewport().setBackground(new Color(43, 43, 43));
                scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 63, 65)));
            }
        }

        // 设置分割面板颜色
        if (mainSplitPane != null) {
            mainSplitPane.setBackground(new Color(43, 43, 43));
            mainSplitPane.setBorder(BorderFactory.createLineBorder(new Color(60, 63, 65)));
            mainSplitPane.setDividerSize(5);
            mainSplitPane.setDividerLocation(mainSplitPane.getDividerLocation()); // 触发重绘
        }

        if (leftSplitPane != null) {
            leftSplitPane.setBackground(new Color(43, 43, 43));
            leftSplitPane.setBorder(BorderFactory.createLineBorder(new Color(60, 63, 65)));
            leftSplitPane.setDividerSize(5);
            leftSplitPane.setDividerLocation(leftSplitPane.getDividerLocation()); // 触发重绘
        }
    }

    /**
     * 应用浅色主题
     */
    private void applyLightTheme() {
        // 设置主界面颜色
        mainFrame.getContentPane().setBackground(new Color(240, 240, 240));

        // 设置树的颜色
        databaseTree.setBackground(Color.WHITE);
        databaseTree.setForeground(Color.BLACK);

        // 设置文本区域的颜色
        sqlTextArea.setBackground(Color.WHITE);
        sqlTextArea.setForeground(Color.BLACK);
        sqlTextArea.setCaretColor(Color.BLACK);

        // 设置表格颜色
        resultTable.setBackground(Color.WHITE);
        resultTable.setForeground(Color.BLACK);
        resultTable.setGridColor(new Color(200, 200, 200));

        // 设置表头颜色
        JTableHeader header = resultTable.getTableHeader();
        header.setBackground(new Color(230, 230, 230));
        header.setForeground(Color.BLACK);
        header.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        // 设置状态栏颜色
        statusLabel.setForeground(Color.BLACK);

        // 设置分页面板颜色
        paginationPanel.setBackground(new Color(240, 240, 240));
        for (Component c : paginationPanel.getComponents()) {
            if (c instanceof JLabel) {
                c.setForeground(Color.BLACK);
            } else if (c instanceof JTextField) {
                JTextField tf = (JTextField) c;
                tf.setBackground(Color.WHITE);
                tf.setForeground(Color.BLACK);
                tf.setCaretColor(Color.BLACK);
                tf.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
            } else if (c instanceof JButton) {
                JButton btn = (JButton) c;
                btn.setBackground(new Color(230, 230, 230));
                btn.setForeground(Color.BLACK);
                btn.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
            }
        }

        // 设置数据编辑面板颜色
        dataEditPanel.setBackground(new Color(240, 240, 240));
        for (Component c : dataEditPanel.getComponents()) {
            if (c instanceof JButton) {
                JButton btn = (JButton) c;
                btn.setBackground(new Color(230, 230, 230));
                btn.setForeground(Color.BLACK);
                btn.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
            } else if (c instanceof JCheckBox) {
                JCheckBox cb = (JCheckBox) c;
                cb.setBackground(new Color(240, 240, 240));
                cb.setForeground(Color.BLACK);
            }
        }

        // 设置滚动面板的颜色
        for (Component c : mainFrame.getContentPane().getComponents()) {
            if (c instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) c;
                scrollPane.getViewport().setBackground(Color.WHITE);
                scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
            }
        }

        // 设置分割面板颜色
        if (mainSplitPane != null) {
            mainSplitPane.setBackground(new Color(240, 240, 240));
            mainSplitPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
            mainSplitPane.setDividerSize(5);
            mainSplitPane.setDividerLocation(mainSplitPane.getDividerLocation()); // 触发重绘
        }

        if (leftSplitPane != null) {
            leftSplitPane.setBackground(new Color(240, 240, 240));
            leftSplitPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
            leftSplitPane.setDividerSize(5);
            leftSplitPane.setDividerLocation(leftSplitPane.getDividerLocation()); // 触发重绘
        }
    }

    /**
     * 设置主题类型
     */
    public void setThemeType(boolean isDarkTheme) {
        this.isDarkTheme = isDarkTheme;
        com.database.visualization.DataBaseVisualizer.isDarkTheme = isDarkTheme;
        com.database.visualization.DataBaseVisualizer.applyTheme();
    }

    /**
     * 更新全局字体大小
     */
    public void updateGlobalFontSize() {
        float factor = com.database.visualization.DataBaseVisualizer.fontSizeFactor;

        // 获取当前默认字体
        Font defaultFont = UIManager.getFont("Label.font");
        if (defaultFont == null) {
            defaultFont = new Font("Dialog", Font.PLAIN, 12);
        }

        // 计算新字体大小
        int newSize = Math.round(defaultFont.getSize() * factor);
        Font newFont = defaultFont.deriveFont((float) newSize);

        // 更新树的字体
        databaseTree.setFont(newFont);

        // 更新文本区域的字体
        sqlTextArea.setFont(newFont);

        // 更新表格字体
        resultTable.setFont(newFont);
        resultTable.getTableHeader().setFont(newFont);

        // 更新分页面板中的组件字体
        for (Component comp : paginationPanel.getComponents()) {
            comp.setFont(newFont);
        }

        // 更新数据编辑面板中的组件字体
        for (Component comp : dataEditPanel.getComponents()) {
            comp.setFont(newFont);
        }

        // 根据字体大小调整行高
        resultTable.setRowHeight(Math.max(25, Math.round(25 * factor)));

        // 更新状态栏字体
        statusLabel.setFont(newFont);

        // 刷新UI
        SwingUtilities.updateComponentTreeUI(mainFrame);
    }
} 