package com.professor.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
    private int   tickCount    = 0;
    private float loadProgress = 0f;
    private int   messageIndex = 0;
    private boolean done       = false;
    private int   doneTimer    = 0;
    private float fadeAlpha    = 0f;
    private boolean fadingIn   = true;
    private float fadeOut      = 0f;

    // Glow pulse
    private float glowPulse = 0f;
    private boolean glowUp  = true;

    // Scanline
    private float scanLine = 0f;

    // Particles [x, y, vx, vy, size, alpha, phase, colorType]
    private final List<float[]> particles = new ArrayList<>();

    // Data stream columns [x, y, speed, char]
    private final List<float[]> dataStreams = new ArrayList<>();

    // Expanding rings [x, y, radius, speed, alpha, colorType]
    private final List<float[]> rings = new ArrayList<>();

    // Floating orbs [x, y, vx, vy, size, alpha, phase]
    private final List<float[]> orbs = new ArrayList<>();

    private static final String[] MESSAGES = {
        "Initializing Professor Client...",
        "Loading exploit modules...",
        "Bypassing anti-cheat systems...",
        "Injecting packet hooks...",
        "Loading GUI components...",
        "Calibrating systems...",
        "All systems ready. Welcome, Professor."
    };

    private static final String CHARS = "PROFESSOR01アイウエオカキクケ10011010";

    public ProfessorSplashScreen() {
        super(Text.literal("Professor Client"));
    }

    @Override
    protected void init() {
        initParticles();
        initDataStreams();
        initRings();
        initOrbs();
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause()      { return false; }

    private void initParticles() {
        particles.clear();
        for (int i = 0; i < 280; i++) {
            particles.add(new float[]{
                RNG.nextFloat() * width,
                RNG.nextFloat() * height,
                (RNG.nextFloat() - 0.5f) * 0.6f,
                -(RNG.nextFloat() * 0.8f + 0.1f),
                RNG.nextFloat() * 2.5f + 0.5f,
                RNG.nextFloat() * 0.9f + 0.1f,
                RNG.nextFloat() * 6.28f,
                RNG.nextInt(3)   // 0=cyan, 1=purple, 2=gold
            });
        }
    }

    private void initDataStreams() {
        dataStreams.clear();
        int cols = Math.max(1, width / 14);
        for (int i = 0; i < cols; i++) {
            dataStreams.add(new float[]{
                i * 14 + 7f,
                RNG.nextFloat() * -height,
                RNG.nextFloat() * 1.4f + 0.4f,
                RNG.nextInt(CHARS.length())
            });
        }
    }

    private void initRings() {
        rings.clear();
        for (int i = 0; i < 8; i++) {
            rings.add(new float[]{
                width  * 0.5f,
                height * 0.5f,
                i * 50f + 10f,
                0.35f + i * 0.08f,
                0.5f + RNG.nextFloat() * 0.4f,
                i % 3   // 0=cyan, 1=purple, 2=gold
            });
        }
    }

    private void initOrbs() {
        orbs.clear();
        for (int i = 0; i < 12; i++) {
            orbs.add(new float[]{
                RNG.nextFloat() * width,
                RNG.nextFloat() * height,
                (RNG.nextFloat() - 0.5f) * 0.3f,
                (RNG.nextFloat() - 0.5f) * 0.3f,
                RNG.nextFloat() * 6f + 3f,
                RNG.nextFloat() * 0.5f + 0.3f,
                RNG.nextFloat() * 6.28f
            });
        }
    }

    @Override
    public void tick() {
        tickCount++;

        if (fadingIn) {
            fadeAlpha = Math.min(1f, fadeAlpha + 0.06f);
            if (fadeAlpha >= 1f) fadingIn = false;
        }

        if (loadProgress < 100f) {
            float speed = loadProgress < 30 ? 0.55f : loadProgress < 70 ? 0.38f : 0.28f;
            loadProgress = Math.min(loadProgress + speed, 100f);
            messageIndex = Math.min(
                (int)(loadProgress / 100f * (MESSAGES.length - 1)),
                MESSAGES.length - 1
            );
        } else if (!done) {
            done = true;
        }

        if (done) {
            doneTimer++;
            if (doneTimer > 60) {
                fadeOut = Math.min(1f, fadeOut + 0.04f);
                if (fadeOut >= 1f) MinecraftClient.getInstance().setScreen(null);
            }
        }

        glowPulse += glowUp ? 0.035f : -0.035f;
        if      (glowPulse >= 1f) { glowPulse = 1f; glowUp = false; }
        else if (glowPulse <= 0f) { glowPulse = 0f; glowUp = true;  }

        scanLine = (scanLine + 2f) % height;

        for (float[] p : particles) {
            p[0] += p[2]; p[1] += p[3]; p[6] += 0.05f;
            if (p[1] < -10) { p[1] = height + 5; p[0] = RNG.nextFloat() * width; }
            if (p[0] < 0)   p[0] = width;
            if (p[0] > width) p[0] = 0;
        }

        for (float[] d : dataStreams) {
            d[1] += d[2];
            if (d[1] > height + 20) { d[1] = -24; d[3] = RNG.nextInt(CHARS.length()); }
            if (RNG.nextInt(22) == 0) d[3] = RNG.nextInt(CHARS.length());
        }

        for (float[] r : rings) {
            r[2] = (r[2] + r[3]) % (Math.max(width, height) * 0.9f);
        }

        for (float[] o : orbs) {
            o[0] += o[2]; o[1] += o[3]; o[6] += 0.03f;
            if (o[0] < 0 || o[0] > width)  o[2] = -o[2];
            if (o[1] < 0 || o[1] > height) o[3] = -o[3];
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float screenFade = (1f - fadeOut) * fadeAlpha;
        int sa = (int)(screenFade * 255);
        if (sa <= 0) return;

        int W = width, H = height, cx = W / 2, cy = H / 2;

        // Deep dark background: #0A0A0F
        ctx.fill(0, 0, W, H, alphaBlend(0xFF0A0A0F, sa));

        drawGrid(ctx, W, H, sa);
        drawRadialGlow(ctx, cx, cy, sa);
        drawOrbs(ctx, sa);
        drawDataStreams(ctx, sa);
        drawParticles(ctx, sa);
        drawRings(ctx, cx, cy, sa);

        // Scanline
        int slA = (int)(0x0C * sa / 255);
        if (slA > 0) ctx.fill(0, (int)scanLine, W, (int)scanLine + 2, (slA << 24) | 0xFFFFFF);

        drawPanel(ctx, cx, cy, W, H, sa);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawGrid(DrawContext ctx, int W, int H, int sa) {
        int a = (int)(0x06 * sa / 255);
        if (a <= 0) return;
        int col = (a << 24) | 0x00F5FF;
        for (int x = 0; x < W; x += 48) ctx.fill(x, 0, x + 1, H, col);
        for (int y = 0; y < H; y += 48) ctx.fill(0, y, W, y + 1, col);
    }

    private void drawRadialGlow(DrawContext ctx, int cx, int cy, int sa) {
        // Cyan glow center
        int[] radii  = {400, 300, 200, 120, 70};
        int[] alphas = {  4,   8,  14,  22, 32};
        for (int i = 0; i < radii.length; i++) {
            int r = radii[i];
            int a = (int)(alphas[i] * (0.5f + 0.5f * glowPulse) * sa / 255);
            ctx.fill(cx - r, cy - r/2, cx + r, cy + r/2, (a << 24) | 0x00F5FF);
        }
    }

    private void drawOrbs(DrawContext ctx, int sa) {
        for (float[] o : orbs) {
            float tw = (MathHelper.sin(o[6]) + 1f) / 2f;
            int a = (int)(o[5] * tw * sa * 0.4f);
            if (a <= 0) continue;
            int sz = (int)o[4];
            // Purple orb glow
            ctx.fill((int)o[0]-sz, (int)o[1]-sz, (int)o[0]+sz, (int)o[1]+sz, (a << 24) | 0x9B00FF);
        }
    }

    private void drawDataStreams(DrawContext ctx, int sa) {
        for (float[] d : dataStreams) {
            if (d[1] < 0) continue;
            int hA = (int)((55 + 45 * glowPulse) * sa / 255);
            ctx.drawText(textRenderer,
                String.valueOf(CHARS.charAt((int)d[3])),
                (int)d[0] - 4, (int)d[1],
                (Math.min(255, hA * 2) << 24) | 0x00F5FF, false);
            if (d[1] > 14)
                ctx.drawText(textRenderer,
                    String.valueOf(CHARS.charAt((int)d[3])),
                    (int)d[0] - 4, (int)d[1] - 14,
                    (hA << 24) | 0x004466, false);
        }
    }

    private void drawParticles(DrawContext ctx, int sa) {
        for (float[] p : particles) {
            float tw = (MathHelper.sin(p[6]) + 1f) / 2f;
            int a = (int)(p[5] * tw * sa);
            if (a < 8) continue;
            int col;
            int colorType = (int)p[7];
            if      (colorType == 0) col = (a << 24) | (p[4] > 1.8f ? 0x00F5FF : 0x007788);
            else if (colorType == 1) col = (a << 24) | (p[4] > 1.8f ? 0x9B00FF : 0x440077);
            else                     col = (a << 24) | (p[4] > 1.8f ? 0xFFD700 : 0x886600);
            int sz = p[4] > 2f ? 2 : 1;
            ctx.fill((int)p[0], (int)p[1], (int)p[0]+sz, (int)p[1]+sz, col);
        }
    }

    private void drawRings(DrawContext ctx, int cx, int cy, int sa) {
        for (float[] r : rings) {
            int rad = (int)r[2];
            if (rad < 2) continue;
            float fadeF = 1f - r[2] / (Math.max(width, height) * 0.9f);
            int a = (int)(r[4] * fadeF * sa / 255 * 35);
            if (a <= 0) continue;
            int col;
            int ct = (int)r[5];
            if      (ct == 0) col = (a << 24) | 0x00F5FF;
            else if (ct == 1) col = (a << 24) | 0x9B00FF;
            else              col = (a << 24) | 0xFFD700;
            ctx.fill(cx - rad, cy - 1, cx + rad, cy + 1, (a/4 << 24) | (col & 0x00FFFFFF));
            ctx.fill(cx - 1, cy - rad, cx + 1, cy + rad, (a/4 << 24) | (col & 0x00FFFFFF));
            ctx.fill(cx - rad, cy - rad/2, cx - rad+1, cy + rad/2, a > 5 ? ((a/2)<<24)|(col&0x00FFFFFF) : 0);
            ctx.fill(cx + rad-1, cy - rad/2, cx + rad, cy + rad/2, a > 5 ? ((a/2)<<24)|(col&0x00FFFFFF) : 0);
        }
    }

    private void drawPanel(DrawContext ctx, int cx, int cy, int W, int H, int sa) {
        int pw = 340, ph = 220;
        int px = cx - pw/2, py = cy - ph/2;

        // Shadow
        int shA = (int)(0x66 * sa / 255);
        ctx.fill(px+8, py+8, px+pw+8, py+ph+8, shA << 24);

        // Panel BG: near-black #0A0A18
        ctx.fill(px, py, px+pw, py+ph, alphaBlend(0xF00A0A18, sa));

        // Subtle inner vignette
        ctx.fill(px, py, px+pw, py+3, alphaBlend(0x220A0A0F, sa));
        ctx.fill(px, py+ph-3, px+pw, py+ph, alphaBlend(0x220A0A0F, sa));

        // Animated border — cyan/purple shifting
        float t = (float)(tickCount * 0.038);
        int bR = Math.max(0, (int)(10 + 30  * Math.abs(Math.sin(t))));
        int bG = Math.max(0, (int)(180 + 75 * Math.sin(t + 1.5)));
        int bB = Math.max(0, (int)(220 + 35 * Math.sin(t + 3.0)));
        int bA = (int)((0.6f + 0.4f * glowPulse) * sa);
        int bc = (bA << 24) | (bR << 16) | (bG << 8) | bB;

        ctx.fill(px,      py,      px+pw,  py+2,    bc);
        ctx.fill(px,      py+ph-2, px+pw,  py+ph,   bc);
        ctx.fill(px,      py,      px+2,   py+ph,   bc);
        ctx.fill(px+pw-2, py,      px+pw,  py+ph,   bc);

        // Corner brackets — gold accent
        int gA = (int)(0xDD * sa / 255);
        int gc = (gA << 24) | 0xFFD700;
        int cl = 18;
        ctx.fill(px,       py,       px+cl, py+2,  gc); ctx.fill(px,      py,       px+2,  py+cl, gc);
        ctx.fill(px+pw-cl, py,       px+pw, py+2,  gc); ctx.fill(px+pw-2, py,       px+pw, py+cl, gc);
        ctx.fill(px,       py+ph-2,  px+cl, py+ph, gc); ctx.fill(px,      py+ph-cl, px+2,  py+ph, gc);
        ctx.fill(px+pw-cl, py+ph-2,  px+pw, py+ph, gc); ctx.fill(px+pw-2, py+ph-cl, px+pw, py+ph, gc);

        // ── Title ──
        int ty = py + 24;
        String title = "PROFESSOR CLIENT";
        int tw = textRenderer.getWidth(title);

        // Cyan glow halo
        int gA2 = (int)((40 + 30 * glowPulse) * sa / 255);
        for (int dx = -4; dx <= 4; dx++) {
            int g2 = Math.max(0, gA2 - Math.abs(dx) * 9);
            if (g2 <= 0) continue;
            ctx.drawText(textRenderer, title, cx-tw/2+dx, ty,    (g2<<24)|0x00F5FF, false);
            ctx.drawText(textRenderer, title, cx-tw/2,    ty+dx, (g2<<24)|0x00F5FF, false);
        }
        int tA = (int)((210 + 45 * glowPulse) * sa / 255);
        ctx.drawText(textRenderer, title, cx-tw/2, ty, (tA<<24)|0x00F5FF, true);

        // Subtitle
        String sub = "v2.0  •  Fabric 1.21.1  •  Elite Client";
        int sA = (int)((120 + 60 * glowPulse) * sa / 255);
        ctx.drawText(textRenderer, sub, cx-textRenderer.getWidth(sub)/2, ty+14, (sA<<24)|0x9B00FF, false);

        // Gold divider
        int divA = (int)(0x55 * sa / 255);
        ctx.fill(px+30, py+46, px+pw-30, py+47, (divA<<24)|0xFFD700);

        // ── Progress ──
        int barX = px+28, barY = py+70, barW = pw-56, barH = 8;

        // Percentage label
        String pct = (int)loadProgress + "%";
        int pA = (int)(200 * sa / 255);
        ctx.drawText(textRenderer, pct, cx-textRenderer.getWidth(pct)/2, barY-14, (pA<<24)|0x00F5FF, false);

        // Track
        ctx.fill(barX-1, barY-1, barX+barW+1, barY+barH+1, (int)(0x33 * sa/255) << 24 | 0x00F5FF);
        ctx.fill(barX, barY, barX+barW, barY+barH, 0x1A000000);

        // Fill: dark purple → neon cyan
        int fillW = (int)(barW * loadProgress / 100f);
        if (fillW > 0) {
            for (int xi = 0; xi < fillW; xi++) {
                float frac = (float)xi / barW;
                int r = (int)MathHelper.lerp(frac, 155f, 0f);
                int g = (int)MathHelper.lerp(frac, 0f,   245f);
                int b = (int)MathHelper.lerp(frac, 255f, 255f);
                ctx.fill(barX+xi, barY, barX+xi+1, barY+barH, 0xFF000000|(r<<16)|(g<<8)|b);
            }
            // Glow cap
            int gcA2 = (int)(130 * glowPulse * sa / 255);
            ctx.fill(barX+fillW-6, barY-2, barX+fillW+2, barY+barH+2, (gcA2<<24)|0x00F5FF);
        }

        // Sub-tiles
        int tileY = barY + barH + 5;
        int tiles  = 28;
        int tileW2 = (barW - tiles + 1) / tiles;
        for (int i = 0; i < tiles; i++) {
            int txp = barX + i * (tileW2 + 1);
            boolean filled = i < (int)(loadProgress / 100f * tiles);
            int tcA = (int)((filled ? 0xCC : 0x15) * sa / 255);
            ctx.fill(txp, tileY, txp+tileW2, tileY+3, (tcA<<24)|(filled ? 0x00F5FF : 0xFFFFFF));
        }

        // Status message
        String msg = MESSAGES[messageIndex];
        int mA = (int)((140 + 80 * glowPulse) * sa / 255);
        ctx.drawText(textRenderer, msg, cx-textRenderer.getWidth(msg)/2, tileY+9, (mA<<24)|0xAAFFFF, false);

        // Done flash
        if (done && doneTimer % 20 < 10) {
            String ready = ">>> READY <<<";
            int rA = (int)(220 * sa / 255);
            ctx.drawText(textRenderer, ready, cx-textRenderer.getWidth(ready)/2, tileY+20, (rA<<24)|0xFFD700, false);
        }

        // Bottom divider + copyright
        int bDivA = (int)(0x33 * sa / 255);
        ctx.fill(px+30, py+ph-28, px+pw-30, py+ph-27, (bDivA<<24)|0x00F5FF);
        String copy = "© Professor Client  —  Elite Fabric Mod  |  1.21.1";
        int copyA = (int)(80 * sa / 255);
        ctx.drawText(textRenderer, copy, cx-textRenderer.getWidth(copy)/2, py+ph-18, (copyA<<24)|0x9B00FF, false);
    }

    private int alphaBlend(int color, int sa) {
        int baseA = (color >>> 24) & 0xFF;
        int newA  = (int)(baseA * sa / 255);
        return (newA << 24) | (color & 0x00FFFFFF);
    }
}
