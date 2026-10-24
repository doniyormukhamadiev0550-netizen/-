package dev.lumina;

/** User-selectable look: two accent colors + dark/light background. */
public final class Theme {
    public static final int[] PALETTE = {0xFF7C5CFF, 0xFF3B82F6, 0xFF22D3EE, 0xFF22C55E, 0xFFFACC15,
            0xFFF97316, 0xFFEF4444, 0xFFEC4899, 0xFFF8FAFC, 0xFF94A3B8};
    public static int c1 = PALETTE[0], c2 = PALETTE[2];
    public static boolean dark = true;

    public static int text() { return dark ? 0xFFFFFFFF : 0xFF14141C; }
    public static int dim() { return dark ? 0xFFA0A0B8 : 0xFF5A5A70; }
    public static int glass() { return dark ? 0xA0141420 : 0xB8F6F8FF; }
    public static int glassHi() { return dark ? 0x26FFFFFF : 0x80FFFFFF; }
    public static int card() { return dark ? 0x30FFFFFF : 0x28000000; }
    public static int cardHover() { return dark ? 0x4AFFFFFF : 0x40000000; }
    public static int off() { return dark ? 0xFF3A3A4C : 0xFFB4B8C8; }
    public static int withAlpha(int c, int a) { return (a << 24) | (c & 0xFFFFFF); }
}
