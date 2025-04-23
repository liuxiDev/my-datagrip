package com.database.visualization.view;

import com.database.visualization.controller.DatabaseService;
import com.database.visualization.model.ConnectionConfig;
import com.database.visualization.model.QueryResultTableModel;
import com.database.visualization.utils.ConnectionManager;
import com.database.visualization.utils.TableColumnAdjuster;
import com.database.visualization.utils.SQLFormatter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.io.FileWriter;
import java.util.Date;
import java.io.FileOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 主窗口
 */
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
        
        // 加载连接
        loadConnections();
        
        // 设置UI主题和颜色
        setApplicationTheme();
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
        
        // 左侧面板撑满窗体高度
        leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeScrollPane, new JPanel());
        leftSplitPane.setDividerLocation(800); // 设置较大值使左侧面板撑满
        
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
        JMenuItem smallFontItem = new JMenuItem("小");
        JMenuItem mediumFontItem = new JMenuItem("中");
        JMenuItem largeFontItem = new JMenuItem("大");
        
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
        
        smallFontItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                com.database.visualization.DataBaseVisualizer.fontSizeFactor = 0.85f;
                updateGlobalFontSize();
            }
        });
        
        mediumFontItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                com.database.visualization.DataBaseVisualizer.fontSizeFactor = 1.0f;
                updateGlobalFontSize();
            }
        });
        
        largeFontItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                com.database.visualization.DataBaseVisualizer.fontSizeFactor = 1.2f;
                updateGlobalFontSize();
            }
        });
        
        themeMenu.add(darkThemeItem);
        themeMenu.add(lightThemeItem);
        fontSizeMenu.add(smallFontItem);
        fontSizeMenu.add(mediumFontItem);
        fontSizeMenu.add(largeFontItem);
        settingsMenu.add(themeMenu);
        settingsMenu.add(fontSizeMenu);
        
        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");
        
        aboutItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "数据库可视化工具 v1.0\n作者：数据库开发团队",
                        "关于",
                        JOptionPane.INFORMATION_MESSAGE);
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
                
                // 处理右键点击
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
                                databaseTree.expandPath(path);
                            }
                            
                            // 对于MySQL数据库，执行USE语句
                            currentConnection = (ConnectionConfig) parentObject;
                            String dbName = (String) userObject;
                            if ("mysql".equalsIgnoreCase(currentConnection.getDatabaseType())) {
                                String sql = String.format("USE %s", dbName);
                                sqlTextArea.setText(sql);
                                executeSQL();
                            }
                        } else if (parentObject instanceof String && parentNode.getParent() != null) {
                            // 双击表节点，执行默认查询
                            if (node.isLeaf()) {
                                String tableName = (String)userObject;
                                String dbName = (String)parentObject;
                                DefaultMutableTreeNode grandParentNode = (DefaultMutableTreeNode)parentNode.getParent();
                                
                                if (grandParentNode != null && grandParentNode.getUserObject() instanceof ConnectionConfig) {
                                    currentConnection = (ConnectionConfig)grandParentNode.getUserObject();
                                    currentTableName = dbName + "." + tableName;
                                    currentPage = 1;
                                    pageField.setText("1");
                                    String sql = String.format("SELECT * FROM %s.%s", dbName, tableName);
                                    sqlTextArea.setText(sql);
                                    executeSQL();
                                }
                            }
                        }
                    }
                    // 如果双击表节点
                    else if (userObject instanceof String && node.isLeaf()) {
                        // 这部分已在上面处理
                    }
                }
            }
        });
    }
    
    /**
     * 处理树节点选择
     */
    private void handleTreeSelection(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) databaseTree.getLastSelectedPathComponent();
        
        if (node == null) return;
        
        Object userObject = node.getUserObject();
        if (userObject instanceof ConnectionConfig) {
            // 选择了连接节点
            currentConnection = (ConnectionConfig) userObject;
            statusLabel.setText("已选择连接: " + currentConnection.getName());
        } else if (userObject instanceof String && node.getParent() != null) {
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
            Object parentObject = parentNode.getUserObject();
            
            if (parentObject instanceof ConnectionConfig) {
                // 选择了数据库/schema节点
                currentConnection = (ConnectionConfig) parentObject;
                String dbName = (String) userObject;
                
                statusLabel.setText("已选择数据库: " + dbName);
                
                // 如果是MySQL数据库，可以生成相应的USE语句
                if ("mysql".equalsIgnoreCase(currentConnection.getDatabaseType())) {
                    String sql = String.format("USE %s", dbName);
                    sqlTextArea.setText(sql);
                }
            } else if (parentObject instanceof String && parentNode.getParent() != null) {
                // 选择了schema下的表或数据库下的表
                DefaultMutableTreeNode grandParentNode = (DefaultMutableTreeNode) parentNode.getParent();
                if (grandParentNode.getUserObject() instanceof ConnectionConfig) {
                    currentConnection = (ConnectionConfig) grandParentNode.getUserObject();
                    String dbOrSchemaName = (String) parentObject;
                    String tableName = (String) userObject;
                    
                    // 处理Redis数据库选择
                    if ("redis".equalsIgnoreCase(currentConnection.getDatabaseType()) && 
                            dbOrSchemaName.startsWith("db")) {
                        // Redis处理逻辑
                        // 提取数据库索引
                        int dbIndex = 0;
                        try {
                            String dbIndexStr = dbOrSchemaName.substring(2, dbOrSchemaName.indexOf(" "));
                            dbIndex = Integer.parseInt(dbIndexStr);
                        } catch (Exception ex) {
                            // 默认使用数据库0
                        }
                        
                        // 选择Redis数据库
                        boolean success = DatabaseService.selectRedisDatabase(currentConnection, dbIndex);
                        
                        if (success) {
                            statusLabel.setText("已切换到Redis数据库: " + dbIndex);
                            
                            // 根据选择的键类型生成相应的命令
                            String redisCmd = "";
                            switch (tableName.toLowerCase()) {
                                case "string":
                                    redisCmd = "KEYS *";
                                    break;
                                case "hash":
                                    redisCmd = "KEYS *";
                                    break;
                                case "list":
                                    redisCmd = "KEYS *";
                                    break;
                                case "set":
                                    redisCmd = "KEYS *";
                                    break;
                                case "zset":
                                    redisCmd = "KEYS *";
                                    break;
                                default:
                                    redisCmd = "KEYS *";
                            }
                            
                            sqlTextArea.setText(redisCmd);
                        } else {
                            statusLabel.setText("切换Redis数据库失败");
                        }
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
                // 更新树
                DefaultMutableTreeNode connNode = new DefaultMutableTreeNode(config);
                rootNode.add(connNode);
                treeModel.reload();
            } else {
                // 提示连接重复
                JOptionPane.showMessageDialog(this,
                        "连接已存在，不能添加重复的连接配置",
                        "添加失败", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
    
    /**
     * 刷新数据库树
     */
    private void refreshDatabaseTree() {
        if (currentConnection == null) {
            return;
        }
        
        // 使用SwingWorker异步加载
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    // 查找当前连接的节点
                    DefaultMutableTreeNode connNode = null;
                    for (int i = 0; i < rootNode.getChildCount(); i++) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) rootNode.getChildAt(i);
                        if (node.getUserObject().equals(currentConnection)) {
                            connNode = node;
                            break;
                        }
                    }
                    
                    if (connNode != null) {
                        // 清空节点
                        connNode.removeAllChildren();
                        
                        // 对Redis连接特殊处理
                        if ("redis".equalsIgnoreCase(currentConnection.getDatabaseType())) {
                            // 获取Redis数据库列表
                            List<String> databases = DatabaseService.getRedisDatabases(currentConnection);
                            
                            // 为每个数据库创建节点
                            for (String db : databases) {
                                String dbName = db;
                                DefaultMutableTreeNode dbNode = new DefaultMutableTreeNode(dbName);
                                connNode.add(dbNode);
                                
                                // 添加Redis键类型作为"表"
                                dbNode.add(new DefaultMutableTreeNode("string"));
                                dbNode.add(new DefaultMutableTreeNode("hash"));
                                dbNode.add(new DefaultMutableTreeNode("list"));
                                dbNode.add(new DefaultMutableTreeNode("set"));
                                dbNode.add(new DefaultMutableTreeNode("zset"));
                            }
                        } else {
                            // 获取数据库/schema列表
                            List<String> schemas = DatabaseService.getSchemas(currentConnection);
                            if (schemas.isEmpty()) {
                                // 如果没有获取到schema，尝试获取数据库列表
                                try {
                                    // 对于MySQL，执行"SHOW DATABASES"命令获取数据库列表
                                    if ("mysql".equalsIgnoreCase(currentConnection.getDatabaseType())) {
                                        Map<String, Object> result = DatabaseService.executeQuery(
                                            currentConnection, "SHOW DATABASES");
                                        
                                        if ((boolean)result.get("success")) {
                                            @SuppressWarnings("unchecked")
                                            List<List<Object>> data = (List<List<Object>>) result.get("data");
                                            
                                            // 创建数据库节点
                                            for (List<Object> row : data) {
                                                if (row.size() > 0 && row.get(0) != null) {
                                                    String dbName = row.get(0).toString();
                                                    DefaultMutableTreeNode dbNode = new DefaultMutableTreeNode(dbName);
                                                    connNode.add(dbNode);
                                                    
                                                    // 获取该数据库下的表
                                                    List<String> tables = DatabaseService.getTables(currentConnection, dbName);
                                                    for (String table : tables) {
                                                        dbNode.add(new DefaultMutableTreeNode(table));
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // 对于其他数据库类型，如果没有schema信息，直接获取表
                                        List<String> tables = DatabaseService.getTables(currentConnection, null);
                                        
                                        // 创建一个默认数据库节点
                                        DefaultMutableTreeNode defaultDbNode = new DefaultMutableTreeNode("Default");
                                        connNode.add(defaultDbNode);
                                        
                                        for (String table : tables) {
                                            defaultDbNode.add(new DefaultMutableTreeNode(table));
                                        }
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            } else {
                                // 如果有schema，按schema组织表
                                for (String schema : schemas) {
                                    DefaultMutableTreeNode schemaNode = new DefaultMutableTreeNode(schema);
                                    connNode.add(schemaNode);
                                    
                                    List<String> tables = DatabaseService.getTables(currentConnection, schema);
                                    for (String table : tables) {
                                        schemaNode.add(new DefaultMutableTreeNode(table));
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
            
            @Override
            protected void done() {
                treeModel.reload();
                statusLabel.setText("刷新完成");
            }
        };
        
        statusLabel.setText("正在刷新...");
        worker.execute();
    }
    
    /**
     * 连接数据库并加载表
     */
    private void connectDatabase(ConnectionConfig config) {
        statusLabel.setText("正在连接数据库...");
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return DatabaseService.testConnection(config);
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        currentConnection = config;
                        refreshDatabaseTree();
                        statusLabel.setText("已连接到: " + config.getName());
                    } else {
                        JOptionPane.showMessageDialog(MainFrame.this,
                            "连接失败，请检查连接信息",
                            "错误", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("连接失败");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    statusLabel.setText("连接出错");
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * 显示表结构对话框
     */
    private void showTableStructure(String tableName) {
        if (currentConnection == null) return;
        
        JDialog dialog = new JDialog(this, "表结构: " + tableName, true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        // 获取表的列信息
        List<Map<String, String>> columns = DatabaseService.getColumns(
                currentConnection, null, tableName);
        
        // 创建表格模型
        String[] columnNames = {"列名", "类型", "大小", "允许NULL"};
        Object[][] data = new Object[columns.size()][4];
        for (int i = 0; i < columns.size(); i++) {
            Map<String, String> column = columns.get(i);
            data[i][0] = column.get("name");
            data[i][1] = column.get("type");
            data[i][2] = column.get("size");
            data[i][3] = column.get("nullable");
        }
        
        JTable table = new JTable(data, columnNames);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 添加操作按钮
        JPanel buttonPanel = new JPanel();
        JButton addColumnBtn = new JButton("添加列");
        JButton editTableBtn = new JButton("修改表");
        JButton dropTableBtn = new JButton("删除表");
        
        addColumnBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddColumnDialog(tableName);
            }
        });
        
        editTableBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showEditTableDialog(tableName);
            }
        });
        
        dropTableBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(dialog,
                        "确定要删除表 " + tableName + " 吗？此操作不可逆！",
                        "确认删除", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    executeSQL("DROP TABLE " + tableName);
                    dialog.dispose();
                    refreshDatabaseTree();
                }
            }
        });
        
        buttonPanel.add(addColumnBtn);
        buttonPanel.add(editTableBtn);
        buttonPanel.add(dropTableBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    /**
     * 显示添加列对话框
     */
    private void showAddColumnDialog(String tableName) {
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
        String[] dataTypes = {"VARCHAR", "INTEGER", "DECIMAL", "BOOLEAN", "DATE", "TIMESTAMP", "TEXT", "BLOB"};
        JComboBox<String> dataTypeCombo = new JComboBox<>(dataTypes);
        panel.add(dataTypeCombo, gbc);
        
        // 长度/精度
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("长度/精度:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        JTextField lengthField = new JTextField("255", 20);
        panel.add(lengthField, gbc);
        
        // 允许NULL
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("允许NULL:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 3;
        JCheckBox nullableCheckBox = new JCheckBox();
        nullableCheckBox.setSelected(true);
        panel.add(nullableCheckBox, gbc);
        
        // 按钮
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("确定");
        JButton cancelButton = new JButton("取消");
        
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String columnName = columnNameField.getText();
                String dataType = (String) dataTypeCombo.getSelectedItem();
                String length = lengthField.getText();
                boolean nullable = nullableCheckBox.isSelected();
                
                if (columnName.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "请输入列名", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + dataType;
                if (dataType.equals("VARCHAR") || dataType.equals("DECIMAL")) {
                    sql += "(" + length + ")";
                }
                
                if (!nullable) {
                    sql += " NOT NULL";
                }
                
                executeSQL(sql);
                dialog.dispose();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        
        buttonPanel.add(okButton);
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
        JDialog dialog = new JDialog(this, "修改表: " + tableName, true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 新表名
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("新表名:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        JTextField newTableNameField = new JTextField(tableName, 20);
        panel.add(newTableNameField, gbc);
        
        // 按钮
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("确定");
        JButton cancelButton = new JButton("取消");
        
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newTableName = newTableNameField.getText();
                
                if (newTableName.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "请输入新表名", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (!newTableName.equals(tableName)) {
                    String sql = "ALTER TABLE " + tableName + " RENAME TO " + newTableName;
                    executeSQL(sql);
                }
                
                dialog.dispose();
                refreshDatabaseTree();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        dialog.setLayout(new BorderLayout());
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    /**
     * 执行SQL
     */
    private void executeSQL() {
        if (currentConnection == null) {
            JOptionPane.showMessageDialog(this,
                    "请先选择一个数据库连接",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        final String sql = sqlTextArea.getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "SQL语句不能为空",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        executeSQLInternal(sql);
    }
    
    /**
     * 执行SQL语句
     */
    private void executeSQL(String sql) {
        if (currentConnection == null) {
            JOptionPane.showMessageDialog(this,
                    "请先选择一个数据库连接",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (sql == null || sql.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "SQL语句不能为空",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        executeSQLInternal(sql);
    }
    
    /**
     * 内部执行SQL语句的方法
     */
    private void executeSQLInternal(String sql) {
        statusLabel.setText("正在执行SQL查询...");
        
        // 检查SQL是否包含LIMIT子句，如果没有且不是更新语句，则添加分页
        final String finalSql;
        final String lowerSql = sql.toLowerCase();
        final boolean isQueryType = lowerSql.startsWith("select") || 
                                   lowerSql.startsWith("show") || 
                                   lowerSql.startsWith("desc");
        
        if (isQueryType && !lowerSql.contains("limit")) {
            // 添加分页
            finalSql = sql + String.format(" LIMIT %d OFFSET %d", 
                    pageSize, (currentPage - 1) * pageSize);
        } else {
            finalSql = sql;
        }
        
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() {
                if (isQueryType) {
                    // 如果是查询类型的SQL，还需要获取总记录数以支持分页
                    if (lowerSql.startsWith("select") && !lowerSql.contains("count(") && currentTableName != null) {
                        // 先执行一个COUNT查询获取总记录数
                        String countSql = "SELECT COUNT(*) AS total FROM " + currentTableName;
                        Map<String, Object> countResult = DatabaseService.executeQuery(currentConnection, countSql);
                        
                        if ((boolean)countResult.get("success")) {
                            @SuppressWarnings("unchecked")
                            List<List<Object>> countData = (List<List<Object>>) countResult.get("data");
                            if (!countData.isEmpty() && !countData.get(0).isEmpty()) {
                                totalRecords = Integer.parseInt(countData.get(0).get(0).toString());
                                totalPages = (int) Math.ceil((double) totalRecords / pageSize);
                            }
                        }
                    }
                    return DatabaseService.executeQuery(currentConnection, finalSql);
                } else {
                    return DatabaseService.executeUpdate(currentConnection, finalSql);
                }
            }
            
            @Override
            protected void done() {
                try {
                    Map<String, Object> result = get();
                    boolean success = (boolean) result.get("success");
                    
                    if (success) {
                        if (result.containsKey("columns") && result.containsKey("data")) {
                            // 查询结果
                            @SuppressWarnings("unchecked")
                            List<String> columns = (List<String>) result.get("columns");
                            @SuppressWarnings("unchecked")
                            List<List<Object>> data = (List<List<Object>>) result.get("data");
                            
                            resultTableModel.setData(columns, data);
                            
                            // 更新分页信息
                            updatePaginationInfo();
                            
                            // 自动调整列宽
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    // 重新应用渲染器到所有列
                                    for (int i = 0; i < resultTable.getColumnCount(); i++) {
                                        if (cellRenderer != null) {
                                            resultTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
                                        }
                                    }
                                    
                                    TableColumnAdjuster adjuster = new TableColumnAdjuster(resultTable);
                                    adjuster.adjustColumns();
                                }
                            });
                            
                            statusLabel.setText("查询成功，当前第 " + currentPage + " 页，共 " + 
                                             totalPages + " 页，显示 " + data.size() + " 条记录");
                        } else {
                            // 更新结果
                            int affectedRows = (int) result.get("affectedRows");
                            resultTableModel.clear();
                            statusLabel.setText("执行成功，影响了 " + affectedRows + " 行");
                            
                            // 如果是修改表结构的操作，刷新树
                            if (lowerSql.startsWith("alter") || 
                                lowerSql.startsWith("create") || 
                                lowerSql.startsWith("drop")) {
                                refreshDatabaseTree();
                            }
                        }
                    } else {
                        String error = (String) result.get("error");
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "执行SQL出错: " + error,
                                "错误", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("执行失败");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "执行SQL出错: " + e.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("执行失败");
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * 更新分页信息
     */
    private void updatePaginationInfo() {
        // 更新UI
        totalPagesLabel.setText("共 " + totalPages + " 页");
        prevPageButton.setEnabled(currentPage > 1);
        nextPageButton.setEnabled(currentPage < totalPages);
        
        // 如果当前页码大于总页数，调整为最后一页
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
            pageField.setText(String.valueOf(currentPage));
        }
    }
    
    /**
     * 执行当前查询（带分页）
     */
    private void executeCurrentQuery() {
        if (currentTableName != null) {
            String sql = String.format("SELECT * FROM %s", currentTableName);
            sqlTextArea.setText(sql);
            executeSQL();
        }
    }
    
    // 添加导入连接配置功能
    private void importConnections() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导入连接配置");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON文件", "json"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                // 这里调用ConnectionManager的方法导入配置
                String filePath = fileChooser.getSelectedFile().getPath();
                importConnectionsFromFile(filePath);
                loadConnections(); // 重新加载连接列表
                JOptionPane.showMessageDialog(this, "成功导入连接配置");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "导入失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // 导入连接配置的逻辑
    private void importConnectionsFromFile(String filePath) {
        try {
            // 使用ConnectionManager导入连接配置
            File file = new File(filePath);
            if (file.exists()) {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<ConnectionConfig> importedConnections = objectMapper.readValue(
                        file, new com.fasterxml.jackson.core.type.TypeReference<List<ConnectionConfig>>() {});
                
                // 添加导入的连接配置
                for (ConnectionConfig config : importedConnections) {
                    ConnectionManager.addConnection(config);
                }
            } else {
                throw new java.io.FileNotFoundException("文件不存在: " + filePath);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "导入失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // 添加导出连接配置功能
    private void exportConnections() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出连接配置");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON文件", "json"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                // 这里调用ConnectionManager的方法导出配置
                String filePath = fileChooser.getSelectedFile().getPath();
                if (!filePath.endsWith(".json")) {
                    filePath += ".json";
                }
                exportConnectionsToFile(filePath);
                JOptionPane.showMessageDialog(this, "成功导出连接配置");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // 导出连接配置的逻辑
    private void exportConnectionsToFile(String filePath) {
        try {
            // 使用ConnectionManager导出连接配置
            List<ConnectionConfig> connections = ConnectionManager.getConnections();
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), connections);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // 添加格式化SQL功能
    private void formatSQL() {
        if (sqlTextArea.getText().trim().isEmpty()) {
            return;
        }
        
        try {
            // 使用SQLFormatter格式化SQL
            String formattedSQL = SQLFormatter.format(sqlTextArea.getText());
            sqlTextArea.setText(formattedSQL);
            JOptionPane.showMessageDialog(this, "SQL格式化成功");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "格式化失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // 添加性能监控对话框
    private void showMonitoringDialog() {
        if (currentConnection == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个数据库连接", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JDialog dialog = new JDialog(this, "性能监控: " + currentConnection.getName(), false);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 创建仪表盘面板
        JPanel gaugePanel = new JPanel(new GridLayout(1, 2, 10, 10));
        CircularProgressBar connectionGauge = new CircularProgressBar("连接池使用率", 0, 100);
        CircularProgressBar responseGauge = new CircularProgressBar("查询响应时间", 0, 100);
        
        connectionGauge.setValue(65); // 示例值
        responseGauge.setValue(80);   // 示例值
        
        gaugePanel.add(connectionGauge);
        gaugePanel.add(responseGauge);
        
        // 创建统计信息面板
        JPanel statsPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        statsPanel.setBorder(BorderFactory.createTitledBorder("数据库统计信息"));
        
        // 添加一些示例统计信息
        addStatItem(statsPanel, "活动连接数", "3");
        addStatItem(statsPanel, "空闲连接数", "7");
        addStatItem(statsPanel, "总连接数", "10");
        addStatItem(statsPanel, "等待线程数", "0");
        addStatItem(statsPanel, "数据库类型", currentConnection.getDatabaseType());
        addStatItem(statsPanel, "服务器版本", "示例版本信息");
        
        // 布局
        mainPanel.add(gaugePanel, BorderLayout.NORTH);
        mainPanel.add(statsPanel, BorderLayout.CENTER);
        
        // 添加刷新按钮
        JButton refreshButton = new JButton("刷新统计信息");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 这里添加刷新逻辑
                connectionGauge.setValue((int)(Math.random() * 100));
                responseGauge.setValue((int)(Math.random() * 100));
                JOptionPane.showMessageDialog(dialog, "统计信息已刷新");
            }
        });
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    // 添加统计信息项
    private void addStatItem(JPanel panel, String name, String value) {
        panel.add(new JLabel(name + ":"));
        panel.add(new JLabel(value));
    }
    
    // 添加安全设置对话框
    private void showSecurityDialog() {
        JDialog dialog = new JDialog(this, "安全设置", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 密码加密选项
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JCheckBox encryptPasswordCheckbox = new JCheckBox("启用密码加密存储", true);
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
        panel.add(algorithmCombo, gbc);
        
        // 密钥强度
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("密钥强度:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        String[] strengths = {"128位", "192位", "256位"};
        JComboBox<String> strengthCombo = new JComboBox<>(strengths);
        panel.add(strengthCombo, gbc);
        
        // 按钮
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("保存设置");
        JButton cancelButton = new JButton("取消");
        
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
    
    /**
     * 显示树节点的右键菜单
     */
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
    
    /**
     * 显示Schema的上下文菜单
     */
    private void showSchemaContextMenu(ConnectionConfig config, String schemaName, int x, int y) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem refreshItem = new JMenuItem("刷新");
        JMenuItem newDatabaseItem = new JMenuItem("新建数据库");
        JMenuItem dropDatabaseItem = new JMenuItem("删除数据库");
        JMenuItem newTableItem = new JMenuItem("新建表");
        
        refreshItem.addActionListener(e -> {
            currentConnection = config;
            refreshDatabaseTree();
        });
        
        newDatabaseItem.addActionListener(e -> {
            createNewDatabase(config);
        });
        
        dropDatabaseItem.addActionListener(e -> {
            dropDatabase(config, schemaName);
        });
        
        newTableItem.addActionListener(e -> {
            createNewTable(config, schemaName);
        });
        
        contextMenu.add(refreshItem);
        contextMenu.addSeparator();
        contextMenu.add(newDatabaseItem);
        contextMenu.add(dropDatabaseItem);
        contextMenu.addSeparator();
        contextMenu.add(newTableItem);
        
        contextMenu.show(databaseTree, x, y);
    }
    
    /**
     * 显示Schema下表的上下文菜单
     */
    private void showSchemaTableContextMenu(ConnectionConfig config, String schemaName, String tableName, int x, int y) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem queryItem = new JMenuItem("查询数据");
        JMenuItem structureItem = new JMenuItem("表结构");
        JMenuItem editItem = new JMenuItem("修改表");
        JMenuItem dropItem = new JMenuItem("删除表");
        JMenuItem emptyItem = new JMenuItem("清空表");
        JMenuItem exportItem = new JMenuItem("导出数据");
        
        queryItem.addActionListener(e -> {
            currentConnection = config;
            currentTableName = schemaName + "." + tableName;
            currentPage = 1;
            pageField.setText("1");
            String sql = String.format("SELECT * FROM %s.%s", schemaName, tableName);
            sqlTextArea.setText(sql);
            executeSQL();
        });
        
        structureItem.addActionListener(e -> {
            currentConnection = config;
            showTableStructure(schemaName + "." + tableName);
        });
        
        editItem.addActionListener(e -> {
            currentConnection = config;
            showEditTableDialog(schemaName + "." + tableName);
        });
        
        dropItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "确定要删除表 " + schemaName + "." + tableName + " 吗？此操作不可逆！",
                    "确认删除", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                currentConnection = config;
                executeSQL("DROP TABLE " + schemaName + "." + tableName);
                refreshDatabaseTree();
            }
        });
        
        emptyItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "确定要清空表 " + schemaName + "." + tableName + " 的所有数据吗？此操作不可逆！",
                    "确认清空", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                currentConnection = config;
                executeSQL("TRUNCATE TABLE " + schemaName + "." + tableName);
            }
        });
        
        exportItem.addActionListener(e -> {
            currentConnection = config;
            exportTableData(schemaName + "." + tableName);
        });
        
        contextMenu.add(queryItem);
        contextMenu.add(structureItem);
        contextMenu.addSeparator();
        contextMenu.add(editItem);
        contextMenu.add(dropItem);
        contextMenu.add(emptyItem);
        contextMenu.addSeparator();
        contextMenu.add(exportItem);
        
        contextMenu.show(databaseTree, x, y);
    }
    
    /**
     * 显示Redis数据库的上下文菜单
     */
    private void showRedisDatabaseContextMenu(ConnectionConfig config, String dbName, int x, int y) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem selectItem = new JMenuItem("选择数据库");
        JMenuItem flushItem = new JMenuItem("清空数据库");
        JMenuItem refreshItem = new JMenuItem("刷新");
        
        selectItem.addActionListener(e -> {
            currentConnection = config;
            
            // 提取数据库索引
            int dbIndex = 0;
            try {
                String dbIndexStr = dbName.substring(2, dbName.indexOf(" "));
                dbIndex = Integer.parseInt(dbIndexStr);
            } catch (Exception ex) {
                // 默认使用数据库0
            }
            
            // 选择Redis数据库
            boolean success = DatabaseService.selectRedisDatabase(currentConnection, dbIndex);
            
            if (success) {
                statusLabel.setText("已切换到Redis数据库: " + dbIndex);
                sqlTextArea.setText("KEYS *");
            } else {
                statusLabel.setText("切换Redis数据库失败");
            }
        });
        
        flushItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "确定要清空Redis数据库 " + dbName + " 中的所有数据吗？此操作不可逆！",
                    "确认清空", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                currentConnection = config;
                
                // 提取数据库索引
                int dbIndex = 0;
                try {
                    String dbIndexStr = dbName.substring(2, dbName.indexOf(" "));
                    dbIndex = Integer.parseInt(dbIndexStr);
                } catch (Exception ex) {
                    // 默认使用数据库0
                }
                
                // 选择数据库并执行FLUSHDB
                if (DatabaseService.selectRedisDatabase(currentConnection, dbIndex)) {
                    executeSQL("FLUSHDB");
                    refreshDatabaseTree();
                }
            }
        });
        
        refreshItem.addActionListener(e -> {
            currentConnection = config;
            refreshDatabaseTree();
        });
        
        contextMenu.add(selectItem);
        contextMenu.add(flushItem);
        contextMenu.addSeparator();
        contextMenu.add(refreshItem);
        
        contextMenu.show(databaseTree, x, y);
    }
    
    /**
     * 显示Redis类型的上下文菜单
     */
    private void showRedisTypeContextMenu(ConnectionConfig config, String dbName, String typeName, int x, int y) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem queryItem = new JMenuItem("查询所有键");
        
        queryItem.addActionListener(e -> {
            currentConnection = config;
            
            // 提取数据库索引
            int dbIndex = 0;
            try {
                String dbIndexStr = dbName.substring(2, dbName.indexOf(" "));
                dbIndex = Integer.parseInt(dbIndexStr);
            } catch (Exception ex) {
                // 默认使用数据库0
            }
            
            // 选择Redis数据库
            if (DatabaseService.selectRedisDatabase(currentConnection, dbIndex)) {
                statusLabel.setText("已切换到Redis数据库: " + dbIndex);
                
                // 根据类型查询键
                String cmd = "KEYS *";
                sqlTextArea.setText(cmd);
                executeSQL();
            }
        });
        
        contextMenu.add(queryItem);
        
        contextMenu.show(databaseTree, x, y);
    }
    
    /**
     * 创建新数据库
     */
    private void createNewDatabase(ConnectionConfig config) {
        String dbName = JOptionPane.showInputDialog(this, "请输入新数据库名称:", "新建数据库", JOptionPane.QUESTION_MESSAGE);
        
        if (dbName != null && !dbName.trim().isEmpty()) {
            currentConnection = config;
            String sql = "CREATE DATABASE " + dbName;
            executeSQL(sql);
            refreshDatabaseTree();
        }
    }
    
    /**
     * 删除数据库
     */
    private void dropDatabase(ConnectionConfig config, String dbName) {
        if (JOptionPane.showConfirmDialog(this,
                "确定要删除数据库 " + dbName + " 吗？此操作不可逆！",
                "确认删除", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            currentConnection = config;
            String sql = "DROP DATABASE " + dbName;
            executeSQL(sql);
            refreshDatabaseTree();
        }
    }
    
    /**
     * 创建新表
     */
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
    
    /**
     * 显示连接的上下文菜单
     */
    private void showConnectionContextMenu(ConnectionConfig config, int x, int y) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem connectItem = new JMenuItem("连接");
        JMenuItem editItem = new JMenuItem("编辑连接");
        JMenuItem deleteItem = new JMenuItem("删除连接");
        JMenuItem refreshItem = new JMenuItem("刷新");
        JMenuItem importSQLItem = new JMenuItem("导入SQL文件");
        JMenuItem exportSQLItem = new JMenuItem("导出数据库");
        
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
        
        importSQLItem.addActionListener(e -> {
            importSQLFile(config);
        });
        
        exportSQLItem.addActionListener(e -> {
            exportDatabase(config);
        });
        
        contextMenu.add(connectItem);
        contextMenu.add(editItem);
        contextMenu.add(deleteItem);
        contextMenu.addSeparator();
        contextMenu.add(refreshItem);
        contextMenu.addSeparator();
        contextMenu.add(importSQLItem);
        contextMenu.add(exportSQLItem);
        
        contextMenu.show(databaseTree, x, y);
    }
    
    /**
     * 导入SQL文件
     */
    private void importSQLFile(ConnectionConfig config) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择SQL文件导入");
        fileChooser.setFileFilter(new FileNameExtensionFilter("SQL文件", "sql"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getPath();
            
            try {
                // 读取SQL文件内容
                java.nio.file.Path path = java.nio.file.Paths.get(filePath);
                String sqlContent = new String(java.nio.file.Files.readAllBytes(path));
                
                // 分割多条SQL语句
                String[] sqlStatements = sqlContent.split(";");
                
                currentConnection = config;
                
                // 执行SQL语句
                final int totalStatements = sqlStatements.length;
                
                SwingWorker<Integer, String> worker = new SwingWorker<Integer, String>() {
                    @Override
                    protected Integer doInBackground() throws Exception {
                        int successCount = 0;
                        
                        for (int i = 0; i < sqlStatements.length; i++) {
                            String sql = sqlStatements[i].trim();
                            if (sql.isEmpty()) continue;
                            
                            publish("执行SQL " + (i + 1) + "/" + totalStatements + ": " + 
                                   (sql.length() > 50 ? sql.substring(0, 50) + "..." : sql));
                            
                            Map<String, Object> result;
                            if (sql.toLowerCase().startsWith("select")) {
                                result = DatabaseService.executeQuery(config, sql);
                            } else {
                                result = DatabaseService.executeUpdate(config, sql);
                            }
                            
                            if ((boolean) result.get("success")) {
                                successCount++;
                            }
                        }
                        return successCount;
                    }
                    
                    @Override
                    protected void process(List<String> chunks) {
                        for (String status : chunks) {
                            statusLabel.setText(status);
                        }
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            int successCount = get();
                            statusLabel.setText("SQL导入完成，成功执行 " + successCount + "/" + totalStatements + " 个语句");
                            refreshDatabaseTree();
                        } catch (Exception e) {
                            e.printStackTrace();
                            statusLabel.setText("SQL导入出错: " + e.getMessage());
                        }
                    }
                };
                
                worker.execute();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                        "导入SQL文件失败: " + e.getMessage(), 
                        "导入失败", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * 导出数据库
     */
    private void exportDatabase(ConnectionConfig config) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出数据库到SQL文件");
        fileChooser.setFileFilter(new FileNameExtensionFilter("SQL文件", "sql"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getPath();
            if (!filePath.endsWith(".sql")) {
                filePath += ".sql";
            }
            
            final String finalPath = filePath;
            
            // 获取数据库中的所有表
            currentConnection = config;
            
            SwingWorker<Boolean, String> worker = new SwingWorker<Boolean, String>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    List<String> tables = DatabaseService.getTables(config, null);
                    if (tables.isEmpty()) {
                        return false;
                    }
                    
                    java.io.PrintWriter writer = new java.io.PrintWriter(new File(finalPath));
                    writer.println("-- 导出数据库: " + config.getName());
                    writer.println("-- 导出时间: " + new Date());
                    writer.println();
                    
                    for (String table : tables) {
                        publish("正在导出表: " + table);
                        
                        // 导出表结构
                        writer.println("-- 表结构: " + table);
                        writer.println("DROP TABLE IF EXISTS `" + table + "`;");
                        
                        Map<String, Object> structResult = DatabaseService.executeQuery(
                                config, "SHOW CREATE TABLE " + table);
                        if ((boolean)structResult.get("success")) {
                            @SuppressWarnings("unchecked")
                            List<List<Object>> data = (List<List<Object>>) structResult.get("data");
                            if (!data.isEmpty() && data.get(0).size() > 1) {
                                String createTableSql = data.get(0).get(1).toString();
                                writer.println(createTableSql + ";");
                            }
                        }
                        writer.println();
                        
                        // 导出表数据
                        writer.println("-- 表数据: " + table);
                        Map<String, Object> dataResult = DatabaseService.executeQuery(
                                config, "SELECT * FROM " + table);
                        if ((boolean)dataResult.get("success")) {
                            @SuppressWarnings("unchecked")
                            List<String> columns = (List<String>) dataResult.get("columns");
                            @SuppressWarnings("unchecked")
                            List<List<Object>> data = (List<List<Object>>) dataResult.get("data");
                            
                            for (List<Object> row : data) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("INSERT INTO `").append(table).append("` (");
                                
                                // 添加列名
                                for (int i = 0; i < columns.size(); i++) {
                                    if (i > 0) sb.append(", ");
                                    sb.append("`").append(columns.get(i)).append("`");
                                }
                                
                                sb.append(") VALUES (");
                                
                                // 添加值
                                for (int i = 0; i < row.size(); i++) {
                                    if (i > 0) sb.append(", ");
                                    
                                    Object value = row.get(i);
                                    if (value == null) {
                                        sb.append("NULL");
                                    } else if (value instanceof Number) {
                                        sb.append(value);
                                    } else {
                                        sb.append("'")
                                          .append(value.toString().replace("'", "''"))
                                          .append("'");
                                    }
                                }
                                
                                sb.append(");");
                                writer.println(sb.toString());
                            }
                        }
                        writer.println();
                    }
                    
                    writer.close();
                    return true;
                }
                
                @Override
                protected void process(List<String> chunks) {
                    for (String status : chunks) {
                        statusLabel.setText(status);
                    }
                }
                
                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            statusLabel.setText("数据库导出成功: " + finalPath);
                            JOptionPane.showMessageDialog(MainFrame.this, 
                                    "数据库已成功导出到: " + finalPath, 
                                    "导出成功", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            statusLabel.setText("数据库导出失败");
                            JOptionPane.showMessageDialog(MainFrame.this, 
                                    "数据库导出失败，未找到任何表", 
                                    "导出失败", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        statusLabel.setText("数据库导出出错: " + e.getMessage());
                        JOptionPane.showMessageDialog(MainFrame.this, 
                                "数据库导出失败: " + e.getMessage(), 
                                "导出失败", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            
            statusLabel.setText("正在导出数据库...");
            worker.execute();
        }
    }
    
    /**
     * 创建数据编辑控制面板
     */
    private JPanel createDataEditPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        panel.setBackground(new Color(60, 63, 65));
        
        JLabel editLabel = new JLabel("编辑操作:");
        editLabel.setForeground(new Color(187, 187, 187));
        
        addRowButton = new JButton("添加行");
        addRowButton.setEnabled(false);
        addRowButton.setBackground(new Color(60, 63, 65));
        addRowButton.setForeground(new Color(187, 187, 187));
        
        deleteRowButton = new JButton("删除行");
        deleteRowButton.setEnabled(false);
        deleteRowButton.setBackground(new Color(60, 63, 65));
        deleteRowButton.setForeground(new Color(187, 187, 187));
        
        submitChangesButton = new JButton("提交更改");
        submitChangesButton.setEnabled(false);
        submitChangesButton.setBackground(new Color(60, 63, 65));
        submitChangesButton.setForeground(new Color(187, 187, 187));
        
        JCheckBox editableCheckBox = new JCheckBox("启用编辑");
        editableCheckBox.setBackground(new Color(60, 63, 65));
        editableCheckBox.setForeground(new Color(187, 187, 187));
        
        // 添加事件监听器
        editableCheckBox.addActionListener(e -> {
            isDataEditable = editableCheckBox.isSelected();
            addRowButton.setEnabled(isDataEditable);
            deleteRowButton.setEnabled(isDataEditable);
            submitChangesButton.setEnabled(isDataEditable);
            resultTableModel.setEditable(isDataEditable);
            
            if (isDataEditable && currentTableName != null) {
                // 获取表的主键信息
                fetchTablePrimaryKeys(currentTableName);
            }
        });
        
        addRowButton.addActionListener(e -> {
            if (currentTableName != null && isDataEditable) {
                resultTableModel.addEmptyRow();
                int lastRow = resultTableModel.getRowCount() - 1;
                resultTable.setRowSelectionInterval(lastRow, lastRow);
                resultTable.scrollRectToVisible(resultTable.getCellRect(lastRow, 0, true));
            }
        });
        
        deleteRowButton.addActionListener(e -> {
            int[] selectedRows = resultTable.getSelectedRows();
            if (selectedRows.length > 0 && isDataEditable) {
                if (JOptionPane.showConfirmDialog(this, 
                        "确定要删除选中的 " + selectedRows.length + " 行数据吗？", 
                        "确认删除", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    
                    // 从表格中删除选中行
                    for (int i = selectedRows.length - 1; i >= 0; i--) {
                        int row = selectedRows[i];
                        // 如果是已存在的行，生成DELETE语句
                        if (row < resultTableModel.getOriginalRowCount()) {
                            String deleteSQL = generateDeleteSQL(row);
                            if (deleteSQL != null) {
                                executeSQL(deleteSQL);
                            }
                        }
                        resultTableModel.removeRow(row);
                    }
                }
            }
        });
        
        submitChangesButton.addActionListener(e -> {
            if (currentTableName != null && isDataEditable) {
                saveTableChanges();
            }
        });
        
        // 添加到面板
        panel.add(editableCheckBox);
        panel.add(addRowButton);
        panel.add(deleteRowButton);
        panel.add(submitChangesButton);
        
        return panel;
    }
    
    /**
     * 获取表的主键信息
     */
    private void fetchTablePrimaryKeys(String tableName) {
        if (currentConnection == null) return;
        
        primaryKeys.clear();
        
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                List<String> keys = new ArrayList<>();
                
                // 执行查询获取主键信息
                String sql = "SHOW KEYS FROM " + tableName + " WHERE Key_name = 'PRIMARY'";
                Map<String, Object> result = DatabaseService.executeQuery(currentConnection, sql);
                
                if ((boolean)result.get("success")) {
                    @SuppressWarnings("unchecked")
                    List<String> columns = (List<String>) result.get("columns");
                    @SuppressWarnings("unchecked")
                    List<List<Object>> data = (List<List<Object>>) result.get("data");
                    
                    int columnNameIndex = columns.indexOf("Column_name");
                    if (columnNameIndex >= 0) {
                        for (List<Object> row : data) {
                            keys.add(row.get(columnNameIndex).toString());
                        }
                    }
                }
                
                return keys;
            }
            
            @Override
            protected void done() {
                try {
                    primaryKeys = get();
                    if (primaryKeys.isEmpty()) {
                        statusLabel.setText("警告: 表 " + tableName + " 没有主键");
                    } else {
                        statusLabel.setText("已获取表 " + tableName + " 的主键: " + String.join(", ", primaryKeys));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    statusLabel.setText("获取主键失败: " + e.getMessage());
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * 保存表格更改
     */
    private void saveTableChanges() {
        if (currentConnection == null || currentTableName == null) return;
        
        // 生成修改记录的UPDATE语句
        StringBuilder batchSQL = new StringBuilder();
        int successCount = 0;
        
        // 生成所有的插入和更新SQL语句
        List<String> sqlStatements = new ArrayList<>();
        
        // 处理所有行
        for (int row = 0; row < resultTableModel.getRowCount(); row++) {
            String sql = null;
            
            if (row < resultTableModel.getOriginalRowCount()) {
                // 现有行的更新
                if (resultTableModel.isRowModified(row)) {
                    sql = generateUpdateSQL(row);
                }
            } else {
                // 新行的插入
                sql = generateInsertSQL(row);
            }
            
            if (sql != null) {
                sqlStatements.add(sql);
            }
        }
        
        // 执行所有SQL语句
        boolean hasSuccess = false;
        for (String sql : sqlStatements) {
            Map<String, Object> result = DatabaseService.executeUpdate(currentConnection, sql);
            if ((boolean)result.get("success")) {
                successCount++;
                batchSQL.append(sql).append(";\n");
                hasSuccess = true;
            } else {
                String error = (String) result.get("error");
                JOptionPane.showMessageDialog(this,
                        "执行SQL出错: " + error + "\nSQL: " + sql,
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        statusLabel.setText("已执行 " + successCount + "/" + sqlStatements.size() + " 个SQL语句");
        
        // 重新加载数据
        if (hasSuccess) {
            executeCurrentQuery();
            
            // 清除修改状态
            resultTableModel.resetModifiedState();
            
            // 显示成功消息
            if (successCount > 0) {
                JOptionPane.showMessageDialog(this, 
                    "成功保存 " + successCount + " 条修改记录", 
                    "保存成功", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    /**
     * 生成UPDATE SQL语句
     */
    private String generateUpdateSQL(int row) {
        if (currentTableName == null || primaryKeys.isEmpty()) return null;
        
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(currentTableName).append(" SET ");
        
        List<String> columnNames = resultTableModel.getColumnNames();
        List<Object> currentValues = resultTableModel.getRowData(row);
        List<Object> originalValues = resultTableModel.getOriginalRowData(row);
        
        boolean hasChanges = false;
        for (int col = 0; col < columnNames.size(); col++) {
            String columnName = columnNames.get(col);
            if (primaryKeys.contains(columnName)) continue;
            
            Object currentValue = currentValues.get(col);
            Object originalValue = originalValues.get(col);
            
            if ((currentValue == null && originalValue != null) ||
                (currentValue != null && !currentValue.equals(originalValue))) {
                
                if (hasChanges) sql.append(", ");
                sql.append(columnName).append(" = ");
                
                if (currentValue == null) {
                    sql.append("NULL");
                } else if (currentValue instanceof Number) {
                    sql.append(currentValue);
                } else {
                    sql.append("'").append(currentValue.toString().replace("'", "''")).append("'");
                }
                
                hasChanges = true;
            }
        }
        
        if (!hasChanges) return null;
        
        // 添加WHERE条件
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
    
    /**
     * 生成INSERT SQL语句
     */
    private String generateInsertSQL(int row) {
        if (currentTableName == null) return null;
        
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(currentTableName).append(" (");
        
        List<String> columnNames = resultTableModel.getColumnNames();
        List<Object> values = resultTableModel.getRowData(row);
        
        // 添加列名
        boolean first = true;
        for (String column : columnNames) {
            if (!first) sql.append(", ");
            sql.append(column);
            first = false;
        }
        
        sql.append(") VALUES (");
        
        // 添加值
        first = true;
        for (Object value : values) {
            if (!first) sql.append(", ");
            
            if (value == null) {
                sql.append("NULL");
            } else if (value instanceof Number) {
                sql.append(value);
            } else {
                sql.append("'").append(value.toString().replace("'", "''")).append("'");
            }
            
            first = false;
        }
        
        sql.append(")");
        return sql.toString();
    }
    
    /**
     * 生成DELETE SQL语句
     */
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
    
    /**
     * 显示表的上下文菜单
     */
    private void showTableContextMenu(String tableName, int x, int y) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem queryItem = new JMenuItem("查询数据");
        JMenuItem structureItem = new JMenuItem("表结构");
        JMenuItem editItem = new JMenuItem("修改表");
        JMenuItem dropItem = new JMenuItem("删除表");
        JMenuItem emptyItem = new JMenuItem("清空表");
        JMenuItem exportItem = new JMenuItem("导出数据");
        
        queryItem.addActionListener(e -> {
            currentTableName = tableName;
            currentPage = 1;
            pageField.setText("1");
            executeCurrentQuery();
        });
        
        structureItem.addActionListener(e -> {
            showTableStructure(tableName);
        });
        
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
        
        exportItem.addActionListener(e -> {
            exportTableData(tableName);
        });
        
        contextMenu.add(queryItem);
        contextMenu.add(structureItem);
        contextMenu.addSeparator();
        contextMenu.add(editItem);
        contextMenu.add(dropItem);
        contextMenu.add(emptyItem);
        contextMenu.addSeparator();
        contextMenu.add(exportItem);
        
        contextMenu.show(databaseTree, x, y);
    }
    
    /**
     * 导出表数据
     */
    private void exportTableData(String tableName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出表数据");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV文件", "csv"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getPath();
            if (!filePath.endsWith(".csv")) {
                filePath += ".csv";
            }
            
            final String finalPath = filePath;
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    try {
                        // 执行查询获取所有数据
                        Map<String, Object> result = DatabaseService.executeQuery(
                                currentConnection, "SELECT * FROM " + tableName);
                        
                        if ((boolean)result.get("success")) {
                            @SuppressWarnings("unchecked")
                            List<String> columns = (List<String>) result.get("columns");
                            @SuppressWarnings("unchecked")
                            List<List<Object>> data = (List<List<Object>>) result.get("data");
                            
                            // 导出到CSV
                            java.io.PrintWriter writer = new java.io.PrintWriter(new File(finalPath));
                            
                            // 写入表头
                            StringBuilder header = new StringBuilder();
                            for (int i = 0; i < columns.size(); i++) {
                                if (i > 0) header.append(",");
                                header.append("\"").append(columns.get(i)).append("\"");
                            }
                            writer.println(header.toString());
                            
                            // 写入数据
                            for (List<Object> row : data) {
                                StringBuilder line = new StringBuilder();
                                for (int i = 0; i < row.size(); i++) {
                                    if (i > 0) line.append(",");
                                    Object value = row.get(i);
                                    if (value != null) {
                                        line.append("\"").append(value.toString().replace("\"", "\"\"")).append("\"");
                                    } else {
                                        line.append("\"\"");
                                    }
                                }
                                writer.println(line.toString());
                            }
                            
                            writer.close();
                            return true;
                        }
                        return false;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                
                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            JOptionPane.showMessageDialog(MainFrame.this, 
                                    "表数据已成功导出到: " + finalPath, 
                                    "导出成功", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(MainFrame.this, 
                                    "导出表数据失败", 
                                    "导出失败", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(MainFrame.this, 
                                "导出表数据失败: " + e.getMessage(), 
                                "导出失败", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            
            statusLabel.setText("正在导出表数据...");
            worker.execute();
        }
    }

    /**
     * 创建分页控制面板
     */
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

    /**
     * 将查询结果导出为CSV文件
     * @param filePath 文件路径
     * @throws Exception 导出异常
     */
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

    /**
     * 将查询结果导出为Excel文件
     * @param filePath 文件路径
     * @throws Exception 导出异常
     */
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

    /**
     * 设置应用程序主题颜色
     */
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
            
            // 设置状态栏颜色
            statusLabel.setForeground(new Color(187, 187, 187));
            
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
            }
            
            if (leftSplitPane != null) {
                leftSplitPane.setBackground(new Color(43, 43, 43));
                leftSplitPane.setBorder(BorderFactory.createLineBorder(new Color(60, 63, 65)));
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
            
            // 设置状态栏颜色
            statusLabel.setForeground(Color.BLACK);
            
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
            }
            
            if (leftSplitPane != null) {
                leftSplitPane.setBackground(new Color(240, 240, 240));
                leftSplitPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
            }
        }
    }
    
    /**
     * 更新全局字体大小
     */
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
} 