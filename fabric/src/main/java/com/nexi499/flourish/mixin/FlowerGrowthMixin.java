package com.nexi499.flourish.mixin;

import com.nexi499.flourish.FlourishMod;
import com.nexi499.flourish.options.FlowerOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FlowerBlock.class)
public abstract class FlowerGrowthMixin extends Block implements BonemealableBlock {
    protected FlowerGrowthMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        FlowerOptions options = FlourishMod.options();
        return state.is(BlockTags.SMALL_FLOWERS)
                && (options.witherRose || state.getBlock() != Blocks.WITHER_ROSE)
                && (options.torchflower || state.getBlock() != Blocks.TORCHFLOWER);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (FlourishMod.options().useTallFlowerBehavior) {
            flourish$dropFlower(level, pos);
            return;
        }
        flourish$spreadFlowers(level, random, pos);
    }

    @Override
    public @NotNull Type getType() {
        return FlourishMod.options().useTallFlowerBehavior ? Type.GROWER : Type.NEIGHBOR_SPREADER;
    }

    @Unique
    private void flourish$dropFlower(ServerLevel level, BlockPos pos) {
        Block.popResource(level, pos, new ItemStack(this, 1));
    }

    @Unique
    private void flourish$spreadFlowers(ServerLevel level, RandomSource random, BlockPos pos) {
        int maxSuccesses = random.nextIntBetweenInclusive(1, 7);
        int successes = 0;
        for (int attempt = 0; attempt < 64 && successes < maxSuccesses; attempt++) {
            BlockPos candidate = pos;
            for (int offset = 0; offset < attempt / 22 + 1; offset++) {
                candidate = candidate.offset(
                        random.nextIntBetweenInclusive(-1, 1),
                        0,
                        random.nextIntBetweenInclusive(-1, 1)
                );
            }
            candidate = candidate.offset(0, random.nextIntBetweenInclusive(-1, 1), 0);
            BlockPos ground = candidate.below();
                BlockState groundState = level.getBlockState(ground);
                if ((groundState.is(BlockTags.DIRT) || groundState.is(Blocks.FARMLAND))
                    && level.getBlockState(candidate).isAir()) {
                level.setBlock(candidate, defaultBlockState(), 1 | 2);
                successes++;
            }
        }
    }
}