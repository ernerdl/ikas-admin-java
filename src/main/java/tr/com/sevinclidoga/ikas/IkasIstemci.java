package tr.com.sevinclidoga.ikas;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * İkas Admin GraphQL istemcisi: OAuth token'ını kendisi alır ve yeniler (401'de bir kez), 135 işlemin hepsini
 * {@link #calistir(String, Map)} ile çağırır. Belgeyi şemadan kendisi kurar ({@link IkasSema}).
 * <p>
 * Mutasyonlar {@link IkasAyarlari#yazmaIzni()} kapalıysa İSTEK GİTMEDEN reddedilir. İş parçacığı güvenlidir.
 * <p>
 * <b>Hız sınırı:</b> İkas uygulama başına 10 saniyede 50 istek tanır ({@code x-ratelimit-*} başlıkları; aynı client id'yi
 * kullanan herkes bu kotayı paylaşır). İstemci kalan kota {@link #YEDEK_KOTA}'ya inince pencere yenilenene kadar bekler
 * (aynı uygulamayı kullanan başka sistemlere pay kalsın); yine de 429 gelirse pencere sonuna kadar bekleyip en çok
 * {@link #HIZ_TEKRAR} kez yeniden dener.
 */
public class IkasIstemci {

    /** Belgelerde varsayılan iç nesne derinliği. */
    public static final int VARSAYILAN_DERINLIK = 2;
    private static final long TOKEN_PAYI_MS = 60_000;
    private static final Pattern MUTASYON = Pattern.compile("^\\s*mutation\\b");
    private static final String UA = "ikas-admin-java/0.1";
    /** Bu kadar istek hakkı kalınca pencere sonuna kadar beklenir. */
    public static final int YEDEK_KOTA = 5;
    /** 429 sonrası en çok kaç kez yeniden denenir. */
    public static final int HIZ_TEKRAR = 3;
    private static final long EN_COK_BEKLEME_MS = 15_000;

    private final IkasAyarlari ayarlar;
    private final IkasSema sema;
    private final HttpClient http;
    private volatile String token;
    private volatile long tokenBitis;
    private volatile int kalanKota = Integer.MAX_VALUE;
    private volatile long kotaYenilenme;

    public IkasIstemci(IkasAyarlari ayarlar) {
        this(ayarlar, IkasSema.varsayilan());
    }

    public IkasIstemci(IkasAyarlari ayarlar, IkasSema sema) {
        this.ayarlar = Objects.requireNonNull(ayarlar, "ayarlar");
        this.sema = Objects.requireNonNull(sema, "sema");
        this.http = HttpClient.newBuilder().connectTimeout(ayarlar.zamanAsimi()).build();
    }

    public IkasAyarlari ayarlar() {
        return ayarlar;
    }

    public IkasSema sema() {
        return sema;
    }

    /**
     * İşlemi çağırır; seçim kümesi şemadan {@link #VARSAYILAN_DERINLIK} derinlikte kurulur.
     *
     * @return işlemin sonucu (ör. listOrder için {@code {count, data:[…], hasNext}})
     */
    public IkasVeri calistir(String islem, Map<String, ?> argumanlar) {
        return calistir(islem, argumanlar, VARSAYILAN_DERINLIK);
    }

    public IkasVeri calistir(String islem, Map<String, ?> argumanlar, int derinlik) {
        return gonder(islem, sema.belge(islem, argumanlar, derinlik), argumanlar);
    }

    /** Seçim kümesini kendiniz verin: {@code calistirSecimle("listOrder", args, "{ data { id orderNumber } }")}. */
    public IkasVeri calistirSecimle(String islem, Map<String, ?> argumanlar, String secim) {
        return gonder(islem, sema.belgeSecimle(islem, argumanlar, secim), argumanlar);
    }

    /** Ham GraphQL belgesi; cevabın tamamı ({@code data}) döner. Mutasyon belgesi yazma iznine tabidir. */
    public IkasVeri ham(String belge, Map<String, ?> degiskenler) {
        if (MUTASYON.matcher(belge).find()) yazmaKontrol("(ham belge)");
        return istek(belge, degiskenler, "(ham)", true);
    }

    /** Canlı şemayı indirir (introspection) — kütüphanedeki şema eskidiyse. */
    public IkasSema semaIndir() {
        String tr = "kind name ofType { kind name ofType { kind name ofType { kind name ofType { kind name ofType { kind name } } } } }";
        String q = "query { __schema { queryType { name } mutationType { name } types { kind name description "
                + "fields(includeDeprecated: true) { name description isDeprecated args { name description type { " + tr + " } } type { " + tr + " } } "
                + "inputFields { name type { " + tr + " } } enumValues(includeDeprecated: true) { name } } } }";
        IkasVeri d = istek(q, Map.of(), "__schema", true);
        return IkasSema.coz("{\"data\":" + d + "}");
    }

    // ── iç ──

    private IkasVeri gonder(String islem, String belge, Map<String, ?> argumanlar) {
        if (sema.islem(islem).mutasyonMu()) yazmaKontrol(islem);
        IkasVeri data = istek(belge, argumanlar, islem, true);
        return data.alan(islem);
    }

    private void yazmaKontrol(String islem) {
        if (!ayarlar.yazmaIzni()) {
            throw new IkasHatasi(islem + " bir yazma işlemidir ve CANLI mağazayı değiştirir; IkasAyarlari.yazmayaIzinVer() "
                    + "olmadan çalıştırılmaz", false, null);
        }
    }

    private IkasVeri istek(String belge, Map<String, ?> degiskenler, String islem, boolean tekrarDenenebilir) {
        Map<String, Object> govde = new LinkedHashMap<>();
        govde.put("query", belge);
        govde.put("variables", degiskenler == null ? Map.of() : degiskenler);
        HttpResponse<String> c = post(ayarlar.graphqlAdresi(), Json.yaz(govde), true, islem);
        if (c.statusCode() == 401 && tekrarDenenebilir) {
            tokenSifirla();
            return istek(belge, degiskenler, islem, false);
        }
        Object cozulmus;
        try {
            cozulmus = Json.coz(c.body());
        } catch (IllegalArgumentException e) {
            throw new IkasHatasi(islem + ": HTTP " + c.statusCode() + " çözülemeyen cevap", c.statusCode() >= 500, e);
        }
        IkasVeri cevap = IkasVeri.of(cozulmus);
        List<IkasHatasi.Hata> hatalar = hatalariCoz(cevap.liste("errors"));
        IkasVeri data = cevap.alan("data");
        // Hata + sonuç yok → istisna. (Sonuç olup hata da olan kısmi cevaplar döndürülür; hatasız null sonuç da geçerlidir.)
        boolean sonucYok = data.bosMu() || (!"(ham)".equals(islem) && data.alan(islem).bosMu());
        if (!hatalar.isEmpty() && sonucYok) {
            if (hatalar.stream().anyMatch(h -> "UNAUTHENTICATED".equals(h.kod())) && tekrarDenenebilir) {
                tokenSifirla();
                return istek(belge, degiskenler, islem, false);
            }
            throw new IkasHatasi(islem, hatalar);
        }
        if (c.statusCode() >= 500) throw new IkasHatasi(islem + ": HTTP " + c.statusCode(), true, null);
        if (c.statusCode() >= 400 && hatalar.isEmpty()) throw new IkasHatasi(islem + ": HTTP " + c.statusCode(), false, null);
        return data;
    }

    static List<IkasHatasi.Hata> hatalariCoz(List<IkasVeri> errors) {
        List<IkasHatasi.Hata> l = new ArrayList<>();
        for (IkasVeri e : errors) {
            IkasVeri ext = e.alan("extensions");
            Map<String, List<String>> dog = new LinkedHashMap<>();
            for (IkasVeri v : ext.liste("validationErrors")) {
                List<String> kisit = new ArrayList<>();
                if (v.alan("constraints").ham() instanceof Map<?, ?> m) m.values().forEach(x -> kisit.add(String.valueOf(x)));
                dog.put(Objects.requireNonNullElse(v.metin("property"), "?"), List.copyOf(kisit));
            }
            l.add(new IkasHatasi.Hata(e.metin("message"), ext.metin("code"), Map.copyOf(dog)));
        }
        return l;
    }

    private String token() {
        String t = token;
        if (t != null && System.currentTimeMillis() < tokenBitis) return t;
        synchronized (this) {
            if (token != null && System.currentTimeMillis() < tokenBitis) return token;
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("grant_type", "client_credentials");
            g.put("client_id", ayarlar.istemciKimligi());
            g.put("client_secret", ayarlar.istemciSirri());
            HttpResponse<String> c = post(ayarlar.tokenAdresi(), Json.yaz(g), false, "oauth/token");
            if (c.statusCode() != 200) {
                throw new IkasHatasi("İkas token alınamadı: HTTP " + c.statusCode(), c.statusCode() >= 500, null);
            }
            IkasVeri v = IkasVeri.of(Json.coz(c.body()));
            String yeni = v.metin("access_token");
            if (yeni == null) throw new IkasHatasi("İkas token cevabında access_token yok", false, null);
            BigDecimal sure = v.sayi("expires_in");
            tokenBitis = System.currentTimeMillis() + (sure == null ? 3_600_000 : sure.longValue() * 1000) - TOKEN_PAYI_MS;
            token = yeni;
            return yeni;
        }
    }

    private synchronized void tokenSifirla() {
        token = null;
        tokenBitis = 0;
    }

    private HttpResponse<String> post(String adres, String govde, boolean yetkili, String islem) {
        for (int deneme = 0; ; deneme++) {
            kotaBekle(islem);
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(adres))
                    .timeout(ayarlar.zamanAsimi())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", UA)   // Varsayılan Java/Python UA'sı İkas'ta 403 alabiliyor
                    .POST(HttpRequest.BodyPublishers.ofString(govde, StandardCharsets.UTF_8));
            if (yetkili) b.header("Authorization", "Bearer " + token());
            HttpResponse<String> c;
            try {
                c = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (HttpTimeoutException e) {
                throw new IkasHatasi(islem + ": zaman aşımı (" + ayarlar.zamanAsimi().toSeconds() + " sn)", true, e);
            } catch (IOException e) {
                throw new IkasHatasi(islem + ": bağlantı hatası: " + e.getMessage(), true, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IkasHatasi(islem + ": kesildi", true, e);
            }
            kotaOku(c);
            if (c.statusCode() != 429) return c;
            if (deneme >= HIZ_TEKRAR) throw new IkasHatasi(islem + ": HTTP 429 hız sınırı (" + (deneme + 1) + " deneme)", true, null);
            if (kotaYenilenme <= System.currentTimeMillis()) kotaYenilenme = System.currentTimeMillis() + bekleme(c);
            kalanKota = 0;
        }
    }

    /** Kota yedeğe indiyse pencerenin yenilenmesini bekler. */
    private void kotaBekle(String islem) {
        long kalan = kotaYenilenme - System.currentTimeMillis();
        if (kalanKota > YEDEK_KOTA || kalan <= 0) return;
        try {
            Thread.sleep(Math.min(kalan + 100, EN_COK_BEKLEME_MS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IkasHatasi(islem + ": hız sınırı beklemesi kesildi", true, e);
        }
        kalanKota = Integer.MAX_VALUE;
    }

    private void kotaOku(HttpResponse<String> c) {
        String kalan = c.headers().firstValue("x-ratelimit-remaining").orElse(null);
        String yenilenme = c.headers().firstValue("x-ratelimit-reset").orElse(null);
        try {
            if (kalan != null) kalanKota = Integer.parseInt(kalan.trim());
            if (yenilenme != null) kotaYenilenme = Instant.parse(yenilenme.trim()).toEpochMilli();
        } catch (NumberFormatException | DateTimeParseException e) {
            // Başlık beklenmedik biçimde → kota takibi yapılmaz, istek yine işlenir.
        }
    }

    /** 429'da pencere sonu bilinmiyorsa: retry-after (İkas milisaniye gönderiyor), o da yoksa 10 sn. */
    private static long bekleme(HttpResponse<String> c) {
        try {
            long ms = Long.parseLong(c.headers().firstValue("retry-after").orElse("10000").trim());
            return Math.min(Math.max(ms, 1000), EN_COK_BEKLEME_MS);
        } catch (NumberFormatException e) {
            return 10_000;
        }
    }
}
