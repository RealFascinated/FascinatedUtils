package cc.fascinated.fascinatedutils.gui.declare;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DeclarativeKeysTest {

    @Test
    void effectiveKeyPrefersExplicitKey() {
        Assertions.assertEquals("stable-id", DeclarativeKeys.effectiveKey("stable-id", 9));
    }

    @Test
    void effectiveKeyUsesIndexWhenUnset() {
        Assertions.assertEquals("__positional:4", DeclarativeKeys.effectiveKey(null, 4));
    }

    @Test
    void effectiveKeyUsesIndexWhenBlank() {
        Assertions.assertEquals("__positional:1", DeclarativeKeys.effectiveKey("", 1));
    }
}
