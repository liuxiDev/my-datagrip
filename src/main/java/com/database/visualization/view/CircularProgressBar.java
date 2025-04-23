package com.database.visualization.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * 圆形进度条组件
 */
public class CircularProgressBar extends JPanel {
    
    private int min;
    private int max;
    private int value;
    private String title;
    
    private Color backgroundColor = new Color(240, 240, 240);
    private Color foregroundColor = new Color(0, 116, 217);
    private Color titleColor = new Color(33, 33, 33);
    private Color valueColor = new Color(33, 33, 33);
    
    public CircularProgressBar(String title, int min, int max) {
        this.title = title;
        this.min = min;
        this.max = max;
        this.value = 0;
        setPreferredSize(new Dimension(150, 150));
    }
    
    public void setValue(int value) {
        this.value = value;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);
        
        // 计算圆的边界
        int x = (width - size) / 2;
        int y = (height - size) / 2;
        
        // 绘制背景圆圈
        g2.setColor(backgroundColor);
        g2.fill(new Ellipse2D.Double(x, y, size, size));
        
        // 绘制前景填充
        double percentage = (double) (value - min) / (max - min);
        double angle = 360 * percentage;
        
        g2.setColor(foregroundColor);
        g2.fill(new Arc2D.Double(x, y, size, size, 90, -angle, Arc2D.PIE));
        
        // 绘制中间的圆形
        int innerSize = size * 2 / 3;
        int innerX = (width - innerSize) / 2;
        int innerY = (height - innerSize) / 2;
        
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Double(innerX, innerY, innerSize, innerSize));
        
        // 绘制文字
        g2.setColor(titleColor);
        g2.setFont(new Font("Arial", Font.BOLD, size / 10));
        
        FontMetrics fm = g2.getFontMetrics();
        Rectangle2D titleBounds = fm.getStringBounds(title, g2);
        int titleX = (int) (width - titleBounds.getWidth()) / 2;
        int titleY = (int) (height - fm.getHeight()) / 2;
        
        g2.drawString(title, titleX, titleY);
        
        // 绘制值
        String valueStr = String.valueOf(value) + "%";
        g2.setColor(valueColor);
        g2.setFont(new Font("Arial", Font.BOLD, size / 8));
        
        fm = g2.getFontMetrics();
        Rectangle2D valueBounds = fm.getStringBounds(valueStr, g2);
        int valueX = (int) (width - valueBounds.getWidth()) / 2;
        int valueY = (int) (titleY + fm.getHeight() + 5);
        
        g2.drawString(valueStr, valueX, valueY);
    }
} 