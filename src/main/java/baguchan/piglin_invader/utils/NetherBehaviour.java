package baguchan.piglin_invader.utils;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.common.Tags;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NetherBehaviour {
    NetherBehaviour DEFAULT = new NetherBehaviour() {

        public boolean attemptSpreadVein(LevelAccessor p_222048_, BlockPos p_222049_, BlockState p_222050_, @Nullable Collection<Direction> p_222051_, boolean p_222052_) {
            if (p_222051_ == null) {
                return regrowNetherPlant(p_222048_, p_222049_, p_222050_, p_222051_) || NetherBehaviour.super.attemptSpreadVein(p_222048_, p_222049_, p_222050_, p_222051_, p_222052_);
            } else if (!p_222051_.isEmpty()) {
                return regrowNetherPlant(p_222048_, p_222049_, p_222050_, p_222051_);
            } else {
                return NetherBehaviour.super.attemptSpreadVein(p_222048_, p_222049_, p_222050_, p_222051_, p_222052_);
            }
        }


        public int attemptUseCharge(NetherSpeader.ChargeCursor p_222054_, LevelAccessor p_222055_, BlockPos p_222056_, RandomSource p_222057_, NetherSpeader p_222058_, boolean p_222059_) {
            return p_222054_.getCharge();
        }

        public int updateDecayDelay(int p_222061_) {
            return Math.max(p_222061_ - 1, 0);
        }
    };

    public static boolean regrowNetherPlant(LevelAccessor p_222364_, BlockPos p_222365_, BlockState p_222366_, Collection<Direction> p_222367_) {
        boolean flag = false;
        BlockState blockstate = Blocks.LAVA.defaultBlockState();
        if (p_222366_.is(BlockTags.NYLIUM) && p_222366_.getBlock() instanceof BonemealableBlock bonemealableBlock) {
            if (p_222364_ instanceof ServerLevel serverLevel) {
                bonemealableBlock.performBonemeal(serverLevel, p_222364_.getRandom(), p_222365_, p_222366_);
                return true;
            }
        }


        if (p_222366_.is(Blocks.WATER)) {
            p_222364_.setBlock(p_222365_, blockstate, 3);
            return true;
        }

        return false;
    }

    public static boolean regrowNether(LevelAccessor p_222364_, BlockPos p_222365_, BlockState p_222366_, Collection<Direction> p_222367_) {

        if (p_222366_.is(Blocks.GRASS_BLOCK)) {

            BlockState blockstate = Blocks.CRIMSON_NYLIUM.defaultBlockState();

            p_222364_.setBlock(p_222365_, blockstate, 3);
            p_222364_.levelEvent(2001, p_222365_, Block.getId(blockstate));
            changeBiome(p_222364_, p_222365_, p_222366_);
            return true;
        } else if (p_222366_.is(BlockTags.OVERWORLD_CARVER_REPLACEABLES)) {

            BlockState blockstate = Blocks.NETHERRACK.defaultBlockState();

            p_222364_.setBlock(p_222365_, blockstate, 3);

            p_222364_.levelEvent(2001, p_222365_, Block.getId(blockstate));

            changeBiome(p_222364_, p_222365_, p_222366_);
            return true;

        }
        return false;
    }

    static void changeBiome(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {

        MutableInt mutableint = new MutableInt(0);
        BoundingBox boundingbox = new BoundingBox(blockPos.getX() - 1, blockPos.getY(), blockPos.getZ() - 1, blockPos.getX() + 1, blockPos.getY() + 20, blockPos.getZ() + 1);

        List<ChunkAccess> list = new ArrayList<>();

        if (levelAccessor instanceof ServerLevel serverLevel) {
            for (int k = SectionPos.blockToSectionCoord(boundingbox.minZ()); k <= SectionPos.blockToSectionCoord(boundingbox.maxZ()); ++k) {
                for (int l = SectionPos.blockToSectionCoord(boundingbox.minX()); l <= SectionPos.blockToSectionCoord(boundingbox.maxX()); ++l) {
                    ChunkAccess chunkaccess = serverLevel.getChunk(l, k, ChunkStatus.FULL, false);
                    list.add(chunkaccess);
                }
            }

            for (ChunkAccess chunkaccess1 : list) {
                chunkaccess1.fillBiomesFromNoise(makeResolver(levelAccessor, mutableint, chunkaccess1, boundingbox), serverLevel.getChunkSource().randomState().sampler());
                chunkaccess1.setUnsaved(true);
            }
            serverLevel.getChunkSource().chunkMap.resendBiomesForChunks(list);
        }
    }

    private static BiomeResolver makeResolver(LevelAccessor levelAccessor, MutableInt p_262615_, ChunkAccess p_262698_, BoundingBox p_262622_) {

        return (p_262550_, p_262551_, p_262552_, p_262553_) -> {
            int i = QuartPos.toBlock(p_262550_);
            int j = QuartPos.toBlock(p_262551_);
            int k = QuartPos.toBlock(p_262552_);
            Holder<Biome> holder = p_262698_.getNoiseBiome(p_262550_, p_262551_, p_262552_);
            if (p_262622_.isInside(i, j, k) && !holder.is(BiomeTags.IS_NETHER)) {
                p_262615_.increment();
                if (holder.is(Tags.Biomes.IS_SANDY)) {
                    Optional<Holder.Reference<Biome>> biomeHolder = levelAccessor.registryAccess().registryOrThrow(Registries.BIOME).getHolder(Biomes.NETHER_WASTES);
                    if (biomeHolder.isPresent()) {
                        return biomeHolder.get();
                    }
                }
                Optional<Holder.Reference<Biome>> biomeHolder = levelAccessor.registryAccess().registryOrThrow(Registries.BIOME).getHolder(Biomes.CRIMSON_FOREST);
                if (biomeHolder.isPresent()) {
                    return biomeHolder.get();
                }
            } else {
                return holder;
            }
            return holder;
        };
    }

    default byte getNetherSpreadDelay() {
        return 1;
    }

    default void onDischarged(LevelAccessor p_222026_, BlockState p_222027_, BlockPos p_222028_, RandomSource p_222029_) {
    }

    default boolean depositCharge(LevelAccessor p_222031_, BlockPos p_222032_, RandomSource p_222033_) {
        return false;
    }

    default boolean attemptSpreadVein(LevelAccessor p_222034_, BlockPos p_222035_, BlockState p_222036_, @Nullable Collection<Direction> p_222037_, boolean p_222038_) {
        return regrowNether(p_222034_, p_222035_, p_222036_, p_222037_);
    }

    default boolean canChangeBlockStateOnSpread() {
        return true;
    }

    default int updateDecayDelay(int p_222045_) {
        return 1;
    }

    int attemptUseCharge(NetherSpeader.ChargeCursor p_222039_, LevelAccessor p_222040_, BlockPos p_222041_, RandomSource p_222042_, NetherSpeader p_222043_, boolean p_222044_);
}