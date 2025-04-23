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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Enumeration;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

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
            // 设置左侧分割面板的高度与主分割面板相同
            int leftPanelHeight = contentHeight - 50; // 减去估算的空间
            leftSplitPane.setPreferredSize(new Dimension(leftSplitPane.getWidth(), leftPanelHeight));
            
            // 重新确定分割位置
            leftSplitPane.setDividerLocation(leftSplitPane.getDividerLocation());
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
     * 处理树节点选择事件
     */
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
    
    /**
     * 加载数据库中的表
     */
    private void loadDatabaseTables(ConnectionConfig config, String dbName, DefaultMutableTreeNode parentNode) {
        // 在后台线程中执行数据库操作
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                // 获取数据库中的表
                return DatabaseService.getTables(config, dbName);
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

    /**
     * 刷新数据库树
     */
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
                            
                            for (String db : databases) {
                                DefaultMutableTreeNode dbNode = new DefaultMutableTreeNode(db);
                                connNode.add(dbNode);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
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

    /**
     * 执行SQL
     */
    private void executeSQL() {
        String sql = sqlTextArea.getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入SQL语句", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        executeSQL(sql);
    }
    
    /**
     * 执行SQL语句
     */
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
    
    /**
     * 执行当前查询（分页）
     */
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
    
    /**
     * 内部执行SQL的实现
     */
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
                            int updateCount = (int) result.get("updateCount");
                            statusLabel.setText("更新成功，影响 " + updateCount + " 行");
                            
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

    /**
     * 更新分页信息
     */
    private void updatePaginationInfo() {
        totalPagesLabel.setText("共 " + totalPages + " 页, 共 " + totalRecords + " 条记录");
        prevPageButton.setEnabled(currentPage > 1);
        nextPageButton.setEnabled(currentPage < totalPages);
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
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导入连接配置");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON文件", "json"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getPath();
            importConnectionsFromFile(filePath);
        }
    }
    
    /**
     * 从文件导入连接配置
     */
    private void importConnectionsFromFile(String filePath) {
        try {
            // 使用ConnectionManager导入连接
            List<ConnectionConfig> importedConfigs = new ArrayList<>();
            
            // 实际导入逻辑
            ObjectMapper mapper = new ObjectMapper();
            File file = new File(filePath);
            importedConfigs = mapper.readValue(file, new TypeReference<List<ConnectionConfig>>() {});
            
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
    
    /**
     * 导出连接配置
     */
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
    
    /**
     * 导出连接配置到文件
     */
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
     * 显示性能监控对话框
     */
    private void showMonitoringDialog() {
        JDialog dialog = new JDialog(this, "性能监控", true);
        dialog.setSize(600, 450);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel statsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        
        // 如果当前连接有效，则获取性能统计
        if (currentConnection != null) {
            try {
                Map<String, String> stats = DatabaseService.getDatabaseStats(currentConnection);
                
                for (Map.Entry<String, String> entry : stats.entrySet()) {
                    addStatItem(statsPanel, entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                statsPanel.add(new JLabel("获取性能统计信息失败: " + e.getMessage()));
            }
        } else {
            statsPanel.add(new JLabel("请先连接到数据库"));
        }
        
        JScrollPane scrollPane = new JScrollPane(statsPanel);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
    
    /**
     * 添加统计项
     */
    private void addStatItem(JPanel panel, String name, String value) {
        JPanel itemPanel = new JPanel(new BorderLayout());
        itemPanel.add(new JLabel(name + ": "), BorderLayout.WEST);
        itemPanel.add(new JLabel(value), BorderLayout.CENTER);
        panel.add(itemPanel);
    }
    
    /**
     * 显示安全设置对话框
     */
    private void showSecurityDialog() {
        JDialog dialog = new JDialog(this, "安全设置", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
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
     * 连接数据库
     */
    private void connectDatabase(ConnectionConfig config) {
        // 创建SwingWorker在后台线程中连接数据库
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return DatabaseService.connect(config);
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

    /**
     * 创建数据编辑控制面板
     */
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
                modifiedRows.add(rowCount - 1);
            }
        });
        
        deleteRowButton.addActionListener(e -> {
            int selectedRow = resultTable.getSelectedRow();
            if (selectedRow != -1) {
                // 记录要删除的行
                modifiedRows.add(selectedRow);
                
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
    
    /**
     * 保存表格更改
     */
    private void saveTableChanges() {
        if (currentConnection == null || currentTableName == null || modifiedRows.isEmpty()) {
            return;
        }
        
        try {
            int successCount = 0;
            List<String> sqlStatements = new ArrayList<>();
            
            for (Integer rowIndex : modifiedRows) {
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
            
            // 执行所有SQL语句
            for (String sql : sqlStatements) {
                Map<String, Object> result = DatabaseService.executeUpdate(currentConnection, sql);
                if ((boolean) result.get("success")) {
                    successCount++;
                }
            }
            
            statusLabel.setText("保存成功 " + successCount + "/" + sqlStatements.size() + " 项修改");
            
            // 清除修改标记
            modifiedRows.clear();
            resultTableModel.clearModifications();
            
            // 刷新表数据
            executeCurrentQuery();
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "保存更改失败: " + e.getMessage(),
                    "保存失败", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 生成更新SQL语句
     */
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
    
    /**
     * 生成插入SQL语句
     */
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
    
    /**
     * 生成删除SQL语句
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
     * 获取表的主键信息
     */
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
    
    /**
     * 显示表结构
     */
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
    
    /**
     * 显示添加列对话框
     */
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
    
    /**
     * 显示编辑表对话框
     */
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
            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            List<String> columns = (List<String>) result.get("columns");
            
            // 创建表格模型
            QueryResultTableModel model = new QueryResultTableModel(false);
            model.setDataFromMap(data, columns);
            
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
    
    /**
     * 显示连接上下文菜单
     */
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
    
    /**
     * 显示数据库/schema上下文菜单
     */
    private void showSchemaContextMenu(ConnectionConfig config, String schemaName, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem refreshItem = new JMenuItem("刷新");
        JMenuItem queryItem = new JMenuItem("执行查询");
        
        refreshItem.addActionListener(e -> {
            currentConnection = config;
            refreshDatabaseTree();
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
        
        menu.add(refreshItem);
        menu.add(queryItem);
        
        menu.show(databaseTree, x, y);
    }
    
    /**
     * 显示表上下文菜单
     */
    private void showSchemaTableContextMenu(ConnectionConfig config, String schemaName, String tableName, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem queryItem = new JMenuItem("查询数据");
        JMenuItem structureItem = new JMenuItem("表结构");
        JMenuItem exportItem = new JMenuItem("导出数据");
        
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
        menu.add(exportItem);
        
        menu.show(databaseTree, x, y);
    }
    
    /**
     * 显示Redis数据库上下文菜单
     */
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
    
    /**
     * 显示Redis类型上下文菜单
     */
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

//    /**
//     * 显示连接上下文菜单
//     * @param node 节点
//     * @param e 鼠标事件
//     */
//    private void showConnectionContextMenu(DefaultMutableTreeNode node, MouseEvent e) {
//        ConnectionNode connectionNode = (ConnectionNode) node.getUserObject();
//
//        JPopupMenu popupMenu = new JPopupMenu();
//
//        JMenuItem connectItem = new JMenuItem("连接");
//        connectItem.addActionListener(event -> {
//            try {
//                connectToDatabase(connectionNode);
//            } catch (Exception ex) {
//                JOptionPane.showMessageDialog(this, "连接失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
//            }
//        });
//
//        JMenuItem editItem = new JMenuItem("编辑");
//        editItem.addActionListener(event -> {
//            editConnection(connectionNode);
//        });
//
//        JMenuItem deleteItem = new JMenuItem("删除");
//        deleteItem.addActionListener(event -> {
//            int result = JOptionPane.showConfirmDialog(this,
//                    "确定要删除连接 '" + connectionNode.getName() + "' 吗?",
//                    "确认删除", JOptionPane.YES_NO_OPTION);
//            if (result == JOptionPane.YES_OPTION) {
//                deleteConnection(connectionNode);
//            }
//        });
//
//        JMenuItem refreshItem = new JMenuItem("刷新");
//        refreshItem.addActionListener(event -> {
//            refreshConnection(connectionNode);
//        });
//
//        popupMenu.add(connectItem);
//        popupMenu.add(editItem);
//        popupMenu.add(deleteItem);
//        popupMenu.addSeparator();
//        popupMenu.add(refreshItem);
//
//        popupMenu.show(e.getComponent(), e.getX(), e.getY());
//    }
//
//    /**
//     * 显示模式上下文菜单
//     * @param node 节点
//     * @param e 鼠标事件
//     */
//    private void showSchemaContextMenu(DefaultMutableTreeNode node, MouseEvent e) {
//        SchemaNode schemaNode = (SchemaNode) node.getUserObject();
//
//        JPopupMenu popupMenu = new JPopupMenu();
//
//        JMenuItem refreshItem = new JMenuItem("刷新");
//        refreshItem.addActionListener(event -> {
//            refreshSchema(schemaNode);
//        });
//
//        JMenuItem queryItem = new JMenuItem("执行查询");
//        queryItem.addActionListener(event -> {
//            openQueryTab(schemaNode);
//        });
//
//        popupMenu.add(refreshItem);
//        popupMenu.add(queryItem);
//
//        popupMenu.show(e.getComponent(), e.getX(), e.getY());
//    }
//
//    /**
//     * 显示表上下文菜单
//     * @param node 节点
//     * @param e 鼠标事件
//     */
//    private void showSchemaTableContextMenu(DefaultMutableTreeNode node, MouseEvent e) {
//        TableNode tableNode = (TableNode) node.getUserObject();
//        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
//        SchemaNode schemaNode = (SchemaNode) parentNode.getUserObject();
//
//        JPopupMenu popupMenu = new JPopupMenu();
//
//        JMenuItem queryDataItem = new JMenuItem("查询数据");
//        queryDataItem.addActionListener(event -> {
//            queryTableData(schemaNode, tableNode);
//        });
//
//        JMenuItem structureItem = new JMenuItem("表结构");
//        structureItem.addActionListener(event -> {
//            showTableStructure(schemaNode, tableNode);
//        });
//
//        JMenuItem exportItem = new JMenuItem("导出到CSV");
//        exportItem.addActionListener(event -> {
//            exportTableData(schemaNode, tableNode);
//        });
//
//        popupMenu.add(queryDataItem);
//        popupMenu.add(structureItem);
//        popupMenu.add(exportItem);
//
//        popupMenu.show(e.getComponent(), e.getX(), e.getY());
//    }
//
//    /**
//     * 显示Redis数据库上下文菜单
//     * @param node 节点
//     * @param e 鼠标事件
//     */
//    private void showRedisDatabaseContextMenu(DefaultMutableTreeNode node, MouseEvent e) {
//        RedisDatabaseNode databaseNode = (RedisDatabaseNode) node.getUserObject();
//
//        JPopupMenu popupMenu = new JPopupMenu();
//
//        JMenuItem selectItem = new JMenuItem("选择数据库");
//        selectItem.addActionListener(event -> {
//            selectRedisDatabase(databaseNode);
//        });
//
//        JMenuItem flushItem = new JMenuItem("清空数据库");
//        flushItem.addActionListener(event -> {
//            int result = JOptionPane.showConfirmDialog(this,
//                    "确定要清空数据库 '" + databaseNode.getName() + "' 吗? 此操作不可恢复!",
//                    "确认清空", JOptionPane.YES_NO_OPTION);
//            if (result == JOptionPane.YES_OPTION) {
//                flushRedisDatabase(databaseNode);
//            }
//        });
//
//        JMenuItem refreshItem = new JMenuItem("刷新");
//        refreshItem.addActionListener(event -> {
//            refreshRedisDatabase(databaseNode);
//        });
//
//        popupMenu.add(selectItem);
//        popupMenu.add(flushItem);
//        popupMenu.addSeparator();
//        popupMenu.add(refreshItem);
//
//        popupMenu.show(e.getComponent(), e.getX(), e.getY());
//    }
}