package PigSpawnerFinder.spiderfinder;

import PigSpawnerFinder.spiderfinder.MineshaftGenerator;
import PigSpawnerFinder.spiderfinder.Spawner;
import PigSpawnerFinder.spiderfinder.Vec3i;

import java.util.*;
import java.util.stream.LongStream;

public class Main {
	public static final int regionSide = 120;
	public static final int spawnerRegionRadius = regionSide / 2 + 8;

	public static void main(String[] args) {
		int centerX = 0;
		int centerZ = 0;
		int radius = 30;


//		Scanner scanner = new Scanner(System.in);
//		System.out.println("Enter world seed");
//		long worldSeed = scanner.nextLong();
//		System.out.println("Enter center X");
//		int centerX = scanner.nextInt();
//		System.out.println("Enter center Z");
//		int centerZ = scanner.nextInt();
//		System.out.println("Enter radius");
//		int radius = scanner.nextInt();
//		System.out.println("Enter number of spawners");
//		int neededCount = scanner.nextInt();
//		System.out.println("Enter number of threads");
//		int threadCount = scanner.nextInt();

		if (args.length<1){
			System.out.println("java -jar <File>.jar <start> (<neededCount>)");
			System.exit(1);
		}

		long starting = 0;
		int neededCount = 8;
		try {
			starting=Long.parseLong(args[0]);
			if (args.length>1){
				neededCount=Integer.parseInt(args[1]);
			}
		}catch (Exception e){
			e.printStackTrace();
			System.exit(1);
		}

		int regionRadius = (int)Math.ceil(radius / 16.0f / regionSide);
		int centerRegionX = Math.floorMod(centerX, 16 * regionSide);
		int centerRegionZ = Math.floorMod(centerZ, 16 * regionSide);

		System.out.println("Starting search");
		int finalNeededCount = neededCount;
		LongStream.range(starting,1L<<48).parallel().forEach(ws->runNormal(finalNeededCount,ws,regionRadius,centerRegionX,centerRegionZ));

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
				getSpawnersInArea(worldSeed, chunkX - spawnerRegionRadius, chunkZ + spawnerRegionRadius, chunkX - spawnerRegionRadius, chunkZ + spawnerRegionRadius, spawners);

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
