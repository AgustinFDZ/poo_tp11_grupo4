package ar.edu.unju.fi.poo.tp10.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base de los tests que usan la base MySQL real (tp10_ventas).
 *
 * Cada test corre dentro de una transacción que se revierte al terminar (@Transactional),
 * así que los datos que crea no quedan guardados en la base.
 */
@SpringBootTest
@AutoConfigureTestEntityManager
@ActiveProfiles("test")
@Transactional
public abstract class AbstractDbTest {

    @Autowired
    protected TestEntityManager em;

    /**
     * Envía a MySQL los INSERT/UPDATE pendientes y vacía la caché de Hibernate,
     * de modo que la próxima lectura venga realmente de la base.
     */
    protected void sincronizarYLimpiar() {
        em.flush();
        em.clear();
    }
}
