package no.nav.familie.ba.sak.config

enum class FeatureToggle(
    val navn: String,
) {
    // Operasjonelle
    KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV("familie-ba-sak.behandling.korreksjon-vedtaksbrev"),
    TEKNISK_VEDLIKEHOLD_HENLEGGELSE("familie-ba-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"),
    TEKNISK_ENDRING("familie-ba-sak.behandling.teknisk-endring"),
    HENT_IDENTER_TIL_PSYS_FRA_INFOTRYGD("familie-ba-sak.hent-identer-til-psys-fra-infotrygd"),
    KAN_KJØRE_AUTOMATISK_VALUTAJUSTERING_FOR_ENKELT_SAK("familie-ba-sak.kan-kjore-autmatisk-valutajustering-behandling-for-enkelt-sak"),
    KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER("familie-ba-sak.kan-opprette-og-endre-sammensatte-kontrollsaker"),

    KAN_OPPRETTE_REVURDERING_MED_ÅRSAK_IVERKSETTE_KA_VEDTAK("familie-ba-sak.kan-opprette-revurdering-med-aarsak-iverksette-ka-vedtak"),

    // NAV-21071 lagt bak toggle og kan evt fjernes på sikt hvis man ikke har trengt å skru den på igjen
    SKAL_OPPRETTE_FREMLEGGSOPPGAVE_EØS_MEDLEM("familie-ba-sak.skalOpprettFremleggsoppgaveDersomEOSMedlem"),

    // NAV-23955
    BYTT_VALUTAJUSTERING_DATO("familie-ba-sak.behandling.valutajustering_dato"),

    // NAV-22995
    SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD("familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd"),
    KJØR_AUTOVEDTAK_OPPDATER_KLASSEKODE_FOR_UTVIDET_BARNETRYGD("familie-ba-sak.kjor-autovedtak-ny-klassekode-for-utvidet-barnetrygd"),
    OPPRETT_AUTOVEDTAK_OPPDATER_KLASSEKODE_FOR_UTVIDET_BARNETRYGD_AUTOMATISK("familie-ba-sak.opprett-autovedtak-ny-klassekode-for-utvidet-barnetrygd-automatisk"),
    AUTOVEDTAK_OPPDATER_KLASSEKODE_FOR_UTVIDET_BARNETRYGD_HØYT_VOLUM("familie-ba-sak.autovedtak-ny-klassekode-for-utvidet-barnetrygd-hoyt-volum"),

    // NAV-23449 - Skrud av/på ny refaktorert logikk for hjemler i brev, skal i teorien produsere det samme resultatet
    BRUK_OMSKRIVING_AV_HJEMLER_I_BREV("familie-ba-sak.bruk_omskriving_av_hjemler_i_brev"),

    // NAV-23733
    BRUK_OVERSTYRING_AV_FOM_SISTE_ANDEL_UTVIDET("familie-ba-sak.bruk-overstyring-av-fom-siste-andel-utvidet"),

    // satsendring
    // Oppretter satsendring-tasker for de som ikke har fått ny task
    SATSENDRING_ENABLET("familie-ba-sak.satsendring-enablet"),

    // kjører satsendring i arbeidstid med lavt eller høyt volum
    SATSENDRING_HØYT_VOLUM("familie-ba-sak.satsendring-hoyt-volum"),

    // Kjører satsendring i utenfor arbeidstid
    SATSENDRING_KVELD("familie-ba-sak.satsendring-kveld"),

    // Kjører satsendring lørdag
    SATSENDRING_LØRDAG("familie-ba-sak.satsendring-lordag"),
}
