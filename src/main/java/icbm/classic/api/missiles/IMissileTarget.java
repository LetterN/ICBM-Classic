package icbm.classic.api.missiles;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Abstracted targeting logic for missiles
 */
public interface IMissileTarget extends INBTSerializable<NBTTagCompound> {

    /**
     * Target position, assume this is not cached and can change
     *
     * @return current targeting position
     */
    Vec3d getPosition();

    /**
     * Checks if the target data is valid
     */
    boolean isValid();

    /**
     * Getter for x position, assume this is not cached and can change
     * @return double or NaN if no target is set
     */
    double getX();

    /**
     * Getter for y position, assume this is not cached and can change
     * @return double or NaN if no target is set
     */
    double getY();

    /**
     * Getter for z position, assume this is not cached and can change
     * @return double or NaN if no target is set
     */
    double getZ();

    ResourceLocation getRegistryName();

    /**
     * Called to see if the missile should save's as an ID for restore. Some
     * missile types are hardcoded with specific target wrappers. Meaning
     * they will always be present and thus don't need to restore between
     * game saves
     *
     * @return true to save, false to only store cache data
     */
    default boolean shouldRestoreFromSave() {
        return true;
    }

    /**
     * Helper to get flat distance in x-z plane
     * @return flat distance to target
     */
    default double calculateFlatDistance(double x, double z) {
        double deltaX = getX() - x;
        double deltaZ = getZ() - z;
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }
}
