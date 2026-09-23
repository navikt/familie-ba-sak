package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class VilkårvurdererTest {
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()

    private val vilkårvurderer =
        Vilkårvurderer(
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        )

    private val fagsak = lagFagsak()
    private val behandling = lagBehandling(fagsak = fagsak)

    @Test
    fun `skal returnere false når det ikke finnes noen tidligere vedtatt behandling`() {
        // Arrange
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null

        // Act
        val resultat =
            vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(
                behandling = behandling,
                barnaFraSøknad = listOf(tilfeldigPerson(fødselsdato = LocalDate.now(), personType = PersonType.BARN)),
            )

        // Assert
        assertThat(resultat).isFalse()
    }

    @Test
    fun `skal returnere false når forrige vedtatte behandling ikke har en aktiv vilkårsvurdering`() {
        // Arrange
        val forrigeBehandling = lagBehandling(fagsak = fagsak)
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns forrigeBehandling
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(forrigeBehandling.id) } returns null

        // Act
        val resultat =
            vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(
                behandling = behandling,
                barnaFraSøknad = listOf(tilfeldigPerson(fødselsdato = LocalDate.now(), personType = PersonType.BARN)),
            )

        // Assert
        assertThat(resultat).isFalse()
    }

    @Test
    fun `skal returnere true når søker har en oppfylt utvidet barnetrygd periode som overlapper med et av barna`() {
        // Arrange
        val forrigeBehandling = lagBehandling(fagsak = fagsak)
        val barnFødselsdato = LocalDate.now().minusYears(1)
        val barn = tilfeldigPerson(fødselsdato = barnFødselsdato, personType = PersonType.BARN)

        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = forrigeBehandling,
                lagPersonResultater = {
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = fagsak.aktør,
                            lagVilkårResultater = { personResultat ->
                                setOf(
                                    lagVilkårResultat(
                                        personResultat = personResultat,
                                        vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                                        resultat = Resultat.OPPFYLT,
                                        periodeFom = barnFødselsdato.minusMonths(1),
                                        periodeTom = barnFødselsdato.plusYears(2),
                                        behandlingId = forrigeBehandling.id,
                                    ),
                                )
                            },
                        ),
                    )
                },
            )

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns forrigeBehandling
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(forrigeBehandling.id) } returns vilkårsvurdering

        // Act
        val resultat =
            vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(
                behandling = behandling,
                barnaFraSøknad = listOf(barn),
            )

        // Assert
        assertThat(resultat).isTrue()
    }

    @Test
    fun `skal returnere true når perioden er løpende uten tom-dato og starter før barnet fyller 18 år`() {
        // Arrange
        val forrigeBehandling = lagBehandling(fagsak = fagsak)
        val barnFødselsdato = LocalDate.now().minusYears(1)
        val barn = tilfeldigPerson(fødselsdato = barnFødselsdato, personType = PersonType.BARN)

        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = forrigeBehandling,
                lagPersonResultater = {
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = fagsak.aktør,
                            lagVilkårResultater = { personResultat ->
                                setOf(
                                    lagVilkårResultat(
                                        personResultat = personResultat,
                                        vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                                        resultat = Resultat.OPPFYLT,
                                        periodeFom = barnFødselsdato.minusMonths(1),
                                        periodeTom = null,
                                        behandlingId = forrigeBehandling.id,
                                    ),
                                )
                            },
                        ),
                    )
                },
            )

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns forrigeBehandling
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(forrigeBehandling.id) } returns vilkårsvurdering

        // Act
        val resultat =
            vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(
                behandling = behandling,
                barnaFraSøknad = listOf(barn),
            )

        // Assert
        assertThat(resultat).isTrue()
    }

    @Test
    fun `skal returnere false når utvidet barnetrygd perioden ikke overlapper med noen av barna`() {
        // Arrange
        val forrigeBehandling = lagBehandling(fagsak = fagsak)
        val barnFødselsdato = LocalDate.now().minusYears(20)
        val barn = tilfeldigPerson(fødselsdato = barnFødselsdato, personType = PersonType.BARN)

        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = forrigeBehandling,
                lagPersonResultater = {
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = fagsak.aktør,
                            lagVilkårResultater = { personResultat ->
                                setOf(
                                    lagVilkårResultat(
                                        personResultat = personResultat,
                                        vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                                        resultat = Resultat.OPPFYLT,
                                        periodeFom = LocalDate.now().minusMonths(1),
                                        periodeTom = LocalDate.now().plusYears(1),
                                        behandlingId = forrigeBehandling.id,
                                    ),
                                )
                            },
                        ),
                    )
                },
            )

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns forrigeBehandling
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(forrigeBehandling.id) } returns vilkårsvurdering

        // Act
        val resultat =
            vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(
                behandling = behandling,
                barnaFraSøknad = listOf(barn),
            )

        // Assert
        assertThat(resultat).isFalse()
    }

    @Test
    fun `skal returnere false når det ikke finnes noen oppfylte utvidet barnetrygd perioder`() {
        // Arrange
        val forrigeBehandling = lagBehandling(fagsak = fagsak)
        val barnFødselsdato = LocalDate.now().minusYears(1)
        val barn = tilfeldigPerson(fødselsdato = barnFødselsdato, personType = PersonType.BARN)

        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = forrigeBehandling,
                lagPersonResultater = {
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = fagsak.aktør,
                            lagVilkårResultater = { personResultat ->
                                setOf(
                                    lagVilkårResultat(
                                        personResultat = personResultat,
                                        vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                                        resultat = Resultat.IKKE_OPPFYLT,
                                        periodeFom = barnFødselsdato.minusMonths(1),
                                        periodeTom = barnFødselsdato.plusYears(2),
                                        behandlingId = forrigeBehandling.id,
                                    ),
                                )
                            },
                        ),
                    )
                },
            )

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns forrigeBehandling
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(forrigeBehandling.id) } returns vilkårsvurdering

        // Act
        val resultat =
            vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(
                behandling = behandling,
                barnaFraSøknad = listOf(barn),
            )

        // Assert
        assertThat(resultat).isFalse()
    }

    @Test
    fun `skal kaste feil når en oppfylt utvidet barnetrygd periode mangler fom-dato`() {
        // Arrange
        val forrigeBehandling = lagBehandling(fagsak = fagsak)
        val barnFødselsdato = LocalDate.now().minusYears(1)
        val barn = tilfeldigPerson(fødselsdato = barnFødselsdato, personType = PersonType.BARN)

        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = forrigeBehandling,
                lagPersonResultater = {
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = fagsak.aktør,
                            lagVilkårResultater = { personResultat ->
                                setOf(
                                    lagVilkårResultat(
                                        personResultat = personResultat,
                                        vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                                        resultat = Resultat.OPPFYLT,
                                        periodeFom = null,
                                        periodeTom = barnFødselsdato.plusYears(2),
                                        behandlingId = forrigeBehandling.id,
                                    ),
                                )
                            },
                        ),
                    )
                },
            )

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns forrigeBehandling
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(forrigeBehandling.id) } returns vilkårsvurdering

        // Act & Assert
        val exception =
            assertThrows<Feil> {
                vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(
                    behandling = behandling,
                    barnaFraSøknad = listOf(barn),
                )
            }
        assertThat(exception.message).isEqualTo("f.o.m dato mangler for VilkårResultat=0")
    }
}
