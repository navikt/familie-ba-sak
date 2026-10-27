package no.nav.familie.ba.sak.internal.tabulærtest

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class GenererTabulærTestService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val vilkårService: VilkårService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingRepository: EndretUtbetalingAndelRepository,
    private val kompetanseRepository: KompetanseRepository,
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
    private val valutakursRepository: ValutakursRepository,
    private val søknadGrunnlagService: SøknadGrunnlagService,
) {
    fun genererTestMedAlleGrunnlagsdata(behandlingId: Long): String =
        genererTest(
            GenererTabulærTestDto(
                behandlingId = behandlingId,
                inkluderKompetanser = true,
                inkluderUtenlandskePeriodebeløp = true,
                inkluderValutakurser = true,
            ),
        )

    fun genererTest(genererTabulærTestDto: GenererTabulærTestDto): String {
        val behandlingId = genererTabulærTestDto.behandlingId
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val forrigeBehandling =
            if (genererTabulærTestDto.inkluderForrigeBehandling) {
                behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
            } else {
                null
            }
        val persongrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)!!
        val persongrunnlagForrigeBehandling = forrigeBehandling?.let { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it.id)!! }
        val aktørIder =
            (persongrunnlag.personer + persongrunnlagForrigeBehandling?.personer.orEmpty())
                .map { it.aktør.aktørId }
                .distinct()
        val test =
            buildString {
                append(hentTekstForBakgrunn())
                append(hentTekstForFagsak(behandling))
                append(hentTekstForBehandlinger(behandling, forrigeBehandling))
                append(hentTekstForPersongrunnlag(persongrunnlag, persongrunnlagForrigeBehandling))

                append(hentTekstForScenario())

                if (genererTabulærTestDto.inkluderDagensDato) {
                    append(hentTekstForDagensDato(LocalDate.now()))
                }
                if (genererTabulærTestDto.inkluderPersonerFremstiltKravFor) {
                    append(
                        hentTekstForPersonerFremstiltKravFor(
                            behandlingId = behandlingId,
                            personerFremstiltKravFor = søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling, forrigeBehandling),
                        ),
                    )
                }
                if (genererTabulærTestDto.inkluderVilkårsvurdering) {
                    append(lagPersonresultaterTekst(forrigeBehandling))
                    append(lagPersonresultaterTekst(behandling))
                    forrigeBehandling?.let {
                        append(
                            hentTekstForVilkårresultater(
                                personResultater = vilkårService.hentVilkårsvurderingThrows(it.id).personResultater,
                                behandlingId = it.id,
                                persongrunnlag = persongrunnlagForrigeBehandling!!,
                            ),
                        )
                    }
                    append(
                        hentTekstForVilkårresultater(
                            personResultater = vilkårService.hentVilkårsvurderingThrows(behandlingId).personResultater,
                            behandlingId = behandlingId,
                            persongrunnlag = persongrunnlag,
                        ),
                    )
                }
                if (genererTabulærTestDto.inkluderKompetanser) {
                    append(
                        hentTekstForKompetanse(
                            kompetanse = kompetanseRepository.finnFraBehandlingId(behandlingId),
                            kompetanseForrigeBehandling = forrigeBehandling?.let { kompetanseRepository.finnFraBehandlingId(it.id) },
                        ),
                    )
                }
                if (genererTabulærTestDto.inkluderUtenlandskePeriodebeløp) {
                    append(
                        hentTekstForUtenlandskPeriodebeløp(
                            utenlandskPeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId),
                            utenlandskPeriodebeløpForrigeBehandling = forrigeBehandling?.let { utenlandskPeriodebeløpRepository.finnFraBehandlingId(it.id) },
                        ),
                    )
                }
                if (genererTabulærTestDto.inkluderValutakurser) {
                    append(
                        hentTekstForValutakurser(
                            valutakurser = valutakursRepository.finnFraBehandlingId(behandlingId),
                            valutakurserForrigeBehandling = forrigeBehandling?.let { valutakursRepository.finnFraBehandlingId(it.id) },
                        ),
                    )
                }
                if (genererTabulærTestDto.inkluderEndredeUtbetalinger) {
                    append(
                        hentTekstForEndretUtbetaling(
                            endredeUtbetalinger = endretUtbetalingRepository.findByBehandlingId(behandlingId),
                            endredeUtbetalingerForrigeBehandling = forrigeBehandling?.let { endretUtbetalingRepository.findByBehandlingId(it.id) },
                        ),
                    )
                }
                if (genererTabulærTestDto.inkluderAndelerTilkjentYtelse) {
                    append(
                        hentTekstForTilkjentYtelse(
                            andeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId),
                            persongrunnlag = persongrunnlag,
                            andelerForrigeBehandling = forrigeBehandling?.let { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(it.id) },
                            persongrunnlagForrigeBehandling = persongrunnlagForrigeBehandling,
                        ),
                    )
                }
            }
        val testMedAnonymeAktørIder =
            aktørIder.foldIndexed(test) { indeks, tekst, aktørId ->
                tekst.replace(aktørId, (indeks + 1).toString())
            }
        return listOfNotNull(forrigeBehandling?.id, behandling.id).foldIndexed(testMedAnonymeAktørIder) { indeks, tekst, id ->
            tekst.replace(id.toString(), (indeks + 1).toString())
        }
    }
}
