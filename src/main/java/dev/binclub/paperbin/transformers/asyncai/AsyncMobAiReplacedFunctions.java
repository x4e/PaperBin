package dev.binclub.paperbin.transformers.asyncai;

import net.minecraft.server.v1_12_R1.*;
import net.minecraft.server.v1_12_R1.BlockPosition.MutableBlockPosition;

import javax.annotation.Nullable;
import java.util.Iterator;

/**
 * @author cookiedragon234 26/May/2020
 */
public class AsyncMobAiReplacedFunctions {
	/**
	 * {@link net.minecraft.server.v1_12_R1.World}
	 */
	public static boolean Worlda(World world, AxisAlignedBB axisalignedbb, Material material) {
		int i = MathHelper.floor(axisalignedbb.a);
		int j = MathHelper.f(axisalignedbb.d);
		int k = MathHelper.floor(axisalignedbb.b);
		int l = MathHelper.f(axisalignedbb.e);
		int i1 = MathHelper.floor(axisalignedbb.c);
		int j1 = MathHelper.f(axisalignedbb.f);
		BlockPosition.PooledBlockPosition blockposition_pooledblockposition = BlockPosition.PooledBlockPosition.s();
		
		for(int k1 = i; k1 < j; ++k1) {
			for(int l1 = k; l1 < l; ++l1) {
				for(int i2 = i1; i2 < j1; ++i2) {
					IBlockData type = world.getTypeIfLoaded(blockposition_pooledblockposition.f(k1, l1, i2)); // PAPERBIN - use getTypeIfLoaded
					if (type != null && type.getMaterial() == material) { // PAPERBIN - use getTypeIfLoaded
						blockposition_pooledblockposition.t();
						return true;
					}
				}
			}
		}
		
		blockposition_pooledblockposition.t();
		return false;
	}
	
	/**
	 * {@link net.minecraft.server.v1_12_R1.PathfinderGoalRandomFly}
	 */
	public static Vec3D PathfinderGoalRandomFlyj(EntityCreature a) {
		BlockPosition var1 = new BlockPosition(a);
		MutableBlockPosition var2 = new MutableBlockPosition();
		MutableBlockPosition var3 = new MutableBlockPosition();
		Iterable var4 = MutableBlockPosition.b(MathHelper.floor(a.locX - 3.0D), MathHelper.floor(a.locY - 6.0D), MathHelper.floor(a.locZ - 3.0D), MathHelper.floor(a.locX + 3.0D), MathHelper.floor(a.locY + 6.0D), MathHelper.floor(a.locZ + 3.0D));
		Iterator var5 = var4.iterator();
		
		BlockPosition var6;
		boolean var8;
		do {
			do {
				if (!var5.hasNext()) {
					return null;
				}
				
				var6 = (BlockPosition)var5.next();
			} while(var1.equals(var6));
			
			IBlockData state = a.world.getTypeIfLoaded(var3.g(var6).c(EnumDirection.DOWN)); // PAPERBIN - use getTypeIfLoaded
			if (state != null) { // PAPERBIN - use getTypeIfLoaded
				Block var7 = state.getBlock();
				var8 = var7 instanceof BlockLeaves || var7 == Blocks.LOG || var7 == Blocks.LOG2;
			} else { var8 = false; } // PAPERBIN - use getTypeIfLoaded
		} while(!var8 || !a.world.isEmpty(var6) || !a.world.isEmpty(var2.g(var6).c(EnumDirection.UP)));
		
		return new Vec3D((double)var6.getX(), (double)var6.getY(), (double)var6.getZ());
	}
	
	/**
	 * Animals use this to find water to jump into if theyre burning
	 *
	 * {@link net.minecraft.server.v1_12_R1.PathfinderGoalPanic}
	 */
	public static BlockPosition PathfinderGoalPanica(World world, Entity entity, int i, int j) {
		BlockPosition blockposition = new BlockPosition(entity);
		int k = blockposition.getX();
		int l = blockposition.getY();
		int i1 = blockposition.getZ();
		float f = (float)(i * i * j * 2);
		BlockPosition blockposition1 = null;
		MutableBlockPosition blockposition_mutableblockposition = new MutableBlockPosition();
		
		for(int j1 = k - i; j1 <= k + i; ++j1) {
			for(int k1 = l - j; k1 <= l + j; ++k1) {
				for(int l1 = i1 - i; l1 <= i1 + i; ++l1) {
					blockposition_mutableblockposition.c(j1, k1, l1);
					IBlockData iblockdata = world.getTypeIfLoaded(blockposition_mutableblockposition); // PAPERBIN - use getTypeIfLoaded
					if (iblockdata != null && iblockdata.getMaterial() == Material.WATER) { // PAPERBIN - use getTypeIfLoaded
						float f1 = (float)((j1 - k) * (j1 - k) + (k1 - l) * (k1 - l) + (l1 - i1) * (l1 - i1));
						if (f1 < f) {
							f = f1;
							blockposition1 = new BlockPosition(blockposition_mutableblockposition);
						}
					}
				}
			}
		}
		
		return blockposition1;
	}
}
