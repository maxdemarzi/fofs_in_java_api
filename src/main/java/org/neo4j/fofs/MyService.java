package org.neo4j.fofs;


import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.tooling.GlobalGraphOperations;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

@Path("/service")
public class MyService {

    ObjectMapper objectMapper = new ObjectMapper();
    private static final RelationshipType FRIENDS = DynamicRelationshipType.withName("FRIENDS");
    public static final Comparator<Map.Entry<Node, MutableInt>> REVERSE_MUTABLE_INT_COMPARATOR = new ReverseMutableIntComparator();

    @GET
    @Path("/helloworld")
    public String helloWorld() {
        return "Hello World!";
    }

    @GET
    @Path("/warmup")
    public String warmUp(@Context GraphDatabaseService db) {
        try ( Transaction tx = db.beginTx() )
        {
            String name;
            Node start;
            for ( Node n : GlobalGraphOperations.at(db).getAllNodes() ) {
                n.getPropertyKeys();
                for ( Relationship relationship : n.getRelationships() ) {
                    start = relationship.getStartNode();
                }
            }
            for ( Relationship r : GlobalGraphOperations.at(db).getAllRelationships() ) {
                r.getPropertyKeys();
                start = r.getStartNode();
            }
        }
        return "Warmed up and ready to go!";
    }

    @GET
    @Path("/user/{username}")
    public Response getUsers(@PathParam("username") String username, @Context GraphDatabaseService db) throws IOException {
        Map<String, Object> results = new HashMap<>();
        try ( Transaction tx = db.beginTx() )
        {
            final Node user = IteratorUtil.singleOrNull(db.findNodesByLabelAndProperty(DynamicLabel.label("User"), "username", username));
            for (String prop : user.getPropertyKeys()) {
                results.put(prop, user.getProperty(prop));
            }
        }

        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }


    @GET
    @Path("/friends/{username}")
    public Response getFriends(@PathParam("username") String username, @Context GraphDatabaseService db) throws IOException {
        List<String> results = new ArrayList<String>();
        try ( Transaction tx = db.beginTx() )
        {
            final Node user = IteratorUtil.singleOrNull(db.findNodesByLabelAndProperty(DynamicLabel.label("User"), "username", username));

            if(user != null){
                for ( Relationship relationship : user.getRelationships(FRIENDS, Direction.BOTH) ){
                    Node friend = relationship.getOtherNode(user);
                    results.add((String)friend.getProperty("username"));
                }
            }
        }

        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

    @GET
    @Path("/fofs/{username}")
    public Response getFofs(@PathParam("username") String username, @Context GraphDatabaseService db) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();

        HashMap<Node, MutableInt> fofs = new HashMap<>();
        try ( Transaction tx = db.beginTx() )
        {
            final Node user = IteratorUtil.singleOrNull(db.findNodesByLabelAndProperty(DynamicLabel.label("User"), "username", username));

            findFofs(fofs, user);
            List<Map.Entry<Node, MutableInt>> fofList = orderFofs(fofs);
            returnFofs(results, fofList.subList(0, Math.min(fofList.size(), 10)));
        }

        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

    private void findFofs(HashMap<Node, MutableInt> fofs, Node user) {
        List<Node> friends = new ArrayList<>();

        if (user != null){
            getFirstLevelFriends(user, friends);
            getSecondLevelFriends(fofs, user, friends);
        }
    }

    private void getFirstLevelFriends(Node user, List<Node> friends) {
        for ( Relationship relationship : user.getRelationships(FRIENDS, Direction.BOTH) ){
            Node friend = relationship.getOtherNode(user);
            friends.add(friend);
        }
    }

    private void getSecondLevelFriends(HashMap<Node, MutableInt> fofs, Node user, List<Node> friends) {
        for ( Node friend : friends ){
            for (Relationship otherRelationship : friend.getRelationships(FRIENDS, Direction.BOTH) ){
                Node fof = otherRelationship.getOtherNode(friend);
                if ((!user.equals(fof) && !friends.contains(fof))) {
                    MutableInt mutableInt = fofs.get(fof);
                    if (mutableInt == null) {
                        fofs.put(fof, new MutableInt(1));
                    } else {
                        mutableInt.increment();
                    }
                }
            }
        }
    }

    private void returnFofs(List<Map<String, Object>> results, List<Map.Entry<Node, MutableInt>> fofList) {
        Map<String, Object> resultsEntry;
        Map<String, Object> fofEntry;
        Node fof;
        for (Map.Entry<Node, MutableInt> entry : fofList) {
            resultsEntry = new HashMap<>();
            fofEntry = new HashMap<>();
            fof = entry.getKey();

            for (String prop : fof.getPropertyKeys()) {
                fofEntry.put(prop, fof.getProperty(prop));
            }

            resultsEntry.put("fof", fofEntry);
            resultsEntry.put("friend_count", entry.getValue());
            results.add(resultsEntry);
        }
    }


    private List<Map.Entry<Node, MutableInt>> orderFofs(HashMap<Node, MutableInt> fofs) {
        List<Map.Entry<Node, MutableInt>> fofList = new ArrayList<>(fofs.entrySet());
        Collections.sort(fofList, new ReverseMutableIntComparator());
        return fofList;
    }

}
