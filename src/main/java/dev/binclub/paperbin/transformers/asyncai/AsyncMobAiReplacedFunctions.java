package dev.binclub.paperbin.transformers.asyncai;

import dev.binclub.paperbin.PaperBinInfo;
import net.minecraft.server.v1_12_R1.*;
import net.minecraft.server.v1_12_R1.BlockPosition.MutableBlockPosition;
import org.bukkit.craftbukkit.v1_12_R1.util.CraftMagicNumbers;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author cookiedragon234 26/May/2020
 */
public class AsyncMobAiReplacedFunctions {
	/**
	 * {@link net.minecraft.server.v1_12_R1.World}
	 */
	public static boolean Worlda(@Nullable Entity entity, AxisAlignedBB axisalignedbb, boolean flag, @Nullable List list, World world) {
		int i = MathHelper.floor(axisalignedbb.a) - 1;
		int j = MathHelper.f(axisalignedbb.d) + 1;
		int k = MathHelper.floor(axisalignedbb.b) - 1;
		int l = MathHelper.f(axisalignedbb.e) + 1;
		int i1 = MathHelper.floor(axisalignedbb.c) - 1;
		int j1 = MathHelper.f(axisalignedbb.f) + 1;
		WorldBorder worldborder = world.getWorldBorder();
		boolean flag1 = entity != null && entity.bz();
		boolean flag2 = entity != null && world.g(entity);
		IBlockData iblockdata = Blocks.STONE.getBlockData();
		BlockPosition.PooledBlockPosition blockposition_pooledblockposition = BlockPosition.PooledBlockPosition.s();
		
		try {
			for(int k1 = i; k1 < j; ++k1) {
				for(int l1 = i1; l1 < j1; ++l1) {
					boolean flag3 = k1 == i || k1 == j - 1;
					boolean flag4 = l1 == i1 || l1 == j1 - 1;
					if ((!flag3 || !flag4) && world.isLoaded(blockposition_pooledblockposition.f(k1, 64, l1))) {
						for(int i2 = k; i2 < l; ++i2) {
							if (!flag3 && !flag4 || i2 != l - 1) {
								boolean flag6;
								if (flag) {
									if (k1 < -30000000 || k1 >= 30000000 || l1 < -30000000 || l1 >= 30000000) {
										boolean flag5 = true;
										flag6 = flag5;
										return flag6;
									}
								} else if (entity != null && flag1 == flag2) {
									entity.k(!flag2);
								}
								
								blockposition_pooledblockposition.f(k1, i2, l1);
								IBlockData iblockdata1;
								if (!flag && !worldborder.a((BlockPosition)blockposition_pooledblockposition) && flag2) {
									iblockdata1 = iblockdata;
								} else {
									iblockdata1 = world.getTypeIfLoaded(blockposition_pooledblockposition); // PAPERBIN - use getTypeIfLoaded
								}
								
								if (iblockdata1 != null) { // PAPERBIN - use getTypeIfLoaded
									iblockdata1.a(world, blockposition_pooledblockposition, axisalignedbb, list, entity, false);
								}
								if (flag && !list.isEmpty()) {
									flag6 = true;
									boolean var23 = flag6;
									return var23;
								}
							}
						}
					}
				}
			}
			
			boolean var27 = !list.isEmpty();
			return var27;
		} finally {
			blockposition_pooledblockposition.t();
		}
	}
	
	/**
	 * {@link net.minecraft.server.v1_12_R1.World}
	 */
	public static IBlockData WorldgetCapturedBlockType(int x, int y, int z, ArrayList capturedBlockStates) {
		org.bukkit.block.BlockState previous = null;
		for (Object ostate : capturedBlockStates.toArray()) {
			previous = (org.bukkit.block.BlockState)ostate;
			if (previous.getX() != x || previous.getY() != y || previous.getZ() != z) {
				break;
			}
		}
		
		if (previous == null) {
			return null;
		}
		
		return CraftMagicNumbers.getBlock(previous.getTypeId()).fromLegacyData(previous.getRawData());
	}
	
	/**
	 * {@link net.minecraft.server.v1_12_R1.PersistentVillage}
	 */
	public static Village PersistentVillagegetClosestVillage(List villages, BlockPosition blockposition, int i) {
		Village village = null;
		double d0 = 3.4028234663852886E38D;
		
		if (villages == null) return null; // PAPERBIN START - Concurrent access of villages
		
		for (Object oVillage : villages.toArray()) {  // PAPERBIN START - Concurrent access of villages
			Village village1 = (Village)oVillage;  // PAPERBIN START - Concurrent access of villages
			double d1 = village1.a().n(blockposition);
			if (d1 < d0) {
				float f = (float)(i + village1.b());
				if (d1 <= (double)(f * f)) {
					village = village1;
					d0 = d1;
				}
			}
		}
		
		return village;
	}
	
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
