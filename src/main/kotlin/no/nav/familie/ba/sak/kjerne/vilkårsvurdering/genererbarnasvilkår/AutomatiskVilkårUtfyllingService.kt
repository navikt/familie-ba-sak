package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.genererbarnasvilkår

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.REVURDERING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.SØKNAD
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.tilPerson
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.beskjærFraOgMed
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutomatiskVilkårUtfyllingService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val featureToggleService: FeatureToggleService,
) {
    @Transactional
    fun utfyllVilkårAutomatiskForNyeBarn(behandlingId: Long) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingId)

        validerAtBarnasVilkårKanAutomatiskUtfylles(behandling, vilkårsvurdering)

        val barnVilkårSkalGenereresFor = hentBarnVilkårSkalAutomatiskUtfyllesFor(behandling)
        val nyeVilkårResultaterTidslinje = genererSøkersVilkårResultatTidslinje(vilkårsvurdering)
        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId)

        val vilkårTyperSomSkalErstattes = setOf(BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER)

        vilkårsvurdering
            .personResultater
            .filter { it.aktør in barnVilkårSkalGenereresFor }
            .forEach { personResultat ->
                val barnetsFødselsdato = personResultat.aktør.tilPerson(persongrunnlag).fødselsdato
                val nyeVilkårResultatPerioder =
                    nyeVilkårResultaterTidslinje
                        .beskjærFraOgMed(barnetsFødselsdato)
                        .tilPerioderIkkeNull()

                val vilkårResultaterSomIkkeSkalOverskrives =
                    personResultat
                        .vilkårResultater
                        .filter { it.vilkårType !in vilkårTyperSomSkalErstattes }
                        .toSet()

                val nyeVilkårResultater =
                    vilkårTyperSomSkalErstattes.flatMap { vilkår ->
                        nyeVilkårResultatPerioder.map { vilkårResultatPeriode ->
                            VilkårResultat(
                                personResultat = personResultat,
                                vilkårType = vilkår,
                                resultat = vilkårResultatPeriode.verdi.resultat,
                                periodeFom = vilkårResultatPeriode.fom,
                                periodeTom = vilkårResultatPeriode.tom,
                                begrunnelse = "Kopiert fra søkers 'Bosatt i riket'-vilkår",
                                sistEndretIBehandlingId = behandlingId,
                                opprinneligKopiertFraVilkårResultat = vilkårResultatPeriode.verdi.id,
                            )
                        }
                    }

                personResultat.setSortedVilkårResultater(vilkårResultaterSomIkkeSkalOverskrives + nyeVilkårResultater)
            }
    }

    private fun genererSøkersVilkårResultatTidslinje(vilkårsvurdering: Vilkårsvurdering): Tidslinje<VilkårResultat> {
        val søkersPersonResultat =
            vilkårsvurdering.personResultater
                .firstOrNull { it.erSøkersResultater() }
                ?: throw Feil("Finner ikke søkers personresultat i vilkårsvurdering for behandling ${vilkårsvurdering.behandling.id}")

        val søkersBosattIRiketVilkårTidslinje =
            søkersPersonResultat
                .vilkårResultater
                .filter { it.vilkårType == BOSATT_I_RIKET }
                .tilTidslinje()

        val søkersBosattIRiketVilkårTidslinjeMedSammenslåttePerioder =
            søkersBosattIRiketVilkårTidslinje.slåSammenLikePerioder { vilkårResultat1, vilkårResultat2 ->
                vilkårResultat1?.resultat == vilkårResultat2?.resultat
            }

        return søkersBosattIRiketVilkårTidslinjeMedSammenslåttePerioder
    }

    private fun hentBarnVilkårSkalAutomatiskUtfyllesFor(behandling: Behandling): List<Aktør> {
        val forrigeVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)
        val barnVilkårSkalGenereresFor = persongrunnlagService.finnNyeBarn(behandling, forrigeVedtatteBehandling).map { it.aktør }
        if (barnVilkårSkalGenereresFor.isEmpty()) {
            throw Feil("Det finnes ingen nye barn i behandling ${behandling.id}")
        }
        return barnVilkårSkalGenereresFor
    }

    private fun validerAtBarnasVilkårKanAutomatiskUtfylles(
        behandling: Behandling,
        vilkårsvurdering: Vilkårsvurdering,
    ) {
        val søkersPersonResultat = vilkårsvurdering.personResultater.firstOrNull { it.erSøkersResultater() }
        if (søkersPersonResultat == null) {
            throw Feil("Finner ikke søkers personresultat i vilkårsvurdering for behandling ${vilkårsvurdering.behandling.id}")
        }
        if (søkersPersonResultat.vilkårResultater.any { it.resultat == Resultat.IKKE_VURDERT }) {
            throw FunksjonellFeil("Du må vurdere alle søkers vilkår før de kan kopieres til barna.")
        }
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_GENERERE_BARNAS_VILKÅR)) {
            throw Feil("Toggle for å generere barnas vilkår er skrudd av")
        }
        if (!(behandling.type == FØRSTEGANGSBEHANDLING || (behandling.type == REVURDERING && behandling.opprettetÅrsak == SØKNAD))) {
            throw Feil("Kan ikke generere barnas vilkår i behandling ${behandling.id} med type ${behandling.type}")
        }
        if (behandling.kategori != BehandlingKategori.EØS) {
            throw Feil("Kan ikke generere barnas vilkår i behandling ${behandling.id} med kategori ${behandling.kategori}")
        }
    }
}
