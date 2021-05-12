package PigSpawnerFinder.spiderfinder;

import kaptainwutax.mcutils.util.math.DistanceMetric;

public class Spawner {
    public int x, y, z;
    public Direction direction;
    public int length;

    public Spawner(int x, int y, int z, Direction direction, int length) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.direction = direction;
        this.length = length;
    }

    public double getDistSq(Spawner spawner){
        return DistanceMetric.EUCLIDEAN_SQ.getDistance(this.x-spawner.x,this.y- spawner.y,this.z- spawner.z);
    }

    @Override
    public String toString() {
        return "Spawner{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", direction=" + direction +
                ", length=" + length +
                '}';
    }
}
