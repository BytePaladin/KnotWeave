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
    private LinearLayout statusBox;
    private MaterialButton btnAutoResolve;

    private LinearLayout propertiesPanel;
    private LinearLayout edgePropertiesContainer;
    private LinearLayout nodePropertiesContainer;
    private TextInputEditText inputLabel;
    private LinearLayout dynamicResourceContainer;
    private MaterialButton btnDeleteSelection;
    private MaterialButton btnRemoveSelectedLine;

    private TextInputEditText inputGenP;
    private TextInputEditText inputGenR;
    private TextInputEditText inputGenI;

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
        statusBox = view.findViewById(R.id.statusBox);
        btnAutoResolve = view.findViewById(R.id.btnAutoResolve);

        propertiesPanel = view.findViewById(R.id.propertiesPanel);
        edgePropertiesContainer = view.findViewById(R.id.edgePropertiesContainer);
        nodePropertiesContainer = view.findViewById(R.id.nodePropertiesContainer);
        inputLabel = view.findViewById(R.id.inputLabel);
        dynamicResourceContainer = view.findViewById(R.id.dynamicResourceContainer);
        btnDeleteSelection = view.findViewById(R.id.btnDeleteSelection);
        btnRemoveSelectedLine = view.findViewById(R.id.btnRemoveSelectedLine);

        inputGenP = view.findViewById(R.id.inputGenP);
        inputGenR = view.findViewById(R.id.inputGenR);
        inputGenI = view.findViewById(R.id.inputGenI);

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
                edges.add(new BankersAlgorithm.Edge(UUID.randomUUID().toString(), source.id, target.id));
                boolean isRequest = "process".equals(source.type);
                showToast("Created " + (isRequest ? "Request" : "Allocation") + ": " + source.label + " ➔ " + target.label);
                updateGraph();
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
        updateGraph();
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
        updateGraph();
    }

    private void addProcess() {
        pCounter++;
        BankersAlgorithm.Node p = new BankersAlgorithm.Node("p" + pCounter, "process", "P" + pCounter);
        p.x = 220f + (float) (Math.random() * 300);
        p.y = 220f + (float) (Math.random() * 300);
        nodes.add(p);
        showToast("Added Process P" + pCounter);
        updateGraph();
    }

    private void addResource() {
        rCounter++;
        BankersAlgorithm.Node r = new BankersAlgorithm.Node("r" + rCounter, "resource", "R" + rCounter);
        r.x = 220f + (float) (Math.random() * 300);
        r.y = 550f + (float) (Math.random() * 300);
        nodes.add(r);
        showToast("Added Resource R" + rCounter);
        updateGraph();
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
        updateGraph();
    }

    private void clearGraph() {
        nodes.clear();
        edges.clear();
        pCounter = 0;
        rCounter = 0;
        canvas.clearSelection();
        showToast("Graph Cleared");
        updateGraph();
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
            updateGraph();
        }
    }

    private void showToast(String message) {
        if (txtToastMsg != null && toastBanner != null) {
            txtToastMsg.setText(message);
            toastBanner.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> toastBanner.setVisibility(View.GONE), 3000);
        }
    }

    private void updateGraph() {
        BankersAlgorithm.DeadlockResult deadlock = BankersAlgorithm.detectDeadlock(nodes, edges);

        canvas.setDeadlockResult(deadlock);
        canvas.setGraph(nodes, edges);

        if (statusBox != null && txtDeadlockStatus != null) {
            if (deadlock.isDeadlocked) {
                statusBox.setBackgroundColor(Color.parseColor("#33EF4444"));
                txtDeadlockStatus.setTextColor(Color.parseColor("#FCA5A5"));
                txtDeadlockStatus.setText("DEADLOCK DETECTED");

                if (btnAutoResolve != null) btnAutoResolve.setVisibility(View.VISIBLE);
            } else {
                statusBox.setBackgroundColor(Color.parseColor("#3310B981"));
                txtDeadlockStatus.setTextColor(Color.parseColor("#6EE7B7"));
                txtDeadlockStatus.setText("NO DEADLOCK");

                if (btnAutoResolve != null) btnAutoResolve.setVisibility(View.GONE);
            }
        }
    }
}
