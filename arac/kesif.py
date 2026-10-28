#!/usr/bin/env python3
"""İkas Admin GraphQL keşif aracı (yalnız geliştirme).

Şemadan (sema/ikas-admin-sema.json) her sorgu için seçim kümesini kendisi kurar, zorunlu parametreleri önceki
cevaplardan bulduğu gerçek kimliklerle doldurur, sorguyu çalıştırır.

  IKAS_CLIENT_ID=… IKAS_CLIENT_SECRET=… IKAS_MAGAZA=… python3 -I arac/kesif.py sorgular

Güvenlik:
  * Yalnız SORGU çalıştırır (salt okuma). Mutasyonlar ayrı, elle seçilmiş betikte.
  * Ham cevap diske YAZILMAZ. src/test/resources/ornek/ altına yalnız ANONİM hali yazılır (beyaz liste).
  * Sayım/özet (iş hacmi içerebilir) deneme-ciktilari/ altına gider (git dışı).
"""
import hashlib
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request

KOK = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SEMA = json.load(open(os.path.join(KOK, "sema", "ikas-admin-sema.json")))["data"]["__schema"]
TIP = {t["name"]: t for t in SEMA["types"]}
ORNEK = os.path.join(KOK, "src", "test", "resources", "ornek")
CIKTI = os.path.join(KOK, "deneme-ciktilari")
UA = {"User-Agent": "ikas-admin-java/0.1 (+https://github.com/ernerdl)"}
GRAPHQL = "https://api.myikas.com/api/v1/admin/graphql"


# ── HTTP ────────────────────────────────────────────────────────────────────────

def token():
    m = os.environ["IKAS_MAGAZA"]
    req = urllib.request.Request(
        f"https://{m}.myikas.com/api/admin/oauth/token",
        json.dumps({"grant_type": "client_credentials", "client_id": os.environ["IKAS_CLIENT_ID"],
                    "client_secret": os.environ["IKAS_CLIENT_SECRET"]}).encode(),
        {"Content-Type": "application/json", "Accept": "application/json", **UA})
    return json.loads(urllib.request.urlopen(req, timeout=30).read())["access_token"]


_TOKEN = None


def gql(sorgu, degiskenler=None):
    global _TOKEN
    _TOKEN = _TOKEN or token()
    req = urllib.request.Request(GRAPHQL, json.dumps({"query": sorgu, "variables": degiskenler or {}}).encode(),
                                 {"Content-Type": "application/json", "Authorization": "Bearer " + _TOKEN, **UA})
    t0 = time.time()
    try:
        govde = urllib.request.urlopen(req, timeout=90).read().decode()
        kod = 200
    except urllib.error.HTTPError as e:
        govde, kod = e.read().decode(errors="replace"), e.code
    time.sleep(0.35)
    try:
        return kod, json.loads(govde), int((time.time() - t0) * 1000)
    except ValueError:
        return kod, {"errors": [{"message": govde[:300]}]}, int((time.time() - t0) * 1000)


# ── şema yardımcıları ───────────────────────────────────────────────────────────

def ic(tr):
    """NON_NULL/LIST sarmalayıcılarını soyup (ad, tür) döndürür."""
    while tr.get("ofType") and tr["kind"] in ("NON_NULL", "LIST"):
        tr = tr["ofType"]
    return tr["name"], tr["kind"]


def zorunlu(tr):
    return tr["kind"] == "NON_NULL"


# İkas'ın "boş olamaz" deyip boş döndürdüğü alanlar (seçilirse tüm sorgu düşer) — canlıda görüldü 2026-10-28
BOZUK_ALAN = {("OrderSession", "journeyId")}


def secim(tip_adi, derinlik, yol=()):
    """Tipin seçim kümesi: skaler/enum alanlar + nesneler (derinlik sınırına kadar). Zorunlu argümanlı alanlar atlanır."""
    t = TIP[tip_adi]
    if t["kind"] in ("SCALAR", "ENUM"):
        return ""
    if t["kind"] in ("UNION", "INTERFACE"):
        return "{ __typename }"
    parcalar = []
    for f in t.get("fields") or []:
        if any(zorunlu(a["type"]) for a in f["args"]) or f.get("isDeprecated") or (tip_adi, f["name"]) in BOZUK_ALAN:
            continue
        ad, tur = ic(f["type"])
        if tur in ("SCALAR", "ENUM"):
            parcalar.append(f["name"])
        elif derinlik > 0 and ad not in yol:
            alt = secim(ad, derinlik - 1, yol + (tip_adi,))
            if alt:
                parcalar.append(f["name"] + " " + alt)
    return "{ " + " ".join(parcalar) + " }" if parcalar else ""


# ── anonimleştirme (beyaz liste) ────────────────────────────────────────────────

ACIK_REFERANS = {"listCountry", "listState", "listCity", "listDistrict", "listTown", "listCurrency", "listLanguage",
                 "listCargoCompany"}
PARA_SAYI = re.compile(r"(?i)(price|amount|total|cost|balance|discount|tax|fee|rate|count|quantity|stock|credit|"
                       r"weight|desi|limit|value|point)")
ZAMAN = re.compile(r"(?i)(at|date|time|until|from|to)$")
ENUM_DEGER = re.compile(r"^[A-Z][A-Z0-9_]*$")
KORU_ANAHTAR = {"__typename", "currencyCode", "currency", "currencySymbol", "locale", "iso2", "iso3", "type", "status",
                "languageCode", "countryCode", "code", "unit", "mimeType"}


def _h(s):
    return hashlib.sha256(("ikas:" + s).encode()).hexdigest()


def sahte_kimlik(v):
    if re.fullmatch(r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", v):
        h = _h(v)
        return f"{h[:8]}-{h[8:12]}-{h[12:16]}-{h[16:20]}-{h[20:32]}"
    return "ANON-" + _h(v)[:10]


HATA_ANAHTAR = {"message", "property", "serviceName"}   # İkas hata mesajları kişisel veri değildir, testler bunlara bakar


def anonim(x, anahtar="", acik=False, ust=""):
    if isinstance(x, dict):
        return {k: anonim(v, k, acik, anahtar) for k, v in x.items()}
    if isinstance(x, list):
        return [anonim(v, anahtar, acik, ust) for v in x]
    if isinstance(x, str) and (anahtar in HATA_ANAHTAR or ust == "constraints"):
        return x
    if isinstance(x, bool) or x is None:
        return x
    if isinstance(x, (int, float)):
        if ZAMAN.search(anahtar) or x > 1_000_000_000_000:   # zaman damgası
            return x
        if PARA_SAYI.search(anahtar):
            return round((int(_h(f"{anahtar}:{x}")[:6], 16) % 50000) / 100, 2) if isinstance(x, float) else int(_h(f"{anahtar}:{x}")[:4], 16) % 50
        return x
    s = str(x)
    if acik:
        return s
    if anahtar in KORU_ANAHTAR or ENUM_DEGER.match(s):
        return s
    if ZAMAN.search(anahtar) and re.match(r"^\d{4}-\d{2}-\d{2}", s):
        return s
    if anahtar.lower().endswith("id") or anahtar == "id" or re.fullmatch(r"[0-9a-f-]{36}", s):
        return sahte_kimlik(s)
    if s.startswith("http"):
        return "https://ornek.invalid/" + _h(s)[:12]
    return "ANON-" + _h(s)[:8]


def kaydet_ornek(ad, cevap, acik=False):
    os.makedirs(ORNEK, exist_ok=True)
    with open(os.path.join(ORNEK, ad + ".json"), "w") as f:
        json.dump(anonim(cevap, acik=acik), f, ensure_ascii=False, indent=1)


# ── sorgu keşfi ─────────────────────────────────────────────────────────────────

def say(veri):
    """Cevaptaki kayıt sayısı (liste ya da sayfalı {data})."""
    if isinstance(veri, list):
        return len(veri)
    if isinstance(veri, dict) and isinstance(veri.get("data"), list):
        return len(veri["data"]) or len(veri.get("results") or [])
    return 1 if veri else 0


def ilk(veri):
    if isinstance(veri, list):
        return veri[0] if veri else None
    if isinstance(veri, dict) and isinstance(veri.get("data"), list):
        return veri["data"][0] if veri["data"] else None
    return veri


def arg_degeri(op, a, ctx):
    ad, tur = ic(a["type"])
    n = a["name"]
    if ad == "PaginationInput":
        return {"limit": 3, "page": 1}
    if not zorunlu(a["type"]):
        return None
    if ad == "StringFilterInput":
        anahtar = {"stateId": "stateId", "countryId": "countryId", "cityId": "cityId", "districtId": "districtId",
                   "orderId": "orderId"}.get(n)
        return {"eq": ctx.get(anahtar)} if anahtar and ctx.get(anahtar) else None
    if ad == "String":
        return {"customerId": ctx.get("customerId"), "orderId": ctx.get("orderId"),
                "imageId": str(__import__("uuid").uuid4()), "videoId": str(__import__("uuid").uuid4()),
                "importJobId": "00000000-0000-0000-0000-000000000000"}.get(n)
    if ad == "ListAbandonedCartInput":
        return {}
    if ad == "SearchInput":
        return {"query": "ekmek", "pagination": {"limit": 3, "page": 1}, "barcodeList": [], "productIdList": [], "skuList": []}
    if ad == "GetIkasWalletWithBalanceInput":
        return {"currencyCode": "TRY"}
    if ad == "ImportTypeEnum":
        return "PRODUCT"
    return None


def gql_tip(tr):
    if tr["kind"] == "NON_NULL":
        return gql_tip(tr["ofType"]) + "!"
    if tr["kind"] == "LIST":
        return "[" + gql_tip(tr["ofType"]) + "]"
    return tr["name"]


def sorgu_calistir(f, ctx, derinlik=3):
    degiskenler, imza, cagri = {}, [], []
    for a in f["args"]:
        v = arg_degeri(f["name"], a, ctx)
        if v is None:
            if zorunlu(a["type"]):
                return {"durum": "ATLANDI", "neden": f"zorunlu parametre bulunamadı: {a['name']}"}
            continue
        degiskenler[a["name"]] = v
        imza.append(f"${a['name']}: {gql_tip(a['type'])}")
        cagri.append(f"{a['name']}: ${a['name']}")
    donus, _ = ic(f["type"])
    sec = secim(donus, derinlik)
    metin = "query" + (f"({', '.join(imza)})" if imza else "") + " { " + f["name"] + \
            (f"({', '.join(cagri)})" if cagri else "") + (" " + sec if sec else "") + " }"
    kod, cevap, ms = gql(metin, degiskenler)
    hatalar = cevap.get("errors") or []
    if hatalar and derinlik > 1 and any(re.search(r"(?i)complex|depth|too (large|deep)|timeout", h.get("message", ""))
                                        for h in hatalar):
        return sorgu_calistir(f, ctx, derinlik - 1)
    veri = (cevap.get("data") or {}).get(f["name"])
    return {"durum": "HATA" if hatalar and veri is None else "TAMAM", "http": kod, "ms": ms, "derinlik": derinlik,
            "hata": "; ".join(h.get("message", "")[:200] for h in hatalar) or None, "kayit": say(veri),
            "veri": veri, "cevap": cevap, "sorgu": metin, "degiskenler": degiskenler}


def sorgular():
    q = TIP[SEMA["queryType"]["name"]]["fields"]
    ctx = {}
    # bağımlılık sırası: önce kimlik üretenler
    sira = ["listOrder", "listCountry", "listState", "listCity", "listDistrict", "listTown", "listCustomer"]
    adlar = sira + sorted(f["name"] for f in q if f["name"] not in sira)
    alanlar = {f["name"]: f for f in q}
    ozet = {}
    for ad in adlar:
        f = alanlar[ad]
        if ad == "listState":
            tr = next((c for c in (ozet.get("listCountry", {}).get("_liste") or []) if c.get("iso2") == "TR"), None)
            ctx["countryId"] = tr and tr.get("id")
        r = sorgu_calistir(f, ctx)
        veri = r.get("veri")
        if ad == "listOrder" and ilk(veri):
            o = ilk(veri)
            ctx["orderId"] = o.get("id")
            ctx["customerId"] = (o.get("customer") or {}).get("id") or o.get("customerId")
        if ad == "listState":
            s = next((x for x in (veri or []) if "Ankara" in (x.get("name") or "")), ilk(veri))
            ctx["stateId"] = s and s.get("id")
        if ad == "listCity":
            c = next((x for x in (veri or []) if "Ankara" in (x.get("name") or "")), ilk(veri))
            ctx["cityId"] = c and c.get("id")
        if ad == "listDistrict":
            ctx["districtId"] = (ilk(veri) or {}).get("id")
        r["_liste"] = veri if isinstance(veri, list) else None
        ozet[ad] = r
        if r["durum"] == "TAMAM":
            kaydet_ornek("sorgu-" + ad, r["cevap"], acik=ad in ACIK_REFERANS)
        print(f"{ad:34s} {r['durum']:7s} kayıt={r.get('kayit', '-')!s:4s} d={r.get('derinlik', '-')} "
              f"{r.get('ms', '-')}ms {(r.get('hata') or r.get('neden') or '')[:110]}", flush=True)
    os.makedirs(CIKTI, exist_ok=True)
    with open(os.path.join(CIKTI, "sorgu-ozet.json"), "w") as f:
        json.dump({k: {kk: vv for kk, vv in v.items() if kk not in ("veri", "cevap", "_liste")} for k, v in ozet.items()},
                  f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    {"sorgular": sorgular}[sys.argv[1]]()
