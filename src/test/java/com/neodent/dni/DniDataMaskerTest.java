package com.neodent.dni;

import com.neodent.dni.util.DniDataMasker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DniDataMaskerTest {

    @Test
    void debeEnmascararNombresMultiplesCorrectamente() {
        // Primer nombre completo, segundo/tercero primeras 3 letras + ***
        assertEquals("Percy Uli***", DniDataMasker.enmascararNombres("PERCY ULISES"));
        assertEquals("Juan Car*** Alb***", DniDataMasker.enmascararNombres("JUAN CARLOS ALBERTO"));
        assertEquals("Luis Fer*** Jav***", DniDataMasker.enmascararNombres("LUIS FERNANDO JAVIER"));
    }

    @Test
    void debeMantenerPrimerNombreUnico() {
        assertEquals("Maria", DniDataMasker.enmascararNombres("MARIA"));
        assertEquals("Pedro", DniDataMasker.enmascararNombres("PEDRO"));
    }

    @Test
    void debeSoportarNombresCompuestosConParticulas() {
        assertEquals("Maria del Car***", DniDataMasker.enmascararNombres("MARIA DEL CARMEN"));
    }

    @Test
    void debeCapitalizarApellidoPaternoCompleto() {
        assertEquals("Molina", DniDataMasker.capitalizarTexto("MOLINA"));
        assertEquals("De la Cruz", DniDataMasker.capitalizarTexto("DE LA CRUZ"));
        assertEquals("Del Solar", DniDataMasker.capitalizarTexto("DEL SOLAR"));
    }

    @Test
    void debeEnmascararApellidoMaternoConPrimerasDosLetrasYDosAsteriscos() {
        assertEquals("Al**", DniDataMasker.enmascararApellidoMaterno("ALVAREZ"));
        assertEquals("Qu**", DniDataMasker.enmascararApellidoMaterno("QUISPE"));
        assertEquals("De la To**", DniDataMasker.enmascararApellidoMaterno("DE LA TORRE"));
        assertEquals("Del So**", DniDataMasker.enmascararApellidoMaterno("DEL SOLAR"));
        assertNull(DniDataMasker.enmascararApellidoMaterno(null));
        assertNull(DniDataMasker.enmascararApellidoMaterno("   "));
    }
}
