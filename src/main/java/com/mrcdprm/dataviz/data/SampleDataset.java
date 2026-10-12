package com.mrcdprm.dataviz.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Uygulamayla gelen açık veri setleri. Her birinin kaynağı ve lisansı uygulamada ve README'de gösterilir;
 * üçü de kaynak gösterilerek ücretsiz kullanılıp dağıtılabilir.
 */
public enum SampleDataset {
    OWID_CO2("owid-co2.csv", "Our World in Data, CO₂ and Greenhouse Gas Emissions",
            "https://github.com/owid/co2-data", "CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/",
            "Our World in Data, based on the Global Carbon Project and the Energy Institute"),
    NASA_GISTEMP("nasa-gistemp.csv", "NASA GISS Surface Temperature Analysis (GISTEMP v4)",
            "https://data.giss.nasa.gov/gistemp/", "Public domain (U.S. Government work)",
            "https://data.giss.nasa.gov/gistemp/",
            "GISTEMP Team, NASA Goddard Institute for Space Studies"),
    IBB_DAMS("ibb-baraj-doluluk.csv", "İBB Açık Veri Portalı, İstanbul Barajları Günlük Doluluk Oranları",
            "https://data.ibb.gov.tr/dataset/istanbul-barajlari-gunluk-doluluk-oranlari",
            "İBB Açık Veri Lisansı", "https://data.ibb.gov.tr/license",
            "İstanbul Büyükşehir Belediyesi, İSKİ");

    private final String resource;
    private final String sourceName;
    private final String sourceUrl;
    private final String license;
    private final String licenseUrl;
    private final String attribution;

    SampleDataset(String resource, String sourceName, String sourceUrl, String license, String licenseUrl,
            String attribution) {
        this.resource = resource;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.license = license;
        this.licenseUrl = licenseUrl;
        this.attribution = attribution;
    }

    public String id() {
        return name();
    }

    public String sourceName() {
        return sourceName;
    }

    public String sourceUrl() {
        return sourceUrl;
    }

    public String license() {
        return license;
    }

    public String licenseUrl() {
        return licenseUrl;
    }

    public String attribution() {
        return attribution;
    }

    /** Proje dosyasından gelen kimliğe güvenilmez: bilinmeyen kimlik boş döner. */
    public static Optional<SampleDataset> of(String id) {
        for (SampleDataset s : values())
            if (s.name().equals(id))
                return Optional.of(s);
        return Optional.empty();
    }

    /** Veri setini uygulamanın içinden okur. */
    public DataSet load(String displayName) {
        try (InputStream in = SampleDataset.class.getResourceAsStream("/com/mrcdprm/dataviz/datasets/" + resource)) {
            if (in == null)
                throw new IllegalStateException("Eksik kaynak: " + resource);
            return DataSet.of(CsvReader.read(in, ImportOptions.AUTO), displayName, DataSet.Source.sample(id()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
