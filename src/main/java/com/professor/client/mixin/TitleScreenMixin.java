package com.professor.client.mixin;

import com.professor.client.ProfessorClientMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Unique private static final int BG      = 0xFF010A14;
    @Unique private static final int PANEL   = 0xFF030F1E;
    @Unique private static final int BORDER  = 0xFFAAEEFF;
    @Unique private static final int BORDER2 = 0xFF55CCFF;
    @Unique private static final int GOLD    = 0xFFFFD700;
    @Unique private static final int CRYSTAL = 0xFF00CCEE;
    @Unique private static final int TITLE_W = 0xFFEEF8FF;
    @Unique private static final int TITLE_G = 0xFFFFD700;

    @Unique private static final int SNOW_CNT  = 280;
    @Unique private final float[] sx  = new float[SNOW_CNT];
    @Unique private final float[] sy  = new float[SNOW_CNT];
    @Unique private final float[] ssp = new float[SNOW_CNT];
    @Unique private final float[] ssz = new float[SNOW_CNT];
    @Unique private final float[] sal = new float[SNOW_CNT];
    @Unique private final float[] sph = new float[SNOW_CNT];

    @Unique private static final int SPARK_CNT = 60;
    @Unique private final float[] gpx = new float[SPARK_CNT];
    @Unique private final float[] gpy = new float[SPARK_CNT];
    @Unique private final float[] gpp = new float[SPARK_CNT];
    @Unique private final float[] gps = new float[SPARK_CNT];

    @Unique private final float[] ringR  = {10f, 55f, 110f, 170f, 240f};
    @Unique private final float[] ringSp = {1.6f, 1.2f, 0.85f, 0.6f, 0.4f};

    @Unique private static final String DS = "XERION01CLIENT10BYPASS00ICE11FROST";
    @Unique private static final int   DC  = 50;
    @Unique private final float[] dsX  = new float[DC];
    @Unique private final float[] dsY  = new float[DC];
    @Unique private final float[] dsSp = new float[DC];
    @Unique private final int[]   dsCh = new int[DC];

    @Unique private boolean ready  = false;
    @Unique private int     frame  = 0;
    @Unique private float   glow   = 0f;
    @Unique private boolean glowUp = true;
    @Unique private float   hue    = 0f;
    @Unique private float   scanA  = 0f;
    @Unique private final Random rng = new Random();

    @Inject(at = @At("HEAD"), method = "init")
    private void xerion$onInit(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int W = mc.getWindow().getScaledWidth();
        int H = mc.getWindow().getScaledHeight();
        for (int i = 0; i < SNOW_CNT; i++) {
            sx[i]  = rng.nextFloat() * W;
            sy[i]  = rng.nextFloat() * H;
            ssp[i] = rng.nextFloat() * 0.85f + 0.18f;
            ssz[i] = rng.nextFloat() * 4.5f + 1f;
            sal[i] = rng.nextFloat() * 0.65f + 0.2f;
            sph[i] = rng.nextFloat() * 6.28f;
        }
        for (int i = 0; i < SPARK_CNT; i++) {
            gpx[i] = rng.nextFloat() * W;
            gpy[i] = rng.nextFloat() * H;
            gpp[i] = rng.nextFloat() * 6.28f;
            gps[i] = rng.nextFloat() * 3f + 1.5f;
        }
        for (int i = 0; i < DC; i++) {
            dsX[i]  = i * (W / (float)DC) + rng.nextFloat() * 6;
            dsY[i]  = rng.nextFloat() * -H;
            dsSp[i] = rng.nextFloat() * 1.5f + 0.4f;
            dsCh[i] = rng.nextInt(DS.length());
        }
        ready = true;
    }

    /* HEAD — update physics only */
    @Inject(at = @At("HEAD"), method = "render")
    private void xerion$onRenderHead(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        if (!ready) return;
        frame++;
        hue   = (hue + 0.0035f) % 1f;
        scanA = (scanA + 1.1f) % (ctx.getScaledWindowHeight() + 80);
        glow += glowUp ? 0.020f : -0.020f;
        if (glow >= 1f) { glow = 1f; glowUp = false; }
        else if (glow <= 0f) { glow = 0f; glowUp = true; }

        int W = ctx.getScaledWindowWidth(), H = ctx.getScaledWindowHeight();
        float drift = frame * 0.010f;

        for (int i = 0; i < SNOW_CNT; i++) {
            sy[i] += ssp[i];
            sx[i] += MathHelper.sin(drift + sph[i]) * 0.42f;
            sph[i] += 0.007f;
            if (sy[i] > H + 6) { sy[i] = -6; sx[i] = rng.nextFloat() * W; }
            if (sx[i] < -6) sx[i] = W + 5;
            if (sx[i] > W + 5) sx[i] = -6;
        }
        for (int i = 0; i < SPARK_CNT; i++) gpp[i] += 0.055f;
        for (int i = 0; i < ringR.length; i++) {
            ringR[i] += ringSp[i];
            if (ringR[i] > Math.max(W, H) * 0.88f) ringR[i] = 10f;
        }
        for (int i = 0; i < DC; i++) {
            dsY[i] += dsSp[i];
            if (dsY[i] > H + 20) { dsY[i] = -20; dsCh[i] = rng.nextInt(DS.length()); }
            if (rng.nextInt(22) == 0) dsCh[i] = rng.nextInt(DS.length());
        }
    }

    /* TAIL — draw our complete custom title OVER everything vanilla drew */
    @Inject(at = @At("TAIL"), method = "render")
    private void xerion$onRenderTail(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        if (!ready) return;

        int W = ctx.getScaledWindowWidth(), H = ctx.getScaledWindowHeight(), cx = W / 2;

        /* ── 1. FULL OPAQUE BACKGROUND — kills vanilla panorama ────────── */
        ctx.fill(0, 0, W, H, BG);
        for (int x = 0; x < W; x += 42) ctx.fill(x, 0, x+1, H, xa(0x113355, 7));
        for (int y = 0; y < H; y += 42) ctx.fill(0, y, W, y+1, xa(0x113355, 7));

        /* scan line */
        ctx.fill(0, (int)(scanA-2), W, (int)scanA, xa(BORDER2, 10));

        /* pulse rings centred in title zone */
        int rcy = H / 5;
        for (float r2 : ringR) {
            int r = (int)r2; if (r < 2) continue;
            float ff = 1f - r2 / (Math.max(W, H) * 0.88f);
            int ra = (int)(ff * ff * 38); if (ra <= 0) continue;
            ctx.fill(cx-r, rcy-1, cx+r, rcy+1, xa(0x55CCFF, ra));
            ctx.fill(cx-1, rcy-r, cx+1, rcy+r, xa(0x55CCFF, ra));
        }

        /* data stream */
        var tr = MinecraftClient.getInstance().textRenderer;
        if (tr != null) {
            for (int i = 0; i < DC; i++) {
                if (dsY[i] < 0) continue;
                ctx.drawText(tr, String.valueOf(DS.charAt(dsCh[i])), (int)dsX[i]-4, (int)dsY[i], xa(0x44AABB, 36), false);
            }
        }

        /* ── 2. SNOW + SPARKLES over entire screen ─────────────────────── */
        for (int i = 0; i < SNOW_CNT; i++) {
            float tw = (MathHelper.sin(sph[i]) + 1f) / 2f;
            int a = (int)(sal[i] * (0.42f + 0.58f * tw) * 220); if (a < 10) continue;
            int sz  = (int)ssz[i];
            int col = ssz[i] > 3.8f ? 0xFFFFFFFF : ssz[i] > 2.5f ? 0xFFCCEEFF : 0xFFAABBCC;
            ctx.fill((int)sx[i], (int)sy[i], (int)sx[i]+sz, (int)sy[i]+sz, xa(col, a));
            if (sz >= 3 && a > 110)
                ctx.fill((int)sx[i]+1,(int)sy[i]+1,(int)sx[i]+sz-1,(int)sy[i]+sz-1, xa(0xFFFFFF, Math.min(255,a+45)));
        }
        for (int i = 0; i < SPARK_CNT; i++) {
            float tw = (MathHelper.sin(gpp[i]) + 1f) / 2f; if (tw < 0.60f) continue;
            int a = (int)((tw-0.60f)*2.5f*210); if (a < 15) continue;
            int sz = (int)gps[i];
            ctx.fill((int)gpx[i]-sz,(int)gpy[i],(int)gpx[i]+sz+1,(int)gpy[i]+1, xa(0xFFFFFF,a));
            ctx.fill((int)gpx[i],(int)gpy[i]-sz,(int)gpx[i]+1,(int)gpy[i]+sz+1, xa(0xFFFFFF,a));
        }

        /* ── 3. TITLE PANEL: covers top portion with solid dark panel ──── */
        /* The vanilla buttons are drawn later in super.render — we draw
           our panel only up to  ~42 % of height, then buttons show below. */
        int cutY   = (int)(H * 0.44f);
        int titleY = (int)(H * 0.20f);

        /* solid panel for top 44 % */
        ctx.fill(0, 0, W, cutY, xa(PANEL, 248));

        /* top radial gradient (faint cyan glow from above) */
        for (int y = 0; y < 90; y++) {
            int ga = (int)((1f - y / 90f) * 16);
            if (ga > 0) ctx.fill(0, y, W, y+1, xa(0xAADDFF, ga));
        }
        /* bottom fade-out of panel into button area */
        for (int y = 0; y < 32; y++) {
            int fa = (int)((1f - y / 32f) * 245);
            ctx.fill(0, cutY - 32 + y, W, cutY - 31 + y, xa(PANEL, fa));
        }

        /* decorative thin dividers */
        int divA = (int)(75 + 55 * glow);
        ctx.fill(cx-240, titleY-24, cx+240, titleY-23, xa(BORDER2, divA));
        ctx.fill(cx-200, titleY+30, cx+200, titleY+31, xa(BORDER2, divA/2));

        /* crystal shards flanking the title */
        int csA = (int)(145 + 85 * glow);
        for (int si = 0; si < 6; si++) {
            drawShard(ctx, cx - 245 - si*15, titleY - 10 + si*3, xa(CRYSTAL, csA - si*18));
            drawShard(ctx, cx + 230 + si*15, titleY - 10 + si*3, xa(CRYSTAL, csA - si*18));
        }

        /* ── 4. BIG ICY "XERION CLIENT" TITLE ─────────────────────────── */
        if (tr != null) {
            String t1 = "XERION";
            String t2 = "CLIENT";

            /* render at 3× scale using matrix */
            int t1w3 = tr.getWidth(t1) * 3;
            int t2w3 = tr.getWidth(t2) * 3;
            int gap   = 8;
            int totalW = t1w3 + gap + t2w3;
            int sx3   = (cx - totalW / 2);
            int sy3   = titleY - 12;

            /* glow halo passes */
            int glA = (int)(48 + 42 * glow);
            ctx.getMatrices().push();
            ctx.getMatrices().scale(3f, 3f, 1f);
            for (int d = -5; d <= 5; d++) {
                int g2 = Math.max(0, glA - Math.abs(d) * 10); if (g2 == 0) continue;
                ctx.drawText(tr, t1, (sx3+d)/3, sy3/3, xa(0x00CCFF, g2), false);
                ctx.drawText(tr, t1, sx3/3, (sy3+d)/3, xa(0x00CCFF, g2), false);
                ctx.drawText(tr, t2, (sx3+t1w3+gap+d)/3, sy3/3, xa(0x00CCFF, g2), false);
                ctx.drawText(tr, t2, (sx3+t1w3+gap)/3, (sy3+d)/3, xa(0x00CCFF, g2), false);
            }
            ctx.getMatrices().pop();

            /* actual title — XERION in rainbow-ice, CLIENT in gold */
            int tA = (int)(235 + 20 * glow);
            int shimmer = hsvToRgb(hue, 0.42f, 1f);
            ctx.getMatrices().push();
            ctx.getMatrices().scale(3f, 3f, 1f);
            ctx.drawText(tr, t1, sx3/3,               sy3/3, xa(shimmer, tA), false);
            ctx.drawText(tr, t2, (sx3+t1w3+gap)/3,    sy3/3, xa(TITLE_G,  tA), false);
            ctx.getMatrices().pop();

            /* subtitle */
            String sub = "❄   Frost Engine  ·  " + ProfessorClientMod.VERSION + "  ·  Fabric 1.21.1   ❄";
            int subA = (int)(155 + 70 * glow);
            ctx.drawText(tr, sub, cx - tr.getWidth(sub)/2, sy3 + 26, xa(CRYSTAL, subA), false);

            /* hint */
            String hint = "—  Press  M  to open the client  —";
            int hintA = (int)(75 + 55 * glow);
            ctx.drawText(tr, hint, cx - tr.getWidth(hint)/2, sy3 + 42, xa(0xFF88AACC, hintA), false);

            /* top-right badge */
            String badge = ProfessorClientMod.CLIENT_NAME + "  ❄  v4.0";
            int bx2 = W - tr.getWidth(badge) - 8;
            int nameA = (int)(195 + 60 * glow);
            for (int d = 1; d <= 2; d++)
                ctx.drawText(tr, badge, bx2+d, 5+d, xa(0x000A14, nameA/(d*2+1)), false);
            ctx.drawText(tr, badge, bx2, 5, xa(0x00CCFF, (int)(45*glow)), false);
            ctx.drawText(tr, badge, bx2, 5, xa(TITLE_W, nameA), false);

            /* bottom status */
            String status = "❄  Xerion Frost Client  ·  M = Client Menu  ·  Fabric 1.21.1  ❄";
            ctx.drawText(tr, status, cx - tr.getWidth(status)/2, H-12, xa(0xFF55CCFF, (int)(65+50*glow)), false);
        }

        /* ── 5. FULL-SCREEN BORDER + CORNER ORNAMENTS ──────────────────── */
        int bA  = (int)(105 + 90 * glow);
        int ogA2 = (int)(30 * glow);
        if (ogA2 > 0) {
            for (int v = 0; v < 5; v++) {
                int la = ogA2 - v*6; if (la <= 0) break;
                ctx.fill(v, v, W-v, v+1,     xa(BORDER, la));
                ctx.fill(v, H-v-1, W-v, H-v, xa(BORDER, la));
                ctx.fill(v, v, v+1, H-v,     xa(BORDER, la));
                ctx.fill(W-v-1, v, W-v, H-v, xa(BORDER, la));
            }
        }
        ctx.fill(0, 0, W, 4,   xa(BORDER, bA));
        ctx.fill(0, H-4, W, H, xa(BORDER, bA));
        ctx.fill(0, 0, 4, H,   xa(BORDER, bA));
        ctx.fill(W-4, 0, W, H, xa(BORDER, bA));

        for (int x = 60; x < W-60; x += 55) {
            int tA = (int)(26 + 18 * MathHelper.sin(frame * 0.04f + x * 0.09f));
            if (tA > 0) ctx.fill(x, 0, x+1, 6, xa(BORDER2, tA));
        }

        int gcA = (int)(215 + 40 * glow);
        drawCorner(ctx, 0, 0, W, H, 30, xa(GOLD, gcA));

        int shA = (int)(130 + 75 * glow);
        drawShard(ctx, 3,    14,   xa(CRYSTAL, shA));
        drawShard(ctx, W-12, 14,   xa(CRYSTAL, shA));
        drawShard(ctx, 3,    H-22, xa(CRYSTAL, shA));
        drawShard(ctx, W-12, H-22, xa(CRYSTAL, shA));

        /* ── 6. Re-draw buttons area so they stay on top of everything ─── */
        /* Vanilla buttons were drawn before TAIL, so we need to re-call
           super.render here — but we CAN'T do that from a mixin TAIL.
           Instead we draw a subtle dark overlay ONLY below the cutY line
           so vanilla buttons remain fully readable. */
        for (int y = cutY; y < H; y++) {
            float t = (float)(y - cutY) / (H - cutY);
            int a = (int)((1f - t * t) * 30);
            if (a > 0) ctx.fill(0, y, W, y+1, xa(BG, a));
        }
    }

    @Unique private static int xa(int rgb, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0x00FFFFFF);
    }

    @Unique private static void drawCorner(DrawContext ctx, int x1, int y1, int x2, int y2, int cs, int col) {
        ctx.fill(x1,    y1,    x1+cs, y1+3, col); ctx.fill(x1,   y1,    x1+3, y1+cs, col);
        ctx.fill(x2-cs, y1,    x2,    y1+3, col); ctx.fill(x2-3, y1,    x2,   y1+cs, col);
        ctx.fill(x1,    y2-3,  x1+cs, y2,   col); ctx.fill(x1,   y2-cs, x1+3, y2,    col);
        ctx.fill(x2-cs, y2-3,  x2,    y2,   col); ctx.fill(x2-3, y2-cs, x2,   y2,    col);
    }

    @Unique private static void drawShard(DrawContext ctx, int x, int y, int col) {
        ctx.fill(x+3, y,   x+5, y+2,  col);
        ctx.fill(x+2, y+2, x+6, y+4,  col);
        ctx.fill(x+1, y+4, x+7, y+7,  col);
        ctx.fill(x+2, y+7, x+6, y+9,  col);
        ctx.fill(x+3, y+9, x+5, y+12, col);
    }

    @Unique private static int hsvToRgb(float h, float s, float v) {
        int i = (int)(h*6);
        float f=h*6-i, p=v*(1-s), q=v*(1-f*s), t=v*(1-(1-f)*s), r, g, b;
        switch (i%6) {
            case 0 -> { r=v; g=t; b=p; } case 1 -> { r=q; g=v; b=p; }
            case 2 -> { r=p; g=v; b=t; } case 3 -> { r=p; g=q; b=v; }
            case 4 -> { r=t; g=p; b=v; } default -> { r=v; g=p; b=q; }
        }
        return 0xFF000000|((int)(r*255)<<16)|((int)(g*255)<<8)|(int)(b*255);
    }
}
