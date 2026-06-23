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

    private int tickCount = 0;
    private float loadProgress = 0f;
    private int messageIndex = 0;
    private boolean done = false;
    private int doneTimer = 0;

    private final List<Star>       stars       = new ArrayList<>();
    private final List<MatrixDrop> matrixDrops = new ArrayList<>();

    // Shooting star state
    private float shootX, shootY, shootDX, shootDY, shootAlpha = 0f;
    private int   shootTimer = 60;

    // Glow pulse (0-1)
    private float glowPulse = 0f;
    private boolean glowUp  = true;

    private static final String[] MESSAGES = {
        "Initializing Professor Client...",
        "Loading modules...",
        "Bypassing anti-cheat...",
        "Injecting hooks...",
        "Loading GUI...",
        "Ready."
    };

    public ProfessorSplashScreen() {
        super(Text.literal("Professor Client"));
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void init() {
        initStars();
        initMatrix();
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }


    // ── Tick ───────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        tickCount++;

        // Progress
        if (loadProgress < 100f) {
            loadProgress = Math.min(loadProgress + 0.55f, 100f);
            messageIndex = Math.min(
                (int)(loadProgress / 100f * (MESSAGES.length - 1)),
                MESSAGES.length - 1
            );
        } else if (!done) {
            done = true;
        }

        if (done) {
            doneTimer++;
            if (doneTimer > 40) {
                MinecraftClient.getInstance().setScreen(null);
            }
        }

        // Glow pulse
        glowPulse += glowUp ? 0.035f : -0.035f;
        if (glowPulse >= 1f) { glowPulse = 1f; glowUp = false; }
        else if (glowPulse <= 0f) { glowPulse = 0f; glowUp = true; }

        // Stars twinkle
        for (Star s : stars) s.twinkle += 0.06f;

        // Matrix drops
        for (MatrixDrop d : matrixDrops) {
            d.y += 0.9f;
            if (d.y > height + 20) {
                d.y = -20;
                d.ch = randomChar();
            }
            if (RNG.nextInt(30) == 0) d.ch = randomChar();
        }

        // Shooting star
        shootTimer--;
        if (shootTimer <= 0) spawnShootingStar();
        if (shootAlpha > 0) {
            shootX    += shootDX;
            shootY    += shootDY;
            shootAlpha = Math.max(0, shootAlpha - 0.025f);
        }
    }

    // ── Init helpers ───────────────────────────────────────────────────────

    private void initStars() {
        stars.clear();
        for (int i = 0; i < 180; i++) {
            stars.add(new Star(
                RNG.nextFloat() * width,
                RNG.nextFloat() * height,
                RNG.nextFloat() * 1.6f + 0.4f,
                RNG.nextFloat() * 0.7f + 0.3f,
                RNG.nextFloat() * (float)(Math.PI * 2)
            ));
        }
    }

    private void initMatrix() {
        matrixDrops.clear();
        int cols = Math.max(1, width / 14);
        for (int i = 0; i < cols; i++) {
            matrixDrops.add(new MatrixDrop(
                i * 14 + 7f,
                RNG.nextFloat() * -height,
                randomChar()
            ));
        }
    }

    private char randomChar() {
        String pool = "PROFESSOR CLIENT 01アイウエオカキクケコサシスセソ";
        return pool.charAt(RNG.nextInt(pool.length()));
    }

    private void spawnShootingStar() {
        shootTimer = RNG.nextInt(120) + 80;
        shootX     = RNG.nextFloat() * width  * 0.6f;
        shootY     = RNG.nextFloat() * height * 0.25f;
        float angle = (float)(Math.PI / 5 + RNG.nextFloat() * Math.PI / 4);
        float speed = RNG.nextFloat() * 5 + 4;
        shootDX    = (float)(Math.cos(angle) * speed);
        shootDY    = (float)(Math.sin(angle) * speed);
        shootAlpha = 1f;
    }

    // ── Render ─────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Deep-space background
        ctx.fill(0, 0, width, height, 0xFF050510);
        // Radial center glow
        drawRadialGlow(ctx);
        // Layers
        renderMatrix(ctx);
        renderStars(ctx);
        renderShootingStar(ctx);
        renderCornerBrackets(ctx);
        renderPanel(ctx);
        super.render(ctx, mouseX, mouseY, delta);
    }

    // Fake radial glow via concentric translucent fills
    private void drawRadialGlow(DrawContext ctx) {
        int cx = width / 2, cy = height / 2;
        int[] radii = {300, 220, 150, 90};
        int[] alphas = {8, 12, 18, 22};
        for (int i = 0; i < radii.length; i++) {
            int r = radii[i];
            int a = (int)(alphas[i] * (0.6f + 0.4f * glowPulse));
            ctx.fill(cx - r, cy - r / 2, cx + r, cy + r / 2, (a << 24) | 0x7C3AED);
        }
    }

    private void renderMatrix(DrawContext ctx) {
        for (MatrixDrop d : matrixDrops) {
            if (d.y < 0) continue;
            // Bright head
            int headA = (int)(35 + 25 * glowPulse);
            ctx.drawText(textRenderer, String.valueOf(d.ch),
                (int)d.x - 4, (int)d.y, (headA * 2 << 24) | 0x67E8F9, false);
            // Dim trail
            if (d.y > 14)
                ctx.drawText(textRenderer, String.valueOf(d.ch),
                    (int)d.x - 4, (int)d.y - 14, (headA << 24) | 0x7C3AED, false);
        }
    }

    private void renderStars(DrawContext ctx) {
        for (Star s : stars) {
            float b = MathHelper.sin(s.twinkle) * 0.45f + 0.55f;
            int a = (int)(b * s.alpha * 210);
            if (a < 8) continue;
            int col = (a << 24) | 0xC4B5FD;
            int sz  = s.size > 1.3f ? 2 : 1;
            ctx.fill((int)s.x, (int)s.y, (int)s.x + sz, (int)s.y + sz, col);
        }
    }

    private void renderShootingStar(DrawContext ctx) {
        if (shootAlpha <= 0.01f) return;
        // Head
        int ha = (int)(shootAlpha * 220);
        ctx.fill((int)shootX, (int)shootY, (int)shootX + 3, (int)shootY + 3,
            (ha << 24) | 0xFFFFFF);
        // Trail segments
        for (int i = 1; i <= 10; i++) {
            float t    = shootAlpha * (1f - i / 10f);
            int   ta   = (int)(t * 180);
            int   tcol = (ta << 24) | 0xA78BFA;
            ctx.fill(
                (int)(shootX - shootDX * i),
                (int)(shootY - shootDY * i),
                (int)(shootX - shootDX * i) + 2,
                (int)(shootY - shootDY * i) + 2,
                tcol
            );
        }
    }

    private void renderCornerBrackets(DrawContext ctx) {
        int len = 22, thick = 2;
        int col = 0xBB06B6D4;
        int m   = 12;
        // Top-left
        ctx.fill(m, m, m + len, m + thick, col);
        ctx.fill(m, m, m + thick, m + len, col);
        // Top-right
        ctx.fill(width - m - len, m, width - m, m + thick, col);
        ctx.fill(width - m - thick, m, width - m, m + len, col);
        // Bottom-left
        ctx.fill(m, height - m - thick, m + len, height - m, col);
        ctx.fill(m, height - m - len, m + thick, height - m, col);
        // Bottom-right
        ctx.fill(width - m - len, height - m - thick, width - m, height - m, col);
        ctx.fill(width - m - thick, height - m - len, width - m, height - m, col);
    }

    private void renderPanel(DrawContext ctx) {
        int pw = 280, ph = 155;
        int px = (width  - pw) / 2;
        int py = (height - ph) / 2;
        int cx = width / 2;

        // Panel BG
        ctx.fill(px, py, px + pw, py + ph, 0xD80D0A1F);

        // Animated border (purple + cyan pulse)
        int ba = (int)(100 + 80 * glowPulse);
        int borderCol = (ba << 24) | 0x7C3AED;
        ctx.fill(px,          py,          px + pw,      py + 1,       borderCol);
        ctx.fill(px,          py + ph - 1, px + pw,      py + ph,      borderCol);
        ctx.fill(px,          py,          px + 1,       py + ph,      borderCol);
        ctx.fill(px + pw - 1, py,          px + pw,      py + ph,      borderCol);

        // Cyan corner accents on panel
        int ac = 0xDD06B6D4;
        for (int[] c : new int[][]{{px,pw-1},{py,ph-1}}) { /* handled below */ }
        panelCorner(ctx, px,      py,      ac, false, false);
        panelCorner(ctx, px + pw, py,      ac, true,  false);
        panelCorner(ctx, px,      py + ph, ac, false, true);
        panelCorner(ctx, px + pw, py + ph, ac, true,  true);

        // ── PROFESSOR CLIENT title ──
        int ty = py + 16;
        // Soft outer glow layers
        int ga = (int)(28 + 22 * glowPulse);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx == 0 && dy == 0) continue;
                int dist = Math.abs(dx) + Math.abs(dy);
                int g2   = Math.max(0, ga - dist * 8);
                ctx.drawText(textRenderer, "PROFESSOR CLIENT",
                    cx - textRenderer.getWidth("PROFESSOR CLIENT") / 2 + dx,
                    ty + dy, (g2 << 24) | 0x7C3AED, false);
            }
        }
        // Main title
        int ta = (int)(210 + 45 * glowPulse);
        ctx.drawText(textRenderer, "PROFESSOR CLIENT",
            cx - textRenderer.getWidth("PROFESSOR CLIENT") / 2, ty,
            (ta << 24) | 0xC4B5FD, true);

        // Subtitle
        String sub = "v1.0.0  \u2022  Fabric 1.21.1";
        int sa = (int)(110 + 50 * glowPulse);
        ctx.drawText(textRenderer, sub,
            cx - textRenderer.getWidth(sub) / 2, ty + 13,
            (sa << 24) | 0x67E8F9, false);

        // ── Divider ──
        ctx.fill(px + 20, py + 33, px + pw - 20, py + 34, 0x33A78BFA);

        // ── Progress area ──
        int barX = px + 20;
        int barY = py + 48;
        int barW = pw - 40;
        int barH = 5;

        // Percent label
        String pct = (int)loadProgress + "%";
        ctx.drawText(textRenderer, pct,
            cx - textRenderer.getWidth(pct) / 2, barY - 10,
            0xFFA78BFA, false);

        // Bar track
        ctx.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0x30FFFFFF);
        ctx.fill(barX, barY, barX + barW, barY + barH, 0x20FFFFFF);
        // Bar fill — purple → cyan horizontal
        int fillW = (int)(barW * loadProgress / 100f);
        if (fillW > 0) {
            // Simulate horizontal gradient with vertical fillGradient calls
            for (int xi = 0; xi < fillW; xi++) {
                float frac  = (float)xi / barW;
                int   rr    = (int)MathHelper.lerp(frac, 0x7C, 0x06);
                int   gg    = (int)MathHelper.lerp(frac, 0x3A, 0xB6);
                int   bb    = (int)MathHelper.lerp(frac, 0xED, 0xD4);
                int   pixel = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
                ctx.fill(barX + xi, barY, barX + xi + 1, barY + barH, pixel);
            }
            // Glow cap at right edge
            int glowCap = (int)(80 * glowPulse);
            ctx.fill(barX + fillW - 3, barY - 1, barX + fillW + 1, barY + barH + 1,
                (glowCap << 24) | 0x06B6D4);
        }

        // Sub-tiles (decorative)
        int tileY = barY + barH + 3;
        int tiles = 22;
        int tileW = (barW - tiles + 1) / tiles;
        for (int i = 0; i < tiles; i++) {
            int tx  = barX + i * (tileW + 1);
            boolean filled = i < (int)(loadProgress / 100f * tiles);
            int tc  = filled ? 0xBB06B6D4 : 0x18FFFFFF;
            ctx.fill(tx, tileY, tx + tileW, tileY + 2, tc);
        }

        // Status message
        String msg = MESSAGES[messageIndex];
        int ma = (int)(140 + 60 * glowPulse);
        ctx.drawText(textRenderer, msg,
            cx - textRenderer.getWidth(msg) / 2, tileY + 7,
            (ma << 24) | 0x67E8F9, false);

        // ── Bottom divider ──
        ctx.fill(px + 20, py + ph - 22, px + pw - 20, py + ph - 21, 0x22A78BFA);

        // ── Bottom stats ──
        int sy = py + ph - 15;
        String left   = "MODULES: 47";
        String mid    = done ? "\u25CF LOADED" : "\u25CF LOADING";
        String right  = "BYPASS: ON";
        int    midCol = done ? 0xFF34D399 : 0xFF67E8F9;

        ctx.drawText(textRenderer, left,  px + 14, sy, 0x80A78BFA, false);
        ctx.drawText(textRenderer, mid,   cx - textRenderer.getWidth(mid) / 2, sy, midCol, false);
        ctx.drawText(textRenderer, right, px + pw - 14 - textRenderer.getWidth(right), sy,
            0x8034D399, false);
    }

    private void panelCorner(DrawContext ctx, int x, int y, int col, boolean flipX, boolean flipY) {
        int len = 10, th = 1;
        int sx = flipX ? x - len : x;
        int sy = flipY ? y - len : y;
        // Horizontal
        ctx.fill(sx, flipY ? y - th : y, sx + len, flipY ? y : y + th, col);
        // Vertical
        ctx.fill(flipX ? x - th : x, sy, flipX ? x : x + th, sy + len, col);
    }

    // ── Inner data classes ──────────────────────────────────────────────────

    private static class Star {
        float x, y, size, alpha, twinkle;
        Star(float x, float y, float size, float alpha, float twinkle) {
            this.x = x; this.y = y; this.size = size;
            this.alpha = alpha; this.twinkle = twinkle;
        }
    }

    private static class MatrixDrop {
        float x, y;
        char ch;
        MatrixDrop(float x, float y, char ch) {
            this.x = x; this.y = y; this.ch = ch;
        }
    }
}
