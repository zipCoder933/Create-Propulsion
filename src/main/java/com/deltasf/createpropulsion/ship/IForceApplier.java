package com.deltasf.createpropulsion.ship;

import net.minecraft.core.BlockPos;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

public interface IForceApplier {
    void applyForces(BlockPos pos, PhysShipImpl ship);
}
