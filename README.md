# PaperBin 🗑️
[![Donate with Bitcoin](https://en.cryptobadges.io/badge/small/12fApkUEecKA8UP6PAiNrGP1d2mvr1XXk9)](https://en.cryptobadges.io/donate/12fApkUEecKA8UP6PAiNrGP1d2mvr1XXk9)
[![Downloads](https://img.shields.io/github/downloads/cookiedragon234/paperbin/total?logo=github&logoColor=white)](https://github.com/cookiedragon234/PaperBin/releases/latest)
[![Discord](https://img.shields.io/discord/658373639137132595?logo=discord&logoColor=white)](https://discord.gg/9wA2G8E)

An experiment at improving the performance of PaperMC. 

This project uses ASM and the attach api to modify Paper classes as they are defined.
Because of this paperbin can even **be run on top of other paperforks**!

## Usage
Make sure you have read the License.
Then download paperbin and run the following command **using a version 8 JVM**, ideally either hotspot, openjdk or graal:
```
java [any jvm arguments] -noverify -jar PaperBin.jar ActualPaper.jar [any paper arguments]
```

## Test Server
This plugin is currently active on `oldfag.org`, feel free to connect and test it out, there are no rules.

## Features:
- Multithreaded mob ai goal calculation
- Anti Chunk Ban
- Anti Crasher (book exploit)
- Anti Dupe (portal entity exploit)
- Anti Elytra Fly (infinite durability exploit)
- Anti Grief (various exploits to break end portals)
- Anti Nether Roof (prevent people glitching above/below bedrock or outside the world border)
- Anti Portal God Mode (exploit allowing invincibility after travelling through a portal)
- Anti Unicode Chat
- Block Tick Rate Limiter (limit the rate at which blocks are ticked)
- Chunk Loading Optimisations (remove some unecessary locks)
- Faster Game Rule Lookups (average O(1) vs original O(n))
- Mob AI Rate Limiting
- Optimised everyone sleeping checks
- Tps Compensation for events such as food/potion consumption, furnace/brewing progress etc
- Village rate limiter (rate limit the rate at which villages are updated)

## Configuration
Configuration is stored in `paperbin.properties`, here is an example:
```properties
antiCrasher=true
chunkLoadOptimisations=true
antiPortalGodmode=true
villageRateLimit=true
antiNetherRoof=true
antiChunkBan=true
antiUnicodeChat=true
antiElytraFly=true
tpsCompensation=true
antiGrief=true
fastGameRule=true
packetOptimisations=true
blockRateLimit=true
debug=false
optimisedEveryoneSleeping=true
antiDupe=true
mobAiMultithreading=true
mobAiRateLimit=false
antiNewChunks=false
```


## YourKit
![YourKit](https://www.yourkit.com/images/yklogo.png)

Thank you YourKit for supporting this project with their amazing profiler.

YourKit supports open source projects with innovative and intelligent tools 
for monitoring and profiling Java and .NET applications.
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/),
[YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/),
and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).
