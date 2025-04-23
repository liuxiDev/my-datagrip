package com.database.visualization;

import com.database.visualization.view.MainFrame;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * 数据库可视化工具主入口
 */
public class DataBaseVisualizer {
    
    // 主题设置：深色或浅色
    public static boolean isDarkTheme = true;
    // 全局字体大小
    public static float fontSizeFactor = 1.0f;
    
    public static void main(String[] args) {
        // 设置UI外观
        try {
            if (isDarkTheme) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
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
    
    /**
     * 应用主题设置
     */
    public static void applyTheme() {
        try {
            if (isDarkTheme) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(FocusManager.getCurrentManager().getActiveWindow());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 