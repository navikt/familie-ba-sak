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
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

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
    fun slettTilkjentYtelseForBehandling(behandlingId: Long) = tilkjentYtelseRepository.findByBehandling(behandlingId)
        ?.let { tilkjentYtelseRepository.delete(it) }

    fun hentLøpendeAndelerTilkjentYtelseMedUtbetalingerForBehandlinger(behandlingIder: List<Long>): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository.finnLøpendeAndelerTilkjentYtelseForBehandlinger(behandlingIder)
            .filter { it.erAndelSomSkalSendesTilOppdrag() }

    fun hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId: Long): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
            .filter { it.erAndelSomSkalSendesTilOppdrag() }

    fun lagreTilkjentYtelseMedOppdaterteAndeler(tilkjentYtelse: TilkjentYtelse) =
        tilkjentYtelseRepository.save(tilkjentYtelse)

    fun hentTilkjentYtelseForBehandling(behandlingId: Long) =
        tilkjentYtelseRepository.findByBehandling(behandlingId)
            ?: error("Fant ikke tilkjent ytelse for behandling $behandlingId")

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
     * Denne metoden henter alle tilkjent ytelser for et barn gruppert på behandling.
     * Den går gjennom alle fagsaker og sørger for å filtrere bort behandling ikke sent til godkjenning,
     * henlagte behandlinger, samt fagsaker som ikke lengre har barn i gjeldende behandling.
     */
    fun hentSentTilGodkjenningTilkjentYtelseForBarn(
        barnIdent: PersonIdent,
        fagsakId: Long
    ): List<TilkjentYtelse> {
        val andreFagsaker = fagsakService.hentFagsakerPåPerson(barnIdent)
            .filter { it.id != fagsakId }

        return andreFagsaker.mapNotNull { fagsak ->
            Behandlingutils.hentSisteBehandlingSomIkkeErTekniskOpphør(
                behandlinger = behandlingRepository.finnBehandlingerSentTilGodkjenning(
                    fagsakId = fagsak.id
                )
            )
        }.map {
            hentTilkjentYtelseForBehandling(behandlingId = it.id)
        }.filter {
            personopplysningGrunnlagRepository
                .findByBehandlingAndAktiv(behandlingId = it.behandling.id)
                ?.barna?.map { barn -> barn.personIdent }
                ?.contains(barnIdent)
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
            if (nyEndretUtbetalingAndel != null) {
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
            ) {
                småbarnstilleggService.hentOgLagrePerioderMedFullOvergangsstønad(
                    personIdent = it,
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
                ?: error("Fant ikke tilkjent ytelse for behandling ${behandling.id}")
        return tilkjentYtelse.apply {
            this.utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag)
            this.stønadTom =
                utbetalingsoppdrag.utbetalingsperiode.maxByOrNull { it.vedtakdatoTom }!!.vedtakdatoTom.toYearMonth()
            this.stønadFom = if (erRentOpphør) null else utbetalingsoppdrag.utbetalingsperiode
                .filter { !it.erEndringPåEksisterendePeriode }
                .minByOrNull { it.vedtakdatoFom }!!.vedtakdatoFom.toYearMonth()
            this.endretDato = LocalDate.now()
            this.opphørFom = opphørsdato?.toYearMonth()
        }
    }
}
