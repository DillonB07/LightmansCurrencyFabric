package io.github.lightman314.lightmanscurrency.network.server.messages.traderinterface;

import io.github.lightman314.lightmanscurrency.common.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.blockentity.traderinterface.TraderInterfaceBlockEntity;
import io.github.lightman314.lightmanscurrency.network.server.ClientToServerPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class CMessageHandlerMessage extends ClientToServerPacket {

    public static final Identifier PACKET_ID = new Identifier(LightmansCurrency.MODID, "traderinterface_handler_message");

    private final BlockPos blockPos;
    private final Identifier handlerType;
    private final NbtCompound message;
    public CMessageHandlerMessage(BlockPos blockPos, Identifier handlerType, NbtCompound message) { super(PACKET_ID); this.blockPos = blockPos; this.handlerType = handlerType; this.message = message; }

    @Override
    protected void encode(PacketByteBuf buffer) { buffer.writeBlockPos(this.blockPos); buffer.writeString(this.handlerType.toString()); buffer.writeNbt(this.message); }

    public static void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buffer, PacketSender responseSender) {
        BlockEntity blockEntity = player.getWorld().getBlockEntity(buffer.readBlockPos());
        if(blockEntity instanceof TraderInterfaceBlockEntity traderInterface)
            traderInterface.receiveHandlerMessage(new Identifier(buffer.readString()), player, buffer.readUnlimitedNbt());
    }
}
