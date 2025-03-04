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
    SJEKK_AKTIV_INFOTRYGD_SAK_REPLIKA("familie-ba-sak.infotrygd-replika-sak-aktiv"),
    KAN_BEHANDLE_SAKER_SOM_BRUKER_ULOVFESTET_MOTREGNING("familie-ba-sak.kan-behandle-saker-som-bruker-ulovfestet-motregning"),

    KAN_OPPRETTE_REVURDERING_MED_ÅRSAK_IVERKSETTE_KA_VEDTAK("familie-ba-sak.kan-opprette-revurdering-med-aarsak-iverksette-ka-vedtak"),

    // NAV-21071 lagt bak toggle og kan evt fjernes på sikt hvis man ikke har trengt å skru den på igjen
    SKAL_OPPRETTE_FREMLEGGSOPPGAVE_EØS_MEDLEM("familie-ba-sak.skalOpprettFremleggsoppgaveDersomEOSMedlem"),

    // NAV-23955
    BYTT_VALUTAJUSTERING_DATO("familie-ba-sak.behandling.valutajustering_dato"),

    // NAV-23733
    BRUK_OVERSTYRING_AV_FOM_SISTE_ANDEL_UTVIDET("familie-ba-sak.bruk-overstyring-av-fom-siste-andel-utvidet"),

    // NAV-22696
    SKAL_BRUKE_NY_VERSJON_AV_OPPDATERING_AV_ANDELER_MED_ENDRINGER("familie-ba-sak.ny-versjon-oppdater-andeler-med-endringer"),

    // NAV-24034
    BRUK_NY_SAKSBEHANDLER_NAVN_FORMAT_I_SIGNATUR("familie-ba-sak.bruk-ny-saksbehandler-navn-format-i-signatur"),

    // NAV-24387
    BRUK_UTBETALINGSTIDSLINJER_VED_GENERERING_AV_PERIODER_TIL_AVSTEMMING("familie-ba-sak.bruk-utbetalingstidslinjer-ved-generering-av-perioder-til-avstemming"),

    // satsendring
    // Oppretter satsendring-tasker for de som ikke har fått ny task
    SATSENDRING_ENABLET("familie-ba-sak.satsendring-enablet"),

    // kjører satsendring i arbeidstid med lavt eller høyt volum
    SATSENDRING_HØYT_VOLUM("familie-ba-sak.satsendring-hoyt-volum"),

    // Kjører satsendring i utenfor arbeidstid
    SATSENDRING_KVELD("familie-ba-sak.satsendring-kveld"),

    // Kjører satsendring lørdag
    SATSENDRING_LØRDAG("familie-ba-sak.satsendring-lordag"),

    BRUK_FUNKSJONALITET_FOR_ULOVFESTET_MOTREGNING("familie-ba-sak.ulovfestet-motregning"),
}
