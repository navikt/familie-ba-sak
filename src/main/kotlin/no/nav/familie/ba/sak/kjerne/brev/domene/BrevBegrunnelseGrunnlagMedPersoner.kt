package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import java.time.LocalDate

data class BrevBegrunnelseGrunnlagMedPersoner(
    val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
    val triggesAv: TriggesAv,
    val personIdenter: List<String>,
) {
    fun hentAntallBarnForBegrunnelse(
        uregistrerteBarn: List<MinimertUregistrertBarn>,
        erAvslagPåKunSøker: Boolean,
        barnasFødselsdatoer: List<LocalDate>
    ) = if (this.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.AVSLAG_UREGISTRERT_BARN)
        uregistrerteBarn.size
    else if (erAvslagPåKunSøker)
        0
    else
        barnasFødselsdatoer.size

    fun hentBarnasFødselsdagerForBegrunnelse(
        uregistrerteBarn: List<MinimertUregistrertBarn>,
        erAvslagPåKunSøker: Boolean,
        personerIBehandling: List<MinimertRestPerson>,
        personerPåBegrunnelse: List<MinimertRestPerson>
    ) = if (this.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.AVSLAG_UREGISTRERT_BARN)
        uregistrerteBarn.mapNotNull { it.fødselsdato }
    else if (erAvslagPåKunSøker) {
        personerIBehandling.filter { it.type == PersonType.BARN }
            .map { it.fødselsdato } + uregistrerteBarn.mapNotNull { it.fødselsdato }
    } else
        personerPåBegrunnelse.filter { it.type == PersonType.BARN }.map { it.fødselsdato }
}
