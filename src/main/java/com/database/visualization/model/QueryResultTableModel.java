package com.database.visualization.model;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询结果表格模型
 */
public class QueryResultTableModel extends AbstractTableModel {
    private List<String> columnNames = new ArrayList<>();
    private List<List<Object>> data = new ArrayList<>();
    
    public QueryResultTableModel() {
    }
    
    public QueryResultTableModel(List<String> columnNames, List<List<Object>> data) {
        this.columnNames = columnNames;
        this.data = data;
    }
    
    @Override
    public int getRowCount() {
        return data.size();
    }
    
    @Override
    public int getColumnCount() {
        return columnNames.size();
    }
    
    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= 0 && rowIndex < data.size() && 
            columnIndex >= 0 && columnIndex < columnNames.size()) {
            List<Object> row = data.get(rowIndex);
            if (columnIndex < row.size()) {
                Object value = row.get(columnIndex);
                return value == null ? "NULL" : value;
            }
        }
        return null;
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
    
    /**
     * 设置查询结果数据
     */
    public void setData(List<String> columnNames, List<List<Object>> data) {
        this.columnNames = columnNames;
        this.data = data;
        fireTableStructureChanged();
    }
    
    /**
     * 清空数据
     */
    public void clear() {
        columnNames.clear();
        data.clear();
        fireTableStructureChanged();
    }
    
    /**
     * 获取所有数据
     */
    public List<List<Object>> getData() {
        return data;
    }
    
    /**
     * 获取所有列名
     */
    public List<String> getColumnNames() {
        return columnNames;
    }
} 