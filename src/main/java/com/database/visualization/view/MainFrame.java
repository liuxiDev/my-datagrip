package com.database.visualization.view;/*
package com.database.visualization.view;

import com.database.visualization.controller.DatabaseService;
import com.database.visualization.model.ConnectionConfig;
import com.database.visualization.model.QueryResultTableModel;
import com.database.visualization.utils.ConnectionManager;
import com.database.visualization.utils.SQLFormatter;
import com.database.visualization.utils.TableColumnAdjuster;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

*/
/**
 * 主窗口
 *//*

public class MainFrame extends JFrame {
    private JTree databaseTree;
    private JTextArea sqlTextArea;
    private JTable resultTable;
    private JLabel statusLabel;
    private JSplitPane mainSplitPane;
    private JSplitPane leftSplitPane;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private ConnectionConfig currentConnection;
    private QueryResultTableModel resultTableModel;
    private DefaultTableCellRenderer cellRenderer;

    // 添加分页相关的字段
    private JPanel paginationPanel;
    private JTextField pageField;
    private JTextField pageSizeField;
    private JButton prevPageButton;
    private JButton nextPageButton;
    private JLabel totalPagesLabel;
    private int currentPage = 1;
    private int pageSize = 500;
    private int totalPages = 1;
    private int totalRecords = 0;
    private String currentTableName;

    // 添加数据编辑相关字段
    private JPanel dataEditPanel;
    private JButton addRowButton;
    private JButton deleteRowButton;
    private JButton submitChangesButton;
    private boolean isDataEditable = false;
    private List<String> primaryKeys = new ArrayList<>();
    private List<Integer> modifiedRows = new ArrayList<>();

    public MainFrame() {
        initComponents();
        setupListeners();

        setTitle("数据库可视化工具");
        setSize(1280, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 设置窗口最小大小
        setMinimumSize(new Dimension(800, 600));

        // 加载连接
        loadConnections();

        // 设置UI主题和应用字体大小
        setApplicationTheme();
        updateGlobalFontSize();

        // 添加窗口大小改变监听器，确保组件高度自适应
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustComponentsHeight();
            }
        });
    }

    */
/**
     * 调整组件高度以适应窗口大小
     *//*

    private void adjustComponentsHeight() {
        // 获取窗口内容面板的高度
        int contentHeight = getContentPane().getHeight();

        if (mainSplitPane != null) {
            // 设置主分割面板的高度为内容面板高度减去工具栏和状态栏高度
            int toolbarHeight = 30; // 估算工具栏高度
            int statusBarHeight = 20; // 估算状态栏高度
            mainSplitPane.setPreferredSize(new Dimension(getWidth(), contentHeight - toolbarHeight - statusBarHeight));

            // 重新确定分割位置
            mainSplitPane.setDividerLocation(mainSplitPane.getDividerLocation());
        }

        if (leftSplitPane != null) {
            // 移除第二个子组件(空白面板)，让树形组件占据整个高度
            if (leftSplitPane.getComponentCount() > 1) {
                leftSplitPane.remove(1);
                leftSplitPane.setDividerSize(0); // 移除分隔条
                leftSplitPane.setDividerLocation(contentHeight - 50); // 设置足够大的位置
                // 添加空组件仅为保持JSplitPane结构
                leftSplitPane.setBottomComponent(new JPanel());
            }

            // 设置左侧分割面板的高度与主窗口内容高度相同
            leftSplitPane.setPreferredSize(new Dimension(leftSplitPane.getWidth(), contentHeight - 50));
        }

        // 重新验证和重绘组件
        validate();
        repaint();
    }

    private void initComponents() {
        // 设置菜单栏
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        // 初始化组件
        rootNode = new DefaultMutableTreeNode("数据库连接");
        treeModel = new DefaultTreeModel(rootNode);
        databaseTree = new JTree(treeModel);
        databaseTree.setRootVisible(true);

        sqlTextArea = new JTextArea();
        sqlTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        sqlTextArea.setBackground(new Color(43, 43, 43));
        sqlTextArea.setForeground(new Color(187, 187, 187));
        sqlTextArea.setCaretColor(Color.WHITE);

        // 创建分页控制面板
        paginationPanel = createPaginationPanel();

        // 创建数据编辑控制面板
        dataEditPanel = createDataEditPanel();

        resultTableModel = new QueryResultTableModel(true); // 设置为可编辑
        resultTable = new JTable(resultTableModel);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        resultTable.setRowSelectionAllowed(true);

        // 设置表格的网格线和边框
        resultTable.setShowGrid(true);
        resultTable.setGridColor(new Color(60, 63, 65)); // 深色网格线
        resultTable.setBorder(BorderFactory.createLineBorder(new Color(60, 63, 65)));

        // 设置表头样式
        JTableHeader header = resultTable.getTableHeader();
        header.setBackground(new Color(43, 43, 43));
        header.setForeground(new Color(187, 187, 187));
        header.setBorder(BorderFactory.createLineBorder(new Color(60, 63, 65)));

        // 设置表格行高
        resultTable.setRowHeight(25);

        // 添加自定义单元格渲染器
        cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (isSelected) {
                    c.setBackground(new Color(75, 110, 175)); // 蓝色选中背景
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? new Color(43, 43, 43) : new Color(49, 51, 53)); // 交替行颜色
                    c.setForeground(new Color(187, 187, 187)); // 浅灰色文字
                }

                // 设置单元格边框
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(60, 63, 65)),
                        BorderFactory.createEmptyBorder(1, 4, 1, 4)
                ));

                return c;
            }
        };

        // 设置所有列使用自定义渲染器
        for (int i = 0; i < resultTable.getColumnCount(); i++) {
            resultTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        // 设置表头渲染器
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) resultTable.getTableHeader().getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(JLabel.LEFT);

        // 创建自定义表头渲染器
        resultTable.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JComponent c = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                c.setBackground(new Color(43, 43, 43));
                c.setForeground(new Color(187, 187, 187));
                c.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(60, 63, 65)),
                        BorderFactory.createEmptyBorder(1, 4, 1, 4)
                ));
                setHorizontalAlignment(JLabel.LEFT);
                setFont(getFont().deriveFont(Font.BOLD));

                return c;
            }
        });

        // 设置背景颜色
        resultTable.setBackground(new Color(43, 43, 43));

        statusLabel = new JLabel("就绪");
        statusLabel.setForeground(new Color(187, 187, 187));

        // 创建工具栏
        JToolBar toolBar = createToolBar();

        // 创建面板和分割窗
        JScrollPane treeScrollPane = new JScrollPane(databaseTree);
        JScrollPane sqlScrollPane = new JScrollPane(sqlTextArea);
        JScrollPane resultScrollPane = new JScrollPane(resultTable);

        // 直接使用树形组件的滚动面板作为左侧面板，不使用分割面板
        // 左侧面板撑满窗体高度
        leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeScrollPane, new JPanel());
        leftSplitPane.setDividerSize(0); // 隐藏分隔条
        leftSplitPane.setDividerLocation(Integer.MAX_VALUE); // 设置分隔位置为最大，使得上面的组件占据所有空间

        JPanel sqlPanel = new JPanel(new BorderLayout());
        sqlPanel.add(new JLabel("SQL查询:"), BorderLayout.NORTH);
        sqlPanel.add(sqlScrollPane, BorderLayout.CENTER);

        // 添加分页和数据编辑面板到SQL面板底部
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(paginationPanel, BorderLayout.NORTH);
        bottomPanel.add(dataEditPanel, BorderLayout.SOUTH);
        sqlPanel.add(bottomPanel, BorderLayout.SOUTH);

        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.add(new JLabel("查询结果:"), BorderLayout.NORTH);
        resultPanel.add(resultScrollPane, BorderLayout.CENTER);

        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sqlPanel, resultPanel);
        rightSplitPane.setDividerLocation(200);

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPane, rightSplitPane);
        mainSplitPane.setDividerLocation(250);

        // 添加组件到主窗口
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(toolBar, BorderLayout.NORTH);
        getContentPane().add(mainSplitPane, BorderLayout.CENTER);
        getContentPane().add(statusLabel, BorderLayout.SOUTH);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        JMenuItem newConnectionItem = new JMenuItem("新建连接");
        JMenuItem importItem = new JMenuItem("导入连接");
        JMenuItem exportItem = new JMenuItem("导出连接");
        JMenuItem exitItem = new JMenuItem("退出");

        newConnectionItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewConnection();
            }
        });

        importItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importConnections();
            }
        });

        exportItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportConnections();
            }
        });

        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                System.exit(0);
            }
        });

        fileMenu.add(newConnectionItem);
        fileMenu.addSeparator();
        fileMenu.add(importItem);
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // 编辑菜单
        JMenu editMenu = new JMenu("编辑");
        JMenuItem formatItem = new JMenuItem("格式化SQL");

        formatItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                formatSQL();
            }
        });

        editMenu.add(formatItem);

        JMenuItem formatSqlItem = new JMenuItem("格式化SQL");
        JMenuItem clearItem = new JMenuItem("清空编辑器");

        JMenuItem executeItem = new JMenuItem("执行SQL");
        executeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeSQL();
            }
        });

        // 添加格式化SQL功能
        formatSqlItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                formatSQL();
            }
        });

        clearItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sqlTextArea.setText("");
            }
        });

        // 工具菜单
        JMenu toolsMenu = new JMenu("工具");
        JMenuItem monitoringItem = new JMenuItem("性能监控");
        JMenuItem securityItem = new JMenuItem("安全设置");

        monitoringItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showMonitoringDialog();
            }
        });

        securityItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSecurityDialog();
            }
        });

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

            fontSizeItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    com.database.visualization.DataBaseVisualizer.fontSizeValue = size;
                    com.database.visualization.DataBaseVisualizer.fontSizeFactor = size / 14.0f;
                    com.database.visualization.DataBaseVisualizer.saveFontSizeSettings();
                    updateGlobalFontSize();

                    // 更新菜单项的标记
                    for (int i = 0; i < fontSizeMenu.getItemCount(); i++) {
                        JMenuItem item = fontSizeMenu.getItem(i);
                        if (item != null) {
                            int itemSize = Integer.parseInt(item.getText());
                            if (itemSize == size) {
                                item.setFont(item.getFont().deriveFont(Font.BOLD));
                                item.setForeground(new Color(75, 110, 175));
                            } else {
                                item.setFont(item.getFont().deriveFont(Font.PLAIN));
                                item.setForeground(UIManager.getColor("MenuItem.foreground"));
                            }
                        }
                    }
                }
            });
            fontSizeMenu.add(fontSizeItem);
        }

        darkThemeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                com.database.visualization.DataBaseVisualizer.isDarkTheme = true;
                com.database.visualization.DataBaseVisualizer.applyTheme();
                setApplicationTheme();
            }
        });

        lightThemeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                com.database.visualization.DataBaseVisualizer.isDarkTheme = false;
                com.database.visualization.DataBaseVisualizer.applyTheme();
                setApplicationTheme();
            }
        });

        themeMenu.add(darkThemeItem);
        themeMenu.add(lightThemeItem);
        settingsMenu.add(themeMenu);
        settingsMenu.add(fontSizeMenu);

        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");

        aboutItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "数据库可视化工具\nVersion 1.0\n作者: Java开发者",
                        "关于", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(toolsMenu);
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton newConnButton = new JButton("新建连接");
        JButton executeButton = new JButton("执行SQL");
        JButton formatButton = new JButton("格式化SQL");
        JButton refreshButton = new JButton("刷新");
        JButton monitorButton = new JButton("性能监控");
        JButton exportButton = new JButton("导出数据");

        newConnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewConnection();
            }
        });

        executeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeSQL();
            }
        });

        formatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                formatSQL();
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshDatabaseTree();
            }
        });

        monitorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showMonitoringDialog();
            }
        });

        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (resultTableModel != null && resultTableModel.getRowCount() > 0) {
                    // 创建导出选项菜单
                    JPopupMenu exportMenu = new JPopupMenu();

                    JMenuItem exportCsvItem = new JMenuItem("导出为CSV");
                    exportCsvItem.addActionListener(evt -> {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setDialogTitle("保存CSV文件");
                        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV文件", "csv");
                        fileChooser.setFileFilter(filter);

                        int result = fileChooser.showSaveDialog(MainFrame.this);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            File file = fileChooser.getSelectedFile();
                            String filePath = file.getAbsolutePath();
                            if (!filePath.endsWith(".csv")) {
                                filePath += ".csv";
                            }

                            try {
                                exportResultsToCsv(filePath);
                                JOptionPane.showMessageDialog(MainFrame.this, "导出成功！");
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(MainFrame.this, "导出失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });

                    JMenuItem exportExcelItem = new JMenuItem("导出为Excel");
                    exportExcelItem.addActionListener(evt -> {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setDialogTitle("保存Excel文件");
                        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                        FileNameExtensionFilter filter = new FileNameExtensionFilter("Excel文件", "xlsx");
                        fileChooser.setFileFilter(filter);

                        int result = fileChooser.showSaveDialog(MainFrame.this);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            File file = fileChooser.getSelectedFile();
                            String filePath = file.getAbsolutePath();
                            if (!filePath.endsWith(".xlsx")) {
                                filePath += ".xlsx";
                            }

                            try {
                                exportResultsToExcel(filePath);
                                JOptionPane.showMessageDialog(MainFrame.this, "导出成功！");
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(MainFrame.this, "导出失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });

                    exportMenu.add(exportCsvItem);
                    exportMenu.add(exportExcelItem);

                    // 在按钮位置显示菜单
                    exportMenu.show(exportButton, 0, exportButton.getHeight());
                } else {
                    JOptionPane.showMessageDialog(MainFrame.this, "没有可导出的数据", "提示", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        toolBar.add(newConnButton);
        toolBar.add(executeButton);
        toolBar.add(formatButton);
        toolBar.add(refreshButton);
        toolBar.add(monitorButton);
        toolBar.add(exportButton);

        return toolBar;
    }

    private void setupListeners() {
        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 保存连接配置
                ConnectionManager.saveConnections();
            }
        });

        // 添加树选择事件监听器
        databaseTree.addTreeSelectionListener(e -> handleTreeSelection(e));

        // 添加树鼠标事件监听器
        databaseTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = databaseTree.getPathForLocation(e.getX(), e.getY());
                if (path == null) return;

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = node.getUserObject();

                // 处理右键点击 - 显示上下文菜单
                if (e.getButton() == MouseEvent.BUTTON3) {
                    showTreeNodeContextMenu(node, e.getX(), e.getY());
                    return;
                }

                // 处理双击
                if (e.getClickCount() == 2) {
                    // 如果双击连接节点
                    if (userObject instanceof ConnectionConfig) {
                        connectDatabase((ConnectionConfig) userObject);
                    }
                    // 如果双击数据库节点
                    else if (userObject instanceof String && node.getParent() != null) {
                        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
                        Object parentObject = parentNode.getUserObject();

                        if (parentObject instanceof ConnectionConfig) {
                            // 如果父节点是连接配置，则展开/折叠
                            if (databaseTree.isExpanded(path)) {
                                databaseTree.collapsePath(path);
                            } else {
                                // 在展开节点前先加载表
                                currentConnection = (ConnectionConfig) parentObject;
                                String dbName = (String) userObject;
                                // 清空并加载表
                                node.removeAllChildren();
                                loadDatabaseTables(currentConnection, dbName, node);
                                databaseTree.expandPath(path);

                                // 对于MySQL数据库，执行USE语句
                                if ("mysql".equalsIgnoreCase(currentConnection.getDatabaseType())) {
                                    String sql = String.format("USE %s", dbName);
                                    sqlTextArea.setText(sql);
                                    executeSQL();
                                }
                            }
                        } else if (parentObject instanceof String && parentNode.getParent() != null) {
                            // 双击表节点，执行查询操作（而不是显示表结构）
                            if (node.isLeaf()) {
                                String tableName = (String) userObject;
                                String dbName = (String) parentObject;
                                DefaultMutableTreeNode grandParentNode = (DefaultMutableTreeNode) parentNode.getParent();

                                if (grandParentNode != null && grandParentNode.getUserObject() instanceof ConnectionConfig) {
                                    currentConnection = (ConnectionConfig) grandParentNode.getUserObject();
                                    currentTableName = dbName + "." + tableName;
                                    // 重置到第一页
                                    currentPage = 1;
                                    pageField.setText("1");

                                    // 设置SQL并执行查询
                                    String sql = String.format("SELECT * FROM %s.%s", dbName, tableName);
                                    sqlTextArea.setText(sql);
                                    executeSQL();
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    */
/**
     * 处理树节点选择事件
     *//*

    private void handleTreeSelection(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) databaseTree.getLastSelectedPathComponent();
        if (node == null) return;

        Object userObject = node.getUserObject();

        // 1. 如果选择的是连接节点
        if (userObject instanceof ConnectionConfig) {
            currentConnection = (ConnectionConfig) userObject;
            statusLabel.setText("已选择连接: " + currentConnection.getName());
        }
        // 2. 如果选择的是数据库/schema节点
        else if (userObject instanceof String && node.getParent() != null) {
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
            Object parentObject = parentNode.getUserObject();

            if (parentObject instanceof ConnectionConfig) {
                currentConnection = (ConnectionConfig) parentObject;
                String dbOrSchemaName = (String) userObject;
                statusLabel.setText("已选择数据库: " + dbOrSchemaName);

                // 重要：清除该节点下的所有子节点，确保每次切换数据库时都重新加载表
                node.removeAllChildren();
                treeModel.nodeStructureChanged(node);

                // 展开节点前，先获取该数据库下的所有表
                loadDatabaseTables(currentConnection, dbOrSchemaName, node);

                // 如果是MySQL数据库，自动执行USE语句
                if ("mysql".equalsIgnoreCase(currentConnection.getDatabaseType())) {
                    String sql = String.format("USE %s", dbOrSchemaName);
                    sqlTextArea.setText(sql);
                    executeSQL();
                }
            }
        }
        // 3. 如果选择的是表节点
        else if (userObject instanceof String && node.isLeaf() && node.getParent() != null) {
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
            Object parentObject = parentNode.getUserObject();

            if (parentObject instanceof String && parentNode.getParent() != null) {
                DefaultMutableTreeNode grandParentNode = (DefaultMutableTreeNode) parentNode.getParent();
                if (grandParentNode.getUserObject() instanceof ConnectionConfig) {
                    currentConnection = (ConnectionConfig) grandParentNode.getUserObject();
                    String dbOrSchemaName = (String) parentObject;
                    String tableName = (String) userObject;

                    // 根据数据库类型处理
                    if ("redis".equalsIgnoreCase(currentConnection.getDatabaseType())) {
                        // Redis数据库
                        currentTableName = dbOrSchemaName + ":" + tableName;
                        String cmd = String.format("SCAN 0 MATCH %s* COUNT 100", tableName);
                        sqlTextArea.setText(cmd);
                        statusLabel.setText("已选择Redis键模式: " + tableName);
                    } else {
                        // 常规数据库表
                        currentTableName = tableName;
                        String sql = String.format("SELECT * FROM %s.%s LIMIT 100", dbOrSchemaName, tableName);
                        sqlTextArea.setText(sql);
                        statusLabel.setText("已选择表: " + dbOrSchemaName + "." + tableName);
                    }
                }
            }
        }
    }

    */
/**
     * 加载数据库中的表
     *//*

    private void loadDatabaseTables(ConnectionConfig config, String dbName, DefaultMutableTreeNode parentNode) {
        if (!currentConnection.getDatabase().equals(dbName)) {
            currentConnection.setDatabase(dbName);
        }
        // 先清空当前节点的子节点
        parentNode.removeAllChildren();


        // 在后台线程中执行数据库操作
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                try {
                    // 获取数据库中的表
                    return DatabaseService.getTables(config, dbName);
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ArrayList<>(); // 返回空列表而不是null
                }
            }

            @Override
            protected void done() {
                try {
                    List<String> tables = get();

                    // 清空现有节点，确保添加的是当前数据库的表
                    parentNode.removeAllChildren();

                    // 添加表节点
                    for (String table : tables) {
                        DefaultMutableTreeNode tableNode = new DefaultMutableTreeNode(table);
                        parentNode.add(tableNode);
                    }

                    // 刷新树
                    treeModel.nodeStructureChanged(parentNode);

                    // 展开数据库节点
                    TreePath path = new TreePath(parentNode.getPath());
                    databaseTree.expandPath(path);
                } catch (Exception e) {
                    e.printStackTrace();
                    statusLabel.setText("加载表失败: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    */
/**
     * 加载所有连接
     *//*

    private void loadConnections() {
        rootNode.removeAllChildren();

        // 这里如果没有任何数据库连接应该忽略，不进行任何连接
        if (ConnectionManager.getConnections().isEmpty()) {
            return;
        }

        for (ConnectionConfig config : ConnectionManager.getConnections()) {
            DefaultMutableTreeNode connNode = new DefaultMutableTreeNode(config);
            rootNode.add(connNode);
        }

        treeModel.reload();
    }

    */
/**
     * 创建分页控制面板
     *//*

    private JPanel createPaginationPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        panel.setBackground(new Color(60, 63, 65));

        JLabel pageLabel = new JLabel("页码:");
        pageLabel.setForeground(new Color(187, 187, 187));
        pageField = new JTextField("1", 3);
        pageField.setBackground(new Color(69, 73, 74));
        pageField.setForeground(new Color(187, 187, 187));
        pageField.setCaretColor(Color.WHITE);

        JLabel pageSizeLabel = new JLabel("每页行数:");
        pageSizeLabel.setForeground(new Color(187, 187, 187));
        pageSizeField = new JTextField(String.valueOf(pageSize), 4);
        pageSizeField.setBackground(new Color(69, 73, 74));
        pageSizeField.setForeground(new Color(187, 187, 187));
        pageSizeField.setCaretColor(Color.WHITE);

        prevPageButton = new JButton("上一页");
        prevPageButton.setBackground(new Color(60, 63, 65));
        prevPageButton.setForeground(new Color(187, 187, 187));

        nextPageButton = new JButton("下一页");
        nextPageButton.setBackground(new Color(60, 63, 65));
        nextPageButton.setForeground(new Color(187, 187, 187));

        totalPagesLabel = new JLabel("共 1 页");
        totalPagesLabel.setForeground(new Color(187, 187, 187));

        // 添加事件监听器
        prevPageButton.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                pageField.setText(String.valueOf(currentPage));
                executeCurrentQuery();
            }
        });

        nextPageButton.addActionListener(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                pageField.setText(String.valueOf(currentPage));
                executeCurrentQuery();
            }
        });

        pageField.addActionListener(e -> {
            try {
                int page = Integer.parseInt(pageField.getText());
                if (page > 0) {
                    currentPage = page;
                    executeCurrentQuery();
                } else {
                    pageField.setText(String.valueOf(currentPage));
                }
            } catch (NumberFormatException ex) {
                pageField.setText(String.valueOf(currentPage));
            }
        });

        pageSizeField.addActionListener(e -> {
            try {
                int size = Integer.parseInt(pageSizeField.getText());
                if (size > 0) {
                    pageSize = size;
                    currentPage = 1;
                    pageField.setText("1");
                    executeCurrentQuery();
                } else {
                    pageSizeField.setText(String.valueOf(pageSize));
                }
            } catch (NumberFormatException ex) {
                pageSizeField.setText(String.valueOf(pageSize));
            }
        });

        // 添加组件到面板
        panel.add(pageLabel);
        panel.add(pageField);
        panel.add(pageSizeLabel);
        panel.add(pageSizeField);
        panel.add(prevPageButton);
        panel.add(nextPageButton);
        panel.add(totalPagesLabel);

        return panel;
    }

    */
/**
     * 将查询结果导出为CSV文件
     *
     * @param filePath 文件路径
     * @throws Exception 导出异常
     *//*

    private void exportResultsToCsv(String filePath) throws Exception {
        if (resultTableModel == null || resultTableModel.getRowCount() == 0) {
            throw new Exception("没有可导出的数据");
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            // 写入表头
            int columnCount = resultTableModel.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                String columnName = resultTableModel.getColumnName(i);
                writer.append(columnName);
                if (i < columnCount - 1) {
                    writer.append(",");
                }
            }
            writer.append("\n");

            // 写入数据
            int rowCount = resultTableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < columnCount; j++) {
                    Object value = resultTableModel.getValueAt(i, j);
                    String cellValue = (value == null) ? "" : value.toString();

                    // 处理包含逗号、引号或换行符的单元格
                    if (cellValue.contains(",") || cellValue.contains("\"") || cellValue.contains("\n")) {
                        cellValue = "\"" + cellValue.replace("\"", "\"\"") + "\"";
                    }

                    writer.append(cellValue);
                    if (j < columnCount - 1) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }

            writer.flush();
        }
    }

    */
/**
     * 将查询结果导出为Excel文件
     *
     * @param filePath 文件路径
     * @throws Exception 导出异常
     *//*

    private void exportResultsToExcel(String filePath) throws Exception {
        if (resultTableModel == null || resultTableModel.getRowCount() == 0) {
            throw new Exception("没有可导出的数据");
        }

        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            // 创建一个工作簿
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("数据");

            // 创建表头行
            Row headerRow = sheet.createRow(0);
            int columnCount = resultTableModel.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                String columnName = resultTableModel.getColumnName(i);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnName);
            }

            // 创建数据行
            int rowCount = resultTableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < columnCount; j++) {
                    Object value = resultTableModel.getValueAt(i, j);
                    Cell cell = row.createCell(j);

                    if (value != null) {
                        // 根据值的类型设置单元格的值
                        if (value instanceof Number) {
                            cell.setCellValue(((Number) value).doubleValue());
                        } else if (value instanceof Boolean) {
                            cell.setCellValue((Boolean) value);
                        } else if (value instanceof Date) {
                            cell.setCellValue((Date) value);
                        } else {
                            cell.setCellValue(value.toString());
                        }
                    }
                }
            }

            // 自动调整列宽
            for (int i = 0; i < columnCount; i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入文件
            workbook.write(fileOut);
            workbook.close();
        }
    }

    */
/**
     * 设置应用程序主题颜色
     *//*

    private void setApplicationTheme() {
        if (com.database.visualization.DataBaseVisualizer.isDarkTheme) {
            // 深色主题
            // 设置主界面颜色
            getContentPane().setBackground(new Color(43, 43, 43));

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

            // 设置表格单元格渲染器颜色
            DefaultTableCellRenderer darkRenderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus,
                                                               int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    if (isSelected) {
                        c.setBackground(new Color(75, 110, 175)); // 蓝色选中背景
                        c.setForeground(Color.WHITE);
                    } else {
                        c.setBackground(row % 2 == 0 ? new Color(43, 43, 43) : new Color(49, 51, 53)); // 交替行颜色
                        c.setForeground(new Color(187, 187, 187)); // 浅灰色文字
                    }

                    // 设置单元格边框
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(60, 63, 65)),
                            BorderFactory.createEmptyBorder(1, 4, 1, 4)
                    ));

                    return c;
                }
            };

            // 应用单元格渲染器到所有列
            for (int i = 0; i < resultTable.getColumnCount(); i++) {
                resultTable.getColumnModel().getColumn(i).setCellRenderer(darkRenderer);
            }

            // 自定义表头渲染器
            resultTable.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus,
                                                               int row, int column) {
                    JComponent c = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    c.setBackground(new Color(43, 43, 43));
                    c.setForeground(new Color(187, 187, 187));
                    c.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(60, 63, 65)),
                            BorderFactory.createEmptyBorder(1, 4, 1, 4)
                    ));
                    setHorizontalAlignment(JLabel.LEFT);
                    setFont(getFont().deriveFont(Font.BOLD));

                    return c;
                }
            });

            // 设置滚动面板的颜色
            for (Component c : getContentPane().getComponents()) {
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
        } else {
            // 浅色主题
            // 设置主界面颜色
            getContentPane().setBackground(new Color(240, 240, 240));

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

            // 设置表格单元格渲染器颜色
            DefaultTableCellRenderer lightRenderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus,
                                                               int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    if (isSelected) {
                        c.setBackground(new Color(51, 153, 255)); // 蓝色选中背景
                        c.setForeground(Color.WHITE);
                    } else {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240)); // 交替行颜色
                        c.setForeground(Color.BLACK);
                    }

                    // 设置单元格边框
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(200, 200, 200)),
                            BorderFactory.createEmptyBorder(1, 4, 1, 4)
                    ));

                    return c;
                }
            };

            // 应用单元格渲染器到所有列
            for (int i = 0; i < resultTable.getColumnCount(); i++) {
                resultTable.getColumnModel().getColumn(i).setCellRenderer(lightRenderer);
            }

            // 自定义表头渲染器
            resultTable.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus,
                                                               int row, int column) {
                    JComponent c = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    c.setBackground(new Color(230, 230, 230));
                    c.setForeground(Color.BLACK);
                    c.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(200, 200, 200)),
                            BorderFactory.createEmptyBorder(1, 4, 1, 4)
                    ));
                    setHorizontalAlignment(JLabel.LEFT);
                    setFont(getFont().deriveFont(Font.BOLD));

                    return c;
                }
            });

            // 设置滚动面板的颜色
            for (Component c : getContentPane().getComponents()) {
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

        // 刷新界面
        SwingUtilities.updateComponentTreeUI(this);
    }

    */
/**
     * 更新全局字体大小
     *//*

    private void updateGlobalFontSize() {
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
        SwingUtilities.updateComponentTreeUI(this);
    }

    */
/**
     * 刷新数据库树
     *//*

    private void refreshDatabaseTree() {
        // 保存当前选中的路径
        TreePath selectedPath = databaseTree.getSelectionPath();

        if (selectedPath != null && selectedPath.getPathCount() > 1) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            Object userObject = selectedNode.getUserObject();

            // 如果当前选中的是数据库节点，只刷新该数据库下的表
            if (userObject instanceof String && selectedNode.getParent() != null) {
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();

                if (parentNode.getUserObject() instanceof ConnectionConfig) {
                    ConnectionConfig config = (ConnectionConfig) parentNode.getUserObject();
                    String dbName = (String) userObject;

                    // 清空节点并重新加载表
                    selectedNode.removeAllChildren();
                    treeModel.nodeStructureChanged(selectedNode);
                    loadDatabaseTables(config, dbName, selectedNode);

                    statusLabel.setText("已刷新数据库: " + dbName);
                    return;
                }
            }
        }

        // 否则刷新整个连接树
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                statusLabel.setText("正在连接数据库...");

                // 存储展开的节点路径
                Enumeration<TreePath> expandedPaths = databaseTree.getExpandedDescendants(
                        new TreePath(rootNode.getPath()));

                // 清除根节点下的所有子节点
                rootNode.removeAllChildren();

                // 添加所有连接到树
                for (ConnectionConfig config : ConnectionManager.getConnections()) {
                    DefaultMutableTreeNode connNode = new DefaultMutableTreeNode(config);
                    rootNode.add(connNode);

                    // 如果是当前连接，加载数据库
                    if (currentConnection != null && config.getId().equals(currentConnection.getId())) {
                        try {
                            // 获取所有数据库/schema
                            List<String> databases = DatabaseService.getDatabases(config);

                            if (databases != null && !databases.isEmpty()) {
                                for (String db : databases) {
                                    DefaultMutableTreeNode dbNode = new DefaultMutableTreeNode(db);
                                    connNode.add(dbNode);
                                }
                            } else {
                                // 如果无法获取数据库列表，添加一个提示节点
                                DefaultMutableTreeNode noDbNode = new DefaultMutableTreeNode("(无数据库)");
                                connNode.add(noDbNode);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // 添加一个错误提示节点
                            DefaultMutableTreeNode errorNode = new DefaultMutableTreeNode("(加载错误)");
                            connNode.add(errorNode);
                        }
                    }
                }

                // 刷新树模型
                treeModel.reload();

                // 恢复展开的节点
                if (expandedPaths != null) {
                    while (expandedPaths.hasMoreElements()) {
                        TreePath path = expandedPaths.nextElement();
                        databaseTree.expandPath(path);
                    }
                }

                // 恢复选中的节点
                if (selectedPath != null) {
                    databaseTree.setSelectionPath(selectedPath);
                }

                return null;
            }

            @Override
            protected void done() {
                statusLabel.setText("数据库树刷新完成");
            }
        };

        worker.execute();
    }

    */
/**
     * 执行SQL
     *//*

    private void executeSQL() {
        String sql = sqlTextArea.getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入SQL语句", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        executeSQL(sql);
    }

    */
/**
     * 执行SQL语句
     *//*

    private void executeSQL(String sql) {
        if (currentConnection == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个数据库连接", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 清空分页参数
        currentPage = 1;
        pageField.setText("1");

        // 执行SQL
        executeSQLInternal(sql);
    }

    */
/**
     * 执行当前查询（分页）
     *//*

    private void executeCurrentQuery() {
        String sql = sqlTextArea.getText().trim();
        if (sql.isEmpty() || currentConnection == null) {
            return;
        }

        // 执行分页查询
        int offset = (currentPage - 1) * pageSize;
        String paginatedSql = sql;

        // 添加分页语句（根据数据库类型）
        if (sql.toLowerCase().contains("limit")) {
            // 已经有LIMIT语句，替换它
            paginatedSql = sql.replaceAll("(?i)LIMIT\\s+\\d+(?:\\s*,\\s*\\d+)?", "LIMIT " + offset + ", " + pageSize);
        } else {
            paginatedSql = sql + " LIMIT " + offset + ", " + pageSize;
        }

        executeSQLInternal(paginatedSql);
    }

    */
/**
     * 内部执行SQL的实现
     *//*

    private void executeSQLInternal(String sql) {
        statusLabel.setText("执行SQL: " + (sql.length() > 50 ? sql.substring(0, 50) + "..." : sql));

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() {
                if (sql.trim().toLowerCase().startsWith("select") ||
                        sql.trim().toLowerCase().startsWith("show") ||
                        sql.trim().toLowerCase().startsWith("desc")) {
                    return DatabaseService.executeQuery(currentConnection, sql);
                } else {
                    return DatabaseService.executeUpdate(currentConnection, sql);
                }
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> result = get();
                    boolean success = (boolean) result.get("success");

                    if (success) {
                        if (result.containsKey("data")) {
                            // 检查数据类型，根据类型选择合适的方法处理
                            Object dataObj = result.get("data");
                            List<String> columns = (List<String>) result.get("columns");

                            if (dataObj instanceof List<?>) {
                                if (((List<?>) dataObj).isEmpty() || ((List<?>) dataObj).get(0) instanceof Map) {
                                    // 如果是List<Map<String, Object>>类型
                                    List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;
                                    resultTableModel.setDataFromMap(data, columns);
                                } else {
                                    // 如果是List<List<Object>>类型
                                    List<List<Object>> data = (List<List<Object>>) dataObj;
                                    resultTableModel.setData(columns, data);
                                }
                            }

                            resultTable.setModel(resultTableModel);

                            // 获取总记录数（如果有）
                            if (result.containsKey("totalRecords")) {
                                totalRecords = (int) result.get("totalRecords");
                                totalPages = (int) Math.ceil((double) totalRecords / pageSize);
                            } else {
                                totalRecords = ((List<?>) dataObj).size();
                                totalPages = 1;
                            }

                            // 调整列宽
                            TableColumnAdjuster adjuster = new TableColumnAdjuster(resultTable);
                            adjuster.adjustColumns();

                            // 尝试解析出表名(仅对SELECT语句)
                            String lowerSql = sql.toLowerCase().trim();
                            if (lowerSql.startsWith("select")) {
                                // 尝试从SQL中提取表名
                                extractTableNameAndFetchPrimaryKeys(lowerSql);
                            }

                            // 更新状态
                            statusLabel.setText("查询执行成功，返回 " + totalRecords + " 条记录");

                            // 更新分页信息
                            updatePaginationInfo();

                            // 添加到历史记录
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    // 如果需要，添加查询历史
                                }
                            });
                        } else {
                            // 添加空值检查，避免空指针异常
                            Object updateCountObj = result.get("updateCount");
                            if (updateCountObj != null) {
                                int updateCount = (int) updateCountObj;
                                statusLabel.setText("更新成功，影响 " + updateCount + " 行");
                            } else {
                                statusLabel.setText("操作执行成功");
                            }

                            // 如果是DDL语句，刷新树
                            String sqlLower = sql.toLowerCase().trim();
                            if (sqlLower.startsWith("create") || sqlLower.startsWith("drop") ||
                                    sqlLower.startsWith("alter") || sqlLower.startsWith("truncate")) {
                                refreshDatabaseTree();
                            }
                        }
                    } else {
                        String errorMessage = (String) result.get("error");
                        statusLabel.setText("SQL执行出错: " + errorMessage);
                        JOptionPane.showMessageDialog(MainFrame.this, errorMessage, "SQL执行错误", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    statusLabel.setText("SQL执行出错: " + e.getMessage());
                    JOptionPane.showMessageDialog(MainFrame.this, e.getMessage(), "SQL执行错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    */
/**
     * 从SQL中提取表名并获取主键
     *
     * @param sql SQL语句
     *//*

    private void extractTableNameAndFetchPrimaryKeys(String sql) {
        try {
            // 简单的表名提取逻辑，可能需要更复杂的解析器
            int fromIndex = sql.indexOf(" from ");
            if (fromIndex == -1) return;

            String afterFrom = sql.substring(fromIndex + 6).trim();

            // 处理表名部分
            String tablePart;
            int whereIndex = afterFrom.indexOf(" where ");
            int orderByIndex = afterFrom.indexOf(" order by ");
            int groupByIndex = afterFrom.indexOf(" group by ");
            int limitIndex = afterFrom.indexOf(" limit ");

            int endIndex = Integer.MAX_VALUE;
            if (whereIndex > 0) endIndex = Math.min(endIndex, whereIndex);
            if (orderByIndex > 0) endIndex = Math.min(endIndex, orderByIndex);
            if (groupByIndex > 0) endIndex = Math.min(endIndex, groupByIndex);
            if (limitIndex > 0) endIndex = Math.min(endIndex, limitIndex);

            if (endIndex < Integer.MAX_VALUE) {
                tablePart = afterFrom.substring(0, endIndex).trim();
            } else {
                tablePart = afterFrom.trim();
            }

            // 处理复杂表名(可能包含别名、连接等)
            String[] tableNames = tablePart.split(",");
            if (tableNames.length > 0) {
                String firstTable = tableNames[0].trim();

                // 移除可能的别名
                if (firstTable.contains(" as ")) {
                    firstTable = firstTable.substring(0, firstTable.indexOf(" as ")).trim();
                } else if (firstTable.contains(" ")) {
                    firstTable = firstTable.substring(0, firstTable.indexOf(" ")).trim();
                }

                // 设置当前表名
                currentTableName = firstTable;

                // 获取表主键
                fetchTablePrimaryKeys(currentTableName);
            }
        } catch (Exception e) {
            // 解析失败，不进行处理
            e.printStackTrace();
        }
    }

    */
/**
     * 更新分页信息
     *//*

    private void updatePaginationInfo() {
        totalPagesLabel.setText("共 " + totalPages + " 页, 共 " + totalRecords + " 条记录");
        prevPageButton.setEnabled(currentPage > 1);
        nextPageButton.setEnabled(currentPage < totalPages);
    }

    */
/**
     * 添加新连接
     *//*

    private void addNewConnection() {
        ConnectionDialog dialog = new ConnectionDialog(this, null, true);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            ConnectionConfig config = dialog.getConnectionConfig();
            boolean added = ConnectionManager.addConnection(config);
            if (added) {
                loadConnections();
            } else {
                JOptionPane.showMessageDialog(this,
                        "添加连接失败，连接可能已存在",
                        "添加连接", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    */
/**
     * 导入连接配置
     *//*

    private void importConnections() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导入连接配置");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON文件", "json"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getPath();
            importConnectionsFromFile(filePath);
        }
    }

    */
/**
     * 从文件导入连接配置
     *//*

    private void importConnectionsFromFile(String filePath) {
        try {
            // 使用ConnectionManager导入连接
            List<ConnectionConfig> importedConfigs = new ArrayList<>();

            // 实际导入逻辑
            ObjectMapper mapper = new ObjectMapper();
            File file = new File(filePath);
            importedConfigs = mapper.readValue(file, new TypeReference<List<ConnectionConfig>>() {
            });

            for (ConnectionConfig config : importedConfigs) {
                ConnectionManager.addConnection(config);
            }

            loadConnections();
            JOptionPane.showMessageDialog(this,
                    "成功导入 " + importedConfigs.size() + " 个连接配置",
                    "导入成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "导入连接配置失败: " + e.getMessage(),
                    "导入失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    */
/**
     * 导出连接配置
     *//*

    private void exportConnections() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出连接配置");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON文件", "json"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getPath();
            if (!filePath.endsWith(".json")) {
                filePath += ".json";
            }

            exportConnectionsToFile(filePath);
        }
    }

    */
/**
     * 导出连接配置到文件
     *//*

    private void exportConnectionsToFile(String filePath) {
        try {
            // 获取所有连接配置
            List<ConnectionConfig> configs = ConnectionManager.getConnections();

            // 使用Jackson序列化为JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), configs);

            JOptionPane.showMessageDialog(this,
                    "成功导出 " + configs.size() + " 个连接配置",
                    "导出成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "导出连接配置失败: " + e.getMessage(),
                    "导出失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    */
/**
     * 格式化SQL
     *//*

    private void formatSQL() {
        String sql = sqlTextArea.getText().trim();
        if (sql.isEmpty()) {
            return;
        }

        // 使用SQL格式化工具格式化
        String formattedSql = SQLFormatter.format(sql);
        sqlTextArea.setText(formattedSql);
    }

    */
/**
     * 显示性能监控对话框
     *//*

    private void showMonitoringDialog() {
        // 创建并显示新的监控对话框
        MonitoringDialog dialog = new MonitoringDialog(this, currentConnection);
        dialog.setVisible(true);
    }


    */
/**
     * 删除数据库
     *//*

    private void dropDatabase(ConnectionConfig config, String dbName) {
        if (JOptionPane.showConfirmDialog(this,
                "确定要删除数据库 " + dbName + " 吗？此操作不可逆！",
                "确认删除", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

            currentConnection = config;
            String sql = "DROP DATABASE " + dbName;

            // 执行SQL
            Map<String, Object> result = DatabaseService.executeUpdate(currentConnection, sql);
            if ((boolean) result.get("success")) {
                JOptionPane.showMessageDialog(this,
                        "数据库 " + dbName + " 已成功删除！",
                        "删除成功",
                        JOptionPane.INFORMATION_MESSAGE);

                // 刷新数据库树
                refreshDatabaseTree();
            } else {
                JOptionPane.showMessageDialog(this,
                        "删除数据库失败: " + result.get("error"),
                        "删除失败",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    */
/**
     * 创建新表
     *//*

    private void createNewTable(ConnectionConfig config, String schemaName) {
        // 弹出创建表对话框
        CreateTableDialog dialog = new CreateTableDialog(this, schemaName);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            currentConnection = config;
            String sql = dialog.getCreateTableSQL();
            if (sql != null && !sql.trim().isEmpty()) {
                executeSQL(sql);
                refreshDatabaseTree();
            }
        }
    }

    */
/**
     * 添加统计项
     *//*

    private void addStatItem(JPanel panel, String name, String value) {
        JPanel itemPanel = new JPanel(new BorderLayout());
        itemPanel.add(new JLabel(name + ": "), BorderLayout.WEST);
        itemPanel.add(new JLabel(value), BorderLayout.CENTER);
        panel.add(itemPanel);
    }

    */
/**
     * 显示安全设置对话框
     *//*

    private void showSecurityDialog() {
        JDialog dialog = new JDialog(this, "安全设置", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 从SecurityManager获取当前设置
        boolean currentEncryptEnabled = com.database.visualization.utils.SecurityManager.isEncryptEnabled();
        String currentAlgorithm = com.database.visualization.utils.SecurityManager.getAlgorithm();
        String currentKeyStrength = com.database.visualization.utils.SecurityManager.getKeyStrength();

        // 密码加密选项
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JCheckBox encryptPasswordCheckbox = new JCheckBox("启用密码加密存储", currentEncryptEnabled);
        panel.add(encryptPasswordCheckbox, gbc);

        // 加密算法
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(new JLabel("加密算法:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        String[] algorithms = {"AES", "DES", "RSA"};
        JComboBox<String> algorithmCombo = new JComboBox<>(algorithms);
        algorithmCombo.setSelectedItem(currentAlgorithm);
        panel.add(algorithmCombo, gbc);

        // 密钥强度
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("密钥强度:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        String[] strengths = {"128位", "192位", "256位"};
        JComboBox<String> strengthCombo = new JComboBox<>(strengths);
        strengthCombo.setSelectedItem(currentKeyStrength);
        panel.add(strengthCombo, gbc);

        // 按钮
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("保存设置");
        JButton cancelButton = new JButton("取消");

        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 保存设置到SecurityManager
                boolean encryptEnabled = encryptPasswordCheckbox.isSelected();
                String algorithm = (String) algorithmCombo.getSelectedItem();
                String keyStrength = (String) strengthCombo.getSelectedItem();

                com.database.visualization.utils.SecurityManager.updateSecuritySettings(
                    encryptEnabled, algorithm, keyStrength);

                JOptionPane.showMessageDialog(dialog, "安全设置已保存");
                dialog.dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.setLayout(new BorderLayout());
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    */
/**
     * 显示树节点的右键菜单
     *//*

    private void showTreeNodeContextMenu(DefaultMutableTreeNode node, int x, int y) {
        if (node == null) return;

        Object userObject = node.getUserObject();

        if (userObject instanceof ConnectionConfig) {
            // 连接节点的右键菜单
            showConnectionContextMenu((ConnectionConfig) userObject, x, y);
        } else if (userObject instanceof String && node.getParent() != null) {
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
            Object parentObject = parentNode.getUserObject();

            if (parentObject instanceof ConnectionConfig) {
                // 数据库/schema节点的右键菜单
                ConnectionConfig config = (ConnectionConfig) parentObject;
                String dbName = (String) userObject;

                // 对于Redis数据库节点
                if ("redis".equalsIgnoreCase(config.getDatabaseType()) && dbName.startsWith("db")) {
                    showRedisDatabaseContextMenu(config, dbName, x, y);
                } else {
                    // 普通数据库/schema节点
                    showSchemaContextMenu(config, dbName, x, y);
                }
            } else if (parentObject instanceof String && parentNode.getParent() != null) {
                // Schema节点或Redis数据库节点下的内容
                DefaultMutableTreeNode grandParentNode = (DefaultMutableTreeNode) parentNode.getParent();
                if (grandParentNode.getUserObject() instanceof ConnectionConfig) {
                    ConnectionConfig config = (ConnectionConfig) grandParentNode.getUserObject();
                    String parentName = (String) parentObject;
                    String nodeName = (String) userObject;

                    if ("redis".equalsIgnoreCase(config.getDatabaseType()) && parentName.startsWith("db")) {
                        // Redis数据库中的类型节点
                        showRedisTypeContextMenu(config, parentName, nodeName, x, y);
                    } else {
                        // 普通Schema下的表
                        showSchemaTableContextMenu(config, parentName, nodeName, x, y);
                    }
                }
            } else if (parentObject instanceof String) {
                // 这种情况可能是双层嵌套的schema/database，使用一般处理
                String schemaName = (String) parentObject;
                DefaultMutableTreeNode rootParentNode = (DefaultMutableTreeNode) parentNode.getParent();
                if (rootParentNode != null && rootParentNode.getUserObject() instanceof ConnectionConfig) {
                    ConnectionConfig config = (ConnectionConfig) rootParentNode.getUserObject();

                    if ("redis".equalsIgnoreCase(config.getDatabaseType()) && schemaName.startsWith("db")) {
                        // Redis数据库节点
                        showRedisDatabaseContextMenu(config, schemaName, x, y);
                    } else {
                        // 普通Schema节点
                        showSchemaContextMenu(config, schemaName, x, y);
                    }
                }
            }
        }
    }

    */
/**
     * 连接数据库
     *//*

    private void connectDatabase(ConnectionConfig config) {
        // 创建SwingWorker在后台线程中连接数据库
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return Boolean.valueOf(DatabaseService.connect(config));
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        currentConnection = config;
                        statusLabel.setText("已连接到: " + config.getName());
                        refreshDatabaseTree();
                    } else {
                        statusLabel.setText("连接失败");
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "连接数据库失败，请检查连接配置",
                                "连接失败", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    statusLabel.setText("连接出错: " + e.getMessage());
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "连接数据库出错: " + e.getMessage(),
                            "连接出错", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    */
/**
     * 显示表上下文菜单
     *//*

    private void showSchemaTableContextMenu(ConnectionConfig config, String schemaName, String tableName, int x, int y) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem queryItem = new JMenuItem("查询数据");
        JMenuItem structureItem = new JMenuItem("表结构");
        JMenuItem editItem = new JMenuItem("修改表");
        JMenuItem dropItem = new JMenuItem("删除表");
        JMenuItem emptyItem = new JMenuItem("清空表");
        JMenuItem exportItem = new JMenuItem("导出数据");

        editItem.addActionListener(e -> {
            showEditTableDialog(tableName);
        });

        dropItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "确定要删除表 " + tableName + " 吗？此操作不可逆！",
                    "确认删除", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                executeSQL("DROP TABLE " + tableName);
                refreshDatabaseTree();
            }
        });

        emptyItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "确定要清空表 " + tableName + " 的所有数据吗？此操作不可逆！",
                    "确认清空", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                executeSQL("TRUNCATE TABLE " + tableName);
            }
        });
        queryItem.addActionListener(e -> {
            currentConnection = config;
            String sql = "SELECT * FROM " + schemaName + "." + tableName + " LIMIT 100";
            sqlTextArea.setText(sql);
            executeSQL();
        });

        structureItem.addActionListener(e -> {
            currentConnection = config;
            showTableStructure(schemaName + "." + tableName);
        });

        exportItem.addActionListener(e -> {
            // 导出数据逻辑
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("导出数据");
            fileChooser.setFileFilter(new FileNameExtensionFilter("CSV文件", "csv"));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getPath();
                if (!filePath.endsWith(".csv")) {
                    filePath += ".csv";
                }

                try {
                    // 先查询数据
                    currentConnection = config;
                    String sql = "SELECT * FROM " + schemaName + "." + tableName;
                    Map<String, Object> result = DatabaseService.executeQuery(currentConnection, sql);

                    if ((boolean) result.get("success")) {
                        List<List<Object>> data = (List<List<Object>>) result.get("data");
                        List<String> columns = (List<String>) result.get("columns");

                        // 导出到CSV
                        try (FileWriter writer = new FileWriter(filePath)) {
                            // 写入表头
                            for (int i = 0; i < columns.size(); i++) {
                                writer.append(columns.get(i));
                                if (i < columns.size() - 1) {
                                    writer.append(",");
                                }
                            }
                            writer.append("\n");

                            // 写入数据
                            for (List<Object> row : data) {
                                for (int i = 0; i < row.size(); i++) {
                                    Object value = row.get(i);
                                    String cell = value == null ? "" : value.toString();

                                    // 对包含逗号、引号、换行符的单元格进行处理
                                    if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
                                        cell = "\"" + cell.replace("\"", "\"\"") + "\"";
                                    }

                                    writer.append(cell);
                                    if (i < row.size() - 1) {
                                        writer.append(",");
                                    }
                                }
                                writer.append("\n");
                            }

                            JOptionPane.showMessageDialog(this, "导出成功", "导出数据", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "查询失败: " + result.get("error"), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        menu.add(queryItem);
        menu.add(structureItem);
        menu.addSeparator();
        menu.add(editItem);
        menu.add(dropItem);
        menu.add(emptyItem);
        menu.addSeparator();
        menu.add(exportItem);


        menu.show(databaseTree, x, y);
    }


    */
/**
     * 保存表格更改
     *//*

    private void saveTableChanges() {
        if (currentConnection == null || currentTableName == null) {
            statusLabel.setText("没有可保存的更改");
            return;
        }

        // 获取表格中所有已修改的行
        List<Integer> allModifiedRows = new ArrayList<>(modifiedRows);
        for (int i = 0; i < resultTableModel.getRowCount(); i++) {
            if (resultTableModel.isRowModified(i) && !allModifiedRows.contains(i)) {
                allModifiedRows.add(i);
            }
        }

        if (allModifiedRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有检测到需要保存的更改", "保存更改", JOptionPane.INFORMATION_MESSAGE);
            statusLabel.setText("没有可保存的更改");
            return;
        }

        try {
            // 确保当前表名格式正确，并设置当前数据库名称
            String schema = null;
            String tableName = currentTableName;

            if (currentTableName.contains(".")) {
                String[] parts = currentTableName.split("\\.");
                schema = parts[0];
                tableName = parts[1];

                // 确保当前连接配置使用正确的数据库
                if (schema != null && !schema.isEmpty() && !schema.equals(currentConnection.getDatabase())) {
                    currentConnection.setDatabase(schema);
                }
            }

            int successCount = 0;
            List<String> sqlStatements = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (Integer rowIndex : allModifiedRows) {
                if (rowIndex >= resultTableModel.getRowCount()) {
                    continue;
                }

                // 检查是否为已删除的行
                if (resultTableModel.isRowDeleted(rowIndex)) {
                    String deleteSql = generateDeleteSQL(rowIndex);
                    if (deleteSql != null) {
                        sqlStatements.add(deleteSql);
                    }
                }
                // 检查是否为新增行
                else if (resultTableModel.isNewRow(rowIndex)) {
                    String insertSql = generateInsertSQL(rowIndex);
                    if (insertSql != null) {
                        sqlStatements.add(insertSql);
                    }
                }
                // 否则是更新
                else if (resultTableModel.isRowModified(rowIndex)) {
                    String updateSql = generateUpdateSQL(rowIndex);
                    if (updateSql != null) {
                        sqlStatements.add(updateSql);
                    }
                }
            }

            if (sqlStatements.isEmpty()) {
                JOptionPane.showMessageDialog(this, "没有生成有效的SQL语句，可能缺少必要的主键或数据", "保存更改", JOptionPane.WARNING_MESSAGE);
                statusLabel.setText("没有需要执行的SQL语句");
                return;
            }

            // 执行所有SQL语句
            for (String sql : sqlStatements) {
                try {
                    Map<String, Object> result = DatabaseService.executeUpdate(currentConnection, sql);
                    if ((boolean) result.get("success")) {
                        successCount++;
                    } else {
                        String error = (String) result.get("error");
                        errors.add(error);
                    }
                } catch (Exception e) {
                    errors.add(e.getMessage());
                }
            }

            if (successCount == sqlStatements.size()) {
                statusLabel.setText("保存成功 " + successCount + "/" + sqlStatements.size() + " 项修改");

                // 清除修改标记
                modifiedRows.clear();
                resultTableModel.clearModifications();

                // 刷新表数据
                executeCurrentQuery();

                // 添加成功提示对话框
                JOptionPane.showMessageDialog(this,
                        "已成功保存所有修改，共 " + successCount + " 项",
                        "保存成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                // 显示错误信息
                StringBuilder errorMsg = new StringBuilder();
                errorMsg.append("保存部分成功 ").append(successCount).append("/").append(sqlStatements.size()).append(" 项修改");
                if (!errors.isEmpty()) {
                    errorMsg.append(", 错误: ").append(errors.get(0));
                }
                statusLabel.setText(errorMsg.toString());

                JOptionPane.showMessageDialog(this,
                        "部分更改未能保存，成功: " + successCount + "/" + sqlStatements.size() +
                                (errors.isEmpty() ? "" : "\n错误: " + String.join("\n", errors)),
                        "保存部分成功", JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("保存失败: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "保存更改失败: " + e.getMessage(),
                    "保存失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    */
/**
     * 生成更新SQL语句
     *//*

    private String generateUpdateSQL(int row) {
        if (currentTableName == null || primaryKeys.isEmpty()) return null;

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(currentTableName).append(" SET ");

        List<String> columnNames = resultTableModel.getColumnNames();
        List<Object> values = resultTableModel.getRowData(row);
        List<Object> originalValues = resultTableModel.getOriginalRowData(row);

        boolean hasChanges = false;

        // 构建SET子句
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            Object value = values.get(i);
            Object originalValue = (originalValues != null && i < originalValues.size()) ? originalValues.get(i) : null;

            // 跳过主键
            if (primaryKeys.contains(columnName)) {
                continue;
            }

            // 比较值是否变化
            if ((value == null && originalValue != null) ||
                    (value != null && !value.equals(originalValue))) {

                if (hasChanges) {
                    sql.append(", ");
                }

                sql.append(columnName).append(" = ");

                if (value == null) {
                    sql.append("NULL");
                } else if (value instanceof Number) {
                    sql.append(value);
                } else {
                    sql.append("'").append(value.toString().replace("'", "''")).append("'");
                }

                hasChanges = true;
            }
        }

        // 如果没有修改，返回null
        if (!hasChanges) {
            return null;
        }

        // 构建WHERE子句
        sql.append(" WHERE ");

        boolean firstKey = true;
        for (String key : primaryKeys) {
            int keyIndex = columnNames.indexOf(key);
            if (keyIndex >= 0) {
                if (!firstKey) sql.append(" AND ");
                sql.append(key).append(" = ");

                Object keyValue = originalValues.get(keyIndex);
                if (keyValue == null) {
                    sql.append("NULL");
                } else if (keyValue instanceof Number) {
                    sql.append(keyValue);
                } else {
                    sql.append("'").append(keyValue.toString().replace("'", "''")).append("'");
                }

                firstKey = false;
            }
        }

        return sql.toString();
    }

    */
/**
     * 生成插入SQL语句
     *//*

    private String generateInsertSQL(int row) {
        if (currentTableName == null) return null;

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(currentTableName).append(" (");

        List<String> columnNames = resultTableModel.getColumnNames();
        List<Object> values = resultTableModel.getRowData(row);

        // 构建列名部分
        StringBuilder columns = new StringBuilder();
        StringBuilder valuesPart = new StringBuilder();

        boolean first = true;
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            Object value = values.get(i);

            // 跳过空值
            if (value == null) {
                continue;
            }

            if (!first) {
                columns.append(", ");
                valuesPart.append(", ");
            }

            columns.append(columnName);

            if (value instanceof Number) {
                valuesPart.append(value);
            } else {
                valuesPart.append("'").append(value.toString().replace("'", "''")).append("'");
            }

            first = false;
        }

        // 如果没有有效的值，返回null
        if (first) {
            return null;
        }

        sql.append(columns).append(") VALUES (").append(valuesPart).append(")");

        return sql.toString();
    }

    */
/**
     * 生成删除SQL语句
     *//*

    private String generateDeleteSQL(int row) {
        if (currentTableName == null || primaryKeys.isEmpty()) return null;

        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(currentTableName).append(" WHERE ");

        List<String> columnNames = resultTableModel.getColumnNames();
        List<Object> values = resultTableModel.getOriginalRowData(row);

        boolean firstKey = true;
        for (String key : primaryKeys) {
            int keyIndex = columnNames.indexOf(key);
            if (keyIndex >= 0) {
                if (!firstKey) sql.append(" AND ");
                sql.append(key).append(" = ");

                Object keyValue = values.get(keyIndex);
                if (keyValue == null) {
                    sql.append("NULL");
                } else if (keyValue instanceof Number) {
                    sql.append(keyValue);
                } else {
                    sql.append("'").append(keyValue.toString().replace("'", "''")).append("'");
                }

                firstKey = false;
            }
        }

        return sql.toString();
    }

    */
/**
     * 获取表的主键信息
     *//*

    private void fetchTablePrimaryKeys(String tableName) {
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return DatabaseService.getPrimaryKeys(currentConnection, tableName);
            }

            @Override
            protected void done() {
                try {
                    primaryKeys = get();
                    if (primaryKeys.isEmpty()) {
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "警告：表" + tableName + "没有主键，编辑功能可能无法正常工作。",
                                "缺少主键", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "获取主键信息失败: " + e.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    */
/**
     * 显示表结构
     *//*

    private void showTableStructure(String tableName) {
        if (currentConnection == null) {
            JOptionPane.showMessageDialog(this, "请先连接数据库", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql = "";

        if ("mysql".equalsIgnoreCase(currentConnection.getDatabaseType())) {
            sql = "SHOW FULL COLUMNS FROM " + tableName;
        } else if ("postgresql".equalsIgnoreCase(currentConnection.getDatabaseType())) {
            sql = "SELECT column_name as Field, data_type as Type, is_nullable as Null, column_default as Default, '' as Extra " +
                    "FROM information_schema.columns WHERE table_name = '" + tableName.substring(tableName.indexOf('.') + 1) +
                    "' ORDER BY ordinal_position";
        } else if ("sqlserver".equalsIgnoreCase(currentConnection.getDatabaseType())) {
            sql = "SELECT column_name as Field, data_type as Type, is_nullable as Null, column_default as Default, '' as Extra " +
                    "FROM information_schema.columns WHERE table_name = '" + tableName.substring(tableName.indexOf('.') + 1) + "'";
        } else if ("oracle".equalsIgnoreCase(currentConnection.getDatabaseType())) {
            sql = "SELECT column_name as Field, data_type as Type, nullable as Null, data_default as Default, '' as Extra " +
                    "FROM all_tab_columns WHERE table_name = '" + tableName.substring(tableName.indexOf('.') + 1).toUpperCase() + "'";
        } else {
            sql = "PRAGMA table_info(" + tableName.substring(tableName.indexOf('.') + 1) + ")";
        }

        // 创建一个新窗口显示表结构
        JDialog dialog = new JDialog(this, "表结构: " + tableName, false);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout());

        // 执行查询并获取结果
        Map<String, Object> result = DatabaseService.executeQuery(currentConnection, sql);

        if ((boolean) result.get("success")) {
            // 检查数据类型，根据类型选择合适的方法处理
            Object dataObj = result.get("data");
            List<String> columns = (List<String>) result.get("columns");

            // 创建表格模型
            QueryResultTableModel model = new QueryResultTableModel(false);

            if (dataObj instanceof List<?>) {
                if (!((List<?>) dataObj).isEmpty() && ((List<?>) dataObj).get(0) instanceof Map) {
                    // 如果是List<Map<String, Object>>类型
                    List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;
                    model.setDataFromMap(data, columns);
                } else {
                    // 如果是List<List<Object>>类型
                    List<List<Object>> data = (List<List<Object>>) dataObj;
                    model.setData(columns, data);
                }
            }

            // 创建表格
            JTable table = new JTable(model);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

            // 调整列宽
            TableColumnAdjuster adjuster = new TableColumnAdjuster(table);
            adjuster.adjustColumns();

            JScrollPane scrollPane = new JScrollPane(table);
            panel.add(scrollPane, BorderLayout.CENTER);

            // 添加操作按钮
            JPanel buttonPanel = new JPanel();
            JButton addColumnButton = new JButton("添加列");
            JButton dropColumnButton = new JButton("删除列");
            JButton closeButton = new JButton("关闭");

            addColumnButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showAddColumnDialog(tableName);
                    dialog.dispose();
                }
            });

            dropColumnButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow != -1) {
                        String columnName = model.getValueAt(selectedRow, 0).toString();

                        if (JOptionPane.showConfirmDialog(dialog,
                                "确定要删除列 " + columnName + " 吗？此操作不可逆！",
                                "确认删除", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

                            String dropSql = "ALTER TABLE " + tableName + " DROP COLUMN " + columnName;
                            executeSQL(dropSql);
                            dialog.dispose();
                        }
                    } else {
                        JOptionPane.showMessageDialog(dialog, "请先选择一列", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            closeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });

            buttonPanel.add(addColumnButton);
            buttonPanel.add(dropColumnButton);
            buttonPanel.add(closeButton);

            panel.add(buttonPanel, BorderLayout.SOUTH);
        } else {
            panel.add(new JLabel("获取表结构失败: " + result.get("error")), BorderLayout.CENTER);
        }

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    */
/**
     * 显示添加列对话框
     *//*

    private void showAddColumnDialog(String tableName) {
        // 创建对话框
        JDialog dialog = new JDialog(this, "添加列到 " + tableName, true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 列名
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("列名:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        JTextField columnNameField = new JTextField(20);
        panel.add(columnNameField, gbc);

        // 数据类型
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("数据类型:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        JComboBox<String> dataTypeCombo = new JComboBox<>();

        // 根据数据库类型添加数据类型
        if ("mysql".equalsIgnoreCase(currentConnection.getDatabaseType())) {
            dataTypeCombo.addItem("VARCHAR(255)");
            dataTypeCombo.addItem("INT");
            dataTypeCombo.addItem("TINYINT");
            dataTypeCombo.addItem("SMALLINT");
            dataTypeCombo.addItem("MEDIUMINT");
            dataTypeCombo.addItem("BIGINT");
            dataTypeCombo.addItem("FLOAT");
            dataTypeCombo.addItem("DOUBLE");
            dataTypeCombo.addItem("DECIMAL(10,2)");
            dataTypeCombo.addItem("DATE");
            dataTypeCombo.addItem("DATETIME");
            dataTypeCombo.addItem("TIMESTAMP");
            dataTypeCombo.addItem("TIME");
            dataTypeCombo.addItem("YEAR");
            dataTypeCombo.addItem("CHAR(1)");
            dataTypeCombo.addItem("TEXT");
            dataTypeCombo.addItem("BLOB");
        } else if ("postgresql".equalsIgnoreCase(currentConnection.getDatabaseType())) {
            dataTypeCombo.addItem("VARCHAR(255)");
            dataTypeCombo.addItem("INTEGER");
            dataTypeCombo.addItem("SMALLINT");
            dataTypeCombo.addItem("BIGINT");
            dataTypeCombo.addItem("REAL");
            dataTypeCombo.addItem("DOUBLE PRECISION");
            dataTypeCombo.addItem("NUMERIC(10,2)");
            dataTypeCombo.addItem("DATE");
            dataTypeCombo.addItem("TIMESTAMP");
            dataTypeCombo.addItem("TIME");
            dataTypeCombo.addItem("CHAR(1)");
            dataTypeCombo.addItem("TEXT");
            dataTypeCombo.addItem("BYTEA");
        } else {
            dataTypeCombo.addItem("VARCHAR(255)");
            dataTypeCombo.addItem("INTEGER");
            dataTypeCombo.addItem("FLOAT");
            dataTypeCombo.addItem("DATE");
            dataTypeCombo.addItem("TEXT");
        }

        panel.add(dataTypeCombo, gbc);

        // 可为空
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("可为空:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        JCheckBox nullableCheckBox = new JCheckBox();
        nullableCheckBox.setSelected(true);
        panel.add(nullableCheckBox, gbc);

        // 默认值
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("默认值:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        JTextField defaultValueField = new JTextField(20);
        panel.add(defaultValueField, gbc);

        // 按钮
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("添加");
        JButton cancelButton = new JButton("取消");

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String columnName = columnNameField.getText().trim();
                String dataType = (String) dataTypeCombo.getSelectedItem();
                boolean nullable = nullableCheckBox.isSelected();
                String defaultValue = defaultValueField.getText().trim();

                if (columnName.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "请输入列名", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // 构建SQL语句
                StringBuilder sql = new StringBuilder();
                sql.append("ALTER TABLE ").append(tableName).append(" ADD COLUMN ").append(columnName).append(" ").append(dataType);

                if (!nullable) {
                    sql.append(" NOT NULL");
                }

                if (!defaultValue.isEmpty()) {
                    sql.append(" DEFAULT ").append(defaultValue);
                }

                // 执行SQL
                executeSQL(sql.toString());

                dialog.dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);

        dialog.setLayout(new BorderLayout());
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    */
/**
     * 显示编辑表对话框
     *//*

    private void showEditTableDialog(String tableName) {
        // 创建对话框
        JDialog dialog = new JDialog(this, "编辑表: " + tableName, true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout());

        // 获取表结构
        String sql = "";

        if ("mysql".equalsIgnoreCase(currentConnection.getDatabaseType())) {
            sql = "SHOW FULL COLUMNS FROM " + tableName;
        } else if ("postgresql".equalsIgnoreCase(currentConnection.getDatabaseType())) {
            sql = "SELECT column_name as Field, data_type as Type, is_nullable as Null, column_default as Default, '' as Extra " +
                    "FROM information_schema.columns WHERE table_name = '" + tableName.substring(tableName.indexOf('.') + 1) +
                    "' ORDER BY ordinal_position";
        } else {
            sql = "PRAGMA table_info(" + tableName.substring(tableName.indexOf('.') + 1) + ")";
        }

        // 执行查询
        Map<String, Object> result = DatabaseService.executeQuery(currentConnection, sql);

        if ((boolean) result.get("success")) {
            // 使用安全的类型检查和转换
            Object rawData = result.get("data");
            List<String> columns = (List<String>) result.get("columns");

            // 创建表格模型
            QueryResultTableModel model = new QueryResultTableModel(false);

            // 根据数据类型调用适当的方法
            if (rawData instanceof List) {
                model.setDataFromMap((List<?>) rawData, columns);
            }

            // 创建表格
            JTable table = new JTable(model);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

            // 调整列宽
            TableColumnAdjuster adjuster = new TableColumnAdjuster(table);
            adjuster.adjustColumns();

            JScrollPane scrollPane = new JScrollPane(table);
            panel.add(scrollPane, BorderLayout.CENTER);

            // 添加操作按钮
            JPanel buttonPanel = new JPanel();
            JButton renameTableButton = new JButton("重命名表");
            JButton addColumnButton = new JButton("添加列");
            JButton dropColumnButton = new JButton("删除列");
            JButton closeButton = new JButton("关闭");

            renameTableButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String newName = JOptionPane.showInputDialog(dialog, "输入新表名:", tableName.substring(tableName.indexOf('.') + 1));

                    if (newName != null && !newName.trim().isEmpty()) {
                        String schema = tableName.substring(0, tableName.indexOf('.'));
                        String sql = "ALTER TABLE " + tableName + " RENAME TO " + schema + "." + newName;
                        executeSQL(sql);
                        dialog.dispose();
                    }
                }
            });

            addColumnButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showAddColumnDialog(tableName);
                    dialog.dispose();
                }
            });

            dropColumnButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow != -1) {
                        String columnName = model.getValueAt(selectedRow, 0).toString();

                        if (JOptionPane.showConfirmDialog(dialog,
                                "确定要删除列 " + columnName + " 吗？此操作不可逆！",
                                "确认删除", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

                            String dropSql = "ALTER TABLE " + tableName + " DROP COLUMN " + columnName;
                            executeSQL(dropSql);
                            dialog.dispose();
                        }
                    } else {
                        JOptionPane.showMessageDialog(dialog, "请先选择一列", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            closeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });

            buttonPanel.add(renameTableButton);
            buttonPanel.add(addColumnButton);
            buttonPanel.add(dropColumnButton);
            buttonPanel.add(closeButton);

            panel.add(buttonPanel, BorderLayout.SOUTH);
        } else {
            panel.add(new JLabel("获取表结构失败: " + result.get("error")), BorderLayout.CENTER);
        }

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    */
/**
     * 显示连接上下文菜单
     *//*

    private void showConnectionContextMenu(ConnectionConfig config, int x, int y) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem connectItem = new JMenuItem("连接");
        JMenuItem editItem = new JMenuItem("编辑连接");
        JMenuItem deleteItem = new JMenuItem("删除连接");
        JMenuItem refreshItem = new JMenuItem("刷新");

        connectItem.addActionListener(e -> {
            connectDatabase(config);
        });

        editItem.addActionListener(e -> {
            ConnectionDialog dialog = new ConnectionDialog(this, config, false);
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                ConnectionConfig newConfig = dialog.getConnectionConfig();
                ConnectionManager.updateConnection(newConfig);
                loadConnections();
            }
        });

        deleteItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "确定要删除连接 " + config.getName() + " 吗？",
                    "确认删除", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                ConnectionManager.deleteConnection(config);
                loadConnections();
            }
        });

        refreshItem.addActionListener(e -> {
            currentConnection = config;
            refreshDatabaseTree();
        });

        menu.add(connectItem);
        menu.add(editItem);
        menu.add(deleteItem);
        menu.addSeparator();
        menu.add(refreshItem);

        menu.show(databaseTree, x, y);
    }

    */
/**
     * 显示数据库/schema上下文菜单
     *//*

    private void showSchemaContextMenu(ConnectionConfig config, String schemaName, int x, int y) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem refreshItem = new JMenuItem("刷新");
        JMenuItem newDatabaseItem = new JMenuItem("新建数据库");
        JMenuItem alterDatabaseItem = new JMenuItem("修改数据库");
        JMenuItem dropDatabaseItem = new JMenuItem("删除数据库");
        JMenuItem newTableItem = new JMenuItem("新建表");
        JMenuItem queryItem = new JMenuItem("执行查询");
        // 添加导出SQL选项
        JMenuItem exportSqlItem = new JMenuItem("导出SQL");
        // 添加批量执行SQL选项
        JMenuItem batchExecuteSqlItem = new JMenuItem("批量执行SQL");

        refreshItem.addActionListener(e -> {
            currentConnection = config;
            refreshDatabaseTree();
        });

        newDatabaseItem.addActionListener(e -> {
            createNewDatabase(config);
        });

        alterDatabaseItem.addActionListener(e -> {
            alterDatabase(config, schemaName);
        });

        dropDatabaseItem.addActionListener(e -> {
            dropDatabase(config, schemaName);
        });

        newTableItem.addActionListener(e -> {
            createNewTable(config, schemaName);
        });

        queryItem.addActionListener(e -> {
            if ("mysql".equalsIgnoreCase(config.getDatabaseType())) {
                sqlTextArea.setText("USE " + schemaName + ";\nSHOW TABLES;");
            } else {
                sqlTextArea.setText("SELECT * FROM information_schema.tables WHERE table_schema = '" + schemaName + "'");
            }

            currentConnection = config;
            executeSQL();
        });

        exportSqlItem.addActionListener(e -> {
            exportDatabaseSql(config, schemaName);
        });

        batchExecuteSqlItem.addActionListener(e -> {
            batchExecuteSql(config, schemaName);
        });

        menu.add(refreshItem);
        menu.addSeparator();
        menu.add(newDatabaseItem);
        menu.add(alterDatabaseItem);
        menu.add(dropDatabaseItem);
        menu.addSeparator();
        menu.add(newTableItem);
        menu.add(queryItem);
        menu.addSeparator();
        menu.add(exportSqlItem);
        menu.add(batchExecuteSqlItem);

        menu.show(databaseTree, x, y);
    }

    */
/**
     * 修改数据库
     *//*

    private void alterDatabase(ConnectionConfig config, String dbName) {
        // 检查是否是MySQL数据库，只有MySQL支持修改字符集和排序规则
        if (!"mysql".equalsIgnoreCase(config.getDatabaseType())) {
            JOptionPane.showMessageDialog(this,
                    "只有MySQL数据库支持此操作",
                    "不支持的操作",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 先查询当前数据库的字符集和排序规则
        String currentCharset = "utf8mb4";
        String currentCollation = "utf8mb4_general_ci";

        try {
            // 确保当前连接使用正确的数据库
            ConnectionConfig tempConfig = new ConnectionConfig(
                    config.getName(),
                    config.getDatabaseType(),
                    config.getHost(),
                    config.getPort(),
                    dbName,
                    config.getUsername(),
                    config.getPassword()
            );

            // 查询当前数据库的字符集和排序规则
            String sql = "SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '" + dbName + "'";
            Map<String, Object> result = DatabaseService.executeQuery(tempConfig, sql);

            if ((boolean) result.get("success")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                if (data.size() > 0) {
                    Map<String, Object> row = data.get(0);
                    if (row.containsKey("DEFAULT_CHARACTER_SET_NAME") && row.containsKey("DEFAULT_COLLATION_NAME")) {
                        currentCharset = row.get("DEFAULT_CHARACTER_SET_NAME").toString();
                        currentCollation = row.get("DEFAULT_COLLATION_NAME").toString();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 出错时使用默认值，不影响界面显示
        }

        // 创建弹出对话框
        JDialog dialog = new JDialog(this, "修改数据库", true);
        dialog.setSize(400, 180);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 数据库名称（仅显示，不可修改）
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("数据库名称:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        JTextField dbNameField = new JTextField(dbName);
        dbNameField.setEditable(false);
        panel.add(dbNameField, gbc);

        // 字符集
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("字符集:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;

        // 创建字符集下拉框，设置当前值
        String[] charsets = {"utf8mb4", "utf8", "latin1", "gbk", "ascii"};
        JComboBox<String> charsetCombo = new JComboBox<>(charsets);

        // 设置当前选中的字符集
        boolean foundCharset = false;
        for (int i = 0; i < charsets.length; i++) {
            if (charsets[i].equalsIgnoreCase(currentCharset)) {
                charsetCombo.setSelectedIndex(i);
                foundCharset = true;
                break;
            }
        }

        // 如果找不到当前字符集，添加到列表中
        if (!foundCharset) {
            charsetCombo.addItem(currentCharset);
            charsetCombo.setSelectedItem(currentCharset);
        }

        panel.add(charsetCombo, gbc);

        // 排序规则
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("排序规则:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;

        // 排序规则下拉框（根据字符集自动更新）
        JComboBox<String> collationCombo = new JComboBox<>();
        updateCollations(charsetCombo.getSelectedItem().toString(), collationCombo);

        // 设置当前选中的排序规则
        boolean foundCollation = false;
        for (int i = 0; i < collationCombo.getItemCount(); i++) {
            if (collationCombo.getItemAt(i).equalsIgnoreCase(currentCollation)) {
                collationCombo.setSelectedIndex(i);
                foundCollation = true;
                break;
            }
        }

        // 如果找不到当前排序规则，添加到列表中
        if (!foundCollation) {
            collationCombo.addItem(currentCollation);
            collationCombo.setSelectedItem(currentCollation);
        }

        panel.add(collationCombo, gbc);

        // 更新排序规则选项
        charsetCombo.addActionListener(e -> {
            updateCollations(charsetCombo.getSelectedItem().toString(), collationCombo);
        });

        // 按钮面板
        JPanel buttonPanel = new JPanel();
        JButton updateButton = new JButton("修改");
        JButton cancelButton = new JButton("取消");

        updateButton.addActionListener(e -> {
            String charset = charsetCombo.getSelectedItem().toString();
            String collation = collationCombo.getSelectedItem().toString();

            currentConnection = config;

            // 确保当前连接使用正确的数据库
            if (config.getDatabase() == null || !config.getDatabase().equals(dbName)) {
                config.setDatabase(dbName);
            }

            String sql = String.format("ALTER DATABASE %s CHARACTER SET %s COLLATE %s",
                    dbName, charset, collation);

            // 执行SQL
            Map<String, Object> result = DatabaseService.executeUpdate(currentConnection, sql);
            if ((boolean) result.get("success")) {
                JOptionPane.showMessageDialog(dialog,
                        "数据库 " + dbName + " 已成功修改！",
                        "修改成功",
                        JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();

                // 刷新数据库树
                refreshDatabaseTree();
            } else {
                JOptionPane.showMessageDialog(dialog,
                        "修改数据库失败: " + result.get("error"),
                        "修改失败",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> {
            dialog.dispose();
        });

        buttonPanel.add(updateButton);
        buttonPanel.add(cancelButton);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }


    */
/**
     * 更新排序规则下拉框
     *//*

    private void updateCollations(String charset, JComboBox<String> collationCombo) {
        collationCombo.removeAllItems();

        // 为不同字符集添加适合的排序规则
        switch (charset) {
            case "utf8mb4":
                collationCombo.addItem("utf8mb4_general_ci");
                collationCombo.addItem("utf8mb4_unicode_ci");
                collationCombo.addItem("utf8mb4_bin");
                collationCombo.addItem("utf8mb4_0900_ai_ci");
                break;
            case "utf8":
                collationCombo.addItem("utf8_general_ci");
                collationCombo.addItem("utf8_unicode_ci");
                collationCombo.addItem("utf8_bin");
                break;
            case "latin1":
                collationCombo.addItem("latin1_swedish_ci");
                collationCombo.addItem("latin1_general_ci");
                collationCombo.addItem("latin1_bin");
                break;
            case "gbk":
                collationCombo.addItem("gbk_chinese_ci");
                collationCombo.addItem("gbk_bin");
                break;
            case "ascii":
                collationCombo.addItem("ascii_general_ci");
                collationCombo.addItem("ascii_bin");
                break;
            default:
                collationCombo.addItem(charset + "_general_ci");
        }
    }

    */
/**
     * 创建新数据库
     *//*

    private void createNewDatabase(ConnectionConfig config) {
        // 创建弹出对话框
        JDialog dialog = new JDialog(this, "新建数据库", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 数据库名称
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("数据库名称:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        JTextField dbNameField = new JTextField(20);
        panel.add(dbNameField, gbc);

        // 字符集
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("字符集:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;

        // 创建字符集下拉框
        String[] charsets = {"utf8mb4", "utf8", "latin1", "gbk", "ascii"};
        JComboBox<String> charsetCombo = new JComboBox<>(charsets);
        panel.add(charsetCombo, gbc);

        // 排序规则
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("排序规则:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;

        // 排序规则下拉框（根据字符集自动更新）
        JComboBox<String> collationCombo = new JComboBox<>();
        updateCollations(charsetCombo.getSelectedItem().toString(), collationCombo);
        panel.add(collationCombo, gbc);

        // 更新排序规则选项
        charsetCombo.addActionListener(e -> {
            updateCollations(charsetCombo.getSelectedItem().toString(), collationCombo);
        });

        // 按钮面板
        JPanel buttonPanel = new JPanel();
        JButton createButton = new JButton("创建");
        JButton cancelButton = new JButton("取消");

        createButton.addActionListener(e -> {
            String dbName = dbNameField.getText().trim();
            if (dbName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "请输入数据库名称", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String charset = charsetCombo.getSelectedItem().toString();
            String collation = collationCombo.getSelectedItem().toString();

            currentConnection = config;
            String sql = String.format("CREATE DATABASE %s CHARACTER SET %s COLLATE %s",
                    dbName, charset, collation);

            // 执行SQL
            Map<String, Object> result = DatabaseService.executeUpdate(currentConnection, sql);
            if ((boolean) result.get("success")) {
                JOptionPane.showMessageDialog(dialog, "数据库 " + dbName + " 创建成功！",
                        "创建成功", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();

                // 刷新数据库树
                refreshDatabaseTree();
            } else {
                JOptionPane.showMessageDialog(dialog, "创建数据库失败: " + result.get("error"),
                        "创建失败", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> {
            dialog.dispose();
        });

        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    */
/**
     * 显示Redis数据库上下文菜单
     *//*

    private void showRedisDatabaseContextMenu(ConnectionConfig config, String dbName, int x, int y) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem selectItem = new JMenuItem("选择数据库");
        JMenuItem flushItem = new JMenuItem("清空数据库");
        JMenuItem refreshItem = new JMenuItem("刷新");

        selectItem.addActionListener(e -> {
            // 选择Redis数据库操作
            currentConnection = config;
            sqlTextArea.setText("SELECT " + dbName.replace("db", ""));
            executeSQL();
        });

        flushItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "确定要清空数据库 " + dbName + " 吗？此操作不可逆！",
                    "确认清空", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                // 清空Redis数据库操作
                currentConnection = config;
                sqlTextArea.setText("FLUSHDB");
                executeSQL();
            }
        });

        refreshItem.addActionListener(e -> {
            currentConnection = config;
            refreshDatabaseTree();
        });

        menu.add(selectItem);
        menu.add(flushItem);
        menu.add(refreshItem);

        menu.show(databaseTree, x, y);
    }

    */
/**
     * 显示Redis类型上下文菜单
     *//*

    private void showRedisTypeContextMenu(ConnectionConfig config, String dbName, String typeName, int x, int y) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem queryItem = new JMenuItem("查询键");

        queryItem.addActionListener(e -> {
            // 查询Redis键操作
            currentConnection = config;

            String cmd = "";
            switch (typeName.toLowerCase()) {
                case "string":
                    cmd = "KEYS *";
                    break;
                case "hash":
                    cmd = "HGETALL *";
                    break;
                case "list":
                    cmd = "LRANGE * 0 -1";
                    break;
                case "set":
                    cmd = "SMEMBERS *";
                    break;
                case "zset":
                    cmd = "ZRANGE * 0 -1 WITHSCORES";
                    break;
                default:
                    cmd = "KEYS *";
            }

            sqlTextArea.setText(cmd);
            executeSQL();
        });

        menu.add(queryItem);

        menu.show(databaseTree, x, y);
    }

    */
/**
     * 导出数据库SQL，包括表结构和数据
     *
     * @param config     数据库连接配置
     * @param schemaName 模式名称
     *//*

    private void exportDatabaseSql(ConnectionConfig config, String schemaName) {
        // Redis不支持SQL导出
        if (config.getDatabaseType().equalsIgnoreCase("redis")) {
            JOptionPane.showMessageDialog(this, "Redis不支持SQL导出功能", "不支持的操作", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存SQL文件");
        fileChooser.setFileFilter(new FileNameExtensionFilter("SQL文件(*.sql)", "sql"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".sql")) {
                filePath += ".sql";
            }

            // 创建进度对话框
            JDialog progressDialog = new JDialog(this, "导出进度", true);
            progressDialog.setLayout(new BorderLayout());
            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            progressBar.setString("准备导出...");
            JLabel statusLabel = new JLabel("准备导出数据...");
            statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            JButton cancelButton = new JButton("取消");

            AtomicBoolean cancelled = new AtomicBoolean(false);
            cancelButton.addActionListener(e -> cancelled.set(true));

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(cancelButton);

            progressDialog.add(statusLabel, BorderLayout.NORTH);
            progressDialog.add(progressBar, BorderLayout.CENTER);
            progressDialog.add(buttonPanel, BorderLayout.SOUTH);
            progressDialog.setSize(400, 150);
            progressDialog.setLocationRelativeTo(this);

            // 使用SwingWorker在后台导出SQL
            String finalFilePath = filePath;
            SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try (FileWriter writer = new FileWriter(finalFilePath)) {
                        // 写入文件头部，包含数据库信息和导出时间
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String header = String.format("-- 导出自数据库: %s\n" +
                                        "-- 数据库类型: %s\n" +
                                        "-- 模式名称: %s\n" +
                                        "-- 导出时间: %s\n\n",
                                config.getName(), config.getDatabaseType(), schemaName, dateFormat.format(new Date()));
                        writer.write(header);

                        // 添加USE语句
                        writer.write(String.format("USE `%s`;\n\n", schemaName));

                        // 获取当前模式下的所有表
                        List<String> tables = DatabaseService.getTables(config, schemaName);
                        int totalTables = tables.size();

                        for (int i = 0; i < tables.size(); i++) {
                            if (cancelled.get()) {
                                break;
                            }

                            String tableName = tables.get(i);
                            int progress = (i * 100) / totalTables;
                            publish(progress);
                            setStatus("正在导出表: " + tableName);

                            // 导出表结构
                            writer.write("-- 表结构: " + tableName + "\n");
                            StringBuilder createTableSql = new StringBuilder("CREATE TABLE " + tableName + " (\n");

                            // 获取表列信息
                            List<Map<String, String>> columns = DatabaseService.getColumns(config, schemaName, tableName);

                            // 获取主键信息 - 对表名做处理，如果包含schema前缀需要去掉
                            String tableNameForPk = tableName;
                            if (tableName.contains(".")) {
                                String[] parts = tableName.split("\\.");
                                tableNameForPk = parts[1];
                            }
                            List<String> primaryKeys = DatabaseService.getPrimaryKeys(config, tableNameForPk);

                            // 去除主键列表中的重复项
                            primaryKeys = getPrimaryKeys(primaryKeys);

                            // 标记表结构是否已写入文件
                            boolean tableWritten = false;

                            int columnCount = 0;
                            for (Map<String, String> column : columns) {
                                if (columnCount > 0) {
                                    createTableSql.append(",\n");
                                }

                                String columnName = column.get("name");
                                String typeName = column.get("type");

                                if (columnName == null || typeName == null) {
                                    // 尝试使用不同的key - 可能是大写
                                    columnName = column.get("COLUMN_NAME");
                                    typeName = column.get("TYPE_NAME");
                                }

                                if (columnName == null || typeName == null) {
                                    // 如果仍然为null，使用默认值避免NullPointerException
                                    columnName = column.get("NAME") != null ? column.get("NAME") : "unknown_column";
                                    typeName = column.get("TYPE") != null ? column.get("TYPE") : "VARCHAR";
                                }

                                createTableSql.append("  ").append(columnName).append(" ")
                                        .append(typeName);

                                // 添加大小信息（如果有）
                                String size = column.get("size");
                                if (size == null) {
                                    size = column.get("COLUMN_SIZE");
                                }
                                if (size == null) {
                                    size = column.get("SIZE");
                                }

                                if (size != null && !size.isEmpty()) {
                                    createTableSql.append("(").append(size).append(")");
                                }

                                // NOT NULL 约束
                                String nullable = column.get("nullable");
                                if (nullable == null) {
                                    nullable = column.get("IS_NULLABLE");
                                }

                                if ("NO".equals(nullable)) {
                                    createTableSql.append(" NOT NULL");
                                }

                                columnCount++;
                            }

                            // 添加主键约束
                            if (!primaryKeys.isEmpty()) {
                                createTableSql.append(",\n  PRIMARY KEY (");

                                // 去除主键列表中的重复项
                                Set<String> uniquePrimaryKeysSe = new LinkedHashSet<>(primaryKeys);
                                List<String> uniquePKList = new ArrayList<>(uniquePrimaryKeysSe);

                                for (int p = 0; p < uniquePKList.size(); p++) {
                                    if (p > 0) {
                                        createTableSql.append(", ");
                                    }
                                    createTableSql.append(uniquePKList.get(p));
                                }
                                createTableSql.append(")");
                            }

                            // 添加collate设置（针对MySQL）作为CREATE TABLE的一部分
                            if ("mysql".equalsIgnoreCase(config.getDatabaseType())) {
                                // 尝试查询表的字符集和排序规则
                                try {
                                    Map<String, Object> collateResult = DatabaseService.executeQuery(
                                            config,
                                            "SELECT TABLE_COLLATION FROM information_schema.TABLES " +
                                                    "WHERE TABLE_SCHEMA = '" + schemaName + "' " +
                                                    "AND TABLE_NAME = '" + tableNameForPk + "'"
                                    );

                                    if ((boolean) collateResult.get("success")) {
                                        List<List<Object>> collateData = (List<List<Object>>) collateResult.get("data");
                                        if (!collateData.isEmpty() && !collateData.get(0).isEmpty() && collateData.get(0).get(0) != null) {
                                            String collation = collateData.get(0).get(0).toString();
                                            createTableSql.append("\n) COLLATE = ").append(collation).append(";\n\n");
                                            writer.write(createTableSql.toString());
                                            // 设置已写入标志，避免重复写入
                                            tableWritten = true;
                                        }
                                    }
                                } catch (Exception ex) {
                                    // 忽略查询字符集错误，不影响整体导出
                                    System.err.println("获取表字符集失败: " + ex.getMessage());
                                }
                            }

                            // 如果没有添加collate（非MySQL或查询失败），则正常结束CREATE TABLE语句
                            if (!tableWritten) {
                                createTableSql.append("\n);\n\n");
                                writer.write(createTableSql.toString());
                            }

                            // 导出表数据
                            writer.write("-- 表数据: " + tableName + "\n");
                            Map<String, Object> queryResult = DatabaseService.executeQuery(config, "SELECT * FROM " + tableName);

                            if ((boolean) queryResult.get("success")) {
                                List<String> columnNames = (List<String>) queryResult.get("columns");
                                List<List<Object>> rows = (List<List<Object>>) queryResult.get("data");

                                for (List<Object> row : rows) {
                                    if (cancelled.get()) {
                                        break;
                                    }

                                    StringBuilder insertSql = new StringBuilder("INSERT INTO " + tableName + " (");
                                    StringBuilder values = new StringBuilder("VALUES (");

                                    boolean first = true;
                                    for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                                        if (!first) {
                                            insertSql.append(", ");
                                            values.append(", ");
                                        }
                                        insertSql.append(columnNames.get(colIndex));

                                        Object value = colIndex < row.size() ? row.get(colIndex) : null;
                                        if (value == null) {
                                            values.append("NULL");
                                        } else if (value instanceof String) {
                                            values.append("'").append(((String) value).replace("'", "''")).append("'");
                                        } else if (value instanceof Date) {
                                            values.append("'").append(dateFormat.format(value)).append("'");
                                        } else {
                                            values.append(value);
                                        }

                                        first = false;
                                    }

                                    insertSql.append(") ");
                                    values.append(");");
                                    writer.write(insertSql.toString() + values.toString() + "\n");
                                }
                            }
                            writer.write("\n");
                        }

                        if (!cancelled.get()) {
                            publish(100);
                            setStatus("导出完成");
                        } else {
                            setStatus("导出已取消");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(MainFrame.this, "导出失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                    return null;
                }

                private List<String> getPrimaryKeys(List<String> primaryKeys) {
                    Set<String> uniquePrimaryKeysSe = new LinkedHashSet<>(primaryKeys);
                    primaryKeys = new ArrayList<>(uniquePrimaryKeysSe);
                    return primaryKeys;
                }

                private void setStatus(String status) {
                    SwingUtilities.invokeLater(() -> statusLabel.setText(status));
                }

                @Override
                protected void process(List<Integer> chunks) {
                    int progress = chunks.get(chunks.size() - 1);
                    progressBar.setValue(progress);
                    progressBar.setString(progress + "%");
                }

                @Override
                protected void done() {
                    progressDialog.dispose();
                    if (!cancelled.get()) {
                        JOptionPane.showMessageDialog(MainFrame.this, "SQL导出成功", "成功", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            };

            worker.execute();
            progressDialog.setVisible(true);
        }
    }

    */
/**
     * 批量执行SQL语句
     *
     * @param config     数据库连接配置
     * @param schemaName 模式名称
     *//*

    private void batchExecuteSql(ConnectionConfig config, String schemaName) {
        // Redis不支持批量执行SQL
        if (config.getDatabaseType().equalsIgnoreCase("redis")) {
            JOptionPane.showMessageDialog(this, "Redis不支持批量执行SQL功能", "不支持的操作", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择SQL文件");
        fileChooser.setFileFilter(new FileNameExtensionFilter("SQL文件(*.sql)", "sql"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();

            // 创建设置对话框
            JDialog settingsDialog = new JDialog(this, "执行设置", true);
            settingsDialog.setLayout(new BorderLayout());

            JPanel settingsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
            settingsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel threadsLabel = new JLabel("线程数:");
            JSpinner threadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 16, 1));

            JLabel batchSizeLabel = new JLabel("批处理大小:");
            JSpinner batchSizeSpinner = new JSpinner(new SpinnerNumberModel(200, 10, 1000, 10));

            settingsPanel.add(threadsLabel);
            settingsPanel.add(threadsSpinner);
            settingsPanel.add(batchSizeLabel);
            settingsPanel.add(batchSizeSpinner);

            JPanel buttonPanel = new JPanel();
            JButton executeButton = new JButton("执行");
            JButton cancelButton = new JButton("取消");

            buttonPanel.add(executeButton);
            buttonPanel.add(cancelButton);

            settingsDialog.add(settingsPanel, BorderLayout.CENTER);
            settingsDialog.add(buttonPanel, BorderLayout.SOUTH);
            settingsDialog.pack();
            settingsDialog.setLocationRelativeTo(this);

            cancelButton.addActionListener(e -> settingsDialog.dispose());

            executeButton.addActionListener(e -> {
                int threads = (int) threadsSpinner.getValue();
                int batchSize = (int) batchSizeSpinner.getValue();
                settingsDialog.dispose();

                // 执行SQL批处理
                executeSqlBatch(config, schemaName, filePath, threads, batchSize);
            });

            settingsDialog.setVisible(true);
        }
    }

    */
/**
     * 执行SQL文件批处理
     *//*

    private void executeSqlBatch(ConnectionConfig config, String schemaName, String filePath, int threads, int batchSize) {
        // 创建进度对话框
        JDialog progressDialog = new JDialog(this, "执行进度", true);
        progressDialog.setLayout(new BorderLayout());

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("准备执行...");

        JLabel statusLabel = new JLabel("正在解析SQL文件...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton stopButton = new JButton("停止");
        AtomicBoolean stopped = new AtomicBoolean(false);
        stopButton.addActionListener(e -> stopped.set(true));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(stopButton);

        progressDialog.add(statusLabel, BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);
        progressDialog.add(buttonPanel, BorderLayout.SOUTH);
        progressDialog.setSize(400, 150);
        progressDialog.setLocationRelativeTo(this);

        // 使用SwingWorker在后台执行SQL
        SwingWorker<Void, Object[]> worker = new SwingWorker<Void, Object[]>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // 读取SQL文件并解析成独立的SQL语句
                    File file = new File(filePath);
                    String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));

                    // 简单分割SQL语句（以分号结尾）
                    List<String> sqlStatements = new ArrayList<>();
                    StringBuilder currentStatement = new StringBuilder();
                    boolean inString = false;
                    boolean escaped = false;

                    for (char c : content.toCharArray()) {
                        // 这里增加判断，如果读取行以-- 开头，那么这行就是注释，直接忽略，从下一行开始重新读取
                        if (c == '\n' && currentStatement.toString().trim().startsWith("--")) {
                            currentStatement = new StringBuilder();
                            continue;
                        }

                        currentStatement.append(c);

                        if (c == '\'' && !escaped) {
                            inString = !inString;
                        }

                        escaped = c == '\\' && !escaped;

                        if (c == ';' && !inString) {
                            String sql = currentStatement.toString().trim();
                            if (!sql.isEmpty() && !sql.startsWith("--")) {
                                sqlStatements.add(sql);
                            }
                            currentStatement = new StringBuilder();
                        }
                    }

                    // 如果最后一个语句没有分号
                    String finalSql = currentStatement.toString().trim();
                    if (!finalSql.isEmpty() && !finalSql.startsWith("--")) {
                        sqlStatements.add(finalSql);
                    }

                    // 过滤注释和空语句
                    sqlStatements = sqlStatements.stream()
                            .map(String::trim)
                            .filter(sql -> !sql.isEmpty() && !sql.startsWith("--"))
                            .collect(java.util.stream.Collectors.toList());

                    int totalStatements = sqlStatements.size();
                    publish(new Object[]{"共找到 " + totalStatements + " 条SQL语句", 0});

                    // 分类SQL语句为DDL(CREATE TABLE)和DML(INSERT等其他语句)
                    List<String> createTableStatements = new ArrayList<>();
                    List<String> otherStatements = new ArrayList<>();

                    // 将SQL语句分为CREATE TABLE和其他语句
                    for (String sql : sqlStatements) {
                        // 判断是否是CREATE TABLE语句
                        if (sql.toUpperCase().contains("CREATE TABLE")) {
                            createTableStatements.add(sql);
                        } else {
                            otherStatements.add(sql);
                        }
                    }

                    publish(new Object[]{"正在执行表结构创建语句...", 0});

                    // 创建线程池执行SQL语句
                    ExecutorService executor = Executors.newFixedThreadPool(threads);

                    // 批量执行
                    AtomicInteger completedCount = new AtomicInteger(0);
                    AtomicInteger errorCount = new AtomicInteger(0);
                    List<Future<?>> futures = new ArrayList<>();

                    // 1. 首先执行所有CREATE TABLE语句 - 这些语句顺序执行，不并行
                    for (String sql : createTableStatements) {
                        if (stopped.get()) {
                            break;
                        }

                        try {
                            // 执行建表语句
                            Map<String, Object> result = DatabaseService.executeUpdate(config, sql);
                            boolean success = (boolean) result.get("success");

                            if (success) {
                                int completed = completedCount.incrementAndGet();
                                int progress = (completed * 100) / totalStatements;
                                publish(new Object[]{"已执行表结构语句: " + completed + "/" + createTableStatements.size() +
                                        ", 错误: " + errorCount.get(), progress});
                            } else {
                                errorCount.incrementAndGet();
                                String errorMessage = (String) result.get("message");
                                publish(new Object[]{"表结构语句错误: " + errorMessage, -1});
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            publish(new Object[]{"表结构语句错误: " + e.getMessage(), -1});
                        }
                    }

                    // 2. 然后执行所有其他语句(如INSERT语句) - 可以并行执行
                    if (!stopped.get()) {
                        publish(new Object[]{"正在执行数据插入语句...", completedCount.get() * 100 / totalStatements});

                        // 将SQL语句分组为批次
                        List<List<String>> batches = new ArrayList<>();
                        for (int i = 0; i < otherStatements.size(); i += batchSize) {
                            batches.add(otherStatements.subList(i, Math.min(i + batchSize, otherStatements.size())));
                        }

                        for (List<String> batch : batches) {
                            if (stopped.get()) {
                                break;
                            }

                            Future<?> future = executor.submit(() -> {
                                for (String sql : batch) {
                                    if (stopped.get()) {
                                        break;
                                    }

                                    try {
                                        // 执行非查询语句
                                        Map<String, Object> result = DatabaseService.executeUpdate(config, sql);
                                        boolean success = (boolean) result.get("success");

                                        if (success) {
                                            int completed = completedCount.incrementAndGet();
                                            int progress = (completed * 100) / totalStatements;
                                            publish(new Object[]{"已执行: " + completed + "/" + totalStatements +
                                                    ", 错误: " + errorCount.get(), progress});
                                        } else {
                                            errorCount.incrementAndGet();
                                            String errorMessage = (String) result.get("message");
                                            publish(new Object[]{"错误: " + errorMessage, -1});
                                        }
                                    } catch (Exception e) {
                                        errorCount.incrementAndGet();
                                        publish(new Object[]{"错误: " + e.getMessage(), -1});
                                    }
                                }
                            });
                            futures.add(future);
                        }

                        // 等待所有任务完成
                        for (Future<?> future : futures) {
                            if (!stopped.get()) {
                                future.get();
                            }
                        }
                    }

                    executor.shutdown();

                    // 如果未被停止，则设置为100%完成
                    if (!stopped.get()) {
                        publish(new Object[]{"执行完成, 总共: " + totalStatements + ", 成功: " +
                                (totalStatements - errorCount.get()) + ", 错误: " + errorCount.get(), 100});
                    } else {
                        publish(new Object[]{"执行已停止", -1});
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    publish(new Object[]{"执行失败: " + e.getMessage(), -1});
                }
                return null;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                Object[] latestUpdate = chunks.get(chunks.size() - 1);
                String status = (String) latestUpdate[0];
                int progress = (int) latestUpdate[1];

                statusLabel.setText(status);
                if (progress >= 0) {
                    progressBar.setValue(progress);
                    progressBar.setString(progress + "%");
                }
            }

            @Override
            protected void done() {
                stopButton.setText("关闭");
                stopButton.removeActionListener(stopButton.getActionListeners()[0]);
                stopButton.addActionListener(e -> progressDialog.dispose());
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }


    */
/**
     * 创建数据编辑控制面板
     *//*

    private JPanel createDataEditPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        panel.setBackground(new Color(60, 63, 65));

        JCheckBox editableCheckBox = new JCheckBox("启用编辑");
        editableCheckBox.setForeground(new Color(187, 187, 187));
        editableCheckBox.setBackground(new Color(60, 63, 65));

        addRowButton = new JButton("添加行");
        addRowButton.setBackground(new Color(60, 63, 65));
        addRowButton.setForeground(new Color(187, 187, 187));
        addRowButton.setEnabled(false);

        deleteRowButton = new JButton("删除行");
        deleteRowButton.setBackground(new Color(60, 63, 65));
        deleteRowButton.setForeground(new Color(187, 187, 187));
        deleteRowButton.setEnabled(false);

        submitChangesButton = new JButton("提交更改");
        submitChangesButton.setBackground(new Color(60, 63, 65));
        submitChangesButton.setForeground(new Color(187, 187, 187));
        submitChangesButton.setEnabled(false);

        // 添加监听器
        editableCheckBox.addActionListener(e -> {
            isDataEditable = editableCheckBox.isSelected();
            resultTableModel.setEditable(isDataEditable);
            addRowButton.setEnabled(isDataEditable);
            deleteRowButton.setEnabled(isDataEditable);
            submitChangesButton.setEnabled(isDataEditable);

            if (isDataEditable && currentTableName != null) {
                // 获取表的主键信息，用于编辑时生成更新语句
                fetchTablePrimaryKeys(currentTableName);
            }
        });

        addRowButton.addActionListener(e -> {
            // 添加新行
            resultTableModel.addRow();

            // 记录修改
            int rowCount = resultTableModel.getRowCount();
            if (rowCount > 0) {
                modifiedRows.add(Integer.valueOf(rowCount - 1));
            }
        });

        deleteRowButton.addActionListener(e -> {
            int selectedRow = resultTable.getSelectedRow();
            if (selectedRow != -1) {
                // 记录要删除的行
                modifiedRows.add(Integer.valueOf(selectedRow));

                // 标记为删除
                resultTableModel.markRowAsDeleted(selectedRow);
            }
        });

        submitChangesButton.addActionListener(e -> {
            // 保存修改
            saveTableChanges();
        });

        panel.add(editableCheckBox);
        panel.add(addRowButton);
        panel.add(deleteRowButton);
        panel.add(submitChangesButton);

        return panel;
    }

}*/
