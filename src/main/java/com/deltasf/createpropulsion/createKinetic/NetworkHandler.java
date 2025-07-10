package com.deltasf.createpropulsion.createKinetic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import static com.deltasf.createpropulsion.createKinetic.CreateKineticMod.K_ID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class NetworkHandler {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        NetworkHandler.registerMessages();
    }

    public static SimpleChannel CHANNEL_INSTANCE;
    private static int netID = 0;

    public static int nextID() {
        return netID ++;
    }

    public static void registerMessages() {
        CHANNEL_INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(K_ID, K_ID), () -> "1.0", s -> true, s -> true);

        CHANNEL_INSTANCE.messageBuilder(CustomTotemPacket.class, nextID())
                .encoder(CustomTotemPacket::encode)
                .decoder(CustomTotemPacket::decode)
                .consumerNetworkThread(CustomTotemPacket::handle)
                .add();
    }

    public static void sendToClient(Object packet, ServerPlayer player) {
        CHANNEL_INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToServer(Object packet) {
        CHANNEL_INSTANCE.sendToServer(packet);
    }
}
