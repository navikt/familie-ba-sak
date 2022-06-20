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
    OPPHØR_EØS_STANDARD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorEosStandard"
    },
    OPPHØR_EØS_SØKER_BER_OM_OPPHØR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorEosSokerBerOmOpphor"
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
