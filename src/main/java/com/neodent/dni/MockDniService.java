package com.neodent.dni;

import com.neodent.dni.dto.DniResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock-dni")
public class MockDniService implements DniService {

    @Override
    public DniResponse buscarPorDni(String dni) {

        return new DniResponse(
            dni,
            "JUAN CARLOS",
            "PEREZ",
            "RAMOS"
        );
    }
}