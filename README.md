# PaperBin 🗑️
[![Donate with Bitcoin](https://en.cryptobadges.io/badge/small/12fApkUEecKA8UP6PAiNrGP1d2mvr1XXk9)](https://en.cryptobadges.io/donate/12fApkUEecKA8UP6PAiNrGP1d2mvr1XXk9)
[![Downloads](https://img.shields.io/github/downloads/bytechef/paperbin/total?logo=github&logoColor=white)](https://github.com/bytechef/PaperBin/releases/latest)
[![Discord](https://img.shields.io/discord/658373639137132595?logo=discord&logoColor=white)](https://discord.gg/9wA2G8E)

An experiment at improving the performance of PaperMC. 

This project uses jvmti to modify Minecraft classes at runtime.
Because of this paperbin can even be run on top of modified versions of paper.

## Usage
Make sure you have read the License.
Then download paperbin and run the following command:
```
java [any jvm arguments] -jar PaperBin.jar ActualPaper.jar [any paper arguments]
```

### Post Java 8
It is highly recommended to run PaperBin on Java 15 using ZGC and large pages for best performance.

PaperBin itself will just work with Java 15, however PaperClip will not. If you use PaperClip normally, follow these steps to get it working on Java 15:
```
java -jar paperclip.jar
java -jar paperbin.jar cache/patched_1.12.2.jar
```
First time you run PaperClip it will create a patched 1.12.2 jar, then crash.
You can then use PaperBin to directly run the patched jar.

## Testomonials
> paperbin
> did lots of impovements to my server
> i can f\*\*\*ing handle 15k entities
> in 4 chunks
> without tps drop

(Kaspian#8508)

> paperbin runs great for a server with 100+ players and wont affect breaking blocks or eating that much when the tps is low would recommend

(cubebuilderunderscore#4344)

> thanks for making such a helpful server tool

(Gav#7669)


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
The first time you run PaperBin it will output the default config to `paperbin.properties`, just replace the defaults as necessary.

## Building

Paperbin needs the patched paper jar as a dependency, so you will need to follow these build steps.
```Bash
git clone https://github.com/x4e/PaperBin
cd PaperBin
mkdir paperbin
wget https://papermc.io/api/v2/projects/paper/versions/1.12.2/builds/1618/downloads/paper-1.12.2-1618.jar -O paperbin/paper.jar
cd paperbin
java -jar paper.jar
cd ../
./gradlew build
```


## YourKit
![YourKit](https://www.yourkit.com/images/yklogo.png)

Thank you YourKit for supporting this project with their amazing profiler.

YourKit supports open source projects with innovative and intelligent tools 
for monitoring and profiling Java and .NET applications.
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/),
[YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/),
and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).
