import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.io.*;

public final class Apply74 implements Opcodes {
    static final String CLIENT="dev/reflash/client/";
    static int changed=0;
    static void assertOne(int n,String label){if(n!=1)throw new AssertionError(label+": expected 1, got "+n);}
    static MethodNode method(ClassNode c,String name){for(MethodNode m:c.methods)if(m.name.equals(name))return m;throw new AssertionError("Missing "+c.name+"."+name);}
    static int replaceMin(MethodNode m,int limit){
        int n=0;
        for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof IntInsnNode q && q.getOpcode()==BIPUSH && q.operand==limit){
            AbstractInsnNode j=i.getNext();
            while(j instanceof LabelNode || j instanceof LineNumberNode || j instanceof FrameNode) j=j.getNext();
            if(j instanceof JumpInsnNode x && (x.getOpcode()==IF_ICMPLT || x.getOpcode()==IFLT)){
                m.instructions.set(i,new InsnNode(ICONST_1));n++;
            }
        }
        return n;
    }
    static int replaceStr(ClassNode c,String before,String after){int count=0;for(MethodNode m:c.methods)for(AbstractInsnNode i:m.instructions.toArray()) if(i instanceof LdcInsnNode l && before.equals(l.cst)){l.cst=after;count++;}return count;}
    static void injectBeforeReturn(MethodNode m,String cls,String name,String desc,boolean passArg0,boolean passArg1){
        for(AbstractInsnNode i:m.instructions.toArray())if(i.getOpcode()==RETURN || i.getOpcode()==IRETURN){
            InsnList add=new InsnList();if(passArg0)add.add(new VarInsnNode(ALOAD,0));if(passArg1)add.add(new VarInsnNode(ALOAD,1));
            add.add(new MethodInsnNode(INVOKESTATIC,cls,name,desc,false));m.instructions.insertBefore(i,add);
        }
    }
    static void process(ClassNode c){
        switch(c.name){
            case "dev/reflash/core/Preferences" -> assertOne(replaceMin(method(c,"setOutputPreset"),24),c.name);
            case "dev/reflash/core/ExportSizeEstimate" -> assertOne(replaceMin(method(c,"calculate"),24),c.name);
            case "dev/reflash/core/ReFlashMotionFilter" -> { assertOne(replaceMin(method(c,"filter"),24),c.name);replaceStr(c,"Output must be even-sized, up to 4K and 24-240 FPS","Output must be even-sized, up to 4K and 1-240 FPS"); }
            case "dev/reflash/core/VideoPostProcessor" -> {for(MethodNode m:c.methods)if(m.name.equals("filter") && m.desc.contains("UpscaleProfile"))assertOne(replaceMin(m,24),c.name);replaceStr(c,"Output must be even dimensions up to 4K and 24-240 FPS","Output must be even dimensions up to 4K and 1-240 FPS");}
            case "dev/reflash/client/NativeReplayExporter" -> {assertOne(replaceMin(method(c,"render"),24),c.name);replaceStr(c,"Select a replay and FPS 24-240; avoid parallel exports","Select a replay and FPS 1-240; avoid parallel exports");}
            case "dev/reflash/client/ReFlashCustomOutputScreen" -> {assertOne(replaceMin(method(c,"save"),30),c.name);replaceStr(c,"FPS must be 30 to 240","FPS must be 1 to 240");replaceStr(c,"FRAME RATE 30-240","FRAME RATE 1-240");}
            case "dev/reflash/client/ReFlashTextEntryScreen" -> {
                MethodNode m=method(c,"method_25426");int n=0;
                for(AbstractInsnNode i:m.instructions.toArray()) if(i instanceof LdcInsnNode l && l.cst instanceof String text && ("Rename".equals(text)||"New name".equals(text))){
                    InsnList extra=new InsnList();extra.add(new VarInsnNode(ALOAD,0));
                    extra.add(new MethodInsnNode(INVOKESTATIC,CLIENT+"ReFlashFps74",("Rename".equals(text)?"actionLabel":"inputLabel"),"(Ljava/lang/Object;)Ljava/lang/String;",false));
                    m.instructions.insertBefore(i,extra);m.instructions.remove(i);n++;
                }if(n!=2)throw new AssertionError("Expected two dialog labels, got "+n);
            }
            case "dev/reflash/client/ReFlashVideoExportScreen" -> {MethodNode m=method(c,"method_25426");int n=0;for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof IntInsnNode q&&q.getOpcode()==BIPUSH&&q.operand==30){m.instructions.set(i,new InsnNode(ICONST_1));n++;break;}assertOne(n,c.name);}
            case "dev/reflash/client/ReFlashScreen" -> {
                MethodNode init=method(c,"method_25426");injectBeforeReturn(init,CLIENT+"ReFlashResponsive74","apply","(Ljava/lang/Object;)V",true,false);
                MethodNode render=method(c,"method_25394");injectBeforeReturn(render,CLIENT+"ReFlashBeta74","draw","(Ljava/lang/Object;Ljava/lang/Object;)V",true,true);
                MethodNode mouse=method(c,"method_25402");int n=0;
                for(AbstractInsnNode i:mouse.instructions.toArray())if(i instanceof MethodInsnNode mm && mm.name.equals("mouseEvent") && mm.owner.equals(CLIENT+"ReFlashUI38")) {
                    InsnList nodes=new InsnList();nodes.add(new VarInsnNode(ALOAD,0));nodes.add(new VarInsnNode(ALOAD,1));
                    nodes.add(new MethodInsnNode(INVOKESTATIC,CLIENT+"ReFlashBeta74","mouse","(Ljava/lang/Object;Ljava/lang/Object;)Z",false));nodes.add(new InsnNode(IOR));
                    mouse.instructions.insert(i,nodes);n++;
                }assertOne(n,"beta mouse interception");
                // Legacy rates: make the first preset 1 FPS. Do not change recording FPS.
                MethodNode clinit=method(c,"<clinit>");int rateIndex=0;
                boolean rates=false;for(AbstractInsnNode i:clinit.instructions.toArray()){
                    if(i instanceof IntInsnNode q && q.getOpcode()==BIPUSH && q.operand==30 && !rates){q.operand=1;rateIndex++;rates=true;}
                }assertOne(rateIndex,"legacy rate array");
            }
            case "dev/reflash/client/ReFlashExportPanel60" -> {
                MethodNode m=method(c,"init");
                // The five quick choices become 1, 30, 60, 120, 240. 144 is available through CUSTOM FPS.
                boolean inRates=false; int count=0;
                Map<String,String> map=Map.of("30 FPS","1 FPS","60 FPS","30 FPS","120 FPS","60 FPS","144 FPS","120 FPS","30","1","60","30","120","60","144","120");
                for(AbstractInsnNode i:m.instructions.toArray()) if(i instanceof LdcInsnNode l && l.cst instanceof String s){
                    if("30 FPS".equals(s))inRates=true;
                    if(inRates && map.containsKey(s)){l.cst=map.get(s);count++;}
                    if(inRates && "240".equals(s))inRates=false;
                }
                if(count!=8)throw new AssertionError("FPS quick presets expected 8 literals, got "+count);
                injectBeforeReturn(m,CLIENT+"ReFlashFps74","install","(Ljava/lang/Object;)V",true,false);
            }
            case "dev/reflash/client/ReFlashAboutUI60" -> replaceStr(c,"30-240 FPS","1-240 FPS");
        }
    }
    public static void main(String[] args)throws Exception {
        Path in=Path.of(args[0]),out=Path.of(args[1]);byte[] b=Files.readAllBytes(in);ClassReader r=new ClassReader(b);ClassNode c=new ClassNode();r.accept(c,0);process(c);
        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);Files.createDirectories(out.getParent());Files.write(out,w.toByteArray());System.out.println("PATCHED "+c.name);
    }
}
