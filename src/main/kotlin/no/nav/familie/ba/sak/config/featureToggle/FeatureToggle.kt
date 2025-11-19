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

    // NAV-
    AUTOMATISK_KJØRING_AV_AUTOVEDTAK_FINNMARKSTILLEGG("familie-ba-sak.kjoering-autovedtak-finnmarkstillegg"),

    AUTOMATISK_KJØRING_AV_AUTOVEDTAK_SVALBARDSTILLEGG("familie-ba-sak.kjoering-autovedtak-svalbardtillegg"),

    KAN_KJØRE_AUTOVEDTAK_FINNMARKSTILLEGG("familie-ba-sak.kan-kjoere-autovedtak-finnmarkstillegg"),

    KAN_KJØRE_AUTOVEDTAK_SVALBARDTILLEGG("familie-ba-sak.kan-kjoere-autovedtak-svalbardtillegg"),

    SKAL_PREUTFYLLE_BOSATT_I_RIKET_I_FØDSELSHENDELSER("familie-ba-sak.skal-preutfylle-bosatt-i-riket-i-foedselshendelse"),

    SKAL_BRUKE_ADRESSEHENDELSELØYPE_FINNMARKSTILLEGG("familie-ba-sak.skal-bruke-adressehendelseloype-for-finnmarkstillegg"),

    SKAL_BRUKE_ADRESSEHENDELSELØYPE_SVALBARDTILLEGG("familie-ba-sak.skal-bruke-adressehendelseloype-for-svalbardtillegg"),

    VALIDER_ENDRING_AV_PREUTFYLTE_VILKÅR("familie-ba-sak.valider-endring-av-preutfylte-vilkaar"),

    OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD("familie-ba-sak.skal-opprette-manuell-oppgave-autokjoring-finnmark-svalbard"),

    // Toggle for å kunne skru av tungtkjørende statistikk for grafana når man f.eks. kjører autovedtak som satsendring/finnmarkstillegg
    TUNGTKJØRENDE_GRAFANA_STATISTIKK("familie-ba-sak.generer-grafana-statistikk"),

    NY_PREUTFYLLING_FOR_BOSATT_I_RIKET_VILKÅR_VED_AUTOVEDTAK_FINNMARK_SVALBARD("familie-ba-sak.ny-preutfylling-for-bosatt-i-riket-vilkaar-ved-autovedtak-finnmark-svalbard"),

    JOURNALFOER_MANUELT_BREV_I_TASK("familie-ba-sak.journalfoer-manuelt-brev-i-task"),

    FILTRER_ADRESSE_FOR_SØKER_PÅ_ELDSTE_BARNS_FØDSELSDATO("familie-ba-sak.filtrer-adresse-for-soker-paa-eldste-barns-fodselsdato"),

    FILTRER_STATSBORGERSKAP_PÅ_ELDSTE_BARNS_FØDSELSDATO("familie-ba-sak.filtrer-statsborgerskap-paa-eldste-barns-fodselsdato"),

    PORTEFØLJEJUSTERING("familie-ba-sak.portefoljejustering"),
}
