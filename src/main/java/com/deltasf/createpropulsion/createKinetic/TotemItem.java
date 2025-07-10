package com.deltasf.createpropulsion.createKinetic;


import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

import java.util.function.BiPredicate;

//EnderpearlItem
public class TotemItem extends Item {


    private BiPredicate<ServerPlayer, ItemStack> _onUse;
//    private String _particleID;
//    private RegistryEntry.Reference<SoundEvent> _regSound;

    public TotemItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    public TotemItem(BiPredicate<ServerPlayer, ItemStack> _onUse) {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        this._onUse = _onUse;
    }


    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (world.isClientSide()) {
//            System.out.println("Displaying item activation");
//            Minecraft.getInstance().gameRenderer.displayItemActivation(new ItemStack(this));
        } else {

            if (_onUse != null && _onUse.test((ServerPlayer) player, itemStack)){//Trigger the animation and sound
                NetworkHandler.sendToClient(new CustomTotemPacket(itemStack), (ServerPlayer) player);
                world.playSound(player, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 0.2f, 1f);

                if (!player.isCreative()) {
                    itemStack.setCount(0);
                }
            }

//            player.level().broadcastEntityEvent(player, (byte) 35);
//            ServerPlayNetworking.send((ServerPlayerEntity) user, new EffigyParticlePayload(_particleID));

            // If the sound is provided as a reg key we do this server side
//            if (_regSound != null) {
//                var serverPlayerEntity = (ServerPlayerEntity) user;
//                serverPlayerEntity.networkHandler.sendPacket(new PlaySoundS2CPacket(_regSound, SoundCategory.NEUTRAL,
//                        serverPlayerEntity.getX(), serverPlayerEntity.getY(), serverPlayerEntity.getZ(), 128.0F, 1.0F, 1l));
//            }
        }

        return InteractionResultHolder.consume(itemStack);
    }

}