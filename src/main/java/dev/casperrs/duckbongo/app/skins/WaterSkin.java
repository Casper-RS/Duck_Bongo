package dev.casperrs.duckbongo.app.skins;

public enum WaterSkin {
    DEFAULT ("water_default.png", "Default"),
    BLACK   ("water_black.png",   "Black"),
    BLUE    ("water_blue.png",    "Blue"),
    GREEN   ("water_green.png",   "Green"),
    PINK    ("water_pink.png",    "Pink"),
    PISS    ("water_piss.png",    "Piss"),
    RED     ("water_red.png",     "Red"),
    URANIUM ("water_uranium.png", "Uranium"),
    WHITE   ("water_white.png",   "White");

    public static final String DIR = "/assets/skin_parts/waters/";

    private final String fileName;
    private final String displayName;
    private final String resourcePath;

    WaterSkin(String fileName, String displayName) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.resourcePath = DIR + fileName;
    }

    public String fileName()     { return fileName; }
    public String displayName()  { return displayName; }
    public String resourcePath() { return resourcePath; }

    public static WaterSkin fromPath(String path) {
        if (path == null || path.isBlank()) return DEFAULT;
        for (WaterSkin s : values()) {
            if (path.endsWith(s.fileName)) return s;
        }
        return DEFAULT;
    }
}
