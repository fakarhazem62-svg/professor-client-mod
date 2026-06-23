package com.professor.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

/**
 * Short activation/loading screen shown every time the key is pressed.
 * Lasts ~2 seconds then transitions to ProfessorScreen.
 */
@Environment(EnvType.CLIENT)
public class KeyActivationScreen extends Screen {

    private float  progress   = 0f;
    private float  fadeIn     = 0f;
    private float  fadeOut    = 0f;
    private boolean closing   = false;
    private int    renderFrame = 0;

    private float   glowPulse = 0f;
    private boolean glowUp    = true;
    private float   scanLine  = 0f;
    private float   hueShift  = 0f;

    private static final String CHARS = "PROFESSOR01AKASATANA10110100";
    private static final int    COLS  = 60;
    private float[] rainX, rainY, rainSpd;
    private int[]   rainCh;

    private float[] ringR   = new float[6];
    private float[] ringSpd = {1.4f, 1.1f, 0.9f, 0.7f, 0.5f, 0.35f};
    private int[]   ringCol = {0x00F5FF, 0x9B00FF, 0x00F5FF, 0xFFD700, 0x9B00FF, 0x00F5FF};

    private static final int PCNT = 120;
    private float[] px, py, pvx, pvy, palp, pph;
    private int[]   pct;

    private static final String[] MSGS = {
        "Connecting modules...",
        "Initializing bypass engine...",
        "Loading packet hooks...",
        "Calibrating anti-cheat bypass...",
        "Ready."
    };

    private final Random rng = new Random();

    public KeyActivationScreen() {
        super(Text.literal("Professor Client — Activating"));
    }

    @Override
    protected void init() {
        initRain();
        initRings();
        initParticles();
    }

    @Override public boolean shouldCloseOnEsc() { return true; }
    @Override public boolean shouldPause()      { return false; }

    private void initRain() {
        rainX = new float[COLS]; rainY = new float[COLS];
        rainSpd = new float[COLS]; rainCh = new int[COLS];
        for (int i = 0; i < COLS; i++) {
            rainX[i]   = (i + 0.5f) * ((float) width / COLS);
            rainY[i]   = rng.nextFloat() * -height;
            rainSpd[i] = rng.nextFloat() * 1.8f + 0.6f;
            rainCh[i]  = rng.nextInt(CHARS.length());
        }
    }

    private void initRings() {
        for (int i = 0; i < ringR.length; i++) ringR[i] = i * 50f + 10f;
    }

    private void initParticles() {
        px  = new float[PCNT]; py  = new float[PCNT];
        pvx = new float[PCNT]; pvy = new float[PCNT];
        palp = new float[PCNT]; pph = new float[PCNT];
        pct  = new int[PCNT];
        for (int i = 0; i < PCNT; i++) resetParticle(i, true);
    }

    private void resetParticle(int i, boolean randY) {
        px[i]   = rng.nextFloat() * width;
        py[i]   = randY ? rng.nextFloat() * height : height + 5;
        pvx[i]  = (rng.nextFloat() - 0.5f) * 0.5f;
        pvy[i]  = -(rng.nextFloat() * 0.8f + 0.15f);
        palp[i] = rng.nextFloat() * 0.9f + 0.1f;
        pph[i]  = rng.nextFloat() * 6.28f;
        pct[i]  = rng.nextInt(3);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderFrame++;

        if (!closing) {
            fadeIn   = Math.min(1f, fadeIn + 0.07f);
            progress = Math.min(1f, progress + 0.013f);
            if (progress >= 1f) closing = true;
        } else {
            fadeOut = Math.min(1f, fadeOut + 0.09f);
            if (fadeOut >= 1f) {
                MinecraftClient.getInstance().setScreen(new ProfessorScreen());
                return;
            }
        }

        float alpha = fadeIn * (1f - fadeOut);
        int   sa    = (int) (alpha * 255);
        if (sa <= 0) return;

        int W = width, H = height, cx = W / 2, cy = H / 2;

        glowPulse += glowUp ? 0.035f : -0.035f;
        if      (glowPulse >= 1f) { glowPulse = 1f; glowUp = false; }
        else if (glowPulse <= 0f) { glowPulse = 0f; glowUp = true; }

        scanLine = (scanLine + 2f) % H;
        hueShift = (hueShift + 0.008f) % 1f;

        for (int i = 0; i < COLS; i++) {
            rainY[i] += rainSpd[i];
            if (rainY[i] > H + 20) { rainY[i] = -20; rainCh[i] = rng.nextInt(CHARS.length()); }
            if (rng.nextInt(25) == 0) rainCh[i] = rng.nextInt(CHARS.length());
        }

        for (int i = 0; i < ringR.length; i++) {
            ringR[i] += ringSpd[i];
            if (ringR[i] > Math.max(W, H) * 0.8f) ringR[i] = 10f;
        }

        for (int i = 0; i < PCNT; i++) {
            px[i] += pvx[i]; py[i] += pvy[i]; pph[i] += 0.04f;
            if (py[i] < -10) resetParticle(i, false);
            if (px[i] < 0) px[i] = W;
            if (px[i] > W) px[i] = 0;
        }

        // Background
        ctx.fill(0, 0, W, H, blA(0xFF000005, sa));

        // Grid
        int ga = sc(0x06, sa);
        if (ga > 0) {
            for (int x = 0; x < W; x += 55) ctx.fill(x, 0, x + 1, H, (ga << 24) | 0x00F5FF);
            for (int y = 0; y < H; y += 55) ctx.fill(0, y, W, y + 1, (ga << 24) | 0x00F5FF);
        }

        // Rings
        for (int i = 0; i < ringR.length; i++) {
            int r = (int) ringR[i]; if (r < 2) continue;
            float ff = 1f - ringR[i] / (Math.max(W, H) * 0.8f);
            int ra = (int) (ff * ff * 55 * sa / 255); if (ra <= 0) continue;
            ctx.fill(cx - r, cy - 1, cx + r, cy + 1, (ra << 24) | ringCol[i]);
            ctx.fill(cx - 1, cy - r, cx + 1, cy + r, (ra << 24) | ringCol[i]);
        }

        // Rain
        for (int i = 0; i < COLS; i++) {
            if (rainY[i] < 0) continue;
            int ra = sc(60 + (int) (40 * glowPulse), sa);
            ctx.drawText(textRenderer, String.valueOf(CHARS.charAt(rainCh[i])),
                    (int) rainX[i] - 4, (int) rainY[i], (ra << 24) | 0x00F5FF, false);
            if (rainY[i] > 14)
                ctx.drawText(textRenderer, String.valueOf(CHARS.charAt(rainCh[i])),
                        (int) rainX[i] - 4, (int) rainY[i] - 14, (ra / 3 << 24) | 0x003344, false);
        }

        // Particles
        for (int i = 0; i < PCNT; i++) {
            float tw = (MathHelper.sin(pph[i]) + 1f) / 2f;
            int a = (int) (palp[i] * tw * sa); if (a < 10) continue;
            int col = switch (pct[i]) {
                case 1  -> (a << 24) | 0x9B00FF;
                case 2  -> (a << 24) | 0xFFD700;
                default -> (a << 24) | 0x00F5FF;
            };
            ctx.fill((int) px[i], (int) py[i], (int) px[i] + 2, (int) py[i] + 2, col);
        }

        // Scanline
        int sla = sc(0x10, sa);
        if (sla > 0) ctx.fill(0, (int) scanLine, W, (int) scanLine + 2, (sla << 24) | 0xFFFFFF);

        // ── Center panel ─────────────────────────────────────────────────
        int pw = 340, ph = 200;
        int ppx = cx - pw / 2, ppy2 = cy - ph / 2;

        ctx.fill(ppx + 8,  ppy2 + 8,  ppx + pw + 8, ppy2 + ph + 8, blA(0x66000000, sa));
        ctx.fill(ppx, ppy2, ppx + pw, ppy2 + ph, blA(0xEE07000F, sa));

        // Animated border
        float t = renderFrame * 0.05f;
        int bR = (int) (Math.abs(Math.sin(t)) * 120);
        int bG = Math.max(0, Math.min(255, (int) (200 + 55 * Math.sin(t + 2.1))));
        int bA = sc((int) ((0.7f + 0.3f * glowPulse) * 255), sa);
        int bc = (bA << 24) | (bR << 16) | (bG << 8) | 255;
        ctx.fill(ppx,      ppy2,       ppx + pw, ppy2 + 2,   bc);
        ctx.fill(ppx,      ppy2+ph-2,  ppx + pw, ppy2 + ph,  bc);
        ctx.fill(ppx,      ppy2,       ppx + 2,  ppy2 + ph,  bc);
        ctx.fill(ppx+pw-2, ppy2,       ppx + pw, ppy2 + ph,  bc);

        // Gold corners
        int gca = sc(0xCC, sa); int gcc = (gca << 24) | 0xFFD700; int cs = 18;
        ctx.fill(ppx,       ppy2,       ppx+cs,   ppy2+2,    gcc); ctx.fill(ppx,      ppy2,       ppx+2,    ppy2+cs,   gcc);
        ctx.fill(ppx+pw-cs, ppy2,       ppx+pw,   ppy2+2,    gcc); ctx.fill(ppx+pw-2, ppy2,       ppx+pw,   ppy2+cs,   gcc);
        ctx.fill(ppx,       ppy2+ph-2,  ppx+cs,   ppy2+ph,   gcc); ctx.fill(ppx,      ppy2+ph-cs, ppx+2,    ppy2+ph,   gcc);
        ctx.fill(ppx+pw-cs, ppy2+ph-2,  ppx+pw,   ppy2+ph,   gcc); ctx.fill(ppx+pw-2, ppy2+ph-cs, ppx+pw,   ppy2+ph,   gcc);

        // Title
        int ta = sc((int) (180 + 75 * glowPulse), sa);
        String t1 = "PROFESSOR", t2 = " CLIENT";
        int tw1 = textRenderer.getWidth(t1), tw2 = textRenderer.getWidth(t2);
        int tx = cx - (tw1 + tw2) / 2, ty = ppy2 + 30;
        for (int d = -3; d <= 3; d++) {
            int ga2 = Math.max(0, sc(16 - Math.abs(d) * 4, sa));
            if (ga2 > 0) ctx.drawText(textRenderer, t1 + t2, tx + d, ty, (ga2 << 24) | 0x00F5FF, false);
        }
        ctx.drawText(textRenderer, t1, tx,      ty, (ta << 24) | 0x00F5FF, false);
        ctx.drawText(textRenderer, t2, tx + tw1, ty, (ta << 24) | 0xFFD700, false);

        String sub = "v3.0  ●  Activating...";
        int suba = sc((int) (100 + 80 * glowPulse), sa);
        ctx.drawText(textRenderer, sub, cx - textRenderer.getWidth(sub) / 2, ty + 12, (suba << 24) | 0x9B00FF, false);

        // Divider
        ctx.fill(ppx + 28, ppy2 + 50, ppx + pw - 28, ppy2 + 51, sc(0x55, sa) << 24 | 0xFFD700);

        // Status message
        int msgIdx = Math.min(MSGS.length - 1, (int) (progress * MSGS.length));
        String msg = MSGS[msgIdx];
        int ma = sc((int) (160 + 60 * glowPulse), sa);
        ctx.drawText(textRenderer, msg, cx - textRenderer.getWidth(msg) / 2, ppy2 + 65, (ma << 24) | 0x00CCFF, false);

        // Progress bar
        int barX = ppx + 28, barY = ppy2 + 83, barW = pw - 56, barH = 10;
        String pctStr = (int) (progress * 100) + "%";
        ctx.drawText(textRenderer, pctStr, cx - textRenderer.getWidth(pctStr) / 2, barY - 13, sc(200, sa) << 24 | 0x00F5FF, false);
        ctx.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, sc(0x55, sa) << 24 | 0x00F5FF);
        ctx.fill(barX, barY, barX + barW, barY + barH, sc(0x18, sa) << 24 | 0x000000);

        int fillW = (int) (barW * progress);
        for (int xi = 0; xi < fillW; xi++) {
            float frac = (float) xi / barW;
            int br = (int) MathHelper.lerp(frac, 155f, 0f);
            int bg = (int) MathHelper.lerp(frac, 0f, 245f);
            ctx.fill(barX + xi, barY, barX + xi + 1, barY + barH, 0xFF000000 | (br << 16) | (bg << 8) | 255);
        }
        if (fillW > 0) {
            int cap = sc((int) (160 + 80 * glowPulse), sa);
            ctx.fill(barX + fillW - 2, barY, barX + fillW + 2, barY + barH, (cap << 24) | 0xFFFFFF);
        }

        // Segments
        for (int s = 1; s < 20; s++) {
            int sx = barX + barW * s / 20;
            ctx.fill(sx, barY, sx + 1, barY + barH, sc(0x30, sa) << 24 | 0x000000);
        }

        // Hint
        String hint = "Press  ESC  to cancel";
        ctx.drawText(textRenderer, hint, cx - textRenderer.getWidth(hint) / 2,
                ppy2 + ph - 18, sc(70, sa) << 24 | 0x556677, false);

        super.render(ctx, mx, my, delta);
    }

    private int sc(int a, int sa) { return Math.max(0, Math.min(255, a * sa / 255)); }
    private int blA(int col, int sa) {
        int a = ((col >> 24) & 0xFF) * sa / 255;
        return (a << 24) | (col & 0x00FFFFFF);
    }
}
