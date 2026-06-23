package com.professor.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
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

    private float shootX, shootY, shootDX, shootDY;
    private float shootAlpha = 0f;
    private int shootTimer = 0;

    private TextFieldWidget packetField;
    private String statusMessage = "";
    private int statusColor = 0xFFAAFFAA;
    private int statusTimer = 0;

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

        int cx = this.width / 2;
        int cy = this.height / 2;

        // Packet count input field
        packetField = new TextFieldWidget(
                this.textRenderer,
                cx - 60, cy - 2,
                120, 18,
                Text.literal("Packet Count")
        );
        packetField.setMaxLength(6);
        packetField.setText("300");
        this.addSelectableChild(packetField);

        // Send Packets button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Send Packets"),
                btn -> sendPackets()
        ).dimensions(cx - 55, cy + 25, 110, 20).build());

        // Presets row
        for (int[] preset : new int[][]{{100, -90}, {300, -25}, {500, 40}, {1000, 105}}) {
            int count = preset[0];
            int offsetX = preset[1];
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(count + "x"),
                    btn -> {
                        packetField.setText(String.valueOf(count));
                    }
            ).dimensions(cx + offsetX, cy - 25, 40, 16).build());
        }

        // Close button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Close"),
                btn -> this.close()
        ).dimensions(cx - 30, cy + 52, 60, 18).build());
    }

    private void sendPackets() {
        if (this.client == null || this.client.player == null ||
                this.client.player.networkHandler == null) {
            setStatus("Not connected to a server!", 0xFFFF5555);
            return;
        }

        int count;
        try {
            count = Integer.parseInt(packetField.getText().trim());
            if (count <= 0 || count > 10000) {
                setStatus("Enter a number between 1 and 10000", 0xFFFFAA00);
                return;
            }
        } catch (NumberFormatException e) {
            setStatus("Invalid number!", 0xFFFF5555);
            return;
        }

        double x = this.client.player.getX();
        double y = this.client.player.getY();
        double z = this.client.player.getZ();
        float yaw = this.client.player.getYaw();
        float pitch = this.client.player.getPitch();
        boolean onGround = this.client.player.isOnGround();

        for (int i = 0; i < count; i++) {
            this.client.player.networkHandler.sendPacket(
                    new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround)
            );
        }

        setStatus("Sent " + count + " packets!", 0xFF55FF55);
    }

    private void setStatus(String msg, int color) {
        this.statusMessage = msg;
        this.statusColor = color;
        this.statusTimer = 80;
    }

    @Override
    public void tick() {
        super.tick();
        if (packetField != null) packetField.tick();
        if (statusTimer > 0) statusTimer--;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        tickCount++;

        int w = this.width;
        int h = this.height;

        // Black background
        context.fill(0, 0, w, h, 0xFF000000);

        // Animated stars
        for (Star star : stars) {
            star.phase += 0.04f;
            float brightness = (float) ((Math.sin(star.phase) + 1.0) / 2.0);
            int alpha = (int) (star.baseAlpha * brightness * 0.9f + 30);
            alpha = Math.min(255, Math.max(0, alpha));

            int sx = (int) ((star.x / 1920f) * w);
            int sy = (int) ((star.y / 1080f) * h);
            int r = Math.max(1, (int) star.radius);

            context.fill(sx, sy, sx + r, sy + r, (alpha << 24) | 0xFFFFFF);
            if (star.radius > 1.2f) {
                int glowAlpha = alpha / 4;
                context.fill(sx - 1, sy - 1, sx + r + 1, sy + r + 1, (glowAlpha << 24) | 0xAADDFF);
            }
        }

        // Shooting star
        shootTimer--;
        if (shootTimer <= 0) spawnShootingStar();
        if (shootAlpha > 0) {
            shootX += shootDX;
            shootY += shootDY;
            shootAlpha -= 0.018f;
            int sa = (int) (shootAlpha * 255);
            if (sa > 0) {
                for (int t = 0; t < 18; t++) {
                    float frac = (float) t / 18;
                    int tx = (int) (shootX - shootDX * t * 0.6f);
                    int ty = (int) (shootY - shootDY * t * 0.6f);
                    int ta = (int) (sa * (1f - frac) * 0.8f);
                    if (ta > 0) context.fill(tx, ty, tx + 2, ty + 2, (ta << 24) | 0xCCEEFF);
                }
            }
        }

        // Panel
        int panelW = 320;
        int panelH = 200;
        int px = w / 2 - panelW / 2;
        int py = h / 2 - panelH / 2;

        context.fill(px, py, px + panelW, py + panelH, 0xCC050510);

        // Animated border
        float glow = (float) ((Math.sin(tickCount * 0.05f) + 1.0) / 2.0);
        int borderColor = 0xFF000000
                | ((int) (80 + glow * 80) << 16)
                | ((int) (glow * 40) << 8)
                | (int) (200 + glow * 55);
        context.fill(px,              py,              px + panelW, py + 2,          borderColor);
        context.fill(px,              py + panelH - 2, px + panelW, py + panelH,     borderColor);
        context.fill(px,              py,              px + 2,      py + panelH,     borderColor);
        context.fill(px + panelW - 2, py,              px + panelW, py + panelH,     borderColor);

        // Title
        float pulse = (float) ((Math.sin(tickCount * 0.07f) + 1.0) / 2.0);
        int titleColor = 0xFF000000
                | ((int) (150 + pulse * 105) << 16)
                | ((int) (50 + pulse * 50) << 8)
                | 0xFF;

        String title = "Professor Client";
        context.drawText(this.textRenderer, title,
                w / 2 - this.textRenderer.getWidth(title) / 2, py + 14, titleColor, false);

        String sub = "* Utility Mod v1.0  |  Fabric 1.21.1 *";
        context.drawText(this.textRenderer, sub,
                w / 2 - this.textRenderer.getWidth(sub) / 2, py + 27, 0xFF5566AA, false);

        context.fill(px + 20, py + 40, px + panelW - 20, py + 41, 0x55334488);

        // Labels
        context.drawText(this.textRenderer, "Packet Count:", px + 20, py + 52, 0xFFCCCCCC, false);
        context.drawText(this.textRenderer, "Presets:", px + 20, py + 72, 0xFF888888, false);

        // Input field label
        String inputLabel = "[ Enter amount and press Send ]";
        context.drawText(this.textRenderer, inputLabel,
                w / 2 - this.textRenderer.getWidth(inputLabel) / 2, py + 52, 0xFF9999BB, false);

        // Status message
        if (statusTimer > 0 && !statusMessage.isEmpty()) {
            int alpha = Math.min(255, statusTimer * 4);
            int col = (statusColor & 0x00FFFFFF) | (alpha << 24);
            context.drawText(this.textRenderer, statusMessage,
                    w / 2 - this.textRenderer.getWidth(statusMessage) / 2,
                    py + panelH - 16, col, false);
        }

        // Corner decorations
        int cs = 8;
        context.fill(px,              py,              px + cs, py + 1,      0xFFFFFFFF);
        context.fill(px,              py,              px + 1,  py + cs,     0xFFFFFFFF);
        context.fill(px + panelW - cs,py,              px + panelW, py + 1, 0xFFFFFFFF);
        context.fill(px + panelW - 1, py,              px + panelW, py + cs,0xFFFFFFFF);
        context.fill(px,              py + panelH - 1, px + cs, py + panelH,0xFFFFFFFF);
        context.fill(px,              py + panelH - cs,px + 1,  py + panelH,0xFFFFFFFF);
        context.fill(px + panelW - cs,py + panelH - 1, px + panelW, py + panelH, 0xFFFFFFFF);
        context.fill(px + panelW - 1, py + panelH - cs,px + panelW, py + panelH, 0xFFFFFFFF);

        // Render widgets
        if (packetField != null) packetField.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (packetField != null && packetField.isFocused()) {
            return packetField.keyPressed(keyCode, scanCode, modifiers)
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (packetField != null && packetField.isFocused()) {
            return packetField.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (packetField != null) packetField.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }

    private static class Star {
        float x, y, radius, phase;
        int baseAlpha;
        Star(float x, float y, float radius, float phase, int baseAlpha) {
            this.x = x; this.y = y;
            this.radius = radius; this.phase = phase;
            this.baseAlpha = baseAlpha;
        }
    }
}
