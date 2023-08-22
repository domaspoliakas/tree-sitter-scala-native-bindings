package dev.poliakas.treesitter.tests

import dev.poliakas.treesitter.core.*
import dev.poliakas.treesitter.yaml.tree_sitter_yaml
import dev.poliakas.treesitter.sql.tree_sitter_sql
import treesitter.all.*
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import scala.scalanative.libc.string.strlen
import scala.scalanative.libc.stdio

object MainSuite extends weaver.FunSuite:
  test("hek"):
    Zone: zone =>
      given Zone = zone

      // first - set up the parsers
      val parserYaml = ts_parser_new()
      ts_parser_set_language(parserYaml, tree_sitter_yaml())

      val parserSql = ts_parser_new()
      ts_parser_set_language(parserSql, tree_sitter_sql())

      val source =
        """hello: 
          |  sql: SELECT column FROM table
          |  potato: tomato""".stripMargin

      val sourceC: CString = toCString(source)

      // parse the main language
      val tree = ts_parser_parse_string(
        parserYaml,
        null,
        sourceC,
        strlen(sourceC).toUInt
      )

      val treeOk = expect(tree != null)

      val rootNode: Ptr[TSNode] = alloc[TSNode](1)
      ts_tree_root_node(tree)(rootNode)

      // Construct the query to find the injections
      val errorOffset = alloc[uint32_t](1)
      val errorType = alloc[TSQueryError](1)

      val injectionQueryString =
        """(block_mapping_pair key: (flow_node) @key value: (flow_node) @value)"""

      val q = Query.create(Language.from(tree_sitter_yaml()), injectionQueryString)

      q match {
        case Left(err) => failure(err.toString())
        case Right(q) => success
      }

      // val injectionQuery = ts_query_new(
      //   tree_sitter_yaml(),
      //   injectionQueryString,
      //   strlen(injectionQueryString).toUInt,
      //   errorOffset,
      //   errorType
      // )

      // val queryOk =
      //   expect(!errorType != TSQueryError.TSQueryErrorNodeType) and
      //     expect(!errorType != TSQueryError.TSQueryErrorSyntax) and
      //     expect(!errorType != TSQueryError.TSQueryErrorLanguage)

      // Create the query iterator to iterate over the results

      // val queryCursor = ts_query_cursor_new()
      // ts_query_cursor_exec(queryCursor, injectionQuery, rootNode)

      // val queryMatch = alloc[TSQueryMatch](1)
      // var moreMatches = ts_query_cursor_next_match(queryCursor, queryMatch)

      // while(moreMatches) {

        // injectionQuery

      // }


      // treeOk and queryOk

      // Parse the document using the outer parser
      // query the ranges for the injections
      // set the ranges for the injections
      // parse again
