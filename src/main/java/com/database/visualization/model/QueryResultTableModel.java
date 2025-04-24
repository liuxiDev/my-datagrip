package com.database.visualization.model;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

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
     * 设置数据（List<Map<String, Object>>数据类型）
     * @param dataList 数据列表
     * @param columnList 列名列表
     */
    public void setDataFromMap(List<Map<String, Object>> dataList, List<String> columnList) {
        this.columnNames = columnList;
        this.data = new ArrayList<>();
        
        // 转换Map数据为列表数据
        for (Map<String, Object> row : dataList) {
            List<Object> rowData = new ArrayList<>();
            for (String column : columnList) {
                rowData.add(row.get(column));
            }
            this.data.add(rowData);
        }
        
        this.originalData = new ArrayList<>(this.data);
        this.modifiedRows = new ArrayList<>();
        
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
    
    /**
     * 添加新行
     */
    public void addRow() {
        List<Object> newRow = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            newRow.add(null);
        }
        data.add(newRow);
        // 不添加到原始数据，这是新行标记
        
        fireTableRowsInserted(data.size() - 1, data.size() - 1);
    }
    
    /**
     * 标记行为已删除
     * @param row 行索引
     */
    public void markRowAsDeleted(int row) {
        // 标记删除状态，实际上可以使用一个额外的列表来记录
        // 这里仅做示例，实际应用中可以设计一个更复杂的状态管理系统
        data.get(row).clear(); // 清空行数据作为标记
        fireTableRowsUpdated(row, row);
    }
    
    /**
     * 检查行是否被标记为删除
     * @param row 行索引
     * @return 是否被标记为删除
     */
    public boolean isRowDeleted(Integer row) {
        if (row >= 0 && row < data.size()) {
            List<Object> rowData = data.get(row);
            // 检查是否为空列表（我们用空列表标记删除）
            return rowData.isEmpty();
        }
        return false;
    }
    
    /**
     * 检查是否为新行
     * @param row 行索引
     * @return 是否为新行
     */
    public boolean isNewRow(Integer row) {
        return row >= originalData.size();
    }
    
    /**
     * 清除所有修改标记
     */
    public void clearModifications() {
        // 清除修改记录
        modifiedRows.clear();
        
        // 重置数据为原始数据
        data.clear();
        for (List<Object> row : originalData) {
            data.add(new ArrayList<>(row));
        }
        
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
            // 获取当前值进行比较
            Object currentValue = data.get(row).get(col);
            
            // 值相同则不需要标记修改
            if ((currentValue == null && value == null) || 
                (currentValue != null && currentValue.equals(value))) {
                return;
            }
            
            // 设置新值
            data.get(row).set(col, value);
            fireTableCellUpdated(row, col);
            
            // 记录修改的行
            if (row < originalRowCount && !modifiedRows.contains(row)) {
                modifiedRows.add(row);
            }
        }
    }
} 