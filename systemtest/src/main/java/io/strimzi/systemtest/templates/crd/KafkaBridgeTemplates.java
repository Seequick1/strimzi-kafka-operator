/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.templates.crd;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.KafkaBridgeList;
import io.strimzi.api.kafka.model.KafkaBridge;
import io.strimzi.api.kafka.model.KafkaBridgeBuilder;
import io.strimzi.api.kafka.model.KafkaBridgeResources;
import io.strimzi.systemtest.Constants;
import io.strimzi.systemtest.resources.ResourceManager;
import io.strimzi.systemtest.resources.kubernetes.NetworkPolicyResource;
import io.strimzi.test.TestUtils;
import org.junit.jupiter.api.extension.ExtensionContext;

import static io.strimzi.systemtest.resources.ResourceManager.CR_CREATION_TIMEOUT;

public class KafkaBridgeTemplates {

    public static final String PATH_TO_KAFKA_BRIDGE_CONFIG = TestUtils.USER_PATH + "/../packaging/examples/bridge/kafka-bridge.yaml";

    private KafkaBridgeTemplates() {}

    public static MixedOperation<KafkaBridge, KafkaBridgeList, Resource<KafkaBridge>> kafkaBridgeClient() {
        return Crds.kafkaBridgeOperation(ResourceManager.kubeClient().getClient());
    }

    public static KafkaBridgeBuilder kafkaBridge(String name, String bootstrap, int kafkaBridgeReplicas) {
        return kafkaBridge(name, name, bootstrap, kafkaBridgeReplicas);
    }

    public static KafkaBridgeBuilder kafkaBridge(String name, String clusterName, String bootstrap, int kafkaBridgeReplicas) {
        KafkaBridge kafkaBridge = getKafkaBridgeFromYaml(PATH_TO_KAFKA_BRIDGE_CONFIG);
        return defaultKafkaBridge(kafkaBridge, name, clusterName, bootstrap, kafkaBridgeReplicas);
    }

    public static KafkaBridgeBuilder kafkaBridgeWithCors(String name, String bootstrap, int kafkaBridgeReplicas,
                                                          String allowedCorsOrigin, String allowedCorsMethods) {
        return kafkaBridgeWithCors(name, name, bootstrap, kafkaBridgeReplicas, allowedCorsOrigin, allowedCorsMethods);
    }

    public static KafkaBridgeBuilder kafkaBridgeWithCors(String name, String clusterName, String bootstrap,
                                                  int kafkaBridgeReplicas, String allowedCorsOrigin,
                                                  String allowedCorsMethods) {
        KafkaBridge kafkaBridge = getKafkaBridgeFromYaml(PATH_TO_KAFKA_BRIDGE_CONFIG);

        KafkaBridgeBuilder kafkaBridgeBuilder = defaultKafkaBridge(kafkaBridge, name, clusterName, bootstrap, kafkaBridgeReplicas);

        kafkaBridgeBuilder
            .editSpec()
                .editHttp()
                    .withNewCors()
                        .withAllowedOrigins(allowedCorsOrigin)
                        .withAllowedMethods(allowedCorsMethods != null ? allowedCorsMethods : "GET,POST,PUT,DELETE,OPTIONS,PATCH")
                    .endCors()
                .endHttp()
            .endSpec();

        return kafkaBridgeBuilder;
    }

    public static KafkaBridgeBuilder kafkaBridgeWithMetrics(String name, String clusterName, String bootstrap) throws Exception {
        return kafkaBridgeWithMetrics(name, clusterName, bootstrap, 1);
    }

    public static KafkaBridgeBuilder kafkaBridgeWithMetrics(String name, String clusterName, String bootstrap, int kafkaBridgeReplicas) throws Exception {
        KafkaBridge kafkaBridge = getKafkaBridgeFromYaml(PATH_TO_KAFKA_BRIDGE_CONFIG);

        return defaultKafkaBridge(kafkaBridge, name, clusterName, bootstrap, kafkaBridgeReplicas)
            .editSpec()
                .withEnableMetrics(true)
            .endSpec();
    }

    private static KafkaBridgeBuilder defaultKafkaBridge(KafkaBridge kafkaBridge, String name, String kafkaClusterName, String bootstrap, int kafkaBridgeReplicas) {
        return new KafkaBridgeBuilder(kafkaBridge)
            .withNewMetadata()
                .withName(name)
                .withNamespace(ResourceManager.kubeClient().getNamespace())
                .withClusterName(kafkaClusterName)
            .endMetadata()
            .editSpec()
                .withBootstrapServers(bootstrap)
                .withReplicas(kafkaBridgeReplicas)
                .withNewInlineLogging()
                    .addToLoggers("bridge.root.logger", "DEBUG")
                .endInlineLogging()
            .endSpec();
    }

    public static KafkaBridge createAndWaitForReadiness(ExtensionContext extensionContext, KafkaBridge kafkaBridge) {
        NetworkPolicyResource.deployNetworkPolicyForResource(extensionContext, kafkaBridge, KafkaBridgeResources.deploymentName(kafkaBridge.getMetadata().getName()));

        TestUtils.waitFor("KafkaBridge creation", Constants.POLL_INTERVAL_FOR_RESOURCE_CREATION, CR_CREATION_TIMEOUT,
            () -> {
                try {
                    kafkaBridgeClient().inNamespace(ResourceManager.kubeClient().getNamespace()).createOrReplace(kafkaBridge);
                    return true;
                } catch (KubernetesClientException e) {
                    if (e.getMessage().contains("object is being deleted")) {
                        return false;
                    } else {
                        throw e;
                    }
                }
            }
        );
        return kafkaBridge;
    }

//    private static DoneableKafkaBridge deployKafkaBridge(KafkaBridge kafkaBridge) {
//
//        // TODO: how??? I can't paste here extension context because of Interface API create(T resource...)
////        if (Environment.DEFAULT_TO_DENY_NETWORK_POLICIES) {
////            KubernetesResource.allowNetworkPolicySettingsForResource(kafkaBridge, KafkaBridgeResources.deploymentName(kafkaBridge.getMetadata().getName()));
////        }
//        return new DoneableKafkaBridge(kafkaBridge, kB -> {
//            TestUtils.waitFor("KafkaBridge creation", Constants.POLL_INTERVAL_FOR_RESOURCE_CREATION, CR_CREATION_TIMEOUT,
//                () -> {
//                    try {
//                        kafkaBridgeClient().inNamespace(ResourceManager.kubeClient().getNamespace()).createOrReplace(kB);
//                        return true;
//                    } catch (KubernetesClientException e) {
//                        if (e.getMessage().contains("object is being deleted")) {
//                            return false;
//                        } else {
//                            throw e;
//                        }
//                    }
//                }
//            );
//            return kB;
//        });
//    }

    private static KafkaBridge getKafkaBridgeFromYaml(String yamlPath) {
        return TestUtils.configFromYaml(yamlPath, KafkaBridge.class);
    }
}
