package no.nav.familie.ba.sak.ekstern.bisys

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BisysService(
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val personopplysningerService: PersonopplysningerService,
    private val fagsakPersonRepository: FagsakPersonRepository,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun hentUtvidetBarnetrygd(personIdent: String, fraDato: LocalDate): BisysUtvidetBarnetrygdResponse {
        val folkeregisteridenter = personopplysningerService.hentIdenter(Ident(personIdent)).filter {
            it.gruppe == "FOLKEREGISTERIDENT"
        }.map { it.ident }

        val samledeUtvidetBarnetrygdPerioder = mutableListOf<UtvidetBarnetrygdPeriode>()
        samledeUtvidetBarnetrygdPerioder.addAll(hentBisysPerioderFraInfotrygd(folkeregisteridenter, fraDato))
        samledeUtvidetBarnetrygdPerioder.addAll(hentBisysPerioderFraBaSak(folkeregisteridenter, fraDato))

        val sammenslåttePerioder =
            samledeUtvidetBarnetrygdPerioder.filter { it.stønadstype == BisysStønadstype.UTVIDET }
                .groupBy { it.beløp }.values
                .flatMap(::slåSammenSammenhengendePerioder).toMutableList()

        sammenslåttePerioder.addAll(
            samledeUtvidetBarnetrygdPerioder.filter { it.stønadstype == BisysStønadstype.SMÅBARNSTILLEGG }
                .groupBy { it.beløp }.values
                .flatMap(::slåSammenSammenhengendePerioder)
        )

        return BisysUtvidetBarnetrygdResponse(
            sammenslåttePerioder.sortedWith(
                compareBy(
                    { it.stønadstype },
                    { it.fomMåned })
            )
        )
    }

    private fun hentBisysPerioderFraInfotrygd(
        personIdenter: List<String>,
        fraDato: LocalDate
    ): List<UtvidetBarnetrygdPeriode> {
        return personIdenter.flatMap {
            infotrygdBarnetrygdClient.hentUtvidetBarnetrygd(it, fraDato.toYearMonth()).perioder
        }
    }

    private fun hentBisysPerioderFraBaSak(
        personIdenter: List<String>,
        fraDato: LocalDate
    ): List<UtvidetBarnetrygdPeriode> {
        val fagsak = fagsakPersonRepository.finnFagsak(personIdenter.map { PersonIdent(ident = it) }.toSet())
        val behandling = fagsak?.let { behandlingService.hentSisteBehandlingSomErIverksatt(it.id) }
        if (fagsak == null || behandling == null) {
            return emptyList()
        }

        val allePerioder =
            tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id)?.andelerTilkjentYtelse
                ?.filter { it.erUtvidet() || it.erSmåbarnstillegg() }
                ?.filter {
                    it.stønadTom.isSameOrAfter(fraDato.toYearMonth())
                }
                ?.map {
                    UtvidetBarnetrygdPeriode(
                        stønadstype = BisysStønadstype.UTVIDET,
                        fomMåned = it.stønadFom,
                        tomMåned = it.stønadTom,
                        beløp = it.sats.toDouble(),
                        manueltBeregnet = false
                    )
                } ?: emptyList()

        return allePerioder
    }

    private fun slåSammenSammenhengendePerioder(utbetalingerAvEtGittBeløp: List<UtvidetBarnetrygdPeriode>)
        : List<UtvidetBarnetrygdPeriode> {
        return utbetalingerAvEtGittBeløp.sortedBy { it.fomMåned }
            .fold(mutableListOf()) { sammenslåttePerioder, nesteUtbetaling ->
                if (sammenslåttePerioder.lastOrNull()?.tomMåned == nesteUtbetaling.fomMåned.minusMonths(1)
                    && sammenslåttePerioder.lastOrNull()?.manueltBeregnet == nesteUtbetaling.manueltBeregnet
                ) {
                    sammenslåttePerioder.apply { add(removeLast().copy(tomMåned = nesteUtbetaling.tomMåned)) }
                } else sammenslåttePerioder.apply { add(nesteUtbetaling) }
            }
    }
}
