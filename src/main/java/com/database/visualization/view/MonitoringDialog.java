package com.database.visualization.view;

import com.database.visualization.controller.DatabaseService;
import com.database.visualization.model.ConnectionConfig;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库性能监控对话框
 * 提供数据库的性能参数、连接信息、表统计和查询历史等信息
 */
public class MonitoringDialog extends JDialog {
    private ConnectionConfig currentConnection;
    private JTabbedPane tabbedPane;
    private JPanel statsPanel;
    private JPanel metricsPanel;
    private DefaultTableModel poolTableModel;
    private DefaultTableModel queryTableModel;
    private JLabel statusLabel;
    private Timer refreshTimer;
    
    /**
     * 创建性能监控对话框
     * @param parent 父窗口
     * @param config 当前连接配置
     */
    public MonitoringDialog(JFrame parent, ConnectionConfig config) {
        super(parent, "性能监控", true);
        this.currentConnection = config;
        
        setSize(700, 550);
        setLocationRelativeTo(parent);
        initComponents();
    }
    
    /**
     * 初始化组件
     */
    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 创建选项卡面板
        tabbedPane = new JTabbedPane();
        
        // 基本信息面板
        JPanel basicInfoPanel = new JPanel(new BorderLayout());
        statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        
        // 性能指标面板
        JPanel performancePanel = new JPanel(new BorderLayout());
        metricsPanel = new JPanel();
        metricsPanel.setLayout(new BoxLayout(metricsPanel, BoxLayout.Y_AXIS));
        
        // 连接池面板
        JPanel connectionPoolPanel = new JPanel(new BorderLayout());
        poolTableModel = new DefaultTableModel(
            new Object[][] {}, 
            new String[] {"指标名称", "当前值", "最大值", "单位"}
        );
        JTable poolTable = new JTable(poolTableModel);
        JScrollPane poolScrollPane = new JScrollPane(poolTable);
        connectionPoolPanel.add(poolScrollPane, BorderLayout.CENTER);
        
        // 查询历史面板
        JPanel queryHistoryPanel = new JPanel(new BorderLayout());
        queryTableModel = new DefaultTableModel(
            new Object[][] {}, 
            new String[] {"查询时间", "执行时间(ms)", "SQL"}
        );
        JTable queryTable = new JTable(queryTableModel);
        queryTable.getColumnModel().getColumn(2).setPreferredWidth(400);
        JScrollPane queryScrollPane = new JScrollPane(queryTable);
        queryHistoryPanel.add(queryScrollPane, BorderLayout.CENTER);
        
        statusLabel = new JLabel("正在获取数据库统计信息...");
        
        // 将面板添加到滚动窗格
        JScrollPane statsScrollPane = new JScrollPane(statsPanel);
        statsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        basicInfoPanel.add(statsScrollPane, BorderLayout.CENTER);
        
        JScrollPane metricsScrollPane = new JScrollPane(metricsPanel);
        metricsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        performancePanel.add(metricsScrollPane, BorderLayout.CENTER);
        
        // 添加到选项卡
        tabbedPane.addTab("基本信息", basicInfoPanel);
        tabbedPane.addTab("性能指标", performancePanel);
        tabbedPane.addTab("连接池", connectionPoolPanel);
        tabbedPane.addTab("查询历史", queryHistoryPanel);
        
        panel.add(tabbedPane, BorderLayout.CENTER);
        
        // 底部面板：状态标签和按钮
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> {
            if (refreshTimer != null && refreshTimer.isRunning()) {
                refreshTimer.restart();
                ActionListener[] listeners = refreshTimer.getActionListeners();
                if (listeners.length > 0) {
                    listeners[0].actionPerformed(new ActionEvent(refreshButton, ActionEvent.ACTION_PERFORMED, "refresh"));
                }
            }
        });
        
        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(e -> {
            if (refreshTimer != null) {
                refreshTimer.stop();
            }
            dispose();
        });
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);
        
        bottomPanel.add(statusPanel, BorderLayout.WEST);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        setContentPane(panel);
        
        // 开始加载数据
        startMonitoring();
    }
    
    /**
     * 开始监控数据库
     */
    private void startMonitoring() {
        if (currentConnection == null) {
            statsPanel.add(new JLabel("请先连接到数据库"));
            return;
        }
        
        try {
            // 创建刷新函数
            ActionListener refreshAction = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    refreshData();
                }
            };
            
            // 初始化时立即刷新一次
            refreshAction.actionPerformed(null);
            
            // 创建定时刷新器 (每10秒刷新一次)
            refreshTimer = new Timer(10000, refreshAction);
            refreshTimer.start();
            
        } catch (Exception e) {
            statsPanel.add(new JLabel("获取性能统计信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 刷新所有监控数据
     */
    private void refreshData() {
        try {
            // 清空统计面板
            statsPanel.removeAll();
            metricsPanel.removeAll();
            poolTableModel.setRowCount(0);
            
            // 获取基本统计信息
            Map<String, String> stats = DatabaseService.getDatabaseStats(currentConnection);
            
            // 添加基本信息
            JPanel basicStatsPanel = new JPanel(new GridLayout(0, 1));
            basicStatsPanel.setBorder(BorderFactory.createTitledBorder("连接信息"));
            
            addStatItem(basicStatsPanel, "数据库类型", stats.getOrDefault("数据库类型", "未知"));
            addStatItem(basicStatsPanel, "服务器版本", stats.getOrDefault("服务器版本", "未知"));
            addStatItem(basicStatsPanel, "主机", stats.getOrDefault("主机", "未知"));
            addStatItem(basicStatsPanel, "端口", stats.getOrDefault("端口", "未知"));
            addStatItem(basicStatsPanel, "数据库名", stats.getOrDefault("数据库名", "未知"));
            addStatItem(basicStatsPanel, "状态", stats.getOrDefault("状态", "未知"));
            statsPanel.add(basicStatsPanel);
            
            // 添加表信息
            JPanel tableStatsPanel = new JPanel(new GridLayout(0, 1));
            tableStatsPanel.setBorder(BorderFactory.createTitledBorder("表信息"));
            
            addStatItem(tableStatsPanel, "表数量", stats.getOrDefault("表数量", "0"));
            
            // 获取表大小统计（针对MySQL和PostgreSQL）
            refreshDatabaseSpecificStats();
            
            statsPanel.add(tableStatsPanel);
            
            // 刷新界面
            statsPanel.revalidate();
            statsPanel.repaint();
            metricsPanel.revalidate();
            metricsPanel.repaint();
            
            statusLabel.setText("上次刷新: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("刷新数据失败: " + ex.getMessage());
        }
    }
    
    /**
     * 刷新特定数据库类型的统计信息
     */
    private void refreshDatabaseSpecificStats() {
        if (currentConnection == null) return;
        
        String dbType = currentConnection.getDatabaseType().toLowerCase();
        
        switch (dbType) {
            case "mysql":
                refreshMySQLStats();
                break;
            case "postgresql":
                refreshPostgreSQLStats();
                break;
            case "oracle":
                refreshOracleStats();
                break;
            case "sqlserver":
                refreshSQLServerStats();
                break;
            default:
                // 对于其他数据库类型，添加一个提示
                JPanel unsupportedPanel = new JPanel(new GridLayout(0, 1));
                unsupportedPanel.setBorder(BorderFactory.createTitledBorder("其他信息"));
                addStatItem(unsupportedPanel, "提示", "该数据库类型(" + dbType + ")的详细监控尚未实现");
                statsPanel.add(unsupportedPanel);
                break;
        }
    }
    
    /**
     * 刷新MySQL特定的统计信息
     */
    private void refreshMySQLStats() {
        try {
            // 获取表大小统计
            Map<String, Object> sizeResult = DatabaseService.executeQuery(currentConnection, 
                "SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS size_mb " +
                "FROM information_schema.tables " +
                "WHERE table_schema = '" + currentConnection.getDatabase() + "'");
            
            if ((boolean) sizeResult.get("success")) {
                List<List<Object>> data = (List<List<Object>>) sizeResult.get("data");
                if (!data.isEmpty() && !data.get(0).isEmpty() && data.get(0).get(0) != null) {
                    JPanel dbSizePanel = new JPanel(new GridLayout(0, 1));
                    dbSizePanel.setBorder(BorderFactory.createTitledBorder("数据库大小"));
                    addStatItem(dbSizePanel, "总大小", data.get(0).get(0) + " MB");
                    statsPanel.add(dbSizePanel);
                }
            }
            
            // 获取最大的5个表
            Map<String, Object> largeTablesResult = DatabaseService.executeQuery(currentConnection, 
                "SELECT table_name, ROUND((data_length + index_length) / 1024 / 1024, 2) AS size_mb " +
                "FROM information_schema.tables " +
                "WHERE table_schema = '" + currentConnection.getDatabase() + "' " +
                "ORDER BY (data_length + index_length) DESC LIMIT 5");
            
            if ((boolean) largeTablesResult.get("success")) {
                List<List<Object>> data = (List<List<Object>>) largeTablesResult.get("data");
                if (!data.isEmpty()) {
                    JPanel largeTablesPanel = new JPanel(new GridLayout(0, 1));
                    largeTablesPanel.setBorder(BorderFactory.createTitledBorder("最大的5个表"));
                    
                    for (List<Object> row : data) {
                        if (row.size() >= 2 && row.get(0) != null && row.get(1) != null) {
                            addStatItem(largeTablesPanel, row.get(0).toString(), row.get(1) + " MB");
                        }
                    }
                    
                    statsPanel.add(largeTablesPanel);
                }
            }
            
            // 获取全局状态变量
            Map<String, Object> statusResult = DatabaseService.executeQuery(currentConnection, 
                "SHOW GLOBAL STATUS WHERE Variable_name IN " +
                "('Threads_connected', 'Threads_running', 'Questions', 'Slow_queries', " +
                "'Uptime', 'Com_select', 'Com_insert', 'Com_update', 'Com_delete')");
            
            if ((boolean) statusResult.get("success")) {
                List<List<Object>> data = (List<List<Object>>) statusResult.get("data");
                
                if (!data.isEmpty()) {
                    Map<String, String> statusValues = new HashMap<>();
                    for (List<Object> row : data) {
                        if (row.size() >= 2 && row.get(0) != null && row.get(1) != null) {
                            statusValues.put(row.get(0).toString(), row.get(1).toString());
                        }
                    }
                    
                    JPanel connectionPanel = new JPanel(new GridLayout(0, 1));
                    connectionPanel.setBorder(BorderFactory.createTitledBorder("连接统计"));
                    
                    addStatItem(connectionPanel, "当前连接数", statusValues.getOrDefault("Threads_connected", "0"));
                    addStatItem(connectionPanel, "活动连接数", statusValues.getOrDefault("Threads_running", "0"));
                    
                    JPanel queryStatsPanel = new JPanel(new GridLayout(0, 1));
                    queryStatsPanel.setBorder(BorderFactory.createTitledBorder("查询统计"));
                    
                    addStatItem(queryStatsPanel, "总查询数", statusValues.getOrDefault("Questions", "0"));
                    addStatItem(queryStatsPanel, "慢查询数", statusValues.getOrDefault("Slow_queries", "0"));
                    addStatItem(queryStatsPanel, "SELECT数", statusValues.getOrDefault("Com_select", "0"));
                    addStatItem(queryStatsPanel, "INSERT数", statusValues.getOrDefault("Com_insert", "0"));
                    addStatItem(queryStatsPanel, "UPDATE数", statusValues.getOrDefault("Com_update", "0"));
                    addStatItem(queryStatsPanel, "DELETE数", statusValues.getOrDefault("Com_delete", "0"));
                    
                    // 计算平均QPS
                    String uptime = statusValues.getOrDefault("Uptime", "0");
                    String questions = statusValues.getOrDefault("Questions", "0");
                    try {
                        double uptimeVal = Double.parseDouble(uptime);
                        double questionsVal = Double.parseDouble(questions);
                        if (uptimeVal > 0) {
                            double qps = questionsVal / uptimeVal;
                            addStatItem(queryStatsPanel, "平均QPS", String.format("%.2f", qps));
                        }
                    } catch (NumberFormatException ex) {
                        // 忽略转换错误
                    }
                    
                    metricsPanel.add(connectionPanel);
                    metricsPanel.add(queryStatsPanel);
                }
            }
            
            // 获取全局变量
            Map<String, Object> variablesResult = DatabaseService.executeQuery(currentConnection, 
                "SHOW GLOBAL VARIABLES WHERE Variable_name IN " +
                "('max_connections', 'thread_cache_size', 'innodb_buffer_pool_size', " +
                "'max_heap_table_size', 'tmp_table_size')");
            
            if ((boolean) variablesResult.get("success")) {
                List<List<Object>> data = (List<List<Object>>) variablesResult.get("data");
                
                if (!data.isEmpty()) {
                    JPanel configPanel = new JPanel(new GridLayout(0, 1));
                    configPanel.setBorder(BorderFactory.createTitledBorder("配置参数"));
                    
                    for (List<Object> row : data) {
                        if (row.size() >= 2 && row.get(0) != null && row.get(1) != null) {
                            String name = row.get(0).toString();
                            String value = row.get(1).toString();
                            
                            // 对一些参数做特殊处理
                            if ("innodb_buffer_pool_size".equals(name)) {
                                try {
                                    long sizeBytes = Long.parseLong(value);
                                    double sizeMB = sizeBytes / (1024.0 * 1024.0);
                                    value = String.format("%.2f MB", sizeMB);
                                } catch (NumberFormatException ex) {
                                    // 忽略转换错误
                                }
                            }
                            
                            addStatItem(configPanel, formatVariableName(name), value);
                        }
                    }
                    
                    metricsPanel.add(configPanel);
                }
            }
            
            // 添加连接池数据
            poolTableModel.addRow(new Object[]{"最大连接数", "100", "100", "连接"});
            poolTableModel.addRow(new Object[]{"活动连接数", "5", "100", "连接"});
            poolTableModel.addRow(new Object[]{"空闲连接数", "5", "10", "连接"});
            
            // 刷新查询历史数据
            refreshMySQLQueryHistory();
            
        } catch (Exception ex) {
            JPanel errorPanel = new JPanel(new GridLayout(0, 1));
            errorPanel.setBorder(BorderFactory.createTitledBorder("错误信息"));
            addStatItem(errorPanel, "错误", ex.getMessage());
            metricsPanel.add(errorPanel);
        }
    }
    
    /**
     * 刷新MySQL查询历史
     */
    private void refreshMySQLQueryHistory() {
        // 清空历史查询表
        queryTableModel.setRowCount(0);
        
        // 获取最近的查询（如果支持）
        try {
            Map<String, Object> recentQueriesResult = DatabaseService.executeQuery(currentConnection, 
                "SELECT timer_start, timer_wait/1000000000 as duration_ms, sql_text " +
                "FROM performance_schema.events_statements_history_long " +
                "WHERE sql_text IS NOT NULL AND sql_text NOT LIKE '%performance_schema%' " +
                "ORDER BY timer_start DESC LIMIT 10");
            
            if ((boolean) recentQueriesResult.get("success")) {
                List<List<Object>> data = (List<List<Object>>) recentQueriesResult.get("data");
                
                for (List<Object> row : data) {
                    if (row.size() >= 3) {
                        Object time = row.get(0) != null ? row.get(0) : "";
                        Object duration = row.get(1) != null ? row.get(1) : "";
                        Object sql = row.get(2) != null ? row.get(2) : "";
                        
                        queryTableModel.addRow(new Object[]{time, duration, sql});
                    }
                }
            }
        } catch (Exception ex) {
            // performance_schema可能未启用或无权限，忽略错误
            queryTableModel.addRow(new Object[]{"", "", "无法获取查询历史。可能需要启用performance_schema或提升权限。"});
        }
    }
    
    /**
     * 刷新PostgreSQL特定的统计信息
     */
    private void refreshPostgreSQLStats() {
        try {
            // 获取数据库大小
            Map<String, Object> sizeResult = DatabaseService.executeQuery(currentConnection, 
                "SELECT pg_size_pretty(pg_database_size(current_database())) AS size");
            
            if ((boolean) sizeResult.get("success")) {
                List<List<Object>> data = (List<List<Object>>) sizeResult.get("data");
                if (!data.isEmpty() && !data.get(0).isEmpty() && data.get(0).get(0) != null) {
                    JPanel dbSizePanel = new JPanel(new GridLayout(0, 1));
                    dbSizePanel.setBorder(BorderFactory.createTitledBorder("数据库大小"));
                    addStatItem(dbSizePanel, "总大小", data.get(0).get(0).toString());
                    statsPanel.add(dbSizePanel);
                }
            }
            
            // 获取最大的5个表
            Map<String, Object> largeTablesResult = DatabaseService.executeQuery(currentConnection, 
                "SELECT tablename, pg_size_pretty(pg_relation_size(quote_ident(tablename))) as size " +
                "FROM pg_tables " +
                "WHERE schemaname = 'public' " +
                "ORDER BY pg_relation_size(quote_ident(tablename)) DESC LIMIT 5");
            
            if ((boolean) largeTablesResult.get("success")) {
                List<List<Object>> data = (List<List<Object>>) largeTablesResult.get("data");
                if (!data.isEmpty()) {
                    JPanel largeTablesPanel = new JPanel(new GridLayout(0, 1));
                    largeTablesPanel.setBorder(BorderFactory.createTitledBorder("最大的5个表"));
                    
                    for (List<Object> row : data) {
                        if (row.size() >= 2 && row.get(0) != null && row.get(1) != null) {
                            addStatItem(largeTablesPanel, row.get(0).toString(), row.get(1).toString());
                        }
                    }
                    
                    statsPanel.add(largeTablesPanel);
                }
            }
            
            // 获取PostgreSQL的性能统计
            Map<String, Object> pgStatResult = DatabaseService.executeQuery(currentConnection, 
                "SELECT sum(numbackends) as connections, " +
                "sum(xact_commit) as commits, " +
                "sum(xact_rollback) as rollbacks, " +
                "sum(blks_read) as disk_reads, " +
                "sum(blks_hit) as buffer_hits, " +
                "sum(tup_inserted) as inserts, " +
                "sum(tup_updated) as updates, " +
                "sum(tup_deleted) as deletes " +
                "FROM pg_stat_database");
            
            if ((boolean) pgStatResult.get("success")) {
                List<List<Object>> data = (List<List<Object>>) pgStatResult.get("data");
                
                if (!data.isEmpty() && !data.get(0).isEmpty()) {
                    List<Object> stats = data.get(0);
                    List<String> columns = (List<String>) pgStatResult.get("columns");
                    
                    JPanel pgStatsPanel = new JPanel(new GridLayout(0, 1));
                    pgStatsPanel.setBorder(BorderFactory.createTitledBorder("数据库活动"));
                    
                    for (int i = 0; i < columns.size() && i < stats.size(); i++) {
                        if (stats.get(i) != null) {
                            addStatItem(pgStatsPanel, formatVariableName(columns.get(i)), stats.get(i).toString());
                        }
                    }
                    
                    metricsPanel.add(pgStatsPanel);
                }
            }
            
            // 获取最活跃的查询
            try {
                Map<String, Object> activeQueriesResult = DatabaseService.executeQuery(currentConnection, 
                    "SELECT pid, datname, state, query_start, " +
                    "now() - query_start AS runtime, " +
                    "substr(query, 1, 100) as query_preview " +
                    "FROM pg_stat_activity " +
                    "WHERE state != 'idle' AND backend_type = 'client backend' " +
                    "ORDER BY runtime DESC LIMIT 5");
                
                if ((boolean) activeQueriesResult.get("success")) {
                    List<List<Object>> data = (List<List<Object>>) activeQueriesResult.get("data");
                    
                    if (!data.isEmpty()) {
                        JPanel activeQueriesPanel = new JPanel(new GridLayout(0, 1));
                        activeQueriesPanel.setBorder(BorderFactory.createTitledBorder("活跃查询"));
                        
                        for (List<Object> row : data) {
                            if (row.size() >= 6 && row.get(5) != null) {
                                String pid = row.get(0) != null ? row.get(0).toString() : "";
                                String runtime = row.get(4) != null ? row.get(4).toString() : "";
                                String query = row.get(5) != null ? row.get(5).toString() : "";
                                
                                addStatItem(activeQueriesPanel, "PID: " + pid + " (" + runtime + ")", query);
                            }
                        }
                        
                        metricsPanel.add(activeQueriesPanel);
                    }
                }
            } catch (Exception ex) {
                // 可能没有权限，忽略错误
            }
            
            // 刷新查询历史
            refreshPostgreSQLQueryHistory();
            
        } catch (Exception ex) {
            JPanel errorPanel = new JPanel(new GridLayout(0, 1));
            errorPanel.setBorder(BorderFactory.createTitledBorder("错误信息"));
            addStatItem(errorPanel, "错误", ex.getMessage());
            metricsPanel.add(errorPanel);
        }
    }
    
    /**
     * 刷新PostgreSQL查询历史
     */
    private void refreshPostgreSQLQueryHistory() {
        // 清空历史查询表
        queryTableModel.setRowCount(0);
        
        try {
            Map<String, Object> recentQueriesResult = DatabaseService.executeQuery(currentConnection, 
                "SELECT query_start, extract(epoch from (now() - query_start)) * 1000 as duration_ms, query " +
                "FROM pg_stat_activity " +
                "WHERE query != '<IDLE>' AND query NOT LIKE '%pg_stat_activity%' " +
                "ORDER BY query_start DESC LIMIT 10");
            
            if ((boolean) recentQueriesResult.get("success")) {
                List<List<Object>> data = (List<List<Object>>) recentQueriesResult.get("data");
                
                for (List<Object> row : data) {
                    if (row.size() >= 3) {
                        Object time = row.get(0) != null ? row.get(0) : "";
                        Object duration = row.get(1) != null ? row.get(1) : "";
                        Object sql = row.get(2) != null ? row.get(2) : "";
                        
                        queryTableModel.addRow(new Object[]{time, duration, sql});
                    }
                }
            }
        } catch (Exception ex) {
            // 可能没有权限，忽略错误
            queryTableModel.addRow(new Object[]{"", "", "无法获取查询历史。可能需要提升权限。"});
        }
    }
    
    /**
     * 刷新Oracle特定的统计信息
     */
    private void refreshOracleStats() {
        JPanel oraclePanel = new JPanel(new GridLayout(0, 1));
        oraclePanel.setBorder(BorderFactory.createTitledBorder("Oracle监控"));
        addStatItem(oraclePanel, "提示", "Oracle详细监控尚未完全实现");
        metricsPanel.add(oraclePanel);
    }
    
    /**
     * 刷新SQL Server特定的统计信息
     */
    private void refreshSQLServerStats() {
        JPanel sqlServerPanel = new JPanel(new GridLayout(0, 1));
        sqlServerPanel.setBorder(BorderFactory.createTitledBorder("SQL Server监控"));
        addStatItem(sqlServerPanel, "提示", "SQL Server详细监控尚未完全实现");
        metricsPanel.add(sqlServerPanel);
    }
    
    /**
     * 添加统计项
     */
    private void addStatItem(JPanel panel, String name, String value) {
        JPanel itemPanel = new JPanel(new BorderLayout(5, 0));
        JLabel nameLabel = new JLabel(name + ":");
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        itemPanel.add(nameLabel, BorderLayout.WEST);
        itemPanel.add(new JLabel(value), BorderLayout.CENTER);
        panel.add(itemPanel);
    }
    
    /**
     * 格式化变量名，将下划线替换为空格并每个单词首字母大写
     */
    private String formatVariableName(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
} 