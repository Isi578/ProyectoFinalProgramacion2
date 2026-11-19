import org.junit.jupiter.api.Test;

/** Ejecuta la batería de integración con Maven/JUnit en el directorio del proyecto. */
class HotelUQIntegrationSmokeTest {
    @Test
    void verificaComprasReservasPuntosInformesYPrecarga() throws Exception {
        HotelUQIntegrationSmoke.passed = 0;
        HotelUQIntegrationSmoke.main(new String[0]);
    }
}