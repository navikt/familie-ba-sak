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

    KAN_OPPRETTE_REVURDERING_MED_ÅRSAK_IVERKSETTE_KA_VEDTAK("familie-ba-sak.kan-opprette-revurdering-med-aarsak-iverksette-ka-vedtak"),
    PREUTFYLLING_VILKÅR("familie-ba-sak.preutfylling-vilkaar"),
    AUTOMAITSK_REGISTRER_SØKNAD("familie-ba-sak.automatisk-registrer-soknad"),

    // NAV-21071 lagt bak toggle og kan evt fjernes på sikt hvis man ikke har trengt å skru den på igjen
    SKAL_OPPRETTE_FREMLEGGSOPPGAVE_EØS_MEDLEM("familie-ba-sak.skalOpprettFremleggsoppgaveDersomEOSMedlem"),

    // NAV-24387
    BRUK_UTBETALINGSTIDSLINJER_VED_GENERERING_AV_PERIODER_TIL_AVSTEMMING("familie-ba-sak.bruk-utbetalingstidslinjer-ved-generering-av-perioder-til-avstemming"),
    SKAL_FINNE_OG_PATCHE_ANDELER_I_FAGAKER_MED_AVVIK("familie-ba-sak.skal-finne-og-patche-andeler-i-fagsaker-med-avvik"),

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

    // Tillatter behandling av klage
    BEHANDLE_KLAGE("familie-ba-sak.klage"),

    SKAL_BRUKE_NY_DIFFERANSEBEREGNING("familie-ba-sak.skal-bruke-ny-differanseberegning"),

    // NAV-25256
    SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN("familie-ba-sak.skjermet-barn"),

    // NAV-25329
    BRUK_NY_OPPRETT_FAGSAK_MODAL("familie-ba-sak.bruk.ny.opprett.fagsak.modal"),
}
