package tr.com.sevinclidoga.ikas;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Testler için sahte İkas: /token OAuth, /graphql isteğin işlem adına göre {@code src/test/resources/ornek/} altındaki
 * ANONİM GERÇEK cevabı döndürür (varsayılan: {@code sorgu-<işlem>} ya da {@code mutasyon-<işlem>}).
 */
final class SahteIkas implements AutoCloseable {

    private static final Pattern ISLEM = Pattern.compile("^\\s*(query|mutation)\\b[^{]*\\{\\s*(\\w+)");

    private final HttpServer sunucu;
    private final Map<String, String> cevaplar = new ConcurrentHashMap<>();
    private final Map<String, Integer> kodlar = new ConcurrentHashMap<>();
    final AtomicInteger tokenIstegi = new AtomicInteger();
    final AtomicInteger graphqlIstegi = new AtomicInteger();
    final AtomicInteger kalan401 = new AtomicInteger();
    final AtomicInteger kalan429 = new AtomicInteger();
    /** Her cevapta gönderilecek x-ratelimit-remaining (null → başlık yok). */
    volatile Integer kotaBasligi;
    volatile String sonGovde;
    volatile String sonYetki;
    volatile String sonUa;
    volatile String sonIslem;

    SahteIkas() throws IOException {
        sunucu = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        sunucu.createContext("/token", ex -> {
            tokenIstegi.incrementAndGet();
            yanit(ex, 200, "{\"access_token\":\"tok-" + tokenIstegi.get() + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}");
        });
        sunucu.createContext("/graphql", ex -> {
            graphqlIstegi.incrementAndGet();
            String govde = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            sonGovde = govde;
            sonYetki = ex.getRequestHeaders().getFirst("Authorization");
            sonUa = ex.getRequestHeaders().getFirst("User-Agent");
            if (kalan401.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                yanit(ex, 401, "{\"errors\":[{\"message\":\"Unauthorized\"}]}");
                return;
            }
            if (kalan429.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                ex.getResponseHeaders().add("x-ratelimit-remaining", "0");
                ex.getResponseHeaders().add("x-ratelimit-reset", java.time.Instant.now().plusMillis(300).toString());
                yanit(ex, 429, "Too Many Requests");
                return;
            }
            if (kotaBasligi != null) {
                ex.getResponseHeaders().add("x-ratelimit-remaining", String.valueOf(kotaBasligi));
                ex.getResponseHeaders().add("x-ratelimit-reset", java.time.Instant.now().plusMillis(400).toString());
            }
            String belge = IkasVeri.of(Json.coz(govde)).metin("query");
            Matcher m = ISLEM.matcher(belge);
            String islem = m.find() ? m.group(2) : "?";
            boolean mutasyon = m.find(0) && "mutation".equals(m.group(1));
            sonIslem = islem;
            String cevap = cevaplar.get(islem);
            if (cevap == null) cevap = ornekVarsa((mutasyon ? "mutasyon-" : "sorgu-") + islem);
            if (cevap == null) {
                yanit(ex, 200, "{\"errors\":[{\"message\":\"sahte: örnek yok " + islem + "\"}],\"data\":null}");
                return;
            }
            yanit(ex, kodlar.getOrDefault(islem, 200), cevap);
        });
        sunucu.start();
    }

    private static void yanit(com.sun.net.httpserver.HttpExchange ex, int kod, String govde) throws IOException {
        byte[] b = govde.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(kod, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }

    /** İşleme belirli örnekle cevap ver. */
    SahteIkas cevap(String islem, String ornekAdi) {
        cevaplar.put(islem, ornek(ornekAdi));
        return this;
    }

    SahteIkas hamCevap(String islem, int kod, String govde) {
        cevaplar.put(islem, govde);
        kodlar.put(islem, kod);
        return this;
    }

    IkasAyarlari ayarlar(boolean yazma) {
        String taban = "http://127.0.0.1:" + sunucu.getAddress().getPort();
        return new IkasAyarlari("deneme", "kimlik", "sir", null, yazma, taban + "/graphql", taban + "/token");
    }

    /** Son isteğin değişkenleri. */
    IkasVeri sonDegiskenler() {
        return IkasVeri.of(Json.coz(sonGovde)).alan("variables");
    }

    String sonBelge() {
        return IkasVeri.of(Json.coz(sonGovde)).metin("query");
    }

    static String ornek(String ad) {
        String s = ornekVarsa(ad);
        if (s == null) throw new IllegalArgumentException("örnek yok: " + ad);
        return s;
    }

    static String ornekVarsa(String ad) {
        try (InputStream in = SahteIkas.class.getResourceAsStream("/ornek/" + ad + ".json")) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        sunucu.stop(0);
    }
}
