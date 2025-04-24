package com.database.visualization.view.menu;

import com.database.visualization.model.ConnectionConfig;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionListener;

/**
 * 上下文菜单管理器类，负责创建和管理各种右键菜单
 */
public class ContextMenuManager {

    /**
     * 显示连接上下文菜单
     */
    public static JPopupMenu createConnectionContextMenu(ConnectionConfig config, ActionListener connectAction,
                                                         ActionListener editAction, ActionListener deleteAction,
                                                         ActionListener refreshAction) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem connectItem = new JMenuItem("连接");
        JMenuItem editItem = new JMenuItem("编辑连接");
        JMenuItem deleteItem = new JMenuItem("删除连接");
        JMenuItem refreshItem = new JMenuItem("刷新");

        connectItem.addActionListener(connectAction);
        editItem.addActionListener(editAction);
        deleteItem.addActionListener(deleteAction);
        refreshItem.addActionListener(refreshAction);

        menu.add(connectItem);
        menu.add(editItem);
        menu.add(deleteItem);
        menu.addSeparator();
        menu.add(refreshItem);

        return menu;
    }

    /**
     * 显示数据库/schema上下文菜单
     */
    public static JPopupMenu createSchemaContextMenu(ConnectionConfig config, String schemaName,
                                                     ActionListener refreshAction, ActionListener newDatabaseAction,
                                                     ActionListener alterDatabaseAction, ActionListener dropDatabaseAction,
                                                     ActionListener newTableAction, ActionListener queryAction,
                                                     ActionListener exportSqlAction, ActionListener batchExecuteSqlAction) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem refreshItem = new JMenuItem("刷新");
        JMenuItem newDatabaseItem = new JMenuItem("新建数据库");
        JMenuItem alterDatabaseItem = new JMenuItem("修改数据库");
        JMenuItem dropDatabaseItem = new JMenuItem("删除数据库");
        JMenuItem newTableItem = new JMenuItem("新建表");
        JMenuItem queryItem = new JMenuItem("执行查询");
        // 添加导出SQL选项
        JMenuItem exportSqlItem = new JMenuItem("导出SQL");
        // 添加批量执行SQL选项
        JMenuItem batchExecuteSqlItem = new JMenuItem("批量执行SQL");

        refreshItem.addActionListener(refreshAction);
        newDatabaseItem.addActionListener(newDatabaseAction);
        alterDatabaseItem.addActionListener(alterDatabaseAction);
        dropDatabaseItem.addActionListener(dropDatabaseAction);
        newTableItem.addActionListener(newTableAction);
        queryItem.addActionListener(queryAction);
        exportSqlItem.addActionListener(exportSqlAction);
        batchExecuteSqlItem.addActionListener(batchExecuteSqlAction);

        menu.add(refreshItem);
        menu.addSeparator();
        menu.add(newDatabaseItem);
        menu.add(alterDatabaseItem);
        menu.add(dropDatabaseItem);
        menu.addSeparator();
        menu.add(newTableItem);
        menu.add(queryItem);
        menu.addSeparator();
        menu.add(exportSqlItem);
        menu.add(batchExecuteSqlItem);

        return menu;
    }

    /**
     * 显示表上下文菜单
     */
    public static JPopupMenu createTableContextMenu(ConnectionConfig config, String schemaName, String tableName,
                                                    ActionListener queryAction, ActionListener structureAction,
                                                    ActionListener editAction, ActionListener dropAction,
                                                    ActionListener emptyAction, ActionListener exportAction) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem queryItem = new JMenuItem("查询数据");
        JMenuItem structureItem = new JMenuItem("表结构");
        JMenuItem editItem = new JMenuItem("修改表");
        JMenuItem dropItem = new JMenuItem("删除表");
        JMenuItem emptyItem = new JMenuItem("清空表");
        JMenuItem exportItem = new JMenuItem("导出数据");

        queryItem.addActionListener(queryAction);
        structureItem.addActionListener(structureAction);
        editItem.addActionListener(editAction);
        dropItem.addActionListener(dropAction);
        emptyItem.addActionListener(emptyAction);
        exportItem.addActionListener(exportAction);

        menu.add(queryItem);
        menu.add(structureItem);
        menu.addSeparator();
        menu.add(editItem);
        menu.add(dropItem);
        menu.add(emptyItem);
        menu.addSeparator();
        menu.add(exportItem);

        return menu;
    }

    /**
     * 显示Redis数据库上下文菜单
     */
    public static JPopupMenu createRedisDatabaseContextMenu(ConnectionConfig config, String dbName,
                                                            ActionListener selectAction, ActionListener flushAction,
                                                            ActionListener refreshAction) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem selectItem = new JMenuItem("选择数据库");
        JMenuItem flushItem = new JMenuItem("清空数据库");
        JMenuItem refreshItem = new JMenuItem("刷新");

        selectItem.addActionListener(selectAction);
        flushItem.addActionListener(flushAction);
        refreshItem.addActionListener(refreshAction);

        menu.add(selectItem);
        menu.add(flushItem);
        menu.add(refreshItem);

        return menu;
    }

    /**
     * 显示Redis类型上下文菜单
     */
    public static JPopupMenu createRedisTypeContextMenu(ConnectionConfig config, String dbName, String typeName,
                                                        ActionListener queryAction) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem queryItem = new JMenuItem("查询键");

        queryItem.addActionListener(queryAction);

        menu.add(queryItem);

        return menu;
    }

    /**
     * 辅助方法，根据节点类型创建合适的上下文菜单
     */
    public static void showTreeNodeContextMenu(DefaultMutableTreeNode node, int x, int y, JTree tree, MenuActionHandler handler) {
        if (node == null) return;

        Object userObject = node.getUserObject();

        if (userObject instanceof ConnectionConfig) {
            // 连接节点的右键菜单
            ConnectionConfig config = (ConnectionConfig) userObject;
            JPopupMenu menu = createConnectionContextMenu(
                    config,
                    e -> handler.connectToDatabase(config),
                    e -> handler.editConnection(config),
                    e -> handler.deleteConnection(config),
                    e -> handler.refreshDatabaseTree()
            );
            menu.show(tree, x, y);
        } else if (userObject instanceof String && node.getParent() != null) {
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
            Object parentObject = parentNode.getUserObject();

            if (parentObject instanceof ConnectionConfig) {
                // 数据库/schema节点的右键菜单
                ConnectionConfig config = (ConnectionConfig) parentObject;
                String dbName = (String) userObject;

                // 对于Redis数据库节点
                if ("redis".equalsIgnoreCase(config.getDatabaseType()) && dbName.startsWith("db")) {
                    JPopupMenu menu = createRedisDatabaseContextMenu(
                            config,
                            dbName,
                            e -> handler.selectRedisDatabase(config, dbName),
                            e -> handler.flushRedisDatabase(config, dbName),
                            e -> handler.refreshDatabaseTree()
                    );
                    menu.show(tree, x, y);
                } else {
                    // 普通数据库/schema节点
                    JPopupMenu menu = createSchemaContextMenu(
                            config,
                            dbName,
                            e -> handler.refreshDatabaseTree(),
                            e -> handler.createNewDatabase(config),
                            e -> handler.alterDatabase(config, dbName),
                            e -> handler.dropDatabase(config, dbName),
                            e -> handler.createNewTable(config, dbName),
                            e -> handler.executeQuery(config, dbName),
                            e -> handler.exportDatabaseSql(config, dbName),
                            e -> handler.batchExecuteSql(config, dbName)
                    );
                    menu.show(tree, x, y);
                }
            } else if (parentObject instanceof String && parentNode.getParent() != null) {
                // Schema节点或Redis数据库节点下的内容
                DefaultMutableTreeNode grandParentNode = (DefaultMutableTreeNode) parentNode.getParent();
                if (grandParentNode.getUserObject() instanceof ConnectionConfig) {
                    ConnectionConfig config = (ConnectionConfig) grandParentNode.getUserObject();
                    String parentName = (String) parentObject;
                    String nodeName = (String) userObject;

                    if ("redis".equalsIgnoreCase(config.getDatabaseType()) && parentName.startsWith("db")) {
                        // Redis数据库中的类型节点
                        JPopupMenu menu = createRedisTypeContextMenu(
                                config,
                                parentName,
                                nodeName,
                                e -> handler.queryRedisKeys(config, parentName, nodeName)
                        );
                        menu.show(tree, x, y);
                    } else {
                        // 普通Schema下的表
                        JPopupMenu menu = createTableContextMenu(
                                config,
                                parentName,
                                nodeName,
                                e -> handler.queryTable(config, parentName, nodeName),
                                e -> handler.showTableStructure(config, parentName, nodeName),
                                e -> handler.editTable(config, parentName, nodeName),
                                e -> handler.dropTable(config, parentName, nodeName),
                                e -> handler.emptyTable(config, parentName, nodeName),
                                e -> handler.exportTableData(config, parentName, nodeName)
                        );
                        menu.show(tree, x, y);
                    }
                }
            }
        }
    }

    /**
     * 菜单动作处理器接口
     */
    public interface MenuActionHandler {
        void connectToDatabase(ConnectionConfig config);

        void editConnection(ConnectionConfig config);

        void deleteConnection(ConnectionConfig config);

        void refreshDatabaseTree();

        void createNewDatabase(ConnectionConfig config);

        void alterDatabase(ConnectionConfig config, String dbName);

        void dropDatabase(ConnectionConfig config, String dbName);

        void createNewTable(ConnectionConfig config, String dbName);

        void executeQuery(ConnectionConfig config, String dbName);

        void exportDatabaseSql(ConnectionConfig config, String dbName);

        void batchExecuteSql(ConnectionConfig config, String dbName);

        void selectRedisDatabase(ConnectionConfig config, String dbName);

        void flushRedisDatabase(ConnectionConfig config, String dbName);

        void queryRedisKeys(ConnectionConfig config, String dbName, String typeName);

        void queryTable(ConnectionConfig config, String schemaName, String tableName);

        void showTableStructure(ConnectionConfig config, String schemaName, String tableName);

        void editTable(ConnectionConfig config, String schemaName, String tableName);

        void dropTable(ConnectionConfig config, String schemaName, String tableName);

        void emptyTable(ConnectionConfig config, String schemaName, String tableName);

        void exportTableData(ConnectionConfig config, String schemaName, String tableName);
    }
} 