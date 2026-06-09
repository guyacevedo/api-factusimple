package com.factusimple.infrastructure.security;

import com.factusimple.infrastructure.exception.DomainExceptions.ForbiddenException;
import com.factusimple.infrastructure.exception.DomainExceptions.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Acceso al usuario autenticado y su tenant (establecimiento). */
@Component
public class CurrentUser {

    public AppUserPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new UnauthorizedException("UNAUTHENTICATED", "No autenticado");
        }
        return principal;
    }

    public UUID id() {
        return principal().getId();
    }

    /** Tenant del solicitante; falla si aún no ha creado su establecimiento. */
    public UUID establishmentId() {
        UUID est = principal().getEstablishmentId();
        if (est == null) {
            throw new ForbiddenException("NO_ESTABLISHMENT",
                    "Debe crear un establecimiento antes de facturar");
        }
        return est;
    }
}
