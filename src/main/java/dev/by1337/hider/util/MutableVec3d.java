package dev.by1337.hider.util;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MutableVec3d {
    public double x;
    public double y;
    public double z;

    public MutableVec3d() {

    }

    public MutableVec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public MutableVec3d(@NotNull Location location) {
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
    }
    public MutableVec3d set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }
    public MutableVec3d normalize() {
        double norm = 1.0 / length();
        x *= norm;
        y *= norm;
        z *= norm;
        return this;
    }
    public MutableVec3d sub(MutableVec3d vec) {
        x -= vec.x;
        y -= vec.y;
        z -= vec.z;
        return this;
    }
    public MutableVec3d add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }
    public MutableVec3d div(double n) {
        x /= n;
        y /= n;
        z /= n;
        return this;
    }
    public MutableVec3d set(MutableVec3d vec3d){
        x = vec3d.x;
        y = vec3d.y;
        z = vec3d.z;
        return this;
    }
    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }


    public boolean equals(double x, double y, double z) {
        return this.x == x && this.y == y && this.z == z;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutableVec3d that = (MutableVec3d) o;
        return Double.compare(x, that.x) == 0 && Double.compare(y, that.y) == 0 && Double.compare(z, that.z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "MutableVec3d{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
