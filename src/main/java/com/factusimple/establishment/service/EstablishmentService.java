package com.factusimple.establishment.service;

import com.factusimple.establishment.dto.EstablishmentDtos.CreateRequest;
import com.factusimple.establishment.dto.EstablishmentDtos.Response;
import com.factusimple.establishment.entity.Establishment;
import com.factusimple.establishment.mapper.EstablishmentMapper;
import com.factusimple.establishment.repository.EstablishmentRepository;
import com.factusimple.infrastructure.exception.DomainExceptions.ConflictException;
import com.factusimple.infrastructure.exception.DomainExceptions.NotFoundException;
import com.factusimple.infrastructure.security.CurrentUser;
import com.factusimple.user.entity.User;
import com.factusimple.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstablishmentService {

    private final EstablishmentRepository establishmentRepository;
    private final UserRepository userRepository;
    private final EstablishmentMapper mapper;
    private final CurrentUser currentUser;

    public EstablishmentService(EstablishmentRepository establishmentRepository,
                                UserRepository userRepository,
                                EstablishmentMapper mapper,
                                CurrentUser currentUser) {
        this.establishmentRepository = establishmentRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    /** Crea el establecimiento del usuario actual y se lo asocia (un tenant por usuario). */
    @Transactional
    public Response createForCurrentUser(CreateRequest request) {
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Usuario no encontrado"));
        if (user.getEstablishment() != null) {
            throw new ConflictException("ESTABLISHMENT_ALREADY_EXISTS",
                    "El usuario ya tiene un establecimiento");
        }
        Establishment est = new Establishment();
        est.setName(request.name());
        est.setIdentification(request.identification());
        est.setDv(request.dv());
        est.setAddress(request.address());
        est.setPhone(request.phone());
        est.setEmail(request.email());
        est.setMunicipalityCode(request.municipalityCode());
        est.setNumberingRangeId(request.numberingRangeId());
        est = establishmentRepository.save(est);

        user.setEstablishment(est);
        userRepository.save(user);
        return mapper.toResponse(est);
    }

    @Transactional(readOnly = true)
    public Response getMine() {
        Establishment est = establishmentRepository.findById(currentUser.establishmentId())
                .orElseThrow(() -> new NotFoundException("ESTABLISHMENT_NOT_FOUND",
                        "Establecimiento no encontrado"));
        return mapper.toResponse(est);
    }
}
