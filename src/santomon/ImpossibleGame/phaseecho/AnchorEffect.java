package santomon.ImpossibleGame.phaseecho;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

class AnchorEffect extends BaseEveryFrameCombatPlugin {


    ShipAPI ship;
    SpriteAPI spriteAPI;
    Network network;
    String spriteName = "graphic/impossible_line.png";

    private Logger log = Global.getLogger(AnchorEffect.class);

    AnchorEffect(ShipAPI ship) {
        // so segments are basically the lines defined by two points.
        this.ship = ship;
        BoundsAPI bounds = this.ship.getExactBounds();
        this.network = new Network(bounds);
        this.spriteAPI = ship.getSpriteAPI();
    }

    @Override
    public void init(CombatEngineAPI engine) {
        super.init(engine);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        super.advance(amount, events);
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
        super.renderInWorldCoords(viewport);
    }

    public static void renderSkeletonInWorldCoords(ShipAPI ship, Network network) {

        for (Network.Edge edge : network.edges) {

            Vector2f centerOfEdge = new Vector2f(
                    edge.start.location.x + edge.end.location.x * 0.5f,
                    edge.start.location.y + edge.end.location.y * 0.5f
            );

            Vector2f offset = Vector2f.sub(centerOfEdge, ship.getVariant().getHullSpec().getModuleAnchor(), null);

        }

    }
}


class Network {
    // not a very efficient implementation is it


    private static final float BOUND_WEIGHT = 3;
    private static final float INNER_WEIGHT = 1;
    public List<Node> nodes = new ArrayList<>();

    public List<Edge> edges = new ArrayList<>();


    Network(BoundsAPI bounds) {

        for (BoundsAPI.SegmentAPI segment : bounds.getSegments()) {
            nodes.add(new Node(segment.getP1()));
        }

        for (Node node1 : nodes.subList(0, nodes.size() - 1)) {
            for (Node node2 : nodes.subList(1, nodes.size())) {
                edges.add(new Edge(node1, node2, BOUND_WEIGHT));
            }
        }
        edges.add(new Edge(nodes.get(nodes.size() - 1), nodes.get(0), BOUND_WEIGHT));


        List<Edge> newEdgesToAdd = new ArrayList<>();

        for (Node node1: nodes) {
            for (Node node2: nodes) {
                if (node1 == node2) continue;

                if (Helpers.arePointsOnSameGridLine(node1.location, node2.location)) {

                    boolean atLeast1Intersection = false;
                    for (Edge edge : edges) {
                        Vector2f intersection = Helpers.getIntersection(node1.location, node2.location, edge.start.location, edge.end.location);
                        if (intersection != null) {
                            atLeast1Intersection = true;
                            break;
                        }
                    }

                    if (!atLeast1Intersection) continue;
                    newEdgesToAdd.add(new Edge(node1, node2, INNER_WEIGHT));
                }
            }
        }

        edges.addAll(newEdgesToAdd);


    }


    public List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.start == node) {
                neighbors.add(edge.end);
            }
            if (edge.end == node) {
                neighbors.add(edge.start);
            }
        }
        return neighbors;
    }

    public List<Edge> getConnectedEdges(Node node) {
        List<Edge> connectedEdges = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.start == node || edge.end == node) {
                connectedEdges.add(edge);
            }
        }
        return connectedEdges;
    }



    class Node {

        public Vector2f location;
        Node(Vector2f location) {
            this.location = location;
        }

    }

    class Edge {
        public float weight = 0;
        public final Node start;
        public final Node end;

        Edge (Node start, Node end) {
            this.start = start;
            this.end = end;
        }

        Edge(Node start, Node end, float weight) {
            this.weight = weight;
            this.start = start;
            this.end = end;

        }

    }


}


class Helpers {
    public static Vector2f getIntersection(Vector2f p1, Vector2f p2, Vector2f p3, Vector2f p4) {
        // Line 1 represented as p1 + t * (p2 - p1)
        // Line 2 represented as p3 + u * (p4 - p3)

        // Calculate the differences
        Vector2f d1 = Vector2f.sub(p2, p1, null); // Direction of the first line
        Vector2f d2 = Vector2f.sub(p4, p3, null); // Direction of the second line

        // Denominator for checking if the lines are parallel
        float denominator = d1.x * d2.y - d1.y * d2.x;

        // If denominator is 0, the lines are parallel (or coincident)
        if (denominator == 0) {
            return null; // Lines are parallel or coincident, no intersection
        }

        // Calculate the difference between points p1 and p3
        Vector2f diff = Vector2f.sub(p3, p1, null);

        // Numerators for calculating the intersection point
        float t = (diff.x * d2.y - diff.y * d2.x) / denominator;
        float u = (diff.x * d1.y - diff.y * d1.x) / denominator;

        // Check if the intersection occurs within the segment bounds (0 <= t <= 1 and 0 <= u <= 1)
        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            // Calculate the intersection point
            Vector2f intersection = Vector2f.add((Vector2f) d1.scale(t), p1, null);
            return intersection;
        }

        // No valid intersection within the line segments
        return null;
    }

    public static boolean arePointsOnSameGridLine(Vector2f p1, Vector2f p2) {
        return (p1.x == p2.x) ^ (p1.y == p2.y);

    }

}
