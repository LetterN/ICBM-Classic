package icbm.zhapin;

import icbm.core.ShengYin;
import icbm.core.ZhuYao;
import icbm.zhapin.cart.EChe;
import icbm.zhapin.daodan.EDaoDan;
import icbm.zhapin.gui.GDianCiQi;
import icbm.zhapin.gui.GFaSheDi;
import icbm.zhapin.gui.GFaSheShiMuo;
import icbm.zhapin.gui.GLeiDaTai;
import icbm.zhapin.gui.GXiaoFaSheQi;
import icbm.zhapin.jiqi.TDianCiQi;
import icbm.zhapin.jiqi.TFaSheDi;
import icbm.zhapin.jiqi.TFaSheJia;
import icbm.zhapin.jiqi.TFaSheShiMuo;
import icbm.zhapin.jiqi.TLeiDaTai;
import icbm.zhapin.jiqi.TXiaoFaSheQi;
import icbm.zhapin.jiqi.TYinDaoQi;
import icbm.zhapin.render.RChe;
import icbm.zhapin.render.RDaoDan;
import icbm.zhapin.render.RDianCiQi;
import icbm.zhapin.render.REZhaDan;
import icbm.zhapin.render.RFaSheDi;
import icbm.zhapin.render.RFaSheJia;
import icbm.zhapin.render.RFaSheShiMuo;
import icbm.zhapin.render.RFeiBlock;
import icbm.zhapin.render.RGuangBang;
import icbm.zhapin.render.RHJiQi;
import icbm.zhapin.render.RHZhaPin;
import icbm.zhapin.render.RItDaoDan;
import icbm.zhapin.render.RItFaSheQi;
import icbm.zhapin.render.RLeiDaTai;
import icbm.zhapin.render.RShouLiuDan;
import icbm.zhapin.render.RSuiPian;
import icbm.zhapin.render.RXiaoFaSheQi;
import icbm.zhapin.render.RYinDaoQi;
import icbm.zhapin.render.RZhaDan;
import icbm.zhapin.render.RZhaPin;
import icbm.zhapin.zhapin.EShouLiuDan;
import icbm.zhapin.zhapin.EZhaDan;
import icbm.zhapin.zhapin.EZhaPin;
import icbm.zhapin.zhapin.TZhaDan;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy
{
	@Override
	public void preInit()
	{
		TickRegistry.registerTickHandler(new TickHandler(), Side.CLIENT);

		MinecraftForgeClient.preloadTexture(ZhuYao.ITEM_PATH);
		MinecraftForgeClient.preloadTexture(ZhuYao.BLOCK_PATH);

		MinecraftForge.EVENT_BUS.register(ShengYin.INSTANCE);
	}

	@Override
	public void init()
	{
		super.init();

		MinecraftForgeClient.registerItemRenderer(ZhuYaoZhaPin.itFaSheQi.itemID, new RItFaSheQi());
		MinecraftForgeClient.registerItemRenderer(ZhuYaoZhaPin.itDaoDan.itemID, new RItDaoDan());
		MinecraftForgeClient.registerItemRenderer(ZhuYaoZhaPin.itTeBieDaoDan.itemID, new RItDaoDan());

		RenderingRegistry.registerBlockHandler(new RHZhaPin());
		RenderingRegistry.registerBlockHandler(new RHJiQi());

		RenderingRegistry.registerEntityRenderingHandler(EZhaDan.class, new REZhaDan());
		RenderingRegistry.registerEntityRenderingHandler(EDaoDan.class, new RDaoDan(0.5F));
		RenderingRegistry.registerEntityRenderingHandler(EZhaPin.class, new RZhaPin());
		RenderingRegistry.registerEntityRenderingHandler(EFeiBlock.class, new RFeiBlock());
		RenderingRegistry.registerEntityRenderingHandler(EGuang.class, new RGuangBang());
		RenderingRegistry.registerEntityRenderingHandler(ESuiPian.class, new RSuiPian());
		RenderingRegistry.registerEntityRenderingHandler(EShouLiuDan.class, new RShouLiuDan());
		RenderingRegistry.registerEntityRenderingHandler(EChe.class, new RChe());

		ClientRegistry.bindTileEntitySpecialRenderer(TXiaoFaSheQi.class, new RXiaoFaSheQi());
		ClientRegistry.bindTileEntitySpecialRenderer(TFaSheDi.class, new RFaSheDi());
		ClientRegistry.bindTileEntitySpecialRenderer(TFaSheShiMuo.class, new RFaSheShiMuo());
		ClientRegistry.bindTileEntitySpecialRenderer(TFaSheJia.class, new RFaSheJia());
		ClientRegistry.bindTileEntitySpecialRenderer(TLeiDaTai.class, new RLeiDaTai());
		ClientRegistry.bindTileEntitySpecialRenderer(TDianCiQi.class, new RDianCiQi());
		ClientRegistry.bindTileEntitySpecialRenderer(TZhaDan.class, new RZhaDan());
		ClientRegistry.bindTileEntitySpecialRenderer(TYinDaoQi.class, new RYinDaoQi());

	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer entityPlayer, World world, int x, int y, int z)
	{
		TileEntity tileEntity = world.getBlockTileEntity(x, y, z);

		if (tileEntity != null || ID == ZhuYao.GUI_SHENG_BUO)
		{
			switch (ID)
			{
				case ZhuYao.GUI_XIA_FA_SHE_QI:
					return new GXiaoFaSheQi(entityPlayer.inventory, (TXiaoFaSheQi) tileEntity);
				case ZhuYao.GUI_FA_SHE_SHI_MUO:
					return new GFaSheShiMuo(((TFaSheShiMuo) tileEntity));
				case ZhuYao.GUI_LEI_DA_TAI:
					return new GLeiDaTai(((TLeiDaTai) tileEntity));
				case ZhuYao.GUI_DIAN_CI_QI:
					return new GDianCiQi((TDianCiQi) tileEntity);
				case ZhuYao.GUI_FA_SHE_DI:
					return new GFaSheDi(entityPlayer.inventory, (TFaSheDi) tileEntity);
			}
		}

		return null;
	}

	@Override
	public boolean isGaoQing()
	{
		return Minecraft.getMinecraft().gameSettings.fancyGraphics;
	}
}
