package com.knotweave.sdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankersAlgorithm {

    public static class Node {
        public String id;
        public String type; // "process" or "resource"
        public String label;
        public int totalInstances = 1;
        public int availableInstances = 1;
        public Map<String, Integer> maxNeed = new HashMap<>(); // Resource ID -> Max Need
        
        // For UI Rendering
        public float x = 0, y = 0;
        public boolean isDeadlocked = false;

        public Node(String id, String type, String label) {
            this.id = id;
            this.type = type;
            this.label = label;
        }
    }

    public static class Edge {
        public String id;
        public String source;
        public String target;

        public Edge(String id, String source, String target) {
            this.id = id;
            this.source = source;
            this.target = target;
        }
    }

    public static class DeadlockResult {
        public boolean isDeadlocked;
        public List<String> deadlockedProcesses = new ArrayList<>();

        // Canonical 2D Matrix & 1D Vector representations (N = processes, M = resources)
        public int[][] allocationMatrix;
        public int[][] requestMatrix;
        public int[] availableVector;
        public String[] processLabels;
        public String[] resourceLabels;

        // Backward compatibility mappings
        public Map<String, Integer> availableInstances = new HashMap<>();
        public Map<String, Map<String, Integer>> allocations = new HashMap<>();
        public Map<String, Map<String, Integer>> requests = new HashMap<>();
    }

    public static class SafeStateResult {
        public boolean isSafe;
        public List<String> safeSequence = new ArrayList<>();

        // Canonical 2D Matrix & 1D Vector representations (N = processes, M = resources)
        public int[][] allocationMatrix;
        public int[][] maxMatrix;
        public int[][] needMatrix;
        public int[] availableVector;
        public String[] processLabels;
        public String[] resourceLabels;
    }

    /**
     * Detects deadlocks in the Resource Allocation Graph using 2D integer matrices
     * and the standard Work-Finish vector algorithm.
     *
     * @param nodes Graph nodes containing processes and resources
     * @param edges Graph edges representing allocations (R -> P) and requests (P -> R)
     * @return DeadlockResult containing deadlock status, deadlocked process list, and 2D matrices
     */
    public static DeadlockResult detectDeadlock(List<Node> nodes, List<Edge> edges) {
        DeadlockResult result = new DeadlockResult();

        // 1. Separate and index Processes (N) and Resources (M)
        List<Node> processNodes = new ArrayList<>();
        List<Node> resourceNodes = new ArrayList<>();

        Map<String, Integer> pIndex = new HashMap<>();
        Map<String, Integer> rIndex = new HashMap<>();

        for (Node n : nodes) {
            if ("process".equals(n.type)) {
                pIndex.put(n.id, processNodes.size());
                processNodes.add(n);
            } else if ("resource".equals(n.type)) {
                rIndex.put(n.id, resourceNodes.size());
                resourceNodes.add(n);
            }
        }

        int n = processNodes.size();
        int m = resourceNodes.size();

        if (n == 0) {
            result.isDeadlocked = false;
            return result;
        }

        // 2. Initialize 2D Matrices and Vectors
        // Allocation[n][m]: instances of resource j currently allocated to process i
        int[][] allocation = new int[n][m];
        // Request[n][m]: instances of resource j requested by process i
        int[][] request = new int[n][m];
        // Available[m]: unallocated instances of resource j
        int[] available = new int[m];

        String[] pLabels = new String[n];
        for (int i = 0; i < n; i++) {
            pLabels[i] = processNodes.get(i).label;
        }

        String[] rLabels = new String[m];
        for (int j = 0; j < m; j++) {
            rLabels[j] = resourceNodes.get(j).label;
            available[j] = resourceNodes.get(j).totalInstances;
        }

        // 3. Populate Matrices from Graph Edges
        for (Edge e : edges) {
            Node sourceNode = findNode(nodes, e.source);
            Node targetNode = findNode(nodes, e.target);

            if (sourceNode != null && targetNode != null) {
                // Request edge: Process -> Resource
                if ("process".equals(sourceNode.type) && "resource".equals(targetNode.type)) {
                    Integer pIdx = pIndex.get(sourceNode.id);
                    Integer rIdx = rIndex.get(targetNode.id);
                    if (pIdx != null && rIdx != null) {
                        request[pIdx][rIdx]++;
                    }
                }
                // Allocation edge: Resource -> Process
                else if ("resource".equals(sourceNode.type) && "process".equals(targetNode.type)) {
                    Integer rIdx = rIndex.get(sourceNode.id);
                    Integer pIdx = pIndex.get(targetNode.id);
                    if (pIdx != null && rIdx != null) {
                        allocation[pIdx][rIdx]++;
                        available[rIdx]--;
                    }
                }
            }
        }

        // Clamp negative available to 0 (in case of graph over-allocation)
        for (int j = 0; j < m; j++) {
            if (available[j] < 0) {
                available[j] = 0;
            }
        }

        // 4. Deadlock Detection Algorithm using Work & Finish Vectors
        // Work vector: initialized to Available
        int[] work = new int[m];
        System.arraycopy(available, 0, work, 0, m);

        // Finish vector: initialized to false for all processes
        boolean[] finish = new boolean[n];
        boolean madeProgress = true;

        while (madeProgress) {
            madeProgress = false;
            for (int i = 0; i < n; i++) {
                if (!finish[i]) {
                    // Check if Request[i][j] <= Work[j] for all resources j
                    boolean canSatisfy = true;
                    for (int j = 0; j < m; j++) {
                        if (request[i][j] > work[j]) {
                            canSatisfy = false;
                            break;
                        }
                    }

                    // If condition is satisfied, process runs to completion and releases resources
                    if (canSatisfy) {
                        for (int j = 0; j < m; j++) {
                            work[j] += allocation[i][j];
                        }
                        finish[i] = true;
                        madeProgress = true;
                    }
                }
            }
        }

        // 5. Any process with finish[i] == false is part of a deadlock
        for (int i = 0; i < n; i++) {
            if (!finish[i]) {
                result.deadlockedProcesses.add(processNodes.get(i).id);
            }
        }

        result.isDeadlocked = !result.deadlockedProcesses.isEmpty();
        result.allocationMatrix = allocation;
        result.requestMatrix = request;
        result.availableVector = available;
        result.processLabels = pLabels;
        result.resourceLabels = rLabels;

        // Backward compatibility mappings
        for (int j = 0; j < m; j++) {
            result.availableInstances.put(resourceNodes.get(j).id, available[j]);
        }
        for (int i = 0; i < n; i++) {
            String pId = processNodes.get(i).id;
            result.allocations.put(pId, new HashMap<>());
            result.requests.put(pId, new HashMap<>());
            for (int j = 0; j < m; j++) {
                String rId = resourceNodes.get(j).id;
                result.allocations.get(pId).put(rId, allocation[i][j]);
                result.requests.get(pId).put(rId, request[i][j]);
            }
        }

        return result;
    }

    /**
     * Evaluates whether the system is in a Safe State using Banker's Algorithm with 2D matrices.
     * Computes Allocation, Max, Need matrices and returns a valid Safe Execution Sequence.
     *
     * @param nodes Graph nodes
     * @param edges Graph edges
     * @return SafeStateResult containing safety flag, safe sequence, and 2D matrices
     */
    public static SafeStateResult isSafeState(List<Node> nodes, List<Edge> edges) {
        SafeStateResult result = new SafeStateResult();

        // 1. Separate and index Processes (N) and Resources (M)
        List<Node> processNodes = new ArrayList<>();
        List<Node> resourceNodes = new ArrayList<>();

        Map<String, Integer> pIndex = new HashMap<>();
        Map<String, Integer> rIndex = new HashMap<>();

        for (Node n : nodes) {
            if ("process".equals(n.type)) {
                pIndex.put(n.id, processNodes.size());
                processNodes.add(n);
            } else if ("resource".equals(n.type)) {
                rIndex.put(n.id, resourceNodes.size());
                resourceNodes.add(n);
            }
        }

        int n = processNodes.size();
        int m = resourceNodes.size();

        if (n == 0) {
            result.isSafe = true;
            return result;
        }

        // 2. Initialize Matrices & Vectors
        int[][] allocation = new int[n][m];
        int[][] request = new int[n][m];
        int[][] maxNeed = new int[n][m];
        int[][] need = new int[n][m];
        int[] available = new int[m];

        String[] pLabels = new String[n];
        for (int i = 0; i < n; i++) {
            pLabels[i] = processNodes.get(i).label;
        }

        String[] rLabels = new String[m];
        for (int j = 0; j < m; j++) {
            rLabels[j] = resourceNodes.get(j).label;
            available[j] = resourceNodes.get(j).totalInstances;
        }

        // Populate Max Matrix from Node configuration
        for (int i = 0; i < n; i++) {
            Node pNode = processNodes.get(i);
            for (int j = 0; j < m; j++) {
                String rId = resourceNodes.get(j).id;
                int mNeed = (pNode.maxNeed != null && pNode.maxNeed.containsKey(rId)) ? pNode.maxNeed.get(rId) : 0;
                maxNeed[i][j] = mNeed;
            }
        }

        // 3. Populate Allocation and Request from Edges
        for (Edge e : edges) {
            Node sourceNode = findNode(nodes, e.source);
            Node targetNode = findNode(nodes, e.target);

            if (sourceNode != null && targetNode != null) {
                if ("resource".equals(sourceNode.type) && "process".equals(targetNode.type)) {
                    Integer rIdx = rIndex.get(sourceNode.id);
                    Integer pIdx = pIndex.get(targetNode.id);
                    if (rIdx != null && pIdx != null) {
                        allocation[pIdx][rIdx]++;
                        available[rIdx]--;
                    }
                } else if ("process".equals(sourceNode.type) && "resource".equals(targetNode.type)) {
                    Integer pIdx = pIndex.get(sourceNode.id);
                    Integer rIdx = rIndex.get(targetNode.id);
                    if (pIdx != null && rIdx != null) {
                        request[pIdx][rIdx]++;
                    }
                }
            }
        }

        // 4. Compute Need Matrix: Need[i][j] = Max(Request[i][j], MaxNeed[i][j] - Allocation[i][j])
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                int explicitNeed = maxNeed[i][j] > 0 ? Math.max(0, maxNeed[i][j] - allocation[i][j]) : 0;
                need[i][j] = Math.max(request[i][j], explicitNeed);
            }
        }

        // Clamp negative available to 0
        for (int j = 0; j < m; j++) {
            if (available[j] < 0) {
                available[j] = 0;
            }
        }

        // 5. Banker's Safety Algorithm using Work & Finish Vectors
        int[] work = new int[m];
        System.arraycopy(available, 0, work, 0, m);

        boolean[] finish = new boolean[n];
        int count = 0;

        while (count < n) {
            boolean found = false;
            for (int i = 0; i < n; i++) {
                if (!finish[i]) {
                    // Check if Need[i][j] <= Work[j] for all resources j
                    boolean canSatisfy = true;
                    for (int j = 0; j < m; j++) {
                        if (need[i][j] > work[j]) {
                            canSatisfy = false;
                            break;
                        }
                    }

                    if (canSatisfy) {
                        for (int j = 0; j < m; j++) {
                            work[j] += allocation[i][j];
                        }
                        result.safeSequence.add(processNodes.get(i).id);
                        finish[i] = true;
                        found = true;
                        count++;
                    }
                }
            }
            if (!found) {
                break;
            }
        }

        result.isSafe = (count == n);
        result.allocationMatrix = allocation;
        result.maxMatrix = maxNeed;
        result.needMatrix = need;
        result.availableVector = available;
        result.processLabels = pLabels;
        result.resourceLabels = rLabels;

        return result;
    }

    private static Node findNode(List<Node> nodes, String id) {
        for (Node n : nodes) {
            if (n.id.equals(id)) return n;
        }
        return null;
    }
}
