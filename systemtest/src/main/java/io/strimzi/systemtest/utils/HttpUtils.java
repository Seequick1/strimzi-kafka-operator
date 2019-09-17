/*
 * Copyright 2019, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.utils;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.strimzi.systemtest.Constants;
import io.strimzi.test.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class HttpUtils {

    private static final Logger LOGGER = LogManager.getLogger(HttpUtils.class);

    private HttpUtils() { }

    public static void waitUntilServiceNameIsReady(String baserURI, String serviceName) {
        LOGGER.info("Waiting till service name {} is present in json", serviceName);
        TestUtils.waitFor("", Constants.GLOBAL_TRACING_POLL, Constants.GLOBAL_TRACING_TIMEOUT,
            () -> {
                Response response = given()
                        .when()
                            .baseUri(baserURI)
                            .relaxedHTTPSValidation()
                            .contentType("application/json")
                            .get("/jaeger/api/services");

                return response.body().peek().print().contains(serviceName);
            });
        LOGGER.info("Service name {} is present", serviceName);
    }

    public static void waitUntilServiceNameIsReady(String baserURI, String... serviceNames) {
        for (String serviceName : serviceNames) {
            waitUntilServiceNameIsReady(baserURI, serviceName);
        }
    }

    public static void waitUntilServiceHasSomeTraces(String baseURI, String serviceName) {
        LOGGER.info("Waiting till service {} has some traces", serviceName);
        TestUtils.waitFor("", Constants.GLOBAL_TRACING_POLL, Constants.GLOBAL_TRACING_TIMEOUT,
            () -> {
                Response response = given()
                            .when()
                                .baseUri(baseURI)
                                .relaxedHTTPSValidation()
                                .contentType("application/json")
                                .get("/jaeger/api/traces?service=" + serviceName);

                JsonPath jsonPathValidator = response.jsonPath();
                Map<Object, Object> data = jsonPathValidator.getMap("$");
                return data.size() > 0;
            });
        LOGGER.info("Service {} has traces", serviceName);
    }

    public static void waitUntilServiceHasSomeTraces(String baseURI, String... serviceNames) {
        for (String serviceName : serviceNames) {
            waitUntilServiceHasSomeTraces(baseURI, serviceName);
        }
    }
}