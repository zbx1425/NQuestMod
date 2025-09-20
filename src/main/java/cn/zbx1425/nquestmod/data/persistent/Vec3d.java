package cn.zbx1425.nquestmod.data.persistent;

public class Vec3d {
    public final double x, y, z;

    public Vec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean isWithin(Vec3d min, Vec3d max) {
        return this.x >= min.x && this.x <= max.x &&
               this.y >= min.y && this.y <= max.y &&
               this.z >= min.z && this.z <= max.z;
    }
}
