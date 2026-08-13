package com.selfcare.qa.karate;

import com.intuit.karate.junit5.Karate;

class KarateTest {

    @Karate.Test
    Karate apiContracts() {
        return Karate.run("health", "loyalty").relativeTo(getClass());
    }
}
