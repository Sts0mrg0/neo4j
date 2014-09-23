/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.v2_2.planner.logical.steps

import org.neo4j.cypher.internal.compiler.v2_2.planner.logical._
import org.neo4j.cypher.internal.compiler.v2_2.planner.logical.plans.{IdName, LogicalPlan}
import org.neo4j.cypher.internal.compiler.v2_2.planner.QueryGraph
import org.neo4j.cypher.internal.compiler.v2_2.planner.logical.steps.LogicalPlanProducer._
import org.neo4j.cypher.internal.compiler.v2_2.ast.PatternExpression

object outerJoin extends CandidateGenerator[PlanTable] {
  def apply(planTable: PlanTable, queryGraph: QueryGraph)(implicit context: LogicalPlanningContext): CandidateList = {

    val outerJoinPlans = for {
      optionalQG <- queryGraph.optionalMatches if optionalQG.argumentIds.size == 1
      joinNode <- optionalQG.argumentIds.headOption.toSeq if queryGraph.patternNodes(joinNode)
      lhs <- planTable.plans if applicable(joinNode, lhs, optionalQG)
    } yield {
      val rhs = context.strategy.plan(optionalQG.withoutArguments())
      planOuterHashJoin(joinNode, lhs, rhs)
    }

    CandidateList(outerJoinPlans.toSeq)
  }

  private def applicable(joinNode: IdName, outerPlan: LogicalPlan, optionalQG: QueryGraph) = {
    val availableInOuterPlan = outerPlan.availableSymbols(joinNode)
    val isSolved = outerPlan.solved.graph.optionalMatches.contains(optionalQG)
    availableInOuterPlan && !isSolved
  }
}
