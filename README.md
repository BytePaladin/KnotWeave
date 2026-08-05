# KnotWeave 🧶

> An interactive Android simulation engine and Resource Allocation Graph (RAG) visualizer for analyzing concurrency and system deadlocks in real time.

---

## 📌 Overview

**KnotWeave** is an educational and diagnostic tool designed to simulate Operating System Resource Allocation Graphs (RAG) directly on Android. It bridges theoretical concurrency concepts with an interactive touch-driven 2D canvas, allowing students, researchers, and developers to model processes and multi-instance resources, inspect allocation matrices, and detect deadlocks as graph topologies evolve.

---

## ✨ Features

- **Interactive Graph Canvas:** Direct touch-and-drag node manipulation with dynamic directional arrow connections for allocations ($R \to P$) and requests ($P \to R$).
- **Canonical 2D Matrix Engine:** Pure mathematical evaluation of system state using standard $N \times M$ integer matrices and 1D state vectors.
- **Real-Time Deadlock Detection:** Instant evaluation of cycle dependencies and resource exhaustion using the Work-Finish algorithm.
- **Visual Deadlock Highlighting:** Deadlocked processes and blocked edges are dynamically highlighted in alert states on canvas.
- **Customizable Resource Instances:** Real-time configuration of single-instance or multi-instance resource capacities.
- **Modular SDK Architecture:** Designed as an independent Android library module (`:sdk`) that can be embedded into any Android application.

---

## 🧮 Theoretical Foundation & Algorithm Design

KnotWeave mathematically represents the system state at any instant using $N$ active processes and $M$ distinct resource types:

$$\text{Processes: } P = \{P_0, P_1, \dots, P_{N-1}\}, \quad \text{Resources: } R = \{R_0, R_1, \dots, R_{M-1}\}$$

### 1. Matrix Formulation
* **Allocation Matrix ($A \in \mathbb{Z}^{N \times M}$):** $A[i][j]$ denotes the number of units of resource $R_j$ currently held by process $P_i$.
* **Request Matrix ($Q \in \mathbb{Z}^{N \times M}$):** $Q[i][j]$ denotes the number of units of resource $R_j$ currently requested by process $P_i$.
* **Available Vector ($V \in \mathbb{Z}^M$):** $V[j]$ denotes the number of available (unallocated) units of resource $R_j$.

### 2. Work-Finish Evaluation Engine
Deadlock detection is computed by simulating resource reclamation over a working vector:

1. Initialize Work vector: $W = V$ and Finish vector: $F[i] = \text{false} \quad \forall i \in [0, N-1]$.
2. Search for an index $i$ such that:
   $$F[i] == \text{false} \quad \text{and} \quad Q[i][j] \le W[j] \quad (\forall j \in [0, M-1])$$
3. If found:
   $$W[j] = W[j] + A[i][j], \quad F[i] = \text{true}, \quad \text{repeat Step 2}.$$
4. If no such $i$ exists and there are processes where $F[i] == \text{false}$, the system contains a **deadlock**, and all processes with $F[i] == \text{false}$ are deadlocked.

---

## 🏗️ Project Architecture

```
KnotWeave/
├── app/                               # Sample Android Application
│   └── src/main/java/.../MainActivity.java
└── sdk/                               # Core KnotWeave Library Module
    └── src/main/java/com/knotweave/sdk/
        ├── BankersAlgorithm.java      # Pure Java 2D Matrix Math & Deadlock Detection Engine
        ├── DeadlockCanvasView.java    # Custom 2D Graphics Canvas & Gesture Dispatcher
        └── KnotWeaveManager.java      # UI State Controller & Event Coordinator
```

- **`BankersAlgorithm.java`:** Pure Java algorithmic core with zero external dependencies. Exposes $O(1)$ index mapping between graph node IDs and canonical matrix structures.
- **`DeadlockCanvasView.java`:** Hardware-accelerated custom `View` that renders processes (circles), resources (rounded cards with instance units), and directed request/allocation vectors with math-calculated arrowheads.
- **`KnotWeaveManager.java`:** Orchestrates canvas touch events, node property modifications, and evaluation triggers.

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio:** Hedgehog (2023.1.1) or newer
- **JDK:** Java 17
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

### Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/BytePaladin/KnotWeave.git
   ```
2. Open the project in Android Studio.
3. Build the debug APK via Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
4. Run the application on an emulator or physical device.

---

## 🎮 How to Use

1. **Add Nodes:** Tap **+ Process** to spawn process nodes ($P_i$) or **+ Resource** to spawn resource nodes ($R_j$).
2. **Connect Edges:**
   - Drag from a **Resource** to a **Process** to create an **Allocation** edge.
   - Drag from a **Process** to a **Resource** to create a **Request** edge.
3. **Inspect Properties:** Tap any node to edit its label or adjust the available total instance capacity.
4. **Observe Real-Time Analysis:** The status banner immediately indicates `NO DEADLOCK` (Safe) or `DEADLOCK DETECTED` (highlighting the deadlocked cycle in red).

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
