package io.github.javaflow.lambda;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MixableTest {

    Mixable<String, Double, Double> stringDoubleMix = (pi, r) -> Double.parseDouble(pi) * r * r;

    Source<String> numberStringSource;
    Source<Double> numberSource;

    @BeforeEach
    void setup() {
        numberStringSource = t -> String.valueOf(Math.PI);
        numberSource = t -> 2.0;
    }

    @Test
    void mix_ShouldGetMixedResult_WhenTwoSourcesMixing() {
        stringDoubleMix.mix(numberStringSource, numberSource).to(t -> {
            assertEquals(Math.PI * 4.0, t);
            return null;
        });
    }
}
