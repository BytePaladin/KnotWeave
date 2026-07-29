package com.knotweave.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DeadlockCanvasView extends View {

    private List<BankersAlgorithm.Node> nodes = new ArrayList<>();
    private List<BankersAlgorithm.Edge> edges = new ArrayList<>();
    private BankersAlgorithm.DeadlockResult deadlockResult = new BankersAlgorithm.DeadlockResult();

    // Basic Paints for Prototype
    private Paint processPaint, resourcePaint;
    private Paint labelPaint, subTextPaint;
    private Paint edgePaint, selectedEdgePaint, deadlockedEdgePaint;
    private Paint selectedNodePaint;

    // Interaction State
    private BankersAlgorithm.Node selectedNode = null;
    private BankersAlgorithm.Edge selectedEdge = null;

    // Touch State
    private BankersAlgorithm.Node touchStartNode = null;
    private float touchStartX, touchStartY;
    private float currentTouchX, currentTouchY;
    private boolean isDragging = false;

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
        init();
    }

    public DeadlockCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        processPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        processPaint.setColor(Color.parseColor("#2D3748"));
        processPaint.setStyle(Paint.Style.FILL);

        resourcePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        resourcePaint.setColor(Color.parseColor("#1A202C"));
        resourcePaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(32f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);

        subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subTextPaint.setColor(Color.parseColor("#A0AEC0"));
        subTextPaint.setTextSize(20f);
        subTextPaint.setTextAlign(Paint.Align.CENTER);

        edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgePaint.setColor(Color.parseColor("#CBD5E0"));
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(5f);

        deadlockedEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        deadlockedEdgePaint.setColor(Color.parseColor("#E53E3E"));
        deadlockedEdgePaint.setStyle(Paint.Style.STROKE);
        deadlockedEdgePaint.setStrokeWidth(6f);

        selectedEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedEdgePaint.setColor(Color.parseColor("#3182CE"));
        selectedEdgePaint.setStyle(Paint.Style.STROKE);
        selectedEdgePaint.setStrokeWidth(7f);

        selectedNodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedNodePaint.setColor(Color.parseColor("#3182CE"));
        selectedNodePaint.setStyle(Paint.Style.STROKE);
        selectedNodePaint.setStrokeWidth(5f);
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

    public BankersAlgorithm.Node getSelectedNode() { return selectedNode; }
    public BankersAlgorithm.Edge getSelectedEdge() { return selectedEdge; }

    public void clearSelection() {
        selectedNode = null;
        selectedEdge = null;
        if (selectionChangeListener != null) selectionChangeListener.onSelectionCleared();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.parseColor("#1A202C"));

        // 1. Draw Edges
        for (BankersAlgorithm.Edge edge : edges) {
            BankersAlgorithm.Node src = findNode(edge.source);
            BankersAlgorithm.Node tgt = findNode(edge.target);
            if (src != null && tgt != null) {
                boolean isSelected = (edge == selectedEdge);
                boolean isDeadlocked = deadlockResult.isDeadlocked &&
                        (deadlockResult.deadlockedProcesses.contains(src.id) || deadlockResult.deadlockedProcesses.contains(tgt.id));

                Paint p = isSelected ? selectedEdgePaint : (isDeadlocked ? deadlockedEdgePaint : edgePaint);
                canvas.drawLine(src.x, src.y, tgt.x, tgt.y, p);

                // Arrowhead pointing to target
                float arrowSize = 16f;
                float angle = (float) Math.atan2(tgt.y - src.y, tgt.x - src.x);
                float arrowX = tgt.x - (float) (Math.cos(angle) * 80f);
                float arrowY = tgt.y - (float) (Math.sin(angle) * 80f);

                Path arrow = new Path();
                arrow.moveTo(arrowX, arrowY);
                arrow.lineTo(arrowX - (float) (Math.cos(angle - Math.PI / 6) * arrowSize), arrowY - (float) (Math.sin(angle - Math.PI / 6) * arrowSize));
                arrow.lineTo(arrowX - (float) (Math.cos(angle + Math.PI / 6) * arrowSize), arrowY - (float) (Math.sin(angle + Math.PI / 6) * arrowSize));
                arrow.close();

                Paint arrowPaint = new Paint(p);
                arrowPaint.setStyle(Paint.Style.FILL);
                canvas.drawPath(arrow, arrowPaint);
            }
        }

        // 2. Draw Active Connection Line while dragging
        if (isDragging && touchStartNode != null) {
            canvas.drawLine(touchStartNode.x, touchStartNode.y, currentTouchX, currentTouchY, edgePaint);
        }

        // 3. Draw Nodes
        for (BankersAlgorithm.Node node : nodes) {
            boolean isSelected = (node == selectedNode);

            if ("process".equals(node.type)) {
                if (isSelected) canvas.drawCircle(node.x, node.y, 85f, selectedNodePaint);
                canvas.drawCircle(node.x, node.y, 80f, processPaint);
                canvas.drawText(node.label, node.x, node.y + 10f, labelPaint);
            } else {
                RectF rect = new RectF(node.x - 90f, node.y - 70f, node.x + 90f, node.y + 70f);
                if (isSelected) canvas.drawRoundRect(new RectF(node.x - 94f, node.y - 74f, node.x + 94f, node.y + 74f), 12f, 12f, selectedNodePaint);
                canvas.drawRoundRect(rect, 12f, 12f, resourcePaint);
                canvas.drawText(node.label, node.x, node.y - 10f, labelPaint);
                canvas.drawText("Units: " + node.totalInstances, node.x, node.y + 30f, subTextPaint);
            }
        }
    }

    private BankersAlgorithm.Node findNode(String id) {
        for (BankersAlgorithm.Node n : nodes) {
            if (n.id.equals(id)) return n;
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartNode = getNodeAt(x, y);
                touchStartX = x;
                touchStartY = y;
                currentTouchX = x;
                currentTouchY = y;
                isDragging = false;

                if (touchStartNode != null) {
                    selectedNode = touchStartNode;
                    selectedEdge = null;
                    if (selectionChangeListener != null) selectionChangeListener.onNodeSelected(selectedNode);
                } else {
                    selectedNode = null;
                    selectedEdge = null;
                    if (selectionChangeListener != null) selectionChangeListener.onSelectionCleared();
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                currentTouchX = x;
                currentTouchY = y;
                float dist = (float) Math.hypot(x - touchStartX, y - touchStartY);
                if (dist > 15f) {
                    isDragging = true;
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                if (isDragging && touchStartNode != null) {
                    BankersAlgorithm.Node target = getNodeAt(x, y);
                    if (target != null && target != touchStartNode) {
                        // Dragged from one node to another -> Create Connection Line!
                        if (touchStartNode.type.equals(target.type)) {
                            if (edgeCreatedListener != null) {
                                edgeCreatedListener.onInvalidConnection("Cannot connect " + touchStartNode.type + " to " + target.type + "!");
                            }
                        } else {
                            if (edgeCreatedListener != null) {
                                edgeCreatedListener.onEdgeCreated(touchStartNode, target);
                            }
                        }
                    } else {
                        // Dragged into empty space -> Move Node position!
                        touchStartNode.x = x;
                        touchStartNode.y = y;
                    }
                }
                isDragging = false;
                touchStartNode = null;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private BankersAlgorithm.Node getNodeAt(float x, float y) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            BankersAlgorithm.Node n = nodes.get(i);
            if (Math.abs(n.x - x) < 95 && Math.abs(n.y - y) < 80) {
                return n;
            }
        }
        return null;
    }
}
