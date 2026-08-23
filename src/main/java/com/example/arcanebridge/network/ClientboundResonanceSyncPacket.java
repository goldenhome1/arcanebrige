package com.example.arcanebridge.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class ClientboundResonanceSyncPacket {
    private final float stability;
    private final int mechLoad;
    private final int arcaneLoad;
    private final int eleLoad;
    private final int mechLimit;
    private final int arcaneLimit;
    private final int eleLimit;

    private final boolean mechShock;
    private final boolean arcaneShock;
    private final boolean eleShock;

    private final boolean inRaidZone;

    public static float clientStability = 100.0F;
    public static int mechLoadStatic = 0;
    public static int arcaneLoadStatic = 0;
    public static int eleLoadStatic = 0;
    public static int mechLimitStatic = 2;
    public static int arcaneLimitStatic = 2;
    public static int eleLimitStatic = 2;

    public static boolean mechShockStatic = false;
    public static boolean arcaneShockStatic = false;
    public static boolean eleShockStatic = false;

    // Статическая переменная для клиента
    public static boolean inRaidZoneStatic = false;

    public ClientboundResonanceSyncPacket(float stability, int mechLoad, int arcaneLoad, int eleLoad,
                                          int mechLimit, int arcaneLimit, int eleLimit,
                                          boolean mechShock, boolean arcaneShock, boolean eleShock,
                                          boolean inRaidZone) {
        this.stability = stability;
        this.mechLoad = mechLoad;
        this.arcaneLoad = arcaneLoad;
        this.eleLoad = eleLoad;
        this.mechLimit = mechLimit;
        this.arcaneLimit = arcaneLimit;
        this.eleLimit = eleLimit;
        this.mechShock = mechShock;
        this.arcaneShock = arcaneShock;
        this.eleShock = eleShock;
        this.inRaidZone = inRaidZone;
    }

    public ClientboundResonanceSyncPacket(FriendlyByteBuf buf) {
        this.stability = buf.readFloat();
        this.mechLoad = buf.readInt();
        this.arcaneLoad = buf.readInt();
        this.eleLoad = buf.readInt();
        this.mechLimit = buf.readInt();
        this.arcaneLimit = buf.readInt();
        this.eleLimit = buf.readInt();
        this.mechShock = buf.readBoolean();
        this.arcaneShock = buf.readBoolean();
        this.eleShock = buf.readBoolean();
        this.inRaidZone = buf.readBoolean();
    }

    public static void encode(ClientboundResonanceSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.stability);
        buf.writeInt(msg.mechLoad);
        buf.writeInt(msg.arcaneLoad);
        buf.writeInt(msg.eleLoad);
        buf.writeInt(msg.mechLimit);
        buf.writeInt(msg.arcaneLimit);
        buf.writeInt(msg.eleLimit);
        buf.writeBoolean(msg.mechShock);
        buf.writeBoolean(msg.arcaneShock);
        buf.writeBoolean(msg.eleShock);
        buf.writeBoolean(msg.inRaidZone);
    }

    public static ClientboundResonanceSyncPacket decode(FriendlyByteBuf buf) {
        return new ClientboundResonanceSyncPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            clientStability = this.stability;
            mechLoadStatic = this.mechLoad;
            arcaneLoadStatic = this.arcaneLoad;
            eleLoadStatic = this.eleLoad;
            mechLimitStatic = this.mechLimit;
            arcaneLimitStatic = this.arcaneLimit;
            eleLimitStatic = this.eleLimit;
            mechShockStatic = this.mechShock;
            arcaneShockStatic = this.arcaneShock;
            eleShockStatic = this.eleShock;

            // Обновляем флаг зоны рейда на клиенте
            inRaidZoneStatic = this.inRaidZone;
        });
        context.setPacketHandled(true);
    }
}