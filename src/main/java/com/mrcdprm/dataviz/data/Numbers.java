package com.mrcdprm.dataviz.data;

import java.util.regex.Pattern;

/**
 * Metindeki sayıyı okur. Dosyadaki sayılar "1234.5", "1,234.5", "1.234,5", "%12,5", "1 250 ₺" gibi
 * yazılmış olabilir; ondalık ayırıcının nokta mı virgül mü olduğu dosyanın tamamına bakılarak seçilir.
 */
public final class Numbers {

    private static final Pattern DOT = Pattern.compile("[+-]?(\\d{1,3}(,\\d{3})+|\\d+)?(\\.\\d+)?([eE][+-]?\\d+)?");
    private static final Pattern COMMA = Pattern.compile("[+-]?(\\d{1,3}(\\.\\d{3})+|\\d+)?(,\\d+)?([eE][+-]?\\d+)?");

    private Numbers() {
    }

    /** Sayıya çevirir; sayı değilse ya da boşsa NaN. */
    public static double parse(String text, DecimalStyle style) {
        if (text == null)
            return Double.NaN;
        String s = clean(text);
        if (s.isEmpty() || s.chars().noneMatch(Character::isDigit))
            return Double.NaN;
        if (style == DecimalStyle.COMMA) {
            if (!COMMA.matcher(s).matches())
                return Double.NaN;
            s = s.replace(".", "").replace(',', '.');
        } else {
            if (!DOT.matcher(s).matches())
                return Double.NaN;
            s = s.replace(",", "");
        }
        try {
            final double value = Double.parseDouble(s);
            return Double.isFinite(value) ? value : Double.NaN;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** Boşlukları, yüzde ve para birimi işaretlerini atar; Unicode eksi işaretini "-" yapar. */
    static String clean(String text) {
        final StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.strip().toCharArray()) {
            switch (c) {
                case ' ', ' ', ' ', ' ', '%', '₺', '$', '€', '£' -> {
                }
                case '−' -> sb.append('-');
                default -> sb.append(c);
            }
        }
        String s = sb.toString();
        if (s.endsWith("TL"))
            s = s.substring(0, s.length() - 2);
        return s;
    }

    /**
     * Örnek değerlere bakıp ondalık ayırıcıyı seçer: hangi biçimde daha çok değer sayı olarak okunuyorsa o.
     * Eşitlikte (ör. sadece "12" ya da "1,234" gibi iki türlü okunabilen değerler) ipucu kullanılır:
     * noktalı virgülle ayrılmış dosyalar genelde virgül ondalıklıdır (Excel'in Türkçe CSV'si).
     */
    public static DecimalStyle detect(Iterable<String> samples, DecimalStyle tieBreaker) {
        int dot = 0;
        int comma = 0;
        for (String s : samples) {
            if (s == null || s.isBlank())
                continue;
            final boolean d = !Double.isNaN(parse(s, DecimalStyle.DOT));
            final boolean c = !Double.isNaN(parse(s, DecimalStyle.COMMA));
            if (d && !c)
                dot++;
            else if (c && !d)
                comma++;
            else if (d) {
                // ikisi de okuyabiliyor: "1,25" gibi virgülden sonra 3'ten farklı basamak sayısı virgül ondalığa işaret eder
                final String t = clean(s);
                if (t.matches("[+-]?\\d+,\\d{1,2}|[+-]?\\d+,\\d{4,}"))
                    comma++;
                else if (t.matches("[+-]?\\d+\\.\\d{1,2}|[+-]?\\d+\\.\\d{4,}"))
                    dot++;
            }
        }
        if (dot == comma)
            return tieBreaker;
        return dot > comma ? DecimalStyle.DOT : DecimalStyle.COMMA;
    }
}
