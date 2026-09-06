/*
 * PlayMoreSounds - A bukkit plugin that manages and plays sounds.
 * Copyright (C) 2022 Christiano Rangel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.epicnicity322.playmoresounds.bukkit.listeners;

import com.epicnicity322.playmoresounds.bukkit.PlayMoreSounds;
import com.epicnicity322.playmoresounds.bukkit.sound.PlayableRichSound;
import com.epicnicity322.playmoresounds.core.config.Configurations;
import com.epicnicity322.yamlhandler.ConfigurationSection;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class OnAsyncPlayerChat extends PMSListener {
    private static final boolean IS_PAPER;
    static {
        boolean paper = false;
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            paper = true;
        } catch (Throwable ignored) {
        }
        IS_PAPER = paper;
    }

    private final @NotNull HashMap<String, HashSet<PlayableRichSound>> filtersAndCriteria = new HashMap<>();

    public OnAsyncPlayerChat(@NotNull PlayMoreSounds plugin) {
        super(plugin);
    }

    static boolean matchesFilter(String filter, String criteria, String message) {
        return switch (filter) {
            case "Starts With" -> message.startsWith(criteria);
            case "Ends With" -> message.endsWith(criteria);
            case "Contains SubString" -> message.toLowerCase().contains(criteria.toLowerCase());
            case "Contains" -> message.toLowerCase().matches(".*\\b" + Pattern.quote(criteria.toLowerCase()) + "\\b.*");
            case "Equals Ignore Case" -> message.equalsIgnoreCase(criteria);
            case "Equals Exactly" -> message.equals(criteria);
            default -> false;
        };
    }

    @Override
    public @NotNull String getName() {
        return "Player Chat";
    }

    @Override
    public void load() {
        filtersAndCriteria.clear();

        var sounds = Configurations.SOUNDS.getConfigurationHolder().getConfiguration();
        var chatTriggers = Configurations.CHAT_SOUNDS.getConfigurationHolder().getConfiguration();

        for (Map.Entry<String, Object> filter : chatTriggers.getNodes().entrySet()) {
            if (filter.getValue() instanceof ConfigurationSection filterSection) {
                HashSet<PlayableRichSound> criteria = new HashSet<>();

                for (Map.Entry<String, Object> criterion : filterSection.getNodes().entrySet()) {
                    if (criterion.getValue() instanceof ConfigurationSection criterionSection) {

                        if (criterionSection.getBoolean("Enabled").orElse(false)) {
                            criteria.add(getRichSound(criterionSection));
                        }
                    }
                }

                if (!criteria.isEmpty()) filtersAndCriteria.put(filter.getKey(), criteria);
            }
        }

        setRichSound(getRichSound(sounds.getConfigurationSection(getName())));

        if (getRichSound() != null || !filtersAndCriteria.isEmpty()) {
            if (!isLoaded()) {
                Bukkit.getPluginManager().registerEvents(this, plugin);
                setLoaded(true);
            }
        } else {
            if (isLoaded()) {
                HandlerList.unregisterAll(this);
                setLoaded(false);
            }
        }
    }

    private void processChatSounds(@NotNull Player player, @NotNull String message, @NotNull Collection<Player> recipients, boolean isCancelled) {
        if (isCancelled) return;

        boolean defaultSound = getRichSound() != null;

        filterLoop:
        for (Map.Entry<String, HashSet<PlayableRichSound>> filter : filtersAndCriteria.entrySet()) {
            for (var criteria : filter.getValue()) {
                ConfigurationSection criteriaSection = criteria.getSection();

                if (!isCancelled || !criteria.isCancellable()) {
                    if (matchesFilter(filter.getKey(), criteriaSection.getName(), message)) {
                        Bukkit.getScheduler().runTask(plugin, () -> criteria.play(player, recipients));

                        if (criteriaSection.getBoolean("Prevent Other Sounds.Default Sound").orElse(false))
                            defaultSound = false;

                        if (criteriaSection.getBoolean("Prevent Other Sounds.Other Filters").orElse(false))
                            break filterLoop;
                    }
                }
            }
        }

        if (defaultSound && (!isCancelled || !getRichSound().isCancellable()))
            Bukkit.getScheduler().runTask(plugin, () -> getRichSound().play(player, recipients));
    }

    // 1. Paper Native AsyncChatEvent (Paper 1.21+)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPaperAsyncChat(AsyncChatEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        Set<Player> recipients = new HashSet<>();
        for (Audience audience : event.viewers()) {
            if (audience instanceof Player p) {
                recipients.add(p);
            }
        }

        processChatSounds(player, message, recipients, event.isCancelled());
    }

    // 2. Legacy Spigot AsyncPlayerChatEvent (Fallback for non-Paper servers)
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (IS_PAPER) {
            // Already handled natively by onPaperAsyncChat
            return;
        }

        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        String message = event.getMessage();
        Set<Player> recipients = new HashSet<>(event.getRecipients());

        processChatSounds(player, message, recipients, event.isCancelled());
    }
}