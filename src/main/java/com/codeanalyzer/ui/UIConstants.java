package com.codeanalyzer.ui;

import java.awt.*;

/**
 * Design system: Dark blue / Cyan accent – professional, modern.
 */
public final class UIConstants {
    private UIConstants() {}

    // ── Primary palette ────────────────────────────────────────────────────
    public static final Color PRIMARY        = new Color(0x1A237E);  // Deep indigo
    public static final Color PRIMARY_DARK   = new Color(0x0D1757);
    public static final Color PRIMARY_LIGHT  = new Color(0x3949AB);
    public static final Color ACCENT         = new Color(0x00BCD4);  // Cyan
    public static final Color ACCENT_DARK    = new Color(0x0097A7);
    public static final Color ACCENT_LIGHT   = new Color(0x80DEEA);

    // ── Status colors ──────────────────────────────────────────────────────
    public static final Color SUCCESS        = new Color(0x00897B);
    public static final Color SUCCESS_LIGHT  = new Color(0xB2DFDB);
    public static final Color WARNING        = new Color(0xF9A825);
    public static final Color WARNING_LIGHT  = new Color(0xFFF9C4);
    public static final Color DANGER         = new Color(0xC62828);
    public static final Color DANGER_LIGHT   = new Color(0xFFCDD2);
    public static final Color INFO           = new Color(0x1565C0);
    public static final Color INFO_LIGHT     = new Color(0xBBDEFB);

    // ── Background / surface ───────────────────────────────────────────────
    public static final Color BACKGROUND     = new Color(0xECEFF1);  // Blue grey 50
    public static final Color SURFACE        = new Color(0xF5F7FA);
    public static final Color CARD_BG        = Color.WHITE;
    public static final Color CARD_BORDER    = new Color(0xCFD8DC);

    // ── Text ───────────────────────────────────────────────────────────────
    public static final Color TEXT           = new Color(0x212121);
    public static final Color TEXT_SECONDARY = new Color(0x546E7A);
    public static final Color TEXT_MUTED     = new Color(0x90A4AE);
    public static final Color TEXT_ON_PRIMARY= Color.WHITE;
    public static final Color BORDER         = new Color(0xCFD8DC);

    // ── Console / log ──────────────────────────────────────────────────────
    public static final Color CONSOLE_BG     = new Color(0x0A1628);
    public static final Color CONSOLE_FG     = new Color(0x80CBC4);
    public static final Color CONSOLE_INFO   = new Color(0x80DEEA);
    public static final Color CONSOLE_WARN   = new Color(0xFFCC80);
    public static final Color CONSOLE_ERROR  = new Color(0xEF9A9A);

    // ── Selection ──────────────────────────────────────────────────────────
    public static final Color TABLE_HEADER_BG  = new Color(0x1A237E);
    public static final Color TABLE_HEADER_FG  = Color.WHITE;
    public static final Color TABLE_ALT_ROW    = new Color(0xF5F9FF);
    public static final Color TABLE_SEL_BG     = new Color(0xBBDEFB);
    public static final Color TABLE_SEL_FG     = new Color(0x0D1757);
    public static final Color TABLE_GRID       = new Color(0xE0E7EF);

    // ── Fonts ──────────────────────────────────────────────────────────────
    public static final Font HEADER      = new Font("Segoe UI", Font.BOLD,  22);
    public static final Font SUBHEADER   = new Font("Segoe UI", Font.BOLD,  14);
    public static final Font MAIN        = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font SMALL       = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font SMALL_BOLD  = new Font("Segoe UI", Font.BOLD,  12);
    public static final Font MONO        = new Font("Consolas", Font.PLAIN, 12);
    public static final Font LABEL       = new Font("Segoe UI", Font.PLAIN, 11);

    // ── Legacy aliases ─────────────────────────────────────────────────────
    public static final Color PRIMARY_COLOR    = PRIMARY;
    public static final Color ACCENT_COLOR     = ACCENT;
    public static final Color BACKGROUND_COLOR = BACKGROUND;
    public static final Color TEXT_COLOR       = TEXT;
    public static final Font  MAIN_FONT        = MAIN;
    public static final Font  HEADER_FONT      = HEADER;

    // ── Spacing ────────────────────────────────────────────────────────────
    public static final int PADDING      = 12;
    public static final int BIG_PADDING  = 20;
    public static final int RADIUS       = 8;
}
