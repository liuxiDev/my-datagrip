package com.database.visualization.view;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
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
        super(owner, "创建表", true);
        this.schemaName = schemaName;
        
        initComponents();
        setupListeners();
        
        // 添加默认的第一列
        ColumnDefinition column = new ColumnDefinition("id", "INT", "", true, true);
        columns.add(column);
        ((ColumnsTableModel)columnsTable.getModel()).fireTableDataChanged();
        
        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setModal(true);
    }
    
    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 表名面板
        JPanel tableNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tableNamePanel.setBorder(BorderFactory.createTitledBorder("表信息"));
        
        JLabel tableNameLabel = new JLabel("表名:");
        tableNameField = new JTextField(20);
        tableNamePanel.add(tableNameLabel);
        tableNamePanel.add(tableNameField);
        
        // 列定义面板
        JPanel columnsPanel = new JPanel(new BorderLayout());
        columnsPanel.setBorder(BorderFactory.createTitledBorder("列定义"));
        
        ColumnsTableModel tableModel = new ColumnsTableModel(columns);
        columnsTable = new JTable(tableModel);
        columnsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 设置表格列宽
        TableColumnModel columnModel = columnsTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(100); // 名称
        columnModel.getColumn(1).setPreferredWidth(100); // 类型
        columnModel.getColumn(2).setPreferredWidth(50); // 长度
        columnModel.getColumn(3).setPreferredWidth(60); // Not Null
        columnModel.getColumn(4).setPreferredWidth(60); // 主键
        columnModel.getColumn(5).setPreferredWidth(80); // 自增
        columnModel.getColumn(6).setPreferredWidth(80); // 唯一
        columnModel.getColumn(7).setPreferredWidth(100); // 默认值
        columnModel.getColumn(8).setPreferredWidth(150); // 注释
        
        // 设置表格编辑器
        JComboBox<String> typeComboBox = new JComboBox<>(new String[]{
            "INT", "BIGINT", "SMALLINT", "TINYINT", "VARCHAR", "CHAR", "TEXT", "DATETIME", 
            "DATE", "TIME", "TIMESTAMP", "DECIMAL", "FLOAT", "DOUBLE", "BOOLEAN", "ENUM", "JSON"
        });
        columnModel.getColumn(1).setCellEditor(new DefaultCellEditor(typeComboBox));
        
        // 设置复选框列的单元格渲染器和编辑器
        TableColumn notNullColumn = columnModel.getColumn(3);
        notNullColumn.setCellRenderer(new CheckBoxRenderer());
        notNullColumn.setCellEditor(new DefaultCellEditor(new JCheckBox()));
        
        TableColumn primaryKeyColumn = columnModel.getColumn(4);
        primaryKeyColumn.setCellRenderer(new CheckBoxRenderer());
        primaryKeyColumn.setCellEditor(new DefaultCellEditor(new JCheckBox()));
        
        TableColumn autoIncrementColumn = columnModel.getColumn(5);
        autoIncrementColumn.setCellRenderer(new CheckBoxRenderer());
        autoIncrementColumn.setCellEditor(new DefaultCellEditor(new JCheckBox()));
        
        TableColumn uniqueKeyColumn = columnModel.getColumn(6);
        uniqueKeyColumn.setCellRenderer(new CheckBoxRenderer());
        uniqueKeyColumn.setCellEditor(new DefaultCellEditor(new JCheckBox()));
        
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
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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
                ColumnDefinition column = new ColumnDefinition("column" + (columns.size() + 1), "VARCHAR", "255", true, false);
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
        
        // 收集唯一约束列
        List<String> uniqueColumns = new ArrayList<>();
        
        for (ColumnDefinition column : columns) {
            sql.append("    ").append(column.getName()).append(" ").append(column.getType());
            
            // 处理有长度的类型
            if (column.getType().equalsIgnoreCase("VARCHAR") || 
                column.getType().equalsIgnoreCase("CHAR") ||
                column.getType().equalsIgnoreCase("DECIMAL")) {
                if (!column.getLength().isEmpty()) {
                    sql.append("(").append(column.getLength()).append(")");
                } else if (column.getType().equalsIgnoreCase("VARCHAR") || column.getType().equalsIgnoreCase("CHAR")) {
                    sql.append("(255)"); // 默认长度
                }
            }
            
            // NOT NULL约束
            if (column.isNotNull()) {
                sql.append(" NOT NULL");
            }
            
            // 自动增长
            if (column.isAutoIncrement()) {
                sql.append(" AUTO_INCREMENT");
            }
            
            // 默认值
            if (!column.getDefaultValue().isEmpty()) {
                if (column.getDefaultValue().equalsIgnoreCase("NULL")) {
                    sql.append(" DEFAULT NULL");
                } else if (column.getType().equalsIgnoreCase("INT") || 
                          column.getType().equalsIgnoreCase("BIGINT") ||
                          column.getType().equalsIgnoreCase("TINYINT") ||
                          column.getType().equalsIgnoreCase("FLOAT") ||
                          column.getType().equalsIgnoreCase("DOUBLE") ||
                          column.getType().equalsIgnoreCase("DECIMAL")) {
                    // 数字类型默认值无需引号
                    sql.append(" DEFAULT ").append(column.getDefaultValue());
                } else {
                    // 其他类型默认值需要引号
                    sql.append(" DEFAULT '").append(column.getDefaultValue().replace("'", "''")).append("'");
                }
            }
            
            // 注释
            if (!column.getComment().isEmpty()) {
                sql.append(" COMMENT '").append(column.getComment().replace("'", "''")).append("'");
            }
            
            sql.append(",\n");
            
            // 收集唯一约束列
            if (column.isUniqueKey() && !column.isPrimaryKey()) {
                uniqueColumns.add(column.getName());
            }
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
            sql.append(")");
            if (uniqueColumns.isEmpty()) {
                sql.append("\n");
            } else {
                sql.append(",\n");
            }
        } else {
            // 如果没有主键且没有唯一约束，移除最后一个逗号和换行符
            if (uniqueColumns.isEmpty()) {
                sql.setLength(sql.length() - 2);
                sql.append("\n");
            }
        }
        
        // 添加唯一约束
        if (!uniqueColumns.isEmpty()) {
            for (int i = 0; i < uniqueColumns.size(); i++) {
                sql.append("    UNIQUE KEY `uk_").append(uniqueColumns.get(i)).append("` (").append(uniqueColumns.get(i)).append(")");
                if (i < uniqueColumns.size() - 1) {
                    sql.append(",\n");
                } else {
                    sql.append("\n");
                }
            }
        }
        
        sql.append(")");
        
        // 添加表引擎和字符集
        sql.append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
        
        sql.append(";");
        
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
        private boolean autoIncrement;
        private boolean uniqueKey;
        private String defaultValue;
        private String comment;
        
        public ColumnDefinition(String name, String type, String length, boolean notNull, boolean primaryKey) {
            this.name = name;
            this.type = type;
            this.length = length;
            this.nullable = !notNull;
            this.primaryKey = primaryKey;
            this.autoIncrement = false;
            this.uniqueKey = false;
            this.defaultValue = "";
            this.comment = "";
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
        
        public boolean isAutoIncrement() {
            return autoIncrement;
        }
        
        public void setAutoIncrement(boolean autoIncrement) {
            this.autoIncrement = autoIncrement;
        }
        
        public boolean isUniqueKey() {
            return uniqueKey;
        }
        
        public void setUniqueKey(boolean uniqueKey) {
            this.uniqueKey = uniqueKey;
        }
        
        public String getDefaultValue() {
            return defaultValue;
        }
        
        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
        
        public String getComment() {
            return comment;
        }
        
        public void setComment(String comment) {
            this.comment = comment;
        }
    }
    
    /**
     * 列表模型
     */
    private static class ColumnsTableModel extends AbstractTableModel {
        private List<ColumnDefinition> columns;
        private final String[] columnNames = {"名称", "类型", "长度", "非空", "主键", "自增", "唯一", "默认值", "注释"};
        
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
            if (columnIndex == 3 || columnIndex == 4 || columnIndex == 5 || columnIndex == 6) {
                return Boolean.class;
            }
            return String.class;
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ColumnDefinition column = columns.get(rowIndex);
            switch (columnIndex) {
                case 0: return column.getName();
                case 1: return column.getType();
                case 2: return column.getLength();
                case 3: return column.isNotNull();
                case 4: return column.isPrimaryKey();
                case 5: return column.isAutoIncrement();
                case 6: return column.isUniqueKey();
                case 7: return column.getDefaultValue();
                case 8: return column.getComment();
                default: return null;
            }
        }
        
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            ColumnDefinition column = columns.get(rowIndex);
            switch (columnIndex) {
                case 0: column.setName((String) aValue); break;
                case 1: column.setType((String) aValue); break;
                case 2: column.setLength((String) aValue); break;
                case 3: column.setNotNull((Boolean) aValue); break;
                case 4: column.setPrimaryKey((Boolean) aValue); break;
                case 5: column.setAutoIncrement((Boolean) aValue); break;
                case 6: column.setUniqueKey((Boolean) aValue); break;
                case 7: column.setDefaultValue((String) aValue); break;
                case 8: column.setComment((String) aValue); break;
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    // 复选框单元格渲染器
    private static class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        public CheckBoxRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Boolean) {
                setSelected((Boolean) value);
            }
            
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            
            return this;
        }
    }
} 