package com.professor.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

@Environment(EnvType.CLIENT)
public class ProfessorSplashScreen extends Screen {

    // ══════════════════════════════════════════════════════════════════════
    //  ICE PALETTE  — all fully opaque for visibility
    // ══════════════════════════════════════════════════════════════════════
    private static final int BG       = 0xFF010A14;   // deep midnight navy
    private static final int PANEL_BG = 0xFF041828;   // solid dark ice
    private static final int BORDER   = 0xFFAAEEFF;   // bright ice blue (solid)
    private static final int BORDER2  = 0xFF55CCFF;   // secondary border
    private static final int TITLE1   = 0xFFEEF8FF;   // snow white text
    private static final int TITLE2   = 0xFFFFD700;   // gold accent
    private static final int TXT_ICE  = 0xFF88DDFF;   // ice blue text
    private static final int TXT_DIM  = 0xFF4488AA;   // dim ice text
    private static final int SNOW_A   = 0xFFFFFFFF;   // pure white snow
    private static final int SNOW_B   = 0xFFCCEEFF;   // light blue snow
    private static final int GRID_C   = 0xFF0A2A44;   // grid line color
    private static final int GOLD     = 0xFFFFD700;
    private static final int CRYSTAL  = 0xFF00CCEE;

    private static final Random RNG = new Random();

    // Progress / state
    private float   loadProgress = 0f;
    private int     msgIdx       = 0;
    private boolean done         = false;
    private int     doneTimer    = 0;
    private float   fadeIn       = 0f;
    private float   fadeOut      = 0f;
    private int     tick         = 0;
    private float   glow         = 0f;
    private boolean glowUp       = true;

    // Snow particles  [x, y, speed, size, alpha, drift-phase]
    private static final int SNOW_COUNT = 220;
    private final float[] sx  = new float[SNOW_COUNT];
    private final float[] sy  = new float[SNOW_COUNT];
    private final float[] ssp = new float[SNOW_COUNT];
    private final float[] ssz = new float[SNOW_COUNT];
    private final float[] sal = new float[SNOW_COUNT];
    private final float[] sph = new float[SNOW_COUNT];

    // Sparkle glitter  [x, y, phase, size]
    private static final int SPARK_COUNT = 60;
    private final float[] gpx = new float[SPARK_COUNT];
    private final float[] gpy = new float[SPARK_COUNT];
    private final float[] gpp = new float[SPARK_COUNT];
    private final float[] gps = new float[SPARK_COUNT];

    // Data-stream columns  [x, y, speed, char]
    private static final int COL_COUNT = 50;
    private final float[] cx2 = new float[COL_COUNT];
    private final float[] cy2 = new float[COL_COUNT];
    private final float[] csp = new float[COL_COUNT];
    private final int[]   cch = new int[COL_COUNT];
    private static final String CHARS = "XERION01FROST10BYPASS00ICE";

    private static final String[] MESSAGES = {
        "Initializing Xerion Client...",
        "Loading frost modules...",
        "Bypassing anti-cheat systems...",
        "Injecting packet hooks...",
        "Calibrating ice engine...",
        "Applying bypass patterns...",
        "❄  All systems ready.  Welcome, operator.  ❄"
    };

    public ProfessorSplashScreen() { super(Text.literal("Xerion Client")); }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause()      { return false; }

    @Override
    protected void init() {
        int W = width, H = height;
        for (int i = 0; i < SNOW_COUNT; i++) {
            sx[i]  = RNG.nextFloat() * W;
            sy[i]  = RNG.nextFloat() * H;
            ssp[i] = RNG.nextFloat() * 0.8f + 0.25f;
            ssz[i] = RNG.nextFloat() * 4f + 1f;
            sal[i] = RNG.nextFloat() * 0.7f + 0.3f;
            sph[i] = RNG.nextFloat() * 6.28f;
        }
        for (int i = 0; i < SPARK_COUNT; i++) {
            gpx[i] = RNG.nextFloat() * W;
            gpy[i] = RNG.nextFloat() * H;
            gpp[i] = RNG.nextFloat() * 6.28f;
            gps[i] = RNG.nextFloat() * 3f + 1f;
        }
        int cols = Math.max(1, W / 16);
        for (int i = 0; i < Math.min(COL_COUNT, cols); i++) {
            cx2[i] = i * (W / (float)Math.min(COL_COUNT, cols)) + 8;
            cy2[i] = RNG.nextFloat() * -H;
            csp[i] = RNG.nextFloat() * 1.2f + 0.3f;
            cch[i] = RNG.nextInt(CHARS.length());
        }
    }

    @Override
    public void tick() {
        tick++;
        fadeIn = Math.min(1f, fadeIn + 0.06f);

        if (loadProgress < 100f) {
            float spd = loadProgress < 30 ? 0.5f : loadProgress < 70 ? 0.35f : 0.22f;
            loadProgress = Math.min(100f, loadProgress + spd);
            msgIdx = Math.min(MESSAGES.length - 1, (int)(loadProgress / 100f * MESSAGES.length));
        } else if (!done) { done = true; }

        if (done) {
            doneTimer++;
            if (doneTimer > 55) {
                fadeOut = Math.min(1f, fadeOut + 0.038f);
                if (fadeOut >= 1f) MinecraftClient.getInstance().setScreen(null);
            }
        }

        glow += glowUp ? 0.032f : -0.032f;
        if (glow >= 1f) { glow = 1f; glowUp = false; }
        else if (glow <= 0f) { glow = 0f; glowUp = true; }

        float drift = tick * 0.012f;
        for (int i = 0; i < SNOW_COUNT; i++) {
            sy[i] += ssp[i];
            sx[i] += (float)Math.sin(drift + sph[i]) * 0.45f;
            sph[i] += 0.008f;
            if (sy[i] > height + 6) { sy[i] = -6; sx[i] = RNG.nextFloat() * width; }
            if (sx[i] < -6)  sx[i] = width + 5;
            if (sx[i] > width + 5) sx[i] = -6;
        }
        for (int i = 0; i < SPARK_COUNT; i++) gpp[i] += 0.055f;
        for (int i = 0; i < COL_COUNT; i++) {
            cy2[i] += csp[i];
            if (cy2[i] > height + 20) { cy2[i] = -20; cch[i] = RNG.nextInt(CHARS.length()); }
            if (RNG.nextInt(22) == 0)  cch[i] = RNG.nextInt(CHARS.length());
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float fade = fadeIn * (1f - fadeOut);
        int sa = (int)(fade * 255);
        if (sa <= 0) return;

        int W = width, H = height, pcx = W / 2, pcy = H / 2;

        // ── BACKGROUND ────────────────────────────────────────────────────
        ctx.fill(0, 0, W, H, BG);

        // Grid (subtle)
        for (int x = 0; x < W; x += 44) ctx.fill(x, 0, x + 1, H, fade(GRID_C, sa));
        for (int y = 0; y < H; y += 44) ctx.fill(0, y, W, y + 1, fade(GRID_C, sa));

        // Data streams (ice)
        for (int i = 0; i < COL_COUNT; i++) {
            if (cy2[i] < 0) continue;
            int a = (int)(55 * fade);
            if (a > 0) ctx.drawText(textRenderer, String.valueOf(CHARS.charAt(cch[i])), (int)cx2[i] - 4, (int)cy2[i], (a << 24) | 0x44AABB, false);
        }

        // ── SNOW ──────────────────────────────────────────────────────────
        for (int i = 0; i < SNOW_COUNT; i++) {
            float tw = (MathHelper.sin(sph[i]) + 1f) / 2f;
            int a = (int)(sal[i] * (0.5f + 0.5f * tw) * 255 * fade);
            if (a < 12) continue;
            int sz = (int)ssz[i];
            int col = (ssz[i] > 3.5f) ? SNOW_A : (ssz[i] > 2.5f) ? SNOW_B : 0xFFAACCDD;
            ctx.fill((int)sx[i], (int)sy[i], (int)sx[i] + sz, (int)sy[i] + sz, withAlpha(col, a));
            // Larger flakes get a center sparkle
            if (sz >= 3 && a > 120) {
                ctx.fill((int)sx[i] + 1, (int)sy[i] + 1, (int)sx[i] + sz - 1, (int)sy[i] + sz - 1, withAlpha(SNOW_A, Math.min(255, a + 60)));
            }
        }

        // Sparkle glitters
        for (int i = 0; i < SPARK_COUNT; i++) {
            float tw = (MathHelper.sin(gpp[i]) + 1f) / 2f;
            if (tw < 0.6f) continue;
            int a = (int)((tw - 0.6f) * 2.5f * 200 * fade);
            if (a < 20) continue;
            int sz = (int)gps[i];
            ctx.fill((int)gpx[i] - sz, (int)gpy[i], (int)gpx[i] + sz, (int)gpy[i] + 1, withAlpha(SNOW_A, a));
            ctx.fill((int)gpx[i], (int)gpy[i] - sz, (int)gpx[i] + 1, (int)gpy[i] + sz, withAlpha(SNOW_A, a));
        }

        // ── PANEL ─────────────────────────────────────────────────────────
        int pw = 440, ph = 280, px = pcx - pw / 2, py = pcy - ph / 2;

        // Drop shadow (deep)
        ctx.fill(px + 14, py + 14, px + pw + 14, py + ph + 14, fade(0xCC000000, sa));
        ctx.fill(px + 7,  py + 7,  px + pw + 7,  py + ph + 7,  fade(0x55000000, sa));

        // Panel background — SOLID opaque dark ice
        ctx.fill(px, py, px + pw, py + ph, fade(PANEL_BG, sa));

        // Subtle shimmer gradient (top → transparent)
        for (int y = 0; y < 60; y++) {
            int ga = (int)((1f - y / 60f) * 18 * fade);
            if (ga > 0) ctx.fill(px, py + y, px + pw, py + y + 1, (ga << 24) | 0xAADDFF);
        }

        // ── BORDER — 5px SOLID bright ice ─────────────────────────────────
        int bA = (int)((0.9f + 0.1f * glow) * 255 * fade);
        // Outer glow ring
        int glowA = (int)(0.25f * glow * 255 * fade);
        if (glowA > 0) {
            ctx.fill(px - 3, py - 3, px + pw + 3, py + 1,      withAlpha(BORDER, glowA));
            ctx.fill(px - 3, py + ph - 1, px + pw + 3, py + ph + 3, withAlpha(BORDER, glowA));
            ctx.fill(px - 3, py - 3, px + 1, py + ph + 3,      withAlpha(BORDER, glowA));
            ctx.fill(px + pw - 1, py - 3, px + pw + 3, py + ph + 3, withAlpha(BORDER, glowA));
        }
        // Main 5px border (solid bright ice)
        ctx.fill(px,     py,     px+pw,  py+5,    withAlpha(BORDER, bA));
        ctx.fill(px,     py+ph-5,px+pw,  py+ph,   withAlpha(BORDER, bA));
        ctx.fill(px,     py,     px+5,   py+ph,   withAlpha(BORDER, bA));
        ctx.fill(px+pw-5,py,     px+pw,  py+ph,   withAlpha(BORDER, bA));
        // Inner highlight line
        int ihA = (int)(0.45f * 255 * fade);
        ctx.fill(px+5, py+5, px+pw-5, py+6,      withAlpha(0xFFEEF8FF, ihA));
        ctx.fill(px+5, py+ph-6, px+pw-5, py+ph-5, withAlpha(0xFFEEF8FF, ihA));

        // GOLD corners — bigger
        int gcA = (int)(0.95f * 255 * fade);
        drawCorner(ctx, px, py, px + pw, py + ph, 30, withAlpha(GOLD, gcA));

        // ── TITLE ─────────────────────────────────────────────────────────
        int ty = py + 32;
        String t1 = "PROFESSOR", t2 = " CLIENT";
        int tw1 = textRenderer.getWidth(t1), tw2 = textRenderer.getWidth(t2);
        int startX = pcx - (tw1 + tw2) / 2;
        // Glow halo
        int gA = (int)((50 + 35 * glow) * fade);
        for (int d = -6; d <= 6; d++) {
            int g2 = Math.max(0, gA - Math.abs(d) * 7);
            if (g2 > 0) {
                ctx.drawText(textRenderer, t1 + t2, startX + d, ty, (g2 << 24) | 0x00CCFF, false);
                ctx.drawText(textRenderer, t1 + t2, startX, ty + d, (g2 << 24) | 0x00CCFF, false);
            }
        }
        int tA = (int)((220 + 35 * glow) * fade);
        ctx.drawText(textRenderer, t1, startX,      ty, withAlpha(TITLE1, tA), false);
        ctx.drawText(textRenderer, t2, startX + tw1, ty, withAlpha(TITLE2, tA), false);

        // Subtitle
        String sub = "❄  Frost Edition  v3.0  |  8 Bypass Modes  |  1.21.1  ❄";
        int sA = (int)((160 + 55 * glow) * fade);
        ctx.drawText(textRenderer, sub, pcx - textRenderer.getWidth(sub) / 2, ty + 13, withAlpha(CRYSTAL, sA), false);

        // ICE divider
        int divA = (int)(120 * fade);
        ctx.fill(px + 30, py + 52, px + pw - 30, py + 53, withAlpha(BORDER2, divA));

        // ── PROGRESS ──────────────────────────────────────────────────────
        int barX = px + 35, barY = py + 78, barW = pw - 70, barH = 12;
        String pctTxt = (int)loadProgress + "%";
        int pA = (int)(235 * fade);

        // Percentage text (bright)
        ctx.drawText(textRenderer, pctTxt, pcx - textRenderer.getWidth(pctTxt) / 2, barY - 15, withAlpha(TITLE1, pA), false);

        // Bar background (dark with bright border)
        ctx.fill(barX - 2, barY - 2, barX + barW + 2, barY + barH + 2, withAlpha(BORDER, (int)(140 * fade)));
        ctx.fill(barX, barY, barX + barW, barY + barH, fade(0xFF010E1E, sa));

        // Bar fill — deep blue → ice white
        int fillW = (int)(barW * loadProgress / 100f);
        for (int xi = 0; xi < fillW; xi++) {
            float fr = (float)xi / barW;
            int r = (int)MathHelper.lerp(fr, 10f, 170f);
            int g = (int)MathHelper.lerp(fr, 80f, 235f);
            ctx.fill(barX + xi, barY, barX + xi + 1, barY + barH, 0xFF000000 | (r << 16) | (g << 8) | 255);
        }
        // Bar cap glow
        if (fillW > 3) {
            int capA = (int)((180 + 75 * glow) * fade);
            ctx.fill(barX + fillW - 5, barY - 2, barX + fillW + 5, barY + barH + 2, withAlpha(TITLE1, capA));
        }

        // Segment dividers in bar
        for (int s = 1; s < 25; s++) {
            int sx2 = barX + barW * s / 25;
            ctx.fill(sx2, barY, sx2 + 1, barY + barH, fade(0x22000000, sa));
        }

        // Block tiles below bar
        int tileY = barY + barH + 6;
        int tiles = 28, tileW = (barW - tiles + 1) / tiles;
        for (int i = 0; i < tiles; i++) {
            int txp = barX + i * (tileW + 1);
            boolean filled = i < (int)(loadProgress / 100f * tiles);
            int tcA = (int)((filled ? 200 : 22) * fade);
            ctx.fill(txp, tileY, txp + tileW, tileY + 5, withAlpha(filled ? BORDER2 : 0xFF335566, tcA));
        }

        // Status message (bright, easy to read)
        String msg = MESSAGES[msgIdx];
        int mA = (int)((200 + 55 * glow) * fade);
        ctx.drawText(textRenderer, msg, pcx - textRenderer.getWidth(msg) / 2, tileY + 10, withAlpha(TXT_ICE, mA), false);

        // Blinking READY
        if (done && doneTimer % 22 < 11) {
            String ready = ">>>  ❄  READY  ❄  <<<";
            int rA = (int)(240 * fade);
            ctx.drawText(textRenderer, ready, pcx - textRenderer.getWidth(ready) / 2, tileY + 24, withAlpha(GOLD, rA), false);
        }

        // Footer
        int cpA = (int)(110 * fade);
        ctx.fill(px + 30, py + ph - 24, px + pw - 30, py + ph - 23, withAlpha(BORDER2, cpA / 2));
        String copy = "(c) Xerion Client  ❄  Frost Fabric Edition  ❄  1.21.1";
        ctx.drawText(textRenderer, copy, pcx - textRenderer.getWidth(copy) / 2, py + ph - 16, withAlpha(TXT_DIM, cpA), false);

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private static int withAlpha(int rgb, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0x00FFFFFF);
    }
    private static int fade(int argb, int sa) {
        int baseA = (argb >>> 24) & 0xFF;
        return withAlpha(argb, baseA * sa / 255);
    }
    private static void drawCorner(DrawContext ctx, int x1, int y1, int x2, int y2, int cs, int col) {
        ctx.fill(x1,    y1,    x1+cs, y1+5,  col); ctx.fill(x1,    y1,    x1+5,  y1+cs, col);
        ctx.fill(x2-cs, y1,    x2,    y1+5,  col); ctx.fill(x2-5,  y1,    x2,    y1+cs, col);
        ctx.fill(x1,    y2-5,  x1+cs, y2,    col); ctx.fill(x1,    y2-cs, x1+5,  y2,    col);
        ctx.fill(x2-cs, y2-5,  x2,    y2,    col); ctx.fill(x2-5,  y2-cs, x2,    y2,    col);
    }
}
