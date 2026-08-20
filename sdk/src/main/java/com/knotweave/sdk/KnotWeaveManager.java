package com.knotweave.sdk;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KnotWeaveManager {

    private List<BankersAlgorithm.Node> nodes = new ArrayList<>();
    private List<BankersAlgorithm.Edge> edges = new ArrayList<>();

    private DeadlockCanvasView canvas;

    private TextView txtDeadlockStatus;
    private TextView txtSafeStatus;
    private TextView txtSafeSequence;
    private LinearLayout statusBox;
    private LinearLayout safeBox;
    private MaterialButton btnAutoResolve;

    private LinearLayout propertiesPanel;
    private LinearLayout edgePropertiesContainer;
    private LinearLayout nodePropertiesContainer;
    private TextInputEditText inputLabel;
    private LinearLayout dynamicResourceContainer;
    private LinearLayout dynamicMaxNeedContainer;
    private MaterialButton btnDeleteSelection;
    private MaterialButton btnRemoveSelectedLine;

    private TextInputEditText inputGenP;
    private TextInputEditText inputGenR;
    private TextInputEditText inputGenI;

    private LinearLayout matrixInputContainer;
    private TableLayout tableAllocation;
    private TableLayout tableRequest;
    private boolean isBuildingMatrix = false;

    private LinearLayout toastBanner;
    private TextView txtToastMsg;

    private int pCounter = 0;
    private int rCounter = 0;
    private Context ctx;

    public View initialize(Context context) {
        this.ctx = context;
        View view = LayoutInflater.from(context).inflate(R.layout.layout_simulator, null);

        canvas = view.findViewById(R.id.deadlockCanvas);

        toastBanner = view.findViewById(R.id.toastBanner);
        txtToastMsg = view.findViewById(R.id.txtToastMsg);

        txtDeadlockStatus = view.findViewById(R.id.txtDeadlockStatus);
        txtSafeStatus = view.findViewById(R.id.txtSafeStatus);
        txtSafeSequence = view.findViewById(R.id.txtSafeSequence);
        statusBox = view.findViewById(R.id.statusBox);
        safeBox = view.findViewById(R.id.safeBox);
        btnAutoResolve = view.findViewById(R.id.btnAutoResolve);

        propertiesPanel = view.findViewById(R.id.propertiesPanel);
        edgePropertiesContainer = view.findViewById(R.id.edgePropertiesContainer);
        nodePropertiesContainer = view.findViewById(R.id.nodePropertiesContainer);
        inputLabel = view.findViewById(R.id.inputLabel);
        dynamicResourceContainer = view.findViewById(R.id.dynamicResourceContainer);
        dynamicMaxNeedContainer = view.findViewById(R.id.dynamicMaxNeedContainer);
        btnDeleteSelection = view.findViewById(R.id.btnDeleteSelection);
        btnRemoveSelectedLine = view.findViewById(R.id.btnRemoveSelectedLine);

        inputGenP = view.findViewById(R.id.inputGenP);
        inputGenR = view.findViewById(R.id.inputGenR);
        inputGenI = view.findViewById(R.id.inputGenI);

        matrixInputContainer = view.findViewById(R.id.matrixInputContainer);
        tableAllocation = view.findViewById(R.id.tableAllocation);
        tableRequest = view.findViewById(R.id.tableRequest);

        MaterialButton btnToggleMatrix = view.findViewById(R.id.btnToggleMatrix);
        MaterialButton btnAutoGenerateBatch = view.findViewById(R.id.btnAutoGenerateBatch);
        MaterialButton btnAddProcess = view.findViewById(R.id.btnAddProcess);
        MaterialButton btnAddResource = view.findViewById(R.id.btnAddResource);
        MaterialButton btnForceDeadlock = view.findViewById(R.id.btnForceDeadlock);
        MaterialButton btnClear = view.findViewById(R.id.btnClear);

        if (btnAutoGenerateBatch != null) btnAutoGenerateBatch.setOnClickListener(v -> autoGenerateBatch());
        if (btnAddProcess != null) btnAddProcess.setOnClickListener(v -> addProcess());
        if (btnAddResource != null) btnAddResource.setOnClickListener(v -> addResource());
        if (btnForceDeadlock != null) btnForceDeadlock.setOnClickListener(v -> forceDeadlock());
        if (btnClear != null) btnClear.setOnClickListener(v -> clearGraph());
        if (btnAutoResolve != null) btnAutoResolve.setOnClickListener(v -> autoResolve());

        if (btnToggleMatrix != null) {
            btnToggleMatrix.setOnClickListener(v -> {
                if (matrixInputContainer.getVisibility() == View.VISIBLE) {
                    matrixInputContainer.setVisibility(View.GONE);
                } else {
                    matrixInputContainer.setVisibility(View.VISIBLE);
                    buildMatrixTables();
                }
            });
        }

        if (btnDeleteSelection != null) btnDeleteSelection.setOnClickListener(v -> deleteSelection());
        if (btnRemoveSelectedLine != null) btnRemoveSelectedLine.setOnClickListener(v -> deleteSelection());

        canvas.setSelectionChangeListener(new DeadlockCanvasView.OnSelectionChangeListener() {
            @Override
            public void onNodeSelected(BankersAlgorithm.Node node) {
                showNodeProperties(node);
            }

            @Override
            public void onEdgeSelected(BankersAlgorithm.Edge edge) {
                showEdgeProperties(edge);
            }

            @Override
            public void onSelectionCleared() {
                if (propertiesPanel != null) propertiesPanel.setVisibility(View.GONE);
            }
        });

        canvas.setEdgeCreatedListener(new DeadlockCanvasView.OnEdgeCreatedListener() {
            @Override
            public void onEdgeCreated(BankersAlgorithm.Node source, BankersAlgorithm.Node target) {
                boolean isRequest = "process".equals(source.type);
                if (!isRequest) {
                    int currentAllocs = 0;
                    for (BankersAlgorithm.Edge e : edges) {
                        if (e.source.equals(source.id)) currentAllocs++;
                    }
                    if (currentAllocs >= source.totalInstances) {
                        showToast("Cannot allocate! " + source.label + " has no available instances.");
                        return;
                    }
                    
                    int newAllocCountForTarget = 0;
                    for (BankersAlgorithm.Edge e : edges) {
                        if (e.source.equals(source.id) && e.target.equals(target.id)) newAllocCountForTarget++;
                    }
                    newAllocCountForTarget++;
                    
                    int currentMax = target.maxNeed.containsKey(source.id) ? target.maxNeed.get(source.id) : 0;
                    if (newAllocCountForTarget > currentMax) {
                        target.maxNeed.put(source.id, newAllocCountForTarget);
                    }
                }

                edges.add(new BankersAlgorithm.Edge(UUID.randomUUID().toString(), source.id, target.id));
                showToast("Created " + (isRequest ? "Request" : "Allocation") + ": " + source.label + " ➔ " + target.label);
                refreshState(true);
            }

            @Override
            public void onInvalidConnection(String reason) {
                showToast(reason);
            }
        });

        if (inputLabel != null) {
            inputLabel.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    BankersAlgorithm.Node selected = canvas.getSelectedNode();
                    if (selected != null) {
                        selected.label = s.toString();
                        canvas.invalidate();
                    }
                }
            });
        }

        autoGenerateBatch();
        return view;
    }

    private void showNodeProperties(BankersAlgorithm.Node node) {
        if (propertiesPanel == null) return;
        propertiesPanel.setVisibility(View.VISIBLE);
        if (nodePropertiesContainer != null) nodePropertiesContainer.setVisibility(View.VISIBLE);
        if (edgePropertiesContainer != null) edgePropertiesContainer.setVisibility(View.GONE);

        if (inputLabel != null) inputLabel.setText(node.label);
        if (dynamicResourceContainer != null) dynamicResourceContainer.removeAllViews();
        if (dynamicMaxNeedContainer != null) dynamicMaxNeedContainer.removeAllViews();

        if ("resource".equals(node.type) && dynamicResourceContainer != null) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 10, 0, 10);

            TextView label = new TextView(ctx);
            label.setText("Total Instances:");
            label.setTextColor(Color.parseColor("#F6BE00"));
            label.setTextSize(13f);
            label.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            label.setLayoutParams(lpText);

            EditText et = new EditText(ctx);
            et.setInputType(InputType.TYPE_CLASS_NUMBER);
            et.setText(String.valueOf(node.totalInstances));
            et.setTextColor(Color.WHITE);
            et.setTextSize(13f);
            et.setTypeface(null, android.graphics.Typeface.BOLD);
            et.setGravity(Gravity.CENTER);
            et.setBackgroundResource(R.drawable.input_field_bg);
            LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(dpToPx(75), dpToPx(38));
            et.setLayoutParams(lpEt);

            et.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    try {
                        node.totalInstances = Math.max(1, Integer.parseInt(s.toString()));
                        updateGraph();
                    } catch (Exception ignored) {}
                }
            });

            row.addView(label);
            row.addView(et);
            dynamicResourceContainer.addView(row);

        } else if ("process".equals(node.type) && dynamicMaxNeedContainer != null) {
            TextView header = new TextView(ctx);
            header.setText("Max Need (Banker's Algorithm)");
            header.setTextColor(Color.parseColor("#99FFFFFF"));
            header.setTextSize(12f);
            header.setPadding(0, 12, 0, 8);
            dynamicMaxNeedContainer.addView(header);

            for (BankersAlgorithm.Node rNode : nodes) {
                if (!"resource".equals(rNode.type)) continue;

                LinearLayout row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, 6, 0, 6);

                TextView label = new TextView(ctx);
                label.setText("Max Need for " + rNode.label + ":");
                label.setTextColor(Color.parseColor("#D0BCFF"));
                label.setTextSize(13f);
                label.setTypeface(null, android.graphics.Typeface.BOLD);
                LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                label.setLayoutParams(lpText);

                EditText et = new EditText(ctx);
                et.setInputType(InputType.TYPE_CLASS_NUMBER);
                int curVal = node.maxNeed.containsKey(rNode.id) ? node.maxNeed.get(rNode.id) : 0;
                et.setText(String.valueOf(curVal));
                et.setTextColor(Color.WHITE);
                et.setTextSize(13f);
                et.setTypeface(null, android.graphics.Typeface.BOLD);
                et.setGravity(Gravity.CENTER);
                et.setBackgroundResource(R.drawable.input_field_bg);
                LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(dpToPx(75), dpToPx(38));
                et.setLayoutParams(lpEt);

                et.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(Editable s) {
                        try {
                            int originalVal = Integer.parseInt(s.toString());
                            int val = originalVal;
                            
                            int currentAlloc = 0;
                            for (BankersAlgorithm.Edge e : edges) {
                                if (e.source.equals(rNode.id) && e.target.equals(node.id)) currentAlloc++;
                            }
                            val = Math.max(currentAlloc, Math.min(val, rNode.totalInstances));
                            
                            if (val != originalVal) {
                                et.removeTextChangedListener(this);
                                et.setText(String.valueOf(val));
                                et.setSelection(et.getText().length());
                                et.addTextChangedListener(this);
                            }

                            node.maxNeed.put(rNode.id, val);
                            updateGraph();
                        } catch (Exception ignored) {}
                    }
                });

                row.addView(label);
                row.addView(et);
                dynamicMaxNeedContainer.addView(row);
            }
        }
    }

    private int dpToPx(int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showEdgeProperties(BankersAlgorithm.Edge edge) {
        if (propertiesPanel == null) return;
        propertiesPanel.setVisibility(View.VISIBLE);
        if (nodePropertiesContainer != null) nodePropertiesContainer.setVisibility(View.GONE);
        if (edgePropertiesContainer != null) edgePropertiesContainer.setVisibility(View.VISIBLE);
    }

    private void deleteSelection() {
        BankersAlgorithm.Node selectedNode = canvas.getSelectedNode();
        BankersAlgorithm.Edge selectedEdge = canvas.getSelectedEdge();

        if (selectedNode != null) {
            nodes.remove(selectedNode);
            edges.removeIf(e -> e.source.equals(selectedNode.id) || e.target.equals(selectedNode.id));
            showToast("Deleted Node: " + selectedNode.label);
        } else if (selectedEdge != null) {
            edges.remove(selectedEdge);
            showToast("Removed Line Connection");
        }

        canvas.clearSelection();
        refreshState(true);
    }

    private void autoGenerateBatch() {
        int countP = 2;
        int countR = 2;
        int instI = 1;

        if (inputGenP != null && inputGenR != null && inputGenI != null) {
            try {
                countP = Math.max(1, Integer.parseInt(inputGenP.getText().toString()));
                countR = Math.max(1, Integer.parseInt(inputGenR.getText().toString()));
                instI = Math.max(1, Integer.parseInt(inputGenI.getText().toString()));
            } catch (Exception ignored) {}
        }

        nodes.clear();
        edges.clear();
        pCounter = 0;
        rCounter = 0;

        for (int i = 0; i < countP; i++) {
            pCounter++;
            BankersAlgorithm.Node p = new BankersAlgorithm.Node("p" + pCounter, "process", "P" + pCounter);
            p.x = 220f + (i % 3) * 280f;
            p.y = 220f + (i / 3) * 280f;
            nodes.add(p);
        }

        for (int i = 0; i < countR; i++) {
            rCounter++;
            BankersAlgorithm.Node r = new BankersAlgorithm.Node("r" + rCounter, "resource", "R" + rCounter);
            r.totalInstances = instI;
            r.x = 220f + (i % 3) * 280f;
            r.y = 600f + (i / 3) * 280f;
            nodes.add(r);
        }

        canvas.clearSelection();
        showToast("Generated " + countP + " Processes & " + countR + " Resources");
        refreshState(true);
    }

    private float[] findFreePosition(float startX, float startY) {
        float x = startX;
        float y = startY;
        float radius = 120f;
        int maxAttempts = 50;
        
        for (int i = 0; i < maxAttempts; i++) {
            boolean collision = false;
            for (BankersAlgorithm.Node n : nodes) {
                if (Math.hypot(n.x - x, n.y - y) < radius * 2) {
                    collision = true;
                    break;
                }
            }
            if (!collision) return new float[]{x, y};
            
            x = startX + (float) (Math.random() * 400 - 200);
            y = startY + (float) (Math.random() * 400 - 200);
        }
        return new float[]{x, y};
    }

    private void addProcess() {
        pCounter++;
        BankersAlgorithm.Node p = new BankersAlgorithm.Node("p" + pCounter, "process", "P" + pCounter);
        float[] pos = findFreePosition(220f, 220f);
        p.x = pos[0];
        p.y = pos[1];
        nodes.add(p);
        showToast("Added Process P" + pCounter);
        refreshState(true);
    }

    private void addResource() {
        rCounter++;
        BankersAlgorithm.Node r = new BankersAlgorithm.Node("r" + rCounter, "resource", "R" + rCounter);
        float[] pos = findFreePosition(220f, 550f);
        r.x = pos[0];
        r.y = pos[1];
        nodes.add(r);
        showToast("Added Resource R" + rCounter);
        refreshState(true);
    }

    private void forceDeadlock() {
        List<BankersAlgorithm.Node> pNodes = new ArrayList<>();
        List<BankersAlgorithm.Node> rNodes = new ArrayList<>();
        for (BankersAlgorithm.Node n : nodes) {
            if ("process".equals(n.type)) pNodes.add(n);
            if ("resource".equals(n.type)) rNodes.add(n);
        }

        if (pNodes.size() < 2 || rNodes.size() < 2) {
            showToast("Need at least 2 processes and 2 resources to force a classic cycle.");
            return;
        }

        int cycleLen = Math.min(pNodes.size(), rNodes.size());
        for (int i = 0; i < cycleLen; i++) {
            BankersAlgorithm.Node p = pNodes.get(i);
            BankersAlgorithm.Node rHold = rNodes.get(i);
            BankersAlgorithm.Node rReq = rNodes.get((i + 1) % cycleLen);

            edges.add(new BankersAlgorithm.Edge(UUID.randomUUID().toString(), rHold.id, p.id));
            edges.add(new BankersAlgorithm.Edge(UUID.randomUUID().toString(), p.id, rReq.id));
        }

        showToast("Forced Deadlock Cycle!");
        refreshState(true);
    }

    private void clearGraph() {
        nodes.clear();
        edges.clear();
        pCounter = 0;
        rCounter = 0;
        canvas.clearSelection();
        showToast("Graph Cleared");
        refreshState(true);
    }

    private void autoResolve() {
        BankersAlgorithm.DeadlockResult deadlock = BankersAlgorithm.detectDeadlock(nodes, edges);
        if (deadlock.isDeadlocked && !deadlock.deadlockedProcesses.isEmpty()) {
            String victimId = deadlock.deadlockedProcesses.get(0);
            BankersAlgorithm.Node victimNode = null;
            for (BankersAlgorithm.Node n : nodes) {
                if (n.id.equals(victimId)) {
                    victimNode = n;
                    break;
                }
            }

            nodes.removeIf(n -> n.id.equals(victimId));
            edges.removeIf(e -> e.source.equals(victimId) || e.target.equals(victimId));

            showToast("Auto Resolved: Killed Process " + (victimNode != null ? victimNode.label : victimId));
            refreshState(true);
        }
    }

    private void showToast(String message) {
        if (txtToastMsg != null && toastBanner != null) {
            txtToastMsg.setText(message);
            toastBanner.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> toastBanner.setVisibility(View.GONE), 3000);
        }
    }

    private void refreshState(boolean rebuildMatrix) {
        if (rebuildMatrix && !isBuildingMatrix && matrixInputContainer != null && matrixInputContainer.getVisibility() == View.VISIBLE) {
            buildMatrixTables();
        }
        updateGraph();
    }

    private void buildMatrixTables() {
        if (matrixInputContainer == null || matrixInputContainer.getVisibility() != View.VISIBLE) return;
        isBuildingMatrix = true;

        tableAllocation.removeAllViews();
        tableRequest.removeAllViews();

        List<BankersAlgorithm.Node> pNodes = new ArrayList<>();
        List<BankersAlgorithm.Node> rNodes = new ArrayList<>();
        for (BankersAlgorithm.Node n : nodes) {
            if ("process".equals(n.type)) pNodes.add(n);
            if ("resource".equals(n.type)) rNodes.add(n);
        }

        if (pNodes.isEmpty() || rNodes.isEmpty()) {
            isBuildingMatrix = false;
            return;
        }

        TableRow headerAlloc = new TableRow(ctx);
        TableRow headerReq = new TableRow(ctx);
        headerAlloc.addView(createMatrixCell("", true));
        headerReq.addView(createMatrixCell("", true));

        for (BankersAlgorithm.Node r : rNodes) {
            headerAlloc.addView(createMatrixCell(r.label, true));
            headerReq.addView(createMatrixCell(r.label, true));
        }
        tableAllocation.addView(headerAlloc);
        tableRequest.addView(headerReq);

        for (BankersAlgorithm.Node p : pNodes) {
            TableRow rowAlloc = new TableRow(ctx);
            TableRow rowReq = new TableRow(ctx);
            
            rowAlloc.addView(createMatrixCell(p.label, true));
            rowReq.addView(createMatrixCell(p.label, true));

            for (BankersAlgorithm.Node r : rNodes) {
                int allocCount = 0;
                int reqCount = 0;
                for (BankersAlgorithm.Edge e : edges) {
                    if (e.source.equals(r.id) && e.target.equals(p.id)) allocCount++;
                    if (e.source.equals(p.id) && e.target.equals(r.id)) reqCount++;
                }

                rowAlloc.addView(createMatrixInputCell(p, r, allocCount, false));
                rowReq.addView(createMatrixInputCell(p, r, reqCount, true));
            }
            tableAllocation.addView(rowAlloc);
            tableRequest.addView(rowReq);
        }

        isBuildingMatrix = false;
    }

    private View createMatrixCell(String text, boolean isHeader) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(isHeader ? Color.parseColor("#D0BCFF") : Color.WHITE);
        tv.setTextSize(13f);
        if (isHeader) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        return tv;
    }

    private View createMatrixInputCell(BankersAlgorithm.Node p, BankersAlgorithm.Node r, int value, boolean isRequest) {
        EditText et = new EditText(ctx);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(value));
        et.setTextColor(Color.WHITE);
        et.setTextSize(13f);
        et.setGravity(Gravity.CENTER);
        et.setBackgroundResource(R.drawable.input_field_bg);
        
        TableRow.LayoutParams lp = new TableRow.LayoutParams(dpToPx(40), dpToPx(36));
        lp.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        et.setLayoutParams(lp);

        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isBuildingMatrix) return;
                int val = 0;
                try { val = Integer.parseInt(s.toString()); } catch (Exception ignored) {}
                val = Math.max(0, val);
                syncEdges(p.id, r.id, val, isRequest, et);
                updateGraph();
            }
        });
        return et;
    }

    private void syncEdges(String pId, String rId, int desiredCount, boolean isRequest, EditText et) {
        String srcId = isRequest ? pId : rId;
        String tgtId = isRequest ? rId : pId;

        if (!isRequest) {
            BankersAlgorithm.Node rNode = null;
            for (BankersAlgorithm.Node n : nodes) if (n.id.equals(rId)) rNode = n;
            
            if (rNode != null) {
                int otherAllocs = 0;
                for (BankersAlgorithm.Edge e : edges) {
                    if (e.source.equals(rId) && !e.target.equals(pId)) otherAllocs++;
                }
                
                int maxAllowed = Math.max(0, rNode.totalInstances - otherAllocs);
                if (desiredCount > maxAllowed) {
                    desiredCount = maxAllowed;
                    if (et != null) {
                        isBuildingMatrix = true;
                        et.setText(String.valueOf(desiredCount));
                        et.setSelection(et.getText().length());
                        isBuildingMatrix = false;
                        showToast("Max instances reached!");
                    }
                }
                
                BankersAlgorithm.Node pNode = null;
                for (BankersAlgorithm.Node n : nodes) if (n.id.equals(pId)) pNode = n;
                if (pNode != null) {
                    int currentMax = pNode.maxNeed.containsKey(rId) ? pNode.maxNeed.get(rId) : 0;
                    if (desiredCount > currentMax) {
                        pNode.maxNeed.put(rId, desiredCount);
                    }
                }
            }
        }

        edges.removeIf(e -> e.source.equals(srcId) && e.target.equals(tgtId));
        for (int i = 0; i < desiredCount; i++) {
            edges.add(new BankersAlgorithm.Edge(UUID.randomUUID().toString(), srcId, tgtId));
        }
        canvas.invalidate();
    }

    private void updateGraph() {
        BankersAlgorithm.DeadlockResult deadlock = BankersAlgorithm.detectDeadlock(nodes, edges);
        BankersAlgorithm.SafeStateResult safe = BankersAlgorithm.isSafeState(nodes, edges);

        canvas.setDeadlockResult(deadlock);
        canvas.setGraph(nodes, edges);

        if (statusBox != null && txtDeadlockStatus != null) {
            if (deadlock.isDeadlocked) {
                statusBox.setBackgroundColor(Color.parseColor("#33EF4444"));
                txtDeadlockStatus.setTextColor(Color.parseColor("#FCA5A5"));
                txtDeadlockStatus.setText("DEADLOCK DETECTED");

                if (safeBox != null) safeBox.setVisibility(View.GONE);
                if (btnAutoResolve != null) btnAutoResolve.setVisibility(View.VISIBLE);
            } else {
                statusBox.setBackgroundColor(Color.parseColor("#3310B981"));
                txtDeadlockStatus.setTextColor(Color.parseColor("#6EE7B7"));
                txtDeadlockStatus.setText("NO DEADLOCK");

                if (safeBox != null) safeBox.setVisibility(View.VISIBLE);
                if (btnAutoResolve != null) btnAutoResolve.setVisibility(View.GONE);

                if (safeBox != null && txtSafeStatus != null && txtSafeSequence != null) {
                    if (safe.isSafe) {
                        safeBox.setBackgroundColor(Color.parseColor("#3338BDF8"));
                        txtSafeStatus.setTextColor(Color.parseColor("#7DDBFC"));
                        txtSafeStatus.setText("State: SAFE (Banker's)");

                        StringBuilder seq = new StringBuilder("Safe Sequence: ");
                        for (int i = 0; i < safe.safeSequence.size(); i++) {
                            String pId = safe.safeSequence.get(i);
                            BankersAlgorithm.Node pNode = null;
                            for (BankersAlgorithm.Node n : nodes) {
                                if (n.id.equals(pId)) {
                                    pNode = n;
                                    break;
                                }
                            }
                            seq.append(pNode != null ? pNode.label : pId.toUpperCase());
                            if (i < safe.safeSequence.size() - 1) seq.append(" ➔ ");
                        }
                        txtSafeSequence.setText(seq.toString());
                    } else {
                        safeBox.setBackgroundColor(Color.parseColor("#33F59E0B"));
                        txtSafeStatus.setTextColor(Color.parseColor("#FCD34D"));
                        txtSafeStatus.setText("State: UNSAFE (Banker's)");
                        txtSafeSequence.setText("Safe Sequence: None");
                    }
                }
            }
        }
    }
}
