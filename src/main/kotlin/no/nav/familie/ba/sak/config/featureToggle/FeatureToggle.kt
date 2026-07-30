package no.nav.familie.ba.sak.config.featureToggle

enum class FeatureToggle(
    val navn: String,
) {
    // Operasjonelle
    TEKNISK_VEDLIKEHOLD_HENLEGGELSE("familie-ba-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"),
    TEKNISK_ENDRING("familie-ba-sak.behandling.teknisk-endring"),
    HENT_IDENTER_TIL_PSYS_FRA_INFOTRYGD("familie-ba-sak.hent-identer-til-psys-fra-infotrygd"),
    KAN_KJØRE_AUTOMATISK_VALUTAJUSTERING_FOR_ENKELT_SAK("familie-ba-sak.kan-kjore-autmatisk-valutajustering-behandling-for-enkelt-sak"),
    KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER("familie-ba-sak.kan-opprette-og-endre-sammensatte-kontrollsaker"),
    SJEKK_AKTIV_INFOTRYGD_SAK_REPLIKA("familie-ba-sak.infotrygd-replika-sak-aktiv"),

    // satsendring
    // Oppretter satsendring-tasker for de som ikke har fått ny task
    SATSENDRING_ENABLET("familie-ba-sak.satsendring-enablet"),

    // kjører satsendring i arbeidstid med lavt eller høyt volum
    SATSENDRING_HØYT_VOLUM("familie-ba-sak.satsendring-hoyt-volum"),

    // Kjører satsendring i utenfor arbeidstid
    SATSENDRING_KVELD("familie-ba-sak.satsendring-kveld"),

    // Kjører satsendring lørdag
    SATSENDRING_LØRDAG("familie-ba-sak.satsendring-lordag"),

    // Populerer antall satsendringer kjørt i grafana. Nyttig å ha på når man kjører satsendring
    SATSENDRING_GRAFANA_STATISTIKK("familie-ba-sak.satsendring-grafana-statistikk"),

    // Toggle for å kunne skru av tungtkjørende statistikk for grafana når man f.eks. kjører autovedtak som satsendring/finnmarkstillegg
    TUNGTKJØRENDE_GRAFANA_STATISTIKK("familie-ba-sak.generer-grafana-statistikk"),

    // Release

    // NAV-25256
    SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN("familie-ba-sak.skjermet-barn"),

    SKAL_HÅNDTERE_FALSK_IDENTITET("familie-ba-sak.skal-handtere-falsk-identitet"),

    HENT_ARBEIDSFORDELING_MED_BEHANDLINGSTYPE("familie-ba-sak.hent-arbeidsfordeling-med-behandlingstype"),

    // NAV-27369
    SKAL_KUNNE_BEHANDLE_BA_INSTITUSJONSFAGSAKER_I_KLAGE("familie-klage.skal-kunne-behandle-ba-institusjon-fagsaker"),

    KAN_OPPRETTE_SKJERMET_BARN_KLAGE("familie-ba-sak.kan-opprette-skjermet-barn-klage"),

    // NAV-28471
    TILLAT_TILGANG_SKJERMET_BARN_UTEN_LØPENDE_ANDELER("familie-ba-sak.tillat-tilgang-skjermet-barn-uten-lopende-andeler"),

    UTLED_AVREGNING_PÅ_TVERS_AV_BEHANDLINGER("familie-ba-sak.utled-avregning-paa-tvers-av-behandlinger"),

    // NAV-28902
    FAGSAKLÅSING_SCHEDULER("familie-ba-sak.fagsaklaasing-scheduler"),
    KAN_LÅSE_FAGSAK("familie-ba-sak.kan-laase-fagsak"),

    // NAV-29028
    PREUTFYLL_VILKÅR_REVURDERING_SØKNAD("familie-ba-sak.preutfyll-vilkar-revurdering-soknad"),

    // NAV-29193
    KAN_GENERERE_BARNAS_VILKÅR("familie-ba-sak.kan-generere-barnas-vilkar"),

    // NAV-25661
    KAN_REGISTRERE_SØKNADSTIDSPUNKT_PÅ_PERSON("familie-ba-sak.kan-registrere-soknadstidspunkt"),

    // NAV-29800
    KAN_KJØRE_SATSENDRING_EØS("familie-ba-sak.kan-kjore-satsendring-eos"),

    // NAV-29936
    SKAL_SLETTE_GAMLE_VEDTAKSBREV_FRA_DB("familie-ba-sak.skal-slette-gamle-vedtaksbrev-fra-db"),

    // NAV-29382
    HENT_VEDTAKSBREV_FRA_JOARK("familie-ba-sak.hent-vedtaksbrev-fra-joark"),
}
