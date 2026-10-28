package tr.com.sevinclidoga.ikas;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * İkas cevabındaki bir değer (nesne, dizi ya da yaprak) — alan adlarıyla gezilir, eksik alanda istisna atmaz.
 * <pre>{@code
 * IkasVeri s = ikas.siparis(id);
 * String no = s.metin("orderNumber");
 * BigDecimal toplam = s.sayi("totalFinalPrice");
 * for (IkasVeri satir : s.liste("orderLineItems")) { ... }
 * String sehir = s.yol("shippingAddress.city.name").metin();
 * }</pre>
 * Değişmez; iş parçacığı güvenlidir.
 */
public final class IkasVeri {

    private static final IkasVeri BOS = new IkasVeri(null);
    private final Object ham;

    private IkasVeri(Object ham) {
        this.ham = ham;
    }

    public static IkasVeri of(Object ham) {
        return ham == null ? BOS : ham instanceof IkasVeri v ? v : new IkasVeri(ham);
    }

    /** Ham değer: Map, List, String, BigDecimal, Boolean ya da null. */
    public Object ham() {
        return ham;
    }

    public boolean bosMu() {
        return ham == null;
    }

    public boolean nesneMi() {
        return ham instanceof Map;
    }

    public boolean diziMi() {
        return ham instanceof List;
    }

    /** Nesnenin alanı; yoksa boş değer. */
    public IkasVeri alan(String ad) {
        return ham instanceof Map<?, ?> m ? of(m.get(ad)) : BOS;
    }

    /** Noktalı yol: "shippingAddress.city.name", dizi indisi: "orderLineItems.0.variant.sku". */
    public IkasVeri yol(String yol) {
        IkasVeri v = this;
        for (String p : yol.split("\\.")) {
            if (v.ham instanceof List<?> l && p.chars().allMatch(Character::isDigit)) {
                int i = Integer.parseInt(p);
                v = i < l.size() ? of(l.get(i)) : BOS;
            } else {
                v = v.alan(p);
            }
        }
        return v;
    }

    /** Dizi elemanları (dizi değilse boş liste). */
    public List<IkasVeri> liste() {
        if (!(ham instanceof List<?> l)) return List.of();
        List<IkasVeri> r = new ArrayList<>(l.size());
        for (Object o : l) r.add(of(o));
        return Collections.unmodifiableList(r);
    }

    public List<IkasVeri> liste(String ad) {
        return alan(ad).liste();
    }

    /**
     * Sayfalı cevapların ({@code {count, data, hasNext, …}}) kayıtları; düz dizi cevaplarında dizinin kendisi.
     * Ürün aramasında kayıtlar {@code results} içindedir — o da desteklenir.
     */
    public List<IkasVeri> kayitlar() {
        if (diziMi()) return liste();
        List<IkasVeri> d = liste("data");
        return !d.isEmpty() || !alan("results").diziMi() ? d : liste("results");
    }

    public String metin() {
        return ham == null || ham instanceof Map || ham instanceof List ? null : ham instanceof BigDecimal b ? b.toPlainString() : ham.toString();
    }

    public String metin(String ad) {
        return alan(ad).metin();
    }

    public BigDecimal sayi() {
        if (ham instanceof BigDecimal b) return b;
        if (ham instanceof String s) {
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public BigDecimal sayi(String ad) {
        return alan(ad).sayi();
    }

    public Integer tamsayi(String ad) {
        BigDecimal b = sayi(ad);
        return b == null ? null : b.intValue();
    }

    public Boolean mantiksal(String ad) {
        Object v = alan(ad).ham;
        return v instanceof Boolean b ? b : null;
    }

    /** Milisaniye zaman damgası alanı (İkas Timestamp) → Instant. */
    public Instant zaman(String ad) {
        BigDecimal b = sayi(ad);
        return b == null ? null : Instant.ofEpochMilli(b.longValue());
    }

    public Optional<String> metinOpt(String ad) {
        return Optional.ofNullable(metin(ad));
    }

    @Override
    public String toString() {
        return Json.yaz(ham);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IkasVeri v && java.util.Objects.equals(ham, v.ham);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(ham);
    }
}
