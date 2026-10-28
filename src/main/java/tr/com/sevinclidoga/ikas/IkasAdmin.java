package tr.com.sevinclidoga.ikas;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * İkas Admin API — Türkçe hazır metotlar. Her metot tek bir İkas işlemidir (parantez içinde); dönüş {@link IkasVeri}.
 * Hazır metodu olmayan her işlem: {@code admin.istemci().calistir("islemAdi", argumanlar)}.
 * <pre>{@code
 * var ikas = new IkasAdmin(IkasAyarlari.of("magazam", clientId, clientSecret));
 * for (IkasVeri s : ikas.siparisler(1, 50).kayitlar()) System.out.println(s.metin("orderNumber"));
 * IkasVeri sonuc = ikas.urunAra("ekmeklik", 1, 20);
 * }</pre>
 * Yazma metotları (javadoc'ta YAZMA) {@link IkasAyarlari#yazmayaIzinVer()} gerektirir; değişiklik CANLI mağazaya gider
 * ve sipariş/ürün/müşteri üzerindeki değişiklikler İkas webhook'larını tetikler.
 */
public class IkasAdmin {

    private final IkasIstemci istemci;
    private volatile String siteKanali;

    public IkasAdmin(IkasAyarlari ayarlar) {
        this(new IkasIstemci(ayarlar));
    }

    public IkasAdmin(IkasIstemci istemci) {
        this.istemci = Objects.requireNonNull(istemci, "istemci");
    }

    public IkasIstemci istemci() {
        return istemci;
    }

    // ═══════════════════════ MAĞAZA ═══════════════════════

    /** Mağaza ve personel bilgisi (getMerchant). */
    public IkasVeri magaza() { return c("getMerchant", m()); }

    /** Mağaza lisansları (getMerchantLicence). */
    public IkasVeri lisans() { return c("getMerchantLicence", m()); }

    /** Bağlı uygulama/kullanıcı (me). */
    public IkasVeri ben() { return c("me", m()); }

    /** Varsayılan satış kanalı (getSalesChannel). */
    public IkasVeri satisKanali() { return c("getSalesChannel", m()); }

    public IkasVeri satisKanallari() { return c("listSalesChannel", m()); }

    public IkasVeri vitrinler() { return c("listStorefront", m()); }

    /** Vitrine gömülü JavaScript'ler (listStorefrontJSScript). */
    public IkasVeri vitrinScriptleri(String vitrinId) { return c("listStorefrontJSScript", m("storefrontId", vitrinId)); }

    /** Vitrin politikaları: KVKK, iade, mesafeli satış … (listStorefrontPolicy). */
    public IkasVeri politikalar() { return c("listStorefrontPolicy", m()); }

    public IkasVeri diller() { return c("listLanguage", m()); }

    public IkasVeri paraBirimleri() { return c("listCurrency", m()); }

    public IkasVeri magazaAyarlari() { return c("listMerchantSettings", m()); }

    public IkasVeri kargoAyarlari() { return c("listShippingSettings", m()); }

    public IkasVeri kargoFirmalari() { return c("listCargoCompany", m()); }

    public IkasVeri odemeYontemleri() { return c("listPaymentGateway", m()); }

    /** Stok lokasyonları / depolar (listStockLocation). */
    public IkasVeri depolar() { return c("listStockLocation", m()); }

    public IkasVeri fiyatListeleri() { return c("listPriceList", m()); }

    public IkasVeri vergiAyarlari() { return c("listTaxSettings", m()); }

    public IkasVeri genelVergiAyarlari() { return c("getGlobalTaxSettings", m()); }

    /** Mağaza içi satış şubeleri (listBranch) ve POS terminalleri (listTerminal). */
    public IkasVeri subeler() { return c("listBranch", m()); }

    public IkasVeri terminaller() { return c("listTerminal", m()); }

    /** Kayıtlı webhook'lar (listWebhook). */
    public IkasVeri webhooklar() { return c("listWebhook", m()); }

    // ═══════════════════════ KONUM ═══════════════════════

    public IkasVeri ulkeler() { return c("listCountry", m()); }

    /** Ülkenin bölgeleri/eyaletleri (listState) — Türkiye'de tek kayıt döner. */
    public IkasVeri bolgeler(String ulkeId) { return c("listState", m("countryId", esit(ulkeId))); }

    /** Bölgenin illeri (listCity). */
    public IkasVeri iller(String bolgeId) { return c("listCity", m("stateId", esit(bolgeId))); }

    /** İlin ilçeleri (listDistrict). */
    public IkasVeri ilceler(String ilId) { return c("listDistrict", m("cityId", esit(ilId))); }

    /** İlçenin mahalleleri (listTown). */
    public IkasVeri mahalleler(String ilceId) { return c("listTown", m("districtId", esit(ilceId))); }

    // ═══════════════════════ SİPARİŞ ═══════════════════════

    /** Siparişler, yeniden eskiye (listOrder). Sonuç: {@code {count, data:[…], hasNext}} → {@link IkasVeri#kayitlar()}. */
    public IkasVeri siparisler(int sayfa, int adet) { return siparisler(m(), sayfa, adet); }

    /**
     * Filtreli siparişler. Filtre anahtarları İkas'ın listOrder argümanlarıdır, ör.
     * {@code Map.of("orderedAt", Map.of("gte", baslangicMs), "status", Map.of("eq", "CREATED"))}.
     */
    public IkasVeri siparisler(Map<String, ?> filtre, int sayfa, int adet) {
        Map<String, Object> a = new LinkedHashMap<>(filtre);
        a.put("pagination", sayfa(sayfa, adet));
        a.putIfAbsent("sort", "-orderedAt");
        return c("listOrder", a);
    }

    public IkasVeri siparis(String id) { return ilk(c("listOrder", m("id", esit(id), "pagination", sayfa(1, 1)))); }

    public IkasVeri siparisNoIle(String siparisNo) {
        return ilk(c("listOrder", m("orderNumber", esit(siparisNo), "pagination", sayfa(1, 1))));
    }

    /** Siparişin ödeme hareketleri (listOrderTransactions). */
    public IkasVeri siparisOdemeleri(String siparisId) { return c("listOrderTransactions", m("orderId", siparisId)); }

    /** Siparişin oturum/yolculuk kayıtları (listOrderSession). İkas'ın bozuk journeyId alanı otomatik dışarıda. */
    public IkasVeri siparisOturumlari(String siparisId) { return c("listOrderSession", m("orderId", esit(siparisId))); }

    public IkasVeri siparisEtiketleri() { return c("listOrderTag", m()); }

    /** Terk edilen sepetler (listAbandonedCheckouts). */
    public IkasVeri terkEdilenSepetler(int sayfa, int adet) {
        return c("listAbandonedCheckouts", m("input", m(), "pagination", sayfa(sayfa, adet)));
    }

    /** YAZMA — siparişe fatura ekler (addOrderInvoice). {@code pdfBase64} isteğe bağlı. */
    public IkasVeri faturaEkle(String siparisId, String uygulamaId, String faturaNo, String pdfBase64, boolean kurumsal,
                               boolean musteriyeBildir) {
        return c("addOrderInvoice", m("input", m("orderId", siparisId, "appId", uygulamaId, "invoiceNumber", faturaNo,
                "base64", pdfBase64, "type", kurumsal ? "COMPANY" : "INDIVIDUAL", "sendNotificationToCustomer", musteriyeBildir)));
    }

    /** Siparişteki faturanın PDF adresi (getOrderInvoicePdfUrl) — kayıt değiştirmez ama İkas'ta mutasyondur. */
    public String faturaPdfAdresi(String siparisId, String faturaId) {
        return c("getOrderInvoicePdfUrl", m("input", m("orderId", siparisId, "invoiceId", faturaId))).metin();
    }

    /** YAZMA — siparişe etiket ekler / çıkarır (addOrderTag / removeOrderTag). */
    public boolean siparisEtiketiEkle(String siparisId, String etiketId) {
        return dogru(c("addOrderTag", m("input", m("orderId", siparisId, "orderTagId", etiketId))));
    }

    public boolean siparisEtiketiKaldir(String siparisId, String etiketId) {
        return dogru(c("removeOrderTag", m("input", m("orderId", siparisId, "orderTagId", etiketId))));
    }

    /** YAZMA — sipariş satırlarını iptal eder (cancelOrderLine). {@code satirlar}: [{orderLineItemId, quantity}, …]. */
    public IkasVeri satirIptal(String siparisId, List<Map<String, ?>> satirlar) {
        return c("cancelOrderLine", m("input", m("orderId", siparisId, "orderLineItems", satirlar)));
    }

    /** YAZMA — iade (refundOrderLine). Girdi İkas'ın OrderRefundInput'u. */
    public IkasVeri iade(Map<String, ?> orderRefundInput) { return c("refundOrderLine", m("input", orderRefundInput)); }

    /** YAZMA — oluşturulmuş paketi geri alır (cancelFulfillment). */
    public IkasVeri paketIptal(Map<String, ?> cancelFulfillmentInput) { return c("cancelFulfillment", m("input", cancelFulfillmentInput)); }

    /** YAZMA — kargoya verir / paket açar (fulfillOrder). */
    public IkasVeri kargoyaVer(Map<String, ?> fulfillOrderInput) { return c("fulfillOrder", m("input", fulfillOrderInput)); }

    /** YAZMA — paket durumu (updateOrderPackageStatus). */
    public IkasVeri paketDurumu(Map<String, ?> input) { return c("updateOrderPackageStatus", m("input", input)); }

    /** YAZMA — sipariş satırları (updateOrderLine). Yeni satıra uuid {@code id} verilmeli (yoksa İkas reddeder). */
    public IkasVeri satirGuncelle(Map<String, ?> updateOrderInput) { return c("updateOrderLine", m("input", updateOrderInput)); }

    /** YAZMA — teslimat / fatura adresini değiştirir (updateOrderAddresses); verilmeyen adres değişmez. */
    public IkasVeri adresGuncelle(String siparisId, Map<String, ?> teslimatAdresi, Map<String, ?> faturaAdresi) {
        return c("updateOrderAddresses", m("input", m("orderId", siparisId, "shippingAddress", teslimatAdresi,
                "billingAddress", faturaAdresi)));
    }

    /** YAZMA — satırları başka depoya alır (changeStockLocation). */
    public IkasVeri depoDegistir(String siparisId, List<String> satirIdleri, String hedefDepoId) {
        return c("changeStockLocation", m("input", m("orderId", siparisId, "orderLineItemIds", satirIdleri,
                "targetStockLocationId", hedefDepoId)));
    }

    /** YAZMA — sipariş için ödeme linki (generateOrderPaymentLink). */
    public String odemeLinki(String siparisId) { return c("generateOrderPaymentLink", m("input", m("orderId", siparisId))).metin(); }

    /** YAZMA — bekleyen ödemeleri onaylar (approvePendingOrderTransactions), ör. havale: MONEY_ORDER. Onaylanacak bir şey yoksa false. */
    public boolean bekleyenOdemeleriOnayla(String siparisId, List<String> odemeYontemleri) {
        return dogru(c("approvePendingOrderTransactions", m("input", m("orderId", siparisId, "paymentMethods", odemeYontemleri))));
    }

    /**
     * YAZMA — kaydın zaman çizelgesine not (addCustomTimelineEntry). {@code kaynakTuru}: ORDER, CUSTOMER, PRODUCT …
     * DİKKAT: var olmayan kimlik için de true döner.
     */
    public boolean zamanCizelgesineNot(String kaynakTuru, String kaynakId, String mesaj) {
        return dogru(c("addCustomTimelineEntry", m("input", m("sourceType", kaynakTuru, "sourceId", kaynakId, "message", mesaj))));
    }

    /** YAZMA — ödemeleriyle birlikte yeni sipariş (createOrderWithTransactions). */
    public IkasVeri siparisOlustur(Map<String, ?> input) { return c("createOrderWithTransactions", m("input", input)); }

    // ═══════════════════════ ÜRÜN ═══════════════════════

    public IkasVeri urunler(int sayfa, int adet) { return urunler(m(), sayfa, adet); }

    /** Filtreli ürünler (listProduct); ör. {@code Map.of("sku", Map.of("eq", "UN-1KG"))}. */
    public IkasVeri urunler(Map<String, ?> filtre, int sayfa, int adet) {
        Map<String, Object> a = new LinkedHashMap<>(filtre);
        a.put("pagination", sayfa(sayfa, adet));
        return c("listProduct", a);
    }

    public IkasVeri urun(String id) { return ilk(c("listProduct", m("id", esit(id), "pagination", sayfa(1, 1)))); }

    public IkasVeri skuIleUrun(String sku) { return ilk(c("listProduct", m("sku", esit(sku), "pagination", sayfa(1, 1)))); }

    /** Metinle ürün arama (searchProducts); sonuçlar {@code results} içinde → {@link IkasVeri#kayitlar()}. */
    public IkasVeri urunAra(String metin, int sayfa, int adet) {
        return c("searchProducts", m("input", m("query", metin, "pagination", sayfa(sayfa, adet), "barcodeList", List.of(),
                "productIdList", List.of(), "skuList", List.of())));
    }

    /** SKU listesiyle arama (searchProducts). */
    public IkasVeri skularlaAra(List<String> skular) {
        return c("searchProducts", m("input", m("skuList", skular, "barcodeList", List.of(), "productIdList", List.of())));
    }

    /** Depo bazında stoklar (listProductStockLocation); ürün kimliği boşsa hepsi. */
    public IkasVeri stoklar(String urunId, int sayfa, int adet) {
        return c("listProductStockLocation", m("productId", urunId == null ? null : esit(urunId), "pagination", sayfa(sayfa, adet)));
    }

    public IkasVeri kategoriler() { return c("listCategory", m()); }

    public IkasVeri markalar() { return c("listProductBrand", m()); }

    public IkasVeri urunEtiketleri() { return c("listProductTag", m()); }

    public IkasVeri birimler() { return c("listProductUnit", m()); }

    public IkasVeri ozellikler() { return c("listProductAttribute", m()); }

    public IkasVeri varyantTipleri() { return c("listVariantType", m()); }

    public IkasVeri secenekSetleri() { return c("listProductOptionSet", m()); }

    public IkasVeri tedarikciler() { return c("listVendor", m()); }

    public IkasVeri adetIndirimleri() { return c("listProductVolumeDiscount", m()); }

    /** Kategori/marka içi ürün sıralamaları (listProductOrder). */
    public IkasVeri urunSiralamalari() { return c("listProductOrder", m()); }

    /** Görsel yükleme için imzalı adres (getImageUploadUrl); {@code gorselId} UUID olmalı. Yükleme yapmaz. */
    public String gorselYuklemeAdresi(String gorselId) { return c("getImageUploadUrl", m("imageId", gorselId)).metin(); }

    /** Video yükleme için imzalı adres (getVideoUploadUrl); {@code videoId} UUID. */
    public IkasVeri videoYuklemeAdresi(String videoId) { return c("getVideoUploadUrl", m("videoId", videoId)); }

    /** YAZMA — varyant fiyatları (saveVariantPrices). */
    public boolean fiyatKaydet(Map<String, ?> saveVariantPricesInput) { return dogru(c("saveVariantPrices", m("input", saveVariantPricesInput))); }

    /** YAZMA — depo bazında stok (saveProductStockLocations). */
    public boolean stokKaydet(Map<String, ?> saveStockLocationsInput) {
        return dogru(c("saveProductStockLocations", m("input", saveStockLocationsInput)));
    }

    /** YAZMA — ürün oluşturur / günceller (saveProduct). */
    public IkasVeri urunKaydet(Map<String, ?> productInput) { return c("saveProduct", m("input", productInput)); }

    /** YAZMA — toplu ürün güncelleme (bulkUpdateProducts); İkas iş kimliği döner. */
    public String urunleriTopluGuncelle(List<Map<String, ?>> girdiler) { return c("bulkUpdateProducts", m("input", girdiler)).metin(); }

    /** YAZMA — ürünleri satış kanalında açar/kapatır (updateProductSalesChannelStatus). */
    public boolean urunSatisDurumu(List<Map<String, ?>> girdiler, String satisKanaliId) {
        return dogru(c("updateProductSalesChannelStatus", m("input", girdiler, "salesChannelId", satisKanaliId)));
    }

    /**
     * Mağazanın SİTE (STOREFRONT) kanalı. İkas'ta site, pazaryeri uygulamaları (ör. Trendyol) ve B2B ayrı satış kanalıdır;
     * ürün açma/kapatma kanal başınadır.
     */
    public String siteKanaliId() {
        String k = siteKanali;
        if (k != null) return k;
        List<String> site = new ArrayList<>();
        for (IkasVeri c : satisKanallari().liste()) if ("STOREFRONT".equals(c.metin("type"))) site.add(c.metin("id"));
        if (site.size() != 1) throw new IkasHatasi("Tek site (STOREFRONT) kanalı bekleniyordu, bulunan: " + site.size(), false, null);
        siteKanali = site.get(0);
        return siteKanali;
    }

    /** YAZMA — ürünleri YALNIZ sitede açar/kapatır; diğer kanallar (pazaryeri, B2B) etkilenmez. */
    public boolean sitedeAcKapat(List<String> urunIdleri, boolean acik) {
        List<Map<String, ?>> g = new ArrayList<>();
        for (String id : urunIdleri) g.add(m("productId", id, "active", acik));
        return urunSatisDurumu(g, siteKanaliId());
    }

    /**
     * YAZMA — ürünün bir kısmını güvenle değiştirir. İkas'ta tek alan güncelleyen işlem yok; {@code saveProduct} ürünün
     * TAMAMINI yazar. Bu metot ürünü {@code ProductInput}'un aynası olan seçimle okur ({@link IkasSema#girdiAynasi}),
     * {@code degisiklik}'i uygular, aynen geri yazar ve varyant fiyatlarını önce/sonra karşılaştırır: fiyat değişmişse
     * {@link IkasHatasi}. Örnek: {@code urunuGuncelle(id, u -> u.put("description", "<p>yeni</p>"))}.
     * <p>
     * DİKKAT (canlıda görüldü 2026-10-28): İkas her {@code saveProduct}'ta varyantların {@code createdAt}'ini şimdiye çeker
     * (gönderilen değeri yok sayar). Varyant kimliği, fiyat, görsel ve kanal ayarı korunur.
     *
     * @param degisiklik okunan ürün (değiştirilebilir Map; iç nesneler Map/List)
     * @return saveProduct cevabı
     */
    @SuppressWarnings("unchecked")
    public IkasVeri urunuGuncelle(String urunId, Consumer<Map<String, Object>> degisiklik) {
        String secim = istemci.sema().girdiAynasi("ProductInput", "Product");
        List<IkasVeri> l = istemci.calistirSecimle("listProduct", m("id", esit(urunId)), "{ data " + secim + " }").kayitlar();
        if (l.isEmpty()) throw new IkasHatasi("Ürün bulunamadı: " + urunId, false, null);
        Map<String, Object> urun = (Map<String, Object>) Json.coz(Json.yaz(l.get(0).ham()));   // değiştirilebilir kopya
        String once = fiyatImzasi(IkasVeri.of(urun));
        degisiklik.accept(urun);
        IkasVeri sonra = istemci.calistirSecimle("saveProduct", m("input", urun),
                "{ id variants { id prices { currency discountPrice priceListId sellPrice } } }");
        if (!once.equals(fiyatImzasi(sonra))) {
            throw new IkasHatasi("Ürün yazıldı ama varyant fiyatları değişmiş görünüyor — panelden kontrol edin: " + urunId, false, null);
        }
        return sonra;
    }

    private static String fiyatImzasi(IkasVeri urun) {
        List<String> l = new ArrayList<>();
        for (IkasVeri v : urun.liste("variants")) {
            for (IkasVeri p : v.liste("prices")) {
                l.add(v.metin("id") + "|" + p.metin("priceListId") + "|" + p.metin("currency") + "|"
                        + sade(p.sayi("sellPrice")) + "|" + sade(p.sayi("discountPrice")));
            }
        }
        l.sort(null);
        return String.join(";", l);
    }

    private static String sade(BigDecimal b) {
        return b == null ? "-" : b.stripTrailingZeros().toPlainString();
    }

    /** YAZMA — ürün siler (deleteProductList). Var olmayan kimlikte false döner. */
    public boolean urunSil(List<String> idler) { return dogru(c("deleteProductList", m("idList", idler))); }

    // ═══════════════════════ TANIMLAR (oluştur / sil) ═══════════════════════

    /** YAZMA — tanım kaydı (saveProductTag, saveProductBrand, saveCategory, saveVendor, saveCustomerTag …). */
    public IkasVeri tanimKaydet(String saveIslemi, Map<String, ?> input) { return c(saveIslemi, m("input", input)); }

    /** YAZMA — tanım silme (deleteProductTagList, deleteProductBrandList …). */
    public boolean tanimSil(String deleteIslemi, List<String> idler) { return dogru(c(deleteIslemi, m("idList", idler))); }

    public IkasVeri urunEtiketiKaydet(String ad) { return tanimKaydet("saveProductTag", m("name", ad, "translations", List.of())); }

    public IkasVeri birimKaydet(String ad) { return tanimKaydet("saveProductUnit", m("name", ad, "translations", List.of())); }

    /** Satış kanalı listesi boşsa marka hiçbir vitrinde görünmez. */
    public IkasVeri markaKaydet(String ad, List<String> satisKanallari) {
        return tanimKaydet("saveProductBrand", m("name", ad, "salesChannelIds", satisKanallari, "translations", List.of()));
    }

    /** Ürün özelliği; İkas TEXT tipte bile en az bir seçenek ister. */
    public IkasVeri ozellikKaydet(String ad, String tur, List<String> secenekler) {
        return tanimKaydet("saveProductAttribute", m("name", ad, "type", tur, "translations", List.of(),
                "options", secenekler.stream().map(s -> Map.of("name", s)).toList()));
    }

    public IkasVeri varyantTipiKaydet(String ad, List<String> degerler) {
        return tanimKaydet("saveVariantType", m("name", ad, "selectionType", "CHOICE", "translations", List.of(),
                "values", degerler.stream().map(s -> Map.of("name", s)).toList()));
    }

    public IkasVeri tedarikciKaydet(Map<String, ?> vendorInput) { return tanimKaydet("saveVendor", vendorInput); }

    public IkasVeri siparisEtiketiKaydet(String ad) { return tanimKaydet("saveOrderTag", m("name", ad)); }

    // ═══════════════════════ MÜŞTERİ ═══════════════════════

    public IkasVeri musteriler(int sayfa, int adet) { return c("listCustomer", m("pagination", sayfa(sayfa, adet))); }

    public IkasVeri musteri(String id) { return ilk(c("listCustomer", m("id", esit(id), "pagination", sayfa(1, 1)))); }

    public IkasVeri epostaIleMusteri(String eposta) { return ilk(c("listCustomer", m("email", esit(eposta), "pagination", sayfa(1, 1)))); }

    public IkasVeri musteriAra(String metin, int sayfa, int adet) {
        return c("listCustomer", m("search", metin, "pagination", sayfa(sayfa, adet)));
    }

    public IkasVeri musteriGruplari() { return c("listCustomerGroup", m()); }

    public IkasVeri musteriEtiketleri() { return c("listCustomerTag", m()); }

    public IkasVeri musteriOzellikleri() { return c("listCustomerAttribute", m()); }

    /** Müşterinin mağaza puanı / hediye bakiyesi (listStoreCredit). */
    public IkasVeri magazaPuani(String musteriId) { return c("listStoreCredit", m("customerId", musteriId)); }

    /** YAZMA — müşteri (saveCustomer). Webhook ile ERP'ye müşteri olarak düşer. */
    public IkasVeri musteriKaydet(Map<String, ?> customerInput) { return c("saveCustomer", m("input", customerInput)); }

    public boolean musteriSil(List<String> idler) { return dogru(c("deleteCustomerList", m("idList", idler))); }

    /** YAZMA — B2B durumu: ACTIVE_ACCOUNT, DISABLED_ACCOUNT, WAITING_FOR_APPROVAL. DİKKAT: var olmayan müşteride de true. */
    public boolean b2bDurumu(String musteriId, String durum) {
        return dogru(c("updateCustomerB2BStatus", m("customerId", musteriId, "b2bStatus", durum)));
    }

    /** YAZMA — e-posta aboneliği: SUBSCRIBED, NOT_SUBSCRIBED, PENDING_CONFIRMATION. DİKKAT: var olmayan müşteride de true. */
    public boolean abonelikDurumu(String musteriId, String durum) {
        return dogru(c("updateSubscriptionStatus", m("customerId", musteriId, "subscriptionStatus", durum)));
    }

    /**
     * YAZMA — mağaza puanı kalan bakiyesini AYARLAR (eklemez) (updateStoreCreditAmount). Yalnız VAR OLAN puan kaydını
     * değiştirir: API'de puan kaydı açan işlem yok (kayıt kimliği {@link #magazaPuani} ile bulunur; yoksa panelden açılır).
     */
    public IkasVeri magazaPuaniAyarla(String puanKaydiId, double kalanTutar) {
        return c("updateStoreCreditAmount", m("input", m("id", puanKaydiId, "amount", kalanTutar)));
    }

    public IkasVeri musteriEtiketiKaydet(String ad) { return tanimKaydet("saveCustomerTag", m("name", ad)); }

    /** Elle yönetilen (otomatik olmayan) müşteri grubu. */
    public IkasVeri musteriGrubuKaydet(String ad) {
        return tanimKaydet("saveCustomerGroup", m("name", ad, "conditions", List.of(), "priceListRules", List.of(), "isAutomated", false));
    }

    // ═══════════════════════ KAMPANYA / KUPON ═══════════════════════

    public IkasVeri kampanyalar(int sayfa, int adet) { return c("listCampaign", m("pagination", sayfa(sayfa, adet))); }

    public IkasVeri kuponlar(String kampanyaId, int sayfa, int adet) {
        return c("listCoupon", m("campaignId", kampanyaId == null ? null : esit(kampanyaId), "pagination", sayfa(sayfa, adet)));
    }

    public IkasVeri kampanyaTeklifleri() { return c("listCampaignOffer", m()); }

    /** YAZMA — kampanya (saveCampaign). Girdi İkas'ın CampaignInput'u; {@code applyCampaignToProductPrice} sitede fiyatı değiştirir. */
    public IkasVeri kampanyaKaydet(Map<String, ?> campaignInput) { return c("saveCampaign", m("input", campaignInput)); }

    /**
     * YAZMA — kuponlu kampanya açar (saveCampaign + campaignAddCoupons). Alan anlamları mağazanın panelden açtığı
     * kampanyalardan okundu: {@code RATIO} → {@code fixedDiscount.amount} YÜZDE, {@code FIXED_AMOUNT} → TL,
     * {@code FREE_SHIPPING} → ücretsiz kargo; en az sepet tutarı = {@code fixedDiscount.priceRange.min}; fiyat tabanı
     * {@code DISCOUNT_PRICE} (panelin varsayılanı). Kupon kodu eklenemezse kampanya silinir.
     *
     * @param tip         RATIO | FIXED_AMOUNT | FREE_SHIPPING
     * @param deger       yüzde ya da tutar (FREE_SHIPPING'de null)
     * @param enAzSepet   null → sınır yok
     * @param bitis       null → süresiz
     * @param kullanim    toplam kullanım sınırı (null → sınırsız)
     * @param satisKanallari örn. {@code List.of(siteKanaliId())}; boş liste → hiçbir kanalda geçerli değil
     * @return kampanya (id)
     */
    public IkasVeri kuponluKampanyaAc(String baslik, String tip, BigDecimal deger, BigDecimal enAzSepet, Instant baslangic,
                                      Instant bitis, Integer kullanim, List<String> satisKanallari, String kuponKodu) {
        Map<String, Object> indirim = new LinkedHashMap<>();
        indirim.put("filters", List.of());
        if (deger != null) indirim.put("amount", deger);
        if (enAzSepet != null) indirim.put("priceRange", m("min", enAzSepet));
        Map<String, Object> tarih = new LinkedHashMap<>();
        tarih.put("start", (baslangic == null ? Instant.now() : baslangic).toEpochMilli());
        if (bitis != null) tarih.put("end", bitis.toEpochMilli());
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("title", baslik);
        c.put("type", tip);
        c.put("hasCoupon", true);
        c.put("applicablePrice", "DISCOUNT_PRICE");
        c.put("canCombineWithOtherCampaigns", false);
        c.put("applyCampaignToProductPrice", false);
        c.put("currencyCodes", List.of("TRY"));
        c.put("salesChannelIds", satisKanallari);
        for (String bos : List.of("applicableCustomerGroupIds", "applicableCustomerIds", "applicableCustomerSegmentIds",
                "periodUsageLimits", "translations")) c.put(bos, List.of());
        c.put("dateRange", tarih);
        if (kullanim != null) c.put("usageLimit", kullanim);
        if ("FREE_SHIPPING".equals(tip)) c.put("isFreeShipping", true);
        c.put("fixedDiscount", indirim);
        IkasVeri k = kampanyaKaydet(c);
        try {
            Map<String, Object> kupon = new LinkedHashMap<>();
            kupon.put("code", kuponKodu);
            kupon.put("canCombineWithOtherCampaigns", false);
            if (kullanim != null) kupon.put("usageLimit", kullanim);
            if (kuponEkle(k.metin("id"), List.of(kupon)).liste().isEmpty()) {
                throw new IkasHatasi("Kupon kodu eklenemedi: " + kuponKodu, false, null);
            }
        } catch (RuntimeException e) {
            kampanyaSil(List.of(k.metin("id")));
            throw e;
        }
        return k;
    }

    /** YAZMA — kampanyaya kupon (campaignAddCoupons). Kupon: {code, usageLimit, canCombineWithOtherCampaigns (zorunlu)}. */
    public IkasVeri kuponEkle(String kampanyaId, List<Map<String, ?>> kuponlar) {
        return c("campaignAddCoupons", m("input", m("campaignId", kampanyaId, "coupons", kuponlar)));
    }

    public boolean kuponSil(List<String> idler) { return c("deleteCouponList", m("idList", idler)).metin() != null; }

    public boolean kampanyaSil(List<String> idler) { return dogru(c("deleteCampaignList", m("idList", idler))); }

    // ═══════════════════════ VİTRİN / WEBHOOK ═══════════════════════

    /** YAZMA — vitrine script (saveStorefrontJSScript). İkas SPA'sı harici {@code <script src>} çalıştırmaz → kod inline olmalı. */
    public IkasVeri scriptKaydet(Map<String, ?> storefrontJSScriptInput) { return c("saveStorefrontJSScript", m("input", storefrontJSScriptInput)); }

    /** YAZMA — verilen vitrinlerin TÜM scriptlerini siler (deleteStorefrontJSScript). */
    public boolean scriptleriSil(List<String> vitrinIdleri) { return dogru(c("deleteStorefrontJSScript", m("storefrontIdList", vitrinIdleri))); }

    /**
     * YAZMA — webhook (saveWebhook). Girdi {@code scopes} DİZİ (["store/order/created"]), cevap her kapsam için ayrı kayıt;
     * kayıtlarda alan TEKİL {@code scope}.
     */
    public IkasVeri webhookKaydet(List<String> kapsamlar, String adres) {
        return c("saveWebhook", m("input", m("scopes", kapsamlar, "endpoint", adres)));
    }

    /** YAZMA — kapsama göre webhook siler (deleteWebhook). */
    public boolean webhookSil(List<String> kapsamlar) { return dogru(c("deleteWebhook", m("scopes", kapsamlar))); }

    // ── yardımcılar ──

    private IkasVeri c(String islem, Map<String, ?> a) {
        return istemci.calistir(islem, a);
    }

    private static IkasVeri ilk(IkasVeri sayfali) {
        List<IkasVeri> k = sayfali.kayitlar();
        return k.isEmpty() ? IkasVeri.of(null) : k.get(0);
    }

    private static boolean dogru(IkasVeri v) {
        return Boolean.TRUE.equals(v.ham());
    }

    static Map<String, Object> sayfa(int sayfa, int adet) {
        if (sayfa < 1 || adet < 1) throw new IllegalArgumentException("sayfa ve adet 1'den büyük olmalı");
        return m("page", sayfa, "limit", adet);
    }

    static Map<String, Object> esit(String v) {
        return m("eq", Objects.requireNonNull(v, "kimlik"));
    }

    /** null değerleri atlayan sıralı harita. */
    static Map<String, Object> m(Object... kv) {
        Map<String, Object> h = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) if (kv[i + 1] != null) h.put((String) kv[i], kv[i + 1]);
        return h;
    }
}
