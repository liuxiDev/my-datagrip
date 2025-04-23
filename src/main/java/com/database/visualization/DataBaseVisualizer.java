package com.database.visualization;

import com.database.visualization.utils.ConnectionManager;
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
    // 实际字体大小值
    public static int fontSizeValue = 14;
    
    public static void main(String[] args) {
        // 加载配置
        loadSettings();
        
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
            
            // 保存设置
            ConnectionManager.saveSetting("isDarkTheme", isDarkTheme);
            
            SwingUtilities.updateComponentTreeUI(FocusManager.getCurrentManager().getActiveWindow());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 保存字体大小设置
     */
    public static void saveFontSizeSettings() {
        ConnectionManager.saveSetting("fontSizeFactor", fontSizeFactor);
        ConnectionManager.saveSetting("fontSizeValue", fontSizeValue);
    }
    
    /**
     * 加载配置
     */
    private static void loadSettings() {
        // 加载主题设置
        Object darkTheme = ConnectionManager.getSetting("isDarkTheme", true);
        if (darkTheme instanceof Boolean) {
            isDarkTheme = (Boolean) darkTheme;
        }
        
        // 加载字体大小设置
        Object fontSize = ConnectionManager.getSetting("fontSizeValue", 14);
        if (fontSize instanceof Integer) {
            fontSizeValue = (Integer) fontSize;
        } else if (fontSize instanceof Number) {
            fontSizeValue = ((Number) fontSize).intValue();
        }
        
        Object sizeFactor = ConnectionManager.getSetting("fontSizeFactor", 1.0f);
        if (sizeFactor instanceof Double) {
            fontSizeFactor = ((Double) sizeFactor).floatValue();
        } else if (sizeFactor instanceof Float) {
            fontSizeFactor = (Float) sizeFactor;
        } else if (sizeFactor instanceof Number) {
            fontSizeFactor = ((Number) sizeFactor).floatValue();
        }
    }
} 