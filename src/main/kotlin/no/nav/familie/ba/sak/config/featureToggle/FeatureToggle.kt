package no.nav.familie.ba.sak.config.featureToggle

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
    STONADSSTATISTIKK_FORTSATT_INNVILGET("familie-ba-sak.stonadsstatistikk-fortsatt-innvilget"),

    KAN_OPPRETTE_REVURDERING_MED_ÅRSAK_IVERKSETTE_KA_VEDTAK("familie-ba-sak.kan-opprette-revurdering-med-aarsak-iverksette-ka-vedtak"),
    PREUTFYLLING_VILKÅR("familie-ba-sak.preutfylling-vilkaar"),
    PREUTFYLLING_VILKÅR_LOVLIG_OPPHOLD("familie-ba-sak.preutfylling-lovlig-opphold"),
    PREUTFYLLING_BOR_HOS_SØKER("familie-ba-sak.preutfylling-bor-hos-soker"),

    AUTOMAITSK_REGISTRER_SØKNAD("familie-ba-sak.automatisk-registrer-soknad"),
    PREUTFYLLING_ENDRET_UTBETALING_3ÅR_ELLER_3MND("familie-ba-sak.preutfylling-endret-utbetaling-3aar-eller-3mnd"),

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

    // NAV-25256
    SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN("familie-ba-sak.skjermet-barn"),

    // NAV-25472
    MINSIDE_AKTIVERING("familie-ba-sak.minside-aktivering"),

    SKAL_INKLUDERE_ÅRSAK_ENDRE_MOTTAKER_I_INITIELL_GENERERING_AV_ANDELER("familie-ba-sak.skal-inkludere-aarsak-endre-mottaker-i-initiell-generering-av-andeler"),

    SKAL_SPLITTE_ENDRET_UTBETALING_ANDELER("familie-ba-sak.skal-splitte-endret-utbetaling-andeler"),

    // NAV-25543
    SKAL_BRUKE_NYTT_FELT_I_EØS_BEGRUNNELSE_DATA_MED_KOMPETANSE("familie-ba-sak.skal-bruke-nytt-felt-i-eos-begrunnelse-data-med-kompetanse"),

    // NAV-
    SKAL_GENERERE_FINNMARKSTILLEGG("familie-ba-sak.andel-generering-finnmark-nord-troms"),

    AUTOMATISK_KJØRING_AV_AUTOVEDTAK_FINNMARKSTILLEGG("familie-ba-sak.kjoering-autovedtak-finnmarkstillegg"),

    KAN_KJØRE_AUTOVEDTAK_FINNMARKSTILLEGG("familie-ba-sak.kan-kjoere-autovedtak-finnmarkstillegg"),

    // NAV-26038
    BRUK_NY_LOGIKK_FOR_AA_FINNE_ENHET_FOR_OPPRETTING_AV_KLAGEBEHANDLING("familie-ba-sak.bruk-ny-logikk-for-aa-finne-enhet-for-oppretting-av-klagebehandling"),

    VALIDER_ENDRING_AV_PREUTFYLTE_VILKÅR("familie-ba-sak.valider-endring-av-preutfylte-vilkaar"),
}
