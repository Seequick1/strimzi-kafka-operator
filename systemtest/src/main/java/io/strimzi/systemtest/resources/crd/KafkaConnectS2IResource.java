/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.resources.crd;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.KafkaConnectS2IList;
import io.strimzi.api.kafka.model.CertSecretSourceBuilder;
import io.strimzi.api.kafka.model.KafkaConnectS2I;
import io.strimzi.api.kafka.model.KafkaConnectS2IBuilder;
import io.strimzi.api.kafka.model.KafkaConnectS2IResources;
import io.strimzi.api.kafka.model.KafkaResources;
import io.strimzi.systemtest.Constants;
import io.strimzi.systemtest.Environment;
import io.strimzi.systemtest.enums.CustomResourceStatus;
import io.strimzi.systemtest.resources.ResourceType;
import io.strimzi.systemtest.resources.kubernetes.NetworkPolicyResource;
import io.strimzi.systemtest.utils.kafkaUtils.KafkaConnectS2IUtils;
import io.strimzi.systemtest.utils.kafkaUtils.KafkaConnectUtils;
import io.strimzi.test.TestUtils;
import io.strimzi.systemtest.resources.ResourceManager;

import java.util.function.Consumer;

import static io.strimzi.systemtest.resources.ResourceManager.CR_CREATION_TIMEOUT;

// Deprecation is suppressed because of KafkaConnectS2I
@SuppressWarnings("deprecation")
public class KafkaConnectS2IResource implements ResourceType<KafkaConnectS2I> {

    public KafkaConnectS2IResource() { }

    @Override
    public String getKind() {
        return KafkaConnectS2I.RESOURCE_KIND;
    }
    @Override
    public KafkaConnectS2I get(String namespace, String name) {
        return kafkaConnectS2IClient().inNamespace(namespace).withName(name).get();
    }
    @Override
    public void create(KafkaConnectS2I resource) {
        // TODO: same as KafkaBridge and KafkaConnect
        kafkaConnectS2IClient().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).createOrReplace(resource);
    }
    @Override
    public void delete(KafkaConnectS2I resource) throws Exception {
        kafkaConnectS2IClient().inNamespace(resource.getMetadata().getNamespace()).withName(
            resource.getMetadata().getName()).withPropagationPolicy(DeletionPropagation.FOREGROUND).delete();
    }

    @Override
    public boolean isReady(KafkaConnectS2I resource) {
        return ResourceManager.waitForResourceStatus(kafkaConnectS2IClient(), resource, CustomResourceStatus.Ready);
    }
    @Override
    public void refreshResource(KafkaConnectS2I existing, KafkaConnectS2I newResource) {
        existing.setMetadata(newResource.getMetadata());
        existing.setSpec(newResource.getSpec());
        existing.setStatus(newResource.getStatus());
    }

    public static MixedOperation<KafkaConnectS2I, KafkaConnectS2IList, Resource<KafkaConnectS2I>> kafkaConnectS2IClient() {
        return Crds.kafkaConnectS2iV1Beta2Operation(ResourceManager.kubeClient().getClient());
    }

    public static void replaceConnectS2IResource(String resourceName, Consumer<KafkaConnectS2I> editor) {
        ResourceManager.replaceCrdResource(KafkaConnectS2I.class, KafkaConnectS2IList.class, resourceName, editor);
    }
}
