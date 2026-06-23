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

    private float  progress  = 0f;
    private float  fadeIn    = 0f;
    private float  fadeOut   = 0f;
    private boolean closing  = false;
    private int    frame     = 0;
    private float  glow      = 0f;
    private boolean glowUp   = true;
    private boolean soundPlayed = false;

    // Matrix rain
    private static final String CHARS = "XERION01CLIENT10BYPASS11CRASH";
    private static final int COLS = 60;
    private float[] rX, rY, rSp; private int[] rCh;

    // Rings
    private final float[] ringR   = {8,55,105,158,210,265,320};
    private final float[] ringSp  = {2.0f,1.6f,1.2f,1.0f,0.7f,0.5f,0.35f};
    private final int[]   ringCol = {0x00C8FF,0x0055FF,0x00AAFF,0x0033CC,0x00DDFF,0x0077FF,0x00C8FF};

    // Particles
    private static final int PC = 140;
    private float[] px,py,pvx,pvy,pal,pph; private int[] pct;

    // Hex grid
    private float hexPulse = 0f;

    private static final String[] MSGS = {
        "Initializing Xerion subsystems...",
        "Loading exploit engine  [✓]",
        "Bypass calibration  [✓]",
        "Packet hooks established  [✓]",
        "Crash modules online  [✓]",
        "Welcome, operator."
    };

    private final Random rng = new Random();

    public KeyActivationScreen() { super(Text.literal("Xerion Client")); }

    @Override protected void init() {
        rX=new float[COLS];rY=new float[COLS];rSp=new float[COLS];rCh=new int[COLS];
        for(int i=0;i<COLS;i++){rX[i]=(i+.5f)*(width/(float)COLS);rY[i]=rng.nextFloat()*-height;rSp[i]=rng.nextFloat()*2.2f+.6f;rCh[i]=rng.nextInt(CHARS.length());}
        px=new float[PC];py=new float[PC];pvx=new float[PC];pvy=new float[PC];pal=new float[PC];pph=new float[PC];pct=new int[PC];
        for(int i=0;i<PC;i++) rp(i,true);
    }

    private void rp(int i,boolean rand){px[i]=rng.nextFloat()*width;py[i]=rand?rng.nextFloat()*height:height+5;pvx[i]=(rng.nextFloat()-.5f)*.45f;pvy[i]=-(rng.nextFloat()*.9f+.15f);pal[i]=rng.nextFloat()*.9f+.1f;pph[i]=rng.nextFloat()*6.28f;pct[i]=rng.nextInt(3);}

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        frame++;

        // Play hello_friend exactly once when screen first appears
        if (!soundPlayed && frame > 2) {
            ProfessorClientMod.playSound(MinecraftClient.getInstance(), "hello_friend");
            soundPlayed = true;
        }

        if (!closing) {
            fadeIn   = Math.min(1f, fadeIn + .065f);
            progress = Math.min(1f, progress + .011f);
            if (progress >= 1f) closing = true;
        } else {
            fadeOut = Math.min(1f, fadeOut + .08f);
            if (fadeOut >= 1f) { MinecraftClient.getInstance().setScreen(new ProfessorScreen()); return; }
        }

        float alpha = fadeIn * (1f - fadeOut);
        int sa = (int)(alpha * 255); if (sa <= 0) return;
        int W=width,H=height,cx=W/2,cy=H/2;

        glow += glowUp ? .035f : -.035f;
        if (glow>=1f){glow=1f;glowUp=false;} else if (glow<=0f){glow=0f;glowUp=true;}
        hexPulse += .02f;

        // Update
        for(int i=0;i<COLS;i++){rY[i]+=rSp[i];if(rY[i]>H+20){rY[i]=-20;rCh[i]=rng.nextInt(CHARS.length());}if(rng.nextInt(24)==0)rCh[i]=rng.nextInt(CHARS.length());}
        for(int i=0;i<ringR.length;i++){ringR[i]+=ringSp[i];if(ringR[i]>Math.max(W,H)*.9f)ringR[i]=8f;}
        for(int i=0;i<PC;i++){px[i]+=pvx[i];py[i]+=pvy[i];pph[i]+=.042f;if(py[i]<-10)rp(i,false);if(px[i]<0)px[i]=W;if(px[i]>W)px[i]=0;}

        // Background
        ctx.fill(0,0,W,H,0xFF000508);

        // Hex-grid lines (diagonal)
        for(int x2=0;x2<W+H;x2+=42){int ha=(int)(8+4*Math.sin(hexPulse+x2*.1f));ctx.fill(x2,0,x2+1,H,(sc(ha,sa)<<24)|0x0044AA);}
        for(int y2=-H;y2<W;y2+=42){int ha=(int)(8+4*Math.sin(hexPulse+y2*.1f));ctx.fill(0,y2,W,y2+1,(sc(ha,sa)<<24)|0x0044AA);}

        // Rings
        for(int i=0;i<ringR.length;i++){int r=(int)ringR[i];if(r<2)continue;float ff=1f-ringR[i]/(Math.max(W,H)*.9f);int ra=(int)(ff*ff*55*sa/255);if(ra<=0)continue;ctx.fill(cx-r,cy-1,cx+r,cy+1,(ra<<24)|ringCol[i]);ctx.fill(cx-1,cy-r,cx+1,cy+r,(ra<<24)|ringCol[i]);}

        // Matrix rain (blue)
        for(int i=0;i<COLS;i++){if(rY[i]<0)continue;int ra=sc(60+(int)(40*glow),sa);ctx.drawText(textRenderer,String.valueOf(CHARS.charAt(rCh[i])),(int)rX[i]-4,(int)rY[i],(ra<<24)|0x0099FF,false);if(rY[i]>14)ctx.drawText(textRenderer,String.valueOf(CHARS.charAt(rCh[i])),(int)rX[i]-4,(int)rY[i]-14,(ra/5<<24)|0x002244,false);}

        // Particles
        for(int i=0;i<PC;i++){float tw=(MathHelper.sin(pph[i])+1f)/2f;int a=(int)(pal[i]*tw*sa);if(a<10)continue;int col=switch(pct[i]){case 1->(a<<24)|0x0066FF;case 2->(a<<24)|0x00CCFF;default->(a<<24)|0x0044AA;};ctx.fill((int)px[i],(int)py[i],(int)px[i]+2,(int)py[i]+2,col);}

        // ── Panel ────────────────────────────────────────────────────────────
        int pw=400,ph=240,ppx=cx-pw/2,ppy=cy-ph/2;

        // Shadow layers
        ctx.fill(ppx+10,ppy+10,ppx+pw+10,ppy+ph+10,blA(0x88000000,sa));
        ctx.fill(ppx+5, ppy+5, ppx+pw+5, ppy+ph+5, blA(0x44000000,sa));

        // Panel body with vertical gradient
        ctx.fill(ppx,ppy,ppx+pw,ppy+ph,blA(0xF2000A1E,sa));
        for(int y=0;y<ph;y+=3){int ga=(int)(8*(1f-(float)y/ph));ctx.fill(ppx,ppy+y,ppx+pw,ppy+y+3,(sc(ga,sa)<<24)|0x0055FF);}

        // Animated border (hue-cycled blue)
        float hue=frame*.045f;
        int bG=Math.max(80,Math.min(240,(int)(160+80*Math.sin(hue))));
        int bBA=(int)((0.7f+.3f*glow)*255);
        int bC=(sc(bBA,sa)<<24)|((bG<<8)|0xFF);
        ctx.fill(ppx,ppy,ppx+pw,ppy+2,bC);ctx.fill(ppx,ppy+ph-2,ppx+pw,ppy+ph,bC);
        ctx.fill(ppx,ppy,ppx+2,ppy+ph,bC);ctx.fill(ppx+pw-2,ppy,ppx+pw,ppy+ph,bC);

        // Inner highlight
        int ih=sc((int)(0.15f*sa),sa);
        ctx.fill(ppx+2,ppy+2,ppx+pw-2,ppy+3,(ih<<24)|0x88DDFF);
        ctx.fill(ppx+2,ppy+ph-3,ppx+pw-2,ppy+ph-2,(ih<<24)|0x88DDFF);

        // Gold corner brackets
        int gcc=(sc(0xCC,sa)<<24)|0xFFD700; int cs=22;
        ctx.fill(ppx,ppy,ppx+cs,ppy+3,gcc);ctx.fill(ppx,ppy,ppx+3,ppy+cs,gcc);
        ctx.fill(ppx+pw-cs,ppy,ppx+pw,ppy+3,gcc);ctx.fill(ppx+pw-3,ppy,ppx+pw,ppy+cs,gcc);
        ctx.fill(ppx,ppy+ph-3,ppx+cs,ppy+ph,gcc);ctx.fill(ppx,ppy+ph-cs,ppx+3,ppy+ph,gcc);
        ctx.fill(ppx+pw-cs,ppy+ph-3,ppx+pw,ppy+ph,gcc);ctx.fill(ppx+pw-3,ppy+ph-cs,ppx+pw,ppy+ph,gcc);

        // ── XERION CLIENT title ───────────────────────────────────────────────
        String t1="XERION",t2=" CLIENT";
        int tw1=textRenderer.getWidth(t1),tw2=textRenderer.getWidth(t2);
        int tx=cx-(tw1+tw2)/2,ty=ppy+26;
        int tA=sc((int)(175+80*glow),sa);
        // Glow
        for(int d=-4;d<=4;d++){int ga=Math.max(0,sc(20-Math.abs(d)*5,sa));if(ga>0){ctx.drawText(textRenderer,t1+t2,tx+d,ty,(ga<<24)|0x00AAFF,false);ctx.drawText(textRenderer,t1+t2,tx,ty+d,(ga<<24)|0x00AAFF,false);}}
        ctx.drawText(textRenderer,t1,tx,ty,(tA<<24)|0x00DDFF,false);
        ctx.drawText(textRenderer,t2,tx+tw1,ty,(tA<<24)|0xFFD700,false);

        // Subtitle
        String sub="v1.0  ⬡  Advanced Client  ⬡  © Xerion Client";
        ctx.drawText(textRenderer,sub,cx-textRenderer.getWidth(sub)/2,ty+13,sc((int)(100+60*glow),sa)<<24|0x004488,false);

        // Divider
        ctx.fill(ppx+30,ppy+48,ppx+pw-30,ppy+49,sc(0x44,sa)<<24|0xFFD700);

        // Status message
        int mi=Math.min(MSGS.length-1,(int)(progress*MSGS.length));
        String msg=MSGS[mi];
        int ma=sc((int)(155+65*glow),sa);
        ctx.drawText(textRenderer,msg,cx-textRenderer.getWidth(msg)/2,ppy+56,(ma<<24)|0x00AAFF,false);

        // Progress bar
        int bx=ppx+30,by=ppy+74,bw=pw-60,bh=12;
        String ps=(int)(progress*100)+"%";
        ctx.drawText(textRenderer,ps,cx-textRenderer.getWidth(ps)/2,by-13,sc(200,sa)<<24|0x00BBFF,false);

        // Bar bg
        ctx.fill(bx-1,by-1,bx+bw+1,by+bh+1,sc(0x55,sa)<<24|0x0055AA);
        ctx.fill(bx,by,bx+bw,by+bh,sc(0x12,sa)<<24|0x000000);

        // Bar fill (gradient blue → cyan)
        int fw=(int)(bw*progress);
        for(int xi=0;xi<fw;xi++){float fr=(float)xi/bw;int bg2=(int)MathHelper.lerp(fr,50f,220f);ctx.fill(bx+xi,by,bx+xi+1,by+bh,0xFF000000|(0<<16)|(bg2<<8)|255);}

        // Bar cap glow
        if(fw>0){int cap=sc((int)(180+75*glow),sa);ctx.fill(bx+fw-3,by-1,bx+fw+3,by+bh+1,(cap<<24)|0xAAEEFF);}

        // Segment dividers
        for(int s=1;s<20;s++){int sx=bx+bw*s/20;ctx.fill(sx,by,sx+1,by+bh,sc(0x22,sa)<<24|0x000000);}

        // Scanline inside bar
        float sl2=(frame*3f)%bw;
        if(sl2<fw)ctx.fill(bx+(int)sl2,by,bx+(int)sl2+4,by+bh,(sc(0x66,sa)<<24)|0xFFFFFF);

        // Footer
        String copy="  © Xerion Client  —  by CrashPass  —  Press ESC to cancel  ";
        ctx.drawText(textRenderer,copy,cx-textRenderer.getWidth(copy)/2,ppy+ph-16,sc(55,sa)<<24|0x223355,false);

        super.render(ctx,mx,my,delta);
    }

    private int sc(int a,int sa){return Math.max(0,Math.min(255,a*sa/255));}
    private int blA(int col,int sa){int a=((col>>24)&0xFF)*sa/255;return(a<<24)|(col&0x00FFFFFF);}
}
