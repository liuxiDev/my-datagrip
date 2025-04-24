package com.database.visualization.controller;

import com.database.visualization.model.ConnectionConfig;
import com.database.visualization.model.QueryResultTableModel;
import com.database.visualization.utils.TableColumnAdjuster;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL执行器类，负责SQL执行和处理查询结果
 */
public class SQLExecutor {
    private final JLabel statusLabel;
    private final JTable resultTable;
    private final QueryResultTableModel resultTableModel;
    private final JTextField pageField;
    private final JLabel totalPagesLabel;
    private ConnectionConfig currentConnection;
    private int currentPage = 1;
    private int pageSize = 500;
    private int totalPages = 1;
    private int totalRecords = 0;
    private String currentTableName;
    private List<String> primaryKeys = new ArrayList<>();

    public SQLExecutor(JLabel statusLabel, JTable resultTable, QueryResultTableModel resultTableModel, JTextField pageField, JLabel totalPagesLabel) {
        this.statusLabel = statusLabel;
        this.resultTable = resultTable;
        this.resultTableModel = resultTableModel;
        this.pageField = pageField;
        this.totalPagesLabel = totalPagesLabel;
    }

    /**
     * 获取当前连接
     */
    public ConnectionConfig getCurrentConnection() {
        return currentConnection;
    }

    /**
     * 设置当前连接
     */
    public void setCurrentConnection(ConnectionConfig connection) {
        this.currentConnection = connection;
    }

    /**
     * 获取当前表名
     */
    public String getCurrentTableName() {
        return currentTableName;
    }

    /**
     * 设置当前表名
     */
    public void setCurrentTableName(String tableName) {
        this.currentTableName = tableName;
    }

    /**
     * 获取当前页
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * 设置当前页
     */
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    /**
     * 获取页大小
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 设置页大小
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 获取主键列表
     */
    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }

    /**
     * 执行SQL语句
     */
    public void executeSQL(String sql) {
        if (currentConnection == null) {
            JOptionPane.showMessageDialog(null, "请先选择一个数据库连接", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 清空分页参数
        currentPage = 1;
        if (pageField != null) {
            pageField.setText("1");
        }

        // 执行SQL
        executeSQLInternal(sql);
    }

    /**
     * 执行当前查询（分页）
     */
    public void executeCurrentQuery(String sql) {
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
        if (statusLabel != null) {
            statusLabel.setText("执行SQL: " + (sql.length() > 50 ? sql.substring(0, 50) + "..." : sql));
        }

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
                            if (statusLabel != null) {
                                statusLabel.setText("查询执行成功，返回 " + totalRecords + " 条记录");
                            }

                            // 更新分页信息
                            updatePaginationInfo();
                        } else {
                            // 添加空值检查，避免空指针异常
                            Object updateCountObj = result.get("updateCount");
                            if (updateCountObj != null) {
                                int updateCount = (int) updateCountObj;
                                if (statusLabel != null) {
                                    statusLabel.setText("更新成功，影响 " + updateCount + " 行");
                                }
                            } else {
                                if (statusLabel != null) {
                                    statusLabel.setText("操作执行成功");
                                }
                            }
                        }
                    } else {
                        String errorMessage = (String) result.get("error");
                        if (statusLabel != null) {
                            statusLabel.setText("SQL执行出错: " + errorMessage);
                        }
                        JOptionPane.showMessageDialog(null, errorMessage, "SQL执行错误", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (statusLabel != null) {
                        statusLabel.setText("SQL执行出错: " + e.getMessage());
                    }
                    JOptionPane.showMessageDialog(null, e.getMessage(), "SQL执行错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    /**
     * 从SQL中提取表名并获取主键
     */
    private void extractTableNameAndFetchPrimaryKeys(String sql) {
        try {
            // 简单的表名提取逻辑
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

    /**
     * 获取表的主键信息
     */
    public void fetchTablePrimaryKeys(String tableName) {
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                try {
                    return DatabaseService.getPrimaryKeys(currentConnection, tableName);
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ArrayList<>();
                }
            }

            @Override
            protected void done() {
                try {
                    primaryKeys = get();
                } catch (Exception e) {
                    e.printStackTrace();
                    primaryKeys = new ArrayList<>();
                }
            }
        };

        worker.execute();
    }

    /**
     * 更新分页信息
     */
    private void updatePaginationInfo() {
        if (totalPagesLabel != null) {
            totalPagesLabel.setText("共 " + totalPages + " 页, 共 " + totalRecords + " 条记录");
        }
    }

    /**
     * 生成更新SQL语句
     */
    public String generateUpdateSQL(int row) {
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
    public String generateInsertSQL(int row) {
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
    public String generateDeleteSQL(int row) {
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
} 