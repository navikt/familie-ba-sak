package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.EØSBegrunnelseMedTriggere

enum class EØSStandardbegrunnelse : IVedtakBegrunnelse {
    INNVILGET_PRIMÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "innvilgetPrimarlandUKStandard"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBarnetBorINorge"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSBEGRUNNELSE_REFUSJON {
        override val sanityApiNavn = "innvilgetTilleggsbegrunnelseRefusjon"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_BARNETRYGD_ALLEREDE_UTBETALT {
        override val sanityApiNavn = "innvilgetPrimarlandBarnetrygdAlleredeUtbetalt"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_PRIMÆRLAND_UK_BARNETRYGD_ALLEREDEUTBETALT {
        override val sanityApiNavn = "innvilgetPrimarlandUkBarnetrygdAlleredeUtbetalt"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSBEGRUNNELSE_REFUSJON_UAVKLART {
        override val sanityApiNavn = "innvilgetTilleggsbegrunnelseRefusjonUavklart"
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
    INNVILGET_SEKUNDÆRLAND_STANDARD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
        override val sanityApiNavn = "innvilgetSekundaerlandStandard"
    },
    INNVILGET_SEKUNDÆRLAND_HARD_KODET {
        override val sanityApiNavn = "innvilgetSekundaerlandHardKodet"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDÆRLAND_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetSekundaerlandAleneansvar"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_NULLUTBETALING {
        override val sanityApiNavn = "innvilgetTilleggstekstNullutbetaling"
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
    INNVILGET_SEKUNDAERLAND_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetSekundaerlandToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SEKUNDAERLAND_UK_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetSekundaerlandUkToArbeidslandNorgeUtbetaler"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SATSENDRING {
        override val sanityApiNavn = "innvilgetTilleggstekstSatsendring"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET
    },

    OPPHØR_EØS_STANDARD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorEosStandard"
    },
    OPPHØR_EØS_SØKER_BER_OM_OPPHØR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorEosSokerBerOmOpphor"
    },
    OPPHØR_BOR_IKKE_I_ET_EØS_LAND {
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
    };

    override val kanDelesOpp: Boolean = false

    override fun enumnavnTilString(): String = this.name

    fun tilEØSBegrunnelseMedTriggere(sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>): EØSBegrunnelseMedTriggere? {
        val sanityEØSBegrunnelse = sanityEØSBegrunnelser.finnBegrunnelse(this) ?: return null
        return EØSBegrunnelseMedTriggere(
            eøsBegrunnelse = this,
            sanityEØSBegrunnelse = sanityEØSBegrunnelse
        )
    }
}
