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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
    
    public MainFrame() {
        initComponents();
        setupListeners();
        
        setTitle("数据库可视化工具");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // 加载连接
        loadConnections();
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
        
        // 创建分页控制面板
        paginationPanel = createPaginationPanel();
        
        resultTableModel = new QueryResultTableModel();
        resultTable = new JTable(resultTableModel);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        statusLabel = new JLabel("就绪");
        
        // 创建工具栏
        JToolBar toolBar = createToolBar();
        
        // 创建面板和分割窗
        JScrollPane treeScrollPane = new JScrollPane(databaseTree);
        JScrollPane sqlScrollPane = new JScrollPane(sqlTextArea);
        JScrollPane resultScrollPane = new JScrollPane(resultTable);
        
        leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeScrollPane, new JPanel());
        leftSplitPane.setDividerLocation(300);
        
        JPanel sqlPanel = new JPanel(new BorderLayout());
        sqlPanel.add(new JLabel("SQL查询:"), BorderLayout.NORTH);
        sqlPanel.add(sqlScrollPane, BorderLayout.CENTER);
        
        // 添加分页面板到SQL面板底部
        sqlPanel.add(paginationPanel, BorderLayout.SOUTH);
        
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
        JMenuItem newConnItem = new JMenuItem("新建连接");
        JMenuItem importItem = new JMenuItem("导入连接配置");
        JMenuItem exportItem = new JMenuItem("导出连接配置");
        JMenuItem exitItem = new JMenuItem("退出");
        
        newConnItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewConnection();
            }
        });
        
        // 添加导入连接配置功能
        importItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importConnections();
            }
        });
        
        // 添加导出连接配置功能
        exportItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportConnections();
            }
        });
        
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        
        fileMenu.add(newConnItem);
        fileMenu.add(importItem);
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // 编辑菜单
        JMenu editMenu = new JMenu("编辑");
        JMenuItem executeItem = new JMenuItem("执行SQL");
        JMenuItem formatSqlItem = new JMenuItem("格式化SQL");
        JMenuItem clearItem = new JMenuItem("清空编辑器");
        
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
        
        editMenu.add(executeItem);
        editMenu.add(formatSqlItem);
        editMenu.add(clearItem);
        
        // 工具菜单
        JMenu toolsMenu = new JMenu("工具");
        JMenuItem monitorItem = new JMenuItem("性能监控");
        JMenuItem securityItem = new JMenuItem("安全设置");
        
        monitorItem.addActionListener(new ActionListener() {
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
        
        toolsMenu.add(monitorItem);
        toolsMenu.add(securityItem);
        
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
        
        toolBar.add(newConnButton);
        toolBar.add(executeButton);
        toolBar.add(formatButton);
        toolBar.add(refreshButton);
        toolBar.add(monitorButton);
        
        return toolBar;
    }
    
    private void setupListeners() {
        // 监听窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DatabaseService.closeAllConnections();
            }
        });
        
        // 监听树节点选择事件
        databaseTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                handleTreeSelection(e);
            }
        });
        
        // 修改树的鼠标监听器
        databaseTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                            databaseTree.getLastSelectedPathComponent();
                    if (node == null) return;
                    
                    Object userObject = node.getUserObject();
                    if (userObject instanceof ConnectionConfig) {
                        // 双击连接节点，连接数据库并展开
                        connectDatabase((ConnectionConfig)userObject);
                    } else if (userObject instanceof String && node.getParent() != null) {
                        // 双击表节点，执行默认查询而不是显示表结构
                        if (node.isLeaf()) {
                            String tableName = (String)userObject;
                            // 设置当前表名，并重置分页
                            currentTableName = tableName;
                            currentPage = 1;
                            pageField.setText("1");
                            // 执行查询
                            String sql = String.format("SELECT * FROM %s", tableName);
                            sqlTextArea.setText(sql);
                            executeSQL();
                        }
                    }
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    // 右键点击，显示上下文菜单
                    int row = databaseTree.getClosestRowForLocation(e.getX(), e.getY());
                    databaseTree.setSelectionRow(row);
                    
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                            databaseTree.getLastSelectedPathComponent();
                    if (node == null) return;
                    
                    Object userObject = node.getUserObject();
                    if (userObject instanceof String && node.getParent() != null) {
                        // 右键点击表节点
                        if (node.isLeaf()) {
                            String tableName = (String)userObject;
                            showTableContextMenu(tableName, e.getX(), e.getY());
                        }
                    } else if (userObject instanceof ConnectionConfig) {
                        // 右键点击连接节点
                        ConnectionConfig config = (ConnectionConfig)userObject;
                        showConnectionContextMenu(config, e.getX(), e.getY());
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
                // 选择了表节点
                currentConnection = (ConnectionConfig) parentObject;
                String tableName = (String) userObject;
                
                // 生成查看表结构的SQL
                String sql = String.format("SELECT * FROM %s LIMIT 100", tableName);
                sqlTextArea.setText(sql);
                statusLabel.setText("已选择表: " + tableName);
            }
        }
    }
    
    /**
     * 加载所有连接
     */
    private void loadConnections() {
        rootNode.removeAllChildren();
        
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
                        
                        // 获取表
                        List<String> schemas = DatabaseService.getSchemas(currentConnection);
                        if (schemas.isEmpty()) {
                            // 如果没有schema，直接获取表
                            List<String> tables = DatabaseService.getTables(currentConnection, null);
                            for (String table : tables) {
                                connNode.add(new DefaultMutableTreeNode(table));
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
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON文件", "json"));
        
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
            java.io.File file = new java.io.File(filePath);
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
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON文件", "json"));
        
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
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(filePath), connections);
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
     * 显示连接的上下文菜单
     */
    private void showConnectionContextMenu(ConnectionConfig config, int x, int y) {
        JPopupMenu contextMenu = new JPopupMenu();
        
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
        
        contextMenu.add(connectItem);
        contextMenu.add(editItem);
        contextMenu.add(deleteItem);
        contextMenu.addSeparator();
        contextMenu.add(refreshItem);
        
        contextMenu.show(databaseTree, x, y);
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
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV文件", "csv"));
        
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
                            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.File(finalPath));
                            
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
        
        JLabel pageLabel = new JLabel("页码:");
        pageField = new JTextField("1", 3);
        
        JLabel pageSizeLabel = new JLabel("每页行数:");
        pageSizeField = new JTextField(String.valueOf(pageSize), 4);
        
        prevPageButton = new JButton("上一页");
        nextPageButton = new JButton("下一页");
        totalPagesLabel = new JLabel("共 1 页");
        
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
} 