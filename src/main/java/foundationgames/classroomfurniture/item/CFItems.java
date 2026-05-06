package foundationgames.classroomfurniture.item;

import foundationgames.classroomfurniture.CFUtil;
import foundationgames.classroomfurniture.ClassroomFurniture;
import foundationgames.classroomfurniture.PropDefinition;
import foundationgames.classroomfurniture.physics.constraint.SwivelPinConstraint;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.player.ItemEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public enum CFItems {;
    private static final List<ItemLike> MOD_ITEMS = new ArrayList<>();

    public static final GrabberItem GRAB_HAND = register("grab_hand", GrabberItem::new);
    public static final GlueGrabberItem GLUE_HAND = register("glue_hand", GlueGrabberItem::new);

    public static final Item HAMMER = register("hammer", Item::new);

    public static final Map<WoodType, PropItem> DESKS = CFUtil.buildMapFromStream(
            CFUtil.WOOD.stream(),
            wt -> register(wt.name() + "_desk", p -> new PropItem(p, PropDefinition.DESKS.get(wt)))
    );
    public static final Map<WoodType, PropItem> CHAIRS = CFUtil.buildMapFromStream(
            CFUtil.WOOD.stream(),
            wt -> register(wt.name() + "_chair", p -> new PropItem(p, PropDefinition.CHAIRS.get(wt)))
    );

    public static final PropItem BLUE_PENCIL_SHARPENER = register("blue_pencil_sharpener",
            p -> new PropItem(
                    p,
                    PropDefinition.PENCIL_SHARPENER,
                    () -> {
                        var constraint = new SwivelPinConstraint();
                        constraint.firstPin.set(0, 3.5/16, 0);
                        constraint.firstSwivelAxis.set(0, 0, 1);
                        constraint.secondPin.set(0, 0.5/16, 0);
                        constraint.secondSwivelAxis.set(0, 0, 1);
                        return constraint;
                    },
                    PropDefinition.PENCIL_SHARPENER_DRUM
            ));


    public static final CreativeModeTab CREATIVE_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), ClassroomFurniture.id("items")),
            FabricCreativeModeTab.builder()
                    .icon(() -> new ItemStack(CFItems.DESKS.get(WoodType.OAK)))
                    .title(Component.translatable("creativeTab." + ClassroomFurniture.ID))
                    .displayItems((params, output) -> {
                        for (var item : MOD_ITEMS) output.accept(item);
                    })
                    .build()
    );

    public static <T extends Item> T register(String name, Function<Item.Properties, T> item) {
        var id = ClassroomFurniture.id(name);
        var e = item.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)));
        MOD_ITEMS.add(e);
        return Registry.register(BuiltInRegistries.ITEM, id, e);
    }

    public static void classload() {
        ItemEvents.USE.register((level, player, hand) -> {
            var stack = player.getItemInHand(hand);
            if (stack.is(Items.BRICK)) {
                if (level.isClientSide()) {
                    return InteractionResult.SUCCESS;
                }

                var flyingBrick = PropDefinition.BRICK.entity().get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);

                if (flyingBrick != null) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }

                    var rng = level.getRandom();
                    var look = player.getHeadLookAngle();
                    var vel = player.getDeltaMovement().scale(2);

                    flyingBrick.setPos(player.getEyePosition().add(player.getHeadLookAngle().scale(0.7)));
                    flyingBrick.externalImpulse.set(look.x, look.y, look.z).mul(40).add(vel.x, vel.y, vel.z);
                    flyingBrick.externalAngularImpulse.set(rng.nextDouble(), rng.nextDouble(), rng.nextDouble()).mul(0.3);

                    level.addFreshEntity(flyingBrick);
                }

                return InteractionResult.CONSUME;
            }

            return null;
        });
    }
}
