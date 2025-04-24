package com.database.visualization.view;

import com.database.visualization.controller.DatabaseService;
import com.database.visualization.model.ConnectionConfig;

import javax.swing.*;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 数据库连接对话框
 */
public class ConnectionDialog extends JDialog {
    private ConnectionConfig connectionConfig;
    private boolean isNewConnection;
    private boolean isConfirmed = false;
    
    private JTextField nameField;
    private JComboBox<String> databaseTypeCombo;
    private JTextField hostField;
    private JTextField portField;
    private JTextField databaseField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextArea urlArea;
    
    private static final Map<String, Integer> DEFAULT_PORTS = new HashMap<>();
    
    static {
        DEFAULT_PORTS.put("mysql", 3306);
        DEFAULT_PORTS.put("postgresql", 5432);
        DEFAULT_PORTS.put("oracle", 1521);
        DEFAULT_PORTS.put("sqlserver", 1433);
        DEFAULT_PORTS.put("redis", 6379);
        DEFAULT_PORTS.put("sqlite", 0); // SQLite不需要端口
    }
    
    public ConnectionDialog(Frame owner, ConnectionConfig config, boolean isNew) {
        super(owner, isNew ? "添加新连接" : "编辑连接", true);
        this.connectionConfig = config != null ? config : new ConnectionConfig();
        this.isNewConnection = isNew;
        
        initComponents();
        loadConnectionData();
        
        setSize(500, 550);
        setLocationRelativeTo(owner);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // 表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 名称
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("连接名称:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        nameField = new JTextField(20);
        formPanel.add(nameField, gbc);
        
        // 数据库类型
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("数据库类型:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        databaseTypeCombo = new JComboBox<>(new String[]{
                "MySQL", "PostgreSQL", "Oracle", "SQL Server", "SQLite", "Redis"
        });
        databaseTypeCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selectedType = e.getItem().toString().toLowerCase();
                    updateUIForDatabaseType(selectedType);
                }
            }
        });
        formPanel.add(databaseTypeCombo, gbc);
        
        // 主机
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("主机:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        hostField = new JTextField(20);
        formPanel.add(hostField, gbc);
        
        // 端口
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("端口:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        portField = new JTextField(20);
        formPanel.add(portField, gbc);
        
        // 数据库
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("数据库:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        databaseField = new JTextField(20);
        formPanel.add(databaseField, gbc);
        
        // 用户名
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("用户名:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        usernameField = new JTextField(20);
        formPanel.add(usernameField, gbc);
        
        // 密码
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("密码:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);
        
        // URL
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("连接URL:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        urlArea = new JTextArea(3, 20);
        urlArea.setEditable(true);
        JScrollPane urlScrollPane = new JScrollPane(urlArea);
        formPanel.add(urlScrollPane, gbc);
        
        // 添加URL监听器
        urlArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                parseUrl();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                parseUrl();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                parseUrl();
            }
        });
        
        // 按钮面板
        JPanel buttonPanel = new JPanel();
        JButton testButton = new JButton("测试连接");
        JButton saveButton = new JButton("保存");
        JButton cancelButton = new JButton("取消");
        
        testButton.addActionListener(this::testConnection);
        saveButton.addActionListener(this::saveConnection);
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        buttonPanel.add(testButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // 添加组件到对话框
        add(new JLabel("  配置数据库连接", SwingConstants.CENTER), BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // 添加监听器更新URL
        CaretListener caretListener = e -> updateConnectionConfig();
        nameField.addCaretListener(caretListener);
        hostField.addCaretListener(caretListener);
        portField.addCaretListener(caretListener);
        databaseField.addCaretListener(caretListener);
        usernameField.addCaretListener(caretListener);
        passwordField.addCaretListener(caretListener);
    }
    
    /**
     * 根据数据库类型更新UI
     */
    private void updateUIForDatabaseType(String type) {
        boolean isSQLite = "sqlite".equalsIgnoreCase(type);
        
        hostField.setEnabled(!isSQLite);
        portField.setEnabled(!isSQLite);
        
        if (isSQLite) {
            hostField.setText("");
            portField.setText("");
        } else {
            if (hostField.getText().isEmpty()) {
                hostField.setText("localhost");
            }
            if (usernameField.getText().isEmpty()) {
                usernameField.setText("root");
            }
            
            if (DEFAULT_PORTS.containsKey(type.toLowerCase())) {
                portField.setText(String.valueOf(DEFAULT_PORTS.get(type.toLowerCase())));
            }
        }
        
        if (isSQLite) {
            databaseField.setText(System.getProperty("user.home") + "/database.db");
        }else{
            databaseField.setText("");
        }
        
        updateConnectionConfig();
    }
    
    /**
     * 解析URL并更新各个字段
     */
    private void parseUrl() {
        // 检查是否由程序内部更新引起，避免循环调用
        if (isUpdatingFields) return;
        
        try {
            isUpdatingFields = true;
            String url = urlArea.getText().trim();
            
            // 检查是否为有效的JDBC URL
            if (url.startsWith("jdbc:")) {
                // 提取数据库类型
                if (url.startsWith("jdbc:mysql:")) {
                    databaseTypeCombo.setSelectedItem("MySQL");
                } else if (url.startsWith("jdbc:postgresql:")) {
                    databaseTypeCombo.setSelectedItem("PostgreSQL");
                } else if (url.startsWith("jdbc:oracle:")) {
                    databaseTypeCombo.setSelectedItem("Oracle");
                } else if (url.startsWith("jdbc:sqlserver:")) {
                    databaseTypeCombo.setSelectedItem("SQL Server");
                } else if (url.startsWith("jdbc:sqlite:")) {
                    databaseTypeCombo.setSelectedItem("SQLite");
                } else if (url.startsWith("jdbc:redis:") || url.startsWith("redis:")) {
                    databaseTypeCombo.setSelectedItem("Redis");
                }
                
                // 提取主机和端口
                String pattern = "jdbc:.*?://(.*?):(\\d+)";
                java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = r.matcher(url);
                
                if (m.find()) {
                    hostField.setText(m.group(1));
                    portField.setText(m.group(2));
                    
                    // 尝试提取数据库名
                    String dbPattern = "jdbc:.*?://.*?:\\d+/(.*?)(?:\\?|$)";
                    java.util.regex.Pattern dbR = java.util.regex.Pattern.compile(dbPattern);
                    java.util.regex.Matcher dbM = dbR.matcher(url);
                    
                    if (dbM.find() && !dbM.group(1).isEmpty()) {
                        databaseField.setText(dbM.group(1));
                        
                        // 自动设置连接名称（如果为空）
                        if (nameField.getText().isEmpty()) {
                            String dbType = databaseTypeCombo.getSelectedItem().toString();
                            String dbName = dbM.group(1);
                            nameField.setText(dbType + " - " + dbName);
                        }
                    }
                } else if (url.startsWith("jdbc:sqlite:")) {
                    // 对SQLite特殊处理
                    String dbPath = url.substring("jdbc:sqlite:".length());
                    databaseField.setText(dbPath);
                    
                    // 自动设置连接名称（如果为空）
                    if (nameField.getText().isEmpty()) {
                        java.io.File file = new java.io.File(dbPath);
                        nameField.setText("SQLite - " + file.getName());
                    }
                }
            }
        } finally {
            isUpdatingFields = false;
        }
    }
    
    /**
     * 更新连接配置
     */
    private void updateConnectionConfig() {
        if (isUpdatingFields) return;
        
        try {
            isUpdatingFields = true;
            
            connectionConfig.setName(nameField.getText());
            String selectedType = ((String) databaseTypeCombo.getSelectedItem()).toLowerCase();
            connectionConfig.setDatabaseType(selectedType);
            connectionConfig.setHost(hostField.getText());
            
            try {
                connectionConfig.setPort(Integer.parseInt(portField.getText()));
            } catch (NumberFormatException e) {
                // 忽略错误的端口号
            }
            
            connectionConfig.setDatabase(databaseField.getText());
            connectionConfig.setUsername(usernameField.getText());
            connectionConfig.setPassword(new String(passwordField.getPassword()));
            
            connectionConfig.generateUrl();
            urlArea.setText(connectionConfig.getUrl());
        } finally {
            isUpdatingFields = false;
        }
    }
    
    /**
     * 加载连接数据到表单
     */
    private void loadConnectionData() {
        isUpdatingFields = true;
        
        // 如果是编辑连接，则加载已有数据
        if (!isNewConnection && connectionConfig != null) {
            // 设置连接名称
            nameField.setText(connectionConfig.getName());
            
            // 设置数据库类型
            String dbType = connectionConfig.getDatabaseType();
            for (int i = 0; i < databaseTypeCombo.getItemCount(); i++) {
                if (databaseTypeCombo.getItemAt(i).toString().equalsIgnoreCase(dbType)) {
                    databaseTypeCombo.setSelectedIndex(i);
                    break;
                }
            }
            
            // 设置主机和端口
            hostField.setText(connectionConfig.getHost());
            portField.setText(String.valueOf(connectionConfig.getPort()));
            
            // 设置数据库名
            databaseField.setText(connectionConfig.getDatabase());
            
            // 设置用户名和密码
            usernameField.setText(connectionConfig.getUsername());
            passwordField.setText(connectionConfig.getPassword());
            
            // 设置连接URL
            urlArea.setText(connectionConfig.getUrl());
            
            // 根据数据库类型更新UI
            updateUIForDatabaseType(dbType.toLowerCase());
        } else {
            // 新连接，设置默认值
            nameField.setText("");
            databaseTypeCombo.setSelectedIndex(0);
            hostField.setText("localhost");
            portField.setText("3306");
            databaseField.setText("");
            usernameField.setText("root");
            passwordField.setText("");
            urlArea.setText("");
            
            // 更新UI
            updateUIForDatabaseType("mysql");
        }
        
        isUpdatingFields = false;
    }
    
    /**
     * 测试连接
     */
    private void testConnection(ActionEvent e) {
        updateConnectionConfig();
        
        if (connectionConfig.getUrl().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                    "请先完成连接配置",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 使用SwingWorker异步测试连接
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return DatabaseService.testConnection(connectionConfig);
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(ConnectionDialog.this, 
                                "连接成功！",
                                "测试连接", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(ConnectionDialog.this, 
                                "连接失败，请检查连接信息",
                                "测试连接", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    JOptionPane.showMessageDialog(ConnectionDialog.this, 
                            "测试连接时发生错误: " + ex.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * 保存连接
     */
    private void saveConnection(ActionEvent e) {
        updateConnectionConfig();
        
        if (connectionConfig.getName().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                    "请输入连接名称",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (connectionConfig.getUrl().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                    "连接URL无效，请检查配置",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        isConfirmed = true;
        dispose();
    }
    
    /**
     * 是否确认保存
     */
    public boolean isConfirmed() {
        return isConfirmed;
    }
    
    /**
     * 获取连接配置
     */
    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }
    
    // 添加类成员变量
    private boolean isUpdatingFields = false;
} 