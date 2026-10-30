package com.sarani.carobd.core.model

/** Read-only export of an already captured report. No repository, command, import or transport access. */
object DiagnosticReportExport {
    const val SCHEMA_VERSION = 1

    fun toJson(report: DiagnosticSessionReport): String {
        val outcomes = report.outcomes.joinToString(",\n") { row ->
            val codes = row.codes.joinToString(",") { code ->
                """{"code":${jsonStringLiteral(code.code)},"category":${jsonStringLiteral(code.category.name)},"responder":${jsonStringLiteral(code.responder)}}"""
            }
            val raw = row.rawLines.joinToString(",", "[", "]", transform = ::jsonStringLiteral)
            """    {"request":${jsonStringLiteral(row.request)},"responder":${jsonStringLiteral(row.responder)},"outcome":${jsonStringLiteral(row.outcome.name)},"codes":[$codes],"rawLines":$raw,"detail":${jsonStringLiteral(row.detail)}}"""
        }
        return """{
  "schemaVersion":$SCHEMA_VERSION,
  "kind":"CAROBD_DTC_REPORT",
  "id":${jsonStringLiteral(report.id)},
  "scope":${jsonStringLiteral(report.scope)},
  "source":${jsonStringLiteral(if (report.simulator) "SIMULATOR" else "PHYSICAL_READ_ONLY")},
  "startedAtMs":${report.startedAtMs},
  "finishedAtMs":${report.finishedAtMs},
  "connectionSessionId":${jsonStringLiteral(report.connectionSessionId)},
  "vehicle":{"id":${jsonStringLiteral(report.vehicleId)},"label":${jsonStringLiteral(report.vehicleLabel)}},
  "connectionProfile":{"id":${jsonStringLiteral(report.profileId)},"label":${jsonStringLiteral(report.profileLabel)}},
  "observedProtocol":${jsonStringLiteral(report.observedProtocol)},
  "adapterReportedIdentity":${jsonStringLiteral(report.adapterIdentity)},
  "adapterAuthenticity":"NOT_VERIFIED",
  "completeness":${jsonStringLiteral(report.completeness.name)},
  "categorizedCodeCount":${report.codes.size},
  "identifiedResponderCount":${report.responders.size},
  "unknownRespondersExcludedFromCount":true,
  "healthAssessment":"NOT_PERFORMED",
  "vehicleValidation":"NOT_ESTABLISHED_BY_REPORT",
  "notCollected":["READINESS_MIL","MODE_02","MODE_09","MODE_06"],
  "outcomes":[
$outcomes
  ]
}"""
    }
}
