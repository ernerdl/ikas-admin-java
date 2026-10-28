package tr.com.sevinclidoga.ikas;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * İkas Admin GraphQL şeması (introspection). Kütüphanenin içinde 2026-10-28 tarihli şema gelir
 * ({@link #varsayilan()}); güncel şema {@link IkasIstemci#semaIndir()} ile çekilebilir.
 * <p>
 * Her işlem için GraphQL belgesini kendisi kurar: verilen argümanlar için değişken bildirimi, dönüş tipi için
 * istenen derinlikte seçim kümesi (skaler/enum alanlar + iç nesneler). Zorunlu argümanlı alt alanlar ve İkas'ın
 * "boş olamaz" deyip boş döndürdüğü bilinen alanlar seçime alınmaz.
 */
public final class IkasSema {

    /** İkas'ın NON_NULL tanımlayıp null döndürdüğü alanlar: seçilirse bütün sorgu düşer (canlıda görüldü 2026-10-28). */
    static final Set<String> BOZUK_ALANLAR = Set.of("OrderSession.journeyId");

    /** Tip referansı: {@code [String!]!} gibi tam yazım + iç tip adı ve türü. */
    public record TipRef(String yazim, String ad, String tur, boolean zorunlu) {
    }

    public record Arguman(String ad, TipRef tip, String aciklama) {
    }

    /**
     * Bir sorgu ya da mutasyon.
     *
     * @param mutasyonMu true ise yazma işlemi
     */
    public record Islem(String ad, boolean mutasyonMu, List<Arguman> argumanlar, TipRef donus, String aciklama) {

        public List<String> zorunluArgumanlar() {
            return argumanlar.stream().filter(a -> a.tip().zorunlu()).map(Arguman::ad).toList();
        }
    }

    private static volatile IkasSema varsayilan;

    private final Map<String, Map<String, Object>> tipler;
    private final Map<String, Islem> islemler;

    private IkasSema(Map<String, Object> introspection) {
        Map<String, Object> sema = harita(harita(introspection.get("data")).get("__schema"));
        Map<String, Map<String, Object>> t = new LinkedHashMap<>();
        for (Object o : liste(sema.get("types"))) t.put((String) harita(o).get("name"), harita(o));
        this.tipler = Collections.unmodifiableMap(t);
        Map<String, Islem> is = new LinkedHashMap<>();
        topla(is, (String) harita(sema.get("queryType")).get("name"), false);
        topla(is, (String) harita(sema.get("mutationType")).get("name"), true);
        this.islemler = Collections.unmodifiableMap(is);
    }

    /** Kütüphaneyle gelen şema (2026-10-28). */
    public static IkasSema varsayilan() {
        IkasSema s = varsayilan;
        if (s == null) {
            synchronized (IkasSema.class) {
                if (varsayilan == null) {
                    try (InputStream in = IkasSema.class.getResourceAsStream("ikas-admin-sema.json")) {
                        if (in == null) throw new IllegalStateException("ikas-admin-sema.json bulunamadı");
                        varsayilan = coz(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
                s = varsayilan;
            }
        }
        return s;
    }

    /** Introspection cevabından şema. */
    @SuppressWarnings("unchecked")
    public static IkasSema coz(String introspectionJson) {
        return new IkasSema((Map<String, Object>) Json.coz(introspectionJson));
    }

    public Islem islem(String ad) {
        Islem i = islemler.get(ad);
        if (i == null) throw new IllegalArgumentException("İkas'ta böyle bir işlem yok: " + ad);
        return i;
    }

    public boolean varMi(String ad) {
        return islemler.containsKey(ad);
    }

    /** Tüm işlem adları (sorgular + mutasyonlar). */
    public Set<String> islemler() {
        return Collections.unmodifiableSet(new TreeSet<>(islemler.keySet()));
    }

    public List<Islem> sorgular() {
        return islemler.values().stream().filter(i -> !i.mutasyonMu()).toList();
    }

    public List<Islem> mutasyonlar() {
        return islemler.values().stream().filter(Islem::mutasyonMu).toList();
    }

    /**
     * İşlem için GraphQL belgesi. Yalnız verilen argümanlar bildirilir; eksik zorunlu argüman varsa istisna.
     *
     * @param derinlik iç nesnelerin kaç seviye açılacağı (0 = yalnız dönüş tipinin kendi skalerleri)
     */
    public String belge(String islemAdi, Map<String, ?> argumanlar, int derinlik) {
        Islem is = islem(islemAdi);
        String secim = secim(is.donus().ad(), derinlik);
        return belgeSecimle(islemAdi, argumanlar, secim);
    }

    /** Seçim kümesini çağıran verir ({@code "{ id orderNumber }"}); skaler dönüşlü işlemlerde boş bırakılır. */
    public String belgeSecimle(String islemAdi, Map<String, ?> argumanlar, String secim) {
        Islem is = islem(islemAdi);
        Map<String, ?> a = argumanlar == null ? Map.of() : argumanlar;
        List<String> eksik = new ArrayList<>();
        for (String z : is.zorunluArgumanlar()) if (a.get(z) == null) eksik.add(z);
        if (!eksik.isEmpty()) throw new IllegalArgumentException(islemAdi + ": eksik zorunlu argüman " + eksik);
        List<String> bildirim = new ArrayList<>(), cagri = new ArrayList<>();
        for (Arguman ar : is.argumanlar()) {
            if (a.get(ar.ad()) == null) continue;
            bildirim.add("$" + ar.ad() + ": " + ar.tip().yazim());
            cagri.add(ar.ad() + ": $" + ar.ad());
        }
        for (String verilen : a.keySet()) {
            if (is.argumanlar().stream().noneMatch(x -> x.ad().equals(verilen))) {
                throw new IllegalArgumentException(islemAdi + ": bilinmeyen argüman '" + verilen + "'");
            }
        }
        boolean skaler = "SCALAR".equals(is.donus().tur()) || "ENUM".equals(is.donus().tur());
        return (is.mutasyonMu() ? "mutation" : "query") + (bildirim.isEmpty() ? "" : "(" + String.join(", ", bildirim) + ")")
                + " { " + islemAdi + (cagri.isEmpty() ? "" : "(" + String.join(", ", cagri) + ")")
                + (skaler || secim == null || secim.isBlank() ? "" : " " + secim) + " }";
    }

    /** Tipin seçim kümesi ({@code "{ a b c { d } }"}); skaler/enum tipte boş metin. */
    public String secim(String tipAdi, int derinlik) {
        return secim(tipAdi, derinlik, new ArrayList<>());
    }

    private String secim(String tipAdi, int derinlik, List<String> yol) {
        Map<String, Object> t = tipler.get(tipAdi);
        if (t == null) return "";
        String tur = (String) t.get("kind");
        if ("SCALAR".equals(tur) || "ENUM".equals(tur)) return "";
        if ("UNION".equals(tur) || "INTERFACE".equals(tur)) return "{ __typename }";
        List<String> p = new ArrayList<>();
        for (Object fo : liste(t.get("fields"))) {
            Map<String, Object> f = harita(fo);
            String ad = (String) f.get("name");
            if (Boolean.TRUE.equals(f.get("isDeprecated")) || BOZUK_ALANLAR.contains(tipAdi + "." + ad)) continue;
            boolean zorunluArg = liste(f.get("args")).stream().anyMatch(x -> tipRef(harita(harita(x).get("type"))).zorunlu());
            if (zorunluArg) continue;
            TipRef ft = tipRef(harita(f.get("type")));
            if ("SCALAR".equals(ft.tur()) || "ENUM".equals(ft.tur())) {
                p.add(ad);
            } else if (derinlik > 0 && !yol.contains(ft.ad())) {
                yol.add(tipAdi);
                String alt = secim(ft.ad(), derinlik - 1, yol);
                yol.remove(yol.size() - 1);
                if (!alt.isEmpty()) p.add(ad + " " + alt);
            }
        }
        return p.isEmpty() ? "" : "{ " + String.join(" ", p) + " }";
    }

    /**
     * Girdi tipinin okunabilir AYNASI: {@code girdiTipi}'nin her alanı (iç içe) {@code ciktiTipi}'nde aynı adla seçilir.
     * Okunan kayıt doğrudan girdi olarak geri gönderilebilir — İkas'ta tek alan güncelleyen işlem olmadığında (ör. ürün)
     * "oku → bir alanı değiştir → aynen yaz" için. Çıktıda karşılığı olmayan alan atlanır ({@link #aynaEksikleri}).
     */
    public String girdiAynasi(String girdiTipi, String ciktiTipi) {
        return ayna(girdiTipi, ciktiTipi, new ArrayList<>());
    }

    /** {@link #girdiAynasi} çıktıda bulamadığı alanlar ("ProductInput.x"); boşsa ayna eksiksizdir. */
    public List<String> aynaEksikleri(String girdiTipi, String ciktiTipi) {
        List<String> eksik = new ArrayList<>();
        ayna(girdiTipi, ciktiTipi, eksik);
        return eksik;
    }

    private String ayna(String girdi, String cikti, List<String> eksik) {
        Map<String, Map<String, Object>> ciktiAlanlari = new LinkedHashMap<>();
        for (Object fo : liste(tipler.getOrDefault(cikti, Map.of()).get("fields"))) {
            ciktiAlanlari.put((String) harita(fo).get("name"), harita(fo));
        }
        List<String> p = new ArrayList<>();
        for (Object fo : liste(tipler.getOrDefault(girdi, Map.of()).get("inputFields"))) {
            Map<String, Object> f = harita(fo);
            String ad = (String) f.get("name");
            Map<String, Object> karsi = ciktiAlanlari.get(ad);
            if (karsi == null) {
                eksik.add(girdi + "." + ad);
                continue;
            }
            TipRef gt = tipRef(harita(f.get("type")));
            if ("INPUT_OBJECT".equals(gt.tur())) {
                p.add(ad + " " + ayna(gt.ad(), tipRef(harita(karsi.get("type"))).ad(), eksik));
            } else {
                p.add(ad);
            }
        }
        return "{ " + String.join(" ", p) + " }";
    }

    /** Enum tipinin değerleri. */
    public List<String> enumDegerleri(String enumAdi) {
        Map<String, Object> t = tipler.get(enumAdi);
        if (t == null || !"ENUM".equals(t.get("kind"))) throw new IllegalArgumentException("enum değil: " + enumAdi);
        return liste(t.get("enumValues")).stream().map(e -> (String) harita(e).get("name")).toList();
    }

    // ── iç ──

    private void topla(Map<String, Islem> hedef, String kokTip, boolean mutasyon) {
        for (Object fo : liste(tipler.get(kokTip).get("fields"))) {
            Map<String, Object> f = harita(fo);
            List<Arguman> args = new ArrayList<>();
            for (Object ao : liste(f.get("args"))) {
                Map<String, Object> a = harita(ao);
                args.add(new Arguman((String) a.get("name"), tipRef(harita(a.get("type"))), (String) a.get("description")));
            }
            hedef.put((String) f.get("name"), new Islem((String) f.get("name"), mutasyon, List.copyOf(args),
                    tipRef(harita(f.get("type"))), (String) f.get("description")));
        }
    }

    static TipRef tipRef(Map<String, Object> t) {
        String tur = (String) t.get("kind");
        if ("NON_NULL".equals(tur)) {
            TipRef ic = tipRef(harita(t.get("ofType")));
            return new TipRef(ic.yazim() + "!", ic.ad(), ic.tur(), true);
        }
        if ("LIST".equals(tur)) {
            TipRef ic = tipRef(harita(t.get("ofType")));
            return new TipRef("[" + ic.yazim() + "]", ic.ad(), ic.tur(), false);
        }
        return new TipRef((String) t.get("name"), (String) t.get("name"), tur, false);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> harita(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> liste(Object o) {
        return o instanceof List ? (List<Object>) o : List.of();
    }
}
