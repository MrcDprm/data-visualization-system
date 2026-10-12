package com.mrcdprm.dataviz.data;

/**
 * Kullanıcıya gösterilecek hata. Mesaj metni yerine bir kod taşır;
 * arayüz kodu seçili dilde "error.<kod>" anahtarıyla metne çevirir.
 */
public final class VizException extends RuntimeException {

    private final String code;
    private final transient Object[] args;

    public VizException(String code, Object... args) {
        super(code);
        this.code = code;
        this.args = args.clone();
    }

    public String code() {
        return code;
    }

    public Object[] args() {
        return args.clone();
    }
}
