package com.mrcdprm.dataviz.ui;

import com.mrcdprm.dataviz.data.CsvReader;
import com.mrcdprm.dataviz.data.VizException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Arayüz metinleri (Türkçe / İngilizce). Metinler i18n/messages_*.properties dosyalarındadır.
 * {0}, {1} yer tutucuları basit metin değiştirmeyle doldurulur; MessageFormat kullanılmaz çünkü
 * Türkçedeki kesme işaretini özel karakter sayıp metni bozar.
 */
public final class I18n {

    public static final Locale TR = Locale.forLanguageTag("tr");
    public static final Locale EN = Locale.ENGLISH;

    private static Locale locale = TR;
    private static ResourceBundle bundle = load(TR);

    private I18n() {
    }

    public static void setLocale(Locale newLocale) {
        locale = "en".equals(newLocale.getLanguage()) ? EN : TR;
        bundle = load(locale);
    }

    public static Locale locale() {
        return locale;
    }

    public static boolean isTurkish() {
        return locale == TR;
    }

    private static ResourceBundle load(Locale l) {
        return ResourceBundle.getBundle("com.mrcdprm.dataviz.i18n.messages", l, ResourceBundle.Control.getNoFallbackControl(
                ResourceBundle.Control.FORMAT_PROPERTIES));
    }

    /** Anahtarın metni; eksik anahtar ekranda !anahtar! olarak görünür (geliştirirken hemen fark edilsin). */
    public static String t(String key, Object... args) {
        String text;
        try {
            text = bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
        for (int i = 0; i < args.length; i++)
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        return text;
    }

    /** Enum değerinin metni: "enum.ChartType.BAR" gibi. */
    public static String of(Enum<?> value) {
        return t("enum." + value.getDeclaringClass().getSimpleName() + "." + value.name());
    }

    public static String error(VizException e) {
        return t("error." + e.code(), e.args());
    }

    public static String warning(CsvReader.Warning w) {
        return t("warning." + w.code(), w.args());
    }

    /** Sayı gösterimi: binlik ayırıcılı, en fazla 2 ondalık ("1.234,56"); çok küçük sayılarda daha fazla. */
    public static String number(double v) {
        if (Double.isNaN(v))
            return "";
        final double a = Math.abs(v);
        final String pattern = a != 0 && a < 0.01 ? "#,##0.####" : "#,##0.##";
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale)).format(v);
    }

    /** Filtre ve eksen sınırları için: 10.000'den küçük tam sayılar ayırıcısız (yıl "1990" kalsın), diğerleri {@link #number}. */
    public static String value(double v) {
        if (!Double.isNaN(v) && v == Math.rint(v) && Math.abs(v) < 10_000)
            return String.valueOf((long) v);
        return number(v);
    }

    /** Tam sayı, binlik ayırıcılı: 16.350 */
    public static String count(long n) {
        return new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(locale)).format(n);
    }

    public static String date(LocalDate date) {
        return date == null ? "" : date.format(DateTimeFormatter.ofPattern(isTurkish() ? "dd.MM.yyyy" : "MMM d, yyyy", locale));
    }
}
