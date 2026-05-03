package foundationgames.classroomfurniture.network;

import foundationgames.classroomfurniture.ClassroomFurniture;
import foundationgames.classroomfurniture.item.GrabInfo;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public class GrabPropPackets {
    public record ServerboundUpdateGrab(ItemStack grabItem, GrabInfo updatedGrab) implements CustomPacketPayload {
        public static final Type<ServerboundUpdateGrab> TYPE = new Type<>(ClassroomFurniture.id("update_grab"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundUpdateGrab> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, ServerboundUpdateGrab::grabItem,
                GrabInfo.STREAM_CODEC, ServerboundUpdateGrab::updatedGrab,
                ServerboundUpdateGrab::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public void receiveServer(ServerPlayNetworking.Context ctx) {
            var grabberEntity = ctx.player();

            if (grabberEntity.getUUID().equals(updatedGrab().grabber)) {
                var currentGrab = GrabInfo.get(grabberEntity);

                currentGrab.updateOnSelfAndProp(grabberEntity.level(), grabItem(), updatedGrab());
            }
        }
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(ServerboundUpdateGrab.TYPE, ServerboundUpdateGrab.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ServerboundUpdateGrab.TYPE, ServerboundUpdateGrab::receiveServer);
    }
}
