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
    private List<List<Object>> originalData = new ArrayList<>();
    private boolean editable = false;
    private int originalRowCount = 0;
    private List<Integer> modifiedRows = new ArrayList<>();
    
    public QueryResultTableModel() {
        this(false);
    }
    
    public QueryResultTableModel(boolean editable) {
        this.editable = editable;
    }
    
    /**
     * 设置数据
     */
    public void setData(List<String> columnNames, List<List<Object>> data) {
        this.columnNames = columnNames;
        this.data = new ArrayList<>(data);
        
        // 保存原始数据用于比较
        this.originalData = new ArrayList<>();
        for (List<Object> row : data) {
            this.originalData.add(new ArrayList<>(row));
        }
        
        this.originalRowCount = data.size();
        fireTableStructureChanged();
    }
    
    /**
     * 清空数据
     */
    public void clear() {
        columnNames.clear();
        data.clear();
        originalData.clear();
        originalRowCount = 0;
        fireTableStructureChanged();
    }
    
    /**
     * 设置是否可编辑
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        fireTableDataChanged();
    }
    
    /**
     * 添加空行
     */
    public void addEmptyRow() {
        if (columnNames.isEmpty()) return;
        
        List<Object> emptyRow = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            emptyRow.add(null);
        }
        
        data.add(emptyRow);
        fireTableRowsInserted(data.size() - 1, data.size() - 1);
    }
    
    /**
     * 删除行
     */
    public void removeRow(int row) {
        if (row >= 0 && row < data.size()) {
            data.remove(row);
            if (row < originalData.size()) {
                originalData.remove(row);
                originalRowCount--;
            }
            fireTableRowsDeleted(row, row);
        }
    }
    
    /**
     * 获取行数据
     */
    public List<Object> getRowData(int row) {
        if (row >= 0 && row < data.size()) {
            return data.get(row);
        }
        return null;
    }
    
    /**
     * 获取原始行数据
     */
    public List<Object> getOriginalRowData(int row) {
        if (row >= 0 && row < originalData.size()) {
            return originalData.get(row);
        }
        return null;
    }
    
    /**
     * 获取原始行数
     */
    public int getOriginalRowCount() {
        return originalRowCount;
    }
    
    /**
     * 获取列名列表
     */
    public List<String> getColumnNames() {
        return columnNames;
    }
    
    /**
     * 判断某一行是否被修改
     * @param row 行索引
     * @return 是否被修改
     */
    public boolean isRowModified(int row) {
        if (row >= data.size() || row >= originalData.size()) {
            return false;
        }
        
        List<Object> currentRow = data.get(row);
        List<Object> originalRow = originalData.get(row);
        
        if (currentRow.size() != originalRow.size()) {
            return true;
        }
        
        for (int i = 0; i < currentRow.size(); i++) {
            Object currentValue = currentRow.get(i);
            Object originalValue = originalRow.get(i);
            
            if ((currentValue == null && originalValue != null) || 
                (currentValue != null && !currentValue.equals(originalValue))) {
                return true;
            }
        }
        
        return modifiedRows.contains(row);
    }
    
    /**
     * 重置修改状态
     */
    public void resetModifiedState() {
        modifiedRows.clear();
        // 更新原始数据
        originalData.clear();
        for (List<Object> row : data) {
            originalData.add(new ArrayList<>(row));
        }
        originalRowCount = data.size();
        fireTableDataChanged();
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
    public String getColumnName(int columnIndex) {
        if (columnIndex >= 0 && columnIndex < columnNames.size()) {
            return columnNames.get(columnIndex);
        }
        return "";
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (data.isEmpty()) {
            return Object.class;
        }
        
        Object value = getValueAt(0, columnIndex);
        return (value != null) ? value.getClass() : Object.class;
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= 0 && rowIndex < data.size() &&
            columnIndex >= 0 && columnIndex < columnNames.size()) {
            List<Object> row = data.get(rowIndex);
            if (columnIndex < row.size()) {
                return row.get(columnIndex);
            }
        }
        return null;
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return editable;
    }
    
    @Override
    public void setValueAt(Object value, int row, int col) {
        if (editable && row < data.size() && col < columnNames.size()) {
            data.get(row).set(col, value);
            fireTableCellUpdated(row, col);
            
            // 记录修改的行
            if (row < originalRowCount && !modifiedRows.contains(row)) {
                modifiedRows.add(row);
            }
        }
    }
} 