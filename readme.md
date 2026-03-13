# Quarkus Camel APP

Instalación

```bash
sudo dnf install java-21-openjdk java-21-openjdk-devel
sudo dnf install maven
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

Compilación

```bash
camel run routes.camel.yaml aggregation.groovy --properties=application.properties
```

```bash
curl -s http://localhost:8080/api/bs/v1.0/account-management/third-party-account-inquiry/123/retrieve | jq
```

En el **application.properties** considerar:

```bash
quarkus.package.jar.type=uber-jar
quarkus.package.jar.add-runner-suffix=false
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

Variable:

```bash
ENDPOINT_SAVINGS_ACCOUNT_URL="http://host.docker.internal:3001/api/proxy/v1.0/prometeus/savings-account"
ENDPOINT_CURRENT_ACCOUNT_URL="http://host.docker.internal:3001/api/proxy/v1.0/prometeus/current-account"
ENDPOINT_PARTY_DATA_URL="http://host.docker.internal:3001/api/proxy/v1.0/prometeus/party-lifecycle-management"
```

Compilar JVW:

```bash
mvn clean package
# java -jar target/quarkus-app/quarkus-run.jar
java -jar target/app-1.0.0.jar

podman build -f src/main/docker/Dockerfile -t sbsd-account-manager:jvm .

podman run --rm --name sbsd-account-manager -p 8080:8080 \
  -e ENDPOINT_SAVINGS_ACCOUNT_URL="${ENDPOINT_SAVINGS_ACCOUNT_URL}" \
  -e ENDPOINT_CURRENT_ACCOUNT_URL="${ENDPOINT_CURRENT_ACCOUNT_URL}" \
  -e ENDPOINT_PARTY_DATA_URL="${ENDPOINT_SAVINGS_ACCOUNT_URL}" \
  sbsd-account-manager:jvm
```

Compilar Nativo:

```bash
./mvnw clean package -Dnative
./app-1.0.0-runner
podman build -f src/main/docker/Dockerfile.native -t sbsd-account-manager:native .
```

Publicar

```bash
# podman login -u $(oc whoami) -p $(oc whoami -t) ${REGISTRY}
podman login -u ${REGISTRY_USER} -p ${REGISTRY_PASSWORD} ${REGISTRY_URL}
podman push sbsd-account-manager:jvm ${REGISTRY_URL}/atp/sbsd-account-manager:jvm
podman push sbsd-account-manager:native ${REGISTRY_URL}/atp/sbsd-account-manager:native
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
      namespace: atp
    data:
      ENDPOINT_SAVINGS_ACCOUNT_URL: "http://host.docker.internal:3001/api/proxy/v1.0/prometeus/savings-account"
      ENDPOINT_CURRENT_ACCOUNT_URL: "http://host.docker.internal:3001/api/proxy/v1.0/prometeus/current-account"
      ENDPOINT_PARTY_DATA_URL: "http://host.docker.internal:3001/api/proxy/v1.0/prometeus/party-lifecycle-management"

  - apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: sbsd-account-manager
      namespace: atp
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
              image: "image-registry.openshift-image-registry.svc:5000/atp/sbsd-account-manager:1.0.0"
              # image: "quay.io/rh_ee_lfalero/sbsd-account-manager:1.0.0"
              imagePullPolicy: Always
              envFrom:
                - configMapRef:
                    name: sbsd-account-manager
  - apiVersion: v1
    kind: Service
    metadata:
      name: sbsd-account-manager
      namespace: atp
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
      namespace: atp
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

Limpiar

```bash
oc -n atp delete ConfigMap sbsd-account-manager
oc -n atp delete Deployment sbsd-account-manager
oc -n atp delete Service sbsd-account-manager
oc -n atp delete Route sbsd-account-manager
oc -n atp delete ImageStream sbsd-account-manager
```
