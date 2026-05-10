package dev.casperrs.duckbongo.app.skins;

public enum DuckSkin {
    DEFAULT         ("duck_default.png",        "Default"),
    BLACK           ("duck_black.png",          "Black"),
    DARK_BLUE       ("duck_darkBlue.png",       "Dark Blue"),
    GREEN           ("duck_green.png",          "Green"),
    GREEN_GUMMY     ("duck_greenGummy.png",     "Green Gummy"),
    LIGHT_BLUE      ("duck_lightBlue.png",      "Light Blue"),
    MIDNIGHT_PURPLE ("duck_midnightPurple.png", "Midnight Purple"),
    PURPLE          ("duck_purple.png",         "Purple"),
    RED             ("duck_red.png",            "Red"),
    URANIUM         ("duck_uranium.png",        "Uranium"),
    WHITE           ("duck_white.png",          "White");

    public static final String DIR = "/assets/skin_parts/ducks/";

    private final String fileName;
    private final String displayName;
    private final String resourcePath;

    DuckSkin(String fileName, String displayName) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.resourcePath = DIR + fileName;
    }

    public String fileName()     { return fileName; }
    public String displayName()  { return displayName; }
    public String resourcePath() { return resourcePath; }

    public static DuckSkin fromPath(String path) {
        if (path == null || path.isBlank()) return DEFAULT;
        for (DuckSkin s : values()) {
            if (path.endsWith(s.fileName)) return s;
        }
        return DEFAULT;
    }
}
