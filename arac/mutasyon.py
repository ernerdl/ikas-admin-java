#!/usr/bin/env python3
"""İkas mutasyon denemeleri — CANLI mağazada güvenli yöntemle (yalnız geliştirme).

  IKAS_CLIENT_ID=… IKAS_CLIENT_SECRET=… IKAS_MAGAZA=… python3 -I arac/mutasyon.py

Üç yöntem, başka hiçbir şey yapılmaz:
  A) OLUŞTUR → SİL: müşteriye görünmeyen, webhook tetiklemeyen tanım kayıtları (etiket, grup, birim, özellik, varyant
     tipi, satış kanalsız marka, arşivli tedarikçi, 2099'da başlayan kuponlu ve satış kanalsız kampanya + kupon).
     Ad öneki "zz-kesif-deneme-silinecek"; silindikten sonra ayrıca sorgulanıp yok olduğu doğrulanır.
  B) SAHTE KİMLİKLE YOKLAMA: var olmayan rastgele UUID ile çağrı → İkas "bulunamadı" der, hiçbir kayıt değişmez.
     Erişim/yetki ve hata biçimi görülür.
  C) DEĞİŞİKLİK YAPMAYAN: gerçek sipariş için fatura PDF adresi.
Sipariş/ürün/müşteri üzerinde GERÇEK yazma YOK (İkas webhook'u bağlı canlı sistemleri tetikler). Webhook, site scripti, vergi,
satış kanalı, ürün/fiyat/stok, müşteri/sipariş oluşturma, kategori, ürün sıralaması, Wallet, uygulama ödemesi: DOKUNULMAZ.
"""
import json
import os
import sys
import time
import uuid

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import kesif as k  # noqa: E402

ON = "zz-kesif-deneme-silinecek"
SONUC = {}


def sahte():
    return str(uuid.uuid4())


def calis(ad, sorgu, degisken=None, ornek=None):
    kod, cevap, ms = k.gql(sorgu, degisken)
    hatalar = cevap.get("errors") or []
    veri = cevap.get("data")
    r = {"http": kod, "ms": ms, "hata": "; ".join(
        (h.get("message", "") + (" [" + str((h.get("extensions") or {}).get("code")) + "]" if (h.get("extensions") or {}).get("code") else ""))[:220]
        for h in hatalar) or None, "veri_var": bool(veri and any(v is not None for v in veri.values()))}
    if ornek:
        k.kaydet_ornek("mutasyon-" + ornek, cevap)
    return r, veri


def kaydet(islem, yontem, sonuc, not_=""):
    SONUC[islem] = {"yontem": yontem, **sonuc, "not": not_}
    durum = "HATA" if sonuc.get("hata") and not sonuc.get("beklenen") else "TAMAM"
    print(f"{islem:36s} {yontem:7s} {durum:5s} {(sonuc.get('hata') or not_ or '')[:120]}", flush=True)


# ── A) oluştur → sil ────────────────────────────────────────────────────────────

def olustur_sil(save, delete, liste, girdi_tipi, girdi, secim="id name"):
    ad = f"{ON}-{int(time.time())}"
    girdi = {**girdi, ("title" if "title" in girdi else "name"): ad}
    r, v = calis(save, f"mutation($i: {girdi_tipi}!) {{ {save}(input: $i) {{ {secim} }} }}", {"i": girdi}, ornek=save)
    kid = ((v or {}).get(save) or {}).get("id")
    if not kid:
        kaydet(save, "A", r, "oluşturulamadı")
        return None
    kaydet(save, "A", r, f"oluşturuldu")
    return kid, (lambda: sil_dogrula(delete, liste, kid))


def sil_dogrula(delete, liste, kid):
    r, v = calis(delete, f"mutation($l: [String!]!) {{ {delete}(idList: $l) }}", {"l": [kid]}, ornek=delete)
    kaydet(delete, "A", r, "silindi")
    r2, v2 = calis(liste + "-dogrula", f"query($i: StringFilterInput) {{ {liste}(id: $i) {{ id }} }}", {"i": {"eq": kid}})
    kalan = (v2 or {}).get(liste) or []
    if kalan:
        print(f"   !!! {liste}: silinen kayıt hâlâ listede görünüyor ({kid})", flush=True)
    SONUC[delete]["dogrulama"] = "yok (silindi)" if not kalan else "HÂLÂ VAR"
    return not kalan


def a_testleri():
    temizlik = []
    for save, delete, liste, tip, girdi in [
        ("saveCustomerTag", "deleteCustomerTagList", "listCustomerTag", "CustomerTagInput", {}),
        ("saveOrderTag", "deleteOrderTagList", "listOrderTag", "OrderTagInput", {}),
        ("saveCustomerGroup", "deleteCustomerGroupList", "listCustomerGroup", "CustomerGroupInput",
         {"conditions": [], "priceListRules": [], "isAutomated": False}),
        ("saveProductTag", "deleteProductTagList", "listProductTag", "ProductTagInput", {"translations": []}),
        ("saveProductUnit", "deleteProductUnitList", "listProductUnit", "ProductUnitInput", {"translations": []}),
        ("saveProductAttribute", "deleteProductAttributeList", "listProductAttribute", "ProductAttributeInput",
         {"type": "TEXT", "options": [{"name": "deneme"}], "translations": []}),
        ("saveVariantType", "deleteVariantTypeList", "listVariantType", "VariantTypeInput",
         {"selectionType": "CHOICE", "translations": [], "values": [{"name": "deneme"}]}),
        ("saveProductBrand", "deleteProductBrandList", "listProductBrand", "ProductBrandInput",
         {"salesChannelIds": [], "translations": []}),
        ("saveVendor", "deleteVendorList", "listVendor", "VendorInput", {"status": "ARCHIVED"}),
    ]:
        try:
            s = olustur_sil(save, delete, liste, tip, girdi)
            if s:
                temizlik.append(s)
                s[1]()
        except Exception as e:  # noqa: BLE001
            print(f"   !!! {save}: {e}", flush=True)
    return temizlik


def kampanya_kupon():
    """2099'da başlayan, kuponlu, satış kanalsız, ürün fiyatına uygulanmayan kampanya → kupon → sil."""
    bas = int(time.mktime((2099, 1, 1, 0, 0, 0, 0, 0, 0)) * 1000)
    girdi = {"title": f"{ON}-{int(time.time())}", "type": "FIXED_AMOUNT", "applicablePrice": "SELL_PRICE",
             "canCombineWithOtherCampaigns": False, "hasCoupon": True, "applyCampaignToProductPrice": False,
             "applicableCustomerGroupIds": [], "applicableCustomerIds": [], "applicableCustomerSegmentIds": [],
             "currencyCodes": ["TRY"], "periodUsageLimits": [], "salesChannelIds": [], "translations": [],
             "dateRange": {"start": bas, "end": bas + 86_400_000}, "usageLimit": 1,
             "fixedDiscount": {"amount": 1, "filters": [], "isApplyByCartAmount": False}}
    r, v = calis("saveCampaign", "mutation($i: CampaignInput!) { saveCampaign(input: $i) { id title hasCoupon dateRange { start end } } }",
                 {"i": girdi}, ornek="saveCampaign")
    cid = ((v or {}).get("saveCampaign") or {}).get("id")
    kaydet("saveCampaign", "A", r, "2099 tarihli, kuponlu, satış kanalsız" if cid else "oluşturulamadı")
    if not cid:
        return
    kod = "ZZKESIF" + uuid.uuid4().hex[:8].upper()
    r, v = calis("campaignAddCoupons", "mutation($i: AddCouponsInput!) { campaignAddCoupons(input: $i) { id code } }",
                 {"i": {"campaignId": cid, "coupons": [{"code": kod, "usageLimit": 1, "canCombineWithOtherCampaigns": False}]}}, ornek="campaignAddCoupons")
    kuponlar = [c["id"] for c in ((v or {}).get("campaignAddCoupons") or [])]
    kaydet("campaignAddCoupons", "A", r, f"{len(kuponlar)} kupon")
    r2, v2 = calis("listCoupon-dogrula", "query($c: StringFilterInput) { listCoupon(campaignId: $c) { data { id code } } }",
                   {"c": {"eq": cid}})
    if kuponlar:
        r, v = calis("deleteCouponList", "mutation($l: [String!]!) { deleteCouponList(idList: $l) }", {"l": kuponlar},
                     ornek="deleteCouponList")
        kaydet("deleteCouponList", "A", r, "kupon silindi")
    sil_dogrula_kampanya(cid)


def sil_dogrula_kampanya(cid):
    r, v = calis("deleteCampaignList", "mutation($l: [String!]!) { deleteCampaignList(idList: $l) }", {"l": [cid]},
                 ornek="deleteCampaignList")
    kaydet("deleteCampaignList", "A", r, "kampanya silindi")
    r2, v2 = calis("listCampaign-dogrula", "query($i: StringFilterInput) { listCampaign(id: $i) { data { id } } }",
                   {"i": {"eq": cid}})
    kalan = ((v2 or {}).get("listCampaign") or {}).get("data") or []
    SONUC["deleteCampaignList"]["dogrulama"] = "yok (silindi)" if not kalan else "HÂLÂ VAR"
    if kalan:
        print("   !!! kampanya hâlâ listede", cid, flush=True)


# ── B) sahte kimlikle yoklama ───────────────────────────────────────────────────

def b_testleri():
    s = sahte
    yoklamalar = [
        ("addOrderInvoice", "AddOrderInvoiceInput", {"appId": s(), "invoiceNumber": "KESIF-0", "orderId": s(),
                                                      "sendNotificationToCustomer": False, "type": "INDIVIDUAL"}, "id"),
        ("addOrderTag", "UpdateOrderTagInput", {"orderId": s(), "orderTagId": s()}, None),
        ("removeOrderTag", "UpdateOrderTagInput", {"orderId": s(), "orderTagId": s()}, None),
        ("approvePendingOrderTransactions", "ApproveOrderTransactionsInput", {"orderId": s(), "paymentMethods": ["MONEY_ORDER"]}, None),
        ("cancelFulfillment", "CancelFulfillmentInput", {"lines": [], "orderId": s(), "orderPackageId": s()}, "id"),
        ("cancelOrderLine", "CancelOrderLineInput", {"orderId": s(), "orderLineItems": []}, "id"),
        ("changeStockLocation", "ChangeStockLocationInput", {"orderId": s(), "orderLineItemIds": [], "targetStockLocationId": s()}, "id"),
        ("generateOrderPaymentLink", "GenerateOrderPaymentLinkInput", {"orderId": s()}, None),
        ("refundOrderLine", "OrderRefundInput", {"orderId": s(), "orderRefundLines": [], "orderRefundTransactions": [],
                                                 "orderRejectedRefundLines": [], "stockLocationId": s(),
                                                 "sendNotificationToCustomer": False}, "id"),
        ("updateOrderAddresses", "UpdateOrderAddressesInput", {"orderId": s()}, "id"),
        ("addCustomTimelineEntry", "TimelineInput", {"message": "kesif-deneme", "sourceId": s(), "sourceType": "ORDER"}, None),
        ("updateStoreCreditAmount", "UpdateStoreCreditAmountInput", {"amount": 0, "id": s()}, "id"),
    ]
    for ad, tip, girdi, secim in yoklamalar:
        r, _ = calis(ad, f"mutation($i: {tip}!) {{ {ad}(input: $i){(' { ' + secim + ' }') if secim else ''} }}", {"i": girdi},
                     ornek=ad + "-bulunamadi")
        r["beklenen"] = True
        kaydet(ad, "B", r, "sahte kimlik → değişiklik yok")
    for ad, govde in [
        ("updateCustomerB2BStatus", f'mutation {{ updateCustomerB2BStatus(customerId: "{s()}", b2bStatus: WAITING_FOR_APPROVAL) }}'),
        ("updateSubscriptionStatus", f'mutation {{ updateSubscriptionStatus(customerId: "{s()}", subscriptionStatus: NOT_SUBSCRIBED) }}'),
    ]:
        r, _ = calis(ad, govde, ornek=ad + "-bulunamadi")
        r["beklenen"] = True
        kaydet(ad, "B", r, "sahte müşteri → değişiklik yok")
    for ad in ["deleteCustomerList", "deleteProductList", "deleteCategoryList", "deleteProductVolumeDiscountList",
               "deleteStorefrontPolicyList", "deleteProductOrderList", "deleteTaxSettingsList", "deleteGlobalTaxSettingsList"]:
        r, _ = calis(ad, f"mutation($l: [String!]!) {{ {ad}(idList: $l) }}", {"l": [s()]}, ornek=ad + "-yok")
        r["beklenen"] = True
        kaydet(ad, "B", r, "sahte kimlik silindi → hiçbir şey olmadı")


# ── C) değişiklik yapmayan ──────────────────────────────────────────────────────

def c_testleri():
    r, v = calis("listOrder-fatura", "query { listOrder(pagination: {limit: 50, page: 1}) { data { id invoices { id invoiceNumber } } } }")
    hedef = next((o for o in ((v or {}).get("listOrder") or {}).get("data", []) if o.get("invoices")), None)
    if not hedef:
        kaydet("getOrderInvoicePdfUrl", "C", {"hata": None}, "son 50 siparişte İkas faturası yok → sahte kimlikle yoklandı")
        r, _ = calis("getOrderInvoicePdfUrl", "mutation($i: GetOrderInvoicePdfUrlInput!) { getOrderInvoicePdfUrl(input: $i) }",
                     {"i": {"orderId": sahte(), "invoiceId": sahte()}}, ornek="getOrderInvoicePdfUrl-bulunamadi")
        r["beklenen"] = True
        kaydet("getOrderInvoicePdfUrl", "B", r, "sahte kimlik")
        return
    r, v = calis("getOrderInvoicePdfUrl", "mutation($i: GetOrderInvoicePdfUrlInput!) { getOrderInvoicePdfUrl(input: $i) }",
                 {"i": {"orderId": hedef["id"], "invoiceId": hedef["invoices"][0]["id"]}}, ornek="getOrderInvoicePdfUrl")
    kaydet("getOrderInvoicePdfUrl", "C", r, "gerçek sipariş, yalnız adres üretir" + (" (adres geldi)" if r["veri_var"] else ""))


DOKUNULMAZ = {
    "saveWebhook": "mağazanın canlı bildirim (webhook) bağlantısını değiştirir; bağlı sistemler olay almayı bırakabilir", "deleteWebhook": "bağlı sistemlerin olay bildirimlerini keser",
    "saveStorefrontJSScript": "canlı sitede çalışan script", "deleteStorefrontJSScript": "TÜM site scriptlerini siler",
    "saveTaxSettings": "vergi ayarı", "saveGlobalTaxSettings": "vergi ayarı", "saveSalesChannel": "satış kanalı",
    "saveProduct": "ürün oluşturur/değiştirir (sitede görünür, webhook tetikler)", "bulkUpdateProducts": "toplu ürün değişikliği",
    "saveVariantPrices": "müşterinin gördüğü fiyat", "saveProductStockLocations": "stok", "updateProductSalesChannelStatus": "ürünü sitede açar/kapatır",
    "saveProductVolumeDiscount": "ürün fiyatına indirim", "saveCategory": "kategori menüde görünebilir",
    "saveProductOrder": "sitedeki ürün sıralaması", "saveStorefrontPolicy": "sitede politika sayfası",
    "saveCustomer": "gerçek müşteri kaydı açar (webhook tetikler)", "createOrderWithTransactions": "gerçek sipariş oluşturur",
    "fulfillOrder": "siparişi kargoya verir", "updateOrderLine": "sipariş satırlarını değiştirir",
    "updateOrderPackageStatus": "paket/kargo durumunu değiştirir", "createWalletTransaction": "para hareketi",
    "createMerchantAppPayment": "uygulama ödemesi", "createMerchantAppPaymentWithSubscription": "uygulama ödemesi",
    "getAppDemoDay": "uygulama aboneliği (geliştirici)",
}

def yeniden():
    """Yalnız düzeltilen iki A testi."""
    s = olustur_sil("saveProductAttribute", "deleteProductAttributeList", "listProductAttribute", "ProductAttributeInput",
                    {"type": "TEXT", "options": [{"name": "deneme"}], "translations": []})
    if s:
        s[1]()
    kampanya_kupon()


if __name__ == "__main__" and len(sys.argv) > 1 and sys.argv[1] == "yoklama":
    b_testleri()
    c_testleri()
    sys.exit(0)

if __name__ == "__main__" and len(sys.argv) > 1 and sys.argv[1] == "yeniden":
    yeniden()
    eski = json.load(open(os.path.join(k.CIKTI, "mutasyon-ozet.json")))
    eski.update(SONUC)
    json.dump(eski, open(os.path.join(k.CIKTI, "mutasyon-ozet.json"), "w"), ensure_ascii=False, indent=1)
    sys.exit(0)

if __name__ == "__main__":
    k._TOKEN = None
    print("── A) oluştur → sil", flush=True)
    a_testleri()
    kampanya_kupon()
    print("── B) sahte kimlikle yoklama", flush=True)
    b_testleri()
    print("── C) değişiklik yapmayan", flush=True)
    c_testleri()
    for ad, neden in DOKUNULMAZ.items():
        SONUC.setdefault(ad, {"yontem": "D", "not": neden})
    os.makedirs(k.CIKTI, exist_ok=True)
    json.dump(SONUC, open(os.path.join(k.CIKTI, "mutasyon-ozet.json"), "w"), ensure_ascii=False, indent=1)
    print(f"toplam {len(SONUC)} mutasyon kaydı", flush=True)
