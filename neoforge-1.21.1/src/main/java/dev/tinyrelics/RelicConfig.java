package dev.tinyrelics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Mod settings. The server keeps the real copy in config/tinyrelics.json and
 * sends it to every client so the settings menu can show it.
 */
public final class RelicConfig {
    public static final int MIN_RADIUS = 8;
    public static final int MAX_RADIUS = 128;
    public static final int MIN_KILLS = 5;
    public static final int MAX_KILLS = 100;

    /** The copy the game logic reads. */
    public static RelicConfig SERVER = new RelicConfig();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file;

    /** How far (in blocks, in every direction) Thor's Hammer strikes. */
    public int hammerRadius = 32;
    /** Whether hammer lightning sets the ground on fire. */
    public boolean hammerFire = false;
    /** true = the hammer only hits hostile mobs, false = it hits everyone. */
    public boolean hammerMonstersOnly = false;
    public boolean charmCraft = true;
    public boolean boomerangCraft = true;
    public boolean bladeCraft = true;
    public boolean staffCraft = true;
    /** Mob kills needed to charge the Undead Staff's soul fire. */
    public int staffKills = 20;
    /** Whether the staff's wither skulls break blocks. */
    public boolean staffSkullGrief = false;

    public RelicConfig copy() {
        RelicConfig c = new RelicConfig();
        c.hammerRadius = this.hammerRadius;
        c.hammerFire = this.hammerFire;
        c.hammerMonstersOnly = this.hammerMonstersOnly;
        c.charmCraft = this.charmCraft;
        c.boomerangCraft = this.boomerangCraft;
        c.bladeCraft = this.bladeCraft;
        c.staffCraft = this.staffCraft;
        c.staffKills = this.staffKills;
        c.staffSkullGrief = this.staffSkullGrief;
        return c;
    }

    public void clamp() {
        this.hammerRadius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, this.hammerRadius));
        this.staffKills = Math.max(MIN_KILLS, Math.min(MAX_KILLS, this.staffKills));
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.hammerRadius);
        buffer.writeBoolean(this.hammerFire);
        buffer.writeBoolean(this.hammerMonstersOnly);
        buffer.writeBoolean(this.charmCraft);
        buffer.writeBoolean(this.boomerangCraft);
        buffer.writeBoolean(this.bladeCraft);
        buffer.writeBoolean(this.staffCraft);
        buffer.writeVarInt(this.staffKills);
        buffer.writeBoolean(this.staffSkullGrief);
    }

    public static RelicConfig read(FriendlyByteBuf buffer) {
        RelicConfig c = new RelicConfig();
        c.hammerRadius = buffer.readVarInt();
        c.hammerFire = buffer.readBoolean();
        c.hammerMonstersOnly = buffer.readBoolean();
        c.charmCraft = buffer.readBoolean();
        c.boomerangCraft = buffer.readBoolean();
        c.bladeCraft = buffer.readBoolean();
        c.staffCraft = buffer.readBoolean();
        c.staffKills = buffer.readVarInt();
        c.staffSkullGrief = buffer.readBoolean();
        c.clamp();
        return c;
    }

    public static void load(Path path) {
        file = path;
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                RelicConfig loaded = GSON.fromJson(reader, RelicConfig.class);
                if (loaded != null) {
                    loaded.clamp();
                    SERVER = loaded;
                }
            } catch (IOException | RuntimeException e) {
                // A broken file must not stop the game: fall back to defaults.
                SERVER = new RelicConfig();
            }
        }
        save();
    }

    public static void save() {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(SERVER, writer);
            }
        } catch (IOException e) {
            // Settings still apply until the next restart.
        }
    }
}
