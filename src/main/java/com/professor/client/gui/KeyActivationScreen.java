package com.professor.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import java.util.Random;

/** Blue loading screen shown every time M is pressed → transitions to ProfessorScreen. */
@Environment(EnvType.CLIENT)
public class KeyActivationScreen extends Screen {

    private float  progress  = 0f;
    private float  fadeIn    = 0f;
    private float  fadeOut   = 0f;
    private boolean closing  = false;
    private int    frame     = 0;
    private float  glow      = 0f;
    private boolean glowUp   = true;
    private float  scan      = 0f;

    private static final String CHARS = "XERION01CLIENT10BYPASS110100";
    private static final int    COLS  = 55;
    private float[] rX, rY, rSp; private int[] rCh;

    private float[] ringR   = {10,60,110,160,210,260};
    private float[] ringSp  = {1.6f,1.3f,1.0f,0.8f,0.55f,0.35f};
    private int[]   ringCol = {0x00AAFF,0x0055FF,0x00DDFF,0x0033CC,0x00AAFF,0x0066FF};

    private static final int PC = 100;
    private float[] px,py,pvx,pvy,pal,pph; private int[] pct;

    private static final String[] MSGS = {
        "Initializing Xerion Client...",
        "Loading exploit modules...",
        "Calibrating bypass engine...",
        "Establishing packet hooks...",
        "All systems online.  Welcome."
    };

    private final Random rng = new Random();

    public KeyActivationScreen() { super(Text.literal("Xerion Client")); }

    @Override protected void init() { initAll(); }
    @Override public boolean shouldPause()      { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }

    private void initAll() {
        rX=new float[COLS];rY=new float[COLS];rSp=new float[COLS];rCh=new int[COLS];
        for(int i=0;i<COLS;i++){rX[i]=(i+.5f)*(width/(float)COLS);rY[i]=rng.nextFloat()*-height;rSp[i]=rng.nextFloat()*2+.5f;rCh[i]=rng.nextInt(CHARS.length());}
        px=new float[PC];py=new float[PC];pvx=new float[PC];pvy=new float[PC];pal=new float[PC];pph=new float[PC];pct=new int[PC];
        for(int i=0;i<PC;i++) rp(i,true);
    }
    private void rp(int i,boolean rand){px[i]=rng.nextFloat()*width;py[i]=rand?rng.nextFloat()*height:height+5;pvx[i]=(rng.nextFloat()-.5f)*.5f;pvy[i]=-(rng.nextFloat()*.8f+.15f);pal[i]=rng.nextFloat()*.9f+.1f;pph[i]=rng.nextFloat()*6.28f;pct[i]=rng.nextInt(3);}

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        frame++;
        if (!closing){ fadeIn=Math.min(1f,fadeIn+.07f); progress=Math.min(1f,progress+.012f); if(progress>=1f)closing=true; }
        else { fadeOut=Math.min(1f,fadeOut+.09f); if(fadeOut>=1f){MinecraftClient.getInstance().setScreen(new ProfessorScreen());return;} }

        float alpha=fadeIn*(1f-fadeOut); int sa=(int)(alpha*255); if(sa<=0)return;
        int W=width,H=height,cx=W/2,cy=H/2;

        glow+=glowUp?.035f:-.035f; if(glow>=1f){glow=1f;glowUp=false;}else if(glow<=0f){glow=0f;glowUp=true;}
        scan=(scan+2f)%H;

        // update rain
        for(int i=0;i<COLS;i++){rY[i]+=rSp[i];if(rY[i]>H+20){rY[i]=-20;rCh[i]=rng.nextInt(CHARS.length());}if(rng.nextInt(28)==0)rCh[i]=rng.nextInt(CHARS.length());}
        for(int i=0;i<ringR.length;i++){ringR[i]+=ringSp[i];if(ringR[i]>Math.max(W,H)*.9f)ringR[i]=10f;}
        for(int i=0;i<PC;i++){px[i]+=pvx[i];py[i]+=pvy[i];pph[i]+=.04f;if(py[i]<-10)rp(i,false);if(px[i]<0)px[i]=W;if(px[i]>W)px[i]=0;}

        // BG
        ctx.fill(0,0,W,H,blA(0xFF01020A,sa));

        // Grid (dark blue)
        int ga=sc(5,sa);
        if(ga>0){for(int x=0;x<W;x+=55)ctx.fill(x,0,x+1,H,(ga<<24)|0x0044AA);for(int y=0;y<H;y+=55)ctx.fill(0,y,W,y+1,(ga<<24)|0x0044AA);}

        // Rings
        for(int i=0;i<ringR.length;i++){int r=(int)ringR[i];if(r<2)continue;float ff=1f-ringR[i]/(Math.max(W,H)*.9f);int ra=(int)(ff*ff*50*sa/255);if(ra<=0)continue;ctx.fill(cx-r,cy-1,cx+r,cy+1,(ra<<24)|ringCol[i]);ctx.fill(cx-1,cy-r,cx+1,cy+r,(ra<<24)|ringCol[i]);}

        // Rain (blue tint)
        for(int i=0;i<COLS;i++){if(rY[i]<0)continue;int ra=sc(55+(int)(35*glow),sa);ctx.drawText(textRenderer,String.valueOf(CHARS.charAt(rCh[i])),(int)rX[i]-4,(int)rY[i],(ra<<24)|0x0099FF,false);if(rY[i]>14)ctx.drawText(textRenderer,String.valueOf(CHARS.charAt(rCh[i])),(int)rX[i]-4,(int)rY[i]-14,(ra/4<<24)|0x002244,false);}

        // Particles
        for(int i=0;i<PC;i++){float tw=(MathHelper.sin(pph[i])+1f)/2f;int a=(int)(pal[i]*tw*sa);if(a<10)continue;int col=switch(pct[i]){case 1->(a<<24)|0x0066FF;case 2->(a<<24)|0x00AAFF;default->(a<<24)|0x00CCFF;};ctx.fill((int)px[i],(int)py[i],(int)px[i]+2,(int)py[i]+2,col);}

        // Scanline
        int sla=sc(0x0D,sa);if(sla>0)ctx.fill(0,(int)scan,W,(int)scan+2,(sla<<24)|0xFFFFFF);

        // ── Panel ──────────────────────────────────────────────────────────
        int pw=360,ph=210,ppx=cx-pw/2,ppy=cy-ph/2;
        ctx.fill(ppx+8,ppy+8,ppx+pw+8,ppy+ph+8,blA(0x77000000,sa));
        ctx.fill(ppx,ppy,ppx+pw,ppy+ph,blA(0xEE010318,sa));

        // Animated blue border
        float t=frame*.05f;
        int bR=0,bG=Math.max(100,Math.min(200,(int)(150+80*Math.sin(t)))),bB=255;
        int bA=sc((int)((0.75f+.25f*glow)*255),sa);
        int bc=(bA<<24)|(bR<<16)|(bG<<8)|bB;
        ctx.fill(ppx,ppy,ppx+pw,ppy+2,bc);ctx.fill(ppx,ppy+ph-2,ppx+pw,ppy+ph,bc);
        ctx.fill(ppx,ppy,ppx+2,ppy+ph,bc);ctx.fill(ppx+pw-2,ppy,ppx+pw,ppy+ph,bc);

        // Inner glow border
        int ib=sc((int)(0.25f*glow*255),sa);
        ctx.fill(ppx+2,ppy+2,ppx+pw-2,ppy+3,(ib<<24)|0x0099FF);
        ctx.fill(ppx+2,ppy+ph-3,ppx+pw-2,ppy+ph-2,(ib<<24)|0x0099FF);

        // Gold corner brackets
        int gca=sc(0xCC,sa);int gcc=(gca<<24)|0xFFD700;int cs=16;
        ctx.fill(ppx,ppy,ppx+cs,ppy+2,gcc);ctx.fill(ppx,ppy,ppx+2,ppy+cs,gcc);
        ctx.fill(ppx+pw-cs,ppy,ppx+pw,ppy+2,gcc);ctx.fill(ppx+pw-2,ppy,ppx+pw,ppy+cs,gcc);
        ctx.fill(ppx,ppy+ph-2,ppx+cs,ppy+ph,gcc);ctx.fill(ppx,ppy+ph-cs,ppx+2,ppy+ph,gcc);
        ctx.fill(ppx+pw-cs,ppy+ph-2,ppx+pw,ppy+ph,gcc);ctx.fill(ppx+pw-2,ppy+ph-cs,ppx+pw,ppy+ph,gcc);

        // Title: XERION CLIENT
        int ta=sc((int)(170+85*glow),sa);
        String t1="XERION",t2=" CLIENT";
        int tw1=textRenderer.getWidth(t1),tw2=textRenderer.getWidth(t2);
        int tx=cx-(tw1+tw2)/2,ty=ppy+28;
        for(int d=-3;d<=3;d++){int ga2=Math.max(0,sc(18-Math.abs(d)*5,sa));if(ga2>0)ctx.drawText(textRenderer,t1+t2,tx+d,ty,(ga2<<24)|0x00AAFF,false);}
        ctx.drawText(textRenderer,t1,tx,ty,(ta<<24)|0x00DDFF,false);
        ctx.drawText(textRenderer,t2,tx+tw1,ty,(ta<<24)|0xFFD700,false);

        // Subtitle
        String sub="v1.0  ●  Loading...  ●  © Xerion Client";
        ctx.drawText(textRenderer,sub,cx-textRenderer.getWidth(sub)/2,ty+12,sc((int)(90+70*glow),sa)<<24|0x0077CC,false);

        // Divider
        ctx.fill(ppx+28,ppy+50,ppx+pw-28,ppy+51,sc(0x55,sa)<<24|0xFFD700);

        // Status
        int mi=Math.min(MSGS.length-1,(int)(progress*MSGS.length));
        ctx.drawText(textRenderer,MSGS[mi],cx-textRenderer.getWidth(MSGS[mi])/2,ppy+62,sc((int)(150+70*glow),sa)<<24|0x00BBFF,false);

        // Progress bar
        int bx=ppx+28,by=ppy+80,bw=pw-56,bh=10;
        String ps=(int)(progress*100)+"%";
        ctx.drawText(textRenderer,ps,cx-textRenderer.getWidth(ps)/2,by-13,sc(200,sa)<<24|0x00AAFF,false);
        ctx.fill(bx-1,by-1,bx+bw+1,by+bh+1,sc(0x55,sa)<<24|0x0055AA);
        ctx.fill(bx,by,bx+bw,by+bh,sc(0x15,sa)<<24|0x000000);
        int fw=(int)(bw*progress);
        for(int xi=0;xi<fw;xi++){float fr=(float)xi/bw;int bg2=(int)MathHelper.lerp(fr,0f,180f);ctx.fill(bx+xi,by,bx+xi+1,by+bh,0xFF000000|(0<<16)|(bg2<<8)|255);}
        if(fw>0){int cap=sc((int)(160+80*glow),sa);ctx.fill(bx+fw-2,by,bx+fw+2,by+bh,(cap<<24)|0xAAEEFF);}
        for(int s=1;s<20;s++){int sx=bx+bw*s/20;ctx.fill(sx,by,sx+1,by+bh,sc(0x25,sa)<<24|0x000000);}

        // Copyright
        String copy="© Xerion Client  —  All rights reserved";
        ctx.drawText(textRenderer,copy,cx-textRenderer.getWidth(copy)/2,ppy+ph-16,sc(60,sa)<<24|0x334466,false);

        super.render(ctx,mx,my,delta);
    }

    private int sc(int a,int sa){return Math.max(0,Math.min(255,a*sa/255));}
    private int blA(int col,int sa){int a=((col>>24)&0xFF)*sa/255;return(a<<24)|(col&0x00FFFFFF);}
}
