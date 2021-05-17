package PigSpawnerFinder.spiderfinder;

import PigSpawnerFinder.spiderfinder.MineshaftGenerator;
import PigSpawnerFinder.spiderfinder.Spawner;
import PigSpawnerFinder.spiderfinder.Vec3i;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class Main {
	public static final int regionSide = 120;
	public static final int spawnerRegionRadius = regionSide / 2 + 8;

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter worldseed : ");
		String ws = scanner.nextLine();
		long worldSeed;
		try {
			worldSeed = Long.parseLong(ws);
		} catch (NumberFormatException e) {
			worldSeed = ws.hashCode();
			System.err.println("You inputted a wrong world seed, we converted it to a string one " + worldSeed);
		}
		System.out.println("Using worldseed : " + worldSeed);
		System.out.println("Enter minimum number of spawner you want : ");
		String sz = scanner.nextLine();
		int neededCount;
		try {
			neededCount = Integer.parseInt(sz);
		} catch (NumberFormatException e) {
			System.out.println("Sorry you inputed a wrong number (too large or something)");
			e.printStackTrace();
			return;
		}
		System.out.println("Enter half size to search for (in chunks) : ");
		sz = scanner.nextLine();
		int size;
		try {
			size = Integer.parseInt(sz);
		} catch (NumberFormatException e) {
			System.out.println("Sorry you inputed a wrong size (too large or something)");
			e.printStackTrace();
			return;
		}
		System.out.printf("Searching an area of %dx%d chunks\n",size*2,size*2);
		for (int chunkX = -size; chunkX < size; chunkX++) {
			int finalChunkX = chunkX;
			long finalWorldSeed = worldSeed;
			IntStream.range(-size, size).parallel().forEach(
					chunkZ -> run(finalWorldSeed, finalChunkX, chunkZ,neededCount)
			);
		}
		System.out.println("We are done, if you didn't see any result then something is wrong");
		System.out.println("Press any key to quit");
		String end = scanner.nextLine();


		//runNormal(neededCount, worldSeed, regionRadius, centerRegionX, centerRegionZ);


//		ArrayList<Spawner> spawners = new ArrayList<>();
//		for (int a = 0; a < 20000000; a++) MineshaftGenerator.generateForChunk(worldSeed, a, a, false, spawners);
//		//getSpawnersInRegion(worldSeed, 0, 0, spawners);
//
//
//		System.out.println("Finished in " + (System.currentTimeMillis() - start) / 1000.0f + "s");
	}

	public static void getSpawnersInArea(long worldSeed, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ, ArrayList<Spawner> spawners){
		for(int x = minChunkX; x <= maxChunkX; x++) {
			for(int z = minChunkZ; z <= maxChunkZ; z++) {
				MineshaftGenerator.generateForChunk(worldSeed, x, z, false, spawners);
			}
		}
	}

	public static void getSpawnersInRegion(long worldSeed, int regionX, int regionZ, ArrayList<Spawner> spawners) {
		int chunkX = regionX * regionSide;
		int chunkZ = regionZ * regionSide;
		int minChunkX = chunkX - spawnerRegionRadius;
		int maxChunkX = chunkX + spawnerRegionRadius;
		int minChunkZ = chunkZ - spawnerRegionRadius;
		int maxChunkZ = chunkZ + spawnerRegionRadius;
		for (int x = minChunkX; x <= maxChunkX; x++) {
			for (int z = minChunkZ; z <= maxChunkZ; z++) {
				MineshaftGenerator.generateForChunk(worldSeed, x, z, false, spawners);
			}
		}
	}

	public static void runNormal(int neededCount, long worldSeed, int regionRadius, int centerRegionX, int centerRegionZ){
		Map<Integer, ArrayList<Spawner>> found = new HashMap<>();
		int minRegionX = centerRegionX - regionRadius;
		int maxRegionX = centerRegionX + regionRadius;
		int minRegionZ = centerRegionZ - regionRadius;
		int maxRegionZ = centerRegionZ + regionRadius;
		for(int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
			for(int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
				ArrayList<Spawner> spawners = new ArrayList<>();
				int chunkX = regionX * regionSide;
				int chunkZ = regionZ * regionSide;

			}
		}

		for(int count = neededCount; count < 20; count++){
			if(found.containsKey(count)){
				for(Spawner spawner : found.get(count)) {
					System.out.printf("%d: FOUND /tp @p %d %d %d worldseed: %d\n",count,spawner.x,spawner.y,spawner.z,worldSeed);
				}
			}
		}
	}

	public static  void run(long worldSeed, int chunkX, int chunkZ,int neededCount){
		Map<Integer, ArrayList<Spawner>> found = new HashMap<>();
		ArrayList<Spawner> spawners = new ArrayList<>();
		MineshaftGenerator.generateForChunk(worldSeed, chunkX, chunkZ, false, spawners);
		for(Spawner spawner1 : spawners) {
			int count = 0;
			for(Spawner spawner2 : spawners) {
				if(spawner1.getDistSq(spawner2) < 484) count++;
			}

			if(count >= neededCount) {
				if(!found.containsKey(count)) found.put(count, new ArrayList<>());
				if(!found.get(count).contains(spawner1)){
					found.get(count).add(spawner1);
				}
			}
		}
		for(int count = neededCount; count < 20; count++){
			if(found.containsKey(count)){
				for(Spawner spawner : found.get(count)) {
					System.out.printf("%d: FOUND /tp @p %d %d %d worldseed: %d\n",count,spawner.x,spawner.y,spawner.z,worldSeed);
				}
			}
		}
	}

	public static void runThreaded(int neededCount, long worldSeed, int regionRadius, int centerRegionX, int centerRegionZ, int threadCount) {
		Map<Integer, List<Spawner>> found = new HashMap<>();
		int minRegionZ = centerRegionZ - regionRadius;
		int maxRegionZ = centerRegionZ + regionRadius;
		int threadRegionOffset = (int)Math.ceil(((double)(regionRadius * 2 + 1)) / threadCount);

		Thread[] threads = new Thread[threadCount];
		for(int threadId = 0; threadId < threadCount; threadId++){
			int threadID = threadId;

			threads[threadId] = new Thread(() -> {
				int minRegionX = centerRegionX - regionRadius + threadRegionOffset * threadID;
				int maxRegionX = minRegionX + threadRegionOffset - 1;
				if(threadID == threadCount - 1) maxRegionX = centerRegionX + regionRadius;
				for(int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
					if(threadID == 0) System.out.println(100.0f * (regionX - minRegionX) / threadRegionOffset + "%");
					for(int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
						ArrayList<Spawner> spawners = new ArrayList<>();
						int chunkX = regionX * regionSide;
						int chunkZ = regionZ * regionSide;
						getSpawnersInArea(worldSeed, chunkX - spawnerRegionRadius, chunkX + spawnerRegionRadius, chunkZ - spawnerRegionRadius, chunkZ + spawnerRegionRadius, spawners);

						for(Spawner spawner1 : spawners) {
							int count = 0;
							for(Spawner spawner2 : spawners) {
								if(spawner1.getDistSq(spawner2) < 484 && spawner1 != spawner2) count++;
							}

							if(count >= neededCount) {
								if(!found.containsKey(count)) found.put(count, new LinkedList<>());
								if(!found.get(count).contains(spawner1)){
									found.get(count).add(spawner1);
									System.out.println("Pos: " + spawner1.x  + " " + spawner1.y  + " " + spawner1.z + " Number: " + count);
								}
							}
						}
					}
				}
			});
			threads[threadId].start();
		}

		while(true) {
			boolean finished = true;
			for(Thread thread : threads) {
				if(thread.isAlive()) {
					finished = false;
					break;
				}
			}
			if(finished) break;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println("InterruptedException: " + e.getMessage());
			}
		}


		for(int count = neededCount; count < 30; count++) {
			if(found.containsKey(count)) {
				for(Spawner spawner : found.get(count)) {
					System.out.println("\nFOUND:");
					System.out.println("Pos: " + spawner.x  + " " + spawner.y  + " " + spawner.z + " Number: " + count);
				}
			}
		}
	}
}
