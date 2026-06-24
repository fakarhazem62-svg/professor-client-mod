package com.professor.client.gui;

import com.professor.client.ProfessorClientMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Complete ice-themed title screen replacement for Xerion Client */
public class XerionTitleScreen extends Screen {

    // ── Palette ───────────────────────────────────────────────────────────
    private static final int BG      = 0xFF010A14;
    private static final int PANEL   = 0xFF041828;
    private static final int BORDER  = 0xFFAAEEFF;
    private static final int BORDER2 = 0xFF55CCFF;
    private static final int GOLD    = 0xFFFFD700;
    private static final int WHITE   = 0xFFEEF8FF;
    private static final int ICE     = 0xFF88DDFF;
    private static final int DIM     = 0xFF336688;
    private static final int RED     = 0xFFFF2244;
    private static final int GREEN   = 0xFF00FF99;
    private static final int CRYSTAL = 0xFF00CCEE;

    // ── Snow ──────────────────────────────────────────────────────────────
    private static final int SCNT = 260;
    private final float[] sx=new float[SCNT], sy=new float[SCNT];
    private final float[] ss=new float[SCNT], sz=new float[SCNT];
    private final float[] sa=new float[SCNT], sp=new float[SCNT];

    // ── Particles ─────────────────────────────────────────────────────────
    private static final int PCNT = 80;
    private final float[] px=new float[PCNT], py=new float[PCNT];
    private final float[] pvx=new float[PCNT], pvy=new float[PCNT];
    private final float[] pa=new float[PCNT], pp=new float[PCNT];

    // ── Crystal shards ────────────────────────────────────────────────────
    private record Shard(float x, float y, float size, float angle, float alpha, int color) {}
    private final List<Shard> shards = new ArrayList<>();

    // ── Menu buttons metadata ─────────────────────────────────────────────
    private record MenuBtn(String icon, String label, String sub, int color) {}
    private static final MenuBtn[] MENU = {
        new MenuBtn("🌍", "SINGLEPLAYER",  "Play alone",       0x55DDFF),
        new MenuBtn("🌐", "MULTIPLAYER",   "Join a server",    0x00FFCC),
        new MenuBtn("🔑", "XERION CLIENT", "Open client GUI",  0xFFD700),
        new MenuBtn("⚙",  "OPTIONS",       "Game settings",    0x88AAFF),
        new MenuBtn("✕",  "QUIT",          "Exit game",        0xFF3355),
    };

    // ── Animation ─────────────────────────────────────────────────────────
    private long  tick  = 0;
    private float glow  = 0f; private boolean glowUp = true;
    private float hue   = 0f;
    private float drift = 0f;
    private float hoverScale = 1f;
    private int   hoveredBtn = -1;
    private float enterAnim  = 0f; // 0→1 on open

    private final Random rng = new Random();

    public XerionTitleScreen() {
        super(Text.literal("Xerion Client"));
    }

    @Override
    protected void init() {
        int W = width, H = height;
        // Init snow
        for (int i=0;i<SCNT;i++) {
            sx[i]=rng.nextFloat()*W; sy[i]=rng.nextFloat()*H;
            ss[i]=rng.nextFloat()*.9f+.2f; sz[i]=rng.nextFloat()*4f+1f;
            sa[i]=rng.nextFloat()*.8f+.2f; sp[i]=rng.nextFloat()*6.28f;
        }
        // Init particles
        for (int i=0;i<PCNT;i++) {
            px[i]=rng.nextFloat()*W; py[i]=rng.nextFloat()*H;
            pvx[i]=(rng.nextFloat()-.5f)*.4f; pvy[i]=-(rng.nextFloat()*.5f+.1f);
            pa[i]=rng.nextFloat()*.7f+.3f; pp[i]=rng.nextFloat()*6.28f;
        }
        // Init shards
        shards.clear();
        for (int i=0;i<18;i++) {
            shards.add(new Shard(
                rng.nextFloat()*W, rng.nextFloat()*H,
                rng.nextFloat()*22+5, rng.nextFloat()*3.14f,
                rng.nextFloat()*.25f+.05f,
                i%3==0?BORDER:i%3==1?BORDER2:CRYSTAL
            ));
        }

        // Buttons — positioned in a centered column
        int cx = W/2, by = H/2 - 40;
        int bw = 280, bh = 28, gap = 6;
        for (int i=0;i<MENU.length;i++) {
            final int fi = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(MENU[i].icon()+" "+MENU[i].label()),
                b -> onMenuClick(fi)
            ).dimensions(cx-bw/2, by+i*(bh+gap), bw, bh).build());
        }
    }

    private void onMenuClick(int i) {
        if (client == null) return;
        ProfessorClientMod.playClickSound(client);
        switch (i) {
            case 0 -> client.setScreen(new SelectWorldScreen(this));
            case 1 -> client.setScreen(new MultiplayerScreen(this));
            case 2 -> { ProfessorMusicManager.onOpen(client); client.setScreen(new KeyActivationScreen()); }
            case 3 -> client.setScreen(new OptionsScreen(this, client.options));
            case 4 -> client.close();
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        tick++; enterAnim = Math.min(1f, enterAnim + .04f);
        glow += glowUp ? .025f : -.025f;
        if (glow>=1f){glow=1f;glowUp=false;} else if(glow<=0f){glow=0f;glowUp=true;}
        hue  = (hue+.002f)%1f; drift += .012f;

        int W=width, H=height, cx=W/2;

        // ── Deep ice background ────────────────────────────────────────────
        ctx.fill(0,0,W,H,BG);
        // Horizontal scan lines
        for (int y=0;y<H;y+=3) ctx.fill(0,y,W,y+1,(5<<24)|0x112244);
        // Vertical ice columns
        for (int x=0;x<W;x+=60) {
            int a=(int)(8+6*Math.sin(drift+x*.04));
            ctx.fill(x,0,x+1,H,(a<<24)|0x2255AA);
        }

        // ── Crystal shards (background decoration) ────────────────────────
        for (Shard s : shards) {
            int a=(int)(s.alpha()*180+s.alpha()*30*glow);
            int sz2=(int)s.size();
            ctx.fill((int)s.x()-1,(int)s.y()-sz2,(int)s.x()+1,(int)s.y()+sz2,wa(s.color(),a));
            ctx.fill((int)s.x()-sz2,(int)s.y()-1,(int)s.x()+sz2,(int)s.y()+1,wa(s.color(),a/2));
        }

        // ── Snow ──────────────────────────────────────────────────────────
        for (int i=0;i<SCNT;i++) {
            sy[i]+=ss[i]; sx[i]+=(float)Math.sin(drift+sp[i])*.5f; sp[i]+=.006f;
            if(sy[i]>H+6){sy[i]=-6;sx[i]=rng.nextFloat()*W;}
            if(sx[i]<-4)sx[i]=W+3; if(sx[i]>W+3)sx[i]=-4;
            float tw=(MathHelper.sin(sp[i])+1f)/2f;
            int a=(int)(sa[i]*(0.5f+0.5f*tw)*255); if(a<15) continue;
            int szI=(int)sz[i]; int col=sz[i]>3.5f?0xFFFFFFFF:sz[i]>2.5f?0xFFCCEEFF:0xFF99BBDD;
            ctx.fill((int)sx[i],(int)sy[i],(int)sx[i]+szI,(int)sy[i]+szI,wa(col,a));
            if(szI>=3&&a>120)ctx.fill((int)sx[i]+1,(int)sy[i]+1,(int)sx[i]+szI-1,(int)sy[i]+szI-1,wa(0xFFFFFFFF,Math.min(255,a+60)));
        }

        // ── Floating particles ────────────────────────────────────────────
        for (int i=0;i<PCNT;i++) {
            px[i]+=pvx[i]; py[i]+=pvy[i]; pp[i]+=.04f;
            if(py[i]<-6){py[i]=H+4;px[i]=rng.nextFloat()*W;}
            if(px[i]<0)px[i]=W; if(px[i]>W)px[i]=0;
            float tw=(MathHelper.sin(pp[i])+1f)/2f;
            int a=(int)(pa[i]*tw*180); if(a<12) continue;
            int col=i%3==0?(a<<24)|0x00CCEE:i%3==1?(a<<24)|GOLD:(a<<24)|0x88EEFF;
            ctx.fill((int)px[i],(int)py[i],(int)px[i]+2,(int)py[i]+2,col);
        }

        // ── Title panel ───────────────────────────────────────────────────
        int titleH = 90, titleY = H/2 - 160;
        int titleX = cx-220;
        // Glow behind title
        for(int g=20;g>0;g--) {
            int ga=(int)(g*4*glow); if(ga<4) continue;
            ctx.fill(titleX-g,titleY-g,titleX+440+g,titleY+titleH+g,wa(0xFF003355,ga));
        }
        ctx.fill(titleX,titleY,titleX+440,titleY+titleH,wa(PANEL,240));
        // Border
        float hrad=hue*6.28f;
        int bR=Math.max(0,Math.min(255,(int)(70+55*Math.abs(Math.sin(hrad)))));
        int bG=Math.max(170,Math.min(255,(int)(200+55*Math.sin(hrad+1.6f))));
        int borderC=(255<<24)|(bR<<16)|(bG<<8)|0xFF;
        ctx.fill(titleX,titleY,titleX+440,titleY+5,borderC);
        ctx.fill(titleX,titleY+titleH-5,titleX+440,titleY+titleH,borderC);
        ctx.fill(titleX,titleY,titleX+5,titleY+titleH,borderC);
        ctx.fill(titleX+435,titleY,titleX+440,titleY+titleH,borderC);
        // Corner accents
        drawCorner(ctx,titleX,titleY,titleX+440,titleY+titleH,28,wa(GOLD,230));

        // ── XERION CLIENT title text ──────────────────────────────────────
        int ty = titleY+12;
        String big1 = "XERION", big2 = " CLIENT";
        int w1=textRenderer.getWidth(big1)*3, w2=textRenderer.getWidth(big2)*3;
        int tx = cx-(w1+w2)/2;
        // Glow layers
        int glA=(int)(60+45*glow);
        for(int d=-8;d<=8;d+=2){
            int ga=Math.max(0,glA-Math.abs(d)*6);if(ga<5)continue;
            ctx.drawTextWithShadow(textRenderer,big1+big2,tx+d,ty,(ga<<24)|0x00CCFF);
            ctx.drawTextWithShadow(textRenderer,big1+big2,tx,ty+d,(ga<<24)|0x00CCFF);
        }
        // Draw manually scaled (3×) — using the text renderer draws normal, so draw it normally centered
        // Since MC text renderer doesn't support scaling directly, draw at normal size but bigger than the sub
        ctx.drawTextWithShadow(textRenderer,"X E R I O N  C L I E N T",cx-textRenderer.getWidth("X E R I O N  C L I E N T")/2,ty+4,wa(WHITE,(int)(230+25*glow)));
        ctx.drawTextWithShadow(textRenderer,"─── ❄ ── ❄ ── ❄ ───",cx-textRenderer.getWidth("─── ❄ ── ❄ ── ❄ ───")/2,ty+16,wa(BORDER,(int)(180+45*glow)));
        ctx.drawTextWithShadow(textRenderer,"CLIENT  v3.0",cx-textRenderer.getWidth("CLIENT  v3.0")/2,ty+28,wa(GOLD,(int)(200+55*glow)));
        ctx.drawTextWithShadow(textRenderer,"ExploitFixer bypass  •  Proxy rotation  •  11 bypass modes",cx-textRenderer.getWidth("ExploitFixer bypass  •  Proxy rotation  •  11 bypass modes")/2,ty+42,wa(CRYSTAL,(int)(140+50*glow)));
        ctx.drawTextWithShadow(textRenderer,"Fabric 1.21.1  |  Press M to open client GUI",cx-textRenderer.getWidth("Fabric 1.21.1  |  Press M to open client GUI")/2,ty+56,wa(DIM,180));

        // ── Menu buttons custom background ────────────────────────────────
        int by2 = H/2-40;
        int bw = 280, bh = 28, gap = 6;
        hoveredBtn = -1;
        for (int i=0;i<MENU.length;i++) {
            int bx2=cx-bw/2, by3=by2+i*(bh+gap);
            boolean over=(mx>=bx2&&mx<=bx2+bw&&my>=by3&&my<=by3+bh);
            if(over) hoveredBtn=i;
            int bc=MENU[i].color();
            float pulse=over?(float)(0.25+0.12*Math.sin(tick*.18)):0f;
            int bgA=(int)((0.55f+pulse)*255);
            // Panel
            ctx.fill(bx2-3,by3-3,bx2+bw+3,by3+bh+3,wa(0xFF001830,120));
            ctx.fill(bx2,by3,bx2+bw,by3+bh,wa(0xFF011828,bgA));
            // Left accent bar
            ctx.fill(bx2,by3,bx2+4,by3+bh,wa(bc,(int)(180+75*pulse)));
            // Right accent bar
            ctx.fill(bx2+bw-4,by3,bx2+bw,by3+bh,wa(bc,(int)(90+60*pulse)));
            // Top border
            ctx.fill(bx2,by3,bx2+bw,by3+2,wa(bc,(int)(120+80*pulse)));
            // Hover glow
            if(over){for(int g2=6;g2>0;g2--){ctx.fill(bx2-g2,by3-g2,bx2+bw+g2,by3+bh+g2,wa(bc,g2*5));}}
            // Sub text (right side, dimmed)
            ctx.drawTextWithShadow(textRenderer,MENU[i].sub(),bx2+bw-6-textRenderer.getWidth(MENU[i].sub()),by3+bh/2-4,wa(bc,(int)(100+80*pulse)));
        }

        // ── Bottom status bar ─────────────────────────────────────────────
        ctx.fill(0,H-18,W,H,wa(PANEL,220));
        ctx.fill(0,H-18,W,H-17,wa(BORDER2,180));
        String status="❄  Xerion Client v3.0  |  Minecraft 1.21.1 Fabric  |  "+
            (client!=null&&client.getNetworkHandler()!=null?"🟢 CONNECTED":"⚪ OFFLINE")+"  ❄";
        ctx.drawTextWithShadow(textRenderer,status,cx-textRenderer.getWidth(status)/2,H-13,wa(DIM,200));

        // Enter animation overlay (fade in from black)
        if(enterAnim<1f){
            int oa=(int)((1f-enterAnim)*(1f-enterAnim)*255);
            if(oa>0) ctx.fill(0,0,W,H,wa(0xFF000000,oa));
        }

        super.render(ctx,mx,my,delta);

        // Play music if applicable
        ProfessorMusicManager.playOnTitleScreen(client);
    }

    @Override public boolean shouldPause() { return false; }

    private static int wa(int rgb,int a){return(Math.max(0,Math.min(255,a))<<24)|(rgb&0x00FFFFFF);}
    private static void drawCorner(DrawContext ctx,int x1,int y1,int x2,int y2,int cs,int col){
        ctx.fill(x1,y1,x1+cs,y1+4,col);ctx.fill(x1,y1,x1+4,y1+cs,col);
        ctx.fill(x2-cs,y1,x2,y1+4,col);ctx.fill(x2-4,y1,x2,y1+cs,col);
        ctx.fill(x1,y2-4,x1+cs,y2,col);ctx.fill(x1,y2-cs,x1+4,y2,col);
        ctx.fill(x2-cs,y2-4,x2,y2,col);ctx.fill(x2-4,y2-cs,x2,y2,col);
    }
}
