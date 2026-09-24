package no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.datagenerator.tilfeldigSøker
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FiltreringsreglerFaktaFødselshendelse
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FiltreringResultatTest {
    @Nested
    inner class Opprett {
        private val behandlingId = 1L

        private val fakta =
            FiltreringsreglerFaktaFødselshendelse(
                søker = tilfeldigSøker(personType = PersonType.SØKER),
                søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                barnaSomSkalVurderes = listOf(tilfeldigPerson(personType = PersonType.BARN)),
                søkerLever = true,
                barnaLever = true,
                søkerHarVerge = false,
                løperBarnetrygdForBarnetPåAnnenForelder = false,
                restenAvBarna = emptyList(),
                erFagsakenMigrertEtterBarnFødt = false,
                morHarIkkeOpphørtBarnetrygd = true,
            )

        @Test
        fun `skal opprette for fakta og oppfylt evaluering`() {
            // Arrange
            val evaluering =
                Evaluering
                    .oppfylt(Filtreringsregel.Identifikator.MOR_LEVER.oppfylt)
                    .copy(identifikator = Filtreringsregel.Identifikator.MOR_LEVER.name)

            // Act
            val filtreringResultat = FiltreringResultat.opprett(behandlingId, fakta, evaluering)

            // Assert
            assertThat(filtreringResultat.behandlingId).isEqualTo(behandlingId)
            assertThat(filtreringResultat.filtreringsregel).isEqualTo(Filtreringsregel.Identifikator.MOR_LEVER)
            assertThat(filtreringResultat.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(filtreringResultat.begrunnelse).isEqualTo(evaluering.begrunnelse)
            assertThat(filtreringResultat.evalueringsårsaker).containsExactly(
                Filtreringsregel.Identifikator.MOR_LEVER.oppfylt
                    .hentNavn(),
            )
            assertThat(filtreringResultat.regelInput).isEqualTo(fakta.convertDataClassToJson())
        }

        @Test
        fun `skal opprette for fakta og ikke oppfylt evaluering`() {
            // Arrange
            val evaluering =
                Evaluering
                    .ikkeOppfylt(Filtreringsregel.Identifikator.BARN_LEVER.ikkeOppfylt)
                    .copy(identifikator = Filtreringsregel.Identifikator.BARN_LEVER.name)

            // Act
            val filtreringResultat = FiltreringResultat.opprett(behandlingId, fakta, evaluering)

            // Assert
            assertThat(filtreringResultat.filtreringsregel).isEqualTo(Filtreringsregel.Identifikator.BARN_LEVER)
            assertThat(filtreringResultat.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
            assertThat(filtreringResultat.evalueringsårsaker).containsExactly(
                Filtreringsregel.Identifikator.BARN_LEVER.ikkeOppfylt
                    .hentNavn(),
            )
        }

        @Test
        fun `skal samle navn fra alle evalueringsårsaker`() {
            // Arrange
            val evaluering =
                Evaluering(
                    resultat = Resultat.OPPFYLT,
                    evalueringÅrsaker =
                        listOf(
                            Filtreringsregel.Identifikator.MOR_LEVER.oppfylt,
                            Filtreringsregel.Identifikator.BARN_LEVER.oppfylt,
                        ),
                    begrunnelse = "Flere årsaker oppfylt",
                    identifikator = Filtreringsregel.Identifikator.MOR_LEVER.name,
                )

            // Act
            val filtreringResultat = FiltreringResultat.opprett(behandlingId, fakta, evaluering)

            // Assert
            assertThat(filtreringResultat.evalueringsårsaker)
                .containsExactly(
                    Filtreringsregel.Identifikator.MOR_LEVER.oppfylt
                        .hentNavn(),
                    Filtreringsregel.Identifikator.BARN_LEVER.oppfylt
                        .hentNavn(),
                )
        }
    }
}
