package no.nav.familie.ba.sak.kjerne.vedtak.domene

import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagRestVedtaksbegrunnelse
import no.nav.familie.ba.sak.dataGenerator.brev.lagBrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.tilMinimertUregisrertBarn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilBrevTekst
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtaksbegrunnelseTest {
    val søker = lagPerson(type = PersonType.SØKER)
    val barn1 = lagPerson(type = PersonType.BARN)
    val barn2 = lagPerson(type = PersonType.BARN)
    val barn3 = lagPerson(type = PersonType.BARN)

    val restVedtaksbegrunnelse = lagRestVedtaksbegrunnelse(
        vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
        personIdenter = listOf(søker, barn1, barn2).map { it.personIdent.ident },
        vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
    )

    val vedtaksperiode = NullablePeriode(LocalDate.now().minusMonths(1), LocalDate.now())

    val personerIPersongrunnlag = listOf(søker, barn1, barn2, barn3).map { it.tilMinimertPerson() }

    val målform = Målform.NB

    val beløp = "1234"

    @Test
    fun `skal ta med alle barnas fødselsdatoer ved avslag på søker, men ikke inkludere dem i antall barn`() {
        val brevBegrunnelseGrunnlagMedPersoner = lagBrevBegrunnelseGrunnlagMedPersoner(
            vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.AVSLAG_BOR_HOS_SØKER,
            personIdenter = listOf(søker).map { it.personIdent.ident },
            vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        )

        val brevbegrunnelse = brevBegrunnelseGrunnlagMedPersoner.tilBrevBegrunnelse(
            vedtaksperiode = vedtaksperiode,
            personerIPersongrunnlag = personerIPersongrunnlag,
            brevMålform = målform,
            uregistrerteBarn = emptyList(),
            beløp = beløp
        ) as BegrunnelseData

        Assertions.assertEquals(true, brevbegrunnelse.gjelderSoker)
        Assertions.assertEquals(
            listOf(barn1, barn2, barn3).map { it.fødselsdato }.tilBrevTekst(),
            brevbegrunnelse.barnasFodselsdatoer
        )
        Assertions.assertEquals(0, brevbegrunnelse.antallBarn)
        Assertions.assertEquals(målform.tilSanityFormat(), brevbegrunnelse.maalform)
        Assertions.assertEquals(beløp, brevbegrunnelse.belop)
    }

    @Test
    fun `skal ta med uregistrerte barn`() {
        val uregistrerteBarn = listOf(
            lagPerson(type = PersonType.BARN),
            lagPerson(type = PersonType.BARN)
        ).map {
            BarnMedOpplysninger(
                ident = it.personIdent.ident,
                fødselsdato = it.fødselsdato
            ).tilMinimertUregisrertBarn()
        }

        val brevBegrunnelseGrunnlagMedPersoner = lagBrevBegrunnelseGrunnlagMedPersoner(
            vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.AVSLAG_UREGISTRERT_BARN,
            personIdenter = emptyList(),
            vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        )

        val brevbegrunnelse = brevBegrunnelseGrunnlagMedPersoner.tilBrevBegrunnelse(
            vedtaksperiode = vedtaksperiode,
            personerIPersongrunnlag = personerIPersongrunnlag,
            brevMålform = målform,
            uregistrerteBarn = uregistrerteBarn,
            beløp = beløp
        ) as BegrunnelseData

        Assertions.assertEquals(false, brevbegrunnelse.gjelderSoker)
        Assertions.assertEquals(
            uregistrerteBarn.map { it.fødselsdato!! }.tilBrevTekst(),
            brevbegrunnelse.barnasFodselsdatoer
        )
        Assertions.assertEquals(2, brevbegrunnelse.antallBarn)
        Assertions.assertEquals(målform.tilSanityFormat(), brevbegrunnelse.maalform)
        Assertions.assertEquals(beløp, brevbegrunnelse.belop)
    }
}
