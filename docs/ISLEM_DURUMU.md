# İkas Admin API işlemleri — gerçek davranış

Her işlem 2026-10-28 tarihinde gerçek İkas mağazasına karşı denendi (İkas'ın herkese açık test ortamı yok). Okumalar
gerçek veriyle; yazmalar yalnız **müşteriye görünmeyen** tanım kayıtlarıyla (oluştur → sil → doğrula) ya da **var olmayan
kimlikle** (İkas "bulunamadı" der, hiçbir şey değişmez). Bu belge yalnız davranışı anlatır.

Özet: **135 işlem** (65 sorgu + 70 mutasyon).
Gerçek istekle denenen: **111** (✅ 81 · 🧪 18 · ⚠️ 12).
İkas'ın yetki vermediği: 🔒 3. Bilerek denenmeyen: ⛔ 21 (canlı mağazada görünür değişiklik).
⛔ işlemlerde kütüphane yalnız argüman adlarını ve zorunlu argümanları şemadan kontrol eder; iç alanları İkas doğrular.
İlk kullanımı kendi mağazanızda dikkatle yapın.

| Durum | Anlam |
|---|---|
| ✅ | Gerçek mağazada denendi, doğru cevap geldi (yazmalarda: oluştur → sil → yok olduğu doğrulandı) |
| 🧪 | Var olmayan kimlikle denendi: girdi biçimi kabul edildi, İkas "bulunamadı" dedi, hiçbir şey değişmedi |
| ⚠️ | Çalışıyor ama beklenmedik davranışı var (nota bakın) |
| 🔒 | İkas bu uygulama türüne yetki vermiyor |
| ⛔ | Bilerek denenmedi: canlı mağazada müşteriye görünür değişiklik yapar ya da bağlı sistemleri tetikler |

## Hız sınırı

Uygulama (client id) başına **10 saniyede 50 istek** (`x-ratelimit-limit`, `x-ratelimit-remaining`, `x-ratelimit-reset`
başlıkları). Aşılınca HTTP 429. Aynı client id'yi kullanan her sistem bu kotayı paylaşır. `IkasIstemci` kalan hak
5'e inince pencere yenilenene kadar bekler, 429'da pencere sonunu bekleyip 3 kez yeniden dener.

## Sorgular — 65

| İşlem | Durum | Not |
|---|---|---|
| getAuthorizedApp | ✅ |  |
| getAvailableSubscriptions | ✅ |  |
| getGlobalTaxSettings | ✅ |  |
| getIkasWalletWithBalance | 🔒 | `UNAUTHORIZED` |
| getImageUploadUrl | ✅ | Görsel kimliği UUID olmalı; yalnız imzalı yükleme adresi üretir, yükleme yapmaz |
| getImportJobData | ✅ | Çalışıyor; içe aktarma işi yoksa `IMPORT_JOB_NOT_FOUND` |
| getImportJobDataList | ✅ | Çalışıyor; iş yoksa `NO_DATA` |
| getLastImportJobData | ✅ | Çalışıyor; iş yoksa `NO_DATA` |
| getMerchant | ✅ |  |
| getMerchantLicence | ✅ |  |
| getSalesChannel | ✅ |  |
| getTimelineEntry | ✅ |  |
| getVideoUploadUrl | ✅ | Video kimliği UUID olmalı; yalnız adres üretir |
| listAbandonedCheckouts | ✅ |  |
| listBranch | ✅ |  |
| listCampaign | ✅ |  |
| listCampaignOffer | ✅ |  |
| listCargoCompany | ✅ |  |
| listCategory | ✅ |  |
| listCity | ✅ |  |
| listCountry | ✅ |  |
| listCoupon | ✅ |  |
| listCurrency | ✅ |  |
| listCustomer | ✅ |  |
| listCustomerAttribute | ✅ |  |
| listCustomerGroup | ✅ |  |
| listCustomerTag | ✅ |  |
| listDistrict | ✅ |  |
| listGlobalTaxSettings | ✅ |  |
| listIkasWallet | 🔒 | `UNAUTHORIZED` — özel uygulamaya cüzdan yetkisi verilmiyor |
| listIkasWalletTransaction | 🔒 | `UNAUTHORIZED` |
| listLanguage | ✅ |  |
| listMerchantAppPayment | ✅ |  |
| listMerchantSettings | ✅ |  |
| listOrder | ✅ |  |
| listOrderSession | ⚠️ | `OrderSession.journeyId` şemada boş olamaz denip boş geliyor; seçilirse bütün sorgu düşer → kütüphane seçmez |
| listOrderTag | ✅ |  |
| listOrderTransactions | ✅ |  |
| listPaymentGateway | ✅ |  |
| listPriceList | ✅ |  |
| listProduct | ✅ |  |
| listProductAttribute | ✅ |  |
| listProductBrand | ✅ |  |
| listProductOptionSet | ✅ |  |
| listProductOrder | ✅ |  |
| listProductStockLocation | ✅ |  |
| listProductTag | ✅ |  |
| listProductUnit | ✅ |  |
| listProductVolumeDiscount | ✅ |  |
| listSalesChannel | ✅ |  |
| listShippingSettings | ✅ |  |
| listState | ✅ |  |
| listStockLocation | ✅ |  |
| listStoreCredit | ✅ |  |
| listStorefront | ✅ |  |
| listStorefrontJSScript | ✅ |  |
| listStorefrontPolicy | ✅ |  |
| listTaxSettings | ✅ |  |
| listTerminal | ✅ |  |
| listTown | ✅ |  |
| listVariantType | ✅ |  |
| listVendor | ✅ |  |
| listWebhook | ✅ |  |
| me | ✅ |  |
| searchProducts | ⚠️ | Kayıtlar `data`'da değil `results`'ta (`IkasVeri.kayitlar()` ikisini de okur) |

## Mutasyonlar — 70

| İşlem | Durum | Not |
|---|---|---|
| addCustomTimelineEntry | ⚠️ | DİKKAT: var olmayan kaynakta da `true` döner |
| addOrderInvoice | 🧪 |  |
| addOrderTag | 🧪 |  |
| approvePendingOrderTransactions | ⚠️ | Var olmayan siparişte hata değil `false` döner |
| bulkUpdateProducts | ⛔ | Denenmedi: toplu ürün değişikliği |
| campaignAddCoupons | ✅ | Kupon başına `canCombineWithOtherCampaigns` ister |
| cancelFulfillment | 🧪 |  |
| cancelOrderLine | 🧪 |  |
| changeStockLocation | 🧪 |  |
| createMerchantAppPayment | ⛔ | Denenmedi: uygulama ödemesi |
| createMerchantAppPaymentWithSubscription | ⛔ | Denenmedi: uygulama ödemesi |
| createOrderWithTransactions | ⛔ | Denenmedi: gerçek sipariş oluşturur |
| createWalletTransaction | ⛔ | Denenmedi: para hareketi |
| deleteCampaignList | ✅ |  |
| deleteCategoryList | 🧪 |  |
| deleteCouponList | ✅ |  |
| deleteCustomerGroupList | ✅ |  |
| deleteCustomerList | 🧪 |  |
| deleteCustomerTagList | ✅ |  |
| deleteGlobalTaxSettingsList | ⚠️ | `DELETE_FUNCTION_NOT_ALLOWED` |
| deleteOrderTagList | ✅ |  |
| deleteProductAttributeList | ✅ |  |
| deleteProductBrandList | ✅ |  |
| deleteProductList | ⚠️ | Var olmayan üründe hata değil `false` döner |
| deleteProductOrderList | 🧪 |  |
| deleteProductTagList | ✅ |  |
| deleteProductUnitList | ✅ |  |
| deleteProductVolumeDiscountList | 🧪 |  |
| deleteStorefrontJSScript | ⛔ | Denenmedi: TÜM site scriptlerini siler |
| deleteStorefrontPolicyList | 🧪 |  |
| deleteTaxSettingsList | ⚠️ | `DELETE_FUNCTION_NOT_ALLOWED` — API'den vergi ayarı silinemiyor |
| deleteVariantTypeList | ✅ |  |
| deleteVendorList | ✅ |  |
| deleteWebhook | ⛔ | Denenmedi: bağlı sistemlerin olay bildirimlerini keser |
| fulfillOrder | 🧪 |  |
| generateOrderPaymentLink | 🧪 |  |
| getAppDemoDay | ⛔ | Denenmedi: uygulama aboneliği (geliştirici) |
| getOrderInvoicePdfUrl | ✅ | Gerçek siparişte denendi: yalnız imzalı PDF adresi üretir, siparişi değiştirmez |
| refundOrderLine | 🧪 |  |
| removeOrderTag | 🧪 |  |
| saveCampaign | ⚠️ | `canCombineWithOtherCampaigns` zorunlu. Deneme: 2099'da başlayan, kuponlu, satış kanalsız kampanya → oluştur/sil |
| saveCategory | ⛔ | Denenmedi: kategori menüde görünebilir |
| saveCustomer | ⛔ | Denenmedi: gerçek müşteri kaydı açar (webhook tetikler) |
| saveCustomerGroup | ✅ |  |
| saveCustomerTag | ✅ |  |
| saveGlobalTaxSettings | ⛔ | Denenmedi: vergi ayarı |
| saveOrderTag | ✅ |  |
| saveProduct | ⚠️ | Canlıda tek üründe denendi (görünmez açıklama notu ekle → tüm alanları karşılaştır → geri al): yalnız açıklama değişti, ANCAK varyantların `createdAt`'i şimdiye çekildi (gönderilen değer yok sayılır). Tam yazımdır: ürünü `ProductInput` aynasıyla eksiksiz okuyup gönderin (`IkasAdmin.urunuGuncelle`) |
| saveProductAttribute | ⚠️ | `options` en az bir seçenek ister |
| saveProductBrand | ✅ | Satış kanalsız oluşturuldu → sitede görünmez |
| saveProductOrder | ⛔ | Denenmedi: sitedeki ürün sıralaması |
| saveProductStockLocations | ⛔ | Denenmedi: stok |
| saveProductTag | ✅ |  |
| saveProductUnit | ✅ |  |
| saveProductVolumeDiscount | ⛔ | Denenmedi: ürün fiyatına indirim |
| saveSalesChannel | ⛔ | Denenmedi: satış kanalı |
| saveStorefrontJSScript | ⛔ | Denenmedi: canlı sitede çalışan script |
| saveStorefrontPolicy | ⛔ | Denenmedi: sitede politika sayfası |
| saveTaxSettings | ⛔ | Denenmedi: vergi ayarı |
| saveVariantPrices | ⛔ | Denenmedi: müşterinin gördüğü fiyat |
| saveVariantType | ✅ |  |
| saveVendor | ✅ | `status: ARCHIVED` ile oluşturuldu |
| saveWebhook | ⛔ | Denenmedi: mağazanın canlı bildirim (webhook) bağlantısını değiştirir; bağlı sistemler olay almayı bırakabilir; Girdi `scopes: [String!]!` (dizi), cevap `scope: String` (tekil); dizi döner |
| updateCustomerB2BStatus | ⚠️ | DİKKAT: var olmayan müşteride de `true` döner |
| updateOrderAddresses | 🧪 |  |
| updateOrderLine | 🧪 | Yeni satıra uuid `id` verilmeli, yoksa İkas reddeder |
| updateOrderPackageStatus | 🧪 |  |
| updateProductSalesChannelStatus | ⛔ | Denenmedi: ürünü sitede açar/kapatır |
| updateStoreCreditAmount | 🧪 |  |
| updateSubscriptionStatus | ⚠️ | DİKKAT: var olmayan müşteride de `true` döner |

## Hata biçimi

- Doğrulama hatası: `extensions.code = ARGUMENT_VALIDATION_ERROR`, `extensions.validationErrors[].property/constraints`.
  `constraints` içinde **"Referenced id not found"** = kayıt yok (`IkasHatasi.bulunamadiMi()`).
- Kayıt yok: `404_OBJECT_NOT_FOUND`, `ORDER_NOT_FOUND`, `NOT_FOUND`, `IMPORT_JOB_NOT_FOUND`.
- Token süresi dolunca `UNAUTHENTICATED` / HTTP 401 → istemci bir kez yeni token alıp tekrarlar.
- Token isteği JSON gövde + tarayıcı dışı özel `User-Agent` ister; form gövdesi ya da varsayılan Java/Python UA'sı 403 alabilir.
