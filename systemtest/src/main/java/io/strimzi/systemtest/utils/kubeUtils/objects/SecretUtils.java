/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.utils.kubeUtils.objects;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.strimzi.systemtest.Constants;
import io.strimzi.test.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.strimzi.test.k8s.KubeClusterResource.cmdKubeClient;
import static io.strimzi.test.k8s.KubeClusterResource.kubeClient;

public class SecretUtils {

    private static final Logger LOGGER = LogManager.getLogger(SecretUtils.class);

    private SecretUtils() { }

    public static void waitForSecretReady(String secretName) {
        LOGGER.info("Waiting for Kafka user secret {}", secretName);
        TestUtils.waitFor("Expected secret " + secretName + " exists", Constants.POLL_INTERVAL_FOR_RESOURCE_READINESS, Constants.TIMEOUT_FOR_SECRET_CREATION,
            () -> kubeClient().getSecret(secretName) != null);
        LOGGER.info("Kafka user secret {} created", secretName);
    }

    public static void createSecret(String secretName, String dataKey, String dataValue) {
        LOGGER.info("Creating secret {}", secretName);
        kubeClient().createSecret(new SecretBuilder()
                .withNewApiVersion("v1")
                .withNewKind("Secret")
                .withNewMetadata()
                    .withName(secretName)
                .endMetadata()
                .withNewType("Opaque")
                    .withData(Collections.singletonMap(dataKey, dataValue))
                .build());
    }

    public static void createSecretFromFile(String pathToOrigin, String key, String name, String namespace) {
        byte[] encoded;
        try {
            encoded = Files.readAllBytes(Paths.get(pathToOrigin));

            Map<String, String> data = new HashMap<>();
            Base64.Encoder encoder = Base64.getEncoder();
            data.put(key, encoder.encodeToString(encoded));

            Secret secret = new SecretBuilder()
                .withData(data)
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(namespace)
                .endMetadata()
                .build();
            kubeClient().namespace(namespace).createSecret(secret);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void waitForClusterSecretsDeletion(String clusterName) {
        LOGGER.info("Waiting for Kafka cluster {} secrets deletion", clusterName);
        TestUtils.waitFor("Expected secrets for Kafka cluster " + clusterName + " will be deleted", Constants.POLL_INTERVAL_FOR_RESOURCE_READINESS, Constants.TIMEOUT_FOR_SECRET_CREATION,
            () -> {
                List<Secret> secretList = kubeClient().listSecrets("strimzi.io/cluster", clusterName);
                if (secretList.isEmpty()) {
                    return true;
                } else {
                    for (Secret secret : secretList) {
                        LOGGER.warn("Secret {} is not deleted yet! Triggering force delete by cmd client!", secret.getMetadata().getName());
                        cmdKubeClient().deleteByName("secret", secret.getMetadata().getName());
                    }
                    return false;
                }
            });
        LOGGER.info("Kafka cluster {} secrets deleted", clusterName);
    }
}
