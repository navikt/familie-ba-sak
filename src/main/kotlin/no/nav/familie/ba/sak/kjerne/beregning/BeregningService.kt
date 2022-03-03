package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
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
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val behandlingRepository: BehandlingRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val småbarnstilleggService: SmåbarnstilleggService
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

    fun hentAndelerTilkjentYtelseMedEndringsutbetalinger(behandlingId: Long): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
            .filter { it.harEndringsutbetalinger() }

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
            )
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

            if (behandlingSomErSendtTilGodkjenning != null) behandlingSomErSendtTilGodkjenning
            else {
                val godkjenteBehandlingerSomIkkeErIverksattEnda =
                    behandlingRepository.finnGodkjenteBehandlingerSomHolderPåÅIverksettes(fagsakId = fagsak.id)
                        .singleOrNull()
                if (godkjenteBehandlingerSomIkkeErIverksattEnda != null) godkjenteBehandlingerSomIkkeErIverksattEnda
                else {
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

    @Transactional
    fun oppdaterBehandlingMedBeregning(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        nyEndretUtbetalingAndel: EndretUtbetalingAndel? = null
    ): TilkjentYtelse {

        val endretUtbetalingAndeler = endretUtbetalingAndelRepository.findByBehandlingId(behandling.id).filter {
            // Ved automatiske behandlinger ønsker vi alltid å ta vare på de gamle endrede andelene
            if (behandling.skalBehandlesAutomatisk) true
            else if (nyEndretUtbetalingAndel != null) {
                it.id == nyEndretUtbetalingAndel.id || it.andelTilkjentYtelser.isNotEmpty()
            } else {
                it.andelTilkjentYtelser.isNotEmpty()
            }
        }

        tilkjentYtelseRepository.slettTilkjentYtelseFor(behandling)
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id)
            ?: throw IllegalStateException("Kunne ikke hente vilkårsvurdering for behandling med id ${behandling.id}")

        val tilkjentYtelse = TilkjentYtelseUtils
            .beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling
            ) { aktørId ->
                småbarnstilleggService.hentOgLagrePerioderMedFullOvergangsstønad(
                    aktør = aktørId,
                    behandlingId = behandling.id
                )
            }

        val andelerTilkjentYtelse = TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
            tilkjentYtelse.andelerTilkjentYtelse,
            endretUtbetalingAndeler
        )
        tilkjentYtelse.andelerTilkjentYtelse.clear()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)

        return tilkjentYtelseRepository.save(tilkjentYtelse)
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
            if (sistIverksatteBehandling == null) emptyList()
            else hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                behandlingId = sistIverksatteBehandling.id
            ).filter { it.erSmåbarnstillegg() }

        val nyeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) emptyList()
            else hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                behandlingId = behandling.id
            ).filter { it.erSmåbarnstillegg() }

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) = hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
            forrigeSmåbarnstilleggAndeler = forrigeSmåbarnstilleggAndeler,
            nyeSmåbarnstilleggAndeler = nyeSmåbarnstilleggAndeler,
        )

        return kanAutomatiskIverksetteSmåbarnstillegg(
            innvilgedeMånedPerioder = innvilgedeMånedPerioder,
            reduserteMånedPerioder = reduserteMånedPerioder
        )
    }

    private fun populerTilkjentYtelse(
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
