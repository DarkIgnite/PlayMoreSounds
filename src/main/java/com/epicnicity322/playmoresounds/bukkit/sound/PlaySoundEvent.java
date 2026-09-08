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

package com.epicnicity322.playmoresounds.bukkit.sound;

import com.epicnicity322.playmoresounds.core.sound.SoundOptions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This event is called before a sound is played.
 */
public class PlaySoundEvent extends Event implements Cancellable {
    private static final @NotNull HandlerList handlers = new HandlerList();
    private final @NotNull PlayableSound sound;
    private final @Nullable Player sourcePlayer;
    private final @NotNull Collection<Player> listeners;
    private final @NotNull Collection<Player> unmodifiableListeners;
    @NotNull Location location;
    private boolean cancelled;
    private boolean global;

    public PlaySoundEvent(@NotNull PlayableSound sound, @Nullable Player sourcePlayer, @NotNull Location location,
                          @NotNull Collection<Player> listeners, boolean global) {
        this.sound = sound;
        this.sourcePlayer = sourcePlayer;
        this.location = location;
        this.listeners = listeners;
        this.unmodifiableListeners = Collections.unmodifiableCollection(listeners);
        this.global = global;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public @NotNull PlayableSound getSound() {
        return sound;
    }

    public @Nullable Player getSourcePlayer() {
        return sourcePlayer;
    }

    public final @NotNull Location getLocation() {
        return location.clone();
    }

    public void setLocation(@NotNull Location location) {
        if (!Objects.equals(this.location.getWorld(), location.getWorld()))
            throw new IllegalArgumentException("New location world is not the same as previous location's world.");

        this.location = location;
    }

    public @NotNull Collection<Player> getListeners() {
        return unmodifiableListeners;
    }

    public void addListener(@NotNull Player listener) {
        if (!validateListener(listener)) return;
        if (!location.getWorld().equals(listener.getWorld())) {
            global = true;
        }

        listeners.remove(listener);
        listeners.add(listener);
    }

    public void removeListener(@NotNull Player listener) {
        listeners.remove(listener);
    }

    public @NotNull HashSet<Player> getValidListeners() {
        return listeners.stream().filter(this::validateListener).collect(Collectors.toCollection(HashSet::new));
    }

    public final boolean playingGlobally() {
        return global;
    }

    public static boolean isChatDisabledInChatControl(@NotNull UUID uuid) {
        try {
            org.bukkit.plugin.Plugin ccPlugin = Bukkit.getPluginManager().getPlugin("ChatControl");
            if (ccPlugin != null && ccPlugin.isEnabled()) {
                Method getDataManager = ccPlugin.getClass().getMethod("getDataManager");
                Object dataManager = getDataManager.invoke(ccPlugin);
                if (dataManager != null) {
                    Method isChatDisabled = dataManager.getClass().getMethod("isChatDisabled", UUID.class);
                    return (boolean) isChatDisabled.invoke(dataManager, uuid);
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public boolean validateListener(@NotNull Player listener) {
        SoundOptions options = getSound().getOptions();

        return (options.ignoresDisabled() || SoundManager.getSoundsState(listener))
                && (options.getPermissionToListen() == null || listener.hasPermission(options.getPermissionToListen()))
                && (sourcePlayer == null || listener.canSee(sourcePlayer));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaySoundEvent that)) return false;

        return cancelled == that.cancelled &&
                global == that.global &&
                sound.equals(that.sound) &&
                Objects.equals(sourcePlayer, that.sourcePlayer) &&
                location.equals(that.location) &&
                listeners.equals(that.listeners);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cancelled, sound, sourcePlayer, location, listeners, global);
    }
}