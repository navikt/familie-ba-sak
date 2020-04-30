package no.nav.familie.ba.sak.dokument.domene

import no.nav.familie.ba.sak.behandling.restDomene.DocFormat
import javax.validation.constraints.NotEmpty

class DokumentRequest(@field:NotEmpty val docFormat: DocFormat,
                      val templateContent: String?,
                      val precompiled: Boolean,
                      val mergeFields: String?,
                      val includeHeader: Boolean,
                      val headerFields: String?)

