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

    private int     tickCount    = 0;
    private float   loadProgress = 0f;
    private int     messageIndex = 0;
    private boolean done         = false;
    private int     doneTimer    = 0;
    private float   fadeAlpha    = 0f;
    private boolean fadingIn     = true;
    private float   fadeOut      = 0f;
    private float   glowPulse    = 0f;
    private boolean glowUp       = true;
    private float   scanLine     = 0f;
    private float   scanLine2    = 0f;

    // particles [x,y,vx,vy,size,alpha,phase,colorType]
    private final List<float[]> particles   = new ArrayList<>();
    // data streams [x,y,speed,char]
    private final List<float[]> dataStreams = new ArrayList<>();
    // rings [cx,cy,radius,speed,alpha,colorType]
    private final List<float[]> rings       = new ArrayList<>();
    // orbs [x,y,vx,vy,size,alpha,phase,colorType]
    private final List<float[]> orbs        = new ArrayList<>();

    private static final String[] MESSAGES = {
        "Initializing Professor Client...",
        "Loading exploit modules...",
        "Bypassing anti-cheat systems...",
        "Injecting packet hooks...",
        "Loading GUI components...",
        "Calibrating bypass patterns...",
        "All systems ready.  Welcome, Professor."
    };

    private static final String CHARS = "PROFESSOR01AKASATANA10110100";

    public ProfessorSplashScreen() {
        super(Text.literal("Professor Client"));
    }

    @Override protected void init() {
        initParticles();
        initDataStreams();
        initRings();
        initOrbs();
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause()      { return false; }

    // ─────────────────────────────────────────────────────────────────────

    private void initParticles() {
        particles.clear();
        for (int i = 0; i < 300; i++) {
            particles.add(new float[]{
                RNG.nextFloat() * width,
                RNG.nextFloat() * height,
                (RNG.nextFloat() - 0.5f) * 0.65f,
                -(RNG.nextFloat() * 0.8f + 0.1f),
                RNG.nextFloat() * 3f + 0.5f,
                RNG.nextFloat() * 0.9f + 0.1f,
                RNG.nextFloat() * 6.28f,
                RNG.nextInt(5) // 0=cyan 1=purple 2=gold 3=red 4=green
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
                RNG.nextFloat() * 1.5f + 0.4f,
                RNG.nextInt(CHARS.length())
            });
        }
    }

    private void initRings() {
        rings.clear();
        for (int i = 0; i < 10; i++) {
            rings.add(new float[]{
                width * 0.5f, height * 0.5f,
                i * 55f + 10f,
                0.3f + i * 0.07f,
                0.5f + RNG.nextFloat() * 0.4f,
                i % 3
            });
        }
    }

    private void initOrbs() {
        orbs.clear();
        for (int i = 0; i < 16; i++) {
            orbs.add(new float[]{
                RNG.nextFloat() * width,  RNG.nextFloat() * height,
                (RNG.nextFloat() - 0.5f) * 0.3f, (RNG.nextFloat() - 0.5f) * 0.3f,
                RNG.nextFloat() * 70f + 20f,
                RNG.nextFloat() * 0.4f + 0.2f,
                RNG.nextFloat() * 6.28f,
                RNG.nextInt(3)
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        tickCount++;

        if (fadingIn) { fadeAlpha = Math.min(1f, fadeAlpha + 0.055f); if (fadeAlpha >= 1f) fadingIn = false; }

        if (loadProgress < 100f) {
            float speed = loadProgress < 30 ? 0.55f : loadProgress < 70 ? 0.38f : 0.26f;
            loadProgress = Math.min(loadProgress + speed, 100f);
            messageIndex = Math.min((int)(loadProgress / 100f * (MESSAGES.length - 1)), MESSAGES.length - 1);
        } else if (!done) { done = true; }

        if (done) {
            doneTimer++;
            if (doneTimer > 65) {
                fadeOut = Math.min(1f, fadeOut + 0.042f);
                if (fadeOut >= 1f) MinecraftClient.getInstance().setScreen(null);
            }
        }

        glowPulse += glowUp ? 0.032f : -0.032f;
        if      (glowPulse >= 1f) { glowPulse = 1f; glowUp = false; }
        else if (glowPulse <= 0f) { glowPulse = 0f; glowUp = true; }

        scanLine  = (scanLine  + 2.5f) % height;
        scanLine2 = (scanLine2 + 1.4f) % height;

        for (float[] p : particles) {
            p[0] += p[2]; p[1] += p[3]; p[6] += 0.048f;
            if (p[1] < -10) { p[1] = height + 5; p[0] = RNG.nextFloat() * width; }
            if (p[0] < 0)   p[0] = width;
            if (p[0] > width) p[0] = 0;
        }

        for (float[] d : dataStreams) {
            d[1] += d[2];
            if (d[1] > height + 20) { d[1] = -24; d[3] = RNG.nextInt(CHARS.length()); }
            if (RNG.nextInt(20) == 0) d[3] = RNG.nextInt(CHARS.length());
        }

        for (float[] r : rings) { r[2] = (r[2] + r[3]) % (Math.max(width, height) * 0.9f); }

        for (float[] o : orbs) {
            o[0] += o[2]; o[1] += o[3]; o[6] += 0.025f;
            if (o[0] < 0 || o[0] > width)  o[2] = -o[2];
            if (o[1] < 0 || o[1] > height) o[3] = -o[3];
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float screenFade = (1f - fadeOut) * fadeAlpha;
        int sa = (int)(screenFade * 255);
        if (sa <= 0) return;

        int W = width, H = height, cx = W / 2, cy = H / 2;

        // BG #050508
        ctx.fill(0, 0, W, H, alphaBlend(0xFF050508, sa));

        drawGrid(ctx, W, H, sa);
        drawOrbs(ctx, sa);
        drawRings(ctx, cx, cy, sa);
        drawDataStreams(ctx, sa);
        drawParticles(ctx, sa);

        // Scanlines
        int slA = (int)(0x0D * sa / 255);
        if (slA > 0) {
            ctx.fill(0, (int)scanLine,  W, (int)scanLine  + 2, (slA << 24) | 0xFFFFFF);
            ctx.fill(0, (int)scanLine2, W, (int)scanLine2 + 1, (slA/2 << 24) | 0xFFFFFF);
        }

        drawPanel(ctx, cx, cy, W, H, sa);
        super.render(ctx, mouseX, mouseY, delta);
    }

    // ─────────────────────────────────────────────────────────────────────

    private void drawGrid(DrawContext ctx, int W, int H, int sa) {
        int a = (int)(0x07 * sa / 255); if (a <= 0) return;
        for (int x = 0; x < W; x += 50) ctx.fill(x, 0, x + 1, H, (a<<24)|0x00F5FF);
        for (int y = 0; y < H; y += 50) ctx.fill(0, y, W, y + 1, (a<<24)|0x00F5FF);
    }

    private void drawOrbs(DrawContext ctx, int sa) {
        for (float[] o : orbs) {
            float tw = (MathHelper.sin(o[6]) + 1f) / 2f;
            int a = (int)(o[5] * tw * sa * 0.25f); if (a <= 0) continue;
            int sz = (int)o[4];
            int c = switch((int)o[7]){ case 1->0x9B00FF; case 2->0xFFD700; default->0x00F5FF; };
            ctx.fill((int)o[0]-sz,(int)o[1]-sz/2,(int)o[0]+sz,(int)o[1]+sz/2,(a<<24)|c);
        }
    }

    private void drawDataStreams(DrawContext ctx, int sa) {
        for (float[] d : dataStreams) {
            if (d[1] < 0) continue;
            int hA = (int)((55 + 45 * glowPulse) * sa / 255);
            ctx.drawText(textRenderer, String.valueOf(CHARS.charAt((int)d[3])),
                (int)d[0] - 4, (int)d[1], (Math.min(255, hA*2)<<24)|0x00F5FF, false);
            if (d[1] > 14)
                ctx.drawText(textRenderer, String.valueOf(CHARS.charAt((int)d[3])),
                    (int)d[0] - 4, (int)d[1] - 14, (hA<<24)|0x003344, false);
        }
    }

    private void drawParticles(DrawContext ctx, int sa) {
        for (float[] p : particles) {
            float tw = (MathHelper.sin(p[6]) + 1f) / 2f;
            int a = (int)(p[5] * tw * sa); if (a < 8) continue;
            boolean big = p[4] > 2f;
            int col = switch((int)p[7]){
                case 1->(a<<24)|(big?0x9B00FF:0x220033);
                case 2->(a<<24)|(big?0xFFD700:0x443300);
                case 3->(a<<24)|(big?0xFF2200:0x330000);
                case 4->(a<<24)|(big?0x00FF66:0x003311);
                default->(a<<24)|(big?0x00F5FF:0x003344);
            };
            int sz = p[4] > 2.5f ? 2 : 1;
            ctx.fill((int)p[0],(int)p[1],(int)p[0]+sz,(int)p[1]+sz, col);
        }
    }

    private void drawRings(DrawContext ctx, int cx, int cy, int sa) {
        for (float[] r : rings) {
            int rad = (int)r[2]; if (rad < 2) continue;
            float ff = 1f - r[2] / (Math.max(width, height) * 0.9f);
            int a = (int)(r[4] * ff * sa / 255 * 40); if (a <= 0) continue;
            int c = switch((int)r[5]){ case 1->(a<<24)|0x9B00FF; case 2->(a<<24)|0xFFD700; default->(a<<24)|0x00F5FF; };
            ctx.fill(cx-rad,cy-1,cx+rad,cy+1,c);
            ctx.fill(cx-1,cy-rad,cx+1,cy+rad,c);
        }
    }

    private void drawPanel(DrawContext ctx, int cx, int cy, int W, int H, int sa) {
        int pw = 360, ph = 240;
        int px = cx - pw/2, py = cy - ph/2;

        // Shadow layers
        ctx.fill(px+10,py+10,px+pw+10,py+ph+10,alphaBlend(0x77000000,sa));
        ctx.fill(px+5, py+5, px+pw+5, py+ph+5, alphaBlend(0x33000000,sa));

        // Panel BG
        ctx.fill(px,py,px+pw,py+ph,alphaBlend(0xF00A0A18,sa));

        // Animated hue border
        float t = tickCount * 0.04f;
        int bR = (int)(Math.abs(Math.sin(t)) * 155);
        int bG = Math.max(0,Math.min(255,(int)(180+75*Math.sin(t+2.1))));
        int bB = Math.max(0,Math.min(255,(int)(240+15*Math.sin(t+4.2))));
        int bA = (int)((0.65f+0.35f*glowPulse)*sa);
        int bc = (bA<<24)|(bR<<16)|(bG<<8)|bB;
        int bc2= ((bA/4)<<24)|(bR<<16)|(bG<<8)|bB;
        ctx.fill(px,     py,     px+pw,  py+2,    bc);
        ctx.fill(px,     py+ph-2,px+pw,  py+ph,   bc);
        ctx.fill(px,     py,     px+2,   py+ph,   bc);
        ctx.fill(px+pw-2,py,     px+pw,  py+ph,   bc);
        ctx.fill(px+2,   py+2,   px+pw-2,py+3,    bc2);
        ctx.fill(px+2,   py+ph-3,px+pw-2,py+ph-2, bc2);
        ctx.fill(px+2,   py+2,   px+3,   py+ph-2, bc2);
        ctx.fill(px+pw-3,py+2,   px+pw-2,py+ph-2, bc2);

        // Gold corners (decorative)
        int gcA = (int)(0xEE * sa / 255);
        int gc = (gcA<<24)|0xFFD700;
        int cs = 22;
        ctx.fill(px,       py,       px+cs,  py+3,  gc); ctx.fill(px,      py,       px+3,   py+cs, gc);
        ctx.fill(px+pw-cs, py,       px+pw,  py+3,  gc); ctx.fill(px+pw-3, py,       px+pw,  py+cs, gc);
        ctx.fill(px,       py+ph-3,  px+cs,  py+ph, gc); ctx.fill(px,      py+ph-cs, px+3,   py+ph, gc);
        ctx.fill(px+pw-cs, py+ph-3,  px+pw,  py+ph, gc); ctx.fill(px+pw-3, py+ph-cs, px+pw,  py+ph, gc);

        // ── TITLE ──
        int ty = py + 28;
        String t1 = "PROFESSOR", t2 = " CLIENT";
        int tw1 = textRenderer.getWidth(t1), tw2 = textRenderer.getWidth(t2);
        int totalW = tw1 + tw2, startX = cx - totalW/2;

        // Glow behind title
        int gA = (int)((38 + 28 * glowPulse) * sa / 255);
        for (int dx = -5; dx <= 5; dx++) {
            int g2 = Math.max(0, gA - Math.abs(dx)*6);
            if (g2 <= 0) continue;
            ctx.drawText(textRenderer, t1+t2, startX+dx, ty,    (g2<<24)|0x00F5FF, false);
            ctx.drawText(textRenderer, t1+t2, startX,    ty+dx, (g2<<24)|0x00F5FF, false);
        }
        int tA = (int)((210+45*glowPulse)*sa/255);
        ctx.drawText(textRenderer, t1, startX,      ty, (tA<<24)|0x00F5FF, false);
        ctx.drawText(textRenderer, t2, startX+tw1,  ty, (tA<<24)|0xFFD700, false);

        // Subtitle
        String sub = "v3.0  |  6 Modules  |  8 Bypass  |  Unlimited";
        int sA = (int)((120+60*glowPulse)*sa/255);
        ctx.drawText(textRenderer, sub, cx-textRenderer.getWidth(sub)/2, ty+12, (sA<<24)|0x9B00FF, false);

        // Gold divider
        int divA = (int)(0x55 * sa / 255);
        ctx.fill(px+32,py+50,px+pw-32,py+51,(divA<<24)|0xFFD700);

        // ── PROGRESS ──
        int barX = px+30, barY = py+72, barW = pw-60, barH = 9;
        String pct = (int)loadProgress + "%";
        int pA = (int)(210*sa/255);
        ctx.drawText(textRenderer, pct, cx-textRenderer.getWidth(pct)/2, barY-13, (pA<<24)|0x00F5FF, false);

        // Bar border
        ctx.fill(barX-1,barY-1,barX+barW+1,barY+barH+1,(int)(0x44*sa/255)<<24|0x00F5FF);
        ctx.fill(barX,barY,barX+barW,barY+barH,0x1A000000);

        // Bar gradient fill (purple → cyan)
        int fillW = (int)(barW * loadProgress / 100f);
        if (fillW > 0) {
            for (int xi = 0; xi < fillW; xi++) {
                float frac = (float)xi / barW;
                int r = (int)MathHelper.lerp(frac, 155f, 0f);
                int g = (int)MathHelper.lerp(frac, 0f,   245f);
                ctx.fill(barX+xi, barY, barX+xi+1, barY+barH, 0xFF000000|(r<<16)|(g<<8)|255);
            }
            // Cap glow
            int gcA2 = (int)(140*glowPulse*sa/255);
            ctx.fill(barX+fillW-6,barY-2,barX+fillW+2,barY+barH+2,(gcA2<<24)|0x00F5FF);
        }

        // Tile segments
        int tileY = barY + barH + 5;
        int tiles = 30, tileW = (barW - tiles + 1) / tiles;
        for (int i = 0; i < tiles; i++) {
            int txp = barX + i * (tileW + 1);
            boolean filled = i < (int)(loadProgress / 100f * tiles);
            int tcA = (int)((filled ? 0xCC : 0x14) * sa / 255);
            ctx.fill(txp, tileY, txp+tileW, tileY+3, (tcA<<24)|(filled?0x00F5FF:0xFFFFFF));
        }

        // Status message
        String msg = MESSAGES[messageIndex];
        int mA = (int)((140+80*glowPulse)*sa/255);
        ctx.drawText(textRenderer, msg, cx-textRenderer.getWidth(msg)/2, tileY+9, (mA<<24)|0xAAFFFF, false);

        // Blinking READY
        if (done && doneTimer%20 < 10) {
            String ready = ">>> READY <<<";
            int rA = (int)(230*sa/255);
            ctx.drawText(textRenderer, ready, cx-textRenderer.getWidth(ready)/2, tileY+22, (rA<<24)|0xFFD700, false);
        }

        // Copyright
        ctx.fill(px+32,py+ph-26,px+pw-32,py+ph-25,(int)(0x33*sa/255)<<24|0x00F5FF);
        String copy = "(c) Professor Client  --  Elite Fabric Mod  1.21.1";
        int copyA = (int)(75*sa/255);
        ctx.drawText(textRenderer, copy, cx-textRenderer.getWidth(copy)/2, py+ph-16, (copyA<<24)|0x9B00FF, false);
    }

    private int alphaBlend(int color, int sa) {
        int baseA = (color >>> 24) & 0xFF;
        int newA  = (int)(baseA * sa / 255);
        return (newA << 24) | (color & 0x00FFFFFF);
    }
}
