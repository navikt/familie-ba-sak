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

    KAN_OPPRETTE_REVURDERING_MED_ÅRSAK_IVERKSETTE_KA_VEDTAK("familie-ba-sak.kan-opprette-revurdering-med-aarsak-iverksette-ka-vedtak"),
    PREUTFYLLING_VILKÅR("familie-ba-sak.preutfylling-vilkaar"),
    PREUTFYLLING_VILKÅR_LOVLIG_OPPHOLD("familie-ba-sak.preutfylling-lovlig-opphold"),
    PREUTFYLLING_BOR_HOS_SØKER("familie-ba-sak.preutfylling-bor-hos-soker"),
    PREUTFYLLING_PERSONOPPLYSNIGSGRUNNLAG("familie-ba-sak.preutfylling-personopplysningsgrunnlag"),
    ARBEIDSFORHOLD_STRENGERE_NEDHENTING("familie-ba-sak.arbeidsforhold-strengere-nedhenting"),

    AUTOMAITSK_REGISTRER_SØKNAD("familie-ba-sak.automatisk-registrer-soknad"),

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

    // NAV-25256
    SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN("familie-ba-sak.skjermet-barn"),

    VALIDER_ENDRING_AV_PREUTFYLTE_VILKÅR("familie-ba-sak.valider-endring-av-preutfylte-vilkaar"),

    // Toggle for å kunne skru av tungtkjørende statistikk for grafana når man f.eks. kjører autovedtak som satsendring/finnmarkstillegg
    TUNGTKJØRENDE_GRAFANA_STATISTIKK("familie-ba-sak.generer-grafana-statistikk"),

    JOURNALFOER_MANUELT_BREV_I_TASK("familie-ba-sak.journalfoer-manuelt-brev-i-task"),

    FILTRER_ADRESSE_FOR_SØKER_PÅ_ELDSTE_BARNS_FØDSELSDATO("familie-ba-sak.filtrer-adresse-for-soker-paa-eldste-barns-fodselsdato"),

    FILTRER_STATSBORGERSKAP_PÅ_ELDSTE_BARNS_FØDSELSDATO("familie-ba-sak.filtrer-statsborgerskap-paa-eldste-barns-fodselsdato"),

    FILTRER_SIVILSTAND_FOR_SØKER_PÅ_ELDSTE_BARNS_FØDSELSDATO("familie-ba-sak.filtrer-sivilstand-for-soker-paa-eldste-barns-fodselsdato"),

    FILTRER_OPPHOLD_PÅ_ELDSTE_BARNS_FØDSELSDATO("familie-ba-sak.filtrer-opphold-paa-eldste-barns-fodselsdato"),

    SKAL_HÅNDTERE_FALSK_IDENTITET("familie-ba-sak.skal-handtere-falsk-identitet"),

    HENT_ARBEIDSFORDELING_MED_BEHANDLINGSTYPE("familie-ba-sak.hent-arbeidsfordeling-med-behandlingstype"),

    SKAL_HENTE_UTBETALINGSLAND_FRA_UTENLANDSKPERIODEBELØP("familie-ba-sak.skal-hente-utbetalingsland-fra-utenlandskperiodebelop"),

    // NAV-27369
    SKAL_KUNNE_BEHANDLE_BA_INSTITUSJONSFAGSAKER_I_KLAGE("familie-klage.skal-kunne-behandle-ba-institusjon-fagsaker"),
}
