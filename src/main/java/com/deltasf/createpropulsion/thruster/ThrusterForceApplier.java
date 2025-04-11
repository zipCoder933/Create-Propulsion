package com.deltasf.createpropulsion.thruster;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.ship.IForceApplier;

import net.minecraft.core.BlockPos;

//Thruster math is taken from Starlance mod and obfuscated to look like i did this myself
public class ThrusterForceApplier implements IForceApplier {
    private ThrusterData data;
    public ThrusterForceApplier(ThrusterData data){
        this.data = data;
    }
    @Override
    public void applyForces(BlockPos pos, PhysShipImpl ship) {
        float throttle = data.getThrust();
        int maxSpeed = 100;
        if (throttle == 0) return;

        //Direction from ship space to world space
        Vector3d force = ship.getTransform().getShipToWorld().transformDirection(data.getDirection(), new Vector3d());
        force.mul(throttle);
        
        //Applying force for max speed case
        Vector3dc linearVelocity = ship.getPoseVel().getVel();
        if (linearVelocity.lengthSquared() >= maxSpeed * maxSpeed) {
            double dot = force.dot(linearVelocity);
            if (dot > 0) {
                Vector3d tPos = VectorConversionsMCKt.toJOMLD(pos)
                    .add(0.5, 0.5, 0.5, new Vector3d())
                    .sub(ship.getTransform().getPositionInShip());
                Vector3d parallel = new Vector3d(tPos).mul(force.dot(tPos) / force.dot(force));
                Vector3d perpendicular = new Vector3d(force).sub(parallel);
                ship.applyInvariantForceToPos(perpendicular, tPos);
                applyScaledForce(ship, linearVelocity, parallel, maxSpeed);
                return;
            }
        }

        //Applying non-clamped force
        Vector3d tPos = VectorConversionsMCKt.toJOMLD(pos)
            .add(0.5, 0.5, 0.5, new Vector3d())
            .sub(ship.getTransform().getPositionInShip());
        ship.applyInvariantForceToPos(force, tPos);
    }

    private static void applyScaledForce(PhysShipImpl ship, Vector3dc linearVelocity, Vector3d force, float maxSpeed){
        assert ValkyrienSkiesMod.getCurrentServer() != null;
        double deltaTime = 1.0 / (VSGameUtilsKt.getVsPipeline(ValkyrienSkiesMod.getCurrentServer()).computePhysTps());
        double mass = ship.getInertia().getShipMass();
        
        Vector3d targetVelocity = (new Vector3d(linearVelocity).add(new Vector3d(force).mul(deltaTime / mass)).normalize(maxSpeed)).sub(linearVelocity);
        ship.applyInvariantForce(targetVelocity.mul(mass / deltaTime));
    }
}
