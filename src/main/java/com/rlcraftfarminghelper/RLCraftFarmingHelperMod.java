package com.rlcraftfarminghelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.IPlantable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(RLCraftFarmingHelperMod.MOD_ID)
public final class RLCraftFarmingHelperMod {
    public static final String MOD_ID = "rlcraftfarminghelper";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public RLCraftFarmingHelperMod() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickCrop(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        HarvestTarget target = findSupportedMatureTarget(level.getBlockState(pos));
        if (target == null) {
            return;
        }

        if (level.isClientSide()) {
            markInteractionHandled(event);
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LOGGER.debug("Intercepted mature crop right-click at {} by {} ({})",
                pos, player.getGameProfile().getName(), target.cropBlock());

        boolean harvested = player.gameMode.destroyBlock(pos);
        if (!harvested) {
            return;
        }

        markInteractionHandled(event);

        if (canReplantAt(level, pos, target.replantedState())
                && consumeOneReplantItem(player, level, pos, target.cropBlock())) {
            level.setBlock(pos, target.replantedState(), Block.UPDATE_ALL);
            LOGGER.debug("Replanted {} at {}", target.cropBlock(), pos);
        } else {
            LOGGER.debug("Harvested {} at {} without replanting", target.cropBlock(), pos);
        }
    }

    private static HarvestTarget findSupportedMatureTarget(BlockState state) {
        Block block = state.getBlock();

        if (block instanceof CropBlock crops) {
            if (!crops.isMaxAge(state)) {
                return null;
            }
            return new HarvestTarget(block, crops.getStateForAge(0));
        }

        if (block instanceof NetherWartBlock) {
            int age = state.getValue(NetherWartBlock.AGE);
            if (age < NetherWartBlock.MAX_AGE) {
                return null;
            }
            return new HarvestTarget(block, state.setValue(NetherWartBlock.AGE, 0));
        }

        return null;
    }

    private static void markInteractionHandled(PlayerInteractEvent.RightClickBlock event) {
        event.setUseBlock(PlayerInteractEvent.Result.DENY);
        event.setUseItem(PlayerInteractEvent.Result.DENY);
        event.setCanceled(true);
    }

    private static boolean canReplantAt(Level level, BlockPos pos, BlockState replantedState) {
        return level.isEmptyBlock(pos) && replantedState.canSurvive(level, pos);
    }

    private static boolean consumeOneReplantItem(
            ServerPlayer player,
            Level level,
            BlockPos cropPos,
            Block cropBlock
    ) {
        for (ItemStack stack : player.getInventory().items) {
            if (isValidReplantStack(stack, level, cropPos, cropBlock)) {
                consumeOne(player, stack);
                return true;
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (isValidReplantStack(stack, level, cropPos, cropBlock)) {
                consumeOne(player, stack);
                return true;
            }
        }

        return false;
    }

    private static void consumeOne(ServerPlayer player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            player.getInventory().setChanged();
        }
    }

    private static boolean isValidReplantStack(
            ItemStack stack,
            Level level,
            BlockPos cropPos,
            Block cropBlock
    ) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        if (item instanceof IPlantable plantable) {
            BlockState plantState = plantable.getPlant(level, cropPos);
            return plantState != null && plantState.getBlock() == cropBlock;
        }

        return false;
    }

    private record HarvestTarget(Block cropBlock, BlockState replantedState) {
    }
}
