package PigSpawnerFinder;

import PigSpawnerFinder.spiderfinder.*;
import kaptainwutax.biomeutils.biome.Biomes;
import kaptainwutax.biomeutils.source.OverworldBiomeSource;
import kaptainwutax.mcutils.rand.ChunkRand;
import kaptainwutax.mcutils.rand.seed.WorldSeed;
import kaptainwutax.mcutils.util.pos.BPos;
import kaptainwutax.mcutils.util.pos.CPos;
import kaptainwutax.mcutils.version.MCVersion;
import kaptainwutax.seedutils.lcg.LCG;
import kaptainwutax.terrainutils.terrain.OverworldChunkGenerator;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import static java.lang.System.out;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PigSpawnerFromWorldSeed {
	public static final MCVersion version = MCVersion.v1_16_5;
	public static int spawnerCount = 0;

	// this is non optimized
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);

		String line;
		long worldSeed;
		while(true){
			System.out.println("Enter worldseed : ");
			line = scanner.nextLine();
			try {
				worldSeed = Long.parseLong(line);
				break;
			} catch (NumberFormatException e) {
				System.out.println("Invalid seed, try again");
			}
		}
		System.out.println("Using worldseed : " + worldSeed);

		int sizea;
		while(true){
			System.out.println("Enter the inner radius of the ring to search for (in chunks) [inclusive, 1 to 1,875,000]: ");
			line = scanner.nextLine();
			try {
				sizea = Integer.parseInt(line);
				if(sizea >= 1 && sizea <= 1875000) break;
				else System.out.println("Number out of bounds, try again");
			} catch (NumberFormatException e) {
				System.out.println("Invalid number, try again");
			}
		}

		int sizeb;
		while(true){
			System.out.println("Enter the outer radius of the ring to search for (in chunks) [inclusive, 1 to 1,875,000]: ");
			line = scanner.nextLine();
			try {
				sizeb = Integer.parseInt(line);
				if(sizeb >= 1 && sizeb <= 1875000) break;
				else if (sizeb < sizea) System.out.println("Outer radius cannot be smaller than inner radius, try again");
				else System.out.println("Number out of bounds, try again");
			} catch (NumberFormatException e) {
				System.out.println("Invalid number, try again");
			}
		}

		boolean quintuple;
		while(true){
			System.out.println("Do you want to find only quintuple clusters or singles too? [1=quintuple, 0=single]");
			line = scanner.nextLine();
			try {
				quintuple = Integer.parseInt(line) == 1;
				if(Integer.parseInt(line) == 0 || Integer.parseInt(line) == 1) break;
				else System.out.println("Number isn't 0 or 1, try again");
			} catch (NumberFormatException e) {
				System.out.println("Invalid number, try again");
			}
		}

		System.out.printf("\nSearching an area of %dx%d (outer radius) minus %dx%d (inner radius) chunks",
			sizeb * 2, sizeb * 2, sizea * 2 - 2, sizea * 2 - 2);
		System.out.println(" for " + (quintuple ? "quintuple" : "single") + " pig spawners");

		System.out.println("Total chunks to search: " + ((2 * sizeb) * (2 * sizeb) -
			(2 * sizea - 2) * (2 * sizea - 2)));

		int totalThreads = Runtime.getRuntime().availableProcessors();
		int threads;
		while(true){
			System.out.printf("\nNumber of threads to use [1 to %d]: \n", totalThreads);
			try {
				line = scanner.nextLine();
				threads = Integer.parseInt(line);
				if(threads >= 1 && threads <= totalThreads){
					break;
				} else System.out.println("Number out of bounds, try again");
			} catch (NumberFormatException e) {
				System.out.println("Invalid number, try again");
			}
		}
		
		scanner.close();
		System.out.printf("Using %d out of the %d available threads\n", threads, totalThreads);

		for(int y = sizea;y <= sizeb;y++){
			if(y % 1000 == 0 || y == sizea) System.out.println("Checking layer " + y);
			int[][] chunks = new int[ 2 * y * 2 * y - ( 2 * y - 2) * (2 * y - 2)][2];
			int i = 0;
			for(int x = -y; x < y - 1; x++){
				chunks[i][0]= x;
				chunks[i][1]= -y;
				i++;
			}
			for(int z = -y + 1; z < y; z++){
				chunks[i][0]= -y;
				chunks[i][1]= z;
				i++;
			}
			for(int x = -y + 1; x < y; x++){
				chunks[i][0]= x;
				chunks[i][1]= y-1;
				i++;
			}
			for(int z = -y; z < y - 1; z++){
				chunks[i][0]= y-1;
				chunks[i][1]= z;
				i++;
			}
			long finalWorldSeed = worldSeed;
			boolean quintuple_ = quintuple;
			final ExecutorService exec = Executors.newFixedThreadPool(threads);
			for(final int[] chunk : chunks) {
				exec.submit(() -> processForChunk(finalWorldSeed, chunk[0],chunk[1], quintuple_));
			}
			exec.shutdown();
			while(!exec.isTerminated()) Thread.yield();
		}
		System.out.printf("Pig spawner finder finished, found %d pig spawners.\n", spawnerCount);
		System.out.println("If spawners were found, they will be in the \"PigSpawnerResult.txt\" file in the same directory as this .jar");
	}

	public static void processForChunk(long worldSeed, int chunkX, int chunkZ, boolean quintuple) {
		ArrayList<Spawner> spawners = new ArrayList<>();

		// get the spawner from the mineshaft
		ArrayList<StructurePiece> pieces = MineshaftGenerator.generateForChunk(
				WorldSeed.toStructureSeed(worldSeed),
				chunkX, chunkZ, true, spawners
		);

		for (Spawner spawner : spawners) {
			//Check spawner height
			if (spawner.y < 58 || spawner.y > 59) continue;

			//Check if it can be at 9 9 but not near supports
			int spawnerChunkX = spawner.x >> 4;
			int spawnerChunkZ = spawner.z >> 4;
			int spawnerOffsetX = spawner.x & 15;
			int spawnerOffsetZ = spawner.z & 15;
			boolean conditionX = spawner.direction.axis == Direction.Axis.X && spawnerOffsetZ == 9;
			boolean conditionZ = spawner.direction.axis == Direction.Axis.Z && spawnerOffsetX == 9;
			if (conditionX || conditionZ) {
					//Check if it isn't too close to mesa
					if (Math.abs(spawnerChunkX) < 2 && Math.abs(spawnerChunkZ) < 2) continue;

					if(quintuple){
						if (!(conditionX && (spawnerOffsetX == 8 || spawnerOffsetX == 10) ||
						conditionZ && (spawnerOffsetZ == 8 || spawnerOffsetZ == 10))) continue;

						//Check if there are no other corridors in the same chunk generated before the one with the spawner (meaning no random calls before our corridor)
						int piecesBeforeSpawner = 0;
						BlockBox spawnerBox = new BlockBox(spawner.x, spawner.y, spawner.z, spawner.x, spawner.y, spawner.z);
						BlockBox chunk = new BlockBox(spawnerChunkX << 4, 0, spawnerChunkZ << 4, (spawnerChunkX << 4) + 15, 255, (spawnerChunkZ << 4) + 15);
						for (StructurePiece piece : pieces) {
							if (piece.boundingBox.intersects(chunk)) {
								if (piece.boundingBox.intersects(spawnerBox)) break;
								else if (piece instanceof MineshaftGenerator.MineshaftCorridor) {
									piecesBeforeSpawner = 1;
									break;
								}
							}
						}
						if (piecesBeforeSpawner != 0) continue;
					}

					// System.out.println("STEP 1: Spawner could work for : " + spawner);
					findOutIfCorrect(worldSeed, spawner, chunkX, chunkZ, quintuple);
			}
		}
	}

	public static void findOutIfCorrect(long worldseed, Spawner spawner, int chunkX, int chunkZ, boolean quintuple) {
		ChunkRand rand = new ChunkRand();

		// everything here can be handled with the structure seed only (the lower 48 bits)
		long structureSeed = WorldSeed.toStructureSeed(worldseed);

		int m = spawner.length * 5;
		LCG skipCeiling = LCG.JAVA.combine(m * 3L);
		LCG skipCobwebs = LCG.JAVA.combine(m * 3L * 2L);
		LCG skip2 = LCG.JAVA.combine(2);
		LCG skip8 = LCG.JAVA.combine(8);
		LCG skip3 = LCG.JAVA.combine(3);

		int spawnerChunkX = spawner.x >> 4;
		int spawnerChunkZ = spawner.z >> 4;

		int spawnerOffsetX = spawner.x & 15;
		int spawnerOffsetZ = spawner.z & 15;
		int spawnerOffset = spawner.direction.axis == Direction.Axis.X ? spawnerOffsetX : spawnerOffsetZ;

		//Check for buried treasure
		rand.setRegionSeed(structureSeed, spawnerChunkX, spawnerChunkZ, 10387320, version);
		if (rand.nextFloat() >= 0.01F) return;

		//Check for cobwebs and spawner position
		//The spawner piece is the first corridor piece generated in this chunk so there are no random calls before it
		// the index and step are super specific to 1.16 (please document yourself)
		rand.setDecoratorSeed(structureSeed, spawnerChunkX << 4, spawnerChunkZ << 4, 0, 3, version);

		//   skip ceiling air blocks
		rand.advance(skipCeiling);
		//   skip cobwebs
		rand.advance(skipCobwebs);
		//   skip supports
		if (rand.nextInt(4) != 0) {
			rand.advance(skip2);
		}
		//   skip additional cobwebs
		rand.advance(skip8);
		//   skip chests
		for (int i = 0; i < 2; i++) {
			if (rand.nextInt(100) == 0) {
				rand.advance(skip3);
			}
		}

		int spawnerShift = rand.nextInt(3) - 1;

		if(quintuple) {
			int spawnerShiftReal = spawner.direction == Direction.NORTH || spawner.direction == Direction.WEST ? -spawnerShift : spawnerShift;
			if (spawnerOffset + spawnerShiftReal != 9) return;
		}

		//Check for no cobwebs near the spawner
		// the index and step are super specific to 1.16 (please document yourself)
		rand.setDecoratorSeed(structureSeed, spawnerChunkX << 4, spawnerChunkZ << 4, 0, 3, MCVersion.v1_16);
		rand.advance(skipCeiling);

		int cobwebsNearby = 0;
		for (int y = 0; y < 2/* && !hasCobwebsNearby*/; y++) {
			for (int x = 0; x < 3/* && !hasCobwebsNearby*/; x++) {
				for (int z = 0; z < m; z++) {
					boolean hasCobweb = rand.nextFloat() < 0.6F;
					if (hasCobweb) {
						if (
								(y == 1 && x == 1 && z == (2 + spawnerShift)) ||
										(y == 0 && x == 2 && z == (2 + spawnerShift)) ||
										(y == 0 && x == 0 && z == (2 + spawnerShift)) ||
										(y == 0 && x == 1 && z == (2 + spawnerShift + 1)) ||
										(y == 0 && x == 1 && z == (2 + spawnerShift - 1))
						) {
							cobwebsNearby ++;
							if (quintuple) return;
						}
					}
				}
			}
		}
		if (cobwebsNearby == 5) return;

		BPos spawnerPos = new BPos((spawnerChunkX << 4) + 9, spawner.y, (spawnerChunkZ << 4) + 9);
		// System.out.println("STEP 2: Spawner passed the buried treasure test : " + spawner);
		processWorldSeed(worldseed, spawnerPos, chunkX, chunkZ, quintuple);

	}

	private static final Set<Integer> BADLANDS = new HashSet<Integer>() {{
		add(Biomes.BADLANDS.getId());
		add(Biomes.BADLANDS_PLATEAU.getId());
		add(Biomes.ERODED_BADLANDS.getId());
		add(Biomes.MODIFIED_BADLANDS_PLATEAU.getId());
		add(Biomes.MODIFIED_WOODED_BADLANDS_PLATEAU.getId());
		add(Biomes.WOODED_BADLANDS_PLATEAU.getId());
	}};


	public static void processWorldSeed(long worldSeed, BPos spawnerPos, int chunkX, int chunkZ, boolean quintuple) {
		OverworldBiomeSource biomeSource;
		OverworldChunkGenerator chunkGenerator;
		CPos spawnerChunkPos = spawnerPos.toChunkPos();

		//Check biomes
		biomeSource = new OverworldBiomeSource(version, worldSeed);

		// those two checks are super intensive, that's why we do it at last
		if (!BADLANDS.contains(biomeSource.getBiomeForNoiseGen((chunkX << 2) + 2, 0, (chunkZ << 2) + 2).getId())) return;
		if (biomeSource.getBiomeForNoiseGen((spawnerChunkPos.getX() << 2) + 2, 0, (spawnerChunkPos.getZ() << 2) + 2) != Biomes.BEACH) return;
		out.println("Passed biome check: " + spawnerPos.getX() + ", " + spawnerPos.getZ());

		//Check depth above spawner
		chunkGenerator = new OverworldChunkGenerator(biomeSource);
		int height = chunkGenerator.getHeightInGround(spawnerPos.getX(), spawnerPos.getZ());
		int depth = height - spawnerPos.getY();
		if (depth < 3 || depth > 6) return;
		out.println("Passed depth check: " + spawnerPos.getX() + ", " + spawnerPos.getZ());

		//Check depth nearby to avoid water
		boolean good = true;
		for (int ox = -1; ox <= 1 && good; ox++) {
			for (int oz = -1; oz <= 1; oz++) {
				height = chunkGenerator.getHeightInGround(spawnerPos.getX() + ox * 4, spawnerPos.getZ() + oz * 4);
				depth = height - spawnerPos.getY();
				if (depth < 3 && height < 62) {
					good = false;
					break;
				}
			}
		}
		if (!good) return;

		System.out.printf("STEP 3 (FINAL) : Found spawner : /tp @p %d %d %d for worldseed %d\n",
				spawnerPos.getX(), spawnerPos.getY(), spawnerPos.getZ(), worldSeed);
		spawnerCount++;
		File file = new File("PigSpawnerResult.txt");
		try {
			boolean ignored = file.createNewFile();
			FileWriter writer = new FileWriter(file, true);
			writer.write(String.format("STEP 3 (FINAL) : Found spawner : /tp @p %d %d %d for worldseed %d\n",
					spawnerPos.getX(), spawnerPos.getY(), spawnerPos.getZ(), worldSeed));
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
