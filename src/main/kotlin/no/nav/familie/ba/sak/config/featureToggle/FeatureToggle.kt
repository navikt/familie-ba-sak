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

    // Preutfylling
    PREUTFYLLING_VILKÅR("familie-ba-sak.preutfylling-vilkaar"),
    PREUTFYLLING_PERSONOPPLYSNIGSGRUNNLAG("familie-ba-sak.preutfylling-personopplysningsgrunnlag"),
    AUTOMAITSK_REGISTRER_SØKNAD("familie-ba-sak.automatisk-registrer-soknad"),
    FILTRERE_REGISTEROPPLYSNINGER("familie-ba-sak.filtrer-registeropplysninger"),

    // Introdusert AbstractPreutfyllBosattIRiketService og tatt den i bruk i PreutfyllBosattIRiketService og PreutfyllBosattIRiketForFødselshendelseService.
    PREUTFYLLING_BOSATT_I_RIKET_FOR_FØDSELSHENDELSE("familie-ba-sak.preutfylling-bosatt-i-riket-for-fodselshendelse"),

    // Dersom den er på brukes oppdatert PreutfyllBosattIRiketService, men er den av brukes GammelPreutfyllBosattIRiketService som er lik gammel versjon av PreutfyllBosattIRiketService før introduksjon av AbstractPreutfyllBosattIRiketService.
    OPPDATERT_PREUTFYLLING_BOSATT_I_RIKET("familie-ba-sak.oppdatert-preutfylling-bosatt-i-riket"),

    // NAV-25256
    SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN("familie-ba-sak.skjermet-barn"),

    HARDKODET_EEAFREG_STATSBORGERSKAP("familie-ba-sak.hardkodet-eeafreg-statsborgerskap"),

    SKAL_HÅNDTERE_FALSK_IDENTITET("familie-ba-sak.skal-handtere-falsk-identitet"),

    HENT_ARBEIDSFORDELING_MED_BEHANDLINGSTYPE("familie-ba-sak.hent-arbeidsfordeling-med-behandlingstype"),

    // NAV-27369
    SKAL_KUNNE_BEHANDLE_BA_INSTITUSJONSFAGSAKER_I_KLAGE("familie-klage.skal-kunne-behandle-ba-institusjon-fagsaker"),

    KAN_OPPRETTE_SKJERMET_BARN_KLAGE("familie-ba-sak.kan-opprette-skjermet-barn-klage"),
}
