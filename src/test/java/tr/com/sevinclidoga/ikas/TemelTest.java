package tr.com.sevinclidoga.ikas;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** JSON, şema/belge kurulumu, istemci (token, 401, hata çözümü, yazma kilidi). */
class TemelTest {

    // ── JSON ──

    @Test
    void json_gidisDonus_kacislarVeSayilar() {
        String m = "{\"a\":\"ş\\\"ğ\\n\\u00fc\",\"b\":[1,2.50,-3e2,true,false,null],\"c\":{}}";
        Object o = Json.coz(m);
        IkasVeri v = IkasVeri.of(o);
        assertEquals("ş\"ğ\nü", v.metin("a"));
        assertEquals(new BigDecimal("2.50"), v.liste("b").get(1).sayi());
        assertEquals(0, new BigDecimal("-300").compareTo(v.liste("b").get(2).sayi()));
        assertEquals(Boolean.TRUE, IkasVeri.of(((List<?>) ((Map<?, ?>) o).get("b")).get(3)).ham());
        // -3e2 yazılınca -300 olur (sayısal eşit); kararlılık: yaz → çöz → yaz aynı metni verir
        assertEquals(Json.yaz(o), Json.yaz(Json.coz(Json.yaz(o))));
        assertEquals("{\"x\":\"\\u0001\"}", Json.yaz(Map.of("x", "\u0001")));
    }

    @Test
    void json_gecersizMetin() {
        for (String k : List.of("{", "[1,]", "{\"a\" 1}", "tru", "\"kapanmayan", "1 2")) {
            assertThrows(IllegalArgumentException.class, () -> Json.coz(k), k);
        }
    }

    @Test
    void ikasVeri_yolVeEksikAlanlar() {
        IkasVeri v = IkasVeri.of(Json.coz("{\"s\":{\"satirlar\":[{\"sku\":\"A\"},{\"sku\":\"B\"}],\"t\":1730000000000}}"));
        assertEquals("B", v.yol("s.satirlar.1.sku").metin());
        assertNull(v.yol("s.yok.olmayan").metin());
        assertTrue(v.alan("yok").liste().isEmpty());
        assertEquals(1730000000000L, v.alan("s").zaman("t").toEpochMilli());
    }

    // ── şema ──

    @Test
    void sema_135Islem_65Sorgu_70Mutasyon() {
        IkasSema s = IkasSema.varsayilan();
        assertEquals(65, s.sorgular().size());
        assertEquals(70, s.mutasyonlar().size());
        assertTrue(s.islem("saveWebhook").mutasyonMu());
        assertFalse(s.islem("listOrder").mutasyonMu());
        assertTrue(s.enumDegerleri("CustomerB2BStatusEnum").contains("ACTIVE_ACCOUNT"));
    }

    @Test
    void sema_belgeKurar_yalnizVerilenArgumanlar() {
        String b = IkasSema.varsayilan().belge("listOrder", Map.of("pagination", Map.of("page", 1, "limit", 2)), 1);
        assertTrue(b.startsWith("query($pagination: PaginationInput) { listOrder(pagination: $pagination) { "), b);
        assertTrue(b.contains(" count ") && b.contains(" data { ") && b.contains(" orderNumber "), b);
        assertFalse(b.contains("$sort"), "verilmeyen argüman bildirilmez");
    }

    @Test
    void sema_eksikVeBilinmeyenArguman() {
        IkasSema s = IkasSema.varsayilan();
        var e = assertThrows(IllegalArgumentException.class, () -> s.belge("listCity", Map.of(), 1));
        assertTrue(e.getMessage().contains("stateId"));
        assertThrows(IllegalArgumentException.class, () -> s.belge("listOrder", Map.of("olmayan", 1), 1));
        assertThrows(IllegalArgumentException.class, () -> s.islem("boyleBirSeyYok"));
    }

    @Test
    void sema_bozukAlanSecilmez_skalerDonusteSecimYok() {
        IkasSema s = IkasSema.varsayilan();
        assertFalse(s.secim("OrderSession", 1).contains("journeyId"), "İkas journeyId'yi null döndürüyor");
        assertTrue(s.secim("OrderSession", 1).contains("orderId"));
        String b = s.belge("getImageUploadUrl", Map.of("imageId", "x"), 2);
        assertTrue(b.endsWith("getImageUploadUrl(imageId: $imageId) }"), b);
        assertEquals("", s.secim("String", 3));
    }

    // ── istemci ──

    @Test
    void token_birKezAlinir_onbellekte() throws Exception {
        try (var sahte = new SahteIkas()) {
            var ist = new IkasIstemci(sahte.ayarlar(false));
            ist.calistir("me", Map.of());
            ist.calistir("listLanguage", Map.of());
            assertEquals(1, sahte.tokenIstegi.get());
            assertEquals("Bearer tok-1", sahte.sonYetki);
            assertTrue(sahte.sonUa.startsWith("ikas-admin-java/"), "varsayılan Java UA'sı İkas'ta 403 alabiliyor");
        }
    }

    @Test
    void cevap401_tokenYenilenir_birKezTekrar() throws Exception {
        try (var sahte = new SahteIkas()) {
            var ist = new IkasIstemci(sahte.ayarlar(false));
            ist.calistir("me", Map.of());
            sahte.kalan401.set(1);
            IkasVeri v = ist.calistir("listLanguage", Map.of());
            assertFalse(v.liste().isEmpty());
            assertEquals(2, sahte.tokenIstegi.get());
        }
    }

    @Test
    void cevap429_pencereSonunaKadarBeklenirTekrarDenenir() throws Exception {
        try (var sahte = new SahteIkas()) {
            var ist = new IkasIstemci(sahte.ayarlar(false));
            sahte.kalan429.set(2);
            long t0 = System.nanoTime();
            assertFalse(ist.calistir("listLanguage", Map.of()).liste().isEmpty());
            assertEquals(3, sahte.graphqlIstegi.get(), "iki 429 + bir başarılı");
            assertTrue((System.nanoTime() - t0) / 1_000_000 >= 300, "pencere sonu (reset başlığı) beklenmeli");
        }
    }

    @Test
    void cevap429_tekrarlarTukenirse_geciciHata() throws Exception {
        try (var sahte = new SahteIkas()) {
            var ist = new IkasIstemci(sahte.ayarlar(false));
            sahte.kalan429.set(IkasIstemci.HIZ_TEKRAR + 1);
            var h = assertThrows(IkasHatasi.class, () -> ist.calistir("listLanguage", Map.of()));
            assertTrue(h.agHatasiMi(), "429 geçici hatadır");
            assertTrue(h.getMessage().contains("429"));
        }
    }

    @Test
    void kotaYedegeInince_istekGondermedenBeklenir() throws Exception {
        try (var sahte = new SahteIkas()) {
            var ist = new IkasIstemci(sahte.ayarlar(false));
            sahte.kotaBasligi = IkasIstemci.YEDEK_KOTA;
            ist.calistir("listLanguage", Map.of());
            sahte.kotaBasligi = null;
            long t0 = System.nanoTime();
            ist.calistir("listLanguage", Map.of());
            assertTrue((System.nanoTime() - t0) / 1_000_000 >= 300, "kota yedekteyken pencere sonu beklenmeli");
            t0 = System.nanoTime();
            ist.calistir("listLanguage", Map.of());
            assertTrue((System.nanoTime() - t0) / 1_000_000 < 300, "kota bilinmiyorsa beklenmez");
        }
    }

    @Test
    void graphqlHatasi_kodVeDogrulamaCozulur_bulunamadiTaninir() throws Exception {
        try (var sahte = new SahteIkas()) {
            sahte.cevap("cancelFulfillment", "mutasyon-cancelFulfillment-bulunamadi");
            var ist = new IkasIstemci(sahte.ayarlar(true));
            var h = assertThrows(IkasHatasi.class, () -> ist.calistir("cancelFulfillment",
                    Map.of("input", Map.of("orderId", "x", "orderPackageId", "y", "lines", List.of()))));
            assertEquals("ARGUMENT_VALIDATION_ERROR", h.kod());
            assertEquals(List.of("Referenced id not found"), h.hatalar().get(0).dogrulama().get("orderId"));
            assertTrue(h.bulunamadiMi());
            assertFalse(h.agHatasiMi());
        }
    }

    @Test
    void yetkisiz_UNAUTHORIZED() throws Exception {
        try (var sahte = new SahteIkas()) {
            sahte.hamCevap("listIkasWallet", 200, "{\"errors\":[{\"message\":\"UNAUTHORIZED\",\"extensions\":{\"code\":\"UNAUTHORIZED\"}}],\"data\":null}");
            var h = assertThrows(IkasHatasi.class, () -> new IkasIstemci(sahte.ayarlar(false)).calistir("listIkasWallet", Map.of()));
            assertEquals("UNAUTHORIZED", h.kod());
        }
    }

    @Test
    void mutasyon_izinsiz_istekGitmez() throws Exception {
        try (var sahte = new SahteIkas()) {
            var ist = new IkasIstemci(sahte.ayarlar(false));
            var h = assertThrows(IkasHatasi.class, () -> ist.calistir("deleteProductList", Map.of("idList", List.of("x"))));
            assertTrue(h.getMessage().contains("CANLI"));
            assertThrows(IkasHatasi.class, () -> ist.ham("mutation { deleteProductList(idList: [\"x\"]) }", Map.of()));
            assertEquals(0, sahte.graphqlIstegi.get());
            assertEquals(0, sahte.tokenIstegi.get());
        }
    }

    @Test
    void sunucuHatasi_ve_baglantiYok_agHatasi() throws Exception {
        try (var sahte = new SahteIkas()) {
            sahte.hamCevap("me", 502, "<html>Bad Gateway</html>");
            var h = assertThrows(IkasHatasi.class, () -> new IkasIstemci(sahte.ayarlar(false)).calistir("me", Map.of()));
            assertTrue(h.agHatasiMi());
        }
        var ayar = new IkasAyarlari("x", "k", "s", Duration.ofSeconds(2), false, "http://127.0.0.1:1/graphql", "http://127.0.0.1:1/token");
        var h = assertThrows(IkasHatasi.class, () -> new IkasIstemci(ayar).calistir("me", Map.of()));
        assertTrue(h.agHatasiMi());
    }

    @Test
    void ayarlar_sirYazdirilmaz() {
        IkasAyarlari a = IkasAyarlari.of("magaza", "kimlik", "cok-gizli");
        assertFalse(a.toString().contains("cok-gizli"));
        assertFalse(a.yazmaIzni());
        assertTrue(a.yazmayaIzinVer().yazmaIzni());
        assertEquals("https://magaza.myikas.com/api/admin/oauth/token", a.tokenAdresi());
    }
}
