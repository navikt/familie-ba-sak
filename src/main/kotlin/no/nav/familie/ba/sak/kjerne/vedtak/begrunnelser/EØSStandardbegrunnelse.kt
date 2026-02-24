package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import com.fasterxml.jackson.annotation.JsonValue

enum class EØSStandardbegrunnelse : IVedtakBegrunnelse {
    INNVILGET_PRIMÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "innvilgetPrimarlandUKStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBarnetBorINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_UK_BARNETRYGD_ALLEREDEUTBETALT {
        override val sanityApiNavn = "innvilgetPrimarlandUkBarnetrygdAlleredeUtbetalt"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBeggeForeldreBosattINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "innvilgetPrimarlandUKOgUtlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_SÆRKULLSBARN_ANDRE_BARN_OVERTATT_ANSVAR {
        override val sanityApiNavn = "innvilgetPrimarlandSaerkullsbarnAndreBarnOvertattAnsvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_UK_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandUkToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_STANDARD {
        override val sanityApiNavn = "innvilgetPrimarlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_UK_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetPrimarlandUKAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSBEGRUNNELSE_UTBETALING_TIL_ANNEN_FORELDER {
        override val sanityApiNavn = "innvilgetTilleggsbegrunnelseUtbetalingTilAnnenForelder"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_BARNET_FLYTTET_TIL_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBarnetFlyttetTilNorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_JOBBER_I_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBeggeForeldreJobberINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_TO_ARBEIDSLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandToArbeidslandAnnetLandUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_SÆRKULLSBARN_ANDRE_BARN {
        override val sanityApiNavn = "innvilgetPrimarlandSarkullsbarnAndreBarn"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_UK_TO_ARBEIDSLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandUkToArbeidslandAnnetLandUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetPrimarlandAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_UNNTAK_ANNEN_FORELDER_IKKE_AKTUELL {
        override val sanityApiNavn = "innvilgetPrimarlandUnntakAnnenForelderIkkeAktuell"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_STANDARD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetSekundaerlandStandard"
    },
    INNVILGET_SEKUNDÆRLAND_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetSekundaerlandAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_NULLUTBETALING {
        override val sanityApiNavn = "innvilgetTilleggstekstNullutbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_UTBETALINGSTABELL {
        override val sanityApiNavn = "innvilgetTilleggstekstUtbetalingstabell"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "innvilgetSekundaerlandUkStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_UK_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetSekundaerlandUkAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "innvilgetSekundaerlandUkOgUtlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetSekundaerlandToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_UK_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetSekundaerlandUkToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_BEGGE_FORELDRE_INAKTIVE_TILBAKE_I_TID {
        override val sanityApiNavn = "innvilgetSekundaerlandBeggeForeldreInaktiveTilbakeITid"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_TO_ARBEIDSLAND_INGEN_UTBETALING {
        override val sanityApiNavn = "innvilgetSekundaerlandToArbeidslandIngenUtbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SATSENDRING {
        override val sanityApiNavn = "innvilgetTilleggstekstSatsendring"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_VALUTAJUSTERING {
        override val sanityApiNavn = "innvilgetTilleggstekstValutajustering"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SATSENDRING_OG_VALUTAJUSTERING {
        override val sanityApiNavn = "innvilgetTilleggstekstSatsendringOgValutajustering"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_DELT_BOSTED_ANNEN_FORELDER_IKKE_SØKT {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundaerDeltBostedAnnenForelderIkkeSoekt"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_TILLEGGSTEKST_VEDTAK_FØR_SED {
        override val sanityApiNavn = "innvilgetPrimaerlandTilleggstekstVedtakFoerSed"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_NORSK_ARBEIDSGIVER_I_EØS_LAND {
        override val sanityApiNavn = "innvilgetPrimaerlandBeggeForeldreNorskArbeidsgiverIEosLand"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_ANNEN_FORELDER_UTSENDT_ARBEIDSTAKER_TIL_NORGE {
        override val sanityApiNavn = "innvilgetPrimaerlandAnnenForelderUtsendtArbeidstakerTilNorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_FÅR_YTELSE_I_UTLANDET {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimaerlandFaarYtelseIUtlandet"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_TO_PRIMÆRLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetSelvstendigRettToPrimaerlandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_TO_PRIMÆRLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "innvilgetSelvstendigRettToPrimaerlandAnnetLandUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_FÅR_YTELSE_I_UTLANDET {
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundaerlandFaarYtelseIUtlandet"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_TO_ARBEIDSLAND_MER_ENN_25_PROSENT_I_NORGE {
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggstekstToArbeidslandMerEnn25INorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE {
        override val sanityApiNavn = "innvilgetSekundaerlandBeggeForeldreBosattINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SEKUNDÆRLAND_YTELSE_FRA_NAV_SAMTIDIG_SOM_JOBBER_I_EØS_LAND {
        override val sanityApiNavn = "innvilgetSekundaerlandYtelseFraNavSamtidigSomJobberIEosLand"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSTEKST_PRIMÆR_DELT_BOSTED_ANNEN_FORELDER_IKKE_RETT {
        override val sanityApiNavn = "innvilgetTilleggstekstPrimaerDeltBostedAnnenForelderIkkeRett"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_FULL_UTBETALING {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundaerFullUtbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_AVTALE_DELT_BOSTED {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundaerAvtaleDeltBosted"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_DELT_BOSTED_ANNEN_FORELDER_IKKE_RETT {
        override val sanityApiNavn = "innvilgetTilleggstekstsekundaerDeltBostedAnnenForelderIkkeRett"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_GYLDIG_KONTONUMMER_REGISTRERT_EØS {
        override val sanityApiNavn = "innvilgetGyldigKontonummerRegistrertEos"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_IKKE_FÅTT_SVAR_PÅ_SED {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundaerIkkeFaattSvarPaaSed"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGESTEKST_UK_FULL_ETTERBETALING {
        override val sanityApiNavn = "innvilgetTilleggestekstUkFullEtterbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_UK_MIDLERTIDIG_DIFFERANSEUTBETALING {
        override val sanityApiNavn = "innvilgetUkMidlertidigDifferanseutbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_UK_SØKER_HAR_NORSK_ARBEIDSGIVER_I_STORBRITANNIA {
        override val sanityApiNavn = "innvilgetUkSokerHarNorskArbeidsgiverIStorbritannia"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_DEN_ANDRE_FORELDEREN_UTSENDT_ARBEIDSTAKER {
        override val sanityApiNavn = "innvilgetPrimaerlandDenAndreForelderenUtsendtArbeidstaker"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_UTSENDT_ARBEIDSTAKER_BARN_MEDLEM_FOLKETRYGDEN {
        override val sanityApiNavn = "innvilgetPrimarlandUtsendtArbeidstakerBarnMedlemFolketrygden"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_SØKER_NORSK_ARBEIDSGIVER_I_EØS_LAND {
        override val sanityApiNavn = "innvilgetPrimarlandSokerNorskArbeidsgiverIEosLand"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_OPPHOLD_ANNET_EØS_LAND_NORGE_LOVVALGSLAND {
        override val sanityApiNavn = "innvilgetPrimarlandOppholdAnnetEosLandNorgeLovvalgsland"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_ANNEN_FORELDER_NORSK_ARBEIDSGIVER_I_EØS_LAND {
        override val sanityApiNavn = "innvilgetPrimarlandAnnenForelderNorskArbeidsgiverIEosLand"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_SELVSTENDIG_RETT_ANNEN_FORELDER_NORSK_ARBEIDSGIVER_EØS {
        override val sanityApiNavn = "innvilgetSekundarlandSelvstendigRettAnnenForelderNorskArbeidsgiverEos"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_HELE_FAMILIEN_BOSATT_UTENFOR_NORGE {
        override val sanityApiNavn = "innvilgetSekundaerlandHeleFamilienBosattUtenforNorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_SØKER_HAR_NORSK_ARBEIDSGIVER_I_EØS_LAND {
        override val sanityApiNavn = "innvilgetSekundarSokerHarNorskArbeidsgiverIEosLand"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_SØKER_HAR_NORSK_ARBEIDSGIVER_I_STORBRITANNIA {
        override val sanityApiNavn = "innvilgetSekundarSokerHarNorskArbeidsgiverIStorbritannia"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimaerlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimaerlandUkStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UK_OG_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimaerlandUkOgStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UTSENDT_ARBEIDSTAKER {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimaerlandUtsendtArbeidstaker"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundaerlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundaerlandUkStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundaerlandUkOgUtlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_VEDTAK_FØR_SED {
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggstekstVedtakForSed"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_SEKUNDÆR_FULL_UTBETALING {
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggstekstSekundaerFullUtbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_NULLUTBETALING {
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggstekstNullutbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_SEKUNDÆR_IKKE_FÅTT_SVAR_PÅ_SED {
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggstekstSekundaerIkkeFaattSvarPaaSed"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_SØKER_BOR_I_NORGE_UNNTATT_MEDLEMSKAP {
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundaerlandSokerBorINorgeUnntattMedlemskap"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_NASJONAL_RETT_SEKUNDÆRLAND_STANDARD_BOSMANN {
        override val sanityApiNavn = "innvilgetNasjonalRettsekundarlandStandardBosmann"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_NASJONAL_RETT_SEKUNDÆRLAND_TO_ARBEIDSLAND {
        override val sanityApiNavn = "innvilgetNasjonalRettSekundarlandToArbeidsland"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_NASJONAL_RETT_SEKUNDÆRLAND_SØKER_FÅR_PENGER_SOM_ERSTATTER_LØNN {
        override val sanityApiNavn = "innvilgetNasjonalRettSekundarSokerFarPengerSomErstatterLonn"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_NASJONAL_RETT_SEKUNDÆRLAND_SØKER_FÅR_PENSJON_FRA_ANNET_LAND {
        override val sanityApiNavn = "innvilgetNasjonalRettSekundarSokerFarPensjonFraAnnetLand"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstDeltBosted"
    },
    INNVILGET_TILLEGGSTEKST_FULL_BARNETRYGD_HAR_AVTALE_DELT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstFullBarnetrygdHarAvtaleDelt"
    },
    INNVILGET_TILLEGSTEKST_TO_ARBEIDSLAND_MER_ENN_25_PROSENT_ARBEID_I_NORGE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstToArbeidslandMerEnn25ProsentArbeidINorge"
    },
    INNVILGET_TILLEGSTEKST_TO_ARBEIDSLAND_SEKUNDÆRLAND_FULL_UTBETALING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstToArbeidslandSekundarlandFullUtbetaling"
    },
    INNVILGET_TILLEGGSTEKST_TO_ARBEIDSLAND_SEKUNDÆRLAND_FÅR_IKKE_BA_I_ANNET_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstToArbeidslandSekundarlandFaarIkkeBaIAnnetLand"
    },
    INNVILGET_TILLEGGSTEKST_TO_ARBEIDSLAND_SEKUNDÆRLAND_NULL_UTBETALING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstToArbeidslandSekundarlandNullUtbetaling"
    },
    INNVILGET_TILLEGSTEKST_OVERTATT_ANSVAR_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstOvertattAnsvarForBarn"
    },
    INNVILGET_MIDLERTIDIG_DIFFERANSEUTBETALING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetMidlertidigDifferanseutbetaling"
    },
    INNVILGET_TILLEGSTEKST_FULL_UTBETALING_FÅR_IKKE_FRA_UK {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstFullUtbetalingFaarIkkeFraUk"
    },
    INNVILGET_PRIMÆRLAND_ALENEFORELDER_OPPHOLD_ANNET_EØS_LAND_NORGE_LOVVALG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetPrimarlandAleneforelderOppholdAnnetEosLandNorgeLovvalg"
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_FÅR_IKKE_BA_I_ANNET_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstSekundarFaarIkkeBaIAnnetLand"
    },
    INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_SEKUNDÆR_FÅR_IKKE_BA_I_ANNET_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggstekstSekundarFaarIkkeBaIAnnetLand"
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_INFO_OM_MULIG_REFUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetTilleggstekstSekundarInfoOmMuligRefusjon"
    },
    INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_SEKUNDÆR_INFO_OM_MULIG_REFUSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggstekstSekundarInfoOmMuligRefusjon"
    },
    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆR_ANNEN_FORELDER_BOR_I_NORGE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundaerAnnenForelderBorINorge"
    },
    INNVILGET_NASJONAL_RETT_SEKUNDÆR_TO_ARBEIDSLAND_SØKER_PENGER_SOM_ERSTATTER_LØNN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetNasjonalRettSekundaerToArbeidslandSokerPengerSomErstatterLonn"
    },
    INNVILGET_SEKUNDÆR_TO_ARBEIDSLAND_SØKER_FÅR_PENSJON_FRA_ANNET_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetSekundaerToArbeidslandSokerFaarPensjonFraAnnetLand"
    },
    INNVILGET_NASJONAL_RETT_SEKUNDÆRLAND_ALENEANSVAR_ARBEID_ANNET_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetNasjonalRettSekundarlandAleneansvarArbeidAnnetLand"
    },
    INNVILGET_NASJONAL_RETT_SEKUNDÆR_ALENEANSVAR_FÅR_PENGER_SOM_ERSTATTER_LØNN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetNasjonalRettSekundarAleneansvarFaarPengerSomErstatterLonn"
    },
    INNVILGET_NASJONAL_RETT_SEKUNDÆR_ALENEANSVAR_FÅR_PENSJON_FRA_ANNET_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetNasjonalRettSekundarAleneansvarFaarPensjonFraAnnetLand"
    },
    OPPHØR_EØS_STANDARD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorEosStandard"
    },
    OPPHØR_EØS_SØKER_BER_OM_OPPHØR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorEosSokerBerOmOpphor"
    },
    OPPHØR_BARN_BOR_IKKE_I_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorBorIkkeIEtEOSland"
    },
    OPPHØR_IKKE_STATSBORGER_I_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorIkkeStatsborgerIEosLand"
    },
    OPPHØR_SENTRUM_FOR_LIVSINTERESSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSentrumForLivsinteresse"
    },
    OPPHØR_IKKE_ANSVAR_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorIkkeAnsvarForBarn"
    },
    OPPHØR_IKKE_OPPHOLDSRETT_SOM_FAMILIEMEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorIkkeOppholdsrettSomFamiliemedlem"
    },
    OPPHØR_SEPARASJONSAVTALEN_GJELDER_IKKE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSeparasjonsavtaleGjelderIkke"
    },
    OPPHØR_SØKER_OG_BARN_BOR_IKKE_I_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSoekerOgBarnBorIkkeIEosLand"
    },
    OPPHØR_SØKER_BOR_IKKE_I_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSoekerBorIkkeIEosLand"
    },
    OPPHØR_ARBEIDER_MER_ENN_25_PROSENT_I_ANNET_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorArbeiderMerEnn25ProsentIAnnetEosLand"
    },
    OPPHØR_UTSENDT_ARBEIDSTAKER_FRA_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorUtsendtArbeidstakerFraEosLand"
    },
    OPPHOR_UGYLDIG_KONTONUMMER_EØS {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorUgyldigKontonummerEos"
    },
    OPPHOR_ETT_BARN_DØD_EØS {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorEttBarnDodEos"
    },
    OPPHOR_FLERE_BARN_DØDE_EØS {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorFlereBarnErDodeEos"
    },
    OPPHØR_SELVSTENDIG_RETT_OPPHØR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSelvstendigRettOpphoer"
    },
    OPPHØR_SELVSTENDIG_RETT_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSelvstendigRettUtsendtArbedstakerFraAnnetEosLand"
    },
    OPPHØR_DELT_BOSTED_BEGGE_FORELDRE_IKKE_OMFATTET_NORSK_LOVGIVNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorDeltBostedBeggeForeldreIkkeOmfattetNorskLovgivning"
    },
    OPPHØR_SELVSTENDIG_RETT_BARN_FLYTTET_FRA_SØKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSelvstendigRettBarnFlyttetFraSoker"
    },
    OPPHOR_SELVSTENDIG_RETT_OPPHOR_FRA_START {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSelvstendigRettOpphorFraStart"
    },
    OPPHOR_SELVSTENDIG_RETT_VAR_IKKE_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EOS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSelvstendigRettVarIkkeUtsendtArbeidstakerFraAnnetEosLand"
    },
    OPPHOR_BARN_BODDE_IKKE_I_ET_EOS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorBarnBoddeIkkeIEtEosLand"
    },
    OPPHOR_SEPARASJONSAVTALEN_GJALDT_IKKE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSeparasjonsavtalenGjaldtIkke"
    },
    OPPHOR_NORGE_VAR_IKKE_SENTRUM_FOR_LIVSINTERESSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorNorgeVarIkkeSentrumForLivsinteresse"
    },
    OPPHOR_HADDE_IKKE_ANSVAR_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorHaddeIkkeAnsvarForBarn"
    },
    OPPHOR_HADDE_IKKE_OPPHOLDSRETT_SOM_FAMILIEMEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorHaddeIkkeOppholdsrettSomFamiliemedlem"
    },
    OPPHOR_SOKER_OG_BARN_BODDE_IKKE_I_EOS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSokerOgBarnBoddeIkkeIEosLand"
    },
    OPPHOR_SOKER_BODDE_IKKE_I_EOS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSokerBoddeIkkeIEosLand"
    },
    OPPHØR_SEKUNDÆRLAND_INGEN_AV_FORELDRENE_JOBBER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorSekundarlandIngenAvForeldreneJobber"
    },
    OPPHØR_ISLAND_AVVENTE_UTBETALING_TIL_SKATTEÅRET_ER_OVER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorIslandAvventeUtbetalingTilSkatteaaretErOver"
    },
    AVSLAG_EØS_IKKE_EØS_BORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosIkkeEosBorger"
    },
    AVSLAG_EØS_IKKE_BOSATT_I_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosIkkeBosattIEosLand"
    },
    AVSLAG_EØS_JOBBER_IKKE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosJobberIkke"
    },
    AVSLAG_EØS_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosUtsendtArbeidstakerFraAnnetEosLand"
    },
    AVSLAG_EØS_ARBEIDER_MER_ENN_25_PROSENT_I_ANNET_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosArbeiderMerEnn25ProsentIAnnetEosLand"
    },
    AVSLAG_ARBEIDER_I_ANNET_EOS_LAND_FÅR_PENSJON_FRA_NORGE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagArbeiderIAnnetEosLandFaarPensjonFraNorge"
    },
    AVSLAG_EØS_KUN_KORTE_USAMMENHENGENDE_ARBEIDSPERIODER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosKunKorteUsammenhengendeArbeidsperioder"
    },
    AVSLAG_EØS_IKKE_PENGER_FRA_NAV_SOM_ERSTATTER_LØNN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosIkkePengerFraNavSomErstatterLoenn"
    },
    AVSLAG_EØS_SEPARASJONSAVTALEN_GJELDER_IKKE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosSeparasjonsavtalenGjelderIkke"
    },
    AVSLAG_EØS_IKKE_LOVLIG_OPPHOLD_SOM_EØS_BORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosIkkeLovligOppholdSomEosBorger"
    },
    AVSLAG_EØS_IKKE_OPPHOLDSRETT_SOM_FAMILIEMEDLEM_AV_EØS_BORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosIkkeOppholdsrettSomFamiliemedlemAvEosBorger"
    },
    AVSLAG_EØS_IKKE_STUDENT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosIkkeStudent"
    },
    AVSLAG_EØS_IKKE_ANSVAR_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosIkkeAnsvarForBarn"
    },
    AVSLAG_EØS_VURDERING_IKKE_ANSVAR_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosVurderingIkkeAnsvarForBarn"
    },
    AVSLAG_FAAR_DAGPENGER_FRA_ANNET_EOS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagFaarDagpengerFraAnnetEosLand"
    },
    AVSLAG_SELVSTENDIG_NAERINGSDRIVENDE_NORGE_ARBEIDSTAKER_I_ANNET_EOS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagSelvstendigNaeringsdrivendeNorgeArbeidstakerIAnnetEosLand"
    },
    AVSLAG_EØS_UREGISTRERT_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagEosUregistrertBarn"
    },
    AVSLAG_SELVSTENDIG_RETT_STANDARD_AVSLAG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagSelvstendigRettStandardAvslag"
    },
    AVSLAG_SELVSTENDIG_RETT_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagSelvstendigRettUtsendtArbeidstakerFraAnnetEosLand"
    },
    AVSLAG_SELVSTENDIG_RETT_BOR_IKKE_FAST_MED_BARNET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagSelvstendigRettBorIkkeFastMedBarnet"
    },
    AVSLAG_SELVSTENDIG_RETT_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagSelvstendigRettForeldreneBorSammen"
    },
    AVSLAG_SEKUNDÆRLAND_INGEN_AV_FORELDRENE_JOBBER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagSekundaerlandIngenAvForeldreneJobber"
    },
    AVSLAG_DELT_BOSTED_BEGGE_FORELDRE_IKKE_OMFATTET_NORSK_LOVVALG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_AVSLAG
        override val sanityApiNavn = "avslagDeltBostedBeggeForeldreIkkeOmfattetNorskLovvalg"
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_ALENEANSVAR {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandBeggeForeldreBosattINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_JOBBER_I_NORGE {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandBeggeForeldreJobberINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandUkStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_UK_ALENEANSVAR {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandUkAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandUkOgUtlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandBarnetBorINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_SÆRKULLSBARN_ANDRE_BARN_OVERTATT_ANSVAR {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandSaerkullsbarnAndreBarn"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_TO_ARBEIDSLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandToArbeidslandAnnetLandUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_UK_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandUkToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_PRIMÆRLAND_UK_TO_ARBEIDSLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetPrimaerlandUkToArbeidslandAnnetLandUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_FÅR_YTELSE_I_UTLANDET {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettPrimaerlandFaarYtelseIUtlandet"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_TILLEGGSBEGRUNNELSE_UTBETALING_TIL_ANNEN_FORELDER {
        override val sanityApiNavn = "fortsattInnvilgetTilleggsbegrunnelseUtbetalingTilAnnenForelder"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSETT_INNVILGET_PRIMÆRLAND_TILLEGGSTEKST_VEDTAK_FØR_SED {
        override val sanityApiNavn = "fortsattInnvilgetTilleggsbegrunnelseVedtakForSed"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_UK_PRIMÆR_SØKER_HAR_NORSK_ARBEIDSGIVER_I_STORBRITANNIA {
        override val sanityApiNavn = "fortsattInnvilgetUkPrimarSokerHarNorskArbeidsgiverIStorbritannia"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSETT_INNVILGET_SEKUNDÆRLAND_STANDARD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandStandard"
    },
    FORTSETT_INNVILGET_TILLEGGSTEKST_NULLUTBETALING {
        override val sanityApiNavn = "fortsattInnvilgetTilleggstekstNullutbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSETT_INNVILGET_SEKUNDÆRLAND_ALENEANSVAR {
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSETT_INNVILGET_SEKUNDÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandUkStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_FÅR_YTELSE_I_UTLANDET {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettSekundaerlandFaarYtelseIUtlandet"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },

    FORTSETT_INNVILGET_SEKUNDÆRLAND_UK_ALENEANSVAR {
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandUkAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SEKUNDÆR_SØKER_HAR_NORSK_ARBEIDSGIVER_I_EØS_LAND {
        override val sanityApiNavn = "fortsattInnvilgetSekundarSokerHarNorskArbeidsgiverIEosLand"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SEKUNDÆR_FÅR_IKKE_BA_I_ANNET_LAND {
        override val sanityApiNavn = "fortsattInnvilgetSekundarFaarIkkeBaIAnnetLand"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_UK_SEKUNDÆR_SØKER_HAR_NORSK_ARBEIDSGIVER_I_STORBRITANNIA {
        override val sanityApiNavn = "fortsattInnvilgetUkSekundarSokerHarNorskArbeidsgiverIStorbritannia"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSETT_INNVILGET_SEKUNDÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandUkOgUtland"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSETT_INNVILGET_SEKUNDÆRLAND_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSETT_INNVILGET_SEKUNDÆRLAND_UK_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandUkToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SEKUNDÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE {
        override val sanityApiNavn = "fortsattInnvilgetSekundaerlandBeggeForeldreBosattINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_TILLEGGSTEKST_SEKUNDÆR_FULL_UTBETALING {
        override val sanityApiNavn = "fortsattInnvilgetTilleggstekstSekundaerFullUtbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_TILLEGGSTEKST_SEKUNDÆR_IKKE_FÅTT_SVAR_PÅ_SED {
        override val sanityApiNavn = "fortsattInnvilgetTilleggsteksterSekundaerIkkeFaattSvarPaaSed"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_TILLEGGSTEKST_UTBETALINGSTABELL {
        override val sanityApiNavn = "fortsattInnvilgetTilleggstekstUtbetalingstabell"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_TILLEGSTEKST_UK_FULL_UTBETALING {
        override val sanityApiNavn = "fortsattInnvilgetTilleggstekstUkFullUtbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_TILLEGGSTEKST_SELVSTENDIG_RETT_SEKUNDÆR_FULL_UTBETALING {
        override val sanityApiNavn = "fortsattInnvilgetTilleggstekstSelvstendigRettSekundaerFullUtbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettPrimaerlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettPrimaerlandUkStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettPrimaerlandUkOgUtlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettSekundaerlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettSekundaerlandUkStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDAERLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettSekundaerlandUkOgUtlandStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_NULLUTBETALING {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettTilleggstekstNullutbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    FORTSATT_INNVILGET_SELVSTENDIG_RETT_TILLEGSTEKST_VEDTAK_FOR_SED {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettTilleggstekstVedtakForSed"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET
    },
    REDUKSJON_BARN_DØD_EØS {
        override val sanityApiNavn = "reduksjonBarnDoedEos"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
    },
    REDUKSJON_SØKER_BER_OM_OPPHØR_EØS {
        override val sanityApiNavn = "reduksjonSokerBerOmOpphoer"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
    },
    REDUKSJON_BARN_BOR_IKKE_I_EØS {
        override val sanityApiNavn = "reduksjonBarnBorIkkeIEosLand"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
    },
    REDUKSJON_IKKE_ANSVAR_FOR_BARN {
        override val sanityApiNavn = "reduksjonIkkeAnsvarForBarn"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
    },
    REDUKSJON_TILLEGGSTEKST_VALUTAJUSTERING {
        override val sanityApiNavn = "reduksjonTilleggstekstValutajustering"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
    },
    REDUKSJON_TILLEGGSTEKST_NULLUTBETALING {
        override val sanityApiNavn = "reduksjonTilleggstekstNullutbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
    },
    REDUKSJON_TILLEGGSTEKST_UTBETALINGSTABELL {
        override val sanityApiNavn = "reduksjonTilleggstekstUtbetalingstabell"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
    },
    REDUKSJON_UK_MIDLERTIDIG_DIFFERANSEUTBETALING {
        override val sanityApiNavn = "reduksjonUkMidlertidigDifferanseutbetaling"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
    },
    REDUKSJON_DELT_BOSTED_BEGGE_FORELDRE_IKKE_OMFATTET_NORSK_LOVVALG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
        override val sanityApiNavn = "reduksjonDeltBostedBeggeForeldreIkkeOmfattetNorskLovvalg"
    },
    REDUKSJON_SELVSTENDIG_RETT_BARN_FLYTTET_FRA_SØKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
        override val sanityApiNavn = "reduksjonSelvstendigRettBarnFlyttetFraSoker"
    },
    REDUKSJON_SELVSTENDIG_RETT_TILLEGGSTEKST_NULLUTBETALING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
        override val sanityApiNavn = "reduksjonSelvstendigRettTilleggstekstNullutbetaling"
    },
    REDUKSJON_SELVSTENDIG_RETT_NORSKE_REGLER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
        override val sanityApiNavn = "reduksjonSelvstendigRettNorskeRegler"
    },
    REDUKSJON_SENTRUM_FOR_LIVSINTERESSE_SÆRKULLSBARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_REDUKSJON
        override val sanityApiNavn = "reduksjonSentrumForLivsinteresseSaerkullsbarn"
    },
    ENDRET_UTBETALING_ETTERBETALING_UTVIDET_EØS_INGEN_UTBETALING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingEtterbetalingUtvidetEosIngenUtbetaling"
    },
    ENDRET_UTBETALING_ETTERBETALING_UTVIDET_SELVSTENDIG_RETT_EØS_INGEN_UTBETALING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingEtterbetalingUtvidetSelvstendigRettEosIngenUtbetaling"
    },
    ;

    override val kanDelesOpp: Boolean = false

    @JsonValue
    override fun enumnavnTilString(): String = EØSStandardbegrunnelse::class.simpleName + "$" + this.name
}
