package material_replacer.zz_404;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ReplaceMaterialCommand {
    private static final Dynamic2CommandExceptionType TOO_BIG_EXCEPTION
            = new Dynamic2CommandExceptionType((maxCount, count) ->
            Text.stringifiedTranslatable("commands.fill.toobig", maxCount, count));
    private static final SimpleCommandExceptionType FAILED_EXCEPTION
            = new SimpleCommandExceptionType(Text.translatable("commands.fill.failed"));

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher,
                 registryAccess,
                 environment) -> dispatcher.register(literal("rem")
                        .then(argument("pos1", BlockPosArgumentType.blockPos())
                                .then(argument("pos2", BlockPosArgumentType.blockPos())
                                        .then(argument("from", StringArgumentType.string())
                                                .then(argument("to", StringArgumentType.string())
                                                        .executes(context -> {
                                                            var pos1 = BlockPosArgumentType.getBlockPos(context, "pos1");
                                                            var pos2 = BlockPosArgumentType.getBlockPos(context, "pos2");
                                                            var from = StringArgumentType.getString(context, "from");
                                                            var to = StringArgumentType.getString(context, "to");
                                                            return replaceMaterial(context.getSource(), BlockBox.create(pos1, pos2), from, to);
                                                        })
                                                )
                                        )
                                )
                        )
                )
        );
    }

    public static int replaceMaterial(@NotNull ServerCommandSource source, @NotNull BlockBox box,
                                      String from, String to) throws CommandSyntaxException {
        ServerWorld world = source.getWorld();

        int i = box.getBlockCountX() * box.getBlockCountY() * box.getBlockCountZ();
        int j = world.getGameRules().getInt(GameRules.COMMAND_MODIFICATION_BLOCK_LIMIT);
        if (i > j) throw TOO_BIG_EXCEPTION.create(j, i);

        int k = 0;

        for (BlockPos pos : iterBox(box)) {
            Block fromBlock = world.getBlockState(pos).getBlock();
            String fromIdName = Registries.BLOCK.getId(fromBlock).toString();
            String toIdName = fromIdName.replaceAll(from, to);
            Identifier toId = Identifier.of(toIdName);
            if (fromIdName.equals(toIdName) || !Registries.BLOCK.containsId(toId)) continue;
            Block toBlock = Registries.BLOCK.get(toId);
            replaceBlock(world, pos, toBlock);
            k++;
        }

        if (k == 0) throw FAILED_EXCEPTION.create();
        else {
            Text text = Text.translatable("commands.fill.success", k);
            source.sendFeedback(() -> text, true);
            return k;
        }
    }

    @SuppressWarnings("unchecked rawtypes")
    public static void replaceBlock(@NotNull ServerWorld world, BlockPos pos, @NotNull Block block) {
        BlockState before = world.getBlockState(pos);
        BlockState after = block.getDefaultState();
        for (Property property : before.getProperties())
            if (after.getProperties().contains(property))
                after = after.with(property, before.get(property));
        world.setBlockState(pos, after);
    }

    @Contract("_ -> !null")
    public static @NotNull Iterable<BlockPos> iterBox(@NotNull BlockBox box) {
        return BlockPos.iterate(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }
}
