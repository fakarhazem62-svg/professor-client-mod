package com.professor.client.gui;

import com.professor.client.ProfessorClientMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class KeyActivationScreen extends Screen {

    // ══════════════════════════════════════════════════════════════════════
    //  ICE PALETTE — fully opaque for maximum visibility
    // ══════════════════════════════════════════════════════════════════════
    private static final int BG       = 0xFF010A14;
    private static final int PANEL_BG = 0xFF041828;
    private static final int BORDER   = 0xFFAAEEFF;
    private static final int BORDER2  = 0xFF55CCFF;
    private static final int TITLE1   = 0xFFEEF8FF;
    private static final int TITLE2   = 0xFFFFD700;
    private static final int TXT_ICE  = 0xFF88DDFF;
    private static final int TXT_DIM  = 0xFF336688;
    private static final int SNOW_W   = 0xFFFFFFFF;
    private static final int SNOW_B   = 0xFFCCEEFF;
    private static final int CRYSTAL  = 0xFF00CCEE;
    private static final int GOLD     = 0xFFFFD700;

    private float  progress = 0f;
    private float  fadeIn   = 0f;
    private float  fadeOut  = 0f;
    private boolean closing = false;
    private int    frame    = 0;
    private float  glow     = 0f;
    private boolean glowUp  = true;

    // Snow
    private static final int SNOW_COUNT = 180;
    private final float[] sx  = new float[SNOW_COUNT];
    private final float[] sy  = new float[SNOW_COUNT];
    private final float[] ssp = new float[SNOW_COUNT];
    private final float[] ssz = new float[SNOW_COUNT];
    private final float[] sal = new float[SNOW_COUNT];
    private final float[] sph = new float[SNOW_COUNT];

    // Sparkles
    private static final int SPARK = 50;
    private final float[] gpx = new float[SPARK], gpy = new float[SPARK];
    private final float[] gpp = new float[SPARK], gps = new float[SPARK];

    // Rings
    private final float[] ringR  = {12,65,120,175,235,295,355};
    private final float[] ringSp = {2.1f,1.6f,1.3f,1.0f,0.75f,0.5f,0.35f};

    // Matrix rain
    private static final String CHARS = "XERION01CLIENT10BYPASS11ICE";
    private static final int COLS = 55;
    private final float[] rX = new float[COLS], rY = new float[COLS];
    private final float[] rSp = new float[COLS]; private final int[] rCh = new int[COLS];

    private static final String[] MSGS = {
        "Initializing Xerion subsystems...",
        "Loading frost modules  [✓]",
        "Bypass calibration  [✓]",
        "Packet hooks established  [✓]",
        "Frost engine online  [✓]",
        "❄  Welcome, operator.  ❄"
    };

    private final Random rng = new Random();

    public KeyActivationScreen() { super(Text.literal("Xerion Client")); }

    @Override protected void init() {
        for (int i = 0; i < SNOW_COUNT; i++) {
            sx[i] = rng.nextFloat() * width;  sy[i] = rng.nextFloat() * height;
            ssp[i] = rng.nextFloat() * 0.85f + 0.2f; ssz[i] = rng.nextFloat() * 4f + 1f;
            sal[i] = rng.nextFloat() * 0.75f + 0.25f; sph[i] = rng.nextFloat() * 6.28f;
        }
        for (int i = 0; i < SPARK; i++) {
            gpx[i] = rng.nextFloat() * width; gpy[i] = rng.nextFloat() * height;
            gpp[i] = rng.nextFloat() * 6.28f; gps[i] = rng.nextFloat() * 3f + 1.5f;
        }
        for (int i = 0; i < COLS; i++) {
            rX[i] = (i + 0.5f) * (width / (float)COLS);
            rY[i] = rng.nextFloat() * -height;
            rSp[i] = rng.nextFloat() * 2.0f + 0.5f;
            rCh[i] = rng.nextInt(CHARS.length());
        }
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        frame++;
        if (!closing) {
            fadeIn   = Math.min(1f, fadeIn + 0.06f);
            progress = Math.min(1f, progress + 0.0095f);
            if (progress >= 1f) closing = true;
        } else {
            fadeOut = Math.min(1f, fadeOut + 0.075f);
            if (fadeOut >= 1f) { MinecraftClient.getInstance().setScreen(new ProfessorScreen()); return; }
        }

        float alpha = fadeIn * (1f - fadeOut);
        int sa = (int)(alpha * 255); if (sa <= 0) return;

        int W = width, H = height, pcx = W / 2, pcy = H / 2;

        glow += glowUp ? 0.033f : -0.033f;
        if (glow >= 1f){glow=1f;glowUp=false;} else if (glow <= 0f){glow=0f;glowUp=true;}

        float drift = frame * 0.011f;

        // Update snow
        for (int i = 0; i < SNOW_COUNT; i++) {
            sy[i] += ssp[i]; sx[i] += (float)Math.sin(drift + sph[i]) * 0.4f; sph[i] += 0.007f;
            if (sy[i] > H + 6) { sy[i] = -6; sx[i] = rng.nextFloat() * W; }
            if (sx[i] < -6) sx[i] = W + 5; if (sx[i] > W + 5) sx[i] = -6;
        }
        for (int i = 0; i < SPARK; i++) gpp[i] += 0.052f;
        // Update rings
        for (int i = 0; i < ringR.length; i++) { ringR[i] += ringSp[i]; if (ringR[i] > Math.max(W,H)*.92f) ringR[i] = 12f; }
        // Update rain
        for (int i = 0; i < COLS; i++) { rY[i] += rSp[i]; if (rY[i] > H + 20) { rY[i] = -20; rCh[i] = rng.nextInt(CHARS.length()); } if (rng.nextInt(25) == 0) rCh[i] = rng.nextInt(CHARS.length()); }

        // ── BACKGROUND ──────────────────────────────────────────────────
        ctx.fill(0, 0, W, H, BG);

        // Grid
        for (int x = 0; x < W; x += 44) { int a = 8 * sa / 255; if(a>0) ctx.fill(x, 0, x+1, H, (a<<24)|0x113355); }
        for (int y = 0; y < H; y += 44) { int a = 8 * sa / 255; if(a>0) ctx.fill(0, y, W, y+1, (a<<24)|0x113355); }

        // Rings
        for (int i = 0; i < ringR.length; i++) {
            int r = (int)ringR[i]; if (r < 2) continue;
            float ff = 1f - ringR[i] / (Math.max(W,H) * .92f);
            int ra = (int)(ff * ff * 50 * sa / 255); if (ra <= 0) continue;
            ctx.fill(pcx-r,pcy-1,pcx+r,pcy+1,(ra<<24)|0x55CCFF);
            ctx.fill(pcx-1,pcy-r,pcx+1,pcy+r,(ra<<24)|0x55CCFF);
        }

        // Matrix rain (faint ice)
        for (int i = 0; i < COLS; i++) {
            if (rY[i] < 0) continue;
            int ra = 40 * sa / 255; if (ra <= 0) continue;
            ctx.drawText(textRenderer, String.valueOf(CHARS.charAt(rCh[i])), (int)rX[i]-4, (int)rY[i], (ra<<24)|0x44BBDD, false);
        }

        // ── SNOW ────────────────────────────────────────────────────────
        for (int i = 0; i < SNOW_COUNT; i++) {
            float tw = (MathHelper.sin(sph[i]) + 1f) / 2f;
            int a = (int)(sal[i] * (0.5f + 0.5f * tw) * 255 * alpha);
            if (a < 12) continue;
            int sz = (int)ssz[i];
            int col = ssz[i] > 3.5f ? SNOW_W : ssz[i] > 2.5f ? SNOW_B : 0xFFAABBCC;
            ctx.fill((int)sx[i],(int)sy[i],(int)sx[i]+sz,(int)sy[i]+sz,withAlpha(col,a));
            if (sz >= 3 && a > 130) ctx.fill((int)sx[i]+1,(int)sy[i]+1,(int)sx[i]+sz-1,(int)sy[i]+sz-1,withAlpha(SNOW_W,Math.min(255,a+50)));
        }
        // Sparkles
        for (int i = 0; i < SPARK; i++) {
            float tw = (MathHelper.sin(gpp[i]) + 1f) / 2f; if (tw < 0.65f) continue;
            int a = (int)((tw-0.65f)*2.86f*200*alpha); if (a<20) continue;
            int sz=(int)gps[i];
            ctx.fill((int)gpx[i]-sz,(int)gpy[i],(int)gpx[i]+sz,(int)gpy[i]+1,withAlpha(SNOW_W,a));
            ctx.fill((int)gpx[i],(int)gpy[i]-sz,(int)gpx[i]+1,(int)gpy[i]+sz,withAlpha(SNOW_W,a));
        }

        // ── PANEL (500 x 310) ──────────────────────────────────────────
        int pw = 500, ph = 310, ppx = pcx - pw/2, ppy = pcy - ph/2;

        // Shadow
        ctx.fill(ppx+14,ppy+14,ppx+pw+14,ppy+ph+14,withAlpha(0xFF000000,140*sa/255));
        ctx.fill(ppx+7, ppy+7, ppx+pw+7, ppy+ph+7, withAlpha(0xFF000000,60*sa/255));

        // Body — solid opaque dark ice
        ctx.fill(ppx,ppy,ppx+pw,ppy+ph,fade(PANEL_BG,sa));
        for(int y=0;y<55;y++){int ga=(int)((1f-y/55f)*20*alpha);if(ga>0)ctx.fill(ppx,ppy+y,ppx+pw,ppy+y+1,(ga<<24)|0xAADDFF);}

        // ── BORDER — 5px SOLID BRIGHT ICE ────────────────────────────
        int bA = (int)((0.92f+0.08f*glow)*255*alpha);
        // Outer glow
        int ogA = (int)(0.28f*glow*255*alpha);
        if(ogA>0){ctx.fill(ppx-4,ppy-4,ppx+pw+4,ppy+1,withAlpha(BORDER,ogA));ctx.fill(ppx-4,ppy+ph-1,ppx+pw+4,ppy+ph+4,withAlpha(BORDER,ogA));ctx.fill(ppx-4,ppy-4,ppx+1,ppy+ph+4,withAlpha(BORDER,ogA));ctx.fill(ppx+pw-1,ppy-4,ppx+pw+4,ppy+ph+4,withAlpha(BORDER,ogA));}
        // Solid 5px
        ctx.fill(ppx,     ppy,     ppx+pw,  ppy+5,    withAlpha(BORDER,bA));
        ctx.fill(ppx,     ppy+ph-5,ppx+pw,  ppy+ph,   withAlpha(BORDER,bA));
        ctx.fill(ppx,     ppy,     ppx+5,   ppy+ph,   withAlpha(BORDER,bA));
        ctx.fill(ppx+pw-5,ppy,     ppx+pw,  ppy+ph,   withAlpha(BORDER,bA));
        // Inner highlight
        int ihA=(int)(0.45f*255*alpha);
        ctx.fill(ppx+5,ppy+5,ppx+pw-5,ppy+6,withAlpha(TITLE1,ihA));
        ctx.fill(ppx+5,ppy+ph-6,ppx+pw-5,ppy+ph-5,withAlpha(TITLE1,ihA));

        // GOLD corners
        int gcA=(int)(0.95f*255*alpha);
        drawCorner(ctx,ppx,ppy,ppx+pw,ppy+ph,32,withAlpha(GOLD,gcA));

        // ── TITLE ───────────────────────────────────────────────────────
        String t1="XERION",t2=" CLIENT";
        int tw1=textRenderer.getWidth(t1),tw2=textRenderer.getWidth(t2);
        int tx=pcx-(tw1+tw2)/2,ty=ppy+32;
        int glA=(int)((55+40*glow)*alpha);
        for(int d=-6;d<=6;d++){int ga=Math.max(0,glA-Math.abs(d)*8);if(ga>0){ctx.drawText(textRenderer,t1+t2,tx+d,ty,(ga<<24)|0x00CCFF,false);ctx.drawText(textRenderer,t1+t2,tx,ty+d,(ga<<24)|0x00CCFF,false);}}
        int tA=(int)((225+30*glow)*alpha);
        ctx.drawText(textRenderer,t1,tx,ty,withAlpha(TITLE1,tA),false);
        ctx.drawText(textRenderer,t2,tx+tw1,ty,withAlpha(TITLE2,tA),false);

        String sub="v2.0  ❄  Frost Engine  ❄  Advanced Client  ❄";
        int sA2=(int)((165+55*glow)*alpha);
        ctx.drawText(textRenderer,sub,pcx-textRenderer.getWidth(sub)/2,ty+13,withAlpha(CRYSTAL,sA2),false);

        // Divider
        int divA=(int)(120*alpha);
        ctx.fill(ppx+30,ppy+54,ppx+pw-30,ppy+55,withAlpha(BORDER2,divA));

        // Status
        int mi=Math.min(MSGS.length-1,(int)(progress*MSGS.length));
        int mA=(int)((195+60*glow)*alpha);
        ctx.drawText(textRenderer,MSGS[mi],pcx-textRenderer.getWidth(MSGS[mi])/2,ppy+64,withAlpha(TXT_ICE,mA),false);

        // ── Progress bar ─────────────────────────────────────────────────
        int bx=ppx+35,by=ppy+83,bw=pw-70,bh=14;
        String ps=(int)(progress*100)+"%";
        ctx.drawText(textRenderer,ps,pcx-textRenderer.getWidth(ps)/2,by-15,withAlpha(TITLE1,(int)(235*alpha)),false);
        ctx.fill(bx-2,by-2,bx+bw+2,by+bh+2,withAlpha(BORDER,(int)(150*alpha)));
        ctx.fill(bx,by,bx+bw,by+bh,fade(0xFF010E1E,sa));
        int fw=(int)(bw*progress);
        for(int xi=0;xi<fw;xi++){float fr=(float)xi/bw;int r=(int)MathHelper.lerp(fr,10f,170f);int g=(int)MathHelper.lerp(fr,80f,235f);ctx.fill(bx+xi,by,bx+xi+1,by+bh,0xFF000000|(r<<16)|(g<<8)|255);}
        if(fw>3){int capA=(int)((190+65*glow)*alpha);ctx.fill(bx+fw-5,by-2,bx+fw+5,by+bh+2,withAlpha(TITLE1,capA));}
        for(int s=1;s<22;s++){int sx2=bx+bw*s/22;ctx.fill(sx2,by,sx2+1,by+bh,fade(0x22000000,sa));}

        // Segment tiles
        int tileY=by+bh+6; int tiles=26; int tileW=(bw-tiles+1)/tiles;
        for(int i=0;i<tiles;i++){int txp=ppx+35+i*(tileW+1);boolean filled=i<(int)(progress*tiles);int tcA=(int)((filled?200:22)*alpha);ctx.fill(txp,tileY,txp+tileW,tileY+5,withAlpha(filled?BORDER2:0xFF335566,tcA));}

        // Footer
        ctx.drawText(textRenderer,"❄  Xerion Client  —  Frost Edition  —  ESC to cancel  ❄",pcx-textRenderer.getWidth("❄  Xerion Client  —  Frost Edition  —  ESC to cancel  ❄")/2,ppy+ph-16,withAlpha(TXT_DIM,(int)(110*alpha)),false);

        super.render(ctx,mx,my,delta);
    }

    private static int withAlpha(int rgb,int a){return(Math.max(0,Math.min(255,a))<<24)|(rgb&0x00FFFFFF);}
    private static int fade(int argb,int sa){int baseA=(argb>>>24)&0xFF;return withAlpha(argb,baseA*sa/255);}
    private static void drawCorner(DrawContext ctx,int x1,int y1,int x2,int y2,int cs,int col){ctx.fill(x1,y1,x1+cs,y1+5,col);ctx.fill(x1,y1,x1+5,y1+cs,col);ctx.fill(x2-cs,y1,x2,y1+5,col);ctx.fill(x2-5,y1,x2,y1+cs,col);ctx.fill(x1,y2-5,x1+cs,y2,col);ctx.fill(x1,y2-cs,x1+5,y2,col);ctx.fill(x2-cs,y2-5,x2,y2,col);ctx.fill(x2-5,y2-cs,x2,y2,col);}
}
