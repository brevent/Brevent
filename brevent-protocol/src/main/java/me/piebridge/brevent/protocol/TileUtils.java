package me.piebridge.brevent.protocol;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by thom on 2017/3/18.
 */

public class TileUtils {

    private static final String CUSTOM_TILE_PREFIX = "custom(";

    private static final int CUSTOM_TILE_LENGTH = 7;


    private TileUtils() {

    }

    public static Collection<String> parseTiles(String tiles) {
        Collection<String> packageNames = new ArrayList<>();
        for (String tile : tiles.split(",")) {
            // custom(com.github.shadowsocks/.ShadowsocksTileService)
            if (tile.startsWith(CUSTOM_TILE_PREFIX) && tile.endsWith(")")) {
                int index = tile.indexOf('/');
                if (index > 0) {
                    packageNames.add(tile.substring(CUSTOM_TILE_LENGTH, index));
                }
            }
        }
        return packageNames;
    }

}
