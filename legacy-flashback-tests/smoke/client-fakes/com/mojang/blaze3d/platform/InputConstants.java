package com.mojang.blaze3d.platform;
public final class InputConstants {
 public enum Type { KEYSYM }
 public record Key(int value){}
 public static final Key UNKNOWN=new Key(-1);
}
