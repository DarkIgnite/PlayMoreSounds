# 🎵 PlayMoreSounds (Forked & Fixed for Paper 1.21+)

Fork of [chrisrnj/PlayMoreSounds](https://github.com/chrisrnj/PlayMoreSounds) maintained by **DarkIgnite** / **Leftycraft**.

---

## 🔧 Changes & Fixes in this Fork

1. **Per-Player Chat Recipient Filtering (ChatControl Integration)**
   - **Problem in Original:** `OnAsyncPlayerChat` in original PlayMoreSounds calculated listeners by querying all players within a radius / entire server, completely ignoring `event.getRecipients()`. Even when a player disabled chat or was removed from the chat recipients list by plugins like `ChatControl`, PlayMoreSounds still played the sound to that player.
   - **Fix:** `OnAsyncPlayerChat`, `PlayableSound`, and `PlayableRichSound` now accept and filter by `event.getRecipients()`. Players who turned off their chat (`/chat off`) will **NEVER** hear chat notification sounds from other players or themselves.
   - **Scoped Strictly to Chat:** All other gameplay sounds (hotbar slot change, flying on/off, player join/quit, teleport, etc.) remain 100% active and are never affected by `/chat off`.

2. **Native Paper 1.21 `AsyncChatEvent` Support**
   - **Problem in Original:** PlayMoreSounds previously only listened to legacy Spigot `AsyncPlayerChatEvent`.
   - **Fix:** Added native listener for Paper 1.21 `io.papermc.paper.event.player.AsyncChatEvent` to respect Paper's Adventure audience viewers and modern chat pipeline.

3. **Strict Event Cancellation (Server Mute Fix)**
   - **Problem in Original:** If sound criteria had `Cancellable: false`, PlayMoreSounds would still play chat sounds even when the chat event was cancelled by staff or server mute commands.
   - **Fix:** Added strict checks on `event.isCancelled()` in `OnAsyncPlayerChat` so that when global mute / muteall is active, zero chat sounds are played.

4. **Thread-Safe & Exception-Safe Listener Iteration**
   - **Fix:** Safely iterates over listener copies in `PlayableSound.java` to allow other plugins to dynamically remove listeners in `PlaySoundEvent` without throwing `ConcurrentModificationException`.

5. **Updated PaperMC Repositories & Compatibility**
   - **Fix:** Updated obsolete repository URLs in `pom.xml` to `https://repo.papermc.io/repository/maven-public/`.

---

## 🔨 Build Instructions

```bash
git clone https://github.com/DarkIgnite/PlayMoreSounds.git
cd PlayMoreSounds
mvn clean package -DskipTests
```
The compiled jar will be at `target/PlayMoreSounds.jar`.