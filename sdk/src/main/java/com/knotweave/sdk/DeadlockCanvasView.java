package com.knotweave.sdk;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeadlockCanvasView extends View {

    private List<BankersAlgorithm.Node> nodes = new ArrayList<>();
    private List<BankersAlgorithm.Edge> edges = new ArrayList<>();
    private BankersAlgorithm.DeadlockResult deadlockResult = new BankersAlgorithm.DeadlockResult();

    // Pan & Zoom Matrix State
    private float scaleFactor = 1.0f;
    private float panOffsetX = 0.0f;
    private float panOffsetY = 0.0f;
    private ScaleGestureDetector scaleDetector;

    // Touch Panning state
    private float lastPanTouchX, lastPanTouchY;
    private boolean isPanning = false;

    // Paints
    private Paint processPaint, resourcePaint;
    private Paint labelPaint, subTextPaint, greenTextPaint, purpleTextPaint;
    private Paint allocEdgePaint, reqEdgePaint, deadlockedEdgePaint, selectedEdgePaint;
    private Paint selectedNodePaint, deadlockedNodePaint;
    private Paint dotAvailablePaint, dotUsedPaint;
    private Paint handleFillPaint, handleBorderPaint;
    private Paint edgeLabelBgPaint, edgeLabelTextPaint;

    // Interaction State
    private BankersAlgorithm.Node selectedNode = null;
    private BankersAlgorithm.Edge selectedEdge = null;
    private BankersAlgorithm.Node draggingNode = null;

    // Cable Drag State
    private boolean isDraggingCable = false;
    private BankersAlgorithm.Node cableStartNode = null;
    private String cableStartPortType = ""; // "out" or "in"
    private float cableStartX, cableStartY;
    private float cableCurrentX, cableCurrentY;

    private float lastWorldX, lastWorldY;
    private float pulseAlpha = 1f;
    private ValueAnimator pulseAnimator;

    // Listeners
    private OnSelectionChangeListener selectionChangeListener;
    private OnEdgeCreatedListener edgeCreatedListener;

    public interface OnSelectionChangeListener {
        void onNodeSelected(BankersAlgorithm.Node node);
        void onEdgeSelected(BankersAlgorithm.Edge edge);
        void onSelectionCleared();
    }

    public interface OnEdgeCreatedListener {
        void onEdgeCreated(BankersAlgorithm.Node source, BankersAlgorithm.Node target);
        void onInvalidConnection(String reason);
    }

    public DeadlockCanvasView(Context context) {
        super(context);
        init(context);
    }

    public DeadlockCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.4f, Math.min(scaleFactor, 3.0f));
                invalidate();
                return true;
            }
        });

        // Translucent Glassmorphic Fills (approx 80% opacity so lines under nodes appear diffused)
        processPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        processPaint.setColor(Color.parseColor("#CC181822"));
        processPaint.setStyle(Paint.Style.FILL);

        resourcePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        resourcePaint.setColor(Color.parseColor("#CC181822"));
        resourcePaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(34f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);

        subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subTextPaint.setColor(Color.parseColor("#B3FFFFFF"));
        subTextPaint.setTextSize(22f);
        subTextPaint.setTextAlign(Paint.Align.CENTER);

        greenTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        greenTextPaint.setColor(Color.parseColor("#81C995"));
        greenTextPaint.setTextSize(22f);
        greenTextPaint.setTextAlign(Paint.Align.CENTER);
        greenTextPaint.setFakeBoldText(true);

        purpleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        purpleTextPaint.setColor(Color.parseColor("#D0BCFF"));
        purpleTextPaint.setTextSize(22f);
        purpleTextPaint.setTextAlign(Paint.Align.CENTER);
        purpleTextPaint.setFakeBoldText(true);

        allocEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        allocEdgePaint.setColor(Color.parseColor("#81C995"));
        allocEdgePaint.setStyle(Paint.Style.STROKE);
        allocEdgePaint.setStrokeWidth(5f);

        reqEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        reqEdgePaint.setColor(Color.parseColor("#D0BCFF"));
        reqEdgePaint.setStyle(Paint.Style.STROKE);
        reqEdgePaint.setStrokeWidth(5f);

        deadlockedEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        deadlockedEdgePaint.setColor(Color.parseColor("#F2B8B5"));
        deadlockedEdgePaint.setStyle(Paint.Style.STROKE);
        deadlockedEdgePaint.setStrokeWidth(8f);

        selectedEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedEdgePaint.setColor(Color.parseColor("#38BDF8"));
        selectedEdgePaint.setStyle(Paint.Style.STROKE);
        selectedEdgePaint.setStrokeWidth(9f);

        selectedNodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedNodePaint.setStyle(Paint.Style.STROKE);
        selectedNodePaint.setStrokeWidth(6f);

        deadlockedNodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        deadlockedNodePaint.setColor(Color.parseColor("#F2B8B5"));
        deadlockedNodePaint.setStyle(Paint.Style.STROKE);
        deadlockedNodePaint.setStrokeWidth(8f);

        dotAvailablePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotAvailablePaint.setColor(Color.parseColor("#81C995"));
        dotAvailablePaint.setStyle(Paint.Style.FILL);

        dotUsedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotUsedPaint.setColor(Color.parseColor("#44FFFFFF"));
        dotUsedPaint.setStyle(Paint.Style.FILL);

        handleFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handleFillPaint.setStyle(Paint.Style.FILL);

        handleBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handleBorderPaint.setColor(Color.parseColor("#121216"));
        handleBorderPaint.setStyle(Paint.Style.STROKE);
        handleBorderPaint.setStrokeWidth(4f);

        edgeLabelBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgeLabelBgPaint.setColor(Color.parseColor("#1E293B"));
        edgeLabelBgPaint.setStyle(Paint.Style.FILL);

        edgeLabelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgeLabelTextPaint.setColor(Color.parseColor("#CBD5E1"));
        edgeLabelTextPaint.setTextSize(18f);
        edgeLabelTextPaint.setTextAlign(Paint.Align.CENTER);
        edgeLabelTextPaint.setFakeBoldText(true);

        pulseAnimator = ValueAnimator.ofFloat(0.3f, 1f);
        pulseAnimator.setDuration(1000);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.addUpdateListener(animation -> {
            pulseAlpha = (float) animation.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    public void setGraph(List<BankersAlgorithm.Node> nodes, List<BankersAlgorithm.Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
        invalidate();
    }

    public void setDeadlockResult(BankersAlgorithm.DeadlockResult result) {
        this.deadlockResult = result;
        invalidate();
    }

    public void setSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    public void setEdgeCreatedListener(OnEdgeCreatedListener listener) {
        this.edgeCreatedListener = listener;
    }

    public BankersAlgorithm.Node getSelectedNode() {
        return selectedNode;
    }

    public BankersAlgorithm.Edge getSelectedEdge() {
        return selectedEdge;
    }

    public void clearSelection() {
        selectedNode = null;
        selectedEdge = null;
        if (selectionChangeListener != null) selectionChangeListener.onSelectionCleared();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.parseColor("#121216"));

        canvas.save();
        // Pan & Zoom
        canvas.translate(panOffsetX, panOffsetY);
        canvas.scale(scaleFactor, scaleFactor);

        // Group edges to calculate curve offsets for multi-edges between same node pairs
        Map<String, List<BankersAlgorithm.Edge>> edgeGroups = getEdgeGroups();

        // 1. Draw Edges FIRST so they pass under nodes
        for (Map.Entry<String, List<BankersAlgorithm.Edge>> entry : edgeGroups.entrySet()) {
            List<BankersAlgorithm.Edge> group = entry.getValue();
            int totalInGroup = group.size();
            for (int i = 0; i < totalInGroup; i++) {
                BankersAlgorithm.Edge edge = group.get(i);
                BankersAlgorithm.Node src = findNode(edge.source);
                BankersAlgorithm.Node tgt = findNode(edge.target);
                if (src != null && tgt != null) {
                    drawEdgeWithOffset(canvas, src, tgt, edge, i, totalInGroup);
                }
            }
        }

        // 2. Draw Active Cable Drag
        if (isDraggingCable && cableStartNode != null) {
            Path p = new Path();
            p.moveTo(cableStartX, cableStartY);
            float midX = (cableStartX + cableCurrentX) / 2;
            p.cubicTo(midX, cableStartY, midX, cableCurrentY, cableCurrentX, cableCurrentY);

            Paint cablePaint = "process".equals(cableStartNode.type) ? reqEdgePaint : allocEdgePaint;
            canvas.drawPath(p, cablePaint);
        }

        // 3. Draw Translucent Nodes on TOP of edges (lines show through as diffused lines)
        for (BankersAlgorithm.Node node : nodes) {
            boolean isSelected = (node == selectedNode);
            boolean isDeadlocked = deadlockResult.deadlockedProcesses.contains(node.id);

            if ("resource".equals(node.type) && deadlockResult.isDeadlocked) {
                for (BankersAlgorithm.Edge e : edges) {
                    if ((e.source.equals(node.id) && deadlockResult.deadlockedProcesses.contains(e.target)) ||
                        (e.target.equals(node.id) && deadlockResult.deadlockedProcesses.contains(e.source))) {
                        isDeadlocked = true;
                        break;
                    }
                }
            }

            if ("process".equals(node.type)) {
                drawProcessNode(canvas, node, isSelected, isDeadlocked);
            } else {
                drawResourceNode(canvas, node, isSelected, isDeadlocked);
            }

            // Connection Handles
            drawNodeHandles(canvas, node);
        }

        canvas.restore();
    }

    private Map<String, List<BankersAlgorithm.Edge>> getEdgeGroups() {
        Map<String, List<BankersAlgorithm.Edge>> groups = new HashMap<>();
        for (BankersAlgorithm.Edge e : edges) {
            String key = e.source.compareTo(e.target) < 0 ? e.source + "_" + e.target : e.target + "_" + e.source;
            if (!groups.containsKey(key)) {
                groups.put(key, new ArrayList<>());
            }
            groups.get(key).add(e);
        }
        return groups;
    }

    private void drawEdgeWithOffset(Canvas canvas, BankersAlgorithm.Node src, BankersAlgorithm.Node tgt, BankersAlgorithm.Edge edge, int index, int total) {
        float srcPortX = getOutPortX(src);
        float srcPortY = src.y;
        float tgtPortX = getInPortX(tgt);
        float tgtPortY = tgt.y;

        float dx = tgtPortX - srcPortX;
        float dy = tgtPortY - srcPortY;
        float dist = (float) Math.hypot(dx, dy);
        if (dist == 0) dist = 1f;

        float perpX = -dy / dist;
        float perpY = dx / dist;

        float offsetMultiplier = (index - (total - 1) / 2.0f) * 70f;
        float midX = (srcPortX + tgtPortX) / 2f + perpX * offsetMultiplier;
        float midY = (srcPortY + tgtPortY) / 2f + perpY * offsetMultiplier;

        boolean isSelected = (edge == selectedEdge);
        boolean isDeadlocked = deadlockResult.deadlockedProcesses.contains(src.id) || deadlockResult.deadlockedProcesses.contains(tgt.id);

        Paint p;
        if (isSelected) {
            p = selectedEdgePaint;
            p.setAlpha(255);
        } else if (isDeadlocked && deadlockResult.isDeadlocked) {
            p = deadlockedEdgePaint;
            p.setAlpha((int) (255 * pulseAlpha));
        } else {
            p = "process".equals(src.type) ? reqEdgePaint : allocEdgePaint;
            p.setAlpha(255);
        }

        Path path = new Path();
        path.moveTo(srcPortX, srcPortY);
        path.quadTo(midX, midY, tgtPortX, tgtPortY);

        canvas.drawPath(path, p);

        // Arrowhead
        float t = 0.9f;
        float arrowX = (1 - t) * (1 - t) * srcPortX + 2 * (1 - t) * t * midX + t * t * tgtPortX;
        float arrowY = (1 - t) * (1 - t) * srcPortY + 2 * (1 - t) * t * midY + t * t * tgtPortY;
        float angle = (float) Math.atan2(tgtPortY - arrowY, tgtPortX - arrowX);

        float arrowSize = 18f;
        Path arrow = new Path();
        arrow.moveTo(tgtPortX, tgtPortY);
        arrow.lineTo(tgtPortX - (float) (Math.cos(angle - Math.PI / 6) * arrowSize), tgtPortY - (float) (Math.sin(angle - Math.PI / 6) * arrowSize));
        arrow.lineTo(tgtPortX - (float) (Math.cos(angle + Math.PI / 6) * arrowSize), tgtPortY - (float) (Math.sin(angle + Math.PI / 6) * arrowSize));
        arrow.close();

        Paint arrowPaint = new Paint(p);
        arrowPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrow, arrowPaint);

        // Edge Label Badge
        String labelText = "process".equals(src.type) ? "Request" : "Allocation";
        if (total > 1) {
            labelText += " (#" + (index + 1) + ")";
        }

        float labelX = (srcPortX + 2 * midX + tgtPortX) / 4f;
        float labelY = (srcPortY + 2 * midY + tgtPortY) / 4f;

        Rect textBounds = new Rect();
        edgeLabelTextPaint.getTextBounds(labelText, 0, labelText.length(), textBounds);
        float paddingX = 14f;
        float paddingY = 8f;

        RectF labelBg = new RectF(
                labelX - textBounds.width() / 2f - paddingX,
                labelY - textBounds.height() / 2f - paddingY,
                labelX + textBounds.width() / 2f + paddingX,
                labelY + textBounds.height() / 2f + paddingY
        );
        canvas.drawRoundRect(labelBg, 6f, 6f, edgeLabelBgPaint);
        canvas.drawText(labelText, labelX, labelY + textBounds.height() / 2f - 2f, edgeLabelTextPaint);
    }

    private void drawProcessNode(Canvas canvas, BankersAlgorithm.Node node, boolean isSelected, boolean isDeadlocked) {
        float radius = 110f;

        if (isDeadlocked) {
            deadlockedNodePaint.setAlpha((int) (255 * pulseAlpha));
            canvas.drawCircle(node.x, node.y, radius + 4, deadlockedNodePaint);
        } else if (isSelected) {
            selectedNodePaint.setColor(Color.parseColor("#D0BCFF"));
            canvas.drawCircle(node.x, node.y, radius + 4, selectedNodePaint);
        }

        // Translucent fill
        canvas.drawCircle(node.x, node.y, radius, processPaint);

        // Top Accent Arc
        Paint topAccent = new Paint(Paint.ANTI_ALIAS_FLAG);
        topAccent.setColor(Color.parseColor("#D0BCFF"));
        topAccent.setStyle(Paint.Style.STROKE);
        topAccent.setStrokeWidth(6f);
        canvas.drawArc(new RectF(node.x - radius, node.y - radius, node.x + radius, node.y + radius), 210, 120, false, topAccent);

        // Title Label
        canvas.drawText(node.label, node.x, node.y - 35, labelPaint);

        // Subtext Lines
        float currentY = node.y + 2;

        // Holds
        if (deadlockResult.allocations != null && deadlockResult.allocations.containsKey(node.id)) {
            List<String> holdsList = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : deadlockResult.allocations.get(node.id).entrySet()) {
                if (entry.getValue() > 0) {
                    BankersAlgorithm.Node rNode = findNode(entry.getKey());
                    String rLabel = rNode != null ? rNode.label : entry.getKey();
                    holdsList.add(rLabel + "(" + entry.getValue() + ")");
                }
            }
            if (!holdsList.isEmpty()) {
                canvas.drawText("Holds: " + String.join(", ", holdsList), node.x, currentY, greenTextPaint);
                currentY += 26;
            }
        }

        // Reqs
        if (deadlockResult.requests != null && deadlockResult.requests.containsKey(node.id)) {
            List<String> reqsList = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : deadlockResult.requests.get(node.id).entrySet()) {
                if (entry.getValue() > 0) {
                    BankersAlgorithm.Node rNode = findNode(entry.getKey());
                    String rLabel = rNode != null ? rNode.label : entry.getKey();
                    reqsList.add(rLabel + "(" + entry.getValue() + ")");
                }
            }
            if (!reqsList.isEmpty()) {
                canvas.drawText("Reqs: " + String.join(", ", reqsList), node.x, currentY, purpleTextPaint);
            }
        }
    }

    private void drawResourceNode(Canvas canvas, BankersAlgorithm.Node node, boolean isSelected, boolean isDeadlocked) {
        float halfW = 115f;
        float halfH = 100f;
        RectF rect = new RectF(node.x - halfW, node.y - halfH, node.x + halfW, node.y + halfH);

        if (isDeadlocked) {
            deadlockedNodePaint.setAlpha((int) (255 * pulseAlpha));
            canvas.drawRoundRect(new RectF(node.x - halfW - 4, node.y - halfH - 4, node.x + halfW + 4, node.y + halfH + 4), 16f, 16f, deadlockedNodePaint);
        } else if (isSelected) {
            selectedNodePaint.setColor(Color.parseColor("#F6BE00"));
            canvas.drawRoundRect(new RectF(node.x - halfW - 4, node.y - halfH - 4, node.x + halfW + 4, node.y + halfH + 4), 16f, 16f, selectedNodePaint);
        }

        // Translucent fill
        canvas.drawRoundRect(rect, 16f, 16f, resourcePaint);

        // Top Accent Line
        Paint topAccent = new Paint(Paint.ANTI_ALIAS_FLAG);
        topAccent.setColor(Color.parseColor("#F6BE00"));
        topAccent.setStyle(Paint.Style.STROKE);
        topAccent.setStrokeWidth(6f);
        canvas.drawLine(node.x - halfW + 16f, node.y - halfH, node.x + halfW - 16f, node.y - halfH, topAccent);

        // Title Label
        canvas.drawText(node.label, node.x, node.y - 45, labelPaint);

        // Unallocated count
        int available = deadlockResult.availableInstances.containsKey(node.id) ? deadlockResult.availableInstances.get(node.id) : node.totalInstances;
        canvas.drawText("Unallocated: " + available + " / " + node.totalInstances, node.x, node.y - 15, subTextPaint);

        // Status Dots
        float startX = node.x - (node.totalInstances * 16f) / 2f + 8f;
        float dotY = node.y + 12f;
        for (int i = 0; i < node.totalInstances; i++) {
            Paint dp = (i < available) ? dotAvailablePaint : dotUsedPaint;
            canvas.drawCircle(startX + (i * 16f), dotY, 5f, dp);
        }

        // Held by / Req by subtext
        float currentY = node.y + 42;
        List<String> heldByList = new ArrayList<>();
        List<String> reqByList = new ArrayList<>();

        if (deadlockResult.allocations != null) {
            for (Map.Entry<String, Map<String, Integer>> pEntry : deadlockResult.allocations.entrySet()) {
                int count = pEntry.getValue().containsKey(node.id) ? pEntry.getValue().get(node.id) : 0;
                if (count > 0) {
                    BankersAlgorithm.Node pNode = findNode(pEntry.getKey());
                    heldByList.add((pNode != null ? pNode.label : pEntry.getKey()) + "(" + count + ")");
                }
            }
        }
        if (deadlockResult.requests != null) {
            for (Map.Entry<String, Map<String, Integer>> pEntry : deadlockResult.requests.entrySet()) {
                int count = pEntry.getValue().containsKey(node.id) ? pEntry.getValue().get(node.id) : 0;
                if (count > 0) {
                    BankersAlgorithm.Node pNode = findNode(pEntry.getKey());
                    reqByList.add((pNode != null ? pNode.label : pEntry.getKey()) + "(" + count + ")");
                }
            }
        }

        if (!heldByList.isEmpty()) {
            canvas.drawText("Held by: " + String.join(", ", heldByList), node.x, currentY, greenTextPaint);
            currentY += 26;
        }
        if (!reqByList.isEmpty()) {
            canvas.drawText("Req by: " + String.join(", ", reqByList), node.x, currentY, purpleTextPaint);
        }
    }

    private void drawNodeHandles(Canvas canvas, BankersAlgorithm.Node node) {
        float inX = getInPortX(node);
        float inY = node.y;
        float outX = getOutPortX(node);
        float outY = node.y;

        int inColor = "process".equals(node.type) ? Color.parseColor("#81C995") : Color.parseColor("#D0BCFF");
        int outColor = "process".equals(node.type) ? Color.parseColor("#D0BCFF") : Color.parseColor("#81C995");

        // In Port
        handleFillPaint.setColor(inColor);
        canvas.drawCircle(inX, inY, 14f, handleFillPaint);
        canvas.drawCircle(inX, inY, 14f, handleBorderPaint);

        Paint inBadgePaint = new Paint(subTextPaint);
        inBadgePaint.setColor(inColor);
        inBadgePaint.setTextSize(18f);
        inBadgePaint.setFakeBoldText(true);
        canvas.drawText("In", inX + 28f, inY + 6f, inBadgePaint);

        // Out Port
        handleFillPaint.setColor(outColor);
        canvas.drawCircle(outX, outY, 14f, handleFillPaint);
        canvas.drawCircle(outX, outY, 14f, handleBorderPaint);

        Paint outBadgePaint = new Paint(subTextPaint);
        outBadgePaint.setColor(outColor);
        outBadgePaint.setTextSize(18f);
        outBadgePaint.setFakeBoldText(true);
        canvas.drawText("Out", outX - 30f, outY + 6f, outBadgePaint);
    }

    private float getInPortX(BankersAlgorithm.Node node) {
        return "process".equals(node.type) ? node.x - 110f : node.x - 115f;
    }

    private float getOutPortX(BankersAlgorithm.Node node) {
        return "process".equals(node.type) ? node.x + 110f : node.x + 115f;
    }

    private BankersAlgorithm.Node findNode(String id) {
        for (BankersAlgorithm.Node n : nodes) {
            if (n.id.equals(id)) return n;
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        float screenX = event.getX();
        float screenY = event.getY();

        float worldX = (screenX - panOffsetX) / scaleFactor;
        float worldY = (screenY - panOffsetY) / scaleFactor;

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                isPanning = false;
                lastPanTouchX = screenX;
                lastPanTouchY = screenY;

                // 1. Check Out Ports AND In Ports for cable creation
                for (BankersAlgorithm.Node n : nodes) {
                    float outX = getOutPortX(n);
                    float outY = n.y;
                    float inX = getInPortX(n);
                    float inY = n.y;

                    if (Math.hypot(worldX - outX, worldY - outY) < (50 / scaleFactor)) {
                        isDraggingCable = true;
                        cableStartNode = n;
                        cableStartPortType = "out";
                        cableStartX = outX;
                        cableStartY = outY;
                        cableCurrentX = worldX;
                        cableCurrentY = worldY;
                        invalidate();
                        return true;
                    } else if (Math.hypot(worldX - inX, worldY - inY) < (50 / scaleFactor)) {
                        isDraggingCable = true;
                        cableStartNode = n;
                        cableStartPortType = "in";
                        cableStartX = inX;
                        cableStartY = inY;
                        cableCurrentX = worldX;
                        cableCurrentY = worldY;
                        invalidate();
                        return true;
                    }
                }

                // 2. Check Edge touch
                BankersAlgorithm.Edge touchedEdge = getEdgeAt(worldX, worldY);
                if (touchedEdge != null) {
                    selectedEdge = touchedEdge;
                    selectedNode = null;
                    if (selectionChangeListener != null) selectionChangeListener.onEdgeSelected(selectedEdge);
                    invalidate();
                    return true;
                }

                // 3. Check Node touch
                BankersAlgorithm.Node touchedNode = getNodeAt(worldX, worldY);
                if (touchedNode != null) {
                    selectedNode = touchedNode;
                    selectedEdge = null;
                    draggingNode = touchedNode;
                    if (selectionChangeListener != null) selectionChangeListener.onNodeSelected(selectedNode);
                } else {
                    selectedNode = null;
                    selectedEdge = null;
                    isPanning = true;
                    if (selectionChangeListener != null) selectionChangeListener.onSelectionCleared();
                }

                lastWorldX = worldX;
                lastWorldY = worldY;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() >= 2) {
                    float dx = screenX - lastPanTouchX;
                    float dy = screenY - lastPanTouchY;
                    panOffsetX += dx;
                    panOffsetY += dy;
                    lastPanTouchX = screenX;
                    lastPanTouchY = screenY;
                    invalidate();
                } else if (isDraggingCable) {
                    cableCurrentX = worldX;
                    cableCurrentY = worldY;
                    invalidate();
                } else if (draggingNode != null) {
                    draggingNode.x += (worldX - lastWorldX);
                    draggingNode.y += (worldY - lastWorldY);

                    resolveCollisions(draggingNode);

                    lastWorldX = worldX;
                    lastWorldY = worldY;
                    invalidate();
                } else if (isPanning) {
                    float dx = screenX - lastPanTouchX;
                    float dy = screenY - lastPanTouchY;
                    panOffsetX += dx;
                    panOffsetY += dy;
                    lastPanTouchX = screenX;
                    lastPanTouchY = screenY;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (isDraggingCable && cableStartNode != null) {
                    BankersAlgorithm.Node targetNode = null;

                    for (BankersAlgorithm.Node n : nodes) {
                        if (n == cableStartNode) continue;

                        float targetInX = getInPortX(n);
                        float targetInY = n.y;
                        float targetOutX = getOutPortX(n);
                        float targetOutY = n.y;

                        if ("out".equals(cableStartPortType) && Math.hypot(worldX - targetInX, worldY - targetInY) < (60 / scaleFactor)) {
                            targetNode = n;
                            break;
                        } else if ("in".equals(cableStartPortType) && Math.hypot(worldX - targetOutX, worldY - targetOutY) < (60 / scaleFactor)) {
                            targetNode = n;
                            break;
                        }
                    }

                    if (targetNode != null) {
                        if (cableStartNode.type.equals(targetNode.type)) {
                            if (edgeCreatedListener != null) {
                                edgeCreatedListener.onInvalidConnection("Cannot connect " + cableStartNode.type + " to " + targetNode.type + "!");
                            }
                        } else {
                            if (edgeCreatedListener != null) {
                                if ("out".equals(cableStartPortType)) {
                                    edgeCreatedListener.onEdgeCreated(cableStartNode, targetNode);
                                } else {
                                    edgeCreatedListener.onEdgeCreated(targetNode, cableStartNode);
                                }
                            }
                        }
                    }

                    isDraggingCable = false;
                    cableStartNode = null;
                    invalidate();
                }
                draggingNode = null;
                isPanning = false;
                break;
        }
        return super.onTouchEvent(event);
    }

    private void resolveCollisions(BankersAlgorithm.Node movedNode) {
        float minDist = 250f;
        for (BankersAlgorithm.Node other : nodes) {
            if (other == movedNode) continue;
            float dx = other.x - movedNode.x;
            float dy = other.y - movedNode.y;
            float dist = (float) Math.hypot(dx, dy);
            if (dist < minDist && dist > 0) {
                float overlap = minDist - dist;
                float nx = dx / dist;
                float ny = dy / dist;
                other.x += nx * overlap * 0.5f;
                other.y += ny * overlap * 0.5f;
            }
        }
    }

    private BankersAlgorithm.Node getNodeAt(float worldX, float worldY) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            BankersAlgorithm.Node n = nodes.get(i);
            if (Math.hypot(n.x - worldX, n.y - worldY) < 115) {
                return n;
            }
        }
        return null;
    }

    private BankersAlgorithm.Edge getEdgeAt(float worldX, float worldY) {
        float touchRadius = 60f / scaleFactor;
        Map<String, List<BankersAlgorithm.Edge>> edgeGroups = getEdgeGroups();

        for (Map.Entry<String, List<BankersAlgorithm.Edge>> entry : edgeGroups.entrySet()) {
            List<BankersAlgorithm.Edge> group = entry.getValue();
            int totalInGroup = group.size();
            for (int i = 0; i < totalInGroup; i++) {
                BankersAlgorithm.Edge e = group.get(i);
                BankersAlgorithm.Node src = findNode(e.source);
                BankersAlgorithm.Node tgt = findNode(e.target);
                if (src != null && tgt != null) {
                    float srcX = getOutPortX(src);
                    float srcY = src.y;
                    float tgtX = getInPortX(tgt);
                    float tgtY = tgt.y;

                    float dx = tgtX - srcX;
                    float dy = tgtY - srcY;
                    float dist = (float) Math.hypot(dx, dy);
                    if (dist == 0) dist = 1f;

                    float perpX = -dy / dist;
                    float perpY = dx / dist;

                    float offsetMultiplier = (i - (totalInGroup - 1) / 2.0f) * 70f;
                    float midX = (srcX + tgtX) / 2f + perpX * offsetMultiplier;
                    float midY = (srcY + tgtY) / 2f + perpY * offsetMultiplier;

                    for (float t = 0.1f; t <= 0.9f; t += 0.1f) {
                        float oneMinusT = 1 - t;
                        float qx = oneMinusT * oneMinusT * srcX + 2 * oneMinusT * t * midX + t * t * tgtX;
                        float qy = oneMinusT * oneMinusT * srcY + 2 * oneMinusT * t * midY + t * t * tgtY;

                        if (Math.hypot(worldX - qx, worldY - qy) < touchRadius) {
                            return e;
                        }
                    }
                }
            }
        }
        return null;
    }
}
