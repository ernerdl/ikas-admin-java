package tr.com.sevinclidoga.ikas;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CANLI İkas mağazasına karşı denemeler — yalnız güvenli işlemler. Varsayılan derlemede çalışmaz:
 * <pre>IKAS_CLIENT_ID=… IKAS_CLIENT_SECRET=… IKAS_MAGAZA=… ./mvnw test -Pcanli-deneme</pre>
 * <ul>
 *   <li>Okumalar: gerçek veriyle, değişiklik yok.</li>
 *   <li>Tanım kayıtları (etiket, birim, satış kanalsız marka, arşivli tedarikçi, 2099'da başlayan kuponlu kanalsız
 *       kampanya…): oluştur → sil → yok olduğunu doğrula. Müşteriye görünmez, webhook tetiklemez.</li>
 *   <li>Sipariş/müşteri yazmaları: YALNIZ var olmayan sahte kimlikle → İkas "bulunamadı" der, hiçbir şey değişmez.</li>
 * </ul>
 * Sipariş/ürün/müşteri üzerinde gerçek yazma YAPILMAZ (İkas webhook'u canlı sistemleri tetikler).
 */
@Tag("canli")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CanliTest {

    static final String ON = "zz-kesif-deneme-silinecek";

    IkasAdmin okur, yazar;
    String siparisId, musteriId;

    @BeforeAll
    void kur() {
        String id = System.getenv("IKAS_CLIENT_ID"), sir = System.getenv("IKAS_CLIENT_SECRET"), mg = System.getenv("IKAS_MAGAZA");
        assumeTrue(id != null && sir != null && mg != null, "IKAS_CLIENT_ID / IKAS_CLIENT_SECRET / IKAS_MAGAZA yok → atlandı");
        IkasAyarlari a = IkasAyarlari.of(mg, id, sir).zamanAsimiyla(Duration.ofSeconds(90));
        okur = new IkasAdmin(a);
        yazar = new IkasAdmin(new IkasIstemci(a.yazmayaIzinVer(), okur.istemci().sema()));
        IkasVeri s = okur.siparisler(1, 1).kayitlar().get(0);
        siparisId = s.metin("id");
        musteriId = s.yol("customer.id").metin();
    }

    // ── okumalar ──

    @Test
    void magazaVeAyarlar() {
        List<Supplier<IkasVeri>> l = List.of(okur::magaza, okur::lisans, okur::ben, okur::satisKanali, okur::satisKanallari,
                okur::vitrinler, okur::politikalar, okur::diller, okur::paraBirimleri, okur::magazaAyarlari, okur::kargoAyarlari,
                okur::kargoFirmalari, okur::odemeYontemleri, okur::depolar, okur::fiyatListeleri, okur::vergiAyarlari,
                okur::genelVergiAyarlari, okur::subeler, okur::terminaller, okur::webhooklar);
        for (Supplier<IkasVeri> f : l) assertDoesNotThrow(f::get);
        assertFalse(okur.kargoFirmalari().liste().isEmpty());
        String vitrin = okur.vitrinler().liste().get(0).metin("id");
        assertDoesNotThrow(() -> okur.vitrinScriptleri(vitrin));
    }

    @Test
    void konumZinciri_ulkedenMahalleye() {
        String tr = okur.ulkeler().liste().stream().filter(u -> "TR".equals(u.metin("iso2"))).findFirst().orElseThrow().metin("id");
        String bolge = okur.bolgeler(tr).liste().get(0).metin("id");
        IkasVeri il = okur.iller(bolge).liste().stream().filter(x -> "Ankara".equals(x.metin("name"))).findFirst().orElseThrow();
        IkasVeri ilce = okur.ilceler(il.metin("id")).liste().get(0);
        assertFalse(okur.mahalleler(ilce.metin("id")).liste().isEmpty());
    }

    @Test
    void siparisOkumalari() {
        assertNotNull(okur.siparis(siparisId).metin("orderNumber"));
        String no = okur.siparis(siparisId).metin("orderNumber");
        assertEquals(siparisId, okur.siparisNoIle(no).metin("id"));
        assertDoesNotThrow(() -> okur.siparisOdemeleri(siparisId));
        assertDoesNotThrow(() -> okur.siparisOturumlari(siparisId), "journeyId İkas'ta bozuk; dışarıda bırakılmalı");
        long dun = LocalDate.now().minusDays(7).atStartOfDay().toInstant(ZoneOffset.ofHours(3)).toEpochMilli();
        assertDoesNotThrow(() -> okur.siparisler(Map.of("orderedAt", Map.of("gte", dun)), 1, 5));
        assertDoesNotThrow(okur::siparisEtiketleri);
        assertDoesNotThrow(() -> okur.terkEdilenSepetler(1, 3));
    }

    @Test
    void urunOkumalari() {
        IkasVeri u = okur.urunler(1, 1).kayitlar().get(0);
        assertEquals(u.metin("id"), okur.urun(u.metin("id")).metin("id"));
        String sku = u.yol("variants.0.sku").metin();
        if (sku != null) {
            assertEquals(u.metin("id"), okur.skuIleUrun(sku).metin("id"));
            assertFalse(okur.skularlaAra(List.of(sku)).kayitlar().isEmpty());
        }
        String kelime = u.metin("name").split(" ")[0];
        assertFalse(okur.urunAra(kelime, 1, 5).kayitlar().isEmpty(), "ürün adının ilk kelimesiyle arama");
        for (Supplier<IkasVeri> f : List.<Supplier<IkasVeri>>of(() -> okur.stoklar(null, 1, 3), okur::kategoriler, okur::markalar,
                okur::urunEtiketleri, okur::birimler, okur::ozellikler, okur::varyantTipleri, okur::secenekSetleri, okur::tedarikciler,
                okur::adetIndirimleri, okur::urunSiralamalari)) {
            assertDoesNotThrow(f::get);
        }
        assertTrue(okur.gorselYuklemeAdresi(UUID.randomUUID().toString()).startsWith("http"), "imzalı yükleme adresi; yükleme yapmaz");
        assertFalse(okur.videoYuklemeAdresi(UUID.randomUUID().toString()).bosMu());
    }

    @Test
    void musteriVeKampanyaOkumalari() {
        assumeTrue(musteriId != null);
        assertEquals(musteriId, okur.musteri(musteriId).metin("id"));
        assertDoesNotThrow(() -> okur.musteriler(1, 3));
        assertDoesNotThrow(okur::musteriGruplari);
        assertDoesNotThrow(okur::musteriEtiketleri);
        assertDoesNotThrow(okur::musteriOzellikleri);
        assertDoesNotThrow(() -> okur.magazaPuani(musteriId));
        assertTrue(okur.epostaIleMusteri("zz-kesif-" + UUID.randomUUID() + "@example.invalid").bosMu(), "olmayan e-posta → boş");
        String ad = okur.musteri(musteriId).metin("firstName");
        if (ad != null) assertFalse(okur.musteriAra(ad, 1, 3).kayitlar().isEmpty(), "müşteri adıyla arama");
        assertDoesNotThrow(() -> okur.kampanyalar(1, 3));
        assertDoesNotThrow(() -> okur.kuponlar(null, 1, 3));
        assertDoesNotThrow(okur::kampanyaTeklifleri);
    }

    @Test
    void walletYetkisiz() {
        IkasHatasi h = assertThrows(IkasHatasi.class, () -> okur.istemci().calistir("listIkasWallet", Map.of()));
        assertEquals("UNAUTHORIZED", h.kod());
    }

    // ── tanım kayıtları: oluştur → sil → doğrula ──

    @Test
    void tanimlar_olusturSilDogrula() {
        String ad = ON + "-" + System.currentTimeMillis();
        record T(Supplier<IkasVeri> kaydet, String sil, String liste) {
        }
        List<T> tanimlar = List.of(
                new T(() -> yazar.urunEtiketiKaydet(ad), "deleteProductTagList", "listProductTag"),
                new T(() -> yazar.birimKaydet(ad), "deleteProductUnitList", "listProductUnit"),
                new T(() -> yazar.markaKaydet(ad, List.of()), "deleteProductBrandList", "listProductBrand"),
                new T(() -> yazar.ozellikKaydet(ad, "TEXT", List.of("deneme")), "deleteProductAttributeList", "listProductAttribute"),
                new T(() -> yazar.varyantTipiKaydet(ad, List.of("deneme")), "deleteVariantTypeList", "listVariantType"),
                new T(() -> yazar.tedarikciKaydet(Map.of("name", ad, "status", "ARCHIVED")), "deleteVendorList", "listVendor"),
                new T(() -> yazar.siparisEtiketiKaydet(ad), "deleteOrderTagList", "listOrderTag"),
                new T(() -> yazar.musteriEtiketiKaydet(ad), "deleteCustomerTagList", "listCustomerTag"),
                new T(() -> yazar.musteriGrubuKaydet(ad), "deleteCustomerGroupList", "listCustomerGroup"));
        for (T t : tanimlar) {
            String id = t.kaydet().get().metin("id");
            assertNotNull(id, t.liste());
            yazar.tanimSil(t.sil(), List.of(id));
            IkasVeri kalan = okur.istemci().calistirSecimle(t.liste(), Map.of("id", Map.of("eq", id)), "{ id }");
            assertTrue(kalan.liste().isEmpty(), t.liste() + ": silinen kayıt hâlâ var");
        }
    }

    @Test
    void kampanyaKupon_2099_kanalsiz_olusturSil() {
        long bas = LocalDate.of(2099, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        IkasVeri k = yazar.kampanyaKaydet(Map.ofEntries(Map.entry("title", ON + "-" + System.currentTimeMillis()),
                Map.entry("type", "FIXED_AMOUNT"), Map.entry("applicablePrice", "SELL_PRICE"),
                Map.entry("canCombineWithOtherCampaigns", false), Map.entry("hasCoupon", true),
                Map.entry("applyCampaignToProductPrice", false), Map.entry("applicableCustomerGroupIds", List.of()),
                Map.entry("applicableCustomerIds", List.of()), Map.entry("applicableCustomerSegmentIds", List.of()),
                Map.entry("currencyCodes", List.of("TRY")), Map.entry("periodUsageLimits", List.of()),
                Map.entry("salesChannelIds", List.of()), Map.entry("translations", List.of()),
                Map.entry("dateRange", Map.of("start", bas, "end", bas + 86_400_000L)), Map.entry("usageLimit", 1),
                Map.entry("fixedDiscount", Map.of("amount", 1, "filters", List.of(), "isApplyByCartAmount", false))));
        String kid = k.metin("id");
        assertNotNull(kid);
        try {
            IkasVeri kup = yazar.kuponEkle(kid, List.of(Map.of("code", "ZZKESIF" + System.currentTimeMillis() % 100000,
                    "usageLimit", 1, "canCombineWithOtherCampaigns", false)));
            assertEquals(1, kup.liste().size());
            assertTrue(yazar.kuponSil(List.of(kup.liste().get(0).metin("id"))));
        } finally {
            yazar.kampanyaSil(List.of(kid));
        }
        assertTrue(okur.istemci().calistirSecimle("listCampaign", Map.of("id", Map.of("eq", kid)), "{ data { id } }")
                .kayitlar().isEmpty());
    }

    // ── sipariş/müşteri yazmaları: yalnız sahte kimlik ──

    @Test
    void siparisYazmalari_sahteKimlik_bulunamadi() {
        String yok = UUID.randomUUID().toString();
        List<Runnable> l = List.of(
                () -> yazar.faturaEkle(yok, yok, "KESIF-0", null, false, false),
                () -> yazar.satirIptal(yok, List.of()),
                () -> yazar.adresGuncelle(yok, null, null),
                () -> yazar.odemeLinki(yok),
                () -> yazar.depoDegistir(yok, List.of(), yok),
                () -> yazar.siparisEtiketiEkle(yok, yok),
                () -> yazar.siparisEtiketiKaldir(yok, yok),
                () -> yazar.magazaPuaniAyarla(yok, 0));
        for (Runnable r : l) {
            IkasHatasi e = assertThrows(IkasHatasi.class, r::run);
            assertTrue(e.bulunamadiMi(), e.getMessage());
        }
        String satir = UUID.randomUUID().toString();
        List<Runnable> paket = List.of(
                () -> yazar.iade(Map.of("orderId", yok, "stockLocationId", yok, "orderRefundLines",
                        List.of(Map.of("orderLineItemId", satir, "price", 1, "quantity", 1, "restockItems", false)))),
                () -> yazar.paketIptal(Map.of("orderId", yok, "orderPackageId", yok)),
                () -> yazar.kargoyaVer(Map.of("orderId", yok, "lines", List.of(Map.of("orderLineItemId", satir, "quantity", 1)))),
                () -> yazar.paketDurumu(Map.of("orderId", yok, "packages", List.of(Map.of("packageId", yok, "status", "DELIVERED")))),
                () -> yazar.satirGuncelle(Map.of("orderId", yok, "editReason", "kesif", "restockItems", false, "orderLineItems", List.of())));
        for (Runnable r : paket) {
            IkasHatasi h = assertThrows(IkasHatasi.class, r::run, "sahte siparişte hata beklenir");
            assertTrue(h.bulunamadiMi(), h.getMessage());
        }
        IkasHatasi h = assertThrows(IkasHatasi.class, () -> yazar.musteriSil(List.of(yok)));
        assertTrue(h.bulunamadiMi(), h.getMessage());
        // İkas bunlarda var olmayan kimliğe de true döner (belgelenen davranış; değişiklik olmaz).
        assertTrue(yazar.b2bDurumu(yok, "DISABLED_ACCOUNT"));
        assertTrue(yazar.abonelikDurumu(yok, "NOT_SUBSCRIBED"));
        assertTrue(yazar.zamanCizelgesineNot("ORDER", yok, "kesif"));
        assertFalse(yazar.urunSil(List.of(yok)), "var olmayan ürün → false");
        assertFalse(yazar.bekleyenOdemeleriOnayla(yok, List.of("MONEY_ORDER")));
    }

    @Test
    void faturaPdfAdresi_gercekSiparis() {
        IkasVeri s = okur.istemci().calistirSecimle("listOrder", Map.of("pagination", Map.of("page", 1, "limit", 50)),
                "{ data { id invoices { id } } }").kayitlar().stream().filter(o -> !o.liste("invoices").isEmpty()).findFirst().orElse(null);
        assumeTrue(s != null, "son 50 siparişte İkas faturası yok");
        String adres = yazar.faturaPdfAdresi(s.metin("id"), s.liste("invoices").get(0).metin("id"));
        assertNotNull(adres);
        assertTrue(adres.startsWith("http"));
    }

    /** Güvenlik ağı: yarıda kalan test olursa "zz-kesif" adlı her tanım kaydı silinir. */
    @AfterAll
    void supur() {
        if (yazar == null) return;
        record L(String liste, String sil, String alan) {
        }
        for (L l : List.of(new L("listProductTag", "deleteProductTagList", "name"), new L("listProductUnit", "deleteProductUnitList", "name"),
                new L("listProductBrand", "deleteProductBrandList", "name"), new L("listProductAttribute", "deleteProductAttributeList", "name"),
                new L("listVariantType", "deleteVariantTypeList", "name"), new L("listVendor", "deleteVendorList", "name"),
                new L("listOrderTag", "deleteOrderTagList", "name"), new L("listCustomerTag", "deleteCustomerTagList", "name"),
                new L("listCustomerGroup", "deleteCustomerGroupList", "name"))) {
            List<String> ids = new ArrayList<>();
            for (IkasVeri v : okur.istemci().calistirSecimle(l.liste(), Map.of(), "{ id " + l.alan() + " }").liste()) {
                String ad = v.metin(l.alan());
                if (ad != null && ad.startsWith(ON)) ids.add(v.metin("id"));
            }
            if (!ids.isEmpty()) {
                System.out.println("SÜPÜRGE: " + l.liste() + " → " + ids.size() + " kayıt silindi");
                yazar.tanimSil(l.sil(), ids);
            }
        }
        List<String> kampanyalar = new ArrayList<>();
        for (IkasVeri v : okur.istemci().calistirSecimle("listCampaign", Map.of("search", ON), "{ data { id title } }").kayitlar()) {
            if (v.metin("title") != null && v.metin("title").startsWith(ON)) kampanyalar.add(v.metin("id"));
        }
        if (!kampanyalar.isEmpty()) {
            System.out.println("SÜPÜRGE: kampanya → " + kampanyalar.size());
            yazar.kampanyaSil(kampanyalar);
        }
    }
}
