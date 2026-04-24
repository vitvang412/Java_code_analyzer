package com.codeanalyzer.ui;

import java.awt.*;

/**
 * Bảng màu / font dùng chung cho toàn bộ UI.
 * Thiết kế theo hướng "Indigo + Emerald" – hiện đại, dễ nhìn, tương phản tốt.
 */
public final class UIConstants {
    private UIConstants() {}

    // ── Màu chính ──────────────────────────────────────────────────────────
    public static final Color PRIMARY       = new Color(0x3F51B5);  // Indigo
    public static final Color PRIMARY_DARK  = new Color(0x303F9F);
    public static final Color PRIMARY_LIGHT = new Color(0x7986CB);
    public static final Color ACCENT        = new Color(0x00BFA5);  // Emerald
    public static final Color ACCENT_DARK   = new Color(0x009688);

    // ── Màu trạng thái ─────────────────────────────────────────────────────
    public static final Color SUCCESS       = new Color(0x2E7D32);
    public static final Color WARNING       = new Color(0xF57C00);
    public static final Color DANGER        = new Color(0xC62828);

    // ── Màu nền / text ─────────────────────────────────────────────────────
    public static final Color BACKGROUND    = new Color(0xF5F7FA);
    public static final Color CARD_BG       = Color.WHITE;
    public static final Color TEXT          = new Color(0x212121);
    public static final Color TEXT_MUTED    = new Color(0x757575);
    public static final Color BORDER        = new Color(0xE0E0E0);

    // ── Màu log / console ──────────────────────────────────────────────────
    public static final Color CONSOLE_BG    = new Color(0x1E1E2E);
    public static final Color CONSOLE_FG    = new Color(0xC8E6C9);

    // ── Fonts ──────────────────────────────────────────────────────────────
    public static final Font HEADER     = new Font("Segoe UI", Font.BOLD,  20);
    public static final Font SUBHEADER  = new Font("Segoe UI", Font.BOLD,  14);
    public static final Font MAIN       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font SMALL      = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font MONO       = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    // ── Legacy aliases (giữ để không vỡ code cũ) ──────────────────────────
    public static final Color PRIMARY_COLOR    = PRIMARY;
    public static final Color ACCENT_COLOR     = ACCENT;
    public static final Color BACKGROUND_COLOR = BACKGROUND;
    public static final Color TEXT_COLOR       = TEXT;
    public static final Font  MAIN_FONT        = MAIN;
    public static final Font  HEADER_FONT      = HEADER;

    // ── Kích thước ─────────────────────────────────────────────────────────
    public static final int PADDING      = 12;
    public static final int BIG_PADDING  = 20;
}
