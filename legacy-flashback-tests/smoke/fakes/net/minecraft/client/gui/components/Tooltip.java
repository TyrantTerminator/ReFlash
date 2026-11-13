package net.minecraft.client.gui.components;
import net.minecraft.network.chat.Component;
public record Tooltip(Component label) { public static Tooltip create(Component c){return new Tooltip(c);} }
