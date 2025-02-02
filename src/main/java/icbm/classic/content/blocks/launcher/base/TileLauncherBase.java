package icbm.classic.content.blocks.launcher.base;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.missiles.ICapabilityMissileStack;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.content.missile.source.MissileSourceBlock;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.content.missile.logic.flight.BallisticFlightLogic;
import icbm.classic.content.missile.targeting.BallisticTargetingData;
import icbm.classic.lib.NBTConstants;
import icbm.classic.api.caps.IMissileHolder;
import icbm.classic.api.caps.IMissileLauncher;
import icbm.classic.api.events.LauncherEvent;
import icbm.classic.api.tile.multiblock.IMultiTile;
import icbm.classic.api.tile.multiblock.IMultiTileHost;
import icbm.classic.config.ConfigLauncher;
import icbm.classic.content.entity.EntityPlayerSeat;
import icbm.classic.content.blocks.launcher.frame.TileLauncherFrame;
import icbm.classic.content.blocks.launcher.screen.TileLauncherScreen;
import icbm.classic.content.blocks.multiblock.MultiBlockHelper;
import icbm.classic.content.reg.BlockReg;
import icbm.classic.lib.capability.launcher.CapabilityMissileHolder;
import icbm.classic.lib.transform.rotation.EulerAngle;
import icbm.classic.lib.transform.vector.Pos;
import icbm.classic.prefab.tile.BlockICBM;
import icbm.classic.api.EnumTier;
import icbm.classic.prefab.tile.TileMachine;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This tile entity is for the base of the missile launcher
 *
 * @author Calclavia, DarkGuardsman
 */
public class TileLauncherBase extends TileMachine implements IMultiTileHost
{

    public static List<BlockPos> northSouthMultiBlockCache = new ArrayList();
    public static List<BlockPos> eastWestMultiBlockCache = new ArrayList();

    private static final EulerAngle angle = new EulerAngle(0, 0, 0);

    static
    {
        northSouthMultiBlockCache.add(new BlockPos(1, 0, 0));
        northSouthMultiBlockCache.add(new BlockPos(1, 1, 0));
        northSouthMultiBlockCache.add(new BlockPos(1, 2, 0));
        northSouthMultiBlockCache.add(new BlockPos(-1, 0, 0));
        northSouthMultiBlockCache.add(new BlockPos(-1, 1, 0));
        northSouthMultiBlockCache.add(new BlockPos(-1, 2, 0));

        eastWestMultiBlockCache.add(new BlockPos(0, 0, 1));
        eastWestMultiBlockCache.add(new BlockPos(0, 1, 1));
        eastWestMultiBlockCache.add(new BlockPos(0, 2, 1));
        eastWestMultiBlockCache.add(new BlockPos(0, 0, -1));
        eastWestMultiBlockCache.add(new BlockPos(0, 1, -1));
        eastWestMultiBlockCache.add(new BlockPos(0, 2, -1));
    }

    // The connected missile launcher frame
    public TileLauncherFrame supportFrame = null;
    public TileLauncherScreen launchScreen = null;

    /**
     * Fake entity to allow player to mount the missile without using the missile entity itself
     */
    public EntityPlayerSeat seat;

    private boolean _destroyingStructure = false;

    private boolean checkMissileCollision = true;
    private boolean hasMissileCollision = false;

    private final LauncherInventory inventory = new LauncherInventory(this);

    /**
     * Client's render cached object, used in place of inventory to avoid affecting GUIs
     */
    public ItemStack cachedMissileStack;

    public final IMissileHolder missileHolder = new CapabilityMissileHolder(inventory, 0);
    public final IMissileLauncher missileLauncher = null; //TODO implement, screen will now only set data instead of being the launcher

    /**
     * Allows the entity to update its state. Overridden in most subclasses, e.g. the mob spawner
     * uses this to count ticks and creates a new spawn inside its implementation.
     */
    @Override
    public void update()
    {
        super.update();
        if (isServer())
        {
            if (ticks % 3 == 0)
            {
                checkMissileCollision = true;

                //Update seat position
                Optional.ofNullable(seat).ifPresent(seat -> seat.setPosition(x() + 0.5, y() + 0.5, z() + 0.5));

                //Create seat if missile
                if (!getMissileStack().isEmpty() && seat == null)  //TODO add hook to disable riding some missiles
                {
                    seat = new EntityPlayerSeat(world);
                    seat.host = this;
                    seat.rideOffset = new Pos(getRotation()).multiply(0.5, 1, 0.5);
                    seat.setPosition(x() + 0.5, y() + 0.5, z() + 0.5);
                    seat.setSize(0.5f, 2.5f);
                    world.spawnEntity(seat);
                }
                //Destroy seat if no missile
                else if (getMissileStack().isEmpty() && seat != null)
                {
                    Optional.ofNullable(seat.getRidingEntity()).ifPresent(Entity::removePassengers);
                    seat.setDead();
                    seat = null;
                }
            }
        }
        //1 second update
        if (ticks % 20 == 0)
        {
            //Only update if frame or screen is invalid
            if (this.supportFrame == null || launchScreen == null || launchScreen.isInvalid() || this.supportFrame.isInvalid())
            {
                //Reset data
                if (this.supportFrame != null)
                {
                    this.supportFrame.launcherBase = null;
                }
                this.supportFrame = null;
                this.launchScreen = null;

                //Check on all 4 sides
                for (EnumFacing rotation : EnumFacing.HORIZONTALS)
                {
                    //Get tile entity on side
                    Pos position = new Pos(getPos()).add(rotation);
                    TileEntity tileEntity = this.world.getTileEntity(position.toBlockPos());

                    //If frame update rotation
                    if (tileEntity instanceof TileLauncherFrame)
                    {
                        this.supportFrame = (TileLauncherFrame) tileEntity;
                        this.supportFrame.launcherBase = this;
                        if (isServer())
                        {
                            this.supportFrame.setRotation(getRotation());
                        }
                    }
                    //If screen, tell the screen the base exists
                    else if (tileEntity instanceof TileLauncherScreen)
                    {
                        this.launchScreen = (TileLauncherScreen) tileEntity;
                    }
                }
            }
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing)
    {
        //Run before screen check to prevent looping
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || capability == ICBMClassicAPI.MISSILE_HOLDER_CAPABILITY)
        {
            return true;
        }
        //Check if we can pass to launcher screen
        else if (launchScreen != null)
        {
            return launchScreen.hasCapability(capability, facing); //TODO pass in self to prevent looping
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return (T) inventory;
        } else if (capability == ICBMClassicAPI.MISSILE_HOLDER_CAPABILITY)
        {
            return (T) missileHolder;
        } else if (launchScreen != null)
        {
            return launchScreen.getCapability(capability, facing);
        }
        return super.getCapability(capability, facing);
    }

    public boolean checkForMissileInBounds()
    {
        //Limit how often we check for collision
        if (checkMissileCollision)
        {
            checkMissileCollision = false;

            //Validate the space above the launcher is free of entities, mostly for smooth reload visuals
            final AxisAlignedBB collisionCheck = new AxisAlignedBB(xi(), yi(), zi(), xi() + 1, yi() + 5, zi() + 1);
            final List<EntityMissile> entities = world.getEntitiesWithinAABB(EntityMissile.class, collisionCheck);
            hasMissileCollision = entities.size() > 0;
        }
        return hasMissileCollision;
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return new TextComponentTranslation("gui.launcherBase.name");
    }

    protected Pos applyInaccuracy(Pos target)
    {
        // Apply inaccuracy
        float inaccuracy = 30f; //TODO config

        //Get value from support frame
        if (this.supportFrame != null)
        {
            inaccuracy = this.supportFrame.getInaccuracy();
        }

        //TODO add distance based inaccuracy addition
        //TODO add tier based inaccuracy, higher tier missiles have a high chance of hitting

        //Randomize distance
        inaccuracy = inaccuracy * getWorld().rand.nextFloat();

        //Randomize radius drop
        angle.setYaw(getWorld().rand.nextFloat() * 360); //TODO fix to use a normal distribution from ICBM 2

        //Apply inaccuracy to target position and return
        return target.add(angle.x() * inaccuracy, 0, angle.z() * inaccuracy);
    }

    /**
     * Launches the missile
     *
     * @param target     - The target in which the missile will land in
     * @param lockHeight - height to wait before curving the missile
     */
    public boolean launchMissile(Pos target, int lockHeight)
    {
        //Allow canceling missile launches
        if (MinecraftForge.EVENT_BUS.post(new LauncherEvent.PreLaunch(missileLauncher, missileHolder)))
        {
            return false;
        }

        final ItemStack stack = getMissileStack();
        if (stack.hasCapability(ICBMClassicAPI.MISSILE_STACK_CAPABILITY, null))
        {
            final ICapabilityMissileStack missileStack = stack.getCapability(ICBMClassicAPI.MISSILE_STACK_CAPABILITY, null);

            if (missileStack != null)
            {
                target = applyInaccuracy(target);

                //TODO add distance check? --- something seems to be missing

                if (isServer())
                {
                    final IMissile missile = missileStack.newMissile(world());
                    final Entity entity = missile.getMissileEntity();
                    entity.setPosition(xi() + 0.5, yi() + 2.2, zi() + 0.5);  //TODO store offset as variable, sync with missile height

                    //Trigger launch event
                    missile.setTargetData(new BallisticTargetingData(target, lockHeight));
                    missile.setFlightLogic(new BallisticFlightLogic());
                    missile.setMissileSource( new MissileSourceBlock(world, getPos(), getBlockState(), null)); //TODO encode player that built launcher, firing method (laser, remote, redstone), and other useful data
                    missile.launch();

                    //Spawn entity
                    ((WorldServer) getWorld()).addScheduledTask(() -> getWorld().spawnEntity(entity));

                    //Grab rider
                    if (seat != null && !seat.getPassengers().isEmpty()) //TODO add hook to disable riding some missiles
                    {
                        final List<Entity> riders = seat.getPassengers();
                        riders.forEach(r -> {
                            entity.dismountRidingEntity();
                            r.startRiding(entity);
                        });
                    }

                    //Remove item
                    inventory.extractItem(0, 1, false);
                    checkMissileCollision = true;
                }
                return true;
            }
        }
        return false;
    }

    // Checks if the missile target is in range
    public boolean isInRange(Pos target)
    {
        if (target != null)
        {
            return !isTargetTooFar(target) && !isTargetTooClose(target);
        }
        return false;
    }

    /**
     * Checks to see if the target is too close.
     *
     * @param target
     * @return
     */
    public boolean isTargetTooClose(Pos target)
    {
        // Check if it is greater than the minimum range
        return new Pos(this.x(), 0, this.z()).distance(new Pos(target.x(), 0, target.z())) < 10;
    }

    // Is the target too far?
    public boolean isTargetTooFar(Pos target)
    {
        // Checks if it is greater than the maximum range for the launcher base
        double distance = new Pos(this.x(), 0, this.z()).distance(new Pos(target.x(), 0, target.z()));


        return distance > getRange();
    }

    public double getRange()
    {
        return getRangeForTier(getTier());
    }

    public static double getRangeForTier(EnumTier tier)
    {
        if (tier == EnumTier.ONE)
        {
            return ConfigLauncher.LAUNCHER_RANGE_TIER1;
        } else if (tier == EnumTier.TWO)
        {
            return ConfigLauncher.LAUNCHER_RANGE_TIER2;
        }
        return ConfigLauncher.LAUNCHER_RANGE_TIER3;
    }

    /**
     * Reads a tile entity from NBT.
     */
    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        inventory.deserializeNBT(nbt.getCompoundTag(NBTConstants.INVENTORY));
    }

    /**
     * Writes a tile entity to NBT.
     */
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt.setTag(NBTConstants.INVENTORY, inventory.serializeNBT());
        return super.writeToNBT(nbt);
    }

    @Override
    public void writeDescPacket(ByteBuf buf)
    {
        super.writeDescPacket(buf);
        ByteBufUtils.writeItemStack(buf, getMissileStack());
    }

    @Override
    public void readDescPacket(ByteBuf buf)
    {
        super.readDescPacket(buf);
        cachedMissileStack = ByteBufUtils.readItemStack(buf);
    }

    public ItemStack getMissileStack()
    {
        if (isClient() && cachedMissileStack != null)
        {
            return cachedMissileStack;
        }
        return missileHolder.getMissileStack();
    }

    public boolean onPlayerRightClick(EntityPlayer player, EnumHand hand, ItemStack heldItem)
    {

        if (!tryInsertMissile(player, hand, heldItem) && launchScreen != null)
        {
            return BlockReg.blockLaunchScreen.onBlockActivated(world, launchScreen.getPos(), world.getBlockState(launchScreen.getPos()), player, hand, EnumFacing.NORTH, 0, 0, 0);
            //return launchScreen.onPlayerActivated(player, side, hit);
        }

        return true;
    }

    public boolean tryInsertMissile(EntityPlayer player, EnumHand hand, ItemStack heldItem)
    {
        if (this.getMissileStack().isEmpty() && missileHolder.canSupportMissile(heldItem))
        {
            if (isServer())
            {
                final ItemStack stackLeft = inventory.insertItem(0, heldItem, false);
                if (!player.capabilities.isCreativeMode)
                {
                    player.setItemStackToSlot(hand == EnumHand.MAIN_HAND ? EntityEquipmentSlot.MAINHAND : EntityEquipmentSlot.OFFHAND, stackLeft);
                    player.inventoryContainer.detectAndSendChanges();
                }
            }
            return true;
        }
        else if (player.isSneaking() && heldItem.isEmpty() && !this.getMissileStack().isEmpty())
        {
            if (isServer())
            {

                player.setItemStackToSlot(hand == EnumHand.MAIN_HAND ? EntityEquipmentSlot.MAINHAND : EntityEquipmentSlot.OFFHAND, this.getMissileStack());
                inventory.extractItem(0, 1, false);
                player.inventoryContainer.detectAndSendChanges();
            }
            return true;
        }
        return false;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox()
    {
        return INFINITE_EXTENT_AABB;
    }

    //==========================================
    //==== Multi-Block code
    //=========================================

    @Override
    public void onMultiTileAdded(IMultiTile tileMulti)
    {
        if (tileMulti instanceof TileEntity)
        {
            BlockPos pos = ((TileEntity) tileMulti).getPos().subtract(getPos());
            if (getLayoutOfMultiBlock().contains(pos))
            {
                tileMulti.setHost(this);
            }
        }
    }

    @Override
    public boolean onMultiTileBroken(IMultiTile tileMulti, Object source, boolean harvest)
    {
        if (!_destroyingStructure && tileMulti instanceof TileEntity)
        {
            BlockPos pos = ((TileEntity) tileMulti).getPos().subtract(getPos());
            if (getLayoutOfMultiBlock().contains(pos))
            {
                MultiBlockHelper.destroyMultiBlockStructure(this, harvest, true, true);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onTileInvalidate(IMultiTile tileMulti)
    {

    }

    @Override
    public boolean onMultiTileActivated(IMultiTile tile, EntityPlayer player, EnumHand hand, EnumFacing side, float xHit, float yHit, float zHit)
    {
        return this.onPlayerRightClick(player, hand, player.getHeldItem(hand));
    }

    @Override
    public void onMultiTileClicked(IMultiTile tile, EntityPlayer player)
    {

    }

    @Override
    public List<BlockPos> getLayoutOfMultiBlock()
    {
        return getLayoutOfMultiBlock(getRotation());
    }

    public static List<BlockPos> getLayoutOfMultiBlock(EnumFacing facing)
    {
        if (facing == EnumFacing.EAST || facing == EnumFacing.WEST)
        {
            return eastWestMultiBlockCache;
        }
        return northSouthMultiBlockCache;
    }

    @Override
    public void setRotation(EnumFacing facingDirection)
    {
        //Only update if state has changed
        if (facingDirection != getRotation()

            //Prevent up and down placement
            && facingDirection != EnumFacing.UP
            && facingDirection != EnumFacing.DOWN)
        {
            //Clear old structure
            if (isServer())
            {
                MultiBlockHelper.destroyMultiBlockStructure(this, false, true, false);
            }

            //Update block state
            world.setBlockState(pos, getBlockState().withProperty(BlockICBM.ROTATION_PROP, facingDirection));

            //Create new structure
            if (isServer())
            {
                MultiBlockHelper.buildMultiBlock(getWorld(), this, true, true);
                markDirty();
            }
        }
    }
}
