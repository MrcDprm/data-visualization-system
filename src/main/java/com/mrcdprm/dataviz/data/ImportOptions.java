package com.mrcdprm.dataviz.data;

/**
 * İçe aktarma ayarları. Boş (null) alanlar otomatik bulunur: karakter kodlaması, ayırıcı, başlık satırı ve ondalık
 * ayırıcı. Kullanıcı içe aktarma penceresinde bunları değiştirebilir; proje dosyasına çözümlenmiş hâli kaydedilir.
 */
public record ImportOptions(String charset, Character delimiter, Boolean header, DecimalStyle decimal) {

    public static final ImportOptions AUTO = new ImportOptions(null, null, null, null);

    public ImportOptions withCharset(String value) {
        return new ImportOptions(value, delimiter, header, decimal);
    }

    public ImportOptions withDelimiter(Character value) {
        return new ImportOptions(charset, value, header, decimal);
    }

    public ImportOptions withHeader(Boolean value) {
        return new ImportOptions(charset, delimiter, value, decimal);
    }

    public ImportOptions withDecimal(DecimalStyle value) {
        return new ImportOptions(charset, delimiter, header, value);
    }
}
