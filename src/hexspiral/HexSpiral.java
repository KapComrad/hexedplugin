package hexspiral;

import arc.math.geom.Point2;
import java.util.ArrayList;
import java.util.List;

public class HexSpiral {
    private static final int size = 50;

    public static int[] spiral(Point2 center, int radius) {
        return spiral(new Hex(0, 0, 0), radius)
                .stream()
                .map(HexSpiral::hexToPixel)
                .map(point -> Point2.pack(point.x + center.x, point.y + center.y))
                .mapToInt(id -> id).toArray();
    }

    private static List<Hex> spiral(Hex center, int radius) {
        var results = new ArrayList<Hex>();
        results.add(new Hex(0, 0, 0));

        for (int i = 1; i <= radius; i++) {
            results.addAll(ring(center, i));
        }
        
        return results;
    }

    private static List<Hex> ring(Hex center, int radius) {
        var results = new ArrayList<Hex>();
        var hex = center.add(Hex.direction(Hex.Direction.BottomLeft).scale(radius));

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < radius; j++) {
                results.add(hex);
                hex = hex.neighbor(Hex.Direction.values()[i]);
            }
        }

        return results;
    }

    private static Point2 hexToPixel(Hex hex) {
        var x = size * 3. / 2. * hex.q();
        var y = size * (((Math.sqrt(3) / 2.) * hex.q()) + Math.sqrt(3) * hex.r());

        return new Point2((int) x, (int) y);
    }
}