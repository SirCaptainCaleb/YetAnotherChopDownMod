package com.sircaptaincaleb.yetanotherchopdownmod;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@Mod(YetAnotherChopDownMod.MODID)
public class YetAnotherChopDownMod {

    public static final String MODID = "yetanotherchopdownmod";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public YetAnotherChopDownMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        IWorld world = event.getWorld();
        BlockState state = world.getBlockState(event.getPos());
        BlockPos pos = event.getPos();
        if (!isWood(state.getBlock())){
            return;
        }
        int radius = 16;
        int dirX = Math.max(-1, Math.min(1, pos.getX() - (int)Math.round(event.getPlayer().getPosition().getX() - 0.5)));
        int dirZ = Math.max(-1, Math.min(1, pos.getZ() - (int)Math.round(event.getPlayer().getPosition().getZ() - 0.5)));
        LinkedList<BlockPos> queue = new LinkedList<BlockPos>();
        HashMap<BlockPos, Integer> used = new HashMap<BlockPos, Integer>();
        queue.add(pos);
        int leaf = 5;
        used.put(pos, leaf);
        while (!queue.isEmpty()) {
            BlockPos top = queue.pollFirst();
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dy = -1; dy <= 1; ++dy) {
                    for (int dz = -1; dz <= 1; ++dz) {
                        BlockPos nPos = top.add(dx, dy, dz);
                        int step = used.get(top);
                        if (step <= 0 || nPos.distanceSq(pos) > radius * radius) {
                            continue;
                        }
                        BlockState nState = world.getBlockState(nPos);
                        boolean log = isWood(nState.getBlock());
                        boolean leaves = isLeaves(nState.getBlock());
                        if ((dy >= 0 && step == leaf && log) || leaves) {
                            step = step - (leaves ? 1 : 0);
                            if (!used.containsKey(nPos) || used.get(nPos) < step) {
                                used.put(nPos, step);
                                queue.push(nPos);
                            }
                        }
                    }
                }
            }
        }
        for (Map.Entry<BlockPos, Integer> entry : used.entrySet()) {
            BlockPos blockPos = entry.getKey();
            if (!pos.equals(blockPos) && isDraggable(world, blockPos.add(0, -1, 0))) {
                int oy = blockPos.getY() - pos.getY();
                drop(world, blockPos, blockPos.add(oy * dirX, 0, oy * dirZ));
            }
        }
    }

    private static boolean isWood(Block block) {
        Set<ResourceLocation> tags = block.getTags();
                return tags.contains(BlockTags.LOGS.getId()) &
                        !block.getRegistryName().toString().contains("bamboo");
    }

    private static boolean isLeaves(Block block) {
        return block.getTags().contains(BlockTags.LEAVES.getId());
    }

    private static boolean isAir(BlockState blockState) {
        return blockState.getMaterial() == Material.AIR;
    }

    private static boolean isPassable(BlockState blockState) {
        return !blockState.getMaterial().blocksMovement();
    }

    private static boolean isDraggable(IWorld world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        return  isWood(block) ||
                isLeaves(block) ||
                isAir(blockState) ||
                isPassable(blockState);
    }

    private static void drop(IWorld world, BlockPos pos, BlockPos newPos) {
        FallingBlockEntity fallingBlock = new FallingBlockEntity(world.getWorld(), newPos.getX(), newPos.getY(), newPos.getZ(), world.getBlockState(pos));
        fallingBlock.setBoundingBox(new AxisAlignedBB(newPos.add(0, 0, 0), newPos.add(1, 1, 1)));
        fallingBlock.fallTime = 1;
        world.addEntity(fallingBlock);
        world.setBlockState(pos, Blocks.AIR.getDefaultState(),67);
    }

}
