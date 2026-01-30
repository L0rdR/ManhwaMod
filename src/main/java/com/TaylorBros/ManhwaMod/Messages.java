package com.TaylorBros.ManhwaMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class Messages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    // Send to everyone seeing the entity (including the entity itself)
    public static <MSG> void sendToAllTracking(MSG message, net.minecraft.world.entity.Entity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }

    // --- SENDER METHODS ---
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);


    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(ManhwaMod.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // 1. Upgrade Requests (Client -> Server)
        net.messageBuilder(PacketUpdateStats.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketUpdateStats::new)
                .encoder(PacketUpdateStats::toBytes)
                .consumerMainThread(PacketUpdateStats::handle)
                .add();

        // 2. Data Sync (Server -> Client)
        net.messageBuilder(PacketSyncSystemData.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncSystemData::new)
                .encoder(PacketSyncSystemData::toBytes)
                .consumerMainThread(PacketSyncSystemData::handle)
                .add();

        // 3. Boot Animation (Server -> Client)
        net.messageBuilder(PacketOpenBootAnimation.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketOpenBootAnimation::new)
                .encoder(PacketOpenBootAnimation::toBytes)
                .consumerMainThread(PacketOpenBootAnimation::handle)
                .add();

        // 4. EQUIP SKILL (Client -> Server)
        net.messageBuilder(PacketEquipSkill.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketEquipSkill::new)
                .encoder(PacketEquipSkill::toBytes)
                .consumerMainThread(PacketEquipSkill::handle)
                .add();

        // 5. CAST SKILL (Client -> Server)
        net.messageBuilder(PacketCastSkill.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketCastSkill::new)
                .encoder(PacketCastSkill::toBytes)
                .consumerMainThread(PacketCastSkill::handle)
                .add();
        // 6. INCREASE STAT (Client -> Server)
        net.messageBuilder(PacketIncreaseStat.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketIncreaseStat::new)
                .encoder(PacketIncreaseStat::toBytes)
                .consumerMainThread(PacketIncreaseStat::handle)
                .add();
        //7. i think this is for buying things from store ( Client -> Server)
        net.messageBuilder(PacketBuyItem.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketBuyItem::new)
                .encoder(PacketBuyItem::toBytes)
                .consumerMainThread(PacketBuyItem::handle)
                .add();
        // this is for extracting skills to skill orb (CLient -> Server)
        net.messageBuilder(PacketExtractSkill.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketExtractSkill::new)
                .encoder(PacketExtractSkill::toBytes)
                .consumerMainThread(PacketExtractSkill::handle)
                .add();
        //this is for the screen shake i believe
        net.messageBuilder(PacketScreenShake.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketScreenShake::new)
                .encoder(PacketScreenShake::toBytes)
                .consumerMainThread(PacketScreenShake::handle)
                .add();
        // 10. PLAY EFFECT (Server -> Clients)
        net.messageBuilder(PacketPlayEffect.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketPlayEffect::new)
                .encoder(PacketPlayEffect::toBytes)
                .consumerMainThread(PacketPlayEffect::handle)
                .add();
    }

}