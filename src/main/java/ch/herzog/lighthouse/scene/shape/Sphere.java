package ch.herzog.lighthouse.scene.shape;

import ch.herzog.lighthouse.math.Point3d;
import ch.herzog.lighthouse.math.Vector3d;
import ch.herzog.lighthouse.render.Intersection;
import ch.herzog.lighthouse.render.Ray;

/**
 * A sphere
 */
public class Sphere extends Shape {

    /**
     * the radius
     */
    private double radius;

    /**
     * Constructs and initializes a Sphere from the specified position.
     *
     * @param position the position
     */
    public Sphere(Point3d position) {
        this(position, 1.0);
    }

    /**
     * Constructs and initializes a Sphere from the specified position and radius.
     *
     * @param position the position
     * @param radius   the radius
     */
    public Sphere(Point3d position, double radius) {
        super(position);
        this.radius = radius;
    }

    /**
     * Calculates the intersection between the sphere and the ray
     *
     * @param ray the ray
     * @return the intersection
     */
    @Override
    public Intersection getIntersection(Ray ray) {
        double a = ray.getDirection().dot(ray.getDirection());
        double b = 2.0 * ray.getDirection().dot(ray.getOrigin().toVector().subtract(position.toVector()));
        double c = ray.getOrigin().toVector().subtract(position.toVector())
                .dot(ray.getOrigin().toVector().subtract(position.toVector())) - radius * radius;

        double discriminant = b * b - 4.0 * a * c;

        if (discriminant < 0) {
            return null;
        } else {
            double time = (-b - Math.sqrt(discriminant)) / 2.0 * a;
            return new Intersection(ray, this, time);
        }
    }

    /**
     * Returns true if the sphere contains the point
     *
     * @param point the point
     * @return true if the sphere contains the point
     */
    @Override
    public boolean contains(Point3d point) {
        return new Vector3d(position, point).length() < radius;
    }

    /**
     * @return the radius
     */
    public double getRadius() {
        return radius;
    }

    /**
     * @param radius the radius
     */
    public void setRadius(double radius) {
        this.radius = radius;
    }

}