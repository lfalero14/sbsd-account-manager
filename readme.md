# How to build and run a Camel application

This project was generated using [Camel Jbang](https://camel.apache.org/manual/camel-jbang.html). Please, refer to the online documentation for learning more about how to configure the export of your Camel application.

This is a brief guide explaining how to build, "containerize" and run your Camel application.

## Build the Maven project (JVM mode)

```bash
./mvnw clean package
```

The application could now immediately run:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

## Create a Docker container (JVM mode)

You can create a container image directly from the `src/main/docker` resources. Here you have a precompiled base configuration which can be enhanced with any further required configuration.

```bash
docker build -f src/main/docker/Dockerfile -t app:1.0.0 .
```

Once the application is published, you can run it directly from the container:

```bash
docker run -it app:1.0.0
```

## Build the Maven project (Native mode)

```bash
./mvnw package -Dnative
```

Native compilation can last a few minutes to complete. Once done, the application could immediately run:

```bash
./app-1.0.0-runner
```

## Create a Docker container (Native mode)

You can create a container image directly from the `src/main/docker` resources. Here you have a precompiled base configuration which can be enhanced with any further required configuration.

```bash
docker build -f src/main/docker/Dockerfile.native -t native-app:1.0.0 .
```

Once the application is published, you can run it directly from the container:

```bash
docker run -it native-app:1.0.0
```

# Nuevos Pasos

Reemplazar groovy: **resource:classpath:aggregation.groovy** por groovy: **resource:file:aggregation.groovy**

```bash
camel run routes.camel.yaml aggregation.groovy --properties=application.properties
```

```bash
curl -s http://localhost:8080/api/bs/v1.0/account-management/third-party-account-inquiry/123/retrieve | jq
```

```bash
camel export \
  --runtime=quarkus \
  --quarkus-group-id=com.redhat.quarkus.platform \
  --quarkus-version=3.27.2.redhat-00002 \
  --repos=https://maven.repository.redhat.com/ga \
  --dep=io.quarkus:quarkus-openshift \
  --gav=ec.com.produbanco:app:1.0.0 \
  --dir=sbsd-account-manager \
  routes.camel.yaml aggregation.groovy application.properties
```

Reemplazar groovy: **resource:file:aggregation.groovy** por groovy: **resource:classpath:aggregation.groovy**

```bash
mv src/main/resources/camel-groovy/aggregation.groovy src/main/resources/aggregation.groovy
```

```bash
mvn clean package
java -jar target/quarkus-app/quarkus-run.jar

podman build -f src/main/docker/Dockerfile.jvm -t sbsd-account-manager:1.0.0 .

ENDPOINT_SAVINGS_ACCOUNT_URL="http://host.docker.internal:3001/api/proxy/v1.0/prometeus/savings-account"
ENDPOINT_CURRENT_ACCOUNT_URL="http://host.docker.internal:3001/api/proxy/v1.0/prometeus/current-account"
ENDPOINT_PARTY_DATA_URL="http://host.docker.internal:3001/api/proxy/v1.0/prometeus/party-lifecycle-management"

podman run --rm --name sbsd-account-manager -p 8080:8080 \
  -e ENDPOINT_SAVINGS_ACCOUNT_URL="${ENDPOINT_SAVINGS_ACCOUNT_URL}" \
  -e ENDPOINT_CURRENT_ACCOUNT_URL="${ENDPOINT_CURRENT_ACCOUNT_URL}" \
  -e ENDPOINT_PARTY_DATA_URL="${ENDPOINT_SAVINGS_ACCOUNT_URL}" \
  sbsd-account-manager:1.0.0
```

```bash
REGISTRY="default-route-openshift-image-registry.apps.cluster-dljqk.dljqk.sandbox1321.opentlc.com"
podman login -u $(oc whoami) -p $(oc whoami -t) ${REGISTRY}
podman push sbsd-account-manager:1.0.0 ${REGISTRY}/dev-mesh/sbsd-account-manager:1.0.0
```

```yaml
apiVersion: v1
kind: List
metadata: {}
items:
  - kind: ConfigMap
    apiVersion: v1
    metadata:
      name: sbsd-account-manager
      namespace: dev-mesh
    data:
      ENDPOINT_SAVINGS_ACCOUNT_URL: "http://host.docker.internal:3001/api/proxy/v1.0/prometeus/savings-account"
      ENDPOINT_CURRENT_ACCOUNT_URL: "http://host.docker.internal:3001/api/proxy/v1.0/prometeus/current-account"
      ENDPOINT_PARTY_DATA_URL: "http://host.docker.internal:3001/api/proxy/v1.0/prometeus/party-lifecycle-management"

  - apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: sbsd-account-manager
      namespace: dev-mesh
    spec:
      replicas: 1
      selector:
        matchLabels:
          app: sbsd-account-manager
      template:
        metadata:
          name: sbsd-account-manager
          labels:
            app: sbsd-account-manager
        spec:
          containers:
            - name: sbsd-account-manager
              # image: "image-registry.openshift-image-registry.svc:5000/dev-mesh/sbsd-account-manager:1.0.0"
              image: "quay.io/rh_ee_lfalero/sbsd-account-manager:1.0.0"
              imagePullPolicy: Always
              envFrom:
                - configMapRef:
                    name: sbsd-account-manager
  - apiVersion: v1
    kind: Service
    metadata:
      name: sbsd-account-manager
      namespace: dev-mesh
    spec:
      selector:
        app: sbsd-account-manager
      type: ClusterIP
      ports:
        - name: 8080-http
          port: 8080
          targetPort: 8080

  - kind: Route
    apiVersion: route.openshift.io/v1
    metadata:
      name: sbsd-account-manager
      namespace: dev-mesh
    spec:
      to:
        kind: Service
        name: sbsd-account-manager
      port:
        targetPort: 8080-http
      tls:
        termination: edge
        insecureEdgeTerminationPolicy: Redirect
      wildcardPolicy: None
```