package com.aghajari.touchview;

import android.graphics.Path;

public class PathData {

    public final static Path[] paths;
    public static Path selected = null;
    public static boolean helperArrowsEnabled = true;

    static {
        paths = new Path[]{
                createSimplePathWithArc(),
                createHeart()
        };
    }

    private static Path createSimplePathWithArc() {
        Path path = new Path();
        path.lineTo(500, 0);
        path.lineTo(500, 500);
        path.lineTo(0, 500);
        path.arcTo(0, 500, 500, 1000, 180, -180, false);
        return path;
    }

    // https://stackoverflow.com/a/41251829/9187189
    private static Path createHeart() {
        Path path = new Path();
        float width = 800;
        float height = 800;

        // Starting point
        path.moveTo(width / 2, height / 5);

        // Upper left path
        path.cubicTo(5 * width / 14, 0,
                0, height / 15,
                width / 28, 2 * height / 5);

        // Lower left path
        path.cubicTo(width / 14, 2 * height / 3,
                3 * width / 7, 5 * height / 6,
                width / 2, height);

        // Lower right path
        path.cubicTo(4 * width / 7, 5 * height / 6,
                13 * width / 14, 2 * height / 3,
                27 * width / 28, 2 * height / 5);

        // Upper right path
        path.cubicTo(width, height / 15,
                9 * width / 14, 0,
                width / 2, height / 5);
        return path;
    }
}
