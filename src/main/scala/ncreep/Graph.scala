package ncreep

import zio.json._

/** Represents a simple directed graph structure. Nodes are given IDs and edges use the IDs to
  * specify connections.
  */
final case class Graph(nodes: Set[Node], edges: Set[Edge]) {
  /** Joins this graph to the given graph creating a larger composite graph. */
  def joinGraph(other: Graph) = Graph(nodes ++ other.nodes, edges ++ other.edges)
}

/** A single graph node.
  * @param id The ID of the node, to be used by edges to specify connections. Needs to be unique
  *           in the graph.
  * @param label A label for the node, to be used when visually representing the graph, doesn't
  *              have to be unique.
  * @param info Extra info attached to the node.
  * @param root 'true' if the given node represent the root of a tree-like structure.
  */
final case class Node(id: String,
                      label: String,
                      info: String,
                      root: Boolean)

/** A directed edge.
  * @param source The ID of the source node of the edge.
  * @param target The ID of the target node of the edge.
  * @param weight The weight of the edge.
  */
final case class Edge(source: String, target: String, weight: Int)

object Graph {
  val empty: Graph = Graph(nodes = Set.empty, edges = Set.empty)

  implicit val encoder: JsonEncoder[Graph] = DeriveJsonEncoder.gen[Graph]
}

object Node {
  implicit val encoder: JsonEncoder[Node] = DeriveJsonEncoder.gen[Node]
}

object Edge {
  implicit val encoder: JsonEncoder[Edge] = DeriveJsonEncoder.gen[Edge]
}

