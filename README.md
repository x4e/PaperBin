# PaperBin 🗑️
An experiment at improving the performance of PaperMC. 

This project uses ASM and the attach api to modify Paper classes as they are defined.

Usage:
```
java [any jvm arguments] -noverify -jar PaperBin.jar ActualPaper.jar [any paper arguments]
```

PaperBin is 100% compatible with any forks of paper, so you dont have to use vanilla paper.

## Features:

##### Anti Chunk Ban
![](https://i.binclub.dev/ka562q74.png)
Splits oversized chunk NBT data into multiple packets

##### Anti Crasher
Attempts to prevent crash exploits related to oversized books

##### Anti Dupe
Various methods of decreasing the likelihood of an item being able to be duped. At the moment should stop death 
teleportation related dupes.

##### Anti Nether Roof
Prevents players from travelling above the nether roof, below nether/overworld bedrock, and outside of the world border
in all dimensions

##### Block Tick Rate Limiter
Limits how often certain blocks are ticked, for example leaves, which consume tick time peforming leaf decay
calculations

##### Chunk Loading Optimisations
Attempts to remove various bottlenecks during chunk loading

##### Faster Game Rule Lookup
Transforms game rules into a HashMap to give average O(1) lookup as opposed to the original O(N) tree map. Disadvantage
is that game rules are no longer alphabetically sorted

##### Food Tps Compensator
Compensates for slow tickrates when calculating player consumption (food, potions etc) and furnaces

##### Mob Ai Rate Limiter
Limits the rate at which Mobs reselect their goals, which at its original speed uses up a huge proportion of tick time

##### Village Rate Limiter
Rate limits how often villages update. This is used for stuff like iron golem spawning, so this can negatively impact
iron farms.
