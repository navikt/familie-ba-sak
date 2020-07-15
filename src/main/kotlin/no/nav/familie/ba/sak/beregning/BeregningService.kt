package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatRepository
import no.nav.familie.ba.sak.behandling.vilkår.SakType.Companion.hentSakType
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class BeregningService(
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
        private val fagsakService: FagsakService,
        private val søknadGrunnlagService: SøknadGrunnlagService,
        private val tilkjentYtelseRepository: TilkjentYtelseRepository,
        private val behandlingResultatRepository: BehandlingResultatRepository,
        private val behandlingRepository: BehandlingRepository
) {


    fun hentAndelerTilkjentYtelseForFagsak(fagsakId: Long): List<AndelTilkjentYtelse> {
        val behandlinger = behandlingRepository.finnBehandlinger(fagsakId)
        return andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(behandlinger.map { it.id }.toList())
    }

    fun hentAndelerTilkjentYtelseForBehandling(behandlingId: Long): List<AndelTilkjentYtelse> {
        return andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandlingId))
    }

    fun lagreOppdaterteAndelerTilkjentYtelse(andeler: List<AndelTilkjentYtelse>) {
        andelTilkjentYtelseRepository.saveAll(andeler)
    }

    fun hentTilkjentYtelseForBehandling(behandlingId: Long): TilkjentYtelse {
        return tilkjentYtelseRepository.findByBehandling(behandlingId)
    }

    @Transactional
    fun oppdaterBehandlingMedBeregning(behandling: Behandling,
                                       personopplysningGrunnlag: PersonopplysningGrunnlag): Ressurs<RestFagsak> {

        andelTilkjentYtelseRepository.slettAlleAndelerTilkjentYtelseForBehandling(behandling.id)
        tilkjentYtelseRepository.slettTilkjentYtelseFor(behandling)
        val behandlingResultat = behandlingResultatRepository.findByBehandlingAndAktiv(behandling.id)
                                 ?: throw IllegalStateException("Kunne ikke hente behandlingsresultat for behandling med id ${behandling.id}")

        val søknadDTO = søknadGrunnlagService.hentAktiv(behandlingResultat.behandling.id)?.hentSøknadDto()
        val sakType = hentSakType(behandlingKategori = behandling.kategori, søknadDTO = søknadDTO)

        val tilkjentYtelse = TilkjentYtelseUtils
                .beregnTilkjentYtelse(behandlingResultat, sakType, personopplysningGrunnlag)

        tilkjentYtelseRepository.save(tilkjentYtelse)

        return fagsakService.hentRestFagsak(behandling.fagsak.id)
    }

    fun oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling: Behandling,
                                                    utbetalingsoppdrag: Utbetalingsoppdrag): TilkjentYtelse {

        val nyTilkjentYtelse = populerTilkjentYtelse(behandling, utbetalingsoppdrag)
        return tilkjentYtelseRepository.save(nyTilkjentYtelse)
    }

    private fun populerTilkjentYtelse(behandling: Behandling,
                                      utbetalingsoppdrag: Utbetalingsoppdrag): TilkjentYtelse {
        val erRentOpphør = utbetalingsoppdrag.utbetalingsperiode.all { it.opphør != null }
        var opphørsdato: LocalDate? = null
        if (erRentOpphør) {
            opphørsdato = utbetalingsoppdrag.utbetalingsperiode.map { it.opphør!!.opphørDatoFom }.sorted().first()
        }

        if (behandling.type == BehandlingType.REVURDERING) {
            val opphørPåRevurdering = utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør != null }
            if (opphørPåRevurdering.isNotEmpty()) {
                opphørsdato = opphørPåRevurdering.maxBy { it.opphør!!.opphørDatoFom }!!.opphør!!.opphørDatoFom
            }
        }

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandling.id)
        return tilkjentYtelse.apply {
            this.utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag)
            this.stønadTom = utbetalingsoppdrag.utbetalingsperiode.maxBy { it.vedtakdatoTom }!!.vedtakdatoTom
            this.stønadFom = if (erRentOpphør) null else utbetalingsoppdrag.utbetalingsperiode
                    .filter { !it.erEndringPåEksisterendePeriode } // TODO: Hva er dette for? Vil aldri finnes hvis ikke opphør?
                    .minBy { it.vedtakdatoFom }!!.vedtakdatoFom
            this.endretDato = LocalDate.now()
            this.opphørFom = opphørsdato
        }
    }
}

