package com.codeanalyzer.ui;

import java.awt.*;

/**
 * Design system: Deep Navy / Violet accent / Emerald – modern, high-contrast, premium.
 */
public final class UIConstants {
    private UIConstants() {}

    // ── Primary palette ────────────────────────────────────────────────────
    public static final Color PRIMARY        = new Color(0x1E293B);  // Slate 800 (deep navy)
    public static final Color PRIMARY_DARK   = new Color(0x0F172A);  // Slate 900
    public static final Color PRIMARY_LIGHT  = new Color(0x334155);  // Slate 700
    public static final Color ACCENT         = new Color(0x6366F1);  // Indigo-500 (violet accent)
    public static final Color ACCENT_DARK    = new Color(0x4F46E5);  // Indigo-600
    public static final Color ACCENT_LIGHT   = new Color(0xA5B4FC);  // Indigo-300

    // ── Status colors ──────────────────────────────────────────────────────
    public static final Color SUCCESS        = new Color(0x059669);  // Emerald-600
    public static final Color SUCCESS_LIGHT  = new Color(0xD1FAE5);  // Emerald-100
    public static final Color WARNING        = new Color(0xD97706);  // Amber-600
    public static final Color WARNING_LIGHT  = new Color(0xFEF3C7);  // Amber-100
    public static final Color DANGER         = new Color(0xDC2626);  // Red-600
    public static final Color DANGER_LIGHT   = new Color(0xFEE2E2);  // Red-100
    public static final Color INFO           = new Color(0x0284C7);  // Sky-600
    public static final Color INFO_LIGHT     = new Color(0xE0F2FE);  // Sky-100

    // ── Background / surface ───────────────────────────────────────────────
    public static final Color BACKGROUND     = new Color(0xF1F5F9);  // Slate-100
    public static final Color SURFACE        = new Color(0xF8FAFC);  // Slate-50
    public static final Color CARD_BG        = Color.WHITE;
    public static final Color CARD_BORDER    = new Color(0xE2E8F0);  // Slate-200

    // ── Text ───────────────────────────────────────────────────────────────
    public static final Color TEXT           = new Color(0x0F172A);  // Slate-900
    public static final Color TEXT_SECONDARY = new Color(0x475569);  // Slate-600
    public static final Color TEXT_MUTED     = new Color(0x94A3B8);  // Slate-400
    public static final Color TEXT_ON_PRIMARY= Color.WHITE;
    public static final Color BORDER         = new Color(0xE2E8F0);  // Slate-200

    // ── Console / log ──────────────────────────────────────────────────────
    public static final Color CONSOLE_BG     = new Color(0x0D1117);  // GitHub dark
    public static final Color CONSOLE_FG     = new Color(0xC9D1D9);  // GitHub text
    public static final Color CONSOLE_INFO   = new Color(0x58A6FF);  // GitHub blue
    public static final Color CONSOLE_WARN   = new Color(0xF0883E);  // GitHub orange
    public static final Color CONSOLE_ERROR  = new Color(0xFF7B72);  // GitHub red
    public static final Color CONSOLE_SUCCESS= new Color(0x3FB950);  // GitHub green

    // ── Table ──────────────────────────────────────────────────────────────
    public static final Color TABLE_HEADER_BG  = new Color(0x1E293B);  // Slate-800
    public static final Color TABLE_HEADER_FG  = Color.WHITE;
    public static final Color TABLE_ALT_ROW    = new Color(0xF8FAFC);  // Slate-50
    public static final Color TABLE_SEL_BG     = new Color(0xEEF2FF);  // Indigo-50
    public static final Color TABLE_SEL_FG     = new Color(0x1E293B);  // Slate-800
    public static final Color TABLE_GRID       = new Color(0xE2E8F0);  // Slate-200

    // ── Fonts ──────────────────────────────────────────────────────────────
    public static final Font HEADER      = new Font("Segoe UI", Font.BOLD,  22);
    public static final Font SUBHEADER   = new Font("Segoe UI", Font.BOLD,  15);
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
    public static final int RADIUS       = 10;
}
