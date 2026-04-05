package com.rlcraftfarminghelper;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = RLCraftFarmingHelperMod.MOD_ID,
        name = RLCraftFarmingHelperMod.NAME,
        version = RLCraftFarmingHelperMod.VERSION,
        acceptableRemoteVersions = "*"
)
@Mod.EventBusSubscriber(modid = RLCraftFarmingHelperMod.MOD_ID)
public class RLCraftFarmingHelperMod {
    public static final String MOD_ID = "rlcraftfarminghelper";
    public static final String NAME = "RLCraft Farming Helper";
    public static final String VERSION = "1.0.1";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickCrop(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }

        World world = event.getWorld();
        BlockPos pos = event.getPos();
        HarvestTarget target = findSupportedMatureTarget(world, pos, world.getBlockState(pos));
        if (target == null) {
            return;
        }

        if (world.isRemote) {
            // Mirror server intent client-side so this click is treated as consumed by the mod.
            markInteractionHandled(event);
            LOGGER.debug("Client acknowledged handled crop click at {} ({})", pos, target.cropBlock.getRegistryName());
            return;
        }

        EntityPlayer player = event.getEntityPlayer();
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }

        LOGGER.debug("Intercepted mature crop right-click at {} by {} ({})", pos, player.getName(), target.cropBlock.getRegistryName());

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        boolean harvested = playerMP.interactionManager.tryHarvestBlock(pos);
        LOGGER.debug("Harvest attempt at {} result={}", pos, harvested);
        if (!harvested) {
            return;
        }

        markInteractionHandled(event);
        LOGGER.debug("Marked interaction handled at {} with use-block/use-item DENY", pos);

        if (canReplantAt(world, pos, target.replantedState)
                && consumeOneReplantItem(player, world, pos, target.cropBlock, target.fallbackSeedItem)) {
            world.setBlockState(pos, target.replantedState, 3);
            LOGGER.debug("Replanted {} at {}", target.cropBlock.getRegistryName(), pos);
        } else {
            LOGGER.debug("Skipped replant at {} (invalid location or no matching replant item)", pos);
        }
    }

    private static HarvestTarget findSupportedMatureTarget(World world, BlockPos pos, IBlockState state) {
        Block block = state.getBlock();

        if (block instanceof BlockCrops) {
            BlockCrops crops = (BlockCrops) block;
            if (!crops.isMaxAge(state)) {
                return null;
            }

            IBlockState replantedState = crops.withAge(0);
            Item fallbackSeedItem = crops.getItemDropped(replantedState, world.rand, 0);
            return new HarvestTarget(block, replantedState, fallbackSeedItem);
        }

        if (block instanceof BlockNetherWart) {
            int age = state.getValue(BlockNetherWart.AGE);
            if (age < 3) {
                return null;
            }

            IBlockState replantedState = state.withProperty(BlockNetherWart.AGE, 0);
            return new HarvestTarget(block, replantedState, Items.NETHER_WART);
        }

        return null;
    }

    private static void markInteractionHandled(PlayerInteractEvent.RightClickBlock event) {
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
    }

    private static boolean canReplantAt(World world, BlockPos pos, IBlockState replantedState) {
        return world.isAirBlock(pos) && replantedState.getBlock().canPlaceBlockAt(world, pos);
    }

    private static boolean consumeOneReplantItem(EntityPlayer player, World world, BlockPos cropPos, Block cropBlock, Item fallbackSeedItem) {
        // Prefer main inventory first, then offhand as a fallback source.
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (isValidReplantStack(stack, world, cropPos, cropBlock, fallbackSeedItem)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.inventory.mainInventory.set(i, ItemStack.EMPTY);
                }
                player.inventory.markDirty();
                return true;
            }
        }

        for (int i = 0; i < player.inventory.offHandInventory.size(); i++) {
            ItemStack stack = player.inventory.offHandInventory.get(i);
            if (isValidReplantStack(stack, world, cropPos, cropBlock, fallbackSeedItem)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.inventory.offHandInventory.set(i, ItemStack.EMPTY);
                }
                player.inventory.markDirty();
                return true;
            }
        }

        return false;
    }

    private static boolean isValidReplantStack(ItemStack stack, World world, BlockPos cropPos, Block cropBlock, Item fallbackSeedItem) {
        if (stack.isEmpty()) {
            return false;
        }

        // Primary strategy: use IPlantable to verify this stack places the same crop block.
        if (stack.getItem() instanceof IPlantable) {
            IBlockState plantState = ((IPlantable) stack.getItem()).getPlant(world, cropPos);
            if (plantState != null && plantState.getBlock() == cropBlock) {
                return true;
            }
        }

        // Fallback for crops/items that do not expose IPlantable correctly.
        return fallbackSeedItem != null
                && fallbackSeedItem != Item.getItemById(0)
                && stack.getItem() == fallbackSeedItem;
    }

    private static class HarvestTarget {
        private final Block cropBlock;
        private final IBlockState replantedState;
        private final Item fallbackSeedItem;

        private HarvestTarget(Block cropBlock, IBlockState replantedState, Item fallbackSeedItem) {
            this.cropBlock = cropBlock;
            this.replantedState = replantedState;
            this.fallbackSeedItem = fallbackSeedItem;
        }
    }
}
