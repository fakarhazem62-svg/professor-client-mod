package com.professor.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Random;

@Mixin(Screen.class)
public class AllScreensMixin {

    @Unique private static final int SNOW = 200;
    @Unique private static final float[] SX  = new float[SNOW];
    @Unique private static final float[] SY  = new float[SNOW];
    @Unique private static final float[] SSP = new float[SNOW];
    @Unique private static final float[] SSZ = new float[SNOW];
    @Unique private static final float[] SAL = new float[SNOW];
    @Unique private static final float[] SPH = new float[SNOW];

    @Unique private static final int SPARK = 45;
    @Unique private static final float[] GPX = new float[SPARK];
    @Unique private static final float[] GPY = new float[SPARK];
    @Unique private static final float[] GPP = new float[SPARK];
    @Unique private static final float[] GPS = new float[SPARK];

    @Unique private static boolean READY    = false;
    @Unique private static int     A_FRAME  = 0;
    @Unique private static float   A_GLOW   = 0f;
    @Unique private static boolean A_GLOWUP = true;
    @Unique private static final Random ARNG = new Random();

    @Unique private static final int C_BG  = 0xFF010A14;
    @Unique private static final int C_GRD = 0xFF113355;
    @Unique private static final int C_BOR = 0xFFAAEEFF;
    @Unique private static final int C_GOL = 0xFFFFD700;
    @Unique private static final int C_CRY = 0xFF00CCEE;
    @Unique private static final int C_TXT = 0xFFCCF0FF;

    @Inject(at = @At("HEAD"), method = "renderBackground", cancellable = true)
    private void xerion$bg(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) return;
        if ((Object)this instanceof TitleScreen) return;

        int W = ctx.getScaledWindowWidth(), H = ctx.getScaledWindowHeight();

        if (!READY) {
            for (int i = 0; i < SNOW; i++) {
                SX[i]  = ARNG.nextFloat() * W; SY[i]  = ARNG.nextFloat() * H;
                SSP[i] = ARNG.nextFloat() * .8f + .18f;
                SSZ[i] = ARNG.nextFloat() * 4f + 1f;
                SAL[i] = ARNG.nextFloat() * .65f + .2f;
                SPH[i] = ARNG.nextFloat() * 6.28f;
            }
            for (int i = 0; i < SPARK; i++) {
                GPX[i] = ARNG.nextFloat() * W; GPY[i] = ARNG.nextFloat() * H;
                GPP[i] = ARNG.nextFloat() * 6.28f; GPS[i] = ARNG.nextFloat() * 3f + 1.5f;
            }
            READY = true;
        }

        A_FRAME++;
        A_GLOW += A_GLOWUP ? 0.022f : -0.022f;
        if (A_GLOW >= 1f) { A_GLOW = 1f; A_GLOWUP = false; }
        else if (A_GLOW <= 0f) { A_GLOW = 0f; A_GLOWUP = true; }

        float drift = A_FRAME * 0.010f;
        for (int i = 0; i < SNOW; i++) {
            SY[i] += SSP[i];
            SX[i] += MathHelper.sin(drift + SPH[i]) * 0.40f;
            SPH[i] += 0.007f;
            if (SY[i] > H + 6) { SY[i] = -6; SX[i] = ARNG.nextFloat() * W; }
            if (SX[i] < -6) SX[i] = W + 5;
            if (SX[i] > W + 5) SX[i] = -6;
        }
        for (int i = 0; i < SPARK; i++) GPP[i] += 0.055f;

        // Ice background
        ctx.fill(0, 0, W, H, C_BG);
        for (int x = 0; x < W; x += 42) ctx.fill(x, 0, x+1, H, xa(C_GRD, 6));
        for (int y = 0; y < H; y += 42) ctx.fill(0, y, W, y+1, xa(C_GRD, 6));
        for (int y = 0; y < 60; y++) {
            int ga = (int)((1f - y/60f) * 12); if (ga > 0) ctx.fill(0, y, W, y+1, xa(0xAADDFF, ga));
        }

        // Snow flakes
        for (int i = 0; i < SNOW; i++) {
            float tw = (MathHelper.sin(SPH[i]) + 1f) / 2f;
            int a = (int)(SAL[i] * (0.4f + 0.6f * tw) * 200); if (a < 10) continue;
            int sz  = (int)SSZ[i];
            int col = SSZ[i] > 3.5f ? 0xFFFFFFFF : SSZ[i] > 2.5f ? 0xFFCCEEFF : 0xFFAABBCC;
            ctx.fill((int)SX[i],(int)SY[i],(int)SX[i]+sz,(int)SY[i]+sz, xa(col,a));
            if (sz >= 3 && a > 110)
                ctx.fill((int)SX[i]+1,(int)SY[i]+1,(int)SX[i]+sz-1,(int)SY[i]+sz-1, xa(0xFFFFFF, Math.min(255,a+40)));
        }
        // Sparkles
        for (int i = 0; i < SPARK; i++) {
            float tw = (MathHelper.sin(GPP[i]) + 1f) / 2f; if (tw < 0.60f) continue;
            int a = (int)((tw-.60f)*2.5f*200); if (a < 15) continue;
            int sz = (int)GPS[i];
            ctx.fill((int)GPX[i]-sz,(int)GPY[i],(int)GPX[i]+sz+1,(int)GPY[i]+1, xa(0xFFFFFF,a));
            ctx.fill((int)GPX[i],(int)GPY[i]-sz,(int)GPX[i]+1,(int)GPY[i]+sz+1, xa(0xFFFFFF,a));
        }

        ci.cancel();
    }

    @Inject(at = @At("TAIL"), method = "render")
    private void xerion$tail(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) return;
        if ((Object)this instanceof TitleScreen) return;
        if (!READY) return;

        int W = ctx.getScaledWindowWidth(), H = ctx.getScaledWindowHeight();

        // Glowing border
        int bA = (int)(85 + 70 * A_GLOW);
        ctx.fill(0, 0, W, 3,   xa(C_BOR, bA));
        ctx.fill(0, H-3, W, H, xa(C_BOR, bA));
        ctx.fill(0, 0, 3, H,   xa(C_BOR, bA));
        ctx.fill(W-3, 0, W, H, xa(C_BOR, bA));

        // Outer glow pulses
        for (int v = 1; v <= 3; v++) {
            int la = (int)((A_GLOW * 22) / v); if (la <= 0) continue;
            ctx.fill(v, v, W-v, v+1,     xa(C_BOR, la));
            ctx.fill(v, H-v-1, W-v, H-v, xa(C_BOR, la));
            ctx.fill(v, v, v+1, H-v,     xa(C_BOR, la));
            ctx.fill(W-v-1, v, W-v, H-v, xa(C_BOR, la));
        }

        // Gold corners
        int gcA = (int)(195 + 55 * A_GLOW);
        aCorner(ctx, 0, 0, W, H, 22, xa(C_GOL, gcA));

        // Crystal shards
        int shA = (int)(110 + 70 * A_GLOW);
        aShard(ctx, 3,    12,   xa(C_CRY, shA));
        aShard(ctx, W-11, 12,   xa(C_CRY, shA));
        aShard(ctx, 3,    H-20, xa(C_CRY, shA));
        aShard(ctx, W-11, H-20, xa(C_CRY, shA));

        // Badge — version v1
        var tr = mc.textRenderer;
        if (tr != null) {
            String badge = "Xerion Client  ❄  v1";
            int bx = W - tr.getWidth(badge) - 7;
            int na = (int)(180 + 60 * A_GLOW);
            for (int d = 1; d <= 2; d++)
                ctx.drawText(tr, badge, bx+d, 4+d, xa(0x000A14, na/(d*2+1)), false);
            ctx.drawText(tr, badge, bx, 4, xa(C_TXT, na), false);
        }
    }

    @Unique private static int xa(int rgb, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0x00FFFFFF);
    }
    @Unique private static void aCorner(DrawContext ctx, int x1, int y1, int x2, int y2, int cs, int col) {
        ctx.fill(x1,    y1,    x1+cs, y1+3, col); ctx.fill(x1,   y1,    x1+3, y1+cs, col);
        ctx.fill(x2-cs, y1,    x2,    y1+3, col); ctx.fill(x2-3, y1,    x2,   y1+cs, col);
        ctx.fill(x1,    y2-3,  x1+cs, y2,   col); ctx.fill(x1,   y2-cs, x1+3, y2,    col);
        ctx.fill(x2-cs, y2-3,  x2,    y2,   col); ctx.fill(x2-3, y2-cs, x2,   y2,    col);
    }
    @Unique private static void aShard(DrawContext ctx, int x, int y, int col) {
        ctx.fill(x+3,y,  x+5,y+2, col); ctx.fill(x+2,y+2,x+6,y+4, col);
        ctx.fill(x+1,y+4,x+7,y+7, col); ctx.fill(x+2,y+7,x+6,y+9, col);
        ctx.fill(x+3,y+9,x+5,y+12,col);
    }
}
