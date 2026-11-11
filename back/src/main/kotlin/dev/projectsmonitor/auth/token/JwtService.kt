package dev.projectsmonitor.auth.token

import dev.projectsmonitor.auth.CurrentUser
import dev.projectsmonitor.auth.Role
import dev.projectsmonitor.config.AppProperties
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

data class IssuedToken(val token: String, val user: CurrentUser)

@Service
class JwtService(private val jwtEncoder: JwtEncoder, private val properties: AppProperties) {

    fun issue(subject: String, role: Role, sessionId: String? = null): IssuedToken {
        val issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val ttl = if (role == Role.ADMIN) properties.jwt.adminTtl else properties.jwt.guestTtl
        val expiresAt = issuedAt.plus(ttl)
        val claims = JwtClaimsSet.builder()
            .subject(subject)
            .claim(CurrentUser.ROLE_CLAIM, role.name)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .apply { sessionId?.let { claim(AdminSession.SESSION_CLAIM, it) } }
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
        return IssuedToken(token, CurrentUser(subject, role, expiresAt))
    }
}
