# İkas Admin API Java İstemcisi

İkas Admin GraphQL API'si için bağımsız **Java 21** istemcisi. Çalışma zamanı bağımlılığı yok (`java.net.http` + kendi
JSON çözücüsü). İkas'ın **135 işleminin hepsi** (65 sorgu + 70 mutasyon) çağrılabilir; sık kullanılan ~100'ü için Türkçe
hazır metot vardır.

İşlemler 2026-10-28'de **gerçek İkas mağazasına karşı denendi** (İkas'ın herkese açık test ortamı yok): 110'u gerçek
istekle, 3'üne İkas yetki vermiyor, 22'si bilerek denenmedi (canlı mağazada görünür değişiklik yapanlar). Ayrıntı:
[`docs/ISLEM_DURUMU.md`](docs/ISLEM_DURUMU.md). Tüm işlemler, argümanlar, zorunlu girdi alanları:
[`docs/ISLEM_ENVANTERI.md`](docs/ISLEM_ENVANTERI.md).

## Hızlı başlangıç

İkas panelinde **Uygulamalar → Özel uygulama** oluşturup client id / secret alın.

```java
var ikas = new IkasAdmin(IkasAyarlari.of("magazam", clientId, clientSecret));   // salt okuma

// Son siparişler (en yeni önce)
for (IkasVeri s : ikas.siparisler(1, 50).kayitlar()) {
    System.out.println(s.metin("orderNumber") + " " + s.sayi("totalFinalPrice") + " "
            + s.yol("shippingAddress.city.name").metin());
    for (IkasVeri satir : s.liste("orderLineItems")) System.out.println("  " + satir.yol("variant.sku").metin());
}

IkasVeri siparis = ikas.siparisNoIle("1042");
IkasVeri urun = ikas.skuIleUrun("ABC-500");
IkasVeri bulunan = ikas.urunAra("ekmeklik", 1, 20);           // kayıtlar results'ta — kayitlar() ikisini de okur
IkasVeri iller = ikas.iller(bolgeId);                          // ülke → bölge → il → ilçe → mahalle
```

Yazma işlemleri bilerek açılır:

```java
var yazar = new IkasAdmin(IkasAyarlari.of("magazam", clientId, clientSecret).yazmayaIzinVer());
yazar.siparisEtiketiEkle(siparisId, etiketId);
yazar.faturaEkle(siparisId, uygulamaId, "SD2026000001", pdfBase64, false, false);   // bireysel, müşteriye bildirmeden
```

Hazır metodu olmayan işlem — belgeyi şemadan kütüphane kurar:

```java
IkasVeri v = ikas.istemci().calistir("listProductOptionSet", Map.of());          // derinlik 2
IkasVeri az = ikas.istemci().calistirSecimle("listOrder",
        Map.of("pagination", Map.of("page", 1, "limit", 10)), "{ data { id orderNumber status } }");
```

## Metotlar

| Bölüm | Okuma | Yazma |
|---|---|---|
| Mağaza | `magaza` `lisans` `ben` `satisKanali(lari)` `vitrinler` `vitrinScriptleri` `politikalar` `diller` `paraBirimleri` `magazaAyarlari` `kargoAyarlari` `kargoFirmalari` `odemeYontemleri` `depolar` `fiyatListeleri` `vergiAyarlari` `genelVergiAyarlari` `subeler` `terminaller` `webhooklar` | |
| Konum | `ulkeler` `bolgeler` `iller` `ilceler` `mahalleler` | |
| Sipariş | `siparisler` (filtreli/sayfalı) `siparis` `siparisNoIle` `siparisOdemeleri` `siparisOturumlari` `siparisEtiketleri` `terkEdilenSepetler` · `faturaPdfAdresi`¹ | `faturaEkle` `siparisEtiketiEkle/Kaldir` `satirIptal` `iade` `paketIptal` `kargoyaVer` `paketDurumu` `satirGuncelle` `adresGuncelle` `depoDegistir` `odemeLinki` `bekleyenOdemeleriOnayla` `zamanCizelgesineNot` `siparisOlustur` |
| Ürün | `urunler` `urun` `skuIleUrun` `urunAra` `skularlaAra` `stoklar` `kategoriler` `markalar` `urunEtiketleri` `birimler` `ozellikler` `varyantTipleri` `secenekSetleri` `tedarikciler` `adetIndirimleri` `urunSiralamalari` `gorselYuklemeAdresi` `videoYuklemeAdresi` | `fiyatKaydet` `stokKaydet` `urunKaydet` `urunleriTopluGuncelle` `urunSatisDurumu` `urunSil` |
| Tanımlar | | `urunEtiketiKaydet` `birimKaydet` `markaKaydet` `ozellikKaydet` `varyantTipiKaydet` `tedarikciKaydet` `siparisEtiketiKaydet` `musteriEtiketiKaydet` `musteriGrubuKaydet` · genel: `tanimKaydet` / `tanimSil` |
| Müşteri | `musteriler` `musteri` `epostaIleMusteri` `musteriAra` `musteriGruplari` `musteriEtiketleri` `musteriOzellikleri` `magazaPuani` | `musteriKaydet` `musteriSil` `b2bDurumu` `abonelikDurumu` `magazaPuaniAyarla` |
| Kampanya | `kampanyalar` `kuponlar` `kampanyaTeklifleri` | `kampanyaKaydet` `kuponEkle` `kuponSil` `kampanyaSil` |
| Vitrin / webhook | | `scriptKaydet` `scriptleriSil` `webhookKaydet` `webhookSil` |

¹ Kayıt değiştirmez ama İkas'ta mutasyon olarak tanımlı → yazma izni ister.

Dönüş her zaman **`IkasVeri`**: alan adıyla gezilir, eksik alanda istisna atmaz (`metin`, `sayi` → `BigDecimal`,
`tamsayi`, `mantiksal`, `zaman` → `Instant`, `liste`, `yol("a.0.b")`, sayfalı cevaplarda `kayitlar()`).

## Bilinmesi gereken tuzaklar (kütüphane çözer)

- **Hız sınırı: 10 saniyede 50 istek, client id başına** — aynı uygulamayı kullanan her sistem paylaşır. İstemci kalan
  hak 5'e inince bekler, 429'da pencere sonunu bekleyip 3 kez yeniden dener.
- Token isteği JSON gövde ve özel `User-Agent` ister; form gövdesi / varsayılan Java-Python UA'sı 403 alabilir.
- `OrderSession.journeyId` şemada boş olamaz denip boş gelir; seçilirse bütün sorgu düşer → kütüphane seçmez.
- `searchProducts` kayıtları `data`'da değil `results`'ta.
- "Kayıt yok" üç farklı biçimde gelir (`404_OBJECT_NOT_FOUND`, `ORDER_NOT_FOUND`, doğrulama hatasında
  "Referenced id not found") → hepsi `IkasHatasi.bulunamadiMi()`.
- Bazı yazmalar var olmayan kimlikte de **`true`** döner (`b2bDurumu`, `abonelikDurumu`, `zamanCizelgesineNot`);
  bazıları **`false`** (`urunSil`, `bekleyenOdemeleriOnayla`). Sonucu "yapıldı" diye okumayın.
- Webhook girdisi `scopes` (dizi), cevabı `scope` (tekil).
- İkas sipariş zamanları milisaniye zaman damgasıdır (UTC); yerel saate çevirirken `Europe/Istanbul` kullanın.

## Güvenlik

- İkas'ta test ortamı yok: her istek **canlı mağazaya** gider. Mutasyonlar `IkasAyarlari.yazmayaIzinVer()` olmadan
  **istek gönderilmeden** reddedilir.
- Sipariş/ürün/müşteri üzerindeki yazmalar İkas webhook'larını tetikler — bağlı sistemleriniz (ERP, muhasebe) tepki verir.
- Client secret `toString` / hata mesajlarına düşmez.
- Test örnekleri (`src/test/resources/ornek/`) gerçek cevaplardan **beyaz listeyle anonimleştirildi** (`arac/kesif.py`):
  yalnız enum, tarih, para birimi, hata metni ve herkese açık coğrafya/kargo listeleri korunur; ad, adres, telefon,
  e-posta, kimlik, tutar, adet sahte değerle değiştirilir.

## Test

```bash
./mvnw test                         # çevrimdışı: sahte sunucu + anonim gerçek cevaplar
IKAS_CLIENT_ID=… IKAS_CLIENT_SECRET=… IKAS_MAGAZA=… ./mvnw test -Pcanli-deneme
                                    # canlı: okumalar + görünmez tanım kayıtları (oluştur → sil) + sahte kimlikli yazmalar
```

Canlı test ad öneki `zz-kesif-deneme-silinecek` olan kayıtları açar ve siler; yarıda kalırsa sonunda süpürür.

Keşif araçları (Python 3, yalnız standart kütüphane): `arac/kesif.py` (65 sorgu + örnek toplama), `arac/mutasyon.py`
(güvenli mutasyon denemeleri), `arac/belge_uret.py` (`docs/` belgelerini üretir).

## Lisans ve sorumluluk

[MIT](LICENSE). ikas ile resmi bir bağı yoktur; API ve adı ikas'a aittir. API davranışı ikas tarafından değiştirilebilir —
`docs/ISLEM_DURUMU.md` 2026-10-28 itibarıyla gözlenendir; güncel şema `IkasIstemci.semaIndir()` ile alınır.
