package com.database.visualization.view.tree;

import com.database.visualization.controller.DatabaseService;
import com.database.visualization.model.ConnectionConfig;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 数据库树管理器类
 */
public class DatabaseTreeManager {
    private final JTree databaseTree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;
    private final JLabel statusLabel;
    private ConnectionConfig currentConnection;

    public DatabaseTreeManager(JTree databaseTree, DefaultTreeModel treeModel, DefaultMutableTreeNode rootNode, JLabel statusLabel) {
        this.databaseTree = databaseTree;
        this.treeModel = treeModel;
        this.rootNode = rootNode;
        this.statusLabel = statusLabel;
    }

    /**
     * 加载数据库中的表
     */
    public void loadDatabaseTables(ConnectionConfig config, String dbName, DefaultMutableTreeNode parentNode) {
        if (config == null || dbName == null || parentNode == null) {
            return;
        }

        ConnectionConfig connectionToUse = config;
        if (!connectionToUse.getDatabase().equals(dbName)) {
            connectionToUse.setDatabase(dbName);
        }
        // 先清空当前节点的子节点
        parentNode.removeAllChildren();

        // 在后台线程中执行数据库操作
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                try {
                    // 获取数据库中的表
                    return DatabaseService.getTables(connectionToUse, dbName);
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ArrayList<>(); // 返回空列表而不是null
                }
            }

            @Override
            protected void done() {
                try {
                    List<String> tables = get();

                    // 清空现有节点，确保添加的是当前数据库的表
                    parentNode.removeAllChildren();

                    // 添加表节点
                    for (String table : tables) {
                        DefaultMutableTreeNode tableNode = new DefaultMutableTreeNode(table);
                        parentNode.add(tableNode);
                    }

                    // 刷新树
                    treeModel.nodeStructureChanged(parentNode);

                    // 展开数据库节点
                    TreePath path = new TreePath(parentNode.getPath());
                    databaseTree.expandPath(path);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (statusLabel != null) {
                        statusLabel.setText("加载表失败: " + e.getMessage());
                    }
                }
            }
        };

        worker.execute();
    }

    /**
     * 刷新数据库树
     */
    public void refreshDatabaseTree(List<ConnectionConfig> connections) {
        // 保存当前选中的路径
        TreePath selectedPath = databaseTree.getSelectionPath();

        if (selectedPath != null && selectedPath.getPathCount() > 1) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            Object userObject = selectedNode.getUserObject();

            // 如果当前选中的是数据库节点，只刷新该数据库下的表
            if (userObject instanceof String && selectedNode.getParent() != null) {
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();

                if (parentNode.getUserObject() instanceof ConnectionConfig) {
                    ConnectionConfig config = (ConnectionConfig) parentNode.getUserObject();
                    String dbName = (String) userObject;

                    // 清空节点并重新加载表
                    selectedNode.removeAllChildren();
                    treeModel.nodeStructureChanged(selectedNode);
                    loadDatabaseTables(config, dbName, selectedNode);

                    if (statusLabel != null) {
                        statusLabel.setText("已刷新数据库: " + dbName);
                    }
                    return;
                }
            }
        }

        // 否则刷新整个连接树
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                if (statusLabel != null) {
                    statusLabel.setText("正在连接数据库...");
                }

                // 存储展开的节点路径
                Enumeration<TreePath> expandedPaths = databaseTree.getExpandedDescendants(
                        new TreePath(rootNode.getPath()));

                // 清除根节点下的所有子节点
                rootNode.removeAllChildren();

                // 添加所有连接到树
                for (ConnectionConfig config : connections) {
                    DefaultMutableTreeNode connNode = new DefaultMutableTreeNode(config);
                    rootNode.add(connNode);

                    // 如果是当前连接，加载数据库
                    if (currentConnection != null && config.getId().equals(currentConnection.getId())) {
                        try {
                            // 获取所有数据库/schema
                            List<String> databases = DatabaseService.getDatabases(config);

                            if (databases != null && !databases.isEmpty()) {
                                for (String db : databases) {
                                    DefaultMutableTreeNode dbNode = new DefaultMutableTreeNode(db);
                                    connNode.add(dbNode);
                                }
                            } else {
                                // 如果无法获取数据库列表，添加一个提示节点
                                DefaultMutableTreeNode noDbNode = new DefaultMutableTreeNode("(无数据库)");
                                connNode.add(noDbNode);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // 添加一个错误提示节点
                            DefaultMutableTreeNode errorNode = new DefaultMutableTreeNode("(加载错误)");
                            connNode.add(errorNode);
                        }
                    }
                }

                // 刷新树模型
                treeModel.reload();

                // 恢复展开的节点
                if (expandedPaths != null) {
                    while (expandedPaths.hasMoreElements()) {
                        TreePath path = expandedPaths.nextElement();
                        databaseTree.expandPath(path);
                    }
                }

                // 恢复选中的节点
                if (selectedPath != null) {
                    databaseTree.setSelectionPath(selectedPath);
                }

                return null;
            }

            @Override
            protected void done() {
                if (statusLabel != null) {
                    statusLabel.setText("数据库树刷新完成");
                }
            }
        };

        worker.execute();
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
    public void setCurrentConnection(ConnectionConfig currentConnection) {
        this.currentConnection = currentConnection;
    }
} 