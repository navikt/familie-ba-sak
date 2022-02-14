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
        gjelderSøker: Boolean,
        barnasFødselsdatoer: List<LocalDate>,
    ): Int {
        val erAvslagUregistrerteBarn =
            this.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.AVSLAG_UREGISTRERT_BARN

        return when {
            erAvslagUregistrerteBarn -> uregistrerteBarn.size
            gjelderSøker && this.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG -> 0
            else -> barnasFødselsdatoer.size
        }
    }

    fun hentBarnasFødselsdagerForBegrunnelse(
        uregistrerteBarn: List<MinimertUregistrertBarn>,
        gjelderSøker: Boolean,
        personerIBehandling: List<MinimertRestPerson>,
        personerPåBegrunnelse: List<MinimertRestPerson>,
        personerMedUtbetaling: List<MinimertRestPerson>,
    ) = when {
        this.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.AVSLAG_UREGISTRERT_BARN ->
            uregistrerteBarn.mapNotNull { it.fødselsdato }

        gjelderSøker -> {
            if (this.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG) {
                personerIBehandling
                    .filter { it.type == PersonType.BARN }
                    .map { it.fødselsdato } +
                    uregistrerteBarn.mapNotNull { it.fødselsdato }
            } else {
                (personerMedUtbetaling + personerPåBegrunnelse).toSet()
                    .filter { it.type == PersonType.BARN }
                    .map { it.fødselsdato }
            }
        }
        else ->
            personerPåBegrunnelse
                .filter { it.type == PersonType.BARN }
                .map { it.fødselsdato }
    }
}
