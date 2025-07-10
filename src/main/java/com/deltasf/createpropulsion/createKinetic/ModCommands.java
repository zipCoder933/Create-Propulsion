package com.deltasf.createpropulsion.createKinetic;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.commands.Commands.argument;

/**
 * Iteration over all ships
 * TODO: If ship number exceeds 5000, we can use  shipObjectWorld.getAllShips().getByChunkPos(x,y,dimension) to get 10x10 chunk area around the player
 * EVERY ship must have a unique slug, and so we really do have to iterate over EVERY one to prevent duplicates.
 * VS2 probably assumes you wont have over 5000 ships in a minecraft world anyway
 */
//https://github.com/ValkyrienSkies/Valkyrien-Skies-2/blob/42e49defd5f398f1b1e1a952d56a0a4407373e31/common/src/main/kotlin/org/valkyrienskies/mod/common/command/VSCommands.kt#L40
//https://github.com/ValkyrienSkies/Valkyrien-Skies-2/blob/25db12ab7eff4d2813b7d1d8b0553e6e7f2e0fc3/common/src/main/kotlin/org/valkyrienskies/mod/common/command/VSCommands.kt#L263
@Mod.EventBusSubscriber
public class ModCommands {

    final static int DELETE_MASSLESS_THRESHOLD = 99;
    final static int PROXIMITY_RADIUS = 20;

    public static int executeParsedCommandOP(CommandSourceStack originalSource, String command, boolean redirectOutput) {
        MinecraftServer server = originalSource.getServer();
        var dispatcher = server.getCommands().getDispatcher();

        // Remove leading slash
        if (command.startsWith("/")) {
            command = command.substring(1);
        }


        try {
            CommandSourceStack serverSource;

            if (redirectOutput) {
                serverSource = new CommandSourceStack(
                        originalSource.getPlayer(), // entity
                        originalSource.getPlayer().position(), // position
                        originalSource.getPlayer().getRotationVector(), // rotation

                        server.getLevel(originalSource.getPlayer().level().dimension()).getServer()
                                .getLevel(originalSource.getPlayer().level().dimension()), // server level access

                        4, // permission level (OP)
                        originalSource.getPlayer().getName().getString(), // name
                        originalSource.getPlayer().getDisplayName(), // display name
                        server, // server
                        originalSource.getPlayer() // entity again
                ).withPermission(4)
                        .withSuppressedOutput(); // ensure messages show
            } else {
                serverSource = server.createCommandSourceStack()
                        .withPermission(4) // Full OP level
                        .withSuppressedOutput();
            }

            ParseResults<CommandSourceStack> parseResults = dispatcher.parse(command, serverSource);
            return dispatcher.execute(parseResults);
        } catch (CommandSyntaxException e) {
            originalSource.sendFailure(Component.literal("Error executing command: " + e.getMessage()));
            return 0;
        } catch (Exception e) {
            originalSource.sendFailure(Component.literal("Error executing command: " + e.getMessage()));
            return 0;
        }
    }

    public static int executeParsedCommand(CommandSourceStack source, String command) {
        // Use the server's command dispatcher
        MinecraftServer server = source.getServer();
        var dispatcher = server.getCommands().getDispatcher();

        // Parse the command string (if a leading slash exists, remove it)
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        ParseResults<CommandSourceStack> parseResults = dispatcher.parse(command, source);
        try {
            // Execute the parsed command and return the result
            return dispatcher.execute(parseResults);
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("Error executing command: " + e.getMessage()));
            return 0;
        }
    }


    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        /**
         * OPERATOR COMMANDS
         */
        dispatcher.register(Commands.literal("vs")
                .requires(source -> source.hasPermission(2)) // Only players with permission level 2 or higher see this command
                .then(Commands.literal("deletemassless").requires(source -> source.hasPermission(2)).executes(context -> {
                    vsDeleteMasslessShips(context);
                    return Command.SINGLE_SUCCESS;
                })).then(Commands.literal("total").requires(source -> source.hasPermission(2)).executes(context -> {
                    vsCountShips(context);
                    return Command.SINGLE_SUCCESS;
                })));

        /**
         * NON-OPERATOR COMMANDS
         */


        dispatcher.register(Commands.literal("ship")
                .then(Commands.literal("totem")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("name", StringArgumentType.word())
                                .suggests(shipSlugSuggestions())//ShipArgument.Companion.ships()
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ItemStack item = player.getMainHandItem(); // or getOffhandItem()

                                    // Check if the item is a Totem
                                    if (item.getItem() != CreateKineticMod.SHIP_TOTEM.get() &&
                                            item.getItem() != CreateKineticMod.FREEZE_SHIP_TOTEM.get()) {
                                        ctx.getSource().sendFailure(Component.literal("You must be holding a Ship Totem!"));
                                        return 0;
                                    }

                                    String newName = StringArgumentType.getString(ctx, "name");

                                    //Ensure the name actually exists
                                    ServerShipWorldCore shipObjectWorld = VSGameUtilsKt.getShipObjectWorld(ctx.getSource().getServer());
                                    boolean found = false;
                                    for (ServerShip ship : shipObjectWorld.getAllShips()) {
                                        if (ship != null && ship.getSlug() != null && newName.equals(ship.getSlug())) {
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        ctx.getSource().sendFailure(Component.literal("No ship with that name exists!"));
                                        return 0;
                                    }

                                    item.setHoverName(Component.literal(newName));

                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("Totem renamed to: " + newName), false);
                                    return 1;
                                })))

                .then(Commands.literal("this")
                        .executes(ctx -> {
                            return executeParsedCommandOP(ctx.getSource(), "vs get-ship",true);
                        })
                )
                .then(Commands.literal("rename")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("old", StringArgumentType.word())
                                .suggests(shipSlugSuggestions())//ShipArgument.Companion.ships()
                                .then(argument("new", StringArgumentType.word()) // /neutron rename <old> <new>
                                        .executes(ctx -> {
                                            CommandSourceStack originalSource2 = (CommandSourceStack) ctx.getSource();
                                            //Ship shipSlug = ShipArgument.Companion.getShip(ctx, "ship");
                                            String oldSlug = StringArgumentType.getString(ctx, "old");
                                            String newSlug = StringArgumentType.getString(ctx, "new");

                                            //Ensure the new name doesn't already exist
                                            ServerShipWorldCore shipObjectWorld = VSGameUtilsKt.getShipObjectWorld(ctx.getSource().getServer());
                                            for (ServerShip ship : shipObjectWorld.getAllShips()) {
                                                if (ship != null && ship.getSlug() != null && ship.getSlug().equalsIgnoreCase(newSlug)) {
                                                    ctx.getSource().sendSystemMessage(
                                                            Component.literal("Cannot rename to " + newSlug + " because it already exists."));
                                                    return 0;
                                                }
                                            }

                                            return executeParsedCommandOP(originalSource2, "vs ship " + oldSlug + " rename " + newSlug, true);
                                        })))
                )
                .then(Commands.literal("recover")
                        .requires(source -> source.hasPermission(2))
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("ship", StringArgumentType.word())
                                .suggests(shipSlugSuggestions())//ShipArgument.Companion.ships()
                                .executes(ctx -> {
                                    Entity sourceEntity = ctx.getSource().getPlayer();
                                    BlockHitResult rayTrace = (BlockHitResult) sourceEntity.pick(10, 1.0F, false);

                                    String shipSlug = StringArgumentType.getString(ctx, "ship");
                                    // Ship shipSlug = ShipArgument.Companion.getShip(ctx, "ship");
                                    return executeParsedCommandOP(ctx.getSource(), "vs teleport " + shipSlug + " "
                                            + rayTrace.getLocation().x + " " + rayTrace.getLocation().y + " " + rayTrace.getLocation().z, true);
                                })))
                .then(Commands.literal("freeze")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("ship", StringArgumentType.word())
                                .suggests(shipSlugSuggestions())//ShipArgument.Companion.ships()
                                .executes(ctx -> {
                                    String shipSlug = StringArgumentType.getString(ctx, "ship");
                                    return executeParsedCommandOP(ctx.getSource(), "vs set-static " + shipSlug + " true", true);
                                }))
                )
                .then(Commands.literal("unfreeze")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("ship", StringArgumentType.word())
                                .suggests(shipSlugSuggestions())//ShipArgument.Companion.ships()
                                .executes(ctx -> {
                                    String shipSlug = StringArgumentType.getString(ctx, "ship");
                                    return executeParsedCommandOP(ctx.getSource(), "vs set-static " + shipSlug + " false",true);
                                }))
                )
        );


    }

    private static SuggestionProvider<CommandSourceStack> shipSlugSuggestions() {
        return (CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) -> {
            List<String> suggestions = new ArrayList<>();
            try {
                ServerPlayer player = context.getSource().getPlayer();
//                VSCommandSource vsContext = (VSCommandSource) context;
//                QueryableShipData<LoadedShip> allShips = vsContext.getShipWorld().getLoadedShips();
                ServerShipWorldCore shipObjectWorld = VSGameUtilsKt.getShipObjectWorld(context.getSource().getServer());

                ResourceKey<Level> dimension = player.level().dimension();
                String playerDimension = dimension.location().toString();
                int chunkX = player.blockPosition().getX() >> 4;
                int chunkZ = player.blockPosition().getZ() >> 4;

                QueryableShipData<ServerShip> allShips = shipObjectWorld.getAllShips();
                for (ServerShip ship : allShips) {
                    //Only add suggestions from the same dimension
                    if (ship.getChunkClaimDimension().endsWith(playerDimension)) {
                        suggestions.add(ship.getSlug());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return SharedSuggestionProvider.suggest(suggestions, builder);
        };
    }


    private static void vsCountShips(CommandContext<CommandSourceStack> cc) {
        ServerShipWorldCore shipObjectWorld = VSGameUtilsKt.getShipObjectWorld(cc.getSource().getServer());
        cc.getSource().sendSystemMessage(Component.literal("There are " + shipObjectWorld.getAllShips().size() + " total ships in the world."));

        /**
         * Player information
         */
        ServerPlayer player = cc.getSource().getPlayer();
        ResourceKey<Level> dimension = player.level().dimension();
        String playerDimension = dimension.location().toString();
        int playerChunkX = player.blockPosition().getX() >> 4;
        int playerChunkZ = player.blockPosition().getZ() >> 4;
        System.out.println("Player Chunk X: " + playerChunkX + " Chunk Z: " + playerChunkZ);
        System.out.println("Player Dimension: " + playerDimension);

        /**
         * Individual ship counters
         */
        AtomicInteger masslessShips = new AtomicInteger(0);
        AtomicInteger shipsInThisDimension = new AtomicInteger(0);
        AtomicInteger shipsWithinProximity = new AtomicInteger(0);


        for (ServerShip ship : shipObjectWorld.getAllShips()) {
            System.out.println("Ship " + ship.getSlug() + " is in dimension " + ship.getChunkClaimDimension());
            if (ship.getInertiaData().getMass() < DELETE_MASSLESS_THRESHOLD) {
                masslessShips.incrementAndGet();
            }
            if (ship.getChunkClaimDimension().endsWith(playerDimension)) {//Ship chunk claim dimension looks like this  minecraft:dimension:minecraft:overworld
                shipsInThisDimension.incrementAndGet();
                if (Math.abs(ship.getChunkClaim().getXMiddle() - playerChunkX) < PROXIMITY_RADIUS
                        && Math.abs(ship.getChunkClaim().getZMiddle() - playerChunkZ) < PROXIMITY_RADIUS) {
                    shipsWithinProximity.incrementAndGet(); //shipsWithinProximity
                }
            }
        }
        cc.getSource().sendSystemMessage(Component.literal("(" + masslessShips.get() + " massless ships)"));
        cc.getSource().sendSystemMessage(Component.literal("(" + shipsInThisDimension.get() + " ships in this dimension)"));
//        cc.getSource().sendSystemMessage(Component.literal("(" + shipsWithinProximity.get() + " ships within " + PROXIMITY_RADIUS + " chunk proximity)"));
    }

//    int getThisShipCommand(CommandContext<CommandSourceStack> ctx) {
//        try {
//            VSCommandSource vsContext = (VSCommandSource) ctx;
//            ShipWorld shipWorld = vsContext.getShipWorld();
//
//
//            Entity sourceEntity = ctx.getSource().getPlayer();
////                                VSCommandSource vsContext = (VSCommandSource) ctx;
//            ServerShipWorldCore shipObjectWorld = VSGameUtilsKt.getShipObjectWorld(ctx.getSource().getServer());
//
//            if (sourceEntity != null) {
//                Ship pickedShip = getRaycastShip(shipObjectWorld, sourceEntity);
//                if (pickedShip != null) {
//                    ctx.getSource().sendSystemMessage(Component.literal("Found ship: " + pickedShip.getSlug()));
//                    return 1;
//                } else {
////                                  ((VSCommandSource) ctx.getSource()).sendVSMessage( new TranslatableComponent(GET_SHIP_FAIL_MESSAGE));
//                    ctx.getSource().sendSystemMessage(Component.literal("No ship found."));
//                    return 0;
//                }
//            } else {
////                              ((VSCommandSource) ctx.getSource()).sendVSMessage( new TranslatableComponent(GET_SHIP_ONLY_USABLE_BY_ENTITIES_MESSAGE));
//                ctx.getSource().sendSystemMessage(Component.literal("This command can only be used by entities."));
//                return 0;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//
//    }
//
//    private static Ship getRaycastShip(ServerShipWorldCore vsContext, Entity sourceEntity) {
//        final int RAYTRACE_RANGE = 10;
//        BlockHitResult rayTrace = (BlockHitResult) sourceEntity.pick(RAYTRACE_RANGE, 1.0F, false);
//
//        Vec3 eyePos = sourceEntity.getEyePosition(1.0F); // Origin (ox, oy, oz)
//        Vec3 viewVec = sourceEntity.getViewVector(1.0F); // Direction (dx, dy, dz)
//
//        float ox = (float) eyePos.x;
//        float oy = (float) eyePos.y;
//        float oz = (float) eyePos.z;
//
//        float dx = (float) viewVec.x;
//        float dy = (float) viewVec.y;
//        float dz = (float) viewVec.z;
//
//        float hitX = (float) rayTrace.getLocation().x;
//        float hitY = (float) rayTrace.getLocation().y;
//        float hitZ = (float) rayTrace.getLocation().z;
//
//        System.out.println("Raytrace hit: " + hitX + ", " + hitY + ", " + hitZ);
//        System.out.println("Raytrace dir: " + dx + ", " + dy + ", " + dz);
//        System.out.println("Raytrace origin: " + ox + ", " + oy + ", " + oz);
//        System.out.println("\n\n\n\n\n\n\n\n\n");
//
//
//        //Get the one closest to the player
////        org.valkyrienskies.core.apigame.world.properties.DimensionId dimensionId = vsContext.getDimensionId();
//        QueryableShipData<LoadedServerShip> loadedShips = vsContext.getLoadedShips();
//        System.out.println("Loaded ships: " + loadedShips.size());
//        Ship pickedShip = null;
//        for (LoadedShip loadedShip : loadedShips) {
//            if (loadedShip != null) {
//                System.out.println("Checking ship: " + loadedShip.getSlug());
//                if (loadedShip.getShipAABB() != null
//                        && loadedShip.getShipAABB().intersectsRay(ox, oy, oz, dx, dy, dz)
//                    //  && loadedShip.getShipAABB().intersectsSphere(hitX, hitY, hitZ, 2)
//                ) {
//                    pickedShip = loadedShip;
//                }
//            }
//        }
//        return pickedShip;
//    }


//
//    private static String getShipBySlug(CommandContext<CommandSourceStack> cc) {
//        VSCommandSource vsContext = (VSCommandSource) cc;
//        ShipWorld shipWorld = vsContext.getShipWorld();
//
//        vsContext.getShipWorld().getAllShips().getById()
//        String slug = cc.getArgument("slug", String.class);
//
//        ChunkPos chunkPos = cc.getSource().getPlayer().chunkPosition();
//        LoadedShip byChunkPos = shipWorld.getLoadedShips().getByChunkPos(chunkPos.x, chunkPos.z, "");
//
//        if (byChunkPos == null) return null;
//        else return byChunkPos.getSlug();
//    }

//    private static int vsListAllShips(CommandContext<CommandSourceStack> cc) {
//        VSCommandSource vsContext = (VSCommandSource) cc;
//        ShipWorld shipWorld = vsContext.getShipWorld();
//
//        cc.getSource().sendSystemMessage(
//                Component.literal(shipWorld.getAllShips().size() + " Total ships")
//        );
//
//
//        StringBuilder stringBuilder = new StringBuilder();
//        shipWorld.getAllShips().forEach((s) -> {
//            stringBuilder.append("Ship  id=").append(s.getId()).append("  \"").append(s.getSlug()).append("\"").append("\n");
//        });
//
//        cc.getSource().sendSystemMessage(
//                Component.literal(stringBuilder.toString())
//        );
//        return 0;
//    }

    private static int vsDeleteMasslessShips(CommandContext<CommandSourceStack> cc) {
        ServerShipWorldCore shipObjectWorld = VSGameUtilsKt.getShipObjectWorld(cc.getSource().getServer());

        ArrayList<ServerShip> shipsToDelete = new ArrayList<>();

        shipObjectWorld.getAllShips().stream().filter((s) -> {
            //Delete ships that have a low / nonexistent mass
            return s.getInertiaData().getMass() < DELETE_MASSLESS_THRESHOLD;
        }).forEach((ship) -> {
            System.out.println("Listing Ship: " + ship.toString());
            shipsToDelete.add(ship);
        });

        cc.getSource().sendSystemMessage(Component.literal("Listed " + shipsToDelete.size() + " massless ships."));

        shipsToDelete.forEach(shipObjectWorld::deleteShip);

        cc.getSource().sendSystemMessage(Component.literal("Deleted massless ships."));

        return 0;
    }
}
