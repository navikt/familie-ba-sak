package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.hentUtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.beregning.tilDatoSegment
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerDeltBosted
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerIngenOverlappendeEndring
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerPeriodeInnenforTilkjentytelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.fraRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.hentGyldigEndretBegrunnelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class EndretUtbetalingAndelService(
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val sanityService: SanityService,
    private val vilkårsvurderingService: VilkårsvurderingService
) {
    fun hentEndredeUtbetalingAndeler(behandlingId: Long) =
        endretUtbetalingAndelRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelId: Long,
        restEndretUtbetalingAndel: RestEndretUtbetalingAndel
    ) {
        val endretUtbetalingAndel = endretUtbetalingAndelRepository.getById(endretUtbetalingAndelId)
        val person =
            persongrunnlagService.hentPersonerPåBehandling(listOf(restEndretUtbetalingAndel.personIdent!!), behandling)
                .first()

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
                ?: throw Feil("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        val andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

        endretUtbetalingAndel.fraRestEndretUtbetalingAndel(restEndretUtbetalingAndel, person)

        val andreEndredeAndelerPåBehandling = endretUtbetalingAndelRepository.findByBehandlingId(behandling.id)
            .filter { it.id != endretUtbetalingAndelId }

        val gyldigTomEtterDagensDato = beregnGyldigTomIFremtiden(
            andreEndredeAndelerPåBehandling = andreEndredeAndelerPåBehandling,
            endretUtbetalingAndel = endretUtbetalingAndel,
            andelTilkjentYtelser = andelTilkjentYtelser
        )

        validerTomDato(tomDato = endretUtbetalingAndel.tom, gyldigTomEtterDagensDato = gyldigTomEtterDagensDato, årsak = endretUtbetalingAndel.årsak)

        if (endretUtbetalingAndel.tom == null) {
            endretUtbetalingAndel.tom = gyldigTomEtterDagensDato
        }

        validerÅrsak(behandling = behandling, person = person, årsak = endretUtbetalingAndel.årsak, endretUtbetalingAndel = endretUtbetalingAndel)
        validerUtbetalingMotÅrsak(årsak = endretUtbetalingAndel.årsak, skalUtbetales = endretUtbetalingAndel.prosent != BigDecimal(0))

        validerIngenOverlappendeEndring(
            endretUtbetalingAndel = endretUtbetalingAndel,
            eksisterendeEndringerPåBehandling = andreEndredeAndelerPåBehandling
        )

        validerPeriodeInnenforTilkjentytelse(endretUtbetalingAndel, andelTilkjentYtelser)

        beregningService.oppdaterBehandlingMedBeregning(
            behandling, personopplysningGrunnlag, endretUtbetalingAndel
        )
    }

    private fun validerÅrsak(
        behandling: Behandling,
        person: Person,
        årsak: Årsak?,
        endretUtbetalingAndel: EndretUtbetalingAndel
    ) {
        if (årsak == Årsak.DELT_BOSTED) {
            val deltBostedPerioder = hentDeltBostedPerioder(behandling, person)
            validerDeltBosted(endretUtbetalingAndel = endretUtbetalingAndel, deltBostedPerioder = deltBostedPerioder)
        }
    }

    private fun hentDeltBostedPerioder(
        behandling: Behandling,
        person: Person
    ): List<Periode> {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)

        val personensVilkår = vilkårsvurdering?.personResultater?.single { it.aktør == person.aktør }

        val deltBostedVilkårResultater = personensVilkår?.vilkårResultater?.filter {
            it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) && it.resultat == Resultat.OPPFYLT
        }

        val datoSegmenter = deltBostedVilkårResultater?.map { it.tilDatoSegment(vilkår = deltBostedVilkårResultater) } // TODO: Feilmeldingene i tilDatoSegment passer ikke til denne bruken
            ?: return emptyList()

        return slåSammenDeltBostedPerioderSomIkkeSkulleHaVærtSplittet(
            perioder = datoSegmenter.map {
                Periode(
                    fom = it.fom,
                    tom = it.tom
                )
            }.toMutableList()
        )
    }

    @Transactional
    fun fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelId: Long,
    ) {
        val endretUtbetalingAndel = endretUtbetalingAndelRepository.getById(endretUtbetalingAndelId)
        endretUtbetalingAndel.andelTilkjentYtelser.forEach { it.endretUtbetalingAndeler.clear() }
        endretUtbetalingAndelRepository.delete(endretUtbetalingAndel)

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
                ?: throw Feil("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
    }

    @Transactional
    fun opprettTomEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling
    ) =
        endretUtbetalingAndelRepository.save(
            EndretUtbetalingAndel(
                behandlingId = behandling.id,
            )
        )

    @Transactional
    fun kopierEndretUtbetalingAndelFraForrigeBehandling(behandling: Behandling, forrigeBehandling: Behandling) {
        hentForBehandling(forrigeBehandling.id).forEach {
            endretUtbetalingAndelRepository.save(
                it.copy(
                    id = 0,
                    behandlingId = behandling.id,
                    andelTilkjentYtelser = mutableListOf()
                )
            )
        }
    }

    fun hentForBehandling(behandlingId: Long) = endretUtbetalingAndelRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun fjernKnytningTilAndelTilkjentYtelse(behandlingId: Long) {
        hentForBehandling(behandlingId).filter { it.andelTilkjentYtelser.isNotEmpty() }.forEach {
            it.andelTilkjentYtelser.clear()
        }
    }

    @Transactional
    fun oppdaterEndreteUtbetalingsandelerMedBegrunnelser(behandling: Behandling): MutableList<EndretUtbetalingAndel> {
        val endredeUtbetalingAndeler = endretUtbetalingAndelRepository.findByBehandlingId(behandling.id)
        val andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

        endredeUtbetalingAndeler.forEach {
            val utvidetScenario =
                andelTilkjentYtelser.hentUtvidetScenarioForEndringsperiode(it.periode)

            it.vedtakBegrunnelseSpesifikasjoner =
                listOf(
                    it.hentGyldigEndretBegrunnelse(
                        sanityService.hentSanityBegrunnelser(),
                        utvidetScenario,
                    )
                )
        }

        return endretUtbetalingAndelRepository.saveAll(endredeUtbetalingAndeler)
    }
}

private fun skalDeltBostedAndelerSlåsSammen(
    førsteAndel: AndelTilkjentYtelse,
    nesteAndel: AndelTilkjentYtelse
): Boolean =
    førsteAndel.stønadTom.sisteDagIInneværendeMåned()
        .erDagenFør(nesteAndel.stønadFom.førsteDagIInneværendeMåned())

// TODO: Denne er veeeldig lik den eksisterende funksjonen som gjør dette på AndelerTilkjentYtelse
private fun slåSammenDeltBostedPerioderSomIkkeSkulleHaVærtSplittet(
    perioder: MutableList<Periode>,
): MutableList<Periode> {
    val sortertePerioder = perioder.sortedBy { it.fom }.toMutableList()
    var periodenViSerPå: Periode = sortertePerioder.first()
    val oppdatertListeMedPerioder = mutableListOf<Periode>()

    for (index in 0 until sortertePerioder.size) {
        val periode = sortertePerioder[index]
        val nestePeriode = if (index == sortertePerioder.size - 1) null else sortertePerioder[index + 1]

        periodenViSerPå = if (nestePeriode != null) {
            val andelerSkalSlåsSammen =
                periode.tom.sisteDagIMåned().erDagenFør(nestePeriode.fom.førsteDagIInneværendeMåned())

            if (andelerSkalSlåsSammen) {
                val nyPeriode = periodenViSerPå.copy(tom = nestePeriode.tom)
                nyPeriode
            } else {
                oppdatertListeMedPerioder.add(periodenViSerPå)
                sortertePerioder[index + 1]
            }
        } else {
            oppdatertListeMedPerioder.add(periodenViSerPå)
            break
        }
    }
    return oppdatertListeMedPerioder
}
