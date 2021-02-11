package net.minecraft.server.v1_12_R1;

import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nullable;

import dev.binclub.paperbin.PaperBinConfig;
import net.minecraft.server.v1_12_R1.MethodProfiler;
import net.minecraft.server.v1_12_R1.PathfinderGoal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PathfinderGoalSelector {
	private static final Logger a = LogManager.getLogger();
	private final Set<net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem> b = Sets.newLinkedHashSet();
	private Set<net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem> c = Sets.newLinkedHashSet(); // PAPERBIN: Not final
	private final MethodProfiler d;
	private int e;
	private int f = 3;
	private int g;
	
	public PathfinderGoalSelector(MethodProfiler var1) {
		this.d = var1;
	}
	
	public void a(int var1, PathfinderGoal var2) {
		this.b.add(new net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem(var1, var2));
	}
	
	public void a(PathfinderGoal var1) {
		Iterator var2 = this.b.iterator();
		
		net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem var3;
		PathfinderGoal var4;
		do {
			if (!var2.hasNext()) {
				return;
			}
			
			var3 = (net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem)var2.next();
			var4 = var3.a;
		} while(var4 != var1);
		
		if (var3.c) {
			var3.c = false;
			var3.a.d();
			this.c.remove(var3);
		}
		
		var2.remove();
	}
	
	public void a() {
		// PAPERBIN START - Separate goal logic
		setupGoals();
		tickGoals();
	}
	
	public void setupGoals() {
		this.d.a("goalSetup");
		Iterator var1;
		net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem var2;
		// PAPERBIN START - Allow concurrent access of `c`
		// Clone the set to allow for iteration while the main game thread is iterating
		Set<net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem> clonedC;
		if (PaperBinConfig.INSTANCE.getMobAiMultithreading()) {
			clonedC = new LinkedHashSet<>(this.c);
		} else {
			clonedC = this.c; // If not enabled just use the original set
		}
		try {
			// PAPERBIN END - Allow concurrent access of `c`
			if (this.e++ % this.f == 0) {
				var1 = this.b.iterator();
				
				label57:
				while (true) {
					do {
						while (true) {
							if (!var1.hasNext()) {
								break label57;
							}
							
							var2 = (net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem) var1.next();
							if (var2.c) {
								break;
							}
							
							if (this.b(var2) && var2.a.a()) {
								var2.c = true;
								var2.a.c();
								clonedC.add(var2); // Paperbin - use local c
							}
						}
					} while (this.b(var2) && this.a(var2));
					
					var2.c = false;
					var2.a.d();
					clonedC.remove(var2); // Paperbin - use local c
				}
			} else {
				var1 = clonedC.iterator(); // Paperbin - use local c
				
				while (var1.hasNext()) {
					var2 = (net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem) var1.next();
					if (!this.a(var2)) {
						var2.c = false;
						var2.a.d();
						var1.remove();
					}
				}
			}
			// PAPERBIN START - Allow concurrent access of `c`
		} finally {
			this.c = clonedC; // Replace c
		}
		// PAPERBIN END - Allow concurrent access of `c`
		
		this.d.b();
	}
	
	public void tickGoals() {
		Iterator var1;
		net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem var2;
		Set<net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem> localC = this.c; // PAPERBIN - use local c reference in case field is changed on another thread
		if (!localC.isEmpty()) {
			this.d.a("goalTick");
			var1 = localC.iterator(); // PAPERBIN - use local c reference in case field is changed on another thread
			
			while(var1.hasNext()) {
				var2 = (net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem)var1.next();
				var2.a.e();
			}
			
			this.d.b();
		}
		// PAPERBIN END - Separate goal logic
	}
	
	private boolean a(net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem var1) {
		return var1.a.b();
	}
	
	private boolean b(net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem var1) {
		if (this.c.isEmpty()) {
			return true;
		} else if (this.b(var1.a.h())) {
			return false;
		} else {
			Iterator var2 = this.c.iterator();
			
			while(var2.hasNext()) {
				net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem var3 = (net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem)var2.next();
				if (var3 != var1) {
					if (var1.b >= var3.b) {
						if (!this.a(var1, var3)) {
							return false;
						}
					} else if (!var3.a.g()) {
						return false;
					}
				}
			}
			
			return true;
		}
	}
	
	private boolean a(net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem var1, net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem var2) {
		return (var1.a.h() & var2.a.h()) == 0;
	}
	
	public boolean b(int var1) {
		return (this.g & var1) > 0;
	}
	
	public void c(int var1) {
		this.g |= var1;
	}
	
	public void d(int var1) {
		this.g &= ~var1;
	}
	
	public void a(int var1, boolean var2) {
		if (var2) {
			this.d(var1);
		} else {
			this.c(var1);
		}
		
	}
	
	class PathfinderGoalSelectorItem {
		public final PathfinderGoal a;
		public final int b;
		public boolean c;
		
		public PathfinderGoalSelectorItem(int var2, PathfinderGoal var3) {
			this.b = var2;
			this.a = var3;
		}
		
		public boolean equals(@Nullable Object var1) {
			if (this == var1) {
				return true;
			} else {
				return var1 != null && this.getClass() == var1.getClass() ? this.a.equals(((net.minecraft.server.v1_12_R1.PathfinderGoalSelector.PathfinderGoalSelectorItem)var1).a) : false;
			}
		}
		
		public int hashCode() {
			return this.a.hashCode();
		}
	}
}
