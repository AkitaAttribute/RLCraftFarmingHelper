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
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickCrop(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote || event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }

        EntityPlayer player = event.getEntityPlayer();
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        IBlockState replantedState;
        Item fallbackSeedItem;

        if (block instanceof BlockCrops) {
            BlockCrops crops = (BlockCrops) block;
            if (!crops.isMaxAge(state)) {
                return;
            }
            replantedState = crops.withAge(0);
            fallbackSeedItem = crops.getItemDropped(replantedState, world.rand, 0);
        } else if (block instanceof BlockNetherWart) {
            // Nether wart is age-based but not a BlockCrops subclass, so handle it explicitly.
            int age = state.getValue(BlockNetherWart.AGE);
            if (age < 3) {
                return;
            }
            replantedState = state.withProperty(BlockNetherWart.AGE, 0);
            fallbackSeedItem = Items.NETHER_WART;
        } else {
            return;
        }

        if (!hasReplantItem(player, world, pos, block, fallbackSeedItem)) {
            return;
        }

        if (!(player instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        boolean harvested = playerMP.interactionManager.tryHarvestBlock(pos);
        if (!harvested || !world.isAirBlock(pos)) {
            return;
        }

        if (!consumeOneReplantItem(player, world, pos, block, fallbackSeedItem)) {
            return;
        }

        world.setBlockState(pos, replantedState, 3);
        event.setCanceled(true);
    }

    private static boolean hasReplantItem(EntityPlayer player, World world, BlockPos cropPos, Block cropBlock, Item fallbackSeedItem) {
        for (ItemStack stack : player.inventory.mainInventory) {
            if (isValidReplantStack(stack, world, cropPos, cropBlock, fallbackSeedItem)) {
                return true;
            }
        }

        for (ItemStack stack : player.inventory.offHandInventory) {
            if (isValidReplantStack(stack, world, cropPos, cropBlock, fallbackSeedItem)) {
                return true;
            }
        }

        return false;
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
}
