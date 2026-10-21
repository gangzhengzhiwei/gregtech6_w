/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregapi.block.misc;

import gregapi.block.BlockBaseMeta;
import gregapi.block.IBlockExtendedMetaData;
import gregapi.data.CS.BlocksGT;
import gregapi.util.WD;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public final class BlockPottedFlower extends FlowerPotBlock implements IBlockExtendedMetaData {
	public BlockPottedFlower(String aNameInternal, BlockBaseFlower aPlant) {
		// The supplier constructor keeps vanilla's block-only insertion map from discarding GT6 metadata.
		super(() -> (FlowerPotBlock)Blocks.FLOWER_POT, () -> aPlant, BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWER_POT).overrideDescription(Blocks.FLOWER_POT.getDescriptionId()).setId(net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(gregapi.GT_API.owner(), gregapi.GT_API.sanitizeRegName(aNameInternal)))));
		registerDefaultState(getStateDefinition().any().setValue(BlockBaseMeta.META, 0));
		BlocksGT.registerPotted(aPlant, this);
	}

	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		super.createBlockStateDefinition(aBuilder);
		aBuilder.add(BlockBaseMeta.META);
	}

	@Override protected List<ItemStack> getDrops(BlockState aState, LootParams.Builder aParams) {
		if (WD.explosionDropDenied(aParams)) return List.of();
		ItemStack tPlant = BlocksGT.pottedContent(aState);
		return tPlant.isEmpty() ? List.of(new ItemStack(Items.FLOWER_POT)) : List.of(new ItemStack(Items.FLOWER_POT), tPlant);
	}

	@Override protected ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader aWorld, BlockPos aPos, BlockState aState, boolean aIncludeData) {
		return BlocksGT.pottedContent(aState);
	}

	@Override protected InteractionResult useItemOn(ItemStack aStack, BlockState aState, Level aWorld, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		if (aStack.getItem() instanceof BlockItem tItem && BlocksGT.potted(tItem.getBlock(), 0) != null) return InteractionResult.CONSUME;
		return super.useItemOn(aStack, aState, aWorld, aPos, aPlayer, aHand, aHit);
	}

	@Override protected InteractionResult useWithoutItem(BlockState aState, Level aWorld, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		ItemStack tPlant = BlocksGT.pottedContent(aState);
		if (!WD.set(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), getEmptyPot().defaultBlockState(), 3)) return InteractionResult.FAIL;
		if (!tPlant.isEmpty() && !aPlayer.addItem(tPlant)) aPlayer.drop(tPlant, false);
		aWorld.gameEvent(aPlayer, GameEvent.BLOCK_CHANGE, aPos);
		return InteractionResult.SUCCESS;
	}
}
