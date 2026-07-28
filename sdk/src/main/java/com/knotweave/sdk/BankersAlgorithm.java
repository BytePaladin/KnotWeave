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
        public Map<String, Integer> availableInstances = new HashMap<>();
        public Map<String, Map<String, Integer>> allocations = new HashMap<>();
        public Map<String, Map<String, Integer>> requests = new HashMap<>();
    }

    public static DeadlockResult detectDeadlock(List<Node> nodes, List<Edge> edges) {
        DeadlockResult result = new DeadlockResult();
        List<String> processes = new ArrayList<>();
        Map<String, Integer> resources = new HashMap<>();
        Map<String, Integer> available = new HashMap<>();

        for (Node n : nodes) {
            if ("process".equals(n.type)) {
                processes.add(n.id);
            } else if ("resource".equals(n.type)) {
                resources.put(n.id, n.totalInstances);
                available.put(n.id, n.totalInstances);
            }
        }

        Map<String, Map<String, Integer>> allocation = new HashMap<>();
        Map<String, Map<String, Integer>> request = new HashMap<>();

        for (String p : processes) {
            allocation.put(p, new HashMap<>());
            request.put(p, new HashMap<>());
            for (String r : resources.keySet()) {
                allocation.get(p).put(r, 0);
                request.get(p).put(r, 0);
            }
        }

        for (Edge e : edges) {
            Node sourceNode = findNode(nodes, e.source);
            Node targetNode = findNode(nodes, e.target);

            if (sourceNode != null && targetNode != null) {
                if ("process".equals(sourceNode.type) && "resource".equals(targetNode.type)) {
                    request.get(sourceNode.id).put(targetNode.id, request.get(sourceNode.id).get(targetNode.id) + 1);
                } else if ("resource".equals(sourceNode.type) && "process".equals(targetNode.type)) {
                    allocation.get(targetNode.id).put(sourceNode.id, allocation.get(targetNode.id).get(sourceNode.id) + 1);
                    available.put(sourceNode.id, available.get(sourceNode.id) - 1);
                }
            }
        }

        for (String r : available.keySet()) {
            if (available.get(r) < 0) available.put(r, 0);
        }

        boolean[] finish = new boolean[processes.size()];
        Map<String, Integer> work = new HashMap<>(available);
        boolean madeProgress = true;

        while (madeProgress) {
            madeProgress = false;
            for (int i = 0; i < processes.size(); i++) {
                if (!finish[i]) {
                    String p = processes.get(i);
                    boolean canSatisfy = true;
                    for (String r : resources.keySet()) {
                        if (request.get(p).get(r) > work.get(r)) {
                            canSatisfy = false;
                            break;
                        }
                    }

                    if (canSatisfy) {
                        for (String r : resources.keySet()) {
                            work.put(r, work.get(r) + allocation.get(p).get(r));
                        }
                        finish[i] = true;
                        madeProgress = true;
                    }
                }
            }
        }

        for (int i = 0; i < processes.size(); i++) {
            if (!finish[i]) {
                result.deadlockedProcesses.add(processes.get(i));
            }
        }

        result.isDeadlocked = !result.deadlockedProcesses.isEmpty();
        result.availableInstances = available;
        result.allocations = allocation;
        result.requests = request;
        return result;
    }

    private static Node findNode(List<Node> nodes, String id) {
        for (Node n : nodes) {
            if (n.id.equals(id)) return n;
        }
        return null;
    }
}
