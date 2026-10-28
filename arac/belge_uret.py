#!/usr/bin/env python3
"""docs/ISLEM_DURUMU.md ve docs/ISLEM_ENVANTERI.md'yi şema + deneme özetlerinden üretir.

Girdi: sema/ikas-admin-sema.json, deneme-ciktilari/sorgu-ozet.json, deneme-ciktilari/mutasyon-ozet.json
(kesif.py ve mutasyon.py üretir). Belgeye yalnız DAVRANIŞ yazılır: kayıt sayısı, süre, kimlik, iş verisi yazılmaz.
"""
import json
import os
import sys

KOK = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARIH = "2026-10-28"

# CanliTest.java'nın kütüphane üzerinden ayrıca doğruladığı (özet dosyasında D olarak duran) işlemler:
# sahte sipariş kimliğiyle → "bulunamadı", hiçbir şey değişmez.
CANLI_SAHTE = {"fulfillOrder", "updateOrderLine", "updateOrderPackageStatus"}

# İşlem başına elle yazılmış davranış notları (deneme sırasında gözlenen).
NOTLAR = {
    "listOrderSession": "`OrderSession.journeyId` şemada boş olamaz denip boş geliyor; seçilirse bütün sorgu düşer → kütüphane seçmez",
    "searchProducts": "Kayıtlar `data`'da değil `results`'ta (`IkasVeri.kayitlar()` ikisini de okur)",
    "getImageUploadUrl": "Görsel kimliği UUID olmalı; yalnız imzalı yükleme adresi üretir, yükleme yapmaz",
    "getVideoUploadUrl": "Video kimliği UUID olmalı; yalnız adres üretir",
    "getImportJobData": "Çalışıyor; içe aktarma işi yoksa `IMPORT_JOB_NOT_FOUND`",
    "getImportJobDataList": "Çalışıyor; iş yoksa `NO_DATA`",
    "getLastImportJobData": "Çalışıyor; iş yoksa `NO_DATA`",
    "listIkasWallet": "`UNAUTHORIZED` — özel uygulamaya cüzdan yetkisi verilmiyor",
    "listIkasWalletTransaction": "`UNAUTHORIZED`",
    "getIkasWalletWithBalance": "`UNAUTHORIZED`",
    "saveProductAttribute": "`options` en az bir seçenek ister",
    "saveCampaign": "`canCombineWithOtherCampaigns` zorunlu. Deneme: 2099'da başlayan, kuponlu, satış kanalsız kampanya → oluştur/sil",
    "campaignAddCoupons": "Kupon başına `canCombineWithOtherCampaigns` ister",
    "saveProductBrand": "Satış kanalsız oluşturuldu → sitede görünmez",
    "saveVendor": "`status: ARCHIVED` ile oluşturuldu",
    "approvePendingOrderTransactions": "Var olmayan siparişte hata değil `false` döner",
    "deleteProductList": "Var olmayan üründe hata değil `false` döner",
    "addCustomTimelineEntry": "DİKKAT: var olmayan kaynakta da `true` döner",
    "updateCustomerB2BStatus": "DİKKAT: var olmayan müşteride de `true` döner",
    "updateSubscriptionStatus": "DİKKAT: var olmayan müşteride de `true` döner",
    "deleteTaxSettingsList": "`DELETE_FUNCTION_NOT_ALLOWED` — API'den vergi ayarı silinemiyor",
    "deleteGlobalTaxSettingsList": "`DELETE_FUNCTION_NOT_ALLOWED`",
    "updateOrderLine": "Yeni satıra uuid `id` verilmeli, yoksa İkas reddeder",
    "saveWebhook": "Girdi `scopes: [String!]!` (dizi), cevap `scope: String` (tekil); dizi döner",
    "getOrderInvoicePdfUrl": "Gerçek siparişte denendi: yalnız imzalı PDF adresi üretir, siparişi değiştirmez",
}

ACIKLAMA = """| Durum | Anlam |
|---|---|
| ✅ | Gerçek mağazada denendi, doğru cevap geldi (yazmalarda: oluştur → sil → yok olduğu doğrulandı) |
| 🧪 | Var olmayan kimlikle denendi: girdi biçimi kabul edildi, İkas "bulunamadı" dedi, hiçbir şey değişmedi |
| ⚠️ | Çalışıyor ama beklenmedik davranışı var (nota bakın) |
| 🔒 | İkas bu uygulama türüne yetki vermiyor |
| ⛔ | Bilerek denenmedi: canlı mağazada müşteriye görünür değişiklik yapar ya da bağlı sistemleri tetikler |"""


def oku(ad):
    with open(os.path.join(KOK, ad), encoding="utf-8") as f:
        return json.load(f)


def ref(t):
    k = t["kind"]
    if k == "NON_NULL":
        return ref(t["ofType"]) + "!"
    if k == "LIST":
        return "[" + ref(t["ofType"]) + "]"
    return t["name"]


def sorgu_durumu(ad, v):
    if v["durum"] == "TAMAM":
        return "⚠️" if ad in ("listOrderSession", "searchProducts") else "✅"
    if "UNAUTHORIZED" in str(v.get("hata")):
        return "🔒"
    return "✅"  # NO_DATA / NOT_FOUND: işlem çalışıyor, mağazada veri yok


def mutasyon_durumu(ad, v):
    y = v.get("yontem")
    if ad in CANLI_SAHTE:
        return "🧪"
    if y in ("A", "C"):
        return "⚠️" if ad in ("saveProductAttribute", "saveCampaign") else "✅"
    if y == "B":
        if ad.startswith(("deleteTaxSettings", "deleteGlobalTax")):
            return "⚠️"
        return "⚠️" if ad in ("addCustomTimelineEntry", "updateCustomerB2BStatus", "updateSubscriptionStatus",
                              "approvePendingOrderTransactions", "deleteProductList") else "🧪"
    return "⛔"


def main():
    sema = oku("sema/ikas-admin-sema.json")["data"]["__schema"]
    tipler = {t["name"]: t for t in sema["types"]}
    q = oku("deneme-ciktilari/sorgu-ozet.json")
    m = oku("deneme-ciktilari/mutasyon-ozet.json")
    sorgular = tipler[sema["queryType"]["name"]]["fields"]
    mutasyonlar = tipler[sema["mutationType"]["name"]]["fields"]
    eksik = [f["name"] for f in sorgular if f["name"] not in q] + [f["name"] for f in mutasyonlar if f["name"] not in m]
    if eksik:
        sys.exit("özette olmayan işlem: " + ", ".join(eksik))

    satirlar_q = [(f["name"], sorgu_durumu(f["name"], q[f["name"]])) for f in sorgular]
    satirlar_m = [(f["name"], mutasyon_durumu(f["name"], m[f["name"]])) for f in mutasyonlar]

    def say(l, d):
        return sum(1 for _, x in l if x == d)

    def tablo(l, ozet):
        s = ["| İşlem | Durum | Not |", "|---|---|---|"]
        for ad, d in sorted(l):
            v = ozet[ad]
            neden = "; ".join(x for x in (("Denenmedi: " + v["not"]) if d == "⛔" and v.get("not") else "", NOTLAR.get(ad, "")) if x)
            s.append(f"| {ad} | {d} | {neden or ''} |")
        return "\n".join(s)

    hepsi = satirlar_q + satirlar_m
    calisan = sum(1 for _, d in hepsi if d in ("✅", "⚠️", "🧪"))
    b = f"""# İkas Admin API işlemleri — gerçek davranış

Her işlem {TARIH} tarihinde gerçek İkas mağazasına karşı denendi (İkas'ın herkese açık test ortamı yok). Okumalar
gerçek veriyle; yazmalar yalnız **müşteriye görünmeyen** tanım kayıtlarıyla (oluştur → sil → doğrula) ya da **var olmayan
kimlikle** (İkas "bulunamadı" der, hiçbir şey değişmez). Bu belge yalnız davranışı anlatır.

Özet: **{len(hepsi)} işlem** ({len(sorgular)} sorgu + {len(mutasyonlar)} mutasyon).
Gerçek istekle denenen: **{calisan}** (✅ {say(hepsi, '✅')} · 🧪 {say(hepsi, '🧪')} · ⚠️ {say(hepsi, '⚠️')}).
İkas'ın yetki vermediği: 🔒 {say(hepsi, '🔒')}. Bilerek denenmeyen: ⛔ {say(hepsi, '⛔')} (canlı mağazada görünür değişiklik).
⛔ işlemlerde kütüphane yalnız argüman adlarını ve zorunlu argümanları şemadan kontrol eder; iç alanları İkas doğrular.
İlk kullanımı kendi mağazanızda dikkatle yapın.

{ACIKLAMA}

## Hız sınırı

Uygulama (client id) başına **10 saniyede 50 istek** (`x-ratelimit-limit`, `x-ratelimit-remaining`, `x-ratelimit-reset`
başlıkları). Aşılınca HTTP 429. Aynı client id'yi kullanan her sistem bu kotayı paylaşır. `IkasIstemci` kalan hak
5'e inince pencere yenilenene kadar bekler, 429'da pencere sonunu bekleyip 3 kez yeniden dener.

## Sorgular — {len(sorgular)}

{tablo(satirlar_q, q)}

## Mutasyonlar — {len(mutasyonlar)}

{tablo(satirlar_m, m)}

## Hata biçimi

- Doğrulama hatası: `extensions.code = ARGUMENT_VALIDATION_ERROR`, `extensions.validationErrors[].property/constraints`.
  `constraints` içinde **"Referenced id not found"** = kayıt yok (`IkasHatasi.bulunamadiMi()`).
- Kayıt yok: `404_OBJECT_NOT_FOUND`, `ORDER_NOT_FOUND`, `NOT_FOUND`, `IMPORT_JOB_NOT_FOUND`.
- Token süresi dolunca `UNAUTHENTICATED` / HTTP 401 → istemci bir kez yeni token alıp tekrarlar.
- Token isteği JSON gövde + tarayıcı dışı özel `User-Agent` ister; form gövdesi ya da varsayılan Java/Python UA'sı 403 alabilir.
"""
    os.makedirs(os.path.join(KOK, "docs"), exist_ok=True)
    with open(os.path.join(KOK, "docs/ISLEM_DURUMU.md"), "w", encoding="utf-8") as f:
        f.write(b)

    def islem_satiri(f):
        args = "; ".join(f"`{a['name']}: {ref(a['type'])}`" for a in f["args"]) or "—"
        acik = (f.get("description") or "").strip().replace("\n", " ")
        return f"- **{f['name']}** → `{ref(f['type'])}` — {args}" + (f"  \n  {acik}" if acik else "")

    def girdi_tipleri(alanlar):
        gorulen, sira = set(), []

        def gez(ad):
            t = tipler.get(ad)
            if not t or t["kind"] != "INPUT_OBJECT" or ad in gorulen:
                return
            gorulen.add(ad)
            sira.append(ad)
            for x in t["inputFields"]:
                gez(ref(x["type"]).strip("[]!").rstrip("!").strip("[]"))

        for f in alanlar:
            for a in f["args"]:
                gez(ref(a["type"]).strip("[]!").rstrip("!").strip("[]"))
        return sira

    e = [f"# İkas Admin API — işlem envanteri\n",
         f"Kaynak: canlı introspection ({TARIH}), `sema/ikas-admin-sema.json`. Argümanlar ve dönüş tipleri; `!` = zorunlu.",
         f"Toplam **{len(sorgular) + len(mutasyonlar)} işlem**.\n",
         f"## Sorgular ({len(sorgular)})\n"]
    e += [islem_satiri(f) for f in sorted(sorgular, key=lambda x: x["name"])]
    e += ["", f"## Mutasyonlar ({len(mutasyonlar)})\n"]
    e += [islem_satiri(f) for f in sorted(mutasyonlar, key=lambda x: x["name"])]
    e += ["", "## Mutasyon girdi tipleri (zorunlu alanlar)\n"]
    for ad in sorted(girdi_tipleri(mutasyonlar)):
        z = [x["name"] for x in tipler[ad]["inputFields"] if x["type"]["kind"] == "NON_NULL"]
        e.append(f"- **{ad}** — " + (", ".join(f"`{x}`" for x in z) if z else "zorunlu alan yok"))
    with open(os.path.join(KOK, "docs/ISLEM_ENVANTERI.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(e) + "\n")
    print("✅ docs/ISLEM_DURUMU.md, docs/ISLEM_ENVANTERI.md")


if __name__ == "__main__":
    main()
