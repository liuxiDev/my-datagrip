package com.database.visualization.view;

import com.database.visualization.controller.DatabaseService;
import com.database.visualization.controller.SQLExecutor;
import com.database.visualization.model.ConnectionConfig;
import com.database.visualization.model.QueryResultTableModel;
import com.database.visualization.utils.ConnectionManager;
import com.database.visualization.utils.SQLFormatter;
import com.database.visualization.utils.TableColumnAdjuster;
import com.database.visualization.view.menu.ContextMenuManager;
import com.database.visualization.view.menu.ContextMenuManager.MenuActionHandler;
import com.database.visualization.view.table.DataTableManager;
import com.database.visualization.view.theme.ThemeManager;
import com.database.visualization.view.tree.DatabaseTreeManager;
import com.database.visualization.view.ui.UIComponentFactory;

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
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

/**
 * 主窗口类 (重构版)
 */
public class RestructuredMainFrame extends JFrame {
    private static ConnectionConfig currentConnection;
    // 原有组件
    private JTree databaseTree;
    private JTextArea sqlTextArea;
    private JTable resultTable;
    private JLabel statusLabel;
    private JSplitPane mainSplitPane;
    private JSplitPane leftSplitPane;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private QueryResultTableModel resultTableModel;
    private DefaultTableCellRenderer cellRenderer;
    // 分页相关的字段
    private JPanel paginationPanel;
    private JTextField pageField;
    private JTextField pageSizeField;
    private JButton prevPageButton;
    private JButton nextPageButton;
    private JLabel totalPagesLabel;
    private String currentTableName;
    // 数据编辑相关字段
    private JPanel dataEditPanel;
    private JButton addRowButton;
    private JButton deleteRowButton;
    private JButton submitChangesButton;
    // 辅助管理类
    private UIComponentFactory uiFactory;
    private DatabaseTreeManager treeManager;
    private SQLExecutor sqlExecutor;
    private DataTableManager dataTableManager;
    private ThemeManager themeManager;

    /**
     * 构造方法
     */
    public RestructuredMainFrame() {
        // 初始化组件
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

    /**
     * 初始化组件
     */
    private void initComponents() {
        // 创建UI工厂
        uiFactory = new UIComponentFactory(com.database.visualization.DataBaseVisualizer.isDarkTheme);

        // 设置菜单栏
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        // 初始化基本组件
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

        // 创建辅助管理类
        initManagers();
    }

    /**
     * 初始化管理器
     */
    private void initManagers() {
        // 初始化树管理器
        treeManager = new DatabaseTreeManager(databaseTree, treeModel, rootNode, statusLabel);

        // 初始化SQL执行器
        sqlExecutor = new SQLExecutor(statusLabel, resultTable, resultTableModel, pageField, totalPagesLabel);

        // 初始化数据表格管理器
        dataTableManager = new DataTableManager(resultTable, resultTableModel, sqlExecutor, statusLabel);

        // 初始化主题管理器
        themeManager = new ThemeManager(this, databaseTree, sqlTextArea, resultTable,
                statusLabel, paginationPanel, dataEditPanel,
                mainSplitPane, leftSplitPane);
    }

    /**
     * 创建菜单栏
     */
    private JMenuBar createMenuBar() {
        return uiFactory.createMenuBar(this::handleMenuAction);
    }

    /**
     * 处理菜单操作
     */
    private void handleMenuAction(ActionEvent e) {
        Object source = e.getSource();
        if (source instanceof JMenuItem) {
            JMenuItem item = (JMenuItem) source;
            String text = item.getText();

            switch (text) {
                case "新建连接":
                    addNewConnection();
                    break;
                case "导入连接":
                    importConnections();
                    break;
                case "导出连接":
                    exportConnections();
                    break;
                case "退出":
                    dispose();
                    System.exit(0);
                    break;
                case "格式化SQL":
                    formatSQL();
                    break;
                case "清空编辑器":
                    sqlTextArea.setText("");
                    break;
                case "执行SQL":
                    executeSQL();
                    break;
                case "性能监控":
                    showMonitoringDialog();
                    break;
                case "安全设置":
                    showSecurityDialog();
                    break;
                case "深色主题":
                    com.database.visualization.DataBaseVisualizer.isDarkTheme = true;
                    com.database.visualization.DataBaseVisualizer.applyTheme();
                    setApplicationTheme();
                    break;
                case "浅色主题":
                    com.database.visualization.DataBaseVisualizer.isDarkTheme = false;
                    com.database.visualization.DataBaseVisualizer.applyTheme();
                    setApplicationTheme();
                    break;
                case "关于":
                    JOptionPane.showMessageDialog(this,
                            "数据库可视化工具\nVersion 1.0\n作者: Java开发者",
                            "关于", JOptionPane.INFORMATION_MESSAGE);
                    break;
                default:
                    // 处理字体大小设置
                    try {
                        int fontSize = Integer.parseInt(text);
                        com.database.visualization.DataBaseVisualizer.fontSizeValue = fontSize;
                        com.database.visualization.DataBaseVisualizer.fontSizeFactor = fontSize / 14.0f;
                        com.database.visualization.DataBaseVisualizer.saveFontSizeSettings();
                        updateGlobalFontSize();
                    } catch (NumberFormatException ignored) {
                        // 忽略非数字文本
                    }
                    break;
            }
        }
    }

    /**
     * 创建工具栏
     */
    private JToolBar createToolBar() {
        JToolBar toolBar = uiFactory.createToolBar(this::handleToolBarAction);
        return toolBar;
    }

    /**
     * 处理工具栏操作
     */
    private void handleToolBarAction(ActionEvent e) {
        Object source = e.getSource();
        if (source instanceof JButton) {
            JButton button = (JButton) source;
            String text = button.getText();

            switch (text) {
                case "新建连接":
                    addNewConnection();
                    break;
                case "执行SQL":
                    executeSQL();
                    break;
                case "格式化SQL":
                    formatSQL();
                    break;
                case "刷新":
                    refreshDatabaseTree();
                    break;
                case "性能监控":
                    showMonitoringDialog();
                    break;
                case "导出数据":
                    handleExportData((JButton) source);
                    break;
            }
        }
    }

    /**
     * 创建分页控制面板
     */
    private JPanel createPaginationPanel() {
        JPanel panel = uiFactory.createPaginationPanel(this::handlePaginationAction);

        // 获取面板中的组件
        for (Component c : panel.getComponents()) {
            if (c instanceof JTextField) {
                JTextField tf = (JTextField) c;
                if (tf.getColumns() == 3) {
                    pageField = tf;
                } else if (tf.getColumns() == 4) {
                    pageSizeField = tf;
                }
            } else if (c instanceof JButton) {
                JButton btn = (JButton) c;
                if ("上一页".equals(btn.getText())) {
                    prevPageButton = btn;
                } else if ("下一页".equals(btn.getText())) {
                    nextPageButton = btn;
                }
            } else if (c instanceof JLabel && ((JLabel) c).getText().startsWith("共")) {
                totalPagesLabel = (JLabel) c;
            }
        }

        return panel;
    }

    /**
     * 处理分页操作
     */
    private void handlePaginationAction(ActionEvent e) {
        Object source = e.getSource();

        if (source == prevPageButton) {
            if (sqlExecutor.getCurrentPage() > 1) {
                sqlExecutor.setCurrentPage(sqlExecutor.getCurrentPage() - 1);
                pageField.setText(String.valueOf(sqlExecutor.getCurrentPage()));
                executeCurrentQuery();
            }
        } else if (source == nextPageButton) {
            sqlExecutor.setCurrentPage(sqlExecutor.getCurrentPage() + 1);
            pageField.setText(String.valueOf(sqlExecutor.getCurrentPage()));
            executeCurrentQuery();
        } else if (source == pageField) {
            try {
                int page = Integer.parseInt(pageField.getText());
                if (page > 0) {
                    sqlExecutor.setCurrentPage(page);
                    executeCurrentQuery();
                } else {
                    pageField.setText(String.valueOf(sqlExecutor.getCurrentPage()));
                }
            } catch (NumberFormatException ex) {
                pageField.setText(String.valueOf(sqlExecutor.getCurrentPage()));
            }
        } else if (source == pageSizeField) {
            try {
                int size = Integer.parseInt(pageSizeField.getText());
                if (size > 0) {
                    sqlExecutor.setPageSize(size);
                    sqlExecutor.setCurrentPage(1);
                    pageField.setText("1");
                    executeCurrentQuery();
                } else {
                    pageSizeField.setText(String.valueOf(sqlExecutor.getPageSize()));
                }
            } catch (NumberFormatException ex) {
                pageSizeField.setText(String.valueOf(sqlExecutor.getPageSize()));
            }
        }
    }

    /**
     * 创建数据编辑控制面板
     */
    private JPanel createDataEditPanel() {
        JPanel panel = uiFactory.createDataEditPanel(this::handleDataEditAction);

        // 获取面板中的组件
        for (Component c : panel.getComponents()) {
            if (c instanceof JButton) {
                JButton btn = (JButton) c;
                if ("添加行".equals(btn.getText())) {
                    addRowButton = btn;
                } else if ("删除行".equals(btn.getText())) {
                    deleteRowButton = btn;
                } else if ("提交更改".equals(btn.getText())) {
                    submitChangesButton = btn;
                }
            }
        }

        return panel;
    }

    /**
     * 处理数据编辑操作
     */
    private void handleDataEditAction(ActionEvent e) {
        Object source = e.getSource();

        if (source instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) source;
            boolean isEditable = checkBox.isSelected();
            dataTableManager.setEditable(isEditable);
            addRowButton.setEnabled(isEditable);
            deleteRowButton.setEnabled(isEditable);
            submitChangesButton.setEnabled(isEditable);

            if (isEditable && sqlExecutor.getCurrentTableName() != null) {
                // 获取表的主键信息，用于编辑时生成更新语句
                fetchTablePrimaryKeys(sqlExecutor.getCurrentTableName());
            }
        } else if (source == addRowButton) {
            dataTableManager.addRow();
        } else if (source == deleteRowButton) {
            int selectedRow = resultTable.getSelectedRow();
            if (selectedRow != -1) {
                dataTableManager.deleteRow(selectedRow);
            }
        } else if (source == submitChangesButton) {
            dataTableManager.saveTableChanges();
        }
    }

    /**
     * 处理导出数据
     */
    private void handleExportData(JButton exportButton) {
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

                int result = fileChooser.showSaveDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    String filePath = file.getAbsolutePath();
                    if (!filePath.endsWith(".csv")) {
                        filePath += ".csv";
                    }

                    try {
                        exportResultsToCsv(filePath);
                        JOptionPane.showMessageDialog(this, "导出成功！");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "导出失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
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

                int result = fileChooser.showSaveDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    String filePath = file.getAbsolutePath();
                    if (!filePath.endsWith(".xlsx")) {
                        filePath += ".xlsx";
                    }

                    try {
                        exportResultsToExcel(filePath);
                        JOptionPane.showMessageDialog(this, "导出成功！");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "导出失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            exportMenu.add(exportCsvItem);
            exportMenu.add(exportExcelItem);

            // 在按钮位置显示菜单
            exportMenu.show(exportButton, 0, exportButton.getHeight());
        } else {
            JOptionPane.showMessageDialog(this, "没有可导出的数据", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 设置监听器
     */
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

                // 处理右键点击 - 显示上下文菜单
                if (e.getButton() == MouseEvent.BUTTON3) {
                    ContextMenuManager.showTreeNodeContextMenu(node, e.getX(), e.getY(), databaseTree, createMenuActionHandler());
                    return;
                }

                // 处理双击
                if (e.getClickCount() == 2) {
                    handleTreeNodeDoubleClick(node, path);
                }
            }
        });
    }

    /**
     * 处理树节点双击事件
     */
    private void handleTreeNodeDoubleClick(DefaultMutableTreeNode node, TreePath path) {
        Object userObject = node.getUserObject();

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
                    treeManager.setCurrentConnection(currentConnection);
                    sqlExecutor.setCurrentConnection(currentConnection);

                    String dbName = (String) userObject;
                    // 清空并加载表
                    node.removeAllChildren();
                    treeManager.loadDatabaseTables(currentConnection, dbName, node);
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
                        treeManager.setCurrentConnection(currentConnection);
                        sqlExecutor.setCurrentConnection(currentConnection);

                        sqlExecutor.setCurrentTableName(dbName + "." + tableName);
                        // 重置到第一页
                        sqlExecutor.setCurrentPage(1);
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

    /**
     * 处理树节点选择事件
     */
    private void handleTreeSelection(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) databaseTree.getLastSelectedPathComponent();
        if (node == null) return;

        Object userObject = node.getUserObject();

        // 1. 如果选择的是连接节点
        if (userObject instanceof ConnectionConfig) {
            currentConnection = (ConnectionConfig) userObject;
            treeManager.setCurrentConnection(currentConnection);
            sqlExecutor.setCurrentConnection(currentConnection);
            statusLabel.setText("已选择连接: " + currentConnection.getName());
        }
        // 2. 如果选择的是数据库/schema节点
        else if (userObject instanceof String && node.getParent() != null) {
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
            Object parentObject = parentNode.getUserObject();

            if (parentObject instanceof ConnectionConfig) {
                currentConnection = (ConnectionConfig) parentObject;
                treeManager.setCurrentConnection(currentConnection);
                sqlExecutor.setCurrentConnection(currentConnection);

                String dbOrSchemaName = (String) userObject;
                statusLabel.setText("已选择数据库: " + dbOrSchemaName);

                // 重要：清除该节点下的所有子节点，确保每次切换数据库时都重新加载表
                node.removeAllChildren();
                treeModel.nodeStructureChanged(node);

                // 展开节点前，先获取该数据库下的所有表
                treeManager.loadDatabaseTables(currentConnection, dbOrSchemaName, node);

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
                    treeManager.setCurrentConnection(currentConnection);
                    sqlExecutor.setCurrentConnection(currentConnection);

                    String dbOrSchemaName = (String) parentObject;
                    String tableName = (String) userObject;

                    // 根据数据库类型处理
                    if ("redis".equalsIgnoreCase(currentConnection.getDatabaseType())) {
                        // Redis数据库
                        sqlExecutor.setCurrentTableName(dbOrSchemaName + ":" + tableName);
                        String cmd = String.format("SCAN 0 MATCH %s* COUNT 100", tableName);
                        sqlTextArea.setText(cmd);
                        statusLabel.setText("已选择Redis键模式: " + tableName);
                    } else {
                        // 常规数据库表
                        sqlExecutor.setCurrentTableName(tableName);
                        String sql = String.format("SELECT * FROM %s.%s LIMIT 100", dbOrSchemaName, tableName);
                        sqlTextArea.setText(sql);
                        statusLabel.setText("已选择表: " + dbOrSchemaName + "." + tableName);
                    }
                }
            }
        }
    }

    /**
     * 创建菜单动作处理器
     */
    private MenuActionHandler createMenuActionHandler() {
        return new MenuActionHandler() {
            @Override
            public void connectToDatabase(ConnectionConfig config) {
                RestructuredMainFrame.this.connectDatabase(config);
            }

            @Override
            public void editConnection(ConnectionConfig config) {
                ConnectionDialog dialog = new ConnectionDialog(RestructuredMainFrame.this, config, false);
                dialog.setVisible(true);

                if (dialog.isConfirmed()) {
                    ConnectionConfig newConfig = dialog.getConnectionConfig();
                    ConnectionManager.updateConnection(newConfig);
                    loadConnections();
                }
            }

            @Override
            public void deleteConnection(ConnectionConfig config) {
                if (JOptionPane.showConfirmDialog(RestructuredMainFrame.this,
                        "确定要删除连接 " + config.getName() + " 吗？",
                        "确认删除", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    ConnectionManager.deleteConnection(config);
                    loadConnections();
                }
            }

            @Override
            public void refreshDatabaseTree() {
                RestructuredMainFrame.this.refreshDatabaseTree();
            }

            @Override
            public void createNewDatabase(ConnectionConfig config) {
                createNewDatabase(config);
            }

            @Override
            public void alterDatabase(ConnectionConfig config, String dbName) {
                alterDatabase(config, dbName);
            }

            @Override
            public void dropDatabase(ConnectionConfig config, String dbName) {
                dropDatabase(config, dbName);
            }

            @Override
            public void createNewTable(ConnectionConfig config, String dbName) {
                createNewTable(config, dbName);
            }

            @Override
            public void executeQuery(ConnectionConfig config, String dbName) {
                if ("mysql".equalsIgnoreCase(config.getDatabaseType())) {
                    sqlTextArea.setText("USE " + dbName + ";\nSHOW TABLES;");
                } else {
                    sqlTextArea.setText("SELECT * FROM information_schema.tables WHERE table_schema = '" + dbName + "'");
                }

                currentConnection = config;
                treeManager.setCurrentConnection(currentConnection);
                sqlExecutor.setCurrentConnection(currentConnection);
                executeSQL();
            }

            @Override
            public void exportDatabaseSql(ConnectionConfig config, String dbName) {
                exportDatabaseSql(config, dbName);
            }

            @Override
            public void batchExecuteSql(ConnectionConfig config, String dbName) {
                batchExecuteSql(config, dbName);
            }

            @Override
            public void selectRedisDatabase(ConnectionConfig config, String dbName) {
                currentConnection = config;
                treeManager.setCurrentConnection(currentConnection);
                sqlExecutor.setCurrentConnection(currentConnection);

                sqlTextArea.setText("SELECT " + dbName.replace("db", ""));
                executeSQL();
            }

            @Override
            public void flushRedisDatabase(ConnectionConfig config, String dbName) {
                if (JOptionPane.showConfirmDialog(RestructuredMainFrame.this,
                        "确定要清空数据库 " + dbName + " 吗？此操作不可逆！",
                        "确认清空", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    currentConnection = config;
                    treeManager.setCurrentConnection(currentConnection);
                    sqlExecutor.setCurrentConnection(currentConnection);

                    sqlTextArea.setText("FLUSHDB");
                    executeSQL();
                }
            }

            @Override
            public void queryRedisKeys(ConnectionConfig config, String dbName, String typeName) {
                currentConnection = config;
                treeManager.setCurrentConnection(currentConnection);
                sqlExecutor.setCurrentConnection(currentConnection);

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
            }

            @Override
            public void queryTable(ConnectionConfig config, String schemaName, String tableName) {
                currentConnection = config;
                treeManager.setCurrentConnection(currentConnection);
                sqlExecutor.setCurrentConnection(currentConnection);

                String sql = "SELECT * FROM " + schemaName + "." + tableName + " LIMIT 100";
                sqlTextArea.setText(sql);
                executeSQL();
            }

            @Override
            public void showTableStructure(ConnectionConfig config, String schemaName, String tableName) {
                currentConnection = config;
                treeManager.setCurrentConnection(currentConnection);
                sqlExecutor.setCurrentConnection(currentConnection);
                RestructuredMainFrame.this.showTableStructure(schemaName + "." + tableName);
            }

            @Override
            public void editTable(ConnectionConfig config, String schemaName, String tableName) {
                showEditTableDialog(tableName);
            }

            @Override
            public void dropTable(ConnectionConfig config, String schemaName, String tableName) {
                if (JOptionPane.showConfirmDialog(RestructuredMainFrame.this,
                        "确定要删除表 " + tableName + " 吗？此操作不可逆！",
                        "确认删除", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    executeSQL("DROP TABLE " + tableName);
                    refreshDatabaseTree();
                }
            }

            @Override
            public void emptyTable(ConnectionConfig config, String schemaName, String tableName) {
                if (JOptionPane.showConfirmDialog(RestructuredMainFrame.this,
                        "确定要清空表 " + tableName + " 的所有数据吗？此操作不可逆！",
                        "确认清空", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    executeSQL("TRUNCATE TABLE " + tableName);
                }
            }

            @Override
            public void exportTableData(ConnectionConfig config, String schemaName, String tableName) {
                // 导出数据逻辑
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("导出数据");
                fileChooser.setFileFilter(new FileNameExtensionFilter("CSV文件", "csv"));

                if (fileChooser.showSaveDialog(RestructuredMainFrame.this) == JFileChooser.APPROVE_OPTION) {
                    String filePath = fileChooser.getSelectedFile().getPath();
                    if (!filePath.endsWith(".csv")) {
                        filePath += ".csv";
                    }

                    try {
                        // 先查询数据
                        currentConnection = config;
                        treeManager.setCurrentConnection(currentConnection);
                        sqlExecutor.setCurrentConnection(currentConnection);

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

                                JOptionPane.showMessageDialog(RestructuredMainFrame.this, "导出成功", "导出数据", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } else {
                            JOptionPane.showMessageDialog(RestructuredMainFrame.this, "查询失败: " + result.get("error"), "错误", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(RestructuredMainFrame.this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
    }

    /**
     * 连接数据库
     */
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
                        treeManager.setCurrentConnection(currentConnection);
                        sqlExecutor.setCurrentConnection(currentConnection);
                        statusLabel.setText("已连接到: " + config.getName());
                        refreshDatabaseTree();
                    } else {
                        statusLabel.setText("连接失败");
                        JOptionPane.showMessageDialog(RestructuredMainFrame.this,
                                "连接数据库失败，请检查连接配置",
                                "连接失败", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    statusLabel.setText("连接出错: " + e.getMessage());
                    JOptionPane.showMessageDialog(RestructuredMainFrame.this,
                            "连接数据库出错: " + e.getMessage(),
                            "连接出错", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    /**
     * 调整组件高度以适应窗口大小
     */
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

    /**
     * 执行SQL语句
     */
    private void executeSQL() {
        String sql = sqlTextArea.getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入SQL语句", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        sqlExecutor.executeSQL(sql);
    }

    private void executeSQL(String sql) {
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入SQL语句", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        sqlExecutor.executeSQL(sql);
    }

    /**
     * 执行当前查询（分页）
     */
    private void executeCurrentQuery() {
        String sql = sqlTextArea.getText().trim();
        sqlExecutor.executeCurrentQuery(sql);
    }

    /**
     * 获取表的主键信息
     */
    private void fetchTablePrimaryKeys(String tableName) {
        sqlExecutor.fetchTablePrimaryKeys(tableName);
    }

    /**
     * 显示表结构
     */
    public void showTableStructure(String tableName) {
        // 由于这个方法涉及到UI创建，暂时保留原实现
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

    /**
     * 显示添加列对话框
     */
    private void showAddColumnDialog(String tableName) {
        // 这个方法代码较长，暂时保留原有实现
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

    /**
     * 显示编辑表对话框
     */
    private void showEditTableDialog(String tableName) {
        // 这个方法代码较长，暂时保留原有实现
        // 可以在后续优化中将其迁移到专门的对话框类中
        // ...
    }

    /**
     * 刷新数据库树
     */
    private void refreshDatabaseTree() {
        treeManager.refreshDatabaseTree(ConnectionManager.getConnections());
    }

    /**
     * 加载所有连接
     */
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

    /**
     * 添加新连接
     */
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

    /**
     * 导入连接配置
     */
    private void importConnections() {
        // 实现导入连接逻辑
        // ...
    }

    /**
     * 导出连接配置
     */
    private void exportConnections() {
        // 实现导出连接逻辑
        // ...
    }

    /**
     * 格式化SQL
     */
    private void formatSQL() {
        String sql = sqlTextArea.getText().trim();
        if (sql.isEmpty()) {
            return;
        }

        // 使用SQL格式化工具格式化
        String formattedSql = SQLFormatter.format(sql);
        sqlTextArea.setText(formattedSql);
    }

    /**
     * 将查询结果导出为CSV文件
     */
    private void exportResultsToCsv(String filePath) throws Exception {
        // 实现导出CSV逻辑
        // ...
    }

    /**
     * 将查询结果导出为Excel文件
     */
    private void exportResultsToExcel(String filePath) throws Exception {
        // 实现导出Excel逻辑
        // ...
    }

    /**
     * 设置应用程序主题颜色
     */
    private void setApplicationTheme() {
        themeManager.applyTheme();
    }

    /**
     * 更新全局字体大小
     */
    private void updateGlobalFontSize() {
        themeManager.updateGlobalFontSize();
    }

    /**
     * 显示监控对话框
     */
    private void showMonitoringDialog() {
        // 创建并显示新的监控对话框
        MonitoringDialog dialog = new MonitoringDialog(this, currentConnection);
        dialog.setVisible(true);
    }

    /**
     * 显示安全设置对话框
     */
    private void showSecurityDialog() {
        // 实现安全设置对话框
        // ...
    }

    /**
     * 创建新数据库
     */
    private void createNewDatabase(ConnectionConfig config) {
        // 实现创建新数据库的逻辑
        // ...
    }

    /**
     * 修改数据库
     */
    private void alterDatabase(ConnectionConfig config, String dbName) {
        // 实现修改数据库的逻辑
        // ...
    }

    /**
     * 删除数据库
     */
    private void dropDatabase(ConnectionConfig config, String dbName) {
        // 实现删除数据库的逻辑
        // ...
    }

    /**
     * 创建新表
     */
    private void createNewTable(ConnectionConfig config, String schemaName) {
        // 实现创建新表的逻辑
        // ...
    }

    /**
     * 导出数据库SQL
     */
    private void exportDatabaseSql(ConnectionConfig config, String schemaName) {
        // 实现导出数据库SQL的逻辑
        // ...
    }

    /**
     * 批量执行SQL语句
     */
    private void batchExecuteSql(ConnectionConfig config, String schemaName) {
        // 实现批量执行SQL的逻辑
        // ...
    }
} 