/*
 *  This file is part of XGlow,
 *  licensed under the Apache License, Version 2.0.
 *
 *  Copyright (c) Xezard (Zotov Ivan)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package ru.xezard.glow;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import ru.xezard.glow.data.glow.manager.GlowsManager;
import ru.xezard.glow.data.glow.processor.GlowProcessor;
import ru.xezard.glow.listeners.EntityDeathListener;
import ru.xezard.glow.listeners.PlayerQuitListener;

public class GlowAPI {
    private final Plugin plugin;

    public GlowAPI(Plugin plugin) {
        this.plugin = plugin;

        PluginManager pluginManager = Bukkit.getPluginManager();

        if (pluginManager.getPlugin("ProtocolLib") == null) {
            plugin.getLogger().warning("[XGlow] No access to ProtocolLib! Is it installed?");
            plugin.getLogger().warning("[XGlow] Plugin has been disabled!");

            pluginManager.disablePlugin(plugin);
            return;
        }

        this.registerListeners(pluginManager);
        this.registerPacketListener();
    }

    private void registerListeners(PluginManager pluginManager) {
        pluginManager.registerEvents(new EntityDeathListener(), this.plugin);
        pluginManager.registerEvents(new PlayerQuitListener(), this.plugin);
    }

    private void registerPacketListener() {
        PacketAdapter adapter = new PacketAdapter(this.plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                Entity entity = packet.getEntityModifier(event).read(0);

                GlowsManager.getInstance().getGlowByEntity(entity).ifPresent((glow) -> {
                    final List<WrappedDataValue> wrappedDataValueList = new ArrayList<>();

                    GlowProcessor.getInstance()
                            .createDataWatcher(entity, glow.sees(event.getPlayer()))
                            .getWatchableObjects()
                            .stream()
                            .filter(Objects::nonNull)
                            .forEach(entry -> {
                                final WrappedDataWatcher.WrappedDataWatcherObject dataWatcherObject = entry.getWatcherObject();
                                wrappedDataValueList.add(new WrappedDataValue(dataWatcherObject.getIndex(), dataWatcherObject.getSerializer(), entry.getRawValue()));
                            });

                    packet.getDataValueCollectionModifier().write(0, wrappedDataValueList);
                    event.setPacket(packet);
                });
            }
        };

        ProtocolLibrary.getProtocolManager().addPacketListener(adapter);
    }
}