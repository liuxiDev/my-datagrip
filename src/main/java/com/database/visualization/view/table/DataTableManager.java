package com.database.visualization.view.table;

import com.database.visualization.controller.DatabaseService;
import com.database.visualization.controller.SQLExecutor;
import com.database.visualization.model.ConnectionConfig;
import com.database.visualization.model.QueryResultTableModel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据表格管理器类，负责表格数据的显示、编辑和保存操作
 */
public class DataTableManager {
    private final JTable resultTable;
    private final QueryResultTableModel resultTableModel;
    private final SQLExecutor sqlExecutor;
    private final JLabel statusLabel;
    private final List<Integer> modifiedRows = new ArrayList<>();
    private boolean isDataEditable = false;

    public DataTableManager(JTable resultTable, QueryResultTableModel resultTableModel, SQLExecutor sqlExecutor, JLabel statusLabel) {
        this.resultTable = resultTable;
        this.resultTableModel = resultTableModel;
        this.sqlExecutor = sqlExecutor;
        this.statusLabel = statusLabel;
    }

    /**
     * 设置表格是否可编辑
     */
    public void setEditable(boolean editable) {
        this.isDataEditable = editable;
        this.resultTableModel.setEditable(editable);
    }

    /**
     * 添加新行
     */
    public void addRow() {
        resultTableModel.addRow();
        int rowCount = resultTableModel.getRowCount();
        if (rowCount > 0) {
            modifiedRows.add(rowCount - 1);
        }
    }

    /**
     * 删除行
     */
    public void deleteRow(int selectedRow) {
        if (selectedRow != -1) {
            modifiedRows.add(selectedRow);
            resultTableModel.markRowAsDeleted(selectedRow);
        }
    }

    /**
     * 保存表格更改
     */
    public void saveTableChanges() {
        ConnectionConfig currentConnection = sqlExecutor.getCurrentConnection();
        String currentTableName = sqlExecutor.getCurrentTableName();

        if (currentConnection == null || currentTableName == null) {
            if (statusLabel != null) {
                statusLabel.setText("没有可保存的更改");
            }
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
            JOptionPane.showMessageDialog(null, "没有检测到需要保存的更改", "保存更改", JOptionPane.INFORMATION_MESSAGE);
            if (statusLabel != null) {
                statusLabel.setText("没有可保存的更改");
            }
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
                    String deleteSql = sqlExecutor.generateDeleteSQL(rowIndex);
                    if (deleteSql != null) {
                        sqlStatements.add(deleteSql);
                    }
                }
                // 检查是否为新增行
                else if (resultTableModel.isNewRow(rowIndex)) {
                    String insertSql = sqlExecutor.generateInsertSQL(rowIndex);
                    if (insertSql != null) {
                        sqlStatements.add(insertSql);
                    }
                }
                // 否则是更新
                else if (resultTableModel.isRowModified(rowIndex)) {
                    String updateSql = sqlExecutor.generateUpdateSQL(rowIndex);
                    if (updateSql != null) {
                        sqlStatements.add(updateSql);
                    }
                }
            }

            if (sqlStatements.isEmpty()) {
                JOptionPane.showMessageDialog(null, "没有生成有效的SQL语句，可能缺少必要的主键或数据", "保存更改", JOptionPane.WARNING_MESSAGE);
                if (statusLabel != null) {
                    statusLabel.setText("没有需要执行的SQL语句");
                }
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
                if (statusLabel != null) {
                    statusLabel.setText("保存成功 " + successCount + "/" + sqlStatements.size() + " 项修改");
                }

                // 清除修改标记
                modifiedRows.clear();
                resultTableModel.clearModifications();

                // 添加成功提示对话框
                JOptionPane.showMessageDialog(null, "已成功保存所有修改，共 " + successCount + " 项", "保存成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // 显示错误信息
                StringBuilder errorMsg = new StringBuilder();
                errorMsg.append("保存部分成功 ").append(successCount).append("/").append(sqlStatements.size()).append(" 项修改");
                if (!errors.isEmpty()) {
                    errorMsg.append(", 错误: ").append(errors.get(0));
                }

                if (statusLabel != null) {
                    statusLabel.setText(errorMsg.toString());
                }

                JOptionPane.showMessageDialog(null, "部分更改未能保存，成功: " + successCount + "/" + sqlStatements.size() +
                        (errors.isEmpty() ? "" : "\n错误: " + String.join("\n", errors)), "保存部分成功", JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (statusLabel != null) {
                statusLabel.setText("保存失败: " + e.getMessage());
            }
            JOptionPane.showMessageDialog(null, "保存更改失败: " + e.getMessage(), "保存失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 获取修改的行列表
     */
    public List<Integer> getModifiedRows() {
        return modifiedRows;
    }

    /**
     * 清空修改标记
     */
    public void clearModifications() {
        modifiedRows.clear();
        resultTableModel.clearModifications();
    }
} 