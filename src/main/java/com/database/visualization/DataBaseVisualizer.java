package com.database.visualization;

import com.database.visualization.view.MainFrame;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * 数据库可视化工具主入口
 */
public class DataBaseVisualizer {
    
    public static void main(String[] args) {
        // 设置UI外观
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 在EDT线程中创建GUI
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    MainFrame mainFrame = new MainFrame();
                    mainFrame.setVisible(true);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
} 