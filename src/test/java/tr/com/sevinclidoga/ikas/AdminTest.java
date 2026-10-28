package tr.com.sevinclidoga.ikas;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** IkasAdmin hazır metotları — sahte sunucu + canlı İkas'tan alınmış anonim gerçek cevaplar (2026-10-28). */
class AdminTest {

    SahteIkas sahte;
    IkasAdmin okur, yazar;

    @BeforeEach
    void kur() throws Exception {
        sahte = new SahteIkas();
        okur = new IkasAdmin(sahte.ayarlar(false));
        yazar = new IkasAdmin(sahte.ayarlar(true));
    }

    @AfterEach
    void kapat() {
        sahte.close();
    }

    record Okuma(String islem, Function<IkasAdmin, IkasVeri> cagri) {
        @Override
        public String toString() {
            return islem;
        }
    }

    static Stream<Okuma> okumalar() {
        return Stream.of(
                new Okuma("getMerchant", IkasAdmin::magaza), new Okuma("getMerchantLicence", IkasAdmin::lisans),
                new Okuma("me", IkasAdmin::ben), new Okuma("getSalesChannel", IkasAdmin::satisKanali),
                new Okuma("listSalesChannel", IkasAdmin::satisKanallari), new Okuma("listStorefront", IkasAdmin::vitrinler),
                new Okuma("listStorefrontJSScript", a -> a.vitrinScriptleri("v1")), new Okuma("listStorefrontPolicy", IkasAdmin::politikalar),
                new Okuma("listLanguage", IkasAdmin::diller), new Okuma("listMerchantSettings", IkasAdmin::magazaAyarlari),
                new Okuma("listShippingSettings", IkasAdmin::kargoAyarlari), new Okuma("listCargoCompany", IkasAdmin::kargoFirmalari),
                new Okuma("listPaymentGateway", IkasAdmin::odemeYontemleri), new Okuma("listStockLocation", IkasAdmin::depolar),
                new Okuma("listPriceList", IkasAdmin::fiyatListeleri), new Okuma("listTaxSettings", IkasAdmin::vergiAyarlari),
                new Okuma("getGlobalTaxSettings", IkasAdmin::genelVergiAyarlari), new Okuma("listWebhook", IkasAdmin::webhooklar),
                new Okuma("listCountry", IkasAdmin::ulkeler), new Okuma("listState", a -> a.bolgeler("tr")),
                new Okuma("listCity", a -> a.iller("b")), new Okuma("listDistrict", a -> a.ilceler("i")),
                new Okuma("listTown", a -> a.mahalleler("c")), new Okuma("listOrder", a -> a.siparisler(1, 3)),
                new Okuma("listOrderTransactions", a -> a.siparisOdemeleri("s")), new Okuma("listOrderSession", a -> a.siparisOturumlari("s")),
                new Okuma("listOrderTag", IkasAdmin::siparisEtiketleri), new Okuma("listAbandonedCheckouts", a -> a.terkEdilenSepetler(1, 3)),
                new Okuma("listProduct", a -> a.urunler(1, 3)), new Okuma("searchProducts", a -> a.urunAra("ekmek", 1, 3)),
                new Okuma("listProductStockLocation", a -> a.stoklar(null, 1, 3)), new Okuma("listCategory", IkasAdmin::kategoriler),
                new Okuma("listProductBrand", IkasAdmin::markalar), new Okuma("listProductTag", IkasAdmin::urunEtiketleri),
                new Okuma("listVariantType", IkasAdmin::varyantTipleri), new Okuma("listVendor", IkasAdmin::tedarikciler),
                new Okuma("listCustomer", a -> a.musteriler(1, 3)), new Okuma("listCustomerGroup", IkasAdmin::musteriGruplari),
                new Okuma("listCustomerTag", IkasAdmin::musteriEtiketleri), new Okuma("listCampaign", a -> a.kampanyalar(1, 3)),
                new Okuma("listCoupon", a -> a.kuponlar(null, 1, 3)), new Okuma("getVideoUploadUrl", a -> a.videoYuklemeAdresi("u")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("okumalar")
    void okuma_dogruIslem_veriDoner(Okuma o) {
        IkasVeri v = o.cagri().apply(okur);
        assertEquals(o.islem(), sahte.sonIslem);
        assertFalse(v.bosMu(), o.islem() + " boş döndü");
        assertTrue(sahte.sonBelge().startsWith("query"));
    }

    @Test
    void bosListeDonenOkumalar_hataVermez() {
        for (Function<IkasAdmin, IkasVeri> f : List.<Function<IkasAdmin, IkasVeri>>of(IkasAdmin::subeler, IkasAdmin::terminaller,
                IkasAdmin::paraBirimleri, IkasAdmin::birimler, IkasAdmin::ozellikler, IkasAdmin::secenekSetleri,
                IkasAdmin::adetIndirimleri, IkasAdmin::urunSiralamalari, IkasAdmin::musteriOzellikleri, IkasAdmin::kampanyaTeklifleri,
                a -> a.magazaPuani("m"))) {
            assertDoesNotThrow(() -> f.apply(okur));
        }
    }

    @Test
    void siparisler_sayfaVeSiralama_kayitlar() {
        IkasVeri v = okur.siparisler(Map.of("status", Map.of("eq", "CREATED")), 2, 3);
        IkasVeri d = sahte.sonDegiskenler();
        assertEquals(2, d.alan("pagination").tamsayi("page"));
        assertEquals(3, d.alan("pagination").tamsayi("limit"));
        assertEquals("-orderedAt", d.metin("sort"));
        assertEquals("CREATED", d.yol("status.eq").metin());
        assertEquals(3, v.kayitlar().size());
        assertNotNull(v.kayitlar().get(0).metin("orderNumber"));
        assertTrue(sahte.sonBelge().contains("$status: OrderStatusEnumInputFilter"));
    }

    @Test
    void tekSiparis_idIle_ilkKayit() {
        IkasVeri s = okur.siparis("abc");
        assertEquals("abc", sahte.sonDegiskenler().yol("id.eq").metin());
        assertNotNull(s.metin("id"));
    }

    @Test
    void urunAra_sonuclarResultsIcinde() {
        IkasVeri v = okur.urunAra("ekmek", 1, 3);
        assertEquals("ekmek", sahte.sonDegiskenler().yol("input.query").metin());
        assertFalse(v.kayitlar().isEmpty(), "searchProducts kayıtları results alanında");
    }

    @Test
    void konum_zorunluFiltreler() {
        okur.iller("bolge-1");
        assertEquals("bolge-1", sahte.sonDegiskenler().yol("stateId.eq").metin());
        assertTrue(sahte.sonBelge().contains("$stateId: StringFilterInput!"));
    }

    // ── yazma ──

    @Test
    void yazma_izinsiz_hicbirIstekGitmez() {
        assertThrows(IkasHatasi.class, () -> okur.urunEtiketiKaydet("x"));
        assertThrows(IkasHatasi.class, () -> okur.webhookSil(List.of("store/order/created")));
        assertThrows(IkasHatasi.class, () -> okur.faturaPdfAdresi("s", "f"), "getOrderInvoicePdfUrl İkas'ta mutasyon");
        assertEquals(0, sahte.graphqlIstegi.get());
    }

    @Test
    void tanimKaydetVeSil_gercekCevaplarla() {
        for (String[] t : new String[][]{{"saveProductTag", "deleteProductTagList"}, {"saveProductUnit", "deleteProductUnitList"},
                {"saveProductBrand", "deleteProductBrandList"}, {"saveVendor", "deleteVendorList"}, {"saveCustomerTag", "deleteCustomerTagList"},
                {"saveCustomerGroup", "deleteCustomerGroupList"}, {"saveOrderTag", "deleteOrderTagList"},
                {"saveVariantType", "deleteVariantTypeList"}, {"saveProductAttribute", "deleteProductAttributeList"}}) {
            IkasVeri k = yazar.tanimKaydet(t[0], Map.of("name", "x"));
            assertNotNull(k.metin("id"), t[0]);
            assertTrue(yazar.tanimSil(t[1], List.of(k.metin("id"))), t[1]);
            assertEquals(t[1], sahte.sonIslem);
        }
    }

    @Test
    void hazirTanimMetotlari_dogruGirdi() {
        yazar.ozellikKaydet("Menşei", "TEXT", List.of("Türkiye"));
        assertEquals("Türkiye", sahte.sonDegiskenler().yol("input.options.0.name").metin());
        assertEquals("TEXT", sahte.sonDegiskenler().yol("input.type").metin());
        yazar.markaKaydet("Marka", List.of());
        assertTrue(sahte.sonDegiskenler().yol("input.salesChannelIds").liste().isEmpty(), "kanalsız marka vitrinde görünmez");
        yazar.musteriGrubuKaydet("Grup");
        assertEquals(Boolean.FALSE, sahte.sonDegiskenler().alan("input").mantiksal("isAutomated"));
    }

    @Test
    void kampanyaKupon_zinciri() {
        IkasVeri k = yazar.kampanyaKaydet(Map.of("title", "x"));
        assertNotNull(k.metin("id"));
        IkasVeri kupon = yazar.kuponEkle(k.metin("id"), List.of(Map.of("code", "KOD", "canCombineWithOtherCampaigns", false)));
        assertFalse(kupon.liste().isEmpty());
        assertEquals("KOD", sahte.sonDegiskenler().yol("input.coupons.0.code").metin());
        assertTrue(yazar.kuponSil(List.of(kupon.liste().get(0).metin("id"))));
        assertTrue(yazar.kampanyaSil(List.of(k.metin("id"))));
    }

    @Test
    void siparisYazmalari_bulunamadiHatasiTanir() {
        sahte.cevap("addOrderInvoice", "mutasyon-addOrderInvoice-bulunamadi")
                .cevap("cancelOrderLine", "mutasyon-cancelOrderLine-bulunamadi")
                .cevap("updateOrderAddresses", "mutasyon-updateOrderAddresses-bulunamadi")
                .cevap("generateOrderPaymentLink", "mutasyon-generateOrderPaymentLink-bulunamadi")
                .cevap("changeStockLocation", "mutasyon-changeStockLocation-bulunamadi")
                .cevap("addOrderTag", "mutasyon-addOrderTag-bulunamadi")
                .cevap("updateStoreCreditAmount", "mutasyon-updateStoreCreditAmount-bulunamadi");
        List<Runnable> islemler = List.of(
                () -> yazar.faturaEkle("s", "app", "F1", null, false, false),
                () -> yazar.satirIptal("s", List.of()),
                () -> yazar.adresGuncelle("s", Map.of("city", "x"), null),
                () -> yazar.odemeLinki("s"),
                () -> yazar.depoDegistir("s", List.of(), "d"),
                () -> yazar.siparisEtiketiEkle("s", "e"),
                () -> yazar.magazaPuaniAyarla("p", 0));
        for (Runnable r : islemler) {
            IkasHatasi h = assertThrows(IkasHatasi.class, r::run);
            assertTrue(h.bulunamadiMi(), h.getMessage());
        }
    }

    @Test
    void sahteKimlik_ucIslemTrueDoner_ikisiDurustFalse() {
        sahte.cevap("updateCustomerB2BStatus", "mutasyon-updateCustomerB2BStatus-bulunamadi")
                .cevap("updateSubscriptionStatus", "mutasyon-updateSubscriptionStatus-bulunamadi")
                .cevap("addCustomTimelineEntry", "mutasyon-addCustomTimelineEntry-bulunamadi")
                .cevap("approvePendingOrderTransactions", "mutasyon-approvePendingOrderTransactions-bulunamadi")
                .cevap("deleteProductList", "mutasyon-deleteProductList-yok");
        assertTrue(yazar.b2bDurumu("yok", "WAITING_FOR_APPROVAL"));
        assertTrue(yazar.abonelikDurumu("yok", "NOT_SUBSCRIBED"));
        assertTrue(yazar.zamanCizelgesineNot("ORDER", "yok", "not"));
        // Bunlar dürüst: var olmayan kimlikte false ("hiçbir şey yapılmadı")
        assertFalse(yazar.bekleyenOdemeleriOnayla("yok", List.of("MONEY_ORDER")));
        assertFalse(yazar.urunSil(List.of("yok")));
    }

    @Test
    void webhook_kapsamDizisi_endpoint() {
        sahte.hamCevap("saveWebhook", 200, "{\"data\":{\"saveWebhook\":[{\"id\":\"w1\",\"scope\":\"store/order/created\",\"endpoint\":\"https://ornek.invalid\"}]}}");
        IkasVeri v = yazar.webhookKaydet(List.of("store/order/created"), "https://ornek.invalid/hook");
        assertEquals("store/order/created", sahte.sonDegiskenler().yol("input.scopes.0").metin(), "girdide DİZİ: scopes");
        assertEquals("store/order/created", v.liste().get(0).metin("scope"), "cevapta TEKİL: scope");
    }

    @Test
    void faturaPdfAdresi_metinDoner() {
        assertNotNull(yazar.faturaPdfAdresi("s", "f"));
    }

    @Test
    void genelCagri_hazirMetoduOlmayanIslem() {
        IkasVeri v = okur.istemci().calistir("listGlobalTaxSettings", Map.of());
        assertFalse(v.bosMu());
        IkasVeri secimli = okur.istemci().calistirSecimle("listOrder", Map.of("pagination", Map.of("page", 1, "limit", 1)), "{ count data { id } }");
        assertTrue(sahte.sonBelge().endsWith("{ count data { id } } }"));
        assertNotNull(secimli);
    }
}
