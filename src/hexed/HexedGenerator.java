package hexed;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import hexspiral.HexSpiral;
import mindustry.content.*;
import mindustry.maps.*;
import mindustry.maps.filters.*;
import mindustry.maps.filters.GenerateFilter.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class HexedGenerator implements Cons<Tiles> {
    public int width = Hex.size, height = Hex.size;

    // elevation --->
    // temperature
    // |
    // v
    Block[][] floors = {
            {Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.grass},
            {Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.grass, Blocks.grass},
            {Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.grass, Blocks.shale},
            {Blocks.darksandTaintedWater, Blocks.darksandTaintedWater, Blocks.moss, Blocks.moss, Blocks.sporeMoss, Blocks.stone},
            {Blocks.ice, Blocks.iceSnow, Blocks.snow, Blocks.dacite, Blocks.hotrock, Blocks.salt}
    };

    Block[][] blocks = {
            {Blocks.stoneWall, Blocks.stoneWall, Blocks.sandWall, Blocks.sandWall, Blocks.pine, Blocks.pine},
            {Blocks.stoneWall, Blocks.stoneWall, Blocks.duneWall, Blocks.duneWall, Blocks.pine, Blocks.pine},
            {Blocks.stoneWall, Blocks.stoneWall, Blocks.duneWall, Blocks.duneWall, Blocks.pine, Blocks.pine},
            {Blocks.sporeWall, Blocks.sporeWall, Blocks.sporeWall, Blocks.sporeWall, Blocks.sporeWall, Blocks.stoneWall},
            {Blocks.iceWall, Blocks.snowWall, Blocks.snowWall, Blocks.snowWall, Blocks.stoneWall, Blocks.saltWall}
    };

    @Override
    public void get(Tiles tiles) {
        int seed1 = Mathf.random(0, 10000), seed2 = Mathf.random(0, 10000);
        Seq<GenerateFilter> ores = new Seq<>();
        maps.addDefaultOres(ores);
        ores.each(o -> ((OreFilter) o).threshold -= 0.05f);
        ores.insert(0, new OreFilter() {{
            ore = Blocks.oreScrap;
            scl += 2 / 2.1F;
        }});
        ores.each(GenerateFilter::randomize);
        GenerateInput in = new GenerateInput();
        IntSeq hex = getHex();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int temp = Mathf.clamp((int) ((Simplex.noise2d(seed1, 12, 0.6, 1.0 / 400, x, y) - 0.5) * 10 * blocks.length), 0, blocks.length - 1);
                int elev = Mathf.clamp((int) (((Simplex.noise2d(seed2, 12, 0.6, 1.0 / 700, x, y) - 0.5) * 10 + 0.15f) * blocks[0].length), 0, blocks[0].length - 1);

                Block floor = floors[temp][elev];
                Block wall = blocks[temp][elev];
                Block ore = Blocks.air;

                for (GenerateFilter f : ores) {
                    in.floor = Blocks.stone;
                    in.block = wall;
                    in.overlay = ore;
                    in.x = x;
                    in.y = y;
                    in.width = in.height = Hex.size;
                    f.apply(in);
                    if (in.overlay != Blocks.air) {
                        ore = in.overlay;
                    }
                }

                tiles.set(x, y, new Tile(x, y, floor.id, ore.id, wall.id));
            }
        }

        for (int i = 0; i < hex.size; i++) {
            int x = Point2.x(hex.get(i));
            int y = Point2.y(hex.get(i));
            Geometry.circle(x, y, width, height, Hex.diameter, (cx, cy) -> {
                if (Intersector.isInsideHexagon(x, y, Hex.diameter, cx, cy)) {
                    Tile tile = tiles.getn(cx, cy);
                    tile.setBlock(Blocks.air);
                }
            });
            float angle = 360f / 3 / 2f - 90;
            for (int a = 0; a < 3; a++) {
                float f = a * 120f + angle;

                Tmp.v1.trnsExact(f, Hex.spacing + 12);
                if (Structs.inBounds(x + (int) Tmp.v1.x, y + (int) Tmp.v1.y, width, height)) {
                    Tmp.v1.trnsExact(f, Hex.spacing / 2 + 7);
                    Bresenham2.line(x, y, x + (int) Tmp.v1.x, y + (int) Tmp.v1.y, (cx, cy) -> {
                        Geometry.circle(cx, cy, width, height, 3, (c2x, c2y) -> tiles.getn(c2x, c2y).setBlock(Blocks.air));
                    });
                }
            }
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Tile tile = tiles.getn(x, y);
                Block wall = tile.block();
                Block floor = tile.floor();

                if (wall == Blocks.air) {
                    if (Mathf.chance(0.03)) {
                        if (floor == Blocks.sand) wall = Blocks.sandBoulder;
                        else if (floor == Blocks.stone) wall = Blocks.boulder;
                        else if (floor == Blocks.shale) wall = Blocks.shaleBoulder;
                        else if (floor == Blocks.darksand) wall = Blocks.boulder;
                        else if (floor == Blocks.moss) wall = Blocks.sporeCluster;
                        else if (floor == Blocks.ice) wall = Blocks.snowBoulder;
                        else if (floor == Blocks.snow) wall = Blocks.snowBoulder;
                    }
                }
                tile.setBlock(wall);
            }
        }

        state.map = new Map(StringMap.of("name", "Hex"));
    }

    public IntSeq getHex() {
        IntSeq array = new IntSeq();
        double h = Math.sqrt(3) * Hex.spacing / 2;
        return new IntSeq(HexSpiral.spiral(new Point2(400, 400), 2));
        //base horizontal spacing=1.5w
        //offset = 3/4w
//        for(int x = 0; x < width / Hex.spacing - 2; x++){
//            for(int y = 0; y < height / (h/2) - 2; y++){
//                int cx = (int)(x * Hex.spacing*1.5 + (y%2)* Hex.spacing*3.0/4) + Hex.spacing/2;
//                int cy = (int)(y * h / 2) + Hex.spacing/2;
//                array.add(Point2.pack(cx, cy));
//            }
//        }

//        int rings = 3;
//
//        for (int ring = 0; ring < rings; ring++) {
//            int circle;
//            int centerX = 400;
//            int centerY = 400;
//            double radius = 100 * ring;
//            if(ring == 0) circle = 1;
//            else circle = ring;
//            System.out.println("Ring: "+ring);
//            for (int i = 0; i < 6*circle; i++) {
//                System.out.println("Radius: "+(int)(i *60/circle)+30);
//                double angle_rad = Math.toRadians((int)(i *60/circle)+30);
//                System.out.println("angle rad: "+angle_rad);
//                int x = centerX + (int) (radius * Math.cos(angle_rad));
//                int y = centerY + (int) (radius * Math.sin(angle_rad));
//                array.add(Point2.pack(x, y));
//            }
//        }
//        return array;
//    }


//        array.add(Point2.pack(325, 443));
//        array.add(Point2.pack(400, 486));
//        array.add(Point2.pack(475, 443));
//        array.add(Point2.pack(475, 357));
//        array.add(Point2.pack(400, 314));
//        array.add(Point2.pack(325, 357));
//        array.add(Point2.pack(250, 486));
//        array.add(Point2.pack(325, 529));
//        array.add(Point2.pack(400, 573));
//        array.add(Point2.pack(475, 529));
//        array.add(Point2.pack(550, 486));
//        array.add(Point2.pack(550, 400));
//        array.add(Point2.pack(550, 314));
//        array.add(Point2.pack(475, 271));
//        array.add(Point2.pack(400, 227));
//        array.add(Point2.pack(325, 271));
//        array.add(Point2.pack(250, 314));
//        array.add(Point2.pack(250, 400));
//        array.add(Point2.pack(400, 400));
//
//        return array;
    }
}
