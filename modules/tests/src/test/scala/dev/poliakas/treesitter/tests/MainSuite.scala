package dev.poliakas.treesitter
package tests

import dev.poliakas.treesitter.core.*
import dev.poliakas.treesitter.yaml.tree_sitter_yaml
import dev.poliakas.treesitter.sql.tree_sitter_sql
import treesitter.all.*
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import scala.scalanative.libc.string.strlen

object MainSuite extends weaver.FunSuite:
  test("hek"):
    Zone: zone =>
      given Zone = zone

      // first - set up the parsers
      val parserYaml = yaml.Language.newParser

      val parserSql = sql.Language.newParser

      val source =
        """hello: 
          |  sql: SELECT column FROM table
          |  potato: tomato""".stripMargin

      // parse the main language
      val treeMaybe = parserYaml.parseString(None, source)

      val treeOk = expect(treeMaybe.isDefined)

      treeMaybe match
        case None => treeOk
        case Some(tree) =>
          val rootNode = ts_tree_root_node(tree.value)

          // Construct the query to find the injections

          val injectionQueryString =
            """(block_mapping_pair key: (flow_node) @key (#eq? @key "sql") value: (flow_node) @value)"""

          val q = Query.create(
            Language.from(tree_sitter_yaml()),
            injectionQueryString
          )

          q match
            case Left(err) => failure(err.toString())
            case Right(q) =>
              val ehoo = expect(2 == q.stringLiteralCount) and
                expect(2 == q.captureCount) and
                expect(1 == q.patternCount)

              val captures =
                q.captures

              val stringLiterals =
                q.stringLiterals

              val predicatesByPattern = q.predicatesByPattern

              val eyoo = ehoo and
                expect(captures(0) == "key") and
                expect(captures(1) == "value") and
                expect(stringLiterals(0) == "eq?") and
                expect(stringLiterals(1) == "sql") and
                expect(predicatesByPattern.length == 1) and
                expect(predicatesByPattern(0).length == 1) and
                expect(
                  predicatesByPattern(0)(0) == Query.Predicate.Eq(
                    false,
                    Query.PredicateArg.StringLiteral("sql"),
                    Query.PredicateArg.Capture("key")
                  )
                )

              val cursor = q.newCursor(Node.from(rootNode))

              val queryMatch = alloc[TSQueryMatch](1)
              var moreMatches = ts_query_cursor_next_match(cursor.value, queryMatch)

              var sqlNodes: List[Map[String, (String, TSNode)]] = Nil

              while (moreMatches) do

                val matcherooney = (!queryMatch)

                val matchCaptures = 
                  (for i <- (0 until matcherooney.capture_count.toInt).toList yield 
                    val capture = (matcherooney.captures + i)
                    val startByte = ts_node_start_byte((!capture).node)
                    val endByte = ts_node_end_byte((!capture).node) 
                    val s = source.slice(startByte.toInt, endByte.toInt)

                    (captures((!capture).index.toInt), (s, (!capture).node))).toMap

                val preds = predicatesByPattern(matcherooney.pattern_index.toInt)

                val shouldInclude = preds.forall:  
                  case Query.Predicate.Eq(negated, left, right) => 
                    val l = left match 
                      case Query.PredicateArg.StringLiteral(literal) => literal
                      case Query.PredicateArg.Capture(captureName) => matchCaptures(captureName)._1

                    val r = right match 
                      case Query.PredicateArg.StringLiteral(literal) => literal
                      case Query.PredicateArg.Capture(captureName) => matchCaptures(captureName)._1

                    (l == r) == !negated
                  case Query.Predicate.AnyOf(_, _, _) => 
                    // TODO
                    false

                if (shouldInclude)
                  sqlNodes = matchCaptures :: sqlNodes

                moreMatches = ts_query_cursor_next_match(cursor.value, queryMatch)
              end while
              
              val ehooey = eyoo and 
                expect(sqlNodes.size == 1) and
                expect(sqlNodes(0).size == 2)
                // expect(sqlNodes(0).get("key").map(_._1) == Some("hek"))

              val rangerooneys = 
                sqlNodes.map: m => 
                  val node = m("key")._2
                  val rangePtr = alloc[TSRange](1)
                  val range = !rangePtr
                  range.start_point = ts_node_start_point(node)
                  range.end_point = ts_node_end_point(node)
                  range.start_byte = ts_node_start_byte(node)
                  range.end_byte = ts_node_end_byte(node)
                  Range.from(rangePtr)

              parserSql.setRanges(rangerooneys)

              val tree = parserSql.parseString(None, source)

              ehooey and expect(tree.isDefined)

      // Parse the document using the outer parser
      // query the ranges for the injections
      // set the ranges for the injections
      // parse again
