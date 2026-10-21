/**
 * Copyright (c) 2026 GregTech 6 NeoForge port, after GregTech6-Unofficial by the GT6U-Team
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

package gt6u;

import gregapi.GT_API;
import gregapi.api.Abstract_Mod;
import gregapi.api.Abstract_Proxy;
import gregapi.api.FMLInitializationEvent;
import gregapi.api.FMLPostInitializationEvent;
import gregapi.api.FMLPreInitializationEvent;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.code.ModData;
import gt6u.data.*;
import gregapi.data.MD;
import gt6u.compat.Compat_Recipes_IndustrialCraft;
import gt6u.items.MultiItemFood_Recipes;
import gt6u.items.MultiItemRandomTools_Recipes;
import gt6u.items.MultiItemTechnological_Recipes;
import gt6u.loaders.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import static gregapi.data.CS.*;

/** Content of GregTech6-Unofficial as an addon of GregTech 6, built on the GT API the way 1.7.10 addons were. */
@Mod(value = GT6U_Main.MOD_ID, depends = {ModIDs.GT})
public class GT6U_Main extends Abstract_Mod {
	public static final String MOD_ID = "gt6u", MOD_NAME = "GT6U Addon";
	public static final ModData MOD_DATA = new ModData(MOD_ID, MOD_NAME);
	public static final String REGISTRY = "gt6u.multitileentity";

	public GT6U_Main(IEventBus aModBus) {
		subscribePhases(aModBus);
		aModBus.addListener(this::onAddPackFinders);
	}
	
	/** GT6U's versions of GT6's own textures sit in a built-in pack above GT6's resources: as mod resources they would rank below
	 *  GregTech's, which loads after the addon. Position.TOP puts the pack last in the list, where it wins (as GT_API.onAddPackFinders). */
	public void onAddPackFinders(AddPackFindersEvent aEvent) {
		if (aEvent.getPackType() != PackType.CLIENT_RESOURCES) return;
		aEvent.addPackFinders(Identifier.fromNamespaceAndPath(MOD_ID, "gt6u_overrides"), PackType.CLIENT_RESOURCES, Component.translatable("pack.gt6u.overrides"), PackSource.BUILT_IN, T, Pack.Position.TOP);
	}

	@Override public String getModID() {return MOD_ID;}
	@Override public String getModName() {return MOD_NAME;}
	@Override public String getModNameForLog() {return "GT6U_Addon";}
	@Override public Abstract_Proxy getProxy() {return null;}

	// Before GregTech's PreInit, which generates items, fluids and recipes from the materials, prefixes and tags defined here.
	@Override public void onModPreInit2(FMLPreInitializationEvent aEvent) {
		// A class literal does not run static definitions; forName does, in this order: tags before the prefixes that name them.
		for (Class<?> tClass : new Class<?>[] {TD_GT6U.class, MT_GT6U.class, OP_GT6U.class, FL_GT6U.class, IL_GT6U.class, RM_GT6U.class, BI_GT6U.class}) {
			try {Class.forName(tClass.getName(), true, tClass.getClassLoader());} catch (ClassNotFoundException e) {throw new IllegalStateException(e);}
		}
		MT_GT6U.changeGT6();
		TD_GT6U.addToGT6();
		// Its own MultiTileEntity registry, built where GT6 builds its own: in the deferred block queue, which runs on RegisterEvent.
		// GT6U kept one registry: an id one of the two does not hold is the other's, for items, names and multiblock parts alike.
		GT_API.deferBlockInit(() -> new MultiTileEntityRegistry(REGISTRY).setFallback("gt.multitileentity"));
		new Compat_Recipes_IndustrialCraft(MD.IC2, this);
	}
	// After GregTech's PreInit, still before registration: GT6U's fluids, items and tool recipes sat in GT6's PreInit loaders.
	@Override public void onModFinalPreInit(FMLPreInitializationEvent aEvent) {
		Loader_Fluids.run();
		Loader_Items.run();
		Loader_Tools.run();
	}
	@Override public void onModInit2(FMLInitializationEvent aEvent) {/**/}
	// GregTech's Init loaders are queued by now, so GT6U's run after them and find GT6's entries and stone layers to change.
	// Each loader is its own queue element, as each had its own try in GT6U's Init list: one failing leaves the others.
	@Override public void onModFinalInit(FMLInitializationEvent aEvent) {
		GT_API.deferItemInit(() -> new Loader_MultiTileEntities().run());
		GT_API.deferItemInit(Loader_Worldgen::run);
	}
	@Override public void onModPostInit2(FMLPostInitializationEvent aEvent) {/**/}
	// GregTech's recipe loaders are queued by now; GT6U's run after them. What GT6U dropped is taken back first, so that a GT6U
	// recipe equal but for its duration to a dropped GT6 one is not taken back with it. One queue element per edit, same order.
	@Override public void onModFinalPostInit(FMLPostInitializationEvent aEvent) {
		for (Runnable[] tEdit : EDITS) GT_API.deferItemInit(tEdit[0]);
		for (Runnable[] tEdit : EDITS) GT_API.deferItemInit(tEdit[1]);
	}

	/** GT6U's edits of GT6's recipe-making code, one per GT6 class: what GT6U dropped from it, then what GT6U added to it. */
	private static final Runnable[][] EDITS = {
		{Loader_Blocks                 ::takeBack, Loader_Blocks                 ::run},
		{Loader_Fuels                  ::takeBack, Loader_Fuels                  ::run},
		{Loader_Recipes_Chem           ::takeBack, Loader_Recipes_Chem           ::run},
		{Loader_Recipes_Food           ::takeBack, Loader_Recipes_Food           ::run},
		{Loader_Recipes_Other          ::takeBack, Loader_Recipes_Other          ::run},
		{Loader_Recipes_Handlers       ::takeBack, Loader_Recipes_Handlers       ::run},
		{Loader_Recipes_Vanilla_OreDict::takeBack, Loader_Recipes_Vanilla_OreDict::run},
		{MultiItemTechnological_Recipes::takeBack, MultiItemTechnological_Recipes::run},
		{MultiItemRandomTools_Recipes  ::takeBack, MultiItemRandomTools_Recipes  ::run},
		{MultiItemFood_Recipes         ::takeBack, MultiItemFood_Recipes         ::run},
	};
	@Override public void onModServerStarting2(ServerStartingEvent aEvent) {/**/}
	@Override public void onModServerStarted2(ServerStartedEvent aEvent) {/**/}
	@Override public void onModServerStopping2(ServerStoppingEvent aEvent) {/**/}
	@Override public void onModServerStopped2(ServerStoppedEvent aEvent) {/**/}
}
