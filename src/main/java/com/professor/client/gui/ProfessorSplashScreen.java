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

    // ─── State ────────────────────────────────────────────────────────────
    private int   ticks        = 0;
    private float load         = 0f;
    private int   msgIdx       = 0;
    private boolean done       = false;
    private int   doneTimer    = 0;
    private float fadeIn       = 0f;
    private float fadeOut      = 0f;
    private float glow         = 0f;
    private boolean glowUp     = true;
    private float scan         = 0f, scan2 = 0f;

    // ─── Particles [x,y,vx,vy,sz,alpha,phase,type] ────────────────────────
    private final List<float[]> particles = new ArrayList<>();
    // ─── Data streams [x,y,speed,char] ────────────────────────────────────
    private final List<float[]> streams   = new ArrayList<>();
    // ─── Orbs [x,y,vx,vy,sz,alpha,phase,type] ────────────────────────────
    private final List<float[]> orbs      = new ArrayList<>();

    private static final int   CYAN   = 0x00F5FF;
    private static final int   PURPLE = 0x9B00FF;
    private static final int   GOLD   = 0xFFD700;
    private static final int   RED    = 0xFF2200;
    private static final int   GREEN  = 0x00FF88;

    private static final String CHARS = "PROFESSOR01AKASATANA10110100";

    private static final String[] MSGS = {
        "Initializing Professor Client v5.0...",
        "Loading exploit modules...",
        "Injecting packet hooks...",
        "Calibrating ExploitFixer bypass...",
        "Loading GUI components...",
        "Mounting Silhouette audio...",
        "All systems ready.  ExploitFixer: BYPASSED."
    };

    public ProfessorSplashScreen() { super(Text.literal("Professor Client")); }

    @Override protected void init()         { initFx(); }
    @Override public boolean shouldPause()  { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }

    // ─── Init ─────────────────────────────────────────────────────────────

    private void initFx() {
        particles.clear(); streams.clear(); orbs.clear();

        for (int i = 0; i < 300; i++) {
            particles.add(new float[]{
                RNG.nextFloat()*width,  RNG.nextFloat()*height,
                (RNG.nextFloat()-0.5f)*0.6f, -(RNG.nextFloat()*0.8f+0.1f),
                RNG.nextFloat()*3f+0.5f, RNG.nextFloat()*0.9f+0.1f,
                RNG.nextFloat()*6.28f,  RNG.nextInt(5)
            });
        }

        int cols = Math.max(1, width/14);
        for (int i = 0; i < cols; i++) {
            streams.add(new float[]{
                i*14+7f, RNG.nextFloat()*-height,
                RNG.nextFloat()*1.5f+0.4f, RNG.nextInt(CHARS.length())
            });
        }

        for (int i = 0; i < 18; i++) {
            orbs.add(new float[]{
                RNG.nextFloat()*width, RNG.nextFloat()*height,
                (RNG.nextFloat()-0.5f)*0.28f, (RNG.nextFloat()-0.5f)*0.28f,
                RNG.nextFloat()*70f+20f, RNG.nextFloat()*0.4f+0.2f,
                RNG.nextFloat()*6.28f,  RNG.nextInt(4)
            });
        }
    }

    // ─── Tick ─────────────────────────────────────────────────────────────

    @Override public void tick() {
        ticks++;

        fadeIn = Math.min(1f, fadeIn + 0.055f);

        if (load < 100f) {
            load = Math.min(load + (load < 30 ? 0.55f : load < 70 ? 0.38f : 0.26f), 100f);
            msgIdx = Math.min((int)(load/100f*(MSGS.length-1)), MSGS.length-1);
        } else if (!done) { done = true; }

        if (done) {
            doneTimer++;
            if (doneTimer > 60) {
                fadeOut = Math.min(1f, fadeOut + 0.04f);
                if (fadeOut >= 1f) MinecraftClient.getInstance().setScreen(null);
            }
        }

        glow += glowUp ? 0.032f : -0.032f;
        if (glow >= 1f) { glow = 1f; glowUp = false; }
        else if (glow <= 0f) { glow = 0f; glowUp = true; }

        scan  = (scan  + 2.5f) % height;
        scan2 = (scan2 + 1.5f) % height;

        for (float[] p : particles) {
            p[0]+=p[2]; p[1]+=p[3]; p[6]+=0.048f;
            if (p[1]<-10){p[1]=height+5;p[0]=RNG.nextFloat()*width;}
            if (p[0]<0)p[0]=width; if (p[0]>width)p[0]=0;
        }
        for (float[] s : streams) {
            s[1]+=s[2];
            if (s[1]>height+20){s[1]=-24;s[3]=RNG.nextInt(CHARS.length());}
            if (RNG.nextInt(20)==0) s[3]=RNG.nextInt(CHARS.length());
        }
        for (float[] o : orbs) {
            o[0]+=o[2]; o[1]+=o[3]; o[6]+=0.024f;
            if (o[0]<0||o[0]>width) o[2]=-o[2];
            if (o[1]<0||o[1]>height)o[3]=-o[3];
        }
    }

    // ─── Render ───────────────────────────────────────────────────────────

    @Override public void render(DrawContext ctx, int mx, int my, float delta) {
        float alpha = (1f - fadeOut) * fadeIn;
        int sa = (int)(alpha * 255); if (sa <= 0) return;

        int W=width, H=height, cx=W/2, cy=H/2;

        // Dark BG
        ctx.fill(0,0,W,H,ab(0xFF06060C,sa));

        // Grid
        int gc=ab(0x07000000|CYAN,sa);
        for(int x=0;x<W;x+=50)ctx.fill(x,0,x+1,H,gc);
        for(int y=0;y<H;y+=50)ctx.fill(0,y,W,y+1,gc);

        // Center radial glow
        int[] radii={420,310,200,120,70},alphas={4,8,14,22,35};
        for(int i=0;i<radii.length;i++){int a2=ab((int)(alphas[i]*(0.5f+0.5f*glow))<<24|CYAN,sa);ctx.fill(cx-radii[i],cy-radii[i]/2,cx+radii[i],cy+radii[i]/2,a2);}

        // Orbs
        for(float[] o:orbs){float tw=(MathHelper.sin(o[6])+1f)/2f;int a2=(int)(o[5]*tw*sa*0.25f);if(a2<=0)continue;int sz=(int)o[4];int c2=switch((int)o[7]){case 1->PURPLE;case 2->GOLD;case 3->RED;default->CYAN;};ctx.fill((int)o[0]-sz,(int)o[1]-sz/2,(int)o[0]+sz,(int)o[1]+sz/2,(a2<<24)|c2);}

        // Streams
        for(float[] s:streams){if(s[1]<0)continue;int hA=(int)((55+45*glow)*sa/255);ctx.drawText(textRenderer,String.valueOf(CHARS.charAt((int)s[3])),(int)s[0]-4,(int)s[1],(Math.min(255,hA*2)<<24)|CYAN,false);if(s[1]>14)ctx.drawText(textRenderer,String.valueOf(CHARS.charAt((int)s[3])),(int)s[0]-4,(int)s[1]-14,(hA<<24)|0x003344,false);}

        // Particles
        for(float[] p:particles){float tw=(MathHelper.sin(p[6])+1f)/2f;int a2=(int)(p[5]*tw*sa);if(a2<8)continue;boolean big=p[4]>2f;int col=switch((int)p[7]){case 1->(a2<<24)|(big?PURPLE:0x220033);case 2->(a2<<24)|(big?GOLD:0x443300);case 3->(a2<<24)|(big?RED:0x330000);case 4->(a2<<24)|(big?GREEN:0x003311);default->(a2<<24)|(big?CYAN:0x003344);};int sz=p[4]>2.5f?2:1;ctx.fill((int)p[0],(int)p[1],(int)p[0]+sz,(int)p[1]+sz,col);}

        // Scanlines
        int slA=(int)(0x0D*sa/255);
        if(slA>0){ctx.fill(0,(int)scan,W,(int)scan+2,(slA<<24)|0xFFFFFF);ctx.fill(0,(int)scan2,W,(int)scan2+1,(slA/2<<24)|0xFFFFFF);}

        // Panel
        drawPanel(ctx, cx, cy, sa);
        super.render(ctx, mx, my, delta);
    }

    // ─── Panel ────────────────────────────────────────────────────────────

    private void drawPanel(DrawContext ctx, int cx, int cy, int sa) {
        int pw=380, ph=260;
        int px=cx-pw/2, py=cy-ph/2;

        // Shadow
        ctx.fill(px+10,py+10,px+pw+10,py+ph+10,ab(0x77000000,sa));
        ctx.fill(px+5, py+5, px+pw+5, py+ph+5, ab(0x33000000,sa));

        // Panel BG
        ctx.fill(px,py,px+pw,py+ph,ab(0xF20A0A18,sa));

        // Animated hue border
        float t=ticks*0.04f;
        int bR=(int)(Math.abs(Math.sin(t))*155);
        int bG=Math.max(0,Math.min(255,(int)(180+75*Math.sin(t+2.1))));
        int bB=Math.max(0,Math.min(255,(int)(240+15*Math.sin(t+4.2))));
        int bA=(int)((0.65f+0.35f*glow)*sa);
        int bc=(bA<<24)|(bR<<16)|(bG<<8)|bB;
        int bc2=((bA/4)<<24)|(bR<<16)|(bG<<8)|bB;
        ctx.fill(px,py,px+pw,py+2,bc);ctx.fill(px,py+ph-2,px+pw,py+ph,bc);
        ctx.fill(px,py,px+2,py+ph,bc);ctx.fill(px+pw-2,py,px+pw,py+ph,bc);
        ctx.fill(px+2,py+2,px+pw-2,py+3,bc2);ctx.fill(px+2,py+ph-3,px+pw-2,py+ph-2,bc2);

        // Gold corners
        int gcA=(int)(0xEE*sa/255); int gc=(gcA<<24)|GOLD; int cs=22;
        ctx.fill(px,py,px+cs,py+3,gc);ctx.fill(px,py,px+3,py+cs,gc);
        ctx.fill(px+pw-cs,py,px+pw,py+3,gc);ctx.fill(px+pw-3,py,px+pw,py+cs,gc);
        ctx.fill(px,py+ph-3,px+cs,py+ph,gc);ctx.fill(px,py+ph-cs,px+3,py+ph,gc);
        ctx.fill(px+pw-cs,py+ph-3,px+pw,py+ph,gc);ctx.fill(px+pw-3,py+ph-cs,px+pw,py+ph,gc);

        // ── Title ──
        String t1="PROFESSOR", t2=" CLIENT";
        int tw1=textRenderer.getWidth(t1),tw2=textRenderer.getWidth(t2);
        int startX=cx-(tw1+tw2)/2, ty=py+28;
        int gA=(int)((32+24*glow)*sa/255);
        for(int dx=-5;dx<=5;dx++){int g2=Math.max(0,gA-Math.abs(dx)*6);if(g2<=0)continue;ctx.drawText(textRenderer,t1+t2,cx-(tw1+tw2)/2+dx,ty,(g2<<24)|CYAN,false);ctx.drawText(textRenderer,t1+t2,cx-(tw1+tw2)/2,ty+dx,(g2<<24)|CYAN,false);}
        int tA=(int)((210+45*glow)*sa/255);
        ctx.drawText(textRenderer,t1,startX,ty,(tA<<24)|CYAN,false);
        ctx.drawText(textRenderer,t2,startX+tw1,ty,(tA<<24)|GOLD,false);

        String sub="v5.0  |  ExploitFixer Bypass  |  Unlimited Packets";
        int sA=(int)((110+60*glow)*sa/255);
        ctx.drawText(textRenderer,sub,cx-textRenderer.getWidth(sub)/2,ty+14,(sA<<24)|PURPLE,false);

        // Gold divider
        ctx.fill(px+32,py+52,px+pw-32,py+53,(int)(0x55*sa/255)<<24|GOLD);

        // ── Progress bar ──
        int barX=px+30,barY=py+72,barW=pw-60,barH=9;
        String pct=(int)load+"%";
        ctx.drawText(textRenderer,pct,cx-textRenderer.getWidth(pct)/2,barY-14,(int)(210*sa/255)<<24|CYAN,false);

        ctx.fill(barX-1,barY-1,barX+barW+1,barY+barH+1,(int)(0x44*sa/255)<<24|CYAN);
        ctx.fill(barX,barY,barX+barW,barY+barH,0x1A000000);

        int fw=(int)(barW*load/100f);
        if(fw>0){
            for(int xi=0;xi<fw;xi++){float frac=(float)xi/barW;int r=(int)(155*(1-frac)),g=(int)(245*frac);ctx.fill(barX+xi,barY,barX+xi+1,barY+barH,0xFF000000|(r<<16)|(g<<8)|255);}
            int gcA2=(int)(130*glow*sa/255);
            ctx.fill(barX+fw-6,barY-2,barX+fw+2,barY+barH+2,(gcA2<<24)|CYAN);
        }

        // Tile segments
        int tileY=barY+barH+5,tiles=30,tw=(barW-tiles+1)/tiles;
        for(int i=0;i<tiles;i++){int txp=barX+i*(tw+1);boolean filled=i<(int)(load/100f*tiles);int tcA=(int)((filled?0xCC:0x14)*sa/255);ctx.fill(txp,tileY,txp+tw,tileY+3,(tcA<<24)|(filled?CYAN:0xFFFFFF));}

        // Message
        String msg=MSGS[msgIdx];
        ctx.drawText(textRenderer,msg,cx-textRenderer.getWidth(msg)/2,tileY+9,(int)((140+80*glow)*sa/255)<<24|0xAAFFFF,false);

        // EF bypass line
        String ef="ExploitFixer: BYPASSED";
        ctx.drawText(textRenderer,ef,cx-textRenderer.getWidth(ef)/2,tileY+20,(int)((ticks%20<12?160+70*glow:0)*sa/255)<<24|GREEN,false);

        // READY flash
        if(done&&doneTimer%20<10){String r=">>> READY <<<";ctx.drawText(textRenderer,r,cx-textRenderer.getWidth(r)/2,tileY+32,(int)(220*sa/255)<<24|GOLD,false);}

        // Bottom divider + copyright
        ctx.fill(px+32,py+ph-26,px+pw-32,py+ph-25,(int)(0x33*sa/255)<<24|CYAN);
        String copy="(c) Professor Client  --  Elite Fabric 1.21.1  |  EF Bypass Active";
        ctx.drawText(textRenderer,copy,cx-textRenderer.getWidth(copy)/2,py+ph-16,(int)(70*sa/255)<<24|PURPLE,false);
    }

    // ─── Util ─────────────────────────────────────────────────────────────

    private int ab(int color, int sa) {
        int baseA=(color>>>24)&0xFF;
        return (Math.min(255,baseA*sa/255)<<24)|(color&0x00FFFFFF);
    }
}
