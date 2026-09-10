package com.neodent.dni;

import com.neodent.dni.dto.ApisNetDniResponse;
import com.neodent.dni.dto.DniResponse;
import com.neodent.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@Profile("!mock-dni")
public class ApisNetDniService implements DniService {

    private final RestClient restClient;

    public ApisNetDniService(
        RestClient.Builder restClientBuilder,
        @Value("${dni.api.base-url}") String baseUrl,
        @Value("${dni.api.token}") String token
    ) {
        this.restClient = restClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader(
                "Authorization",
                "Bearer " + token
            )
            .build();
    }

    @Override
    public DniResponse buscarPorDni(String dni) {

        validarDni(dni);

        try {

            ApisNetDniResponse response = restClient
                .get()
                .uri(uriBuilder ->
                    uriBuilder
                        .path("/v2/reniec/dni")
                        .queryParam("numero", dni)
                        .build()
                )
                .retrieve()
                .body(ApisNetDniResponse.class);

            if (response == null) {
                throw new BusinessException(
                    "No se obtuvo información para el DNI consultado"
                );
            }

            return new DniResponse(
                dni,
                response.nombres(),
                response.apellidoPaterno(),
                response.apellidoMaterno()
            );

        } catch (RestClientResponseException ex) {

            HttpStatusCode status = ex.getStatusCode();

            if (status.value() == 404) {
                throw new BusinessException(
                    "No se encontraron datos para el DNI ingresado"
                );
            }

            if (status.value() == 401 || status.value() == 403) {
                throw new BusinessException(
                    "El servicio de consulta DNI no está autorizado"
                );
            }

            if (status.value() == 429) {
                throw new BusinessException(
                    "Se alcanzó temporalmente el límite de consultas DNI"
                );
            }

            if (status.is5xxServerError()) {
                throw new BusinessException(
                    "El servicio de consulta DNI no está disponible temporalmente"
                );
            }

            throw new BusinessException(
                "No se pudo consultar el DNI"
            );

        } catch (ResourceAccessException ex) {

            throw new BusinessException(
                "No fue posible conectarse con el servicio de consulta DNI"
            );
        }
    }

    private void validarDni(String dni) {

        if (dni == null || !dni.matches("\\d{8}")) {
            throw new BusinessException(
                "El DNI debe contener exactamente 8 dígitos"
            );
        }
    }
}