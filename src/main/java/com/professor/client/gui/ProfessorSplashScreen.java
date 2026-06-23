package com.professor.client.gui;

import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class ProfessorSplashScreen extends Screen {

    private static final Random RNG = new Random();

    // State
    private int  tickCount    = 0;
    private float loadProgress = 0f;
    private int  messageIndex = 0;
    private boolean done      = false;
    private int  doneTimer    = 0;
    private float fadeAlpha   = 0f;
    private boolean fadingIn  = true;
    private float fadeOut     = 0f;

    // Glow pulse
    private float glowPulse = 0f;
    private boolean glowUp  = true;

    // Scanline
    private float scanLine = 0f;

    // Blue particles  [x, y, vx, vy, size, alpha, phase]
    private final List<float[]> particles = new ArrayList<>();

    // Hex grid cells  [x, y, phase]
    private final List<float[]> hexCells = new ArrayList<>();

    // Data stream columns  [x, y, speed, char]
    private final List<float[]> dataStreams = new ArrayList<>();

    // Rings  [x, y, radius, speed, alpha]
    private final List<float[]> rings = new ArrayList<>();

    private static final String[] MESSAGES = {
        "Initializing Professor Client...",
        "Loading modules...",
        "Bypassing anti-cheat...",
        "Injecting hooks...",
        "Loading GUI components...",
        "Connecting systems...",
        "All systems ready."
    };

    private static final String CHARS = "PROFESSOR01アイウエオカキクケコ";

    public ProfessorSplashScreen() {
        super(Text.literal("Professor Client"));
    }

    @Override
    protected void init() {
        initParticles();
        initDataStreams();
        initRings();
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause()      { return false; }

    // ── Init helpers ───────────────────────────────────────────────────────

    private void initParticles() {
        particles.clear();
        for (int i = 0; i < 220; i++) {
            particles.add(new float[]{
                RNG.nextFloat() * width,
                RNG.nextFloat() * height,
                (RNG.nextFloat() - 0.5f) * 0.5f,
                -(RNG.nextFloat() * 0.7f + 0.15f),
                RNG.nextFloat() * 2.2f + 0.6f,
                RNG.nextFloat() * 0.8f + 0.2f,
                RNG.nextFloat() * 6.28f
            });
        }
    }

    private void initDataStreams() {
        dataStreams.clear();
        int cols = Math.max(1, width / 16);
        for (int i = 0; i < cols; i++) {
            dataStreams.add(new float[]{
                i * 16 + 8f,
                RNG.nextFloat() * -height,
                RNG.nextFloat() * 1.2f + 0.5f,
                RNG.nextInt(CHARS.length())
            });
        }
    }

    private void initRings() {
        rings.clear();
        for (int i = 0; i < 6; i++) {
            rings.add(new float[]{
                width  * 0.5f,
                height * 0.5f,
                i * 40f + 20f,
                0.4f + i * 0.1f,
                0.4f + RNG.nextFloat() * 0.4f
            });
        }
    }

    // ── Tick ───────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        tickCount++;

        // Fade in
        if (fadingIn) {
            fadeAlpha = Math.min(1f, fadeAlpha + 0.055f);
            if (fadeAlpha >= 1f) fadingIn = false;
        }

        // Progress
        if (loadProgress < 100f) {
            loadProgress = Math.min(loadProgress + 0.42f, 100f);
            messageIndex = Math.min(
                (int)(loadProgress / 100f * (MESSAGES.length - 1)),
                MESSAGES.length - 1
            );
        } else if (!done) {
            done = true;
        }

        if (done) {
            doneTimer++;
            if (doneTimer > 55) {
                fadeOut = Math.min(1f, fadeOut + 0.045f);
                if (fadeOut >= 1f) MinecraftClient.getInstance().setScreen(null);
            }
        }

        // Glow pulse
        glowPulse += glowUp ? 0.04f : -0.04f;
        if      (glowPulse >= 1f) { glowPulse = 1f; glowUp = false; }
        else if (glowPulse <= 0f) { glowPulse = 0f; glowUp = true;  }

        // Scanline
        scanLine = (scanLine + 1.8f) % height;

        // Particles
        for (float[] p : particles) {
            p[0] += p[2]; p[1] += p[3]; p[6] += 0.05f;
            if (p[1] < -10) { p[1] = height + 5; p[0] = RNG.nextFloat() * width; }
            if (p[0] < 0)   p[0] = width;
            if (p[0] > width) p[0] = 0;
        }

        // Data streams
        for (float[] d : dataStreams) {
            d[1] += d[2];
            if (d[1] > height + 20) { d[1] = -24; d[3] = RNG.nextInt(CHARS.length()); }
            if (RNG.nextInt(25) == 0) d[3] = RNG.nextInt(CHARS.length());
        }

        // Rings
        for (float[] r : rings) {
            r[2] = (r[2] + r[3]) % (Math.max(width, height) * 0.8f);
        }
    }

    // ── Render ─────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float screenFade = (1f - fadeOut) * fadeAlpha;
        int   sa         = (int)(screenFade * 255);
        if (sa <= 0) return;

        int W = width, H = height, cx = W / 2, cy = H / 2;

        // Dark-blue background
        ctx.fill(0, 0, W, H, alphaBlend(0xFF000A14, sa));

        // Background layers
        drawGrid(ctx, W, H, sa);
        drawRadialGlow(ctx, cx, cy, sa);
        drawDataStreams(ctx, sa);
        drawParticles(ctx, sa);
        drawRings(ctx, cx, cy, sa);

        // Scanline
        int slA = (int)(0x0A * sa / 255);
        if (slA > 0) ctx.fill(0, (int)scanLine, W, (int)scanLine + 2, (slA << 24) | 0xFFFFFF);

        // Panel
        drawPanel(ctx, cx, cy, W, H, sa);

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── Draw helpers ───────────────────────────────────────────────────────

    private void drawGrid(DrawContext ctx, int W, int H, int sa) {
        int a = (int)(0x07 * sa / 255);
        if (a <= 0) return;
        int col = (a << 24) | 0x0055BB;
        for (int x = 0; x < W; x += 44) ctx.fill(x, 0, x + 1, H, col);
        for (int y = 0; y < H; y += 44) ctx.fill(0, y, W, y + 1, col);
    }

    private void drawRadialGlow(DrawContext ctx, int cx, int cy, int sa) {
        int[] radii  = {340, 260, 180, 110, 66};
        int[] alphas = { 5,   9,  15,  22,  30};
        for (int i = 0; i < radii.length; i++) {
            int r = radii[i];
            int a = (int)(alphas[i] * (0.5f + 0.5f * glowPulse) * sa / 255);
            ctx.fill(cx - r, cy - r / 2, cx + r, cy + r / 2, (a << 24) | 0x0066CC);
        }
    }

    private void drawDataStreams(DrawContext ctx, int sa) {
        for (float[] d : dataStreams) {
            if (d[1] < 0) continue;
            int hA = (int)((50 + 40 * glowPulse) * sa / 255);
            ctx.drawText(textRenderer,
                String.valueOf(CHARS.charAt((int)d[3])),
                (int)d[0] - 4, (int)d[1],
                (Math.min(255, hA * 2) << 24) | 0x00A8FF, false);
            if (d[1] > 14)
                ctx.drawText(textRenderer,
                    String.valueOf(CHARS.charAt((int)d[3])),
                    (int)d[0] - 4, (int)d[1] - 14,
                    (hA << 24) | 0x0044AA, false);
        }
    }

    private void drawParticles(DrawContext ctx, int sa) {
        for (float[] p : particles) {
            float twinkle = (MathHelper.sin(p[6]) + 1f) / 2f;
            int a = (int)(p[5] * twinkle * sa);
            if (a < 10) continue;
            int col = (a << 24) | (p[4] > 1.8f ? 0x00A8FF : 0x0055AA);
            int sz  = p[4] > 2f ? 2 : 1;
            ctx.fill((int)p[0], (int)p[1], (int)p[0] + sz, (int)p[1] + sz, col);
        }
    }

    private void drawRings(DrawContext ctx, int cx, int cy, int sa) {
        for (float[] r : rings) {
            int rad = (int)r[2];
            if (rad < 2) continue;
            int a = (int)(r[4] * (1f - r[2] / (Math.max(width, height) * 0.8f)) * sa / 255 * 40);
            if (a <= 0) continue;
            int col = (a << 24) | 0x0088CC;
            // Draw 4 corners of ring as line segments
            ctx.fill(cx - rad, cy - 1, cx + rad, cy + 1, (a/4 << 24) | 0x0088CC);
            ctx.fill(cx - 1, cy - rad, cx + 1, cy + rad, (a/4 << 24) | 0x0088CC);
        }
    }

    private void drawPanel(DrawContext ctx, int cx, int cy, int W, int H, int sa) {
        int pw = 320, ph = 200;
        int px = cx - pw / 2, py = cy - ph / 2;

        // Shadow
        int shA = (int)(0x50 * sa / 255);
        ctx.fill(px + 6, py + 6, px + pw + 6, py + ph + 6, (shA << 24));

        // BG
        ctx.fill(px, py, px + pw, py + ph, alphaBlend(0xF0000818, sa));

        // Animated border
        float t = (float)(tickCount * 0.04);
        int bR = Math.max(0, (int)(20 + 40  * Math.sin(t)));
        int bG = Math.max(0, (int)(120 + 80 * Math.sin(t + 2.1)));
        int bB = Math.max(0, (int)(220 + 35 * Math.sin(t + 4.2)));
        int bA = (int)((0.55f + 0.45f * glowPulse) * sa);
        int bc = (bA << 24) | (bR << 16) | (bG << 8) | bB;
        ctx.fill(px,        py,        px + pw,  py + 2,    bc);
        ctx.fill(px,        py+ph-2,   px + pw,  py + ph,   bc);
        ctx.fill(px,        py,        px + 2,   py + ph,   bc);
        ctx.fill(px+pw-2,   py,        px + pw,  py + ph,   bc);

        // Corner brackets
        int cA = (int)(0xCC * sa / 255);
        int cc = (cA << 24) | 0x00D4FF;
        int cl = 16;
        ctx.fill(px,       py,       px+cl, py+2,  cc); ctx.fill(px,       py,       px+2, py+cl, cc);
        ctx.fill(px+pw-cl, py,       px+pw, py+2,  cc); ctx.fill(px+pw-2,  py,       px+pw,py+cl, cc);
        ctx.fill(px,       py+ph-2,  px+cl, py+ph, cc); ctx.fill(px,       py+ph-cl, px+2, py+ph, cc);
        ctx.fill(px+pw-cl, py+ph-2,  px+pw, py+ph, cc); ctx.fill(px+pw-2,  py+ph-cl, px+pw,py+ph, cc);

        // ── Title ──
        int ty = py + 22;
        String title = "PROFESSOR CLIENT";
        int tw = textRenderer.getWidth(title);
        // Glow layers
        int gA = (int)((32 + 22 * glowPulse) * sa / 255);
        for (int dx = -3; dx <= 3; dx++) {
            int g2 = Math.max(0, gA - Math.abs(dx) * 8);
            if (g2 <= 0) continue;
            ctx.drawText(textRenderer, title, cx - tw/2 + dx, ty,     (g2 << 24) | 0x0066BB, false);
            ctx.drawText(textRenderer, title, cx - tw/2,      ty + dx, (g2 << 24) | 0x0066BB, false);
        }
        int tA = (int)((200 + 55 * glowPulse) * sa / 255);
        ctx.drawText(textRenderer, title, cx - tw/2, ty, (tA << 24) | 0x00D4FF, true);

        // Subtitle
        String sub = "v2.0  •  Fabric 1.21.1";
        int sA = (int)((110 + 55 * glowPulse) * sa / 255);
        ctx.drawText(textRenderer, sub, cx - textRenderer.getWidth(sub)/2, ty + 13, (sA << 24) | 0x0077AA, false);

        // Divider
        ctx.fill(px + 24, py + 42, px + pw - 24, py + 43, 0x44004488);

        // ── Progress bar ──
        int barX = px + 26, barY = py + 62, barW = pw - 52, barH = 7;

        String pct = (int)loadProgress + "%";
        int pA = (int)(180 * sa / 255);
        ctx.drawText(textRenderer, pct, cx - textRenderer.getWidth(pct)/2, barY - 12, (pA << 24) | 0x00AAEE, false);

        // Track
        ctx.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0x44004488);
        ctx.fill(barX, barY, barX + barW, barY + barH, 0x1A001133);

        // Fill gradient (dark-blue → cyan)
        int fillW = (int)(barW * loadProgress / 100f);
        if (fillW > 0) {
            for (int xi = 0; xi < fillW; xi++) {
                float frac = (float)xi / barW;
                int gg = (int)MathHelper.lerp(frac, 44f, 168f);
                int bb = (int)MathHelper.lerp(frac, 160f, 255f);
                ctx.fill(barX + xi, barY, barX + xi + 1, barY + barH, 0xFF000000 | (gg << 8) | bb);
            }
            // Glow cap
            int gcA = (int)(120 * glowPulse * sa / 255);
            ctx.fill(barX + fillW - 5, barY - 2, barX + fillW + 2, barY + barH + 2, (gcA << 24) | 0x00D4FF);
        }

        // Sub-tiles
        int tileY = barY + barH + 5;
        int tiles  = 26;
        int tileW  = (barW - tiles + 1) / tiles;
        for (int i = 0; i < tiles; i++) {
            int tx = barX + i * (tileW + 1);
            boolean filled = i < (int)(loadProgress / 100f * tiles);
            int tcA = filled ? (int)(0xAA * sa / 255) : (int)(0x18 * sa / 255);
            ctx.fill(tx, tileY, tx + tileW, tileY + 3,
                (tcA << 24) | (filled ? 0x00AAFF : 0xFFFFFF));
        }

        // Status message
        String msg = MESSAGES[messageIndex];
        int mA = (int)((130 + 70 * glowPulse) * sa / 255);
        ctx.drawText(textRenderer, msg, cx - textRenderer.getWidth(msg)/2, tileY + 8, (mA << 24) | 0x0088BB, false);

        // ── Bottom ──
        ctx.fill(px + 24, py + ph - 30, px + pw - 24, py + ph - 29, 0x22004488);

        // Copyright
        String copy = "\u00A9 Professor Client  \u2014  All Rights Reserved";
        int copyA = (int)(75 * sa / 255);
        ctx.drawText(textRenderer, copy, cx - textRenderer.getWidth(copy)/2, py + ph - 20, (copyA << 24) | 0x004477, false);
    }

    // ── Utility ────────────────────────────────────────────────────────────

    private int alphaBlend(int color, int sa) {
        int baseA = (color >>> 24) & 0xFF;
        int newA  = (int)(baseA * sa / 255);
        return (newA << 24) | (color & 0x00FFFFFF);
    }
}
