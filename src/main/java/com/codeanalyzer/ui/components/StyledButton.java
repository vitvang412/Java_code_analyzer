package com.codeanalyzer.ui.components;

import com.codeanalyzer.ui.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Nút bấm có bo tròn + hover effect. Dùng chung cho toàn UI.
 *
 * <pre>
 *   new StyledButton("▶ Bắt đầu", StyledButton.Variant.PRIMARY);
 *   new StyledButton("Xoá",        StyledButton.Variant.DANGER);
 * </pre>
 */
public class StyledButton extends JButton {

    public enum Variant {
        PRIMARY(UIConstants.PRIMARY,  UIConstants.PRIMARY_DARK,  Color.WHITE),
        ACCENT (UIConstants.ACCENT,   UIConstants.ACCENT_DARK,   Color.WHITE),
        SUCCESS(UIConstants.SUCCESS,  UIConstants.SUCCESS.darker(), Color.WHITE),
        DANGER (UIConstants.DANGER,   UIConstants.DANGER.darker(),  Color.WHITE),
        NEUTRAL(new Color(0xECEFF1),  new Color(0xCFD8DC),       UIConstants.TEXT);

        final Color base, hover, fg;
        Variant(Color base, Color hover, Color fg) {
            this.base = base; this.hover = hover; this.fg = fg;
        }
    }

    private final Variant variant;
    private boolean hovered = false;

    public StyledButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setFont(UIConstants.MAIN.deriveFont(Font.BOLD));
        setForeground(variant.fg);
        setFocusPainted(false);
        setBorder(new EmptyBorder(8, 16, 8, 16));
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
            @Override public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg = isEnabled()
                ? (hovered ? variant.hover : variant.base)
                : new Color(0xBDBDBD);

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.dispose();

        super.paintComponent(g);
    }
}
