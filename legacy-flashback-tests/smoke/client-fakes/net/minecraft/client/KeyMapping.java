package net.minecraft.client;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class KeyMapping{
 public static final class Category {public static Category register(Identifier id){return new Category();}}
 private static final Map<String,KeyMapping> ALL=new ConcurrentHashMap<>();
 private final String name;private InputConstants.Key bound;
 public KeyMapping(String key,InputConstants.Type type,int code,Category cat){this.name=key;this.bound=new InputConstants.Key(code);ALL.put(key,this);}
 public boolean consumeClick(){return false;}
 public net.minecraft.network.chat.Component getTranslatedKeyMessage(){return net.minecraft.network.chat.Component.literal(bound.value()==296?"F7":bound.value()==298?"F9":bound.value()==-1?"Unbound":"F"+(bound.value()-289));}
 public static KeyMapping get(String name){return ALL.get(name);}
 public boolean isUnbound(){return bound.value()==-1;}
 public void setKey(InputConstants.Key key){bound=key;}
 public static void resetMapping(){}
}
