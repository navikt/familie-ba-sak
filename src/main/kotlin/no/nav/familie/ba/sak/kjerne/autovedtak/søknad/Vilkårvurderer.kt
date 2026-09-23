package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class Vilkårvurderer(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {
    fun oppfyllerSøkerVilkårForUtvidetBarnetrygd(
        behandling: Behandling,
        barnaFraSøknad: List<Person>,
    ): Boolean {
        val forrigeVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)
        if (forrigeVedtatteBehandling == null) {
            return false
        }

        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(forrigeVedtatteBehandling.id)
        if (vilkårsvurdering == null) {
            return false
        }

        val søkersVilkårResultater = vilkårsvurdering.personResultater.single { it.erSøkersResultater() }.vilkårResultater

        return søkersVilkårResultater
            .filter { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
            .filter { it.erOppfylt() }
            .map { Periode.fraVilkårResultat(it) }
            .any { periode -> barnaFraSøknad.any { barn -> periode.overlapperMedBarnFraSøknad(barn) } }
    }

    private data class Periode(
        val fom: LocalDate,
        val tom: LocalDate?,
    ) {
        fun overlapperMedBarnFraSøknad(barnFraSøknad: Person): Boolean {
            val periodenStarterFørBarnetFyller18 = fom.isBefore(barnFraSøknad.fødselsdato.plusYears(18))
            val periodenSlutterEtterFødselsdatoen = tom?.isAfter(barnFraSøknad.fødselsdato) ?: true
            return periodenStarterFørBarnetFyller18 && periodenSlutterEtterFødselsdatoen
        }

        companion object {
            fun fraVilkårResultat(resultat: VilkårResultat): Periode {
                val fom = resultat.periodeFom
                val tom = resultat.periodeTom
                if (fom == null) {
                    throw Feil("f.o.m dato mangler for VilkårResultat=${resultat.id}")
                }
                return Periode(fom, tom)
            }
        }
    }
}
