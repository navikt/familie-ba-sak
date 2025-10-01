package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.datagenerator.lagBegrunnelseData
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class IVedtakBegrunnelseTest {
    @Test
    fun `Skal serialiseres med prefix`() {
        val serialisertStandardbegrunnelse =
            objectMapper.writeValueAsString(Standardbegrunnelse.INNVILGET_BOR_HOS_SØKER)
        Assertions.assertEquals(
            objectMapper.readValue(serialisertStandardbegrunnelse, Standardbegrunnelse::class.java),
            Standardbegrunnelse.INNVILGET_BOR_HOS_SØKER,
        )

        val serialisertEØSStandardbegrunnelse =
            objectMapper.writeValueAsString(EØSStandardbegrunnelse.AVSLAG_EØS_IKKE_EØS_BORGER)
        Assertions.assertEquals(
            objectMapper.readValue(serialisertEØSStandardbegrunnelse, EØSStandardbegrunnelse::class.java),
            EØSStandardbegrunnelse.AVSLAG_EØS_IKKE_EØS_BORGER,
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = Standardbegrunnelse::class,
        names = [
            "INNVILGET_FINNMARKSTILLEGG_MED_DATO",
            "INNVILGET_FINNMARKSTILLEGG_INSTITUSJON",
            "INNVILGET_SVALBARDTILLEGG",
            "INNVILGET_FINNMARKSTILLEGG_UTEN_DATO",
            "INNVILGET_AUTOVEDTAK_FØDSEL_SVALBARDTILLEGG",
            "REDUKSJON_FINNMARKSTILLEGG",
            "REDUKSJON_SVALBARDTILLEGG",
            "REDUKSJON_FINNMARKSTILLEGG_BODDE_IKKE_I_TILLEGGSONE",
            "REDUKSJON_SVALBARDTILLEGG_BODDE_IKKE_PÅ_SVALBARD",
            "FORTSATT_INNVILGET_FINNMARKSTILLEGG",
            "FORTSATT_INNVILGET_SVALBARDTILLEGG",
        ],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `Finnmark og Svalbard-begrunnelser blir sortert nederst`(standardbegrunnelse: Standardbegrunnelse) {
        val borHosSøker = lagBegrunnelseData(Standardbegrunnelse.INNVILGET_BOR_HOS_SØKER)
        val avslagGift = lagBegrunnelseData(Standardbegrunnelse.AVSLAG_GIFT)

        val begrunnelseMedFinnmarkEllerSvalbard =
            lagBegrunnelseData(standardbegrunnelse)

        val usortertListe = listOf(avslagGift, begrunnelseMedFinnmarkEllerSvalbard, borHosSøker)
        val sortertListe = usortertListe.sorted()

        assertThat(sortertListe.last()).isEqualTo(begrunnelseMedFinnmarkEllerSvalbard)
    }
}
