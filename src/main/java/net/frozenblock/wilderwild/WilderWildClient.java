/*
 * Copyright 2023-2025 FrozenBlock
 * This file is part of Wilder Wild.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package net.frozenblock.wilderwild;

import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.frozenblock.lib.config.frozenlib_config.FrozenLibConfig;
import net.frozenblock.lib.debug.client.api.DebugRendererEvents;
import net.frozenblock.lib.debug.client.impl.DebugRenderManager;
import net.frozenblock.lib.menu.api.SplashTextAPI;
import net.frozenblock.lib.music.api.client.pitch.MusicPitchApi;
import net.frozenblock.lib.music.api.client.structure.StructureMusic;
import net.frozenblock.lib.music.api.client.structure.StructureMusicApi;
import net.frozenblock.wilderwild.client.WWBlockRenderLayers;
import net.frozenblock.wilderwild.client.WWFluidRendering;
import net.frozenblock.wilderwild.client.WWItemProperties;
import net.frozenblock.wilderwild.client.WWModelLayers;
import net.frozenblock.wilderwild.client.WWParticleEngine;
import net.frozenblock.wilderwild.client.WWTints;
import net.frozenblock.wilderwild.client.WilderEasterEggs;
import net.frozenblock.wilderwild.client.renderer.debug.OstrichDebugRenderer;
import net.frozenblock.wilderwild.config.WWAmbienceAndMiscConfig;
import net.frozenblock.wilderwild.networking.WWClientNetworking;
import net.frozenblock.wilderwild.registry.WWBiomes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public final class WilderWildClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer("wilderwild");

		SplashTextAPI.addSplashLocation(WWConstants.id("texts/splashes.txt"));
		WilderEasterEggs.hatchEasterEggs();

		WWBlockRenderLayers.init();
		WWFluidRendering.init();
		WWItemProperties.init();
		WWModelLayers.init();
		WWParticleEngine.init();
		WWTints.initBlocks();
		WWTints.initItems();

		WWClientNetworking.registerPacketReceivers();

		Function<Long, Float> dyingPitchShifting = (l) -> WWAmbienceAndMiscConfig.Client.DISTORTED_DYING_FOREST_MUSIC ?
			(0.98F + Mth.sin((float) ((l * Math.PI) / 1200F)) * 0.025F) : 1F;
		MusicPitchApi.registerForBiome(WWBiomes.DYING_FOREST.location(), dyingPitchShifting);
		MusicPitchApi.registerForBiome(WWBiomes.DYING_MIXED_FOREST.location(), dyingPitchShifting);
		MusicPitchApi.registerForBiome(WWBiomes.SNOWY_DYING_FOREST.location(), dyingPitchShifting);
		MusicPitchApi.registerForBiome(WWBiomes.SNOWY_DYING_MIXED_FOREST.location(), dyingPitchShifting);

		if (WWAmbienceAndMiscConfig.get().music.ancientCityMusic) {
			StructureMusicApi.registerMusicForStructure(
				BuiltinStructures.ANCIENT_CITY,
				new StructureMusic(
					new Music(SoundEvents.MUSIC_BIOME_DEEP_DARK, 6000, 12000, false),
					false
				)
			);
		}

		if (WWAmbienceAndMiscConfig.get().music.wilderExtraMusic) {
			ResourceManagerHelper.registerBuiltinResourcePack(
				WWConstants.id("wilder_extra_music"), modContainer.get(),
				Component.translatable("pack.wilderwild.wilder_extra_music"),
				ResourcePackActivationType.ALWAYS_ENABLED
			);
		}

		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return WWConstants.id("minecraft_live_sculk_sensor");
			}

			@Override
			public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
				WWConstants.MC_LIVE_TENDRILS = resourceManager.getResource(WWConstants.id("textures/entity/sculk_sensor/new_tendril_enabler.png")).isPresent();
			}
		});

		DebugRendererEvents.DEBUG_RENDERERS_CREATED.register(client -> {
			OstrichDebugRenderer ostrichDebugRenderer = new OstrichDebugRenderer(client);

			ClientTickEvents.START_WORLD_TICK.register(clientLevel -> {
				if (FrozenLibConfig.IS_DEBUG) {
					ostrichDebugRenderer.tick();
				}
			});

			DebugRenderManager.addClearRunnable(ostrichDebugRenderer::clear);

			DebugRenderManager.registerRenderer(WWConstants.id("ostrich"), ostrichDebugRenderer::render);
		});
	}

}
