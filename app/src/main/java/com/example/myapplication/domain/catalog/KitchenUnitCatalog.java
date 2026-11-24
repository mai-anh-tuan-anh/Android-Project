package com.example.myapplication.domain.catalog;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Canonical cooking units and safe same-dimension conversions for module 7. */
public final class KitchenUnitCatalog {
    private static final Map<String, Unit> UNITS = new HashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();
    private static final Set<String> AMBIGUOUS_ALIASES = new HashSet<>();

    static {
        add("MG", "Miligam", "mg", "milligram", "milligrams", "milli");
        add("G", "Gam", "g", "gram", "grams", "gr", "gam");
        add("KG", "Kilogam", "kg", "kilogram", "kilograms", "kilo", "ký");
        add("OZ", "Ounce", "oz", "ounce", "ounces");
        add("LB", "Pound", "lb", "pound", "pounds");
        add("ML", "Mililit", "ml", "milliliter", "milliliters", "millilitre", "millilitres", "cc");
        add("L", "Lít", "l", "liter", "liters", "litre", "litres");
        add("TSP", "Teaspoon", "tsp", "teaspoon", "tea spoon");
        add("TBSP", "Tablespoon", "tbsp", "tablespoon", "table spoon");
        add("CUP", "Cup", "cup", "cups");
        add("FLOZ", "Fluid ounce", "fl oz", "fluid ounce", "fluid ounces");
        add("PCS", "Cái", "cái", "chiếc", "piece", "pieces");
        add("ITEM", "Đơn vị", "đơn vị", "unit", "units");
        add("PIECE", "Miếng", "miếng");
        add("SLICE", "Lát", "lát", "slice", "slices");
        add("CLOVE", "Tép", "tép", "clove", "cloves");
        add("BULB", "Củ", "củ", "bulb", "bulbs");
        add("BUNCH", "Bó", "bó", "bunch", "bunches");
        add("SPRIG", "Nhánh", "nhánh", "sprig", "sprigs");
        add("LEAF", "Lá", "lá", "leaf", "leaves");
        add("EGG", "Quả trứng", "quả trứng", "egg", "eggs");
        add("CAN", "Lon", "lon", "can", "cans");
        add("BOTTLE", "Chai", "chai", "bottle", "bottles");
        add("PACK", "Gói", "gói", "pack", "packs");
        add("BOX", "Hộp", "hộp", "box", "boxes");
        add("BAG", "Túi", "túi", "bag", "bags");
        add("STICK", "Thanh", "thanh", "stick", "sticks");
        add("PINCH", "Nhúm", "nhúm", "pinch", "pinches");
        add("DASH", "Một ít", "dash", "a dash", "dash");
        add("MUONG_CA_PHE", "Muỗng cà phê", "muỗng cà phê", "muỗng cafe", "thìa cà phê");
        add("MUONG_CANH", "Muỗng canh", "muỗng canh", "thìa canh");
        add("BAT", "Bát", "bát");
        add("CHEN", "Chén", "chén");
        add("TO", "Tô", "tô");
        add("LY", "Ly", "ly");
        add("COC", "Cốc", "cốc");
        add("DIA", "Đĩa", "đĩa");
        add("NAM", "Nắm", "nắm");
        add("NHUM", "Nhúm", "nhúm");
        add("LAT", "Lát", "lát");
        add("KHOANH", "Khoanh", "khoanh");
        add("KHUC", "Khúc", "khúc");
        add("QUA", "Quả", "quả");
        add("TRAI", "Trái", "trái");
    }

    private KitchenUnitCatalog() { }

    private static void add(String id, String name, String symbol, String... aliases) {
        Unit unit = new Unit(id, name, symbol);
        UNITS.put(id, unit);
        registerAlias(id, id);
        registerAlias(name, id);
        registerAlias(symbol, id);
        for (String alias : aliases) registerAlias(alias, id);
    }

    private static void registerAlias(String alias, String id) {
        String key = normalize(alias);
        if (AMBIGUOUS_ALIASES.contains(key)) return;
        String previous = ALIASES.get(key);
        if (previous != null && !previous.equals(id)) {
            ALIASES.remove(key);
            AMBIGUOUS_ALIASES.add(key);
        } else {
            ALIASES.put(key, id);
        }
    }

    /** Checks a catalog key directly without interpreting it as a user-entered alias. */
    public static boolean isKnownUnitId(String unitId) {
        return UNITS.containsKey(unitId);
    }

    public static String canonicalId(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        String clean = input.trim().replaceAll("\\s+", " ");
        String key = normalize(clean);
        if (AMBIGUOUS_ALIASES.contains(key)) return "CUSTOM:" + clean;
        String known = ALIASES.get(key);
        return known == null ? "CUSTOM:" + clean : known;
    }

    public static String label(String id) {
        Unit unit = UNITS.get(id);
        return unit == null ? id != null && id.startsWith("CUSTOM:") ? id.substring(7) : id : unit.symbol;
    }

    public static String dimension(String id) {
        if ("MG".equals(id) || "G".equals(id) || "KG".equals(id) || "OZ".equals(id) || "LB".equals(id)) {
            return "MASS";
        }
        if ("ML".equals(id) || "L".equals(id) || "TSP".equals(id) || "TBSP".equals(id)
                || "CUP".equals(id) || "FLOZ".equals(id)) return "VOLUME";
        return "COUNT:" + id;
    }

    /** Converts supported mass or volume units to g or ml, without cross-dimension assumptions. */
    public static BigDecimal toBaseAmount(BigDecimal amount, String id) {
        BigDecimal factor;
        switch (id == null ? "" : id) {
            case "MG": factor = new BigDecimal("0.001"); break;
            case "G": factor = BigDecimal.ONE; break;
            case "KG": factor = new BigDecimal("1000"); break;
            case "OZ": factor = new BigDecimal("28.3495"); break;
            case "LB": factor = new BigDecimal("453.592"); break;
            case "ML": factor = BigDecimal.ONE; break;
            case "L": factor = new BigDecimal("1000"); break;
            case "TSP": factor = new BigDecimal("4.929"); break;
            case "TBSP": factor = new BigDecimal("14.787"); break;
            case "CUP": factor = new BigDecimal("236.588"); break;
            case "FLOZ": factor = new BigDecimal("29.574"); break;
            default: return null;
        }
        return amount.multiply(factor);
    }

    private static String normalize(String value) {
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
        return normalized.replaceAll("\\s+", " ");
    }

    private static final class Unit {
        final String id;
        final String name;
        final String symbol;
        Unit(String id, String name, String symbol) {
            this.id = id;
            this.name = name;
            this.symbol = symbol;
        }
    }
}
