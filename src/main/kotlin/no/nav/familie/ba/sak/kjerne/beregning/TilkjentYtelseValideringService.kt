package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.finnAktørIderMedUgyldigEtterbetalingsperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import org.springframework.stereotype.Service

@Service
class TilkjentYtelseValideringService(
    private val totrinnskontrollService: TotrinnskontrollService,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService,
    private val personidentService: PersonidentService,
    private val behandlingService: BehandlingService
) {
    fun validerAtIngenUtbetalingerOverstiger100Prosent(behandling: Behandling) {
        if (behandling.erMigrering() || behandling.erTekniskEndring()) return
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)

        if (totrinnskontroll?.godkjent == true) {
            val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id)

            val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandling.id)

            val barnMedAndreRelevanteTilkjentYtelser = personopplysningGrunnlag.barna.map {
                Pair(
                    it,
                    beregningService.hentRelevanteTilkjentYtelserForBarn(it.aktør, behandling.fagsak.id)
                )
            }

            TilkjentYtelseValidering.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
                behandlendeBehandlingTilkjentYtelse = tilkjentYtelse,
                barnMedAndreRelevanteTilkjentYtelser = barnMedAndreRelevanteTilkjentYtelser,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )
        }
    }

    fun barnetrygdLøperForAnnenForelder(behandling: Behandling, barna: List<Person>): Boolean {
        return barna.any {
            beregningService.hentRelevanteTilkjentYtelserForBarn(barnAktør = it.aktør, fagsakId = behandling.fagsak.id)
                .isNotEmpty()
        }
    }

    fun finnAktørerMedUgyldigEtterbetalingsperiode(
        behandlingId: Long
    ): List<Aktør> {

        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingId)

        val forrigeBehandling = behandlingService.hentForrigeBehandlingSomErIverksatt(behandling = behandlingService.hent(behandlingId))
        val forrigeAndelerTilkjentYtelse =
            forrigeBehandling?.let { beregningService.hentOptionalTilkjentYtelseForBehandling(behandlingId = it.id) }?.andelerTilkjentYtelse?.toList()

        val aktørIderMedUgyldigEtterbetaling = finnAktørIderMedUgyldigEtterbetalingsperiode(
            forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
            andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.toList(),
            kravDato = tilkjentYtelse.behandling.opprettetTidspunkt
        )

        return aktørIderMedUgyldigEtterbetaling.map { aktørId -> personidentService.hentAktør(identEllerAktørId = aktørId) }
    }
}
