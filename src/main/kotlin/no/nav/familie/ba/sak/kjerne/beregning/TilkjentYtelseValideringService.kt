package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.hentAktørIderForDenneOgForrigeBehandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
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

    fun finnPersonerMedUgyldigEtterbetalingsperiode(
        behandlingId: Long
    ): List<String> {

        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingId)

        val forrigeBehandling = behandlingService.hentForrigeBehandlingSomErIverksatt(behandling = behandlingService.hent(behandlingId))
        val forrigeAndelerTilkjentYtelse =
            forrigeBehandling?.let { beregningService.hentOptionalTilkjentYtelseForBehandling(behandlingId = it.id) }?.andelerTilkjentYtelse?.toList()

        val andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.toList()

        val gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(kravDato = tilkjentYtelse.behandling.opprettetTidspunkt)

        val aktørIder =
            hentAktørIderForDenneOgForrigeBehandling(
                andelerTilkjentYtelse,
                forrigeAndelerTilkjentYtelse
            )

        val personerMedUgyldigEtterbetaling = mutableListOf<String>()

        aktørIder.forEach { aktørId ->
            val andelerTilkjentYtelseForPerson = andelerTilkjentYtelse.filter { it.aktør.aktørId == aktørId }
            val forrigeAndelerTilkjentYtelseForPerson =
                forrigeAndelerTilkjentYtelse?.filter { it.aktør.aktørId == aktørId }

            val etterbetalingErUgyldig = TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerTilkjentYtelseForPerson,
                andelerTilkjentYtelseForPerson,
                gyldigEtterbetalingFom
            )

            if (etterbetalingErUgyldig) {
                val personIdent = personidentService.hentAktør(aktørId).aktivFødselsnummer()
                personerMedUgyldigEtterbetaling.add(personIdent)
            }
        }

        return personerMedUgyldigEtterbetaling
    }
}
