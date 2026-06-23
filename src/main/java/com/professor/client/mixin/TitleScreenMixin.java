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

    @Unique private static final int SNOW_CNT  = 220;
    @Unique private static final int SPARK_CNT = 45;

    @Unique private final float[] sx  = new float[SNOW_CNT];
    @Unique private final float[] sy  = new float[SNOW_CNT];
    @Unique private final float[] ssp = new float[SNOW_CNT];
    @Unique private final float[] ssz = new float[SNOW_CNT];
    @Unique private final float[] sal = new float[SNOW_CNT];
    @Unique private final float[] sph = new float[SNOW_CNT];

    @Unique private final float[] gpx = new float[SPARK_CNT];
    @Unique private final float[] gpy = new float[SPARK_CNT];
    @Unique private final float[] gpp = new float[SPARK_CNT];
    @Unique private final float[] gps = new float[SPARK_CNT];

    @Unique private boolean snowInit = false;
    @Unique private int     frame    = 0;
    @Unique private float   glow     = 0f;
    @Unique private boolean glowUp   = true;
    @Unique private float   hue      = 0f;
    @Unique private final Random rng = new Random();

    @Inject(at = @At("HEAD"), method = "init")
    private void xerion$onInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        int W = client.getWindow().getScaledWidth();
        int H = client.getWindow().getScaledHeight();
        for (int i = 0; i < SNOW_CNT; i++) {
            sx[i]  = rng.nextFloat() * W;
            sy[i]  = rng.nextFloat() * H;
            ssp[i] = rng.nextFloat() * 0.75f + 0.18f;
            ssz[i] = rng.nextFloat() * 4f + 1f;
            sal[i] = rng.nextFloat() * 0.6f + 0.2f;
            sph[i] = rng.nextFloat() * 6.28f;
        }
        for (int i = 0; i < SPARK_CNT; i++) {
            gpx[i] = rng.nextFloat() * W;
            gpy[i] = rng.nextFloat() * H;
            gpp[i] = rng.nextFloat() * 6.28f;
            gps[i] = rng.nextFloat() * 2.5f + 1f;
        }
        snowInit = true;
    }

    @Inject(at = @At("HEAD"), method = "render")
    private void xerion$onRenderHead(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        if (!snowInit) return;
        frame++;
        hue = (hue + 0.003f) % 1f;
        glow += glowUp ? 0.022f : -0.022f;
        if (glow >= 1f) { glow = 1f; glowUp = false; }
        else if (glow <= 0f) { glow = 0f; glowUp = true; }

        int W = ctx.getScaledWindowWidth();
        int H = ctx.getScaledWindowHeight();
        float drift = frame * 0.009f;

        for (int i = 0; i < SNOW_CNT; i++) {
            sy[i] += ssp[i];
            sx[i] += MathHelper.sin(drift + sph[i]) * 0.38f;
            sph[i] += 0.007f;
            if (sy[i] > H + 6) { sy[i] = -6; sx[i] = rng.nextFloat() * W; }
            if (sx[i] < -6) sx[i] = W + 5;
            if (sx[i] > W + 5) sx[i] = -6;
        }
        for (int i = 0; i < SPARK_CNT; i++) gpp[i] += 0.052f;
    }

    @Inject(at = @At("TAIL"), method = "render")
    private void xerion$onRenderTail(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        if (!snowInit) return;
        int W = ctx.getScaledWindowWidth();
        int H = ctx.getScaledWindowHeight();

        // ── Snow particles ───────────────────────────────────────────────
        for (int i = 0; i < SNOW_CNT; i++) {
            float tw = (MathHelper.sin(sph[i]) + 1f) / 2f;
            int a = (int)(sal[i] * (0.4f + 0.6f * tw) * 210);
            if (a < 10) continue;
            int sz  = (int)ssz[i];
            int col = ssz[i] > 3.5f ? 0xFFFFFFFF : ssz[i] > 2.5f ? 0xFFCCEEFF : 0xFFAABBCC;
            ctx.fill((int)sx[i], (int)sy[i], (int)sx[i]+sz, (int)sy[i]+sz, withA(col, a));
            if (sz >= 3 && a > 100)
                ctx.fill((int)sx[i]+1,(int)sy[i]+1,(int)sx[i]+sz-1,(int)sy[i]+sz-1, withA(0xFFFFFFFF, Math.min(255,a+40)));
        }

        // ── Star sparkles ────────────────────────────────────────────────
        for (int i = 0; i < SPARK_CNT; i++) {
            float tw = (MathHelper.sin(gpp[i]) + 1f) / 2f;
            if (tw < 0.58f) continue;
            int a = (int)((tw - 0.58f) * 2.38f * 190);
            if (a < 15) continue;
            int sz = (int)gps[i];
            ctx.fill((int)gpx[i]-sz,(int)gpy[i],(int)gpx[i]+sz+1,(int)gpy[i]+1, withA(0xFFFFFFFF,a));
            ctx.fill((int)gpx[i],(int)gpy[i]-sz,(int)gpx[i]+1,(int)gpy[i]+sz+1, withA(0xFFFFFFFF,a));
        }

        // ── Crystal border (top + bottom glow edges) ─────────────────────
        int bA = (int)(90 + 75 * glow);
        for (int y = 0; y < 5; y++) {
            int la = bA - y * 18; if (la <= 0) break;
            ctx.fill(0, y, W, y+1, withA(0xFF55CCFF, la));
            ctx.fill(0, H-y-1, W, H-y, withA(0xFF55CCFF, la));
        }
        for (int x = 0; x < 3; x++) {
            int la = bA/2 - x*12; if (la <= 0) break;
            ctx.fill(x, 0, x+1, H, withA(0xFF55CCFF, la));
            ctx.fill(W-x-1, 0, W-x, H, withA(0xFF55CCFF, la));
        }

        // ── Gold corner ornaments ─────────────────────────────────────────
        int gcA = (int)(200 + 55 * glow);
        drawCorner(ctx, 0, 0, W, H, 22, withA(0xFFFFD700, gcA));

        // ── Small ice crystal ticks along top border ──────────────────────
        for (int x = 40; x < W - 40; x += 60) {
            int tA = (int)(30 + 25 * MathHelper.sin(frame * 0.04f + x * 0.1f));
            if (tA > 0) ctx.fill(x, 0, x+1, 4, withA(0xFFAAEEFF, tA));
        }

        // ── Client name badge (top-right) ─────────────────────────────────
        var tr = MinecraftClient.getInstance().textRenderer;
        if (tr == null) return;

        String badge = ProfessorClientMod.CLIENT_NAME + "  ❄  v4.0";
        int bw = tr.getWidth(badge);
        int nameA = (int)(190 + 60 * glow);

        // Shadow layers
        for (int d = 1; d <= 2; d++)
            ctx.drawText(tr, badge, W-bw-7+d, 5+d, withA(0xFF000A14, nameA/(d*2+1)), false);

        // Cyan glow halo
        int gha = (int)(55 * glow);
        if (gha > 0) ctx.drawText(tr, badge, W-bw-7, 5, withA(0xFF00CCFF, gha), false);

        // Main text — ice white
        ctx.drawText(tr, badge, W-bw-7, 5, withA(0xFFCCF0FF, nameA), false);

        // ── Bottom-left status ────────────────────────────────────────────
        String status = "❄  Frost Engine  ·  Press M to open";
        int stA = (int)(75 + 45 * glow);
        ctx.drawText(tr, status, 6, H-13, withA(0xFF55CCFF, stA), false);

        // ── Tiny ice shards in all 4 corners ─────────────────────────────
        drawShard(ctx, 2, 12, withA(0xFF88DDFF, (int)(120+60*glow)));
        drawShard(ctx, W-10, 12, withA(0xFF88DDFF, (int)(120+60*glow)));
        drawShard(ctx, 2, H-18, withA(0xFF88DDFF, (int)(100+50*glow)));
        drawShard(ctx, W-10, H-18, withA(0xFF88DDFF, (int)(100+50*glow)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    @Unique private static int withA(int rgb, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0x00FFFFFF);
    }

    @Unique private static void drawCorner(DrawContext ctx, int x1, int y1, int x2, int y2, int cs, int col) {
        ctx.fill(x1,    y1,    x1+cs, y1+2, col); ctx.fill(x1,   y1,   x1+2, y1+cs, col);
        ctx.fill(x2-cs, y1,    x2,    y1+2, col); ctx.fill(x2-2, y1,   x2,   y1+cs, col);
        ctx.fill(x1,    y2-2,  x1+cs, y2,   col); ctx.fill(x1,   y2-cs,x1+2, y2,    col);
        ctx.fill(x2-cs, y2-2,  x2,    y2,   col); ctx.fill(x2-2, y2-cs,x2,   y2,    col);
    }

    @Unique private static void drawShard(DrawContext ctx, int x, int y, int col) {
        ctx.fill(x+3, y,   x+4, y+2,  col);
        ctx.fill(x+2, y+2, x+5, y+3,  col);
        ctx.fill(x+1, y+3, x+6, y+5,  col);
        ctx.fill(x+2, y+5, x+5, y+6,  col);
        ctx.fill(x+3, y+6, x+4, y+8,  col);
    }
}
