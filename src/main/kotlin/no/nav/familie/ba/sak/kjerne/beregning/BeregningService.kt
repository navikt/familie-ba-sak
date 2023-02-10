package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForUtbetalingsoppdragFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.pakkInnForUtbetaling
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class BeregningService(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val behandlingRepository: BehandlingRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val småbarnstilleggService: SmåbarnstilleggService,
    private val tilkjentYtelseEndretAbonnenter: List<TilkjentYtelseEndretAbonnent> = emptyList(),
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
) {
    fun slettTilkjentYtelseForBehandling(behandlingId: Long) =
        tilkjentYtelseRepository.findByBehandlingOptional(behandlingId)
            ?.let { tilkjentYtelseRepository.delete(it) }

    fun hentLøpendeAndelerTilkjentYtelseMedUtbetalingerForBehandlinger(
        behandlingIder: List<Long>,
        avstemmingstidspunkt: LocalDateTime
    ): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository.finnLøpendeAndelerTilkjentYtelseForBehandlinger(
            behandlingIder,
            avstemmingstidspunkt.toLocalDate().toYearMonth()
        )
            .filter { it.erAndelSomSkalSendesTilOppdrag() }

    fun hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId: Long): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
            .filter { it.erAndelSomSkalSendesTilOppdrag() }

    fun hentAndelerTilkjentYtelseForBehandling(behandlingId: Long): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)

    fun lagreTilkjentYtelseMedOppdaterteAndeler(tilkjentYtelse: TilkjentYtelse) =
        tilkjentYtelseRepository.save(tilkjentYtelse)

    fun hentTilkjentYtelseForBehandling(behandlingId: Long) =
        tilkjentYtelseRepository.findByBehandling(behandlingId)

    fun hentOptionalTilkjentYtelseForBehandling(behandlingId: Long) =
        tilkjentYtelseRepository.findByBehandlingOptional(behandlingId)

    fun hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(fagsakId: Long): List<TilkjentYtelse> {
        val iverksatteBehandlinger = behandlingRepository.findByFagsakAndAvsluttet(fagsakId)
        return iverksatteBehandlinger.mapNotNull {
            tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(
                it.id
            )?.takeIf { tilkjentYtelse ->
                tilkjentYtelse.andelerTilkjentYtelse.any { aty -> aty.erAndelSomSkalSendesTilOppdrag() }
            }
        }
    }

    /**
     * Denne metoden henter alle relaterte behandlinger på en person.
     * Per fagsak henter man tilkjent ytelse fra:
     * 1. Behandling som er til godkjenning
     * 2. Siste behandling som er iverksatt
     * 3. Filtrer bort behandlinger der barnet ikke lenger finnes
     */
    fun hentRelevanteTilkjentYtelserForBarn(
        barnAktør: Aktør,
        fagsakId: Long
    ): List<TilkjentYtelse> {
        val andreFagsaker = fagsakService.hentFagsakerPåPerson(barnAktør)
            .filter { it.id != fagsakId }

        return andreFagsaker.mapNotNull { fagsak ->
            val behandlingSomErSendtTilGodkjenning = behandlingRepository.finnBehandlingerSentTilGodkjenning(
                fagsakId = fagsak.id
            ).singleOrNull()

            if (behandlingSomErSendtTilGodkjenning != null) {
                behandlingSomErSendtTilGodkjenning
            } else {
                val godkjenteBehandlingerSomIkkeErIverksattEnda =
                    behandlingRepository.finnBehandlingerSomHolderPåÅIverksettes(fagsakId = fagsak.id).singleOrNull()
                if (godkjenteBehandlingerSomIkkeErIverksattEnda != null) {
                    godkjenteBehandlingerSomIkkeErIverksattEnda
                } else {
                    val iverksatteBehandlinger = behandlingRepository.finnIverksatteBehandlinger(fagsakId = fagsak.id)
                    Behandlingutils.hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger)
                }
            }
        }.map {
            hentTilkjentYtelseForBehandling(behandlingId = it.id)
        }.filter {
            personopplysningGrunnlagRepository
                .findByBehandlingAndAktiv(behandlingId = it.behandling.id)
                ?.barna?.map { barn -> barn.aktør }
                ?.contains(barnAktør)
                ?: false
        }.map { it }
    }

    fun erAlleUtbetalingsperioderPåNullKroner(behandling: Behandling): Boolean {
        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

        return andelerTilkjentYtelse.all { it.kalkulertUtbetalingsbeløp == 0 }
    }

    @Transactional
    fun oppdaterBehandlingMedBeregning(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        nyEndretUtbetalingAndel: EndretUtbetalingAndel? = null
    ): TilkjentYtelse {
        val endreteUtbetalingAndeler = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id).filter {
                // Ved automatiske behandlinger ønsker vi alltid å ta vare på de gamle endrede andelene
                if (behandling.skalBehandlesAutomatisk) {
                    true
                } else if (nyEndretUtbetalingAndel != null) {
                    it.id == nyEndretUtbetalingAndel.id || it.andelerTilkjentYtelse.isNotEmpty()
                } else {
                    it.andelerTilkjentYtelse.isNotEmpty()
                }
            }

        tilkjentYtelseRepository.slettTilkjentYtelseFor(behandling)
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id)
            ?: throw IllegalStateException("Kunne ikke hente vilkårsvurdering for behandling med id ${behandling.id}")

        val tilkjentYtelse =
            TilkjentYtelseUtils.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling,
                endretUtbetalingAndeler = endreteUtbetalingAndeler
            ) { søkerAktør ->
                småbarnstilleggService.hentOgLagrePerioderMedFullOvergangsstønad(
                    søkerAktør = søkerAktør,
                    behandlingId = behandling.id
                )
            }

        val lagretTilkjentYtelse = tilkjentYtelseRepository.save(tilkjentYtelse)
        tilkjentYtelseEndretAbonnenter.forEach { it.endretTilkjentYtelse(lagretTilkjentYtelse) }
        return lagretTilkjentYtelse
    }

    fun oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
        behandling: Behandling,
        utbetalingsoppdrag: Utbetalingsoppdrag
    ): TilkjentYtelse {
        val nyTilkjentYtelse = populerTilkjentYtelse(behandling, utbetalingsoppdrag)
        return tilkjentYtelseRepository.save(nyTilkjentYtelse)
    }

    fun kanAutomatiskIverksetteSmåbarnstilleggEndring(
        behandling: Behandling,
        sistIverksatteBehandling: Behandling?
    ): Boolean {
        if (!behandling.skalBehandlesAutomatisk || !behandling.erSmåbarnstillegg()) return false

        val forrigeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) {
                emptyList()
            } else {
                hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                    behandlingId = sistIverksatteBehandling.id
                ).filter { it.erSmåbarnstillegg() }
            }

        val nyeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) {
                emptyList()
            } else {
                hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                    behandlingId = behandling.id
                ).filter { it.erSmåbarnstillegg() }
            }

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) = hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
            forrigeSmåbarnstilleggAndeler = forrigeSmåbarnstilleggAndeler,
            nyeSmåbarnstilleggAndeler = nyeSmåbarnstilleggAndeler
        )

        return kanAutomatiskIverksetteSmåbarnstillegg(
            innvilgedeMånedPerioder = innvilgedeMånedPerioder,
            reduserteMånedPerioder = reduserteMånedPerioder
        )
    }

    /**
     * Henter alle barn på behandlingen som har minst en periode med tilkjentytelse.
     */
    fun finnBarnFraBehandlingMedTilkjentYtelse(behandlingId: Long): List<Aktør> {
        val andelerTilkjentYtelse = andelTilkjentYtelseRepository
            .finnAndelerTilkjentYtelseForBehandling(behandlingId)

        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)?.barna?.map { it.aktør }
            ?.filter {
                andelerTilkjentYtelse.any { aty -> aty.aktør == it }
            } ?: emptyList()
    }

    fun hentSisteOffsetPerIdent(
        fagsakId: Long,
        andelTilkjentYtelseForUtbetalingsoppdragFactory: AndelTilkjentYtelseForUtbetalingsoppdragFactory
    ): Map<String, Int> {
        val alleAndelerTilkjentYtelserIverksattMotØkonomi =
            hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(fagsakId)
                .flatMap { it.andelerTilkjentYtelse }
                .filter { it.erAndelSomSkalSendesTilOppdrag() }
                .pakkInnForUtbetaling(andelTilkjentYtelseForUtbetalingsoppdragFactory)

        val alleTideligereKjederIverksattMotØkonomi =
            ØkonomiUtils.kjedeinndelteAndeler(alleAndelerTilkjentYtelserIverksattMotØkonomi)

        return ØkonomiUtils.gjeldendeForrigeOffsetForKjede(alleTideligereKjederIverksattMotØkonomi)
    }

    fun hentSisteOffsetPåFagsak(behandling: Behandling): Int? =
        behandlingHentOgPersisterService.hentBehandlingerSomErIverksatt(behandling = behandling)
            .mapNotNull { iverksattBehandling ->
                hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(iverksattBehandling.id)
                    .takeIf { it.isNotEmpty() }
                    ?.let { andelerTilkjentYtelse ->
                        andelerTilkjentYtelse.maxByOrNull { it.periodeOffset!! }?.periodeOffset?.toInt()
                    }
            }.maxByOrNull { it }

    fun populerTilkjentYtelse(
        behandling: Behandling,
        utbetalingsoppdrag: Utbetalingsoppdrag
    ): TilkjentYtelse {
        val erRentOpphør =
            utbetalingsoppdrag.utbetalingsperiode.isNotEmpty() && utbetalingsoppdrag.utbetalingsperiode.all { it.opphør != null }
        var opphørsdato: LocalDate? = null
        if (erRentOpphør) {
            opphørsdato = utbetalingsoppdrag.utbetalingsperiode.minOf { it.opphør!!.opphørDatoFom }
        }

        if (behandling.type == BehandlingType.REVURDERING) {
            val opphørPåRevurdering = utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør != null }
            if (opphørPåRevurdering.isNotEmpty()) {
                opphørsdato = opphørPåRevurdering.maxByOrNull { it.opphør!!.opphørDatoFom }!!.opphør!!.opphørDatoFom
            }
        }

        val tilkjentYtelse =
            tilkjentYtelseRepository.findByBehandling(behandling.id)

        return tilkjentYtelse.apply {
            this.utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag)
            this.stønadTom = tilkjentYtelse.andelerTilkjentYtelse.maxOfOrNull { it.stønadTom }
            this.stønadFom =
                if (erRentOpphør) null else tilkjentYtelse.andelerTilkjentYtelse.minOfOrNull { it.stønadFom }
            this.endretDato = LocalDate.now()
            this.opphørFom = opphørsdato?.toYearMonth()
        }
    }
}

interface TilkjentYtelseEndretAbonnent {
    fun endretTilkjentYtelse(tilkjentYtelse: TilkjentYtelse)
}
