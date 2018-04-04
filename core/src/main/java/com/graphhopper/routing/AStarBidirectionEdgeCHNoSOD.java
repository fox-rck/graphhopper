/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing;

import com.graphhopper.routing.ch.AStarCHEntry;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.BeelineWeightApproximator;
import com.graphhopper.routing.weighting.ConsistentWeightApproximator;
import com.graphhopper.routing.weighting.WeightApproximator;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Helper;

public class AStarBidirectionEdgeCHNoSOD extends AbstractBidirectionEdgeCHNoSOD<AStarCHEntry> {
    private final boolean useHeuristicForNodeOrder = false;
    private ConsistentWeightApproximator weightApprox;

    public AStarBidirectionEdgeCHNoSOD(Graph graph, Weighting weighting, TraversalMode traversalMode) {
        super(graph, weighting, traversalMode);
        setApproximation(new BeelineWeightApproximator(nodeAccess, weighting).setDistanceCalc(Helper.DIST_PLANE));
    }

    @Override
    public void init(int from, double fromWeight, int to, double toWeight) {
        weightApprox.setFrom(from);
        weightApprox.setTo(to);
        super.init(from, fromWeight, to, toWeight);
    }

    @Override
    protected boolean fwdSearchCanBeStopped() {
        return getMinCurrPathWeight(currFrom.weight, currFrom.adjNode, false) > bestPath.getWeight();
    }

    @Override
    protected boolean bwdSearchCanBeStopped() {
        return getMinCurrPathWeight(currTo.weight, currTo.adjNode, true) > bestPath.getWeight();
    }

    @Override
    protected AStarCHEntry createStartEntry(int node, double weight, boolean reverse) {
        final double heapWeight = getHeapWeight(node, reverse, weight);
        return new AStarCHEntry(node, heapWeight, weight);
    }

    @Override
    protected AStarCHEntry createEntry(EdgeIteratorState edge, int edgeId, double weight, AStarCHEntry parent, boolean reverse) {
        int neighborNode = edge.getAdjNode();
        double heapWeight = getHeapWeight(neighborNode, reverse, weight);
        AStarCHEntry entry = new AStarCHEntry(edge.getEdge(), edgeId, neighborNode, heapWeight, weight);
        entry.parent = parent;
        return entry;
    }

    @Override
    protected void updateEntry(AStarCHEntry entry, EdgeIteratorState edge, int edgeId, double weight, AStarCHEntry parent, boolean reverse) {
        entry.edge = edge.getEdge();
        entry.incEdge = edgeId;
        entry.weight = getHeapWeight(edge.getAdjNode(), reverse, weight);
        entry.weightOfVisitedPath = weight;
        entry.parent = parent;
    }

    public WeightApproximator getApproximation() {
        return weightApprox.getApproximation();
    }

    public AStarBidirectionEdgeCHNoSOD setApproximation(WeightApproximator weightApproximator) {
        weightApprox = new ConsistentWeightApproximator(weightApproximator);
        return this;
    }

    private double getHeapWeight(int node, boolean reverse, double weightOfVisitedPath) {
        if (useHeuristicForNodeOrder) {
            return weightOfVisitedPath + weightApprox.approximate(node, reverse);
        }
        return weightOfVisitedPath;
    }

    private double getMinCurrPathWeight(double weight, int node, boolean reverse) {
        if (useHeuristicForNodeOrder) {
            return weight;
        }
        return weight + weightApprox.approximate(node, reverse);
    }

    @Override
    public String getName() {
        return "astarbi|ch|edge_based|no_sod";
    }
}
