package tr.com.sevinclidoga.ikas;

import java.util.List;
import java.util.Map;

/**
 * İkas isteği başarısız. {@link #agHatasiMi()} true ise aynı istek sonra tekrar denenebilir (bağlantı, zaman aşımı,
 * 5xx). İkas işlemi reddettiyse (GraphQL errors) {@link #hatalar()} doludur.
 */
public class IkasHatasi extends RuntimeException {

    /**
     * Tek GraphQL hatası.
     *
     * @param mesaj      İkas mesajı ("Object not found", "error_messages.order.order_not_found" …)
     * @param kod        {@code extensions.code} (404_OBJECT_NOT_FOUND, ORDER_NOT_FOUND, ARGUMENT_VALIDATION_ERROR, UNAUTHORIZED …)
     * @param dogrulama  ARGUMENT_VALIDATION_ERROR ayrıntısı: alan → kısıt mesajları
     *                   (ör. orderId → "Referenced id not found" = kayıt yok)
     */
    public record Hata(String mesaj, String kod, Map<String, List<String>> dogrulama) {

        /** Kayıt bulunamadı mı (İkas bunu üç farklı biçimde söyler). */
        public boolean bulunamadiMi() {
            return (kod != null && (kod.contains("NOT_FOUND"))) || (mesaj != null && mesaj.toLowerCase().contains("not_found"))
                    || dogrulama.values().stream().flatMap(List::stream).anyMatch(s -> s.contains("Referenced id not found"));
        }
    }

    private final boolean agHatasi;
    private final List<Hata> hatalar;

    public IkasHatasi(String mesaj, boolean agHatasi, Throwable neden) {
        super(mesaj, neden);
        this.agHatasi = agHatasi;
        this.hatalar = List.of();
    }

    public IkasHatasi(String islem, List<Hata> hatalar) {
        super(islem + ": " + String.join("; ", hatalar.stream().map(h -> h.mesaj() + (h.kod() != null ? " [" + h.kod() + "]" : "")
                + (h.dogrulama().isEmpty() ? "" : " " + h.dogrulama())).toList()));
        this.agHatasi = false;
        this.hatalar = List.copyOf(hatalar);
    }

    public boolean agHatasiMi() {
        return agHatasi;
    }

    public List<Hata> hatalar() {
        return hatalar;
    }

    /** Hatalardan herhangi biri "kayıt yok" mu. */
    public boolean bulunamadiMi() {
        return hatalar.stream().anyMatch(Hata::bulunamadiMi);
    }

    /** İlk hatanın kodu (yoksa null). */
    public String kod() {
        return hatalar.isEmpty() ? null : hatalar.get(0).kod();
    }
}
