package dev.projectsmonitor.ssrf

import dev.projectsmonitor.common.enums.HealthReason
import java.io.IOException

class RejectedAddressException(val reason: HealthReason, message: String) : IOException(message)
