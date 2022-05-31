package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Uendelighet
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/test/vilkaarsvurdering")
@ProtectedWithClaims(issuer = "azuread")
@Validated
@Profile("!prod")
class VilkårsvurderingTestController(
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val persongrunnlagService: PersongrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val aktørIdRepository: AktørIdRepository,
) {

    @PostMapping()
    fun opprettBehandlingMedVilkårsvurdering(
        @RequestBody personresultater: List<TestPersonResultat>
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val personer = personresultater.tilPersoner()
            .map { it.copy(aktør = aktørIdRepository.saveAndFlush(it.aktør)) }

        val søker = personer.first { it.type == PersonType.SØKER }
        val barn = personer.filter { it.type == PersonType.BARN }

        fagsakService.hentEllerOpprettFagsak(søker.aktør.aktivFødselsnummer())

        val behandling = behandlingService.opprettBehandling(
            NyBehandling(
                kategori = BehandlingKategori.EØS,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                søkersIdent = søker.aktør.aktivFødselsnummer(),
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                søknadMottattDato = LocalDate.now(),
                barnasIdenter = barn.map { it.aktør.aktivFødselsnummer() }
            )
        )
        
        // Registrere persongrunnlag fra søknad
        val persongrunnlag = persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
            behandling = behandling,
            aktør = søker.aktør,
            barnFraInneværendeBehandling = barn.map { it.aktør },
            målform = Målform.NB
        )

        // Opprett og lagre vilkårsvurdering
        val vilkårsvurdering = personresultater.tilVilkårsvurdering(
            behandling,
            personer.map { p -> persongrunnlag.personer.first { lagret -> lagret.aktør.aktivFødselsnummer() == p.aktør.aktivFødselsnummer() } }
        )

        vilkårsvurderingService.lagreInitielt(
            vilkårsvurdering
        )

        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandling.id)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }
}

data class TestVilkårsvurdering(
    val personresultater: List<TestPersonResultat>
)

data class TestPersonResultat(
    val startTidspunkt: LocalDate,
    val vilkårsresultater: List<TestVilkårResult>
)

data class TestVilkårResult(
    val tidslinje: String,
    val vilkår: Vilkår
)

private fun Iterable<TestPersonResultat>.tilPersoner(): List<Person> {
    val personer = this.mapIndexed() { indeks, personresultat ->
        when (indeks) {
            0 -> tilfeldigPerson(personType = PersonType.SØKER)
            else -> tilfeldigPerson(personType = PersonType.BARN)
        }
    }
    return personer
}

fun Iterable<TestPersonResultat>.tilVilkårsvurdering(
    behandling: Behandling,
    personer: List<Person>
): Vilkårsvurdering {

    val builder = VilkårsvurderingBuilder<Måned>(behandling)

    this.forEachIndexed { indeks, personresultat ->
        val person = personer[indeks]

        val personBuilder =
            builder.forPerson(person, MånedTidspunkt(personresultat.startTidspunkt.toYearMonth(), Uendelighet.INGEN))

        personresultat.vilkårsresultater.forEach { vilkårresultat ->
            personBuilder.medVilkår(vilkårresultat.tidslinje, vilkårresultat.vilkår)
        }

        personBuilder.byggPerson()
    }

    return builder.byggVilkårsvurdering()
}
