package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.barn
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.steg.EndringerIUtbetalingForBehandlingSteg
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BeregningService(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val behandlingRepository: BehandlingRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val tilkjentYtelseEndretAbonnenter: List<TilkjentYtelseEndretAbonnent> = emptyList(),
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val tilkjentYtelseGenerator: TilkjentYtelseGenerator,
) {
    fun slettTilkjentYtelseForBehandling(behandlingId: Long) =
        tilkjentYtelseRepository
            .findByBehandlingOptional(behandlingId)
            ?.let { tilkjentYtelseRepository.delete(it) }

    fun hentLøpendeAndelerTilkjentYtelseMedUtbetalingerForBehandlinger(
        behandlingIder: List<Long>,
        avstemmingstidspunkt: LocalDateTime,
    ): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository
            .finnLøpendeAndelerTilkjentYtelseForBehandlinger(
                behandlingIder,
                avstemmingstidspunkt.toLocalDate().toYearMonth(),
            ).filter { it.erAndelSomSkalSendesTilOppdrag() }

    fun hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId: Long): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository
            .finnAndelerTilkjentYtelseForBehandling(behandlingId)
            .filter { it.erAndelSomSkalSendesTilOppdrag() }

    fun hentAndelerTilkjentYtelseForBehandling(behandlingId: Long): List<AndelTilkjentYtelse> = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)

    fun hentTilkjentYtelseForBehandling(behandlingId: Long) = tilkjentYtelseRepository.findByBehandling(behandlingId)

    fun hentOptionalTilkjentYtelseForBehandling(behandlingId: Long) = tilkjentYtelseRepository.findByBehandlingOptional(behandlingId)

    /**
     * Denne metoden henter alle relaterte behandlinger på en person.
     * Per fagsak henter man tilkjent ytelse fra:
     * 1. Behandling som holder på å iverksettes
     * 2. Siste behandling som er vedtatt
     * 3. Filtrer bort behandlinger der barnet ikke lenger finnes
     */
    fun hentRelevanteTilkjentYtelserForBarn(
        barnAktør: Aktør,
        fagsakId: Long,
    ): List<TilkjentYtelse> {
        val andreFagsaker =
            fagsakService
                .hentFagsakerPåPerson(barnAktør)
                .filter { it.id != fagsakId }

        return andreFagsaker
            .mapNotNull { fagsak ->
                val godkjentBehandlingerSomIkkeErIverksattEnda =
                    behandlingRepository.finnBehandlingerSomHolderPåÅIverksettes(fagsakId = fagsak.id).singleOrNull()
                godkjentBehandlingerSomIkkeErIverksattEnda ?: behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsak.id)
            }.map {
                hentTilkjentYtelseForBehandling(behandlingId = it.id)
            }.filter {
                personopplysningGrunnlagRepository
                    .finnSøkerOgBarnAktørerTilAktiv(behandlingId = it.behandling.id)
                    .barn()
                    .map { barn -> barn.aktør }
                    .contains(barnAktør)
            }.map { it }
    }

    fun erEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling: Behandling): Boolean = hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling) == EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING

    fun hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling: Behandling): EndringerIUtbetalingForBehandlingSteg {
        val endringerIUtbetaling =
            hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomiTidslinje(behandling)
                .tilPerioder()
                .any { it.verdi == true }

        return if (endringerIUtbetaling) EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING else EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING
    }

    fun hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomiTidslinje(behandling: Behandling): Tidslinje<Boolean> {
        val nåværendeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)
        val forrigeAndeler = hentAndelerFraForrigeIverksattebehandling(behandling)

        if (nåværendeAndeler.isEmpty() && forrigeAndeler.isEmpty()) return tomTidslinje()

        return EndringIUtbetalingUtil.lagEndringIUtbetalingTidslinje(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
        )
    }

    fun hentAndelerFraForrigeIverksattebehandling(behandling: Behandling): List<AndelTilkjentYtelse> {
        val forrigeBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling)
        return forrigeBehandling?.let { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(it.id) }
            ?: emptyList()
    }

    fun hentAndelerFraForrigeVedtatteBehandling(behandling: Behandling): List<AndelTilkjentYtelse> {
        val forrigeBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
        return forrigeBehandling?.let { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(it.id) }
            ?: emptyList()
    }

    @Transactional
    fun oppdaterBehandlingMedBeregning(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        nyEndretUtbetalingAndel: EndretUtbetalingAndel? = null,
    ): TilkjentYtelse {
        val endreteUtbetalingAndeler =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id)
                .filter {
                    // Ved automatiske behandlinger ønsker vi alltid å ta vare på de gamle endrede andelene
                    if (behandling.skalBehandlesAutomatisk) {
                        true
                    } else if (nyEndretUtbetalingAndel != null) {
                        it.id == nyEndretUtbetalingAndel.id || it.andelerTilkjentYtelse.isNotEmpty()
                    } else {
                        it.andelerTilkjentYtelse.isNotEmpty()
                    }
                }

        return genererOgLagreTilkjentYtelse(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag,
            endreteUtbetalingAndeler = endreteUtbetalingAndeler,
        )
    }

    private fun genererOgLagreTilkjentYtelse(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        endreteUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
    ): TilkjentYtelse {
        tilkjentYtelseRepository.slettTilkjentYtelseFor(behandling)

        val tilkjentYtelse =
            tilkjentYtelseGenerator.genererTilkjentYtelse(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                endretUtbetalingAndeler = endreteUtbetalingAndeler,
            )

        val lagretTilkjentYtelse = tilkjentYtelseRepository.saveAndFlush(tilkjentYtelse)
        tilkjentYtelseEndretAbonnenter.forEach { it.endretTilkjentYtelse(lagretTilkjentYtelse) }
        return lagretTilkjentYtelse
    }

    // For at endret utbetaling andeler skal fungere så må man generere andeler før man kobler endringene på andelene
    // Dette er fordi en endring regnes som gyldig når den overlapper med en andel og har gyldig årsak
    // Hvis man ikke genererer andeler før man kobler på endringene så vil ingen av endringene ses på som gyldige, altså ikke oppdatere noen andeler
    @Transactional
    fun genererTilkjentYtelseFraVilkårsvurdering(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
    ): TilkjentYtelse {
        // 1: Genererer andeler fra vilkårsvurderingen uten å ta hensyn til endret utbetaling andeler
        genererOgLagreTilkjentYtelse(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag,
            endreteUtbetalingAndeler = emptyList(),
        )

        // 2: Genererer andeler som også tar hensyn til endret utbetaling andeler
        return oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
    }

    /**
     * Henter alle barn på behandlingen som har minst en periode med tilkjentytelse.
     */
    fun finnBarnFraBehandlingMedTilkjentYtelse(behandlingId: Long): List<Aktør> {
        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository
                .finnAndelerTilkjentYtelseForBehandling(behandlingId)

        return personopplysningGrunnlagRepository
            .findByBehandlingAndAktiv(behandlingId)
            ?.barna
            ?.map { it.aktør }
            ?.filter {
                andelerTilkjentYtelse.any { aty -> aty.aktør == it }
            } ?: emptyList()
    }
}

interface TilkjentYtelseEndretAbonnent {
    fun endretTilkjentYtelse(tilkjentYtelse: TilkjentYtelse)
}
