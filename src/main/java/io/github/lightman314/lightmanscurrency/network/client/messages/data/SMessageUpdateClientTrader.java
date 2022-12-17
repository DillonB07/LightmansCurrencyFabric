package io.github.lightman314.lightmanscurrency.network.client.messages.data;

import io.github.lightman314.lightmanscurrency.client.data.ClientTraderData;
import io.github.lightman314.lightmanscurrency.common.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.network.client.ServerToClientPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class SMessageUpdateClientTrader extends ServerToClientPacket {

    public static final Identifier PACKET_ID = new Identifier(LightmansCurrency.MODID, "datasync_trader_update");

    private final NbtCompound data;

    public SMessageUpdateClientTrader(NbtCompound data) { super(PACKET_ID); this.data = data; }

    @Override
    protected void encode(PacketByteBuf buffer) { buffer.writeNbt(data); }

    @Environment(EnvType.CLIENT)
    public static void handle(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buffer, PacketSender responseSender) {
        ClientTraderData.UpdateTrader(buffer.readUnlimitedNbt());
    }

}
