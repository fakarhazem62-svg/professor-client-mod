package com.professor.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class ProfessorScreen extends Screen {

    private static final Random RANDOM = new Random();
    private final List<Star> stars = new ArrayList<>();
    private long tickCount = 0;

    // Shooting star
    private float shootX, shootY, shootDX, shootDY;
    private float shootAlpha = 0f;
    private int shootTimer = 0;

    public ProfessorScreen() {
        super(Text.literal("Professor Client"));
        initStars();
        spawnShootingStar();
    }

    private void initStars() {
        stars.clear();
        for (int i = 0; i < 180; i++) {
            stars.add(new Star(
                    RANDOM.nextFloat() * 1920,
                    RANDOM.nextFloat() * 1080,
                    RANDOM.nextFloat() * 2.5f + 0.5f,
                    RANDOM.nextFloat() * 255,
                    RANDOM.nextInt(80) + 20
            ));
        }
    }

    private void spawnShootingStar() {
        shootX = RANDOM.nextFloat() * 800;
        shootY = RANDOM.nextFloat() * 100 + 20;
        float angle = (float) (Math.PI / 4 + RANDOM.nextFloat() * 0.5f);
        float speed = RANDOM.nextFloat() * 6 + 8;
        shootDX = (float) (Math.cos(angle) * speed);
        shootDY = (float) (Math.sin(angle) * speed);
        shootAlpha = 1.0f;
        shootTimer = 60 + RANDOM.nextInt(120);
    }

    @Override
    protected void init() {
        initStars();

        int btnW = 220;
        int btnH = 24;
        int cx = this.width / 2;
        int cy = this.height / 2;

        // Packet Flood Button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("⚡  إرسال 300 باكت للسيرفر  ⚡"),
                btn -> sendPackets()
        ).dimensions(cx - btnW / 2, cy + 20, btnW, btnH).build());

        // Close Button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("✖  إغلاق"),
                btn -> this.close()
        ).dimensions(cx - 60, cy + 60, 120, btnH).build());
    }

    private void sendPackets() {
        if (this.client == null || this.client.player == null ||
                this.client.player.networkHandler == null) return;

        double x = this.client.player.getX();
        double y = this.client.player.getY();
        double z = this.client.player.getZ();
        float yaw = this.client.player.getYaw();
        float pitch = this.client.player.getPitch();
        boolean onGround = this.client.player.isOnGround();

        for (int i = 0; i < 300; i++) {
            this.client.player.networkHandler.sendPacket(
                    new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround)
            );
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        tickCount++;

        int w = this.width;
        int h = this.height;

        // === Black Background ===
        context.fill(0, 0, w, h, 0xFF000000);

        // === Animated Stars ===
        for (Star star : stars) {
            star.phase += 0.04f;
            float brightness = (float) ((Math.sin(star.phase) + 1.0) / 2.0);
            int alpha = (int) (star.baseAlpha * brightness * 0.9f + 30);
            alpha = Math.min(255, Math.max(0, alpha));

            int sx = (int) ((star.x / 1920f) * w);
            int sy = (int) ((star.y / 1080f) * h);
            int r = (int) star.radius;
            if (r < 1) r = 1;

            // Draw glowing star
            int col = (alpha << 24) | 0xFFFFFF;
            context.fill(sx, sy, sx + r, sy + r, col);

            // Outer glow (softer, slightly bigger)
            if (star.radius > 1.2f) {
                int glowAlpha = alpha / 4;
                int glow = (glowAlpha << 24) | 0xAADDFF;
                context.fill(sx - 1, sy - 1, sx + r + 1, sy + r + 1, glow);
            }
        }

        // === Shooting Star ===
        shootTimer--;
        if (shootTimer <= 0) {
            spawnShootingStar();
        }
        if (shootAlpha > 0) {
            shootX += shootDX;
            shootY += shootDY;
            shootAlpha -= 0.018f;
            int sa = (int) (shootAlpha * 255);
            if (sa > 0) {
                int trailLen = 18;
                for (int t = 0; t < trailLen; t++) {
                    float frac = (float) t / trailLen;
                    int tx = (int) (shootX - shootDX * t * 0.6f);
                    int ty = (int) (shootY - shootDY * t * 0.6f);
                    int ta = (int) (sa * (1f - frac) * 0.8f);
                    if (ta > 0) {
                        int sc = (ta << 24) | 0xCCEEFF;
                        context.fill(tx, ty, tx + 2, ty + 2, sc);
                    }
                }
            }
        }

        // === Panel Background ===
        int panelW = 340;
        int panelH = 180;
        int px = w / 2 - panelW / 2;
        int py = h / 2 - panelH / 2;

        // Semi-transparent dark panel
        context.fill(px, py, px + panelW, py + panelH, 0xCC050510);

        // Animated purple/cyan border glow
        float glow = (float) ((Math.sin(tickCount * 0.05f) + 1.0) / 2.0);
        int borderR = (int) (80 + glow * 80);
        int borderG = (int) (0 + glow * 40);
        int borderB = (int) (200 + glow * 55);
        int borderColor = 0xFF000000 | (borderR << 16) | (borderG << 8) | borderB;

        // Top border
        context.fill(px, py, px + panelW, py + 2, borderColor);
        // Bottom border
        context.fill(px, py + panelH - 2, px + panelW, py + panelH, borderColor);
        // Left border
        context.fill(px, py, px + 2, py + panelH, borderColor);
        // Right border
        context.fill(px + panelW - 2, py, px + panelW, py + panelH, borderColor);

        // === Title: Professor Client ===
        float pulse = (float) ((Math.sin(tickCount * 0.07f) + 1.0) / 2.0);
        int titleR = (int) (150 + pulse * 105);
        int titleG = (int) (50 + pulse * 50);
        int titleB = 255;
        int titleColor = 0xFF000000 | (titleR << 16) | (titleG << 8) | titleB;

        String title = "Professor Client";
        int titleW = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title, w / 2 - titleW / 2, py + 16, titleColor, false);

        // Subtitle line
        String sub = "✦ Utility Mod v1.0 ✦";
        int subW = this.textRenderer.getWidth(sub);
        context.drawText(this.textRenderer, sub, w / 2 - subW / 2, py + 30, 0xFF6688CC, false);

        // Divider line
        context.fill(px + 20, py + 44, px + panelW - 20, py + 45, 0x88334488);

        // Packet info text
        String info = "اضغط الزر لإرسال 300 باكت للسيرفر";
        int infoW = this.textRenderer.getWidth(info);
        context.drawText(this.textRenderer, info, w / 2 - infoW / 2, py + 52, 0xFFAAAAAA, false);

        // Warning text
        String warn = "⚠  استخدم بمسؤولية  ⚠";
        int warnW = this.textRenderer.getWidth(warn);
        context.drawText(this.textRenderer, warn, w / 2 - warnW / 2, py + 64, 0xFFFF8800, false);

        // === Render Buttons ===
        super.render(context, mouseX, mouseY, delta);

        // === Corner decorations ===
        int cs = 8;
        context.fill(px, py, px + cs, py + 1, 0xFFFFFFFF);
        context.fill(px, py, px + 1, py + cs, 0xFFFFFFFF);
        context.fill(px + panelW - cs, py, px + panelW, py + 1, 0xFFFFFFFF);
        context.fill(px + panelW - 1, py, px + panelW, py + cs, 0xFFFFFFFF);
        context.fill(px, py + panelH - 1, px + cs, py + panelH, 0xFFFFFFFF);
        context.fill(px, py + panelH - cs, px + 1, py + panelH, 0xFFFFFFFF);
        context.fill(px + panelW - cs, py + panelH - 1, px + panelW, py + panelH, 0xFFFFFFFF);
        context.fill(px + panelW - 1, py + panelH - cs, px + panelW, py + panelH, 0xFFFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    // ── Star data class ──────────────────────────────────────────────────────
    private static class Star {
        float x, y, radius, phase;
        int baseAlpha;

        Star(float x, float y, float radius, float phase, int baseAlpha) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.phase = phase;
            this.baseAlpha = baseAlpha;
        }
    }
}
