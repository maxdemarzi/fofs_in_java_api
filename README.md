Neo4j Cypher Unmanaged Extension
================================

This is an unmanaged extension translating a FOF Cypher query.

1. Build it:

        mvn clean package

2. Copy target/fofs_in_java_api-1.0.jar to the plugins/ directory of your Neo4j server.

3. Configure Neo4j by adding a line to conf/neo4j-server.properties:

        org.neo4j.server.thirdparty_jaxrs_classes= org.neo4j.fofs=/example

4. Start Neo4j server.

5. Query it over HTTP:

        curl http://localhost:7474/example/helloworld
