package supercoder79.databreaker.mixin.fix_log_spam;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.RegistryLookupCodec;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.SaveVersionInfo;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;
import java.util.function.BiFunction;

@Mixin(LevelStorage.class)
public abstract class MixinLevelStorageClient {
    @Shadow protected abstract int getCurrentVersion();

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private static ImmutableList<String> GENERATOR_OPTION_KEYS;

    /**
     * @reason Avoid datafixing and return the provided dynamic
     * @author SuperCoder79
     */
    @Environment(EnvType.CLIENT)
    @Overwrite
    public BiFunction<File, DataFixer, LevelSummary> createLevelDataParser(File file, boolean bl) {
        return (file2, dataFixer) -> {
            try {
                NbtCompound compoundTag = NbtIo.readCompressed(new FileInputStream(file2));
                NbtCompound compoundTag2 = compoundTag.getCompound("Data");
                compoundTag2.remove("Player");
                int i = compoundTag2.contains("DataVersion", 99) ? compoundTag2.getInt("DataVersion") : -1;
                Dynamic<NbtElement> dynamic = new Dynamic<>(NbtOps.INSTANCE, compoundTag2);
                SaveVersionInfo saveVersionInfo = SaveVersionInfo.fromDynamic(dynamic);
                int j = saveVersionInfo.getLevelFormatVersion();
                if (j != 19132 && j != 19133) {
                    return null;
                } else {
                    boolean bl2 = j != this.getCurrentVersion();
                    File file3 = new File(file, "icon.png");
                    DataPackSettings dataPackSettings = dynamic.get("DataPacks").result().map(d -> method_29580(d)).orElse(DataPackSettings.SAFE_MODE);
                    LevelInfo levelInfo = LevelInfo.fromDynamic(dynamic, dataPackSettings);
                    return new LevelSummary(levelInfo, saveVersionInfo, file.getName(), bl2, bl, file3);
                }
            } catch (Exception var15) {
                return null;
            }
        };
    }

    /**
     * @reason Avoid datafixing and return the provided dynamic
     * @author SuperCoder79
     */
    @Overwrite
    private static Pair<GeneratorOptions, Lifecycle> readGeneratorProperties(Dynamic<?> dynamic, DataFixer dataFixer, int i) {
        Dynamic<?> dynamic2 = dynamic.get("WorldGenSettings").orElseEmptyMap();
        UnmodifiableIterator var4 = GENERATOR_OPTION_KEYS.iterator();

        while(var4.hasNext()) {
            String string = (String)var4.next();
            Optional<? extends Dynamic<?>> optional = dynamic.get(string).result();
            if (optional.isPresent()) {
                dynamic2 = dynamic2.set(string, (Dynamic)optional.get());
            }
        }

        Dynamic<?> dynamic3 = dynamic2;
        DataResult<GeneratorOptions> dataResult = GeneratorOptions.CODEC.parse(dynamic3);
        final Logger var10002 = LOGGER;
        return Pair.of(dataResult.resultOrPartial(Util.addPrefix("WorldGenSettings: ", var10002::error)).orElseGet(() -> GeneratorOptions.getDefaultOptions(DynamicRegistryManager.Impl.method_39199(dynamic3))), dataResult.lifecycle());
    }

    private static DataPackSettings method_29580(Dynamic<?> dynamic) {
        DataResult var10000 = DataPackSettings.CODEC.parse(dynamic);
        Logger var10001 = LOGGER;
        var10001.getClass();
        return (DataPackSettings)var10000.resultOrPartial(var10001::error).orElse(DataPackSettings.SAFE_MODE);
    }
}
