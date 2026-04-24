package com.codeanalyzer.ui.components;

import com.codeanalyzer.ui.UIConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A modern rounded button with hover/press animations.
 */
public class StyledButton extends JButton {

    public enum Variant { PRIMARY, ACCENT, SUCCESS, DANGER, WARNING, NEUTRAL }

    private final Color baseColor;
    private final Color hoverColor;
    private final Color pressColor;
    private Color currentBg;
    private boolean hovered = false;
    private boolean pressed = false;

    public StyledButton(String text, Variant variant) {
        super(text);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(UIConstants.SMALL_BOLD);
        setForeground(Color.WHITE);

        baseColor  = resolveBase(variant);
        hoverColor = baseColor.brighter();
        pressColor = baseColor.darker();
        currentBg  = baseColor;

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true;  currentBg = hoverColor; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; currentBg = baseColor;  repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = true;  currentBg = pressColor; repaint(); }
            @Override public void mouseReleased(MouseEvent e){ pressed = false; currentBg = hovered ? hoverColor : baseColor; repaint(); }
        });

        setPreferredSize(new Dimension(getPreferredSize().width + 24, 32));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg = isEnabled() ? currentBg : new Color(0xB0BEC5);
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), UIConstants.RADIUS, UIConstants.RADIUS));

        // Subtle shine overlay at top
        if (isEnabled() && !pressed) {
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight() / 2.0f, UIConstants.RADIUS, UIConstants.RADIUS));
        }

        g2.dispose();
        super.paintComponent(g);
    }

    private Color resolveBase(Variant v) {
        return switch (v) {
            case PRIMARY -> UIConstants.PRIMARY;
            case ACCENT  -> UIConstants.ACCENT_DARK;
            case SUCCESS -> UIConstants.SUCCESS;
            case DANGER  -> UIConstants.DANGER;
            case WARNING -> UIConstants.WARNING;
            case NEUTRAL -> new Color(0x546E7A);
        };
    }
}
