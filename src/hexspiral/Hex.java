package hexspiral;

import java.util.List;

public record Hex(int q, int r, int s) {
    private static final List<Hex> directionVectors = List.of(
            new Hex(+1, 0, -1), new Hex(+1, -1, 0), new Hex(0, -1, +1),
            new Hex(-1, 0, +1), new Hex(-1, +1, 0), new Hex(0, +1, -1)
    );

    public enum Direction {
        Right,
        TopRight,
        TopLeft,
        Left,
        BottomLeft,
        BottomRight
    }

    public static Hex direction(Direction direction) {
        return directionVectors.get(direction.ordinal());
    }

    public Hex scale(int factor) {
        return new Hex(q * factor, r * factor, s * factor);
    }

    public Hex add(Hex other) {
        return new Hex(q + other.q, r + other.r, s + other.s);
    }

    public Hex neighbor(Direction direction) {
        return add(Hex.direction(direction));
    }
}