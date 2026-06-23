package com.professor.client.gui;

import com.professor.client.ProfessorClientMod;
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

    // ══ ICE PALETTE ═══════════════════════════════════════════════════════
    private static final int BG       = 0xFF010A14;
    private static final int PANEL_BG = 0xFF041828;
    private static final int BORDER   = 0xFFAAEEFF;
    private static final int BORDER2  = 0xFF55CCFF;
    private static final int TITLE1   = 0xFFEEF8FF;
    private static final int TITLE2   = 0xFFFFD700;
    private static final int TXT_ICE  = 0xFF88DDFF;
    private static final int TXT_DIM  = 0xFF4488AA;
    private static final int SNOW_A   = 0xFFFFFFFF;
    private static final int SNOW_B   = 0xFFCCEEFF;
    private static final int GRID_C   = 0xFF0A2A44;
    private static final int GOLD     = 0xFFFFD700;
    private static final int CRYSTAL  = 0xFF00CCEE;
    private static final int GREEN    = 0xFF00FF99;

    private static final Random RNG = new Random();

    // State
    private float   loadProgress = 0f;
    private int     msgIdx       = 0;
    private boolean done         = false;
    private int     doneTimer    = 0;
    private float   fadeIn       = 0f;
    private float   fadeOut      = 0f;
    private int     tick         = 0;
    private float   glow         = 0f;
    private boolean glowUp       = true;
    private float   hue          = 0f;
    private float   crystalSpin  = 0f;

    // Snow
    private static final int SNOW_COUNT = 250;
    private final float[] sx  = new float[SNOW_COUNT];
    private final float[] sy  = new float[SNOW_COUNT];
    private final float[] ssp = new float[SNOW_COUNT];
    private final float[] ssz = new float[SNOW_COUNT];
    private final float[] sal = new float[SNOW_COUNT];
    private final float[] sph = new float[SNOW_COUNT];

    // Sparkles
    private static final int SPARK_COUNT = 70;
    private final float[] gpx = new float[SPARK_COUNT];
    private final float[] gpy = new float[SPARK_COUNT];
    private final float[] gpp = new float[SPARK_COUNT];
    private final float[] gps = new float[SPARK_COUNT];

    // Data stream
    private static final int    COL_COUNT = 55;
    private final float[] cx2 = new float[COL_COUNT];
    private final float[] cy2 = new float[COL_COUNT];
    private final float[] csp = new float[COL_COUNT];
    private final int[]   cch = new int[COL_COUNT];
    private static final String CHARS = "XERION01FROST10BYPASS00ICE11CLIENT";

    // Pulse rings
    private final float[] ringR  = {8f, 50f, 100f, 160f, 220f};
    private final float[] ringSp = {1.8f, 1.3f, 0.9f, 0.65f, 0.45f};

    private static final String[] MESSAGES = {
        "Initializing Xerion Client  v4.0…",
        "Loading frost modules…  [✓]",
        "Calibrating bypass engine…  [✓]",
        "Injecting packet hooks…  [✓]",
        "Loading proxy manager…  [✓]",
        "Applying ice theme…  [✓]",
        "❄  All systems ready.  Welcome, operator.  ❄"
    };

    public ProfessorSplashScreen() { super(Text.literal("Xerion Client")); }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause()      { return false; }

    @Override
    protected void init() {
        int W = width, H = height;
        for (int i = 0; i < SNOW_COUNT; i++) {
            sx[i]  = RNG.nextFloat() * W; sy[i]  = RNG.nextFloat() * H;
            ssp[i] = RNG.nextFloat() * 0.8f + 0.22f;
            ssz[i] = RNG.nextFloat() * 4.5f + 1f;
            sal[i] = RNG.nextFloat() * 0.7f + 0.28f;
            sph[i] = RNG.nextFloat() * 6.28f;
        }
        for (int i = 0; i < SPARK_COUNT; i++) {
            gpx[i] = RNG.nextFloat() * W; gpy[i] = RNG.nextFloat() * H;
            gpp[i] = RNG.nextFloat() * 6.28f; gps[i] = RNG.nextFloat() * 3.5f + 1f;
        }
        int cols = Math.max(1, W / 14);
        for (int i = 0; i < Math.min(COL_COUNT, cols); i++) {
            cx2[i] = i * (W / (float)Math.min(COL_COUNT, cols)) + 7;
            cy2[i] = RNG.nextFloat() * -H;
            csp[i] = RNG.nextFloat() * 1.4f + 0.3f;
            cch[i] = RNG.nextInt(CHARS.length());
        }
    }

    @Override
    public void tick() {
        tick++;
        fadeIn = Math.min(1f, fadeIn + 0.055f);
        hue    = (hue + 0.004f) % 1f;
        crystalSpin += 0.018f;

        if (loadProgress < 100f) {
            float spd = loadProgress < 25 ? 0.55f : loadProgress < 65 ? 0.38f : 0.20f;
            loadProgress = Math.min(100f, loadProgress + spd);
            msgIdx = Math.min(MESSAGES.length - 1, (int)(loadProgress / 100f * MESSAGES.length));
        } else if (!done) { done = true; }

        if (done) {
            doneTimer++;
            if (doneTimer > 50) {
                fadeOut = Math.min(1f, fadeOut + 0.040f);
                if (fadeOut >= 1f) MinecraftClient.getInstance().setScreen(null);
            }
        }

        glow += glowUp ? 0.030f : -0.030f;
        if (glow >= 1f) { glow = 1f; glowUp = false; }
        else if (glow <= 0f) { glow = 0f; glowUp = true; }

        float drift = tick * 0.011f;
        for (int i = 0; i < SNOW_COUNT; i++) {
            sy[i] += ssp[i]; sx[i] += MathHelper.sin(drift + sph[i]) * 0.42f; sph[i] += 0.008f;
            if (sy[i] > height + 6) { sy[i] = -6; sx[i] = RNG.nextFloat() * width; }
            if (sx[i] < -6) sx[i] = width + 5;
            if (sx[i] > width + 5) sx[i] = -6;
        }
        for (int i = 0; i < SPARK_COUNT; i++) gpp[i] += 0.058f;
        for (int i = 0; i < ringR.length; i++) {
            ringR[i] += ringSp[i];
            if (ringR[i] > Math.max(width, height) * 0.85f) ringR[i] = 8f;
        }
        for (int i = 0; i < COL_COUNT; i++) {
            cy2[i] += csp[i];
            if (cy2[i] > height + 20) { cy2[i] = -20; cch[i] = RNG.nextInt(CHARS.length()); }
            if (RNG.nextInt(20) == 0) cch[i] = RNG.nextInt(CHARS.length());
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float fade = fadeIn * (1f - fadeOut);
        int   sa   = (int)(fade * 255);
        if (sa <= 0) return;

        int W = width, H = height, pcx = W/2, pcy = H/2;

        // ── Background ────────────────────────────────────────────────────
        ctx.fill(0, 0, W, H, BG);
        for (int x = 0; x < W; x += 42) ctx.fill(x, 0, x+1, H, fade(GRID_C, sa));
        for (int y = 0; y < H; y += 42) ctx.fill(0, y, W, y+1, fade(GRID_C, sa));

        // Expanding pulse rings from center
        for (int i = 0; i < ringR.length; i++) {
            int r = (int)ringR[i]; if (r < 2) continue;
            float ff = 1f - ringR[i] / (Math.max(W,H) * 0.85f);
            int ra = (int)(ff * ff * 45 * sa / 255); if (ra <= 0) continue;
            ctx.fill(pcx-r, pcy-1, pcx+r, pcy+1, withA(0xFF55CCFF, ra));
            ctx.fill(pcx-1, pcy-r, pcx+1, pcy+r, withA(0xFF55CCFF, ra));
        }

        // Data streams
        for (int i = 0; i < COL_COUNT; i++) {
            if (cy2[i] < 0) continue;
            int a = (int)(60 * fade); if (a <= 0) continue;
            ctx.drawText(textRenderer, String.valueOf(CHARS.charAt(cch[i])),
                (int)cx2[i]-4, (int)cy2[i], withA(0xFF44AABB, a), false);
        }

        // ── Snow ──────────────────────────────────────────────────────────
        for (int i = 0; i < SNOW_COUNT; i++) {
            float tw = (MathHelper.sin(sph[i]) + 1f) / 2f;
            int a = (int)(sal[i] * (0.45f + 0.55f * tw) * 255 * fade);
            if (a < 10) continue;
            int sz  = (int)ssz[i];
            int col = ssz[i] > 4f ? SNOW_A : ssz[i] > 2.8f ? SNOW_B : 0xFFAABBCC;
            ctx.fill((int)sx[i], (int)sy[i], (int)sx[i]+sz, (int)sy[i]+sz, withA(col, a));
            if (sz >= 3 && a > 110) ctx.fill((int)sx[i]+1,(int)sy[i]+1,(int)sx[i]+sz-1,(int)sy[i]+sz-1, withA(SNOW_A, Math.min(255, a+50)));
        }
        for (int i = 0; i < SPARK_COUNT; i++) {
            float tw = (MathHelper.sin(gpp[i]) + 1f) / 2f;
            if (tw < 0.58f) continue;
            int a = (int)((tw - 0.58f) * 2.38f * 210 * fade); if (a < 18) continue;
            int sz = (int)gps[i];
            ctx.fill((int)gpx[i]-sz,(int)gpy[i],(int)gpx[i]+sz+1,(int)gpy[i]+1, withA(SNOW_A, a));
            ctx.fill((int)gpx[i],(int)gpy[i]-sz,(int)gpx[i]+1,(int)gpy[i]+sz+1, withA(SNOW_A, a));
        }

        // ── Panel ─────────────────────────────────────────────────────────
        int pw = 460, ph = 295, px = pcx-pw/2, py = pcy-ph/2;
        ctx.fill(px+15, py+15, px+pw+15, py+ph+15, fade(0xCC000000, sa));
        ctx.fill(px+8,  py+8,  px+pw+8,  py+ph+8,  fade(0x55000000, sa));
        ctx.fill(px, py, px+pw, py+ph, fade(PANEL_BG, sa));
        for (int y = 0; y < 65; y++) {
            int ga = (int)((1f - y/65f) * 20 * fade);
            if (ga > 0) ctx.fill(px, py+y, px+pw, py+y+1, withA(0xAADDFF, ga));
        }

        // Border
        int bA  = (int)((0.88f + 0.12f * glow) * 255 * fade);
        int ogA = (int)(0.30f * glow * 255 * fade);
        if (ogA > 0) {
            ctx.fill(px-4, py-4, px+pw+4, py+1,     withA(BORDER, ogA));
            ctx.fill(px-4, py+ph-1, px+pw+4, py+ph+4, withA(BORDER, ogA));
            ctx.fill(px-4, py-4, px+1, py+ph+4,     withA(BORDER, ogA));
            ctx.fill(px+pw-1, py-4, px+pw+4, py+ph+4, withA(BORDER, ogA));
        }
        ctx.fill(px,     py,      px+pw,   py+5,    withA(BORDER, bA));
        ctx.fill(px,     py+ph-5, px+pw,   py+ph,   withA(BORDER, bA));
        ctx.fill(px,     py,      px+5,    py+ph,   withA(BORDER, bA));
        ctx.fill(px+pw-5,py,      px+pw,   py+ph,   withA(BORDER, bA));
        // Inner highlight
        int ihA = (int)(0.42f * 255 * fade);
        ctx.fill(px+5, py+5, px+pw-5, py+6,       withA(TITLE1, ihA));
        ctx.fill(px+5, py+ph-6, px+pw-5, py+ph-5, withA(TITLE1, ihA));
        // Gold corners
        int gcA = (int)(0.95f * 255 * fade);
        drawCorner(ctx, px, py, px+pw, py+ph, 32, withA(GOLD, gcA));
        // Crystal shards at corners
        int csA = (int)(140 * fade);
        drawShard(ctx, px+5,      py+5,      withA(CRYSTAL, csA));
        drawShard(ctx, px+pw-14,  py+5,      withA(CRYSTAL, csA));
        drawShard(ctx, px+5,      py+ph-18,  withA(CRYSTAL, csA));
        drawShard(ctx, px+pw-14,  py+ph-18,  withA(CRYSTAL, csA));

        // ── Title ─────────────────────────────────────────────────────────
        int ty = py + 28;
        String t1 = "XERION", t2 = " CLIENT";
        int tw1 = textRenderer.getWidth(t1), tw2 = textRenderer.getWidth(t2);
        int startX = pcx - (tw1+tw2)/2;
        // Glow halo
        int gA = (int)((55 + 40 * glow) * fade);
        for (int d = -5; d <= 5; d++) {
            int g2 = Math.max(0, gA - Math.abs(d) * 8);
            if (g2 > 0) { ctx.drawText(textRenderer, t1+t2, startX+d, ty, withA(0x00CCFF, g2), false);
                          ctx.drawText(textRenderer, t1+t2, startX, ty+d, withA(0x00CCFF, g2), false); }
        }
        int tA = (int)((225 + 30 * glow) * fade);
        ctx.drawText(textRenderer, t1, startX,       ty, withA(TITLE1, tA), false);
        ctx.drawText(textRenderer, t2, startX + tw1, ty, withA(TITLE2, tA), false);

        // Subtitle
        String sub = "❄  Frost Edition  " + ProfessorClientMod.VERSION + "  ·  Fabric 1.21.1  ❄";
        int sA = (int)((155 + 60 * glow) * fade);
        ctx.drawText(textRenderer, sub, pcx - textRenderer.getWidth(sub)/2, ty+13, withA(CRYSTAL, sA), false);

        // Divider
        ctx.fill(px+28, py+50, px+pw-28, py+51, withA(BORDER2, (int)(115 * fade)));

        // ── Progress bar ──────────────────────────────────────────────────
        int barX = px+32, barY = py+75, barW = pw-64, barH = 13;
        String pct = (int)loadProgress + "%";
        ctx.drawText(textRenderer, pct, pcx - textRenderer.getWidth(pct)/2, barY-16, withA(TITLE1, (int)(240*fade)), false);

        ctx.fill(barX-2, barY-2, barX+barW+2, barY+barH+2, withA(BORDER, (int)(145*fade)));
        ctx.fill(barX, barY, barX+barW, barY+barH, fade(0xFF010E1E, sa));

        int fillW = (int)(barW * loadProgress / 100f);
        for (int xi = 0; xi < fillW; xi++) {
            float fr = (float)xi / barW;
            int r = (int)MathHelper.lerp(fr, 8f, 160f);
            int g = (int)MathHelper.lerp(fr, 75f, 230f);
            ctx.fill(barX+xi, barY, barX+xi+1, barY+barH, 0xFF000000|(r<<16)|(g<<8)|255);
        }
        if (fillW > 4) {
            int capA = (int)((185 + 70 * glow) * fade);
            ctx.fill(barX+fillW-6, barY-2, barX+fillW+6, barY+barH+2, withA(TITLE1, capA));
        }
        for (int s = 1; s < 28; s++) { int sx2 = barX + barW*s/28; ctx.fill(sx2, barY, sx2+1, barY+barH, fade(0x22000000, sa)); }

        // Block tiles
        int tileY = barY + barH + 6;
        int tiles = 32, tileW = (barW - tiles + 1) / tiles;
        for (int i = 0; i < tiles; i++) {
            int txp = barX + i*(tileW+1);
            boolean filled = i < (int)(loadProgress / 100f * tiles);
            ctx.fill(txp, tileY, txp+tileW, tileY+5, withA(filled ? BORDER2 : 0xFF335566, (int)((filled?200:22)*fade)));
        }

        // Status message
        String msg = MESSAGES[msgIdx];
        ctx.drawText(textRenderer, msg, pcx - textRenderer.getWidth(msg)/2, tileY+10, withA(TXT_ICE, (int)((200+55*glow)*fade)), false);

        // READY blink
        if (done && doneTimer % 20 < 10) {
            String ready = ">>>  ❄  READY  ❄  <<<";
            ctx.drawText(textRenderer, ready, pcx - textRenderer.getWidth(ready)/2, tileY+24, withA(GOLD, (int)(245*fade)), false);
        }

        // Footer
        ctx.fill(px+28, py+ph-22, px+pw-28, py+ph-21, withA(BORDER2, (int)(55*fade)));
        String copy = ProfessorClientMod.CLIENT_NAME + "  " + ProfessorClientMod.VERSION + "  ❄  Frost Fabric Edition  ❄  1.21.1";
        ctx.drawText(textRenderer, copy, pcx - textRenderer.getWidth(copy)/2, py+ph-14, withA(TXT_DIM, (int)(115*fade)), false);

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private static int withA(int rgb, int a) { return (Math.max(0,Math.min(255,a))<<24)|(rgb&0x00FFFFFF); }
    private static int fade(int argb, int sa) { int ba=(argb>>>24)&0xFF; return withA(argb, ba*sa/255); }
    private static void drawCorner(DrawContext ctx, int x1, int y1, int x2, int y2, int cs, int col) {
        ctx.fill(x1,y1,x1+cs,y1+5,col); ctx.fill(x1,y1,x1+5,y1+cs,col);
        ctx.fill(x2-cs,y1,x2,y1+5,col); ctx.fill(x2-5,y1,x2,y1+cs,col);
        ctx.fill(x1,y2-5,x1+cs,y2,col); ctx.fill(x1,y2-cs,x1+5,y2,col);
        ctx.fill(x2-cs,y2-5,x2,y2,col); ctx.fill(x2-5,y2-cs,x2,y2,col);
    }
    private static void drawShard(DrawContext ctx, int x, int y, int col) {
        ctx.fill(x+3,y,   x+5,y+2,  col);
        ctx.fill(x+2,y+2, x+6,y+4,  col);
        ctx.fill(x+1,y+4, x+7,y+6,  col);
        ctx.fill(x+2,y+6, x+6,y+8,  col);
        ctx.fill(x+3,y+8, x+5,y+10, col);
    }
}
