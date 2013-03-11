package icbm.core.di;

import icbm.api.ICBM;
import icbm.api.ICBMTab;
import icbm.core.ZhuYao;

import java.util.ArrayList;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.prefab.block.BlockAdvanced;
import universalelectricity.prefab.implement.IRedstoneProvider;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BICBM extends BlockAdvanced
{
	private int maxMetadata = 0;
	protected Icon iconTop, iconSide, iconBottom;

	public BICBM(int id, String name, Material material)
	{
		super(ICBM.CONFIGURATION.getBlock(name, id).getInt(), material);
		this.setUnlocalizedName(ZhuYao.PREFIX + name);
		this.setCreativeTab(ICBMTab.INSTANCE);
	}

	public BICBM(int id, String name, Material material, int maxMetadata)
	{
		this(id, name, material);
		this.maxMetadata = maxMetadata;
	}

	@Override
	public int damageDropped(int metadata)
	{
		return metadata;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void func_94332_a(IconRegister iconRegister)
	{
		super.func_94332_a(iconRegister);
		this.iconTop = iconRegister.func_94245_a(this.func_94330_A() + "_top");
		this.iconSide = iconRegister.func_94245_a(this.func_94330_A() + "_side");
		this.iconBottom = iconRegister.func_94245_a(this.func_94330_A() + "_bottom");
	}

	/**
	 * Is this block powering the block on the specified side
	 */
	@Override
	public int isProvidingStrongPower(IBlockAccess par1IBlockAccess, int x, int y, int z, int side)
	{
		TileEntity tileEntity = par1IBlockAccess.getBlockTileEntity(x, y, z);

		if (tileEntity instanceof IRedstoneProvider)
		{
			return ((IRedstoneProvider) tileEntity).isPoweringTo(ForgeDirection.getOrientation(side)) ? 15 : 0;
		}

		return 0;
	}

	/**
	 * Is this block indirectly powering the block on the specified side
	 */
	@Override
	public int isProvidingWeakPower(IBlockAccess par1IBlockAccess, int x, int y, int z, int side)
	{
		TileEntity tileEntity = par1IBlockAccess.getBlockTileEntity(x, y, z);

		if (tileEntity instanceof IRedstoneProvider)
		{
			return ((IRedstoneProvider) tileEntity).isIndirectlyPoweringTo(ForgeDirection.getOrientation(side)) ? 15 : 0;
		}

		return 0;
	}

	@Override
	public void addCreativeItems(ArrayList list)
	{
		for (int i = 0; i < this.maxMetadata; i++)
		{
			list.add(new ItemStack(this, 1, i));
		}
	}
}
