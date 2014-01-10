package icbm.explosion.missile.ex;

import icbm.core.ICBMConfiguration;
import icbm.core.prefab.render.ModelICBM;
import icbm.explosion.explosive.blast.BlastEmp;
import icbm.explosion.missile.missile.Missile;
import icbm.explosion.model.missiles.MMDianCi;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapedOreRecipe;
import calclavia.lib.recipe.UniversalRecipe;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ExEMP extends Missile
{
    public ExEMP(String mingZi, int tier)
    {
        super(mingZi, tier);
    }

    @Override
    public void doCreateExplosion(World world, double x, double y, double z, Entity entity)
    {
        new BlastEmp(world, entity, x, y, z, 50).setEffectBlocks().setEffectEntities().explode();
    }

    @Override
    public void init()
    {
        RecipeHelper.addRecipe(new ShapedOreRecipe(this.getItemStack(), new Object[] { "RBR", "BTB", "RBR", 'T', replsive.getItemStack(), 'R', Block.blockRedstone, 'B', UniversalRecipe.BATTERY.get() }), this.getUnlocalizedName(), ICBMConfiguration.CONFIGURATION, true);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ModelICBM getMissileModel()
    {
        return new MMDianCi();
    }
}
