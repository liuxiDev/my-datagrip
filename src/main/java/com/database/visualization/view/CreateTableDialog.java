package com.database.visualization.view;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建表对话框
 */
public class CreateTableDialog extends JDialog {
    private JTextField tableNameField;
    private JTable columnsTable;
    private JButton addColumnButton;
    private JButton removeColumnButton;
    private JButton okButton;
    private JButton cancelButton;
    private boolean confirmed = false;
    private String schemaName;
    private List<ColumnDefinition> columns = new ArrayList<>();
    
    public CreateTableDialog(JFrame owner, String schemaName) {
        super(owner, "创建新表", true);
        this.schemaName = schemaName;
        
        initComponents();
        setupListeners();
        
        // 添加默认的第一列
        ColumnDefinition column = new ColumnDefinition("id", "INT", "", false, true);
        columns.add(column);
        ((ColumnsTableModel)columnsTable.getModel()).fireTableDataChanged();
        
        setSize(600, 400);
        setLocationRelativeTo(owner);
    }
    
    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 表名面板
        JPanel tableNamePanel = new JPanel(new BorderLayout());
        tableNamePanel.setBorder(BorderFactory.createTitledBorder("表信息"));
        
        JPanel tableNameInnerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        tableNameInnerPanel.add(new JLabel("Schema:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        tableNameInnerPanel.add(new JLabel(schemaName), gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        tableNameInnerPanel.add(new JLabel("表名:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        tableNameField = new JTextField(20);
        tableNameInnerPanel.add(tableNameField, gbc);
        
        tableNamePanel.add(tableNameInnerPanel, BorderLayout.CENTER);
        
        // 列定义面板
        JPanel columnsPanel = new JPanel(new BorderLayout());
        columnsPanel.setBorder(BorderFactory.createTitledBorder("列定义"));
        
        ColumnsTableModel tableModel = new ColumnsTableModel(columns);
        columnsTable = new JTable(tableModel);
        columnsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 设置列类型的下拉列表
        JComboBox<String> typeComboBox = new JComboBox<>(new String[] {
                "INT", "BIGINT", "TINYINT", "VARCHAR", "CHAR", "TEXT", "DATE", "DATETIME", "TIMESTAMP", "DECIMAL", "FLOAT", "DOUBLE", "BOOLEAN"
        });
        columnsTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(typeComboBox));
        
        JScrollPane scrollPane = new JScrollPane(columnsTable);
        columnsPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 列操作按钮面板
        JPanel columnButtonsPanel = new JPanel();
        addColumnButton = new JButton("添加列");
        removeColumnButton = new JButton("删除列");
        
        columnButtonsPanel.add(addColumnButton);
        columnButtonsPanel.add(removeColumnButton);
        columnsPanel.add(columnButtonsPanel, BorderLayout.SOUTH);
        
        // 对话框按钮面板
        JPanel buttonPanel = new JPanel();
        okButton = new JButton("创建");
        cancelButton = new JButton("取消");
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        // 添加到主面板
        mainPanel.add(tableNamePanel, BorderLayout.NORTH);
        mainPanel.add(columnsPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }
    
    private void setupListeners() {
        addColumnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ColumnDefinition column = new ColumnDefinition("column" + (columns.size() + 1), "VARCHAR", "255", false, false);
                columns.add(column);
                ((ColumnsTableModel)columnsTable.getModel()).fireTableDataChanged();
            }
        });
        
        removeColumnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = columnsTable.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < columns.size()) {
                    columns.remove(selectedRow);
                    ((ColumnsTableModel)columnsTable.getModel()).fireTableDataChanged();
                }
            }
        });
        
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validateInput()) {
                    confirmed = true;
                    dispose();
                }
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });
    }
    
    private boolean validateInput() {
        // 验证表名
        String tableName = tableNameField.getText().trim();
        if (tableName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入表名", "验证错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // 验证列定义
        if (columns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请至少添加一列", "验证错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // 验证是否有主键
        boolean hasPrimaryKey = false;
        for (ColumnDefinition column : columns) {
            if (column.isPrimaryKey()) {
                hasPrimaryKey = true;
                break;
            }
        }
        
        if (!hasPrimaryKey) {
            int answer = JOptionPane.showConfirmDialog(this, 
                    "没有定义主键，确定要继续吗？", 
                    "主键缺失", JOptionPane.YES_NO_OPTION);
            return answer == JOptionPane.YES_OPTION;
        }
        
        return true;
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public String getCreateTableSQL() {
        if (!confirmed) {
            return null;
        }

        StringBuilder sql = new StringBuilder("CREATE TABLE " + schemaName + "." + tableNameField.getText() + " (\n");
        
        for (ColumnDefinition column : columns) {
            sql.append("    ").append(column.getName()).append(" ").append(column.getType());
            
            if (column.getType().equals("VARCHAR") && !column.getLength().isEmpty()) {
                sql.append("(").append(column.getLength()).append(")");
            }
            
            if (column.isNotNull()) {
                sql.append(" NOT NULL");
            }
            
            sql.append(",\n");
        }
        
        // 添加主键约束
        List<String> primaryKeys = getPrimaryKeys();
        if (!primaryKeys.isEmpty()) {
            sql.append("    PRIMARY KEY (");
            for (int i = 0; i < primaryKeys.size(); i++) {
                sql.append(primaryKeys.get(i));
                if (i < primaryKeys.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")\n");
        } else {
            // 如果没有主键，移除最后一个逗号和换行符
            sql.setLength(sql.length() - 2);
            sql.append("\n");
        }
        
        sql.append(");");
        
        return sql.toString();
    }
    
    private List<String> getPrimaryKeys() {
        List<String> primaryKeys = new ArrayList<>();
        for (ColumnDefinition column : columns) {
            if (column.isPrimaryKey()) {
                primaryKeys.add(column.getName());
            }
        }
        return primaryKeys;
    }
    
    /**
     * 列定义类
     */
    private static class ColumnDefinition {
        private String name;
        private String type;
        private String length;
        private boolean nullable;
        private boolean primaryKey;
        
        public ColumnDefinition(String name, String type, String length, boolean nullable, boolean primaryKey) {
            this.name = name;
            this.type = type;
            this.length = length;
            this.nullable = nullable;
            this.primaryKey = primaryKey;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getLength() {
            return length;
        }
        
        public void setLength(String length) {
            this.length = length;
        }
        
        public boolean isNullable() {
            return nullable;
        }
        
        public void setNullable(boolean nullable) {
            this.nullable = nullable;
        }
        
        public boolean isPrimaryKey() {
            return primaryKey;
        }
        
        public void setPrimaryKey(boolean primaryKey) {
            this.primaryKey = primaryKey;
        }
        
        public boolean isNotNull() {
            return !nullable;
        }
        
        public void setNotNull(boolean notNull) {
            this.nullable = !notNull;
        }
    }
    
    /**
     * 列表模型
     */
    private static class ColumnsTableModel extends AbstractTableModel {
        private List<ColumnDefinition> columns;
        private final String[] columnNames = {"列名", "类型", "长度", "允许NULL", "主键"};
        
        public ColumnsTableModel(List<ColumnDefinition> columns) {
            this.columns = columns;
        }
        
        @Override
        public int getRowCount() {
            return columns.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 3: // 允许NULL
                case 4: // 主键
                    return Boolean.class;
                default:
                    return String.class;
            }
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ColumnDefinition column = columns.get(rowIndex);
            
            switch (columnIndex) {
                case 0:
                    return column.getName();
                case 1:
                    return column.getType();
                case 2:
                    return column.getLength();
                case 3:
                    return column.isNullable();
                case 4:
                    return column.isPrimaryKey();
                default:
                    return null;
            }
        }
        
        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            ColumnDefinition column = columns.get(rowIndex);
            
            switch (columnIndex) {
                case 0:
                    column.setName((String)value);
                    break;
                case 1:
                    column.setType((String)value);
                    break;
                case 2:
                    column.setLength((String)value);
                    break;
                case 3:
                    column.setNullable((Boolean)value);
                    break;
                case 4:
                    column.setPrimaryKey((Boolean)value);
                    break;
            }
            
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
} 