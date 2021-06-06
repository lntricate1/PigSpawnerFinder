package PigSpawnerFinder;

import PigSpawnerFinder.spiderfinder.*;
import kaptainwutax.biomeutils.biome.Biome;
import kaptainwutax.biomeutils.biome.Biomes;
import kaptainwutax.biomeutils.source.BiomeSource;
import kaptainwutax.biomeutils.source.OverworldBiomeSource;
import kaptainwutax.mcutils.rand.ChunkRand;
import kaptainwutax.mcutils.rand.seed.WorldSeed;
import kaptainwutax.mcutils.state.Dimension;
import kaptainwutax.mcutils.util.data.Pair;
import kaptainwutax.mcutils.util.pos.BPos;
import kaptainwutax.mcutils.util.pos.CPos;
import kaptainwutax.mcutils.version.MCVersion;
import kaptainwutax.seedutils.lcg.LCG;
import kaptainwutax.terrainutils.ChunkGenerator;
import kaptainwutax.terrainutils.terrain.OverworldChunkGenerator;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class ChunkKernel implements Runnable {


	private final BiomeSource biomeSource;
	private final ChunkGenerator chunkGenerator;
	private final int startX;
	private final int startZ;
	private final int stride;
	private final int threadId;
	private final MCVersion version;

	public ChunkKernel(MCVersion mcVersion, Dimension dimension, long worldseed, int startX, int startZ, int stride, int threadId) {
		this.biomeSource = BiomeSource.of(dimension, mcVersion, worldseed);
		this.chunkGenerator = ChunkGenerator.of(dimension, biomeSource);
		this.version=mcVersion;
		this.startX = startX;
		this.startZ = startZ;
		this.stride = stride;
		this.threadId = threadId;
	}

	public void execute(BiomeSource biomeSource, int chunkX, int chunkZ) {
		processForChunk(biomeSource.getWorldSeed(), chunkX, chunkZ);
	}



	public void processForChunk(long worldSeed, int chunkX, int chunkZ) {
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
			if (spawner.direction.axis == Direction.Axis.X && spawnerOffsetZ == 9 && (spawnerOffsetX == 8 || spawnerOffsetX == 10) ||
					spawner.direction.axis == Direction.Axis.Z && spawnerOffsetX == 9 && (spawnerOffsetZ == 8 || spawnerOffsetZ == 10)) {

				//Check if it isn't too close to mesa
				if (Math.abs(spawnerChunkX) < 2 && Math.abs(spawnerChunkZ) < 2) continue;

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

				System.out.println("STEP 1: Spawner could work for : " + spawner);
				findOutIfCorrect(worldSeed, spawner, chunkX, chunkZ);
			}
		}
	}

	public void findOutIfCorrect(long worldseed, Spawner spawner, int chunkX, int chunkZ) {
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
		int spawnerShiftReal = spawner.direction == Direction.NORTH || spawner.direction == Direction.WEST ? -spawnerShift : spawnerShift;
		if (spawnerOffset + spawnerShiftReal != 9) return;

		//Check for no cobwebs near the spawner
		// the index and step are super specific to 1.16 (please document yourself)
		rand.setDecoratorSeed(structureSeed, spawnerChunkX << 4, spawnerChunkZ << 4, 0, 3, version);
		rand.advance(skipCeiling);

		boolean hasCobwebsNearby = false;
		for (int y = 0; y < 2 && !hasCobwebsNearby; y++) {
			for (int x = 0; x < 3 && !hasCobwebsNearby; x++) {
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
							hasCobwebsNearby = true;
							break;
						}
					}
				}
			}
		}
		if (hasCobwebsNearby) return;

		BPos spawnerPos = new BPos((spawnerChunkX << 4) + 9, spawner.y, (spawnerChunkZ << 4) + 9);
		System.out.println("STEP 2: Spawner passed the buried treasure test : " + spawner);
		processWorldSeed(worldseed, spawnerPos, chunkX, chunkZ);

	}

	private static final Set<Integer> BADLANDS = new HashSet<Integer>() {{
		add(Biomes.BADLANDS.getId());
		add(Biomes.BADLANDS_PLATEAU.getId());
		add(Biomes.ERODED_BADLANDS.getId());
		add(Biomes.MODIFIED_BADLANDS_PLATEAU.getId());
		add(Biomes.MODIFIED_WOODED_BADLANDS_PLATEAU.getId());
		add(Biomes.WOODED_BADLANDS_PLATEAU.getId());
	}};


	public void processWorldSeed(long worldSeed, BPos spawnerPos, int chunkX, int chunkZ) {
		CPos spawnerChunkPos = spawnerPos.toChunkPos();

		//Check biomes
		// those two checks are super intensive, that's why we do it at last
		if (!BADLANDS.contains(biomeSource.getBiomeForNoiseGen((chunkX << 2) + 2, 0, (chunkZ << 2) + 2).getId())) return;
		if (biomeSource.getBiomeForNoiseGen((spawnerChunkPos.getX() << 2) + 2, 0, (spawnerChunkPos.getZ() << 2) + 2) != Biomes.BEACH) return;
//            System.out.println("Good biomes: " + worldSeed);

		//Check depth above spawner
		int height = chunkGenerator.getHeightInGround(spawnerPos.getX(), spawnerPos.getZ());
		int depth = height - spawnerPos.getY();
		if (depth < 3 || depth > 6) return;

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

	@Override
	public void run() {
		int precision = 1000;
		int percent = stride / precision;
		long startTime = System.nanoTime();
		int current = 0;
		double average = 0;
		for (int x = 0; x < stride; x++) {
			for (int z = 0; z < stride; z++) {
				execute(this.biomeSource, startX + x, startZ + z);
			}
			if (threadId == 0 && (x % percent) == 0) {
				long currentTime = System.nanoTime();
				double seconds = (currentTime - startTime) / 1e9;
				int minutesLeft = (int) ((precision - current) * seconds / current / 60);
				System.out.printf("%f%% done in %.2f seconds, ETA: %d hours %d min %n", x / (double) stride * 100, seconds, minutesLeft / 60, minutesLeft % 60);
				current++;
			}
		}
	}
}
