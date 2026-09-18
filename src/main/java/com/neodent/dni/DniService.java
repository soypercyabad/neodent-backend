package com.neodent.dni;

import com.neodent.dni.dto.DniResponse;

public interface DniService {

    DniResponse buscarPorDni(String dni);
}