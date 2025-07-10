package com.deltasf.createpropulsion.createKinetic;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CustomTotemPacket {
    private final ItemStack item;

    public CustomTotemPacket(ItemStack item) {
        this.item = item;
    }

    public static void encode(CustomTotemPacket msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.item);
    }

    public static CustomTotemPacket decode(FriendlyByteBuf buf) {
        ItemStack item = buf.readItem();
        return new CustomTotemPacket(item);
    }

    public static void handle(CustomTotemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && msg.item != null) {
                ItemStack stack = msg.item;
                stack.setCount(1);

                //If we play the sound here, it will only sound for this player
//                Level world = Minecraft.getInstance().level;
//                world.playSound(mc.player, mc.player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 0.2f, 1f);

                Minecraft.getInstance().gameRenderer.displayItemActivation(stack);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
