/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.kafka.listeners;

import io.strimzi.api.kafka.model.KafkaResources;
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListener;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListenerBuilder;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import io.strimzi.systemtest.AbstractST;
import io.strimzi.systemtest.annotations.OpenShiftOnly;
import io.strimzi.systemtest.kafkaclients.externalClients.BasicExternalKafkaClient;
import io.strimzi.systemtest.kafkaclients.internalClients.InternalKafkaClient;
import io.strimzi.systemtest.resources.ResourceManager;
import io.strimzi.systemtest.resources.crd.KafkaClientsResource;
import io.strimzi.systemtest.resources.crd.KafkaResource;
import io.strimzi.systemtest.resources.crd.KafkaTopicResource;
import io.strimzi.systemtest.resources.crd.KafkaUserResource;
import io.strimzi.systemtest.utils.kafkaUtils.KafkaTopicUtils;
import io.strimzi.systemtest.utils.kafkaUtils.KafkaUserUtils;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static io.strimzi.systemtest.Constants.EXTERNAL_CLIENTS_USED;
import static io.strimzi.systemtest.Constants.INTERNAL_CLIENTS_USED;
import static io.strimzi.systemtest.Constants.LOADBALANCER_SUPPORTED;
import static io.strimzi.systemtest.Constants.NODEPORT_SUPPORTED;

public class MultipleListenersST extends AbstractST {

    private static final Logger LOGGER = LogManager.getLogger(MultipleListenersST.class);
    public static final String NAMESPACE = "multiple-listeners-cluster-test";

    // only 4 type of listeners
    private Map<KafkaListenerType, List<GenericKafkaListener>> testCases = new HashMap<>(4);

    @Tag(NODEPORT_SUPPORTED)
    @Tag(EXTERNAL_CLIENTS_USED)
    @Test
    void testMultipleNodePorts() {
        runTestCase(testCases.get(KafkaListenerType.NODEPORT));
    }

    @Tag(INTERNAL_CLIENTS_USED)
    @Test
    void testMultipleInternal() {
        runTestCase(testCases.get(KafkaListenerType.INTERNAL));
    }

    @Tag(NODEPORT_SUPPORTED)
    @Tag(EXTERNAL_CLIENTS_USED)
    @Tag(INTERNAL_CLIENTS_USED)
    @Test
    void testCombinationOfInternalAndExternalListeners() {
        List<GenericKafkaListener> multipleDifferentListeners = new ArrayList<>();

        List<GenericKafkaListener> internalListeners = testCases.get(KafkaListenerType.INTERNAL);
        List<GenericKafkaListener> nodeportListeners = testCases.get(KafkaListenerType.NODEPORT);

        multipleDifferentListeners.addAll(internalListeners);
        multipleDifferentListeners.addAll(nodeportListeners);

        // run INTERNAL + NODEPORT listeners
        runTestCase(multipleDifferentListeners);
    }

    @Tag(LOADBALANCER_SUPPORTED)
    @Tag(EXTERNAL_CLIENTS_USED)
    @Test
    void testMultipleLoadBalancers() {
        runTestCase(testCases.get(KafkaListenerType.LOADBALANCER));
    }

    // DONE

    @OpenShiftOnly
    @Tag(EXTERNAL_CLIENTS_USED)
    @Test
    void testMultipleRoutes() {
        runTestCase(testCases.get(KafkaListenerType.ROUTE));
    }

    @Tag(NODEPORT_SUPPORTED)
    @OpenShiftOnly
    @Tag(EXTERNAL_CLIENTS_USED)
    @Tag(INTERNAL_CLIENTS_USED)
    @Test
    void testMixtureOfExternalListeners() {
        List<GenericKafkaListener> multipleDifferentListeners = new ArrayList<>();

        List<GenericKafkaListener> routeListeners = testCases.get(KafkaListenerType.ROUTE);
        List<GenericKafkaListener> nodeportListeners = testCases.get(KafkaListenerType.NODEPORT);

        multipleDifferentListeners.addAll(routeListeners);
        multipleDifferentListeners.addAll(nodeportListeners);

        // run ROUTE + NODEPORT listeners
        runTestCase(multipleDifferentListeners);
    }

    @Tag(NODEPORT_SUPPORTED)
    @Tag(LOADBALANCER_SUPPORTED)
    @OpenShiftOnly
    @Tag(EXTERNAL_CLIENTS_USED)
    @Tag(INTERNAL_CLIENTS_USED)
    @Test
    void testCombinationOfEveryKindOfListener() {
        List<GenericKafkaListener> multipleDifferentListeners = new ArrayList<>();

        List<GenericKafkaListener> internalListeners = testCases.get(KafkaListenerType.INTERNAL);
        List<GenericKafkaListener> nodeportListeners = testCases.get(KafkaListenerType.NODEPORT);
        List<GenericKafkaListener> routeListeners = testCases.get(KafkaListenerType.ROUTE);
        List<GenericKafkaListener> loadbalancersListeners = testCases.get(KafkaListenerType.LOADBALANCER);

        multipleDifferentListeners.addAll(internalListeners);
        multipleDifferentListeners.addAll(nodeportListeners);
        multipleDifferentListeners.addAll(routeListeners);
        multipleDifferentListeners.addAll(loadbalancersListeners);

        // run INTERNAL + NODEPORT + ROUTE + LOADBALANCER listeners
        runTestCase(multipleDifferentListeners);
    }

    private void runTestCase(List<GenericKafkaListener> listeners) {

        LOGGER.info("This is listeners {}, which will verified.", listeners);

        // exercise phase
        KafkaResource.kafkaEphemeral(CLUSTER_NAME, 3)
            .editSpec()
                .editKafka()
                    .withNewListeners()
                        .withGenericKafkaListeners(listeners)
                    .endListeners()
                .endKafka()
            .endSpec()
            .done();

        String kafkaUsername = KafkaUserUtils.generateRandomNameOfKafkaUser();
        KafkaUser kafkaUserInstance = KafkaUserResource.tlsUser(CLUSTER_NAME, kafkaUsername).done();

        for (GenericKafkaListener listener : listeners) {

            String topicName = KafkaTopicUtils.generateRandomNameOfTopic();
            KafkaTopicResource.topic(CLUSTER_NAME, topicName).done();

            boolean isTlsEnabled = listener.isTls();

            if (listener.getType() != KafkaListenerType.INTERNAL) {

                if (isTlsEnabled) {
                    BasicExternalKafkaClient externalTlsKafkaClient = new BasicExternalKafkaClient.Builder()
                        .withTopicName(topicName)
                        .withNamespaceName(NAMESPACE)
                        .withClusterName(CLUSTER_NAME)
                        .withMessageCount(MESSAGE_COUNT)
                        .withKafkaUsername(kafkaUsername)
                        .withListenerName(listener.getName())
                        .withSecurityProtocol(SecurityProtocol.SSL)
                        .build();

                    // verify phase
                    externalTlsKafkaClient.verifyProducedAndConsumedMessages(
                        externalTlsKafkaClient.sendMessagesTls(),
                        externalTlsKafkaClient.receiveMessagesTls()
                    );
                } else {
                    BasicExternalKafkaClient externalPlainKafkaClient = new BasicExternalKafkaClient.Builder()
                        .withTopicName(topicName)
                        .withNamespaceName(NAMESPACE)
                        .withClusterName(CLUSTER_NAME)
                        .withMessageCount(MESSAGE_COUNT)
                        .withSecurityProtocol(SecurityProtocol.PLAINTEXT)
                        .withListenerName(listener.getName())
                        .build();

                    // verify phase
                    externalPlainKafkaClient.verifyProducedAndConsumedMessages(
                        externalPlainKafkaClient.sendMessagesPlain(),
                        externalPlainKafkaClient.receiveMessagesPlain()
                    );
                }
            } else {
                // using internal clients
                if (isTlsEnabled) {
                    KafkaClientsResource.deployKafkaClients(true, KAFKA_CLIENTS_NAME + "-tls",
                        listener.getName(), kafkaUserInstance).done();

                    final String kafkaClientsTlsPodName =
                        ResourceManager.kubeClient().listPodsByPrefixInName(KAFKA_CLIENTS_NAME + "-tls").get(0).getMetadata().getName();

                    InternalKafkaClient internalTlsKafkaClient = new InternalKafkaClient.Builder()
                            .withUsingPodName(kafkaClientsTlsPodName)
                            .withBootstrapServer(KafkaResources.bootstrapAddressOnSpecificPort(CLUSTER_NAME, listener.getPort()))
                            .withTopicName(topicName)
                            .withNamespaceName(NAMESPACE)
                            .withClusterName(CLUSTER_NAME)
                            .withKafkaUsername(kafkaUsername)
                            .withMessageCount(MESSAGE_COUNT)
                            .build();

                    LOGGER.info("Checking produced and consumed messages to pod:{}", kafkaClientsTlsPodName);

                    // verify phase
                    internalTlsKafkaClient.checkProducedAndConsumedMessages(
                        internalTlsKafkaClient.sendMessagesTls(),
                        internalTlsKafkaClient.receiveMessagesTls()
                    );
                } else {
                    KafkaClientsResource.deployKafkaClients(false, KAFKA_CLIENTS_NAME + "-plain").done();

                    final String kafkaClientsPlainPodName =
                        ResourceManager.kubeClient().listPodsByPrefixInName(KAFKA_CLIENTS_NAME + "-plain").get(0).getMetadata().getName();

                    InternalKafkaClient internalPlainKafkaClient = new InternalKafkaClient.Builder()
                        .withUsingPodName(kafkaClientsPlainPodName)
                        .withBootstrapServer(KafkaResources.bootstrapAddressOnSpecificPort(CLUSTER_NAME, listener.getPort()))
                        .withTopicName(topicName)
                        .withNamespaceName(NAMESPACE)
                        .withClusterName(CLUSTER_NAME)
                        .withMessageCount(MESSAGE_COUNT)
                        .build();

                    LOGGER.info("Checking produced and consumed messages to pod:{}", kafkaClientsPlainPodName);

                    // verify phase
                    internalPlainKafkaClient.checkProducedAndConsumedMessages(
                        internalPlainKafkaClient.sendMessagesPlain(),
                        internalPlainKafkaClient.receiveMessagesPlain()
                    );
                }
            }
        }
    }

    private Map<KafkaListenerType, List<GenericKafkaListener>> generateTestCases() {

        LOGGER.info("Starting to generate test cases for multiple listeners");

        int stochasticCount;

        for (KafkaListenerType kafkaListenerType : KafkaListenerType.values()) {

            LOGGER.info("Generating {} listener", kafkaListenerType.name());

            List<GenericKafkaListener> testCase = new ArrayList<>(5);

            switch (kafkaListenerType) {
                case NODEPORT:
                    stochasticCount = ThreadLocalRandom.current().nextInt(2, 5);

                    for (int j = 0; j < stochasticCount; j++) {

                        boolean stochasticCommunication = ThreadLocalRandom.current().nextInt(2) == 0;

                        testCase.add(new GenericKafkaListenerBuilder()
                            .withName(generateRandomListenerName())
                            .withPort(6090 + j)
                            .withType(KafkaListenerType.NODEPORT)
                            .withTls(stochasticCommunication)
                            .build());
                    }
                    break;
                case LOADBALANCER:
                    stochasticCount = ThreadLocalRandom.current().nextInt(2, 3);

                    for (int j = 0; j < stochasticCount; j++) {

                        boolean stochasticCommunication = ThreadLocalRandom.current().nextInt(2) == 0;

                        testCase.add(new GenericKafkaListenerBuilder()
                            .withName(generateRandomListenerName())
                            .withPort(7090 + j)
                            .withType(KafkaListenerType.LOADBALANCER)
                            .withTls(stochasticCommunication)
                            .build());
                    }
                    break;
                case ROUTE:
                    testCase.add(new GenericKafkaListenerBuilder()
                        .withName(generateRandomListenerName())
                        .withPort(8091)
                        .withType(KafkaListenerType.ROUTE)
                        // Route or Ingress type listener and requires enabled TLS encryption
                        .withTls(true)
                        .build());
                    break;
                case INTERNAL:
                    stochasticCount = ThreadLocalRandom.current().nextInt(2, 4);

                    for (int j = 0; j < stochasticCount; j++) {

                        boolean stochasticCommunication = ThreadLocalRandom.current().nextInt(2) == 0;

                        testCase.add(new GenericKafkaListenerBuilder()
                            .withName(generateRandomListenerName())
                            .withPort(10090 + j)
                            .withType(KafkaListenerType.INTERNAL)
                            .withTls(stochasticCommunication)
                            .build());
                    }
            }
            testCases.put(kafkaListenerType, testCase);
        }

        LOGGER.info("Finished will generation of test cases for multiple listeners");

        return testCases;
    }

    private String generateRandomListenerName() {
        final String lexicon = "abcdefghilkfmnoprstwxyz";

        StringBuilder builder = new StringBuilder();

        while (builder.toString().length() == 0) {
            // spec.containers[j].ports[i].name = "tcp-[generated-name]": must be no more than 15 characters
            int length = new Random().nextInt(5) + 5;
            for (int i = 0; i < length; i++) {
                builder.append(lexicon.charAt(new Random().nextInt(lexicon.length())));
            }
        }
        return builder.toString();
    }

    @BeforeAll
    void setup() throws Exception {
        ResourceManager.setClassResources();
        installClusterOperator(NAMESPACE);

        generateTestCases();
    }
}
