# PigSpawnerFinder
Finds seeds with pig spawners

Based on the code by Andrew https://github.com/Gaider10/PigSpawnerFinder

Head to [Releases](https://github.com/hube12/PigSpawnerFinder/releases) to download the .exe

You can double click the .exe to run the console and input the seed and the size or use the jar 
as following : `java -jar PigSpawnerFinder-1.0.0.jar` 


Example of input sequence (<<< means we entered a sequence of character)

```shell
Enter worldseed :
<<< -2464334185163669257
Using worldseed : -2464334185163669257
Enter half size to search for (in chunks) :
<<< 1200
Searching an area of 2400x2400 chunks
STEP 1: Spawner could work for : Spawner{x=6105, y=58, z=18968, direction=NORTH, length=3}
STEP 2: Spawner passed the buried treasure test : Spawner{x=6105, y=58, z=18968, direction=NORTH, length=3}
STEP 3 (FINAL) : Found spawner : /tp @p 6105 58 18969 for worldseed -2464334185163669257
STEP 1: Spawner could work for : Spawner{x=8473, y=58, z=-4120, direction=NORTH, length=4}
We are done, if you didn't see any STEP X (from 1 to 3, 3 being the final true one)
Then you have a non possible worldseed or a too small of an area, remember those are rare.
Press any key to quit
<<< Enter
```