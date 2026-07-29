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

    private LinearLayout propertiesPanel;
    private LinearLayout nodePropertiesContainer;
    private TextInputEditText inputLabel;
    private LinearLayout dynamicResourceContainer;
    private MaterialButton btnDeleteSelection;

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

        propertiesPanel = view.findViewById(R.id.propertiesPanel);
        nodePropertiesContainer = view.findViewById(R.id.nodePropertiesContainer);
        inputLabel = view.findViewById(R.id.inputLabel);
        dynamicResourceContainer = view.findViewById(R.id.dynamicResourceContainer);
        btnDeleteSelection = view.findViewById(R.id.btnDeleteSelection);

        MaterialButton btnAddProcess = view.findViewById(R.id.btnAddProcess);
        MaterialButton btnAddResource = view.findViewById(R.id.btnAddResource);
        MaterialButton btnClear = view.findViewById(R.id.btnClear);

        btnAddProcess.setOnClickListener(v -> addProcess());
        btnAddResource.setOnClickListener(v -> addResource());
        btnClear.setOnClickListener(v -> clearGraph());

        btnDeleteSelection.setOnClickListener(v -> deleteSelection());

        canvas.setSelectionChangeListener(new DeadlockCanvasView.OnSelectionChangeListener() {
            @Override
            public void onNodeSelected(BankersAlgorithm.Node node) {
                showNodeProperties(node);
            }

            @Override
            public void onEdgeSelected(BankersAlgorithm.Edge edge) {
                propertiesPanel.setVisibility(View.GONE);
            }

            @Override
            public void onSelectionCleared() {
                propertiesPanel.setVisibility(View.GONE);
            }
        });

        canvas.setEdgeCreatedListener(new DeadlockCanvasView.OnEdgeCreatedListener() {
            @Override
            public void onEdgeCreated(BankersAlgorithm.Node source, BankersAlgorithm.Node target) {
                edges.add(new BankersAlgorithm.Edge(UUID.randomUUID().toString(), source.id, target.id));
                showToast("Connected: " + source.label + " ➔ " + target.label);
                updateGraph();
            }

            @Override
            public void onInvalidConnection(String reason) {
                showToast(reason);
            }
        });

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

        // Initialize basic graph
        addProcess();
        addResource();

        return view;
    }

    private void showNodeProperties(BankersAlgorithm.Node node) {
        propertiesPanel.setVisibility(View.VISIBLE);
        nodePropertiesContainer.setVisibility(View.VISIBLE);

        inputLabel.setText(node.label);
        dynamicResourceContainer.removeAllViews();

        if ("resource".equals(node.type)) {
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

    private void deleteSelection() {
        BankersAlgorithm.Node selectedNode = canvas.getSelectedNode();
        if (selectedNode != null) {
            nodes.remove(selectedNode);
            edges.removeIf(e -> e.source.equals(selectedNode.id) || e.target.equals(selectedNode.id));
            showToast("Deleted Node: " + selectedNode.label);
        }
        canvas.clearSelection();
        updateGraph();
    }

    private void addProcess() {
        pCounter++;
        BankersAlgorithm.Node p = new BankersAlgorithm.Node("p" + pCounter, "process", "P" + pCounter);
        p.x = 220f + (float) (Math.random() * 250);
        p.y = 220f + (float) (Math.random() * 250);
        nodes.add(p);
        updateGraph();
    }

    private void addResource() {
        rCounter++;
        BankersAlgorithm.Node r = new BankersAlgorithm.Node("r" + rCounter, "resource", "R" + rCounter);
        r.x = 220f + (float) (Math.random() * 250);
        r.y = 500f + (float) (Math.random() * 250);
        nodes.add(r);
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

    private void showToast(String message) {
        txtToastMsg.setText(message);
        toastBanner.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> toastBanner.setVisibility(View.GONE), 3000);
    }

    private void updateGraph() {
        BankersAlgorithm.DeadlockResult deadlock = BankersAlgorithm.detectDeadlock(nodes, edges);
        canvas.setDeadlockResult(deadlock);
        canvas.setGraph(nodes, edges);

        if (deadlock.isDeadlocked) {
            statusBox.setBackgroundColor(Color.parseColor("#33EF4444"));
            txtDeadlockStatus.setTextColor(Color.parseColor("#FCA5A5"));
            txtDeadlockStatus.setText("DEADLOCK DETECTED");
        } else {
            statusBox.setBackgroundColor(Color.parseColor("#3310B981"));
            txtDeadlockStatus.setTextColor(Color.parseColor("#6EE7B7"));
            txtDeadlockStatus.setText("NO DEADLOCK");
        }
    }
}
