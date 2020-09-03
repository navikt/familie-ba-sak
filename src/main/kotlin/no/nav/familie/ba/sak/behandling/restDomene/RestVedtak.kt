package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.vedtak.UtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingresultatOgVilkårBegrunnelse
import java.time.LocalDate
import java.time.LocalDateTime

data class RestVedtak(
        val aktiv: Boolean,
        val vedtaksdato: LocalDate?,
        val personBeregninger: List<RestVedtakPerson>,
        val utbetalingBegrunnelser: List<RestUtbetalingBegrunnelse>,
        val id: Long
)

data class RestUtbetalingBegrunnelse(
        val id: Long?,
        val fom: LocalDate,
        val tom: LocalDate,
        val resultat: BehandlingResultatType?,
        var behandlingresultatOgVilkårBegrunnelse: BehandlingresultatOgVilkårBegrunnelse?,
        val opprettetTidspunkt: LocalDateTime
)

data class RestPutUtbetalingBegrunnelse(
        val resultat: BehandlingResultatType?,
        val behandlingresultatOgVilkårBegrunnelse: BehandlingresultatOgVilkårBegrunnelse?
)

data class RestVedtakBegrunnelse(
        val id: BehandlingresultatOgVilkårBegrunnelse,
        val navn: String
)

fun Vedtak.toRestVedtak(restVedtakPerson: List<RestVedtakPerson>) = RestVedtak(
        aktiv = this.aktiv,
        personBeregninger = restVedtakPerson,
        vedtaksdato = this.vedtaksdato,
        id = this.id,
        utbetalingBegrunnelser = this.utbetalingBegrunnelser.map {
            it.toRestUtbetalingBegrunnelse()
        }.sortedBy { it.opprettetTidspunkt }
)

fun UtbetalingBegrunnelse.toRestUtbetalingBegrunnelse() =
        RestUtbetalingBegrunnelse(
                id = this.id,
                fom = this.fom,
                tom = this.tom,
                resultat = this.resultat,
                behandlingresultatOgVilkårBegrunnelse = this.behandlingresultatOgVilkårBegrunnelse,
                opprettetTidspunkt = this.opprettetTidspunkt
        )

