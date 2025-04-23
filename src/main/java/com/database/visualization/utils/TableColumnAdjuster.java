package com.database.visualization.utils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * 表格列宽自动调整工具
 */
public class TableColumnAdjuster implements PropertyChangeListener {
    private JTable table;
    private int spacing;
    private boolean isAdjusting;
    private boolean dynamicAdjustment;
    private final int maxWidth = 300; // 最大列宽
    
    public TableColumnAdjuster(JTable table) {
        this(table, 6);
    }
    
    public TableColumnAdjuster(JTable table, int spacing) {
        this.table = table;
        this.spacing = spacing;
        this.isAdjusting = false;
        this.dynamicAdjustment = false;
        installListeners();
    }
    
    public void adjustColumns() {
        TableColumnModel tcm = table.getColumnModel();
        
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            adjustColumn(i);
        }
    }
    
    public void adjustColumn(final int column) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        
        if (!tableColumn.getResizable()) return;
        
        int columnHeaderWidth = getColumnHeaderWidth(column);
        int columnDataWidth = getColumnDataWidth(column);
        int preferredWidth = Math.max(columnHeaderWidth, columnDataWidth);
        
        // 限制最大宽度
        preferredWidth = Math.min(preferredWidth, maxWidth);
        
        updateTableColumn(column, preferredWidth);
    }
    
    private int getColumnHeaderWidth(int column) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        Object value = tableColumn.getHeaderValue();
        TableCellRenderer renderer = tableColumn.getHeaderRenderer();
        
        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }
        
        Component c = renderer.getTableCellRendererComponent(table, value, false, false, -1, column);
        return c.getPreferredSize().width + spacing;
    }
    
    private int getColumnDataWidth(int column) {
        int preferredWidth = 0;
        
        for (int row = 0; row < table.getRowCount(); row++) {
            preferredWidth = Math.max(preferredWidth, getCellDataWidth(row, column));
            
            // 计算列最大宽度时，仅检查前50行
            if (row > 50) break;
        }
        
        return preferredWidth;
    }
    
    private int getCellDataWidth(int row, int column) {
        TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
        Component c = table.prepareRenderer(cellRenderer, row, column);
        return c.getPreferredSize().width + table.getIntercellSpacing().width;
    }
    
    private void updateTableColumn(int column, int width) {
        final TableColumn tableColumn = table.getColumnModel().getColumn(column);
        
        if (!tableColumn.getResizable()) return;
        
        isAdjusting = true;
        tableColumn.setPreferredWidth(width);
        isAdjusting = false;
    }
    
    private void installListeners() {
        table.addPropertyChangeListener(this);
    }
    
    public void propertyChange(PropertyChangeEvent e) {
        // 当表格模型变化时调整列宽
        if ("model".equals(e.getPropertyName())) {
            if (!isAdjusting) adjustColumns();
        }
    }
    
    // 设置动态调整
    public void setDynamicAdjustment(boolean dynamic) {
        this.dynamicAdjustment = dynamic;
    }
} 