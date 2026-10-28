package tr.com.sevinclidoga.ikas;

import java.time.Duration;
import java.util.Objects;

/**
 * Bağlantı ayarları. İkas'ın herkese açık test ortamı yoktur: her istek CANLI mağazaya gider. Bu yüzden yazma
 * işlemleri (mutasyonlar) {@link #yazmayaIzinVer()} çağrılmadan reddedilir.
 *
 * @param magaza        mağaza alt alan adı ({@code https://<magaza>.myikas.com})
 * @param istemciKimligi özel uygulama client id
 * @param istemciSirri   özel uygulama client secret
 * @param zamanAsimi     tek istek için üst süre
 * @param yazmaIzni      mutasyonlara izin
 * @param graphqlAdresi  GraphQL uç noktası (test için değiştirilebilir)
 * @param tokenAdresi    OAuth token adresi; boşsa {@code https://<magaza>.myikas.com/api/admin/oauth/token}
 */
public record IkasAyarlari(String magaza, String istemciKimligi, String istemciSirri, Duration zamanAsimi, boolean yazmaIzni,
                           String graphqlAdresi, String tokenAdresi) {

    public static final String GRAPHQL = "https://api.myikas.com/api/v1/admin/graphql";

    public IkasAyarlari {
        Objects.requireNonNull(magaza, "magaza");
        Objects.requireNonNull(istemciKimligi, "istemciKimligi");
        Objects.requireNonNull(istemciSirri, "istemciSirri");
        if (zamanAsimi == null) zamanAsimi = Duration.ofSeconds(60);
        if (graphqlAdresi == null || graphqlAdresi.isBlank()) graphqlAdresi = GRAPHQL;
        if (tokenAdresi == null || tokenAdresi.isBlank()) tokenAdresi = "https://" + magaza + ".myikas.com/api/admin/oauth/token";
    }

    /** Salt okuma (yazma kapalı). */
    public static IkasAyarlari of(String magaza, String istemciKimligi, String istemciSirri) {
        return new IkasAyarlari(magaza, istemciKimligi, istemciSirri, null, false, null, null);
    }

    /** Aynı ayarlar, mutasyonlara izinli. Bilerek çağrılmalı: değişiklikler canlı mağazaya gider. */
    public IkasAyarlari yazmayaIzinVer() {
        return new IkasAyarlari(magaza, istemciKimligi, istemciSirri, zamanAsimi, true, graphqlAdresi, tokenAdresi);
    }

    public IkasAyarlari zamanAsimiyla(Duration d) {
        return new IkasAyarlari(magaza, istemciKimligi, istemciSirri, d, yazmaIzni, graphqlAdresi, tokenAdresi);
    }

    /** Sır loglara / hata mesajlarına düşmesin. */
    @Override
    public String toString() {
        return "IkasAyarlari[magaza=" + magaza + ", yazmaIzni=" + yazmaIzni + "]";
    }
}
