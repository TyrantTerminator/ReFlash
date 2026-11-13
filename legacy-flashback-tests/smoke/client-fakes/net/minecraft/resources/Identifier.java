package net.minecraft.resources;
public final class Identifier {
  private final String path;
  private Identifier(String p){path=p;}
  public static Identifier fromNamespaceAndPath(String a,String b){return new Identifier(a+":"+b);}
  public String toString(){return path;}
}
