package dev.poliakas.treesitter.core

import scala.scalanative.libc.stdlib.malloc
import scala.scalanative.libc.stdlib.free
import scala.scalanative.unsafe.Ptr
import treesitter.all.*

case class QueryIterator private (
  queryCursor: QueryCursor,
  queryMatchPtr: Ptr[TSQueryMatch],
  captures: List[String],
  // TODO could probably put everything query related into its own package
  predicatesByPattern: List[List[Query.Predicate]],
  sourceString: String
  ) extends Iterator[QueryIterator.Match]:

  private var nextMatch: QueryIterator.Match = null
  private var nextMatchFetched = false
  private var nodes: List[TSNode] = Nil

  private def fetchNext(): Boolean = 
    if !nextMatchFetched then
      val moreMatches = ts_query_cursor_next_match(queryCursor.value, queryMatchPtr)

      if moreMatches then
        val queryMatch = !queryMatchPtr
        val matchCaptures =
          val captoo = for i <- (0 until queryMatch.capture_count.toInt).toList yield
            val capturePtr = (queryMatch.captures + i)
            val capture = !capturePtr

            val nodePtr = malloc(TSNode._tag.size).asInstanceOf[Ptr[TSNode]]
            val node = !nodePtr
            nodes = !nodePtr :: nodes

            node.context = capture.node.context
            node.id = capture.node.id
            node.tree = capture.node.tree

            captures(capture.index.toInt) -> nodePtr

          captoo.toMap

        val predicates = predicatesByPattern(queryMatch.pattern_index.toInt)

        val shouldInclude = predicates.forall:
          case Query.Predicate.Eq(negated, left, right) =>
            val l = left match
              case Query.PredicateArg.StringLiteral(literal) => literal
              case Query.PredicateArg.Capture(captureName) => Node.from(matchCaptures(captureName)).sliceFrom(sourceString)

            val r = right match
              case Query.PredicateArg.StringLiteral(literal) => literal
              case Query.PredicateArg.Capture(captureName) => Node.from(matchCaptures(captureName)).sliceFrom(sourceString)

            (l == r) != negated
          case Query.Predicate.AnyOf(_, _, _) =>
            throw new Exception("any-of? and not-any-of? are not currently supported")

        if shouldInclude then
          nextMatch = QueryIterator.Match(matchCaptures.view.mapValues(Node.from(_)).toMap)
        else
          fetchNext()
      end if
        
      nextMatchFetched = true
      
    nextMatch != null

  def delete: Unit = 
    queryCursor.delete
    free(queryMatchPtr.asInstanceOf[Ptr[Byte]])
    nodes.foreach(ptr => free(ptr.asInstanceOf[Ptr[Byte]]))

  override def hasNext: Boolean = 
    fetchNext()

  override def next(): QueryIterator.Match = 
    if fetchNext() then
      nextMatchFetched = false
      val r = nextMatch
      nextMatch = null
      r
    else 
      throw new NoSuchElementException()


object QueryIterator:

  case class Match(matches: Map[String, Node])

  def apply(
    q: Query,
    sourceString: String,
    at: Node,
  ): QueryIterator = {
    val queryMatch = malloc(TSQueryMatch._tag.size)
    val queryCursor = q.newCursor(at)
    QueryIterator(
      queryCursor, 
      queryMatch.asInstanceOf[Ptr[TSQueryMatch]],
      q.captures,
      q.predicatesByPattern,
      sourceString
      )
  }
