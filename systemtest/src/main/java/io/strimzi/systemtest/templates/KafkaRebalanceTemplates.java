package io.strimzi.systemtest.templates;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.KafkaRebalanceList;
import io.strimzi.api.kafka.model.KafkaRebalance;
import io.strimzi.api.kafka.model.KafkaRebalanceBuilder;
import io.strimzi.systemtest.resources.ResourceManager;
import io.strimzi.test.TestUtils;

import java.util.HashMap;
import java.util.Map;

public class KafkaRebalanceTemplates {

    public static final String PATH_TO_KAFKA_REBALANCE_CONFIG = TestUtils.USER_PATH + "/../examples/cruise-control/kafka-rebalance.yaml";

    private KafkaRebalanceTemplates() {}

    public static MixedOperation<KafkaRebalance, KafkaRebalanceList, Resource<KafkaRebalance>> kafkaRebalanceClient() {
        return Crds.kafkaRebalanceOperation(ResourceManager.kubeClient().getClient());
    }

    public static KafkaRebalanceBuilder kafkaRebalance(String name) {
        KafkaRebalance kafkaRebalance = getKafkaRebalanceFromYaml(PATH_TO_KAFKA_REBALANCE_CONFIG);
        return defaultKafkaRebalance(kafkaRebalance, name);
    }

    private static KafkaRebalanceBuilder defaultKafkaRebalance(KafkaRebalance kafkaRebalance, String name) {

        Map<String, String> kafkaRebalanceLabels = new HashMap<>();
        kafkaRebalanceLabels.put("strimzi.io/cluster", name);

        return new KafkaRebalanceBuilder(kafkaRebalance)
            .editMetadata()
                .withName(name)
                .withNamespace(ResourceManager.kubeClient().getNamespace())
                .withLabels(kafkaRebalanceLabels)
            .endMetadata();
    }


    public static KafkaRebalanceBuilder kafkaRebalanceWithoutWait(KafkaRebalance kafkaRebalance) {
        kafkaRebalanceClient().inNamespace(ResourceManager.kubeClient().getNamespace()).createOrReplace(kafkaRebalance);
        return new KafkaRebalanceBuilder(kafkaRebalance);
    }

    public static void deleteKafkaRebalanceWithoutWait(String resourceName) {
        kafkaRebalanceClient().inNamespace(ResourceManager.kubeClient().getNamespace()).withName(resourceName).withPropagationPolicy(DeletionPropagation.FOREGROUND).delete();
    }

    private static KafkaRebalance getKafkaRebalanceFromYaml(String yamlPath) {
        return TestUtils.configFromYaml(yamlPath, KafkaRebalance.class);
    }
}
